package com.mallx.search.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mallx.product.entity.ProductSpu;
import com.mallx.product.service.ProductSpuService;
import com.mallx.search.config.ProductSearchProperties;
import com.mallx.search.document.ProductSearchDocument;
import com.mallx.search.vo.ProductSearchIndexStatusVO;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** 负责单个文档收敛和无停机全量重建。 */
@Service
@RequiredArgsConstructor
public class ProductSearchIndexService {
    /** 所有应用实例共享的索引读写锁名称。 */
    private static final String INDEX_LOCK = "mallx:search:product-index:lock";
    private static final DateTimeFormatter VERSION_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    private final ElasticsearchOperations operations;
    private final ProductSearchDocumentAssembler assembler;
    private final ProductSpuService spuService;
    private final ProductSearchProperties properties;
    private final RedissonClient redissonClient;

    /** 消息重复或乱序时重新读取最新数据，最终仍会得到同一文档。 */
    public void synchronize(Long spuId) {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(INDEX_LOCK);
        RLock readLock = readWriteLock.readLock();
        readLock.lock();
        try {
            ensureIndex();
            Map<Long, ProductSearchDocument> documents = assembler.build(List.of(spuId));
            ProductSearchDocument document = documents.get(spuId);
            IndexCoordinates alias = IndexCoordinates.of(properties.getIndexAlias());
            if (document == null) {
                // 已删除、已下架或没有在售 SKU 时移除历史文档。
                operations.delete(String.valueOf(spuId), alias);
            } else {
                // 文档 ID 固定为 SPU ID，save 对重复事件执行幂等覆盖。
                operations.save(document, alias);
            }
        } finally {
            readLock.unlock();
        }
    }

    /** 新索引构建完成后原子切换别名，消费线程在重建期间等待写锁。 */
    public long rebuild() {
        // 使用读写锁保证并发重建时只有一个实例在重建，其他实例在等待重建完成后继续访问新索引。
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(INDEX_LOCK);
        RLock writeLock = readWriteLock.writeLock();
        writeLock.lock();
        try {
            // 线上查询继续访问旧别名，新物理索引在后台独立构建。
            String newIndexName = physicalIndexName();
            createPhysicalIndex(newIndexName);
            IndexCoordinates newIndex = IndexCoordinates.of(newIndexName);
            long lastId = 0L;
            long indexed = 0L;
            int batchSize = properties.getRebuildBatchSize();
            while (true) {
                // 使用主键游标而不是 offset，数据量大时查询性能更稳定。
                List<Long> spuIds = spuService.list(
                                Wrappers.<ProductSpu>lambdaQuery()
                                        .select(ProductSpu::getId)
                                        .gt(ProductSpu::getId, lastId)
                                        .orderByAsc(ProductSpu::getId)
                                        .last("LIMIT " + batchSize))
                        .stream()
                        .map(ProductSpu::getId)
                        .toList();
                if (spuIds.isEmpty()) {
                    break;
                }
                Map<Long, ProductSearchDocument> documents = assembler.build(spuIds);
                List<IndexQuery> indexQueries = documents.values().stream()
                        .map(document -> new IndexQueryBuilder()
                                .withId(String.valueOf(document.getSpuId()))
                                .withObject(document)
                                .build())
                        .toList();
                if (!indexQueries.isEmpty()) {
                    // 一批文档一次 bulk 写入，减少逐条 HTTP 请求开销。
                    operations.bulkIndex(indexQueries, newIndex);
                    indexed += indexQueries.size();
                }
                lastId = spuIds.get(spuIds.size() - 1);
            }
            operations.indexOps(newIndex).refresh();
            // 所有批次成功后才切换别名，构建失败不会影响线上旧索引。
            switchAlias(newIndexName);
            return indexed;
        } finally {
            writeLock.unlock();
        }
    }

    /** 查询逻辑别名是否存在及其文档数，用于后台健康检查。 */
    public ProductSearchIndexStatusVO status() {
        IndexCoordinates alias = IndexCoordinates.of(properties.getIndexAlias());
        boolean available = operations.indexOps(alias).exists();
        long count = available
                ? operations.count(NativeQuery.builder().withQuery(query -> query.matchAll(match -> match)).build(),
                        ProductSearchDocument.class, alias)
                : 0L;
        return ProductSearchIndexStatusVO.builder()
                .alias(properties.getIndexAlias())
                .available(available)
                .documentCount(count)
                .build();
    }

    /**
     * 首次搜索或首次增量同步时创建空物理索引和别名。
     * Redisson 初始化锁防止多个应用实例同时创建同名资源。
     */
    public void ensureIndex() {
        IndexCoordinates alias = IndexCoordinates.of(properties.getIndexAlias()); // 逻辑别名，所有读写都通过它定位当前生效的物理索引
        if (operations.indexOps(alias).exists()) {
            return; // 快速路径：绝大多数调用发生在索引已存在时，无锁直接返回，避免每次同步都抢分布式锁
        }
        RLock initLock = redissonClient.getLock(INDEX_LOCK + ":init"); // 初始化专用分布式锁，跨实例互斥，防止并发创建出多个物理索引
        initLock.lock();
        try {
            if (operations.indexOps(alias).exists()) {
                return; // 双重检查：等待锁期间其他实例可能已经创建完成，拿到锁后必须重新确认
            }
            String physicalIndexName = physicalIndexName(); // 生成带时间戳与随机后缀的物理索引名，便于识别版本并避免撞名
            createPhysicalIndex(physicalIndexName); // 先建物理索引：从文档注解生成 settings 与 mapping（ES 要求别名只能指向已存在的索引）
            addAlias(physicalIndexName); // 再把逻辑别名指向该索引并标记为写索引，后续写入自然落到新索引
        } finally {
            initLock.unlock(); // 无论创建成功或异常都要释放锁，避免初始化锁泄漏导致所有实例无法初始化
        }
    }

    /** 根据文档注解生成 settings 和 mapping，再创建指定版本物理索引。 */
    private void createPhysicalIndex(String indexName) {
        IndexOperations mappingOperations = operations.indexOps(ProductSearchDocument.class);
        IndexOperations physicalOperations = operations.indexOps(IndexCoordinates.of(indexName));
        if (!physicalOperations.create(
                mappingOperations.createSettings(), mappingOperations.createMapping())) {
            throw new IllegalStateException("创建商品搜索索引失败：" + indexName);
        }
    }

    /**
     * 在一个 alias 请求中移除全部旧指向并添加新指向，避免先删后加产生无索引窗口。
     */
    private void switchAlias(String newIndexName) {
        String alias = properties.getIndexAlias();
        IndexOperations aliasOperations = operations.indexOps(IndexCoordinates.of(alias));
        Map<String, Set<org.springframework.data.elasticsearch.core.index.AliasData>> currentAliases =
                aliasOperations.exists() ? aliasOperations.getAliases(alias) : Map.of();
        List<AliasAction> actions = new ArrayList<>();
        currentAliases.keySet().forEach(indexName -> actions.add(
                new AliasAction.Remove(AliasActionParameters.builder()
                        .withIndices(indexName).withAliases(alias).build())));
        actions.add(new AliasAction.Add(AliasActionParameters.builder()
                .withIndices(newIndexName).withAliases(alias).withIsWriteIndex(true).build()));
        operations.indexOps(IndexCoordinates.of(newIndexName))
                .alias(new AliasActions(actions.toArray(AliasAction[]::new)));
    }

    /** 首次创建索引时直接建立逻辑别名，并标记为写索引。 */
    private void addAlias(String indexName) {
        AliasAction.Add add = new AliasAction.Add(AliasActionParameters.builder()
                .withIndices(indexName)
                .withAliases(properties.getIndexAlias())
                .withIsWriteIndex(true)
                .build());
        operations.indexOps(IndexCoordinates.of(indexName)).alias(new AliasActions(add));
    }

    /** 时间戳方便人工识别版本，随机后缀避免多实例快速重建时名称碰撞。 */
    private String physicalIndexName() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        return properties.getIndexAlias() + "_v"
                + VERSION_FORMAT.format(LocalDateTime.now()) + "_" + suffix;
    }
}
