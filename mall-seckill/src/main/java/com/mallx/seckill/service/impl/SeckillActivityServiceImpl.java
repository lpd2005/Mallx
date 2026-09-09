package com.mallx.seckill.service.impl;

import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.mallx.common.enums.ResultCode;
import com.mallx.common.exception.BusinessException;
import com.mallx.inventory.service.InventoryStockService;
import com.mallx.inventory.vo.InventoryStockVO;
import com.mallx.product.entity.ProductSku;
import com.mallx.product.enums.ProductStatus;
import com.mallx.product.service.ProductSkuService;
import com.mallx.seckill.constant.SeckillRedisKeys;
import com.mallx.seckill.dto.SeckillActivityRequest;
import com.mallx.seckill.dto.SeckillItemRequest;
import com.mallx.seckill.entity.SeckillActivity;
import com.mallx.seckill.entity.SeckillItem;
import com.mallx.seckill.enums.SeckillActivityStatus;
import com.mallx.seckill.mapper.SeckillActivityMapper;
import com.mallx.seckill.service.SeckillActivityService;
import com.mallx.seckill.service.SeckillItemService;
import com.mallx.seckill.service.SeckillRedisService;
import com.mallx.seckill.vo.SeckillActivityVO;
import com.mallx.seckill.vo.SeckillItemVO;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 秒杀活动管理实现。
 *
 * <p>草稿阶段允许修改；预热时同时校验商品状态和真实库存，并使用 Redisson 锁避免多实例重复预热。</p>
 */
@Service
@RequiredArgsConstructor
public class SeckillActivityServiceImpl extends ServiceImpl<SeckillActivityMapper, SeckillActivity>
        implements SeckillActivityService {
    private final SeckillItemService itemService;
    private final ProductSkuService productSkuService;
    private final InventoryStockService inventoryStockService;
    private final SeckillRedisService redisService;
    private final RedissonClient redissonClient;

    /** 创建仅保存在数据库中的草稿活动。 */
    @Override
    @Transactional
    public SeckillActivityVO createActivity(SeckillActivityRequest request) {
        validateWindow(request.getStartAt(), request.getEndAt());
        SeckillActivity activity = new SeckillActivity();
        activity.setActivityName(request.getActivityName().trim());
        activity.setStartAt(request.getStartAt());
        activity.setEndAt(request.getEndAt());
        activity.setStatus(SeckillActivityStatus.DRAFT.getValue());
        if (!save(activity)) {
            throw internal("秒杀活动创建失败");
        }
        return toVO(activity, List.of());
    }

    /** 已预热或已关闭的活动不可再修改，避免数据库和 Redis 元数据不一致。 */
    @Override
    @Transactional
    public SeckillActivityVO updateActivity(Long activityId, SeckillActivityRequest request) {
        SeckillActivity activity = requireActivity(activityId);
        requireDraft(activity);
        validateWindow(request.getStartAt(), request.getEndAt());
        activity.setActivityName(request.getActivityName().trim());
        activity.setStartAt(request.getStartAt());
        activity.setEndAt(request.getEndAt());
        if (!updateById(activity)) {
            throw internal("秒杀活动修改失败");
        }
        return toVO(activity, listItems(activityId));
    }

    /** 添加秒杀 SKU，并校验上架状态、价格和可用库存。 */
    @Override
    @Transactional
    public SeckillItemVO addItem(Long activityId, SeckillItemRequest request) {
        SeckillActivity activity = requireActivity(activityId);
        requireDraft(activity);
        long duplicate = itemService.lambdaQuery()
                .eq(SeckillItem::getActivityId, activityId)
                .eq(SeckillItem::getSkuId, request.getSkuId()).count();
        if (duplicate > 0) {
            throw bad("该 SKU 已加入当前秒杀活动");
        }
        ProductSku sku = productSkuService.requireSku(request.getSkuId());
        if (!ProductStatus.ON_SHELF.matches(sku.getStatus())) {
            throw bad("只有已上架 SKU 才能参加秒杀");
        }
        if (request.getSeckillPrice().compareTo(sku.getSalePrice()) > 0) {
            throw bad("秒杀价格不能高于商品销售价");
        }
        InventoryStockVO stock = inventoryStockService.getStock(request.getSkuId());
        if (stock.getAvailableStock() < request.getTotalStock()) {
            throw bad("秒杀库存不能超过当前可用库存");
        }
        SeckillItem item = new SeckillItem();
        item.setActivityId(activityId);
        item.setSkuId(request.getSkuId());
        item.setSeckillPrice(request.getSeckillPrice());
        item.setTotalStock(request.getTotalStock());
        item.setSoldStock(0);
        item.setLimitPerUser(request.getLimitPerUser());
        item.setStatus(1);
        if (!itemService.save(item)) {
            throw internal("秒杀商品添加失败");
        }
        return toVO(item);
    }

    /** 修改草稿活动商品；预热后禁止修改资格库存和价格。 */
    @Override
    @Transactional
    public SeckillItemVO updateItem(Long activityId, Long itemId, SeckillItemRequest request) {
        SeckillActivity activity = requireActivity(activityId);
        requireDraft(activity);
        SeckillItem item = requireItem(activityId, itemId);
        long duplicate = itemService.lambdaQuery()
                .eq(SeckillItem::getActivityId, activityId)
                .eq(SeckillItem::getSkuId, request.getSkuId())
                .ne(SeckillItem::getId, itemId).count();
        if (duplicate > 0) {
            throw bad("该 SKU 已加入当前秒杀活动");
        }
        ProductSku sku = productSkuService.requireSku(request.getSkuId());
        if (!ProductStatus.ON_SHELF.matches(sku.getStatus())) {
            throw bad("只有已上架 SKU 才能参加秒杀");
        }
        if (request.getSeckillPrice().compareTo(sku.getSalePrice()) > 0) {
            throw bad("秒杀价格不能高于商品销售价");
        }
        InventoryStockVO stock = inventoryStockService.getStock(request.getSkuId());
        if (stock.getAvailableStock() < request.getTotalStock()) {
            throw bad("秒杀库存不能超过当前可用库存");
        }
        item.setSkuId(request.getSkuId());
        item.setSeckillPrice(request.getSeckillPrice());
        item.setTotalStock(request.getTotalStock());
        item.setLimitPerUser(request.getLimitPerUser());
        if (!itemService.updateById(item)) {
            throw internal("秒杀商品修改失败");
        }
        return toVO(item);
    }

    /** 从草稿活动中物理移除商品配置。 */
    @Override
    @Transactional
    public void removeItem(Long activityId, Long itemId) {
        SeckillActivity activity = requireActivity(activityId);
        requireDraft(activity);
        SeckillItem item = requireItem(activityId, itemId);
        if (!itemService.removeById(item.getId())) {
            throw internal("秒杀商品删除失败");
        }
    }

    /**
     * 预热活动数据。
     *
     * <p>同一个活动使用独立分布式锁，不同活动仍可并行预热。Redis 库存采用 set-if-absent，
     * 重复预热不会把已经扣减的资格库存重置。</p>
     */
    @Override
    @Transactional
    public SeckillActivityVO prepareActivity(Long activityId) {
        RLock lock = redissonClient.getLock(SeckillRedisKeys.warmupLock(activityId));
        boolean locked = false;
        try {
            locked = lock.tryLock(0, 30, TimeUnit.SECONDS);
            if (!locked) {
                throw bad("活动正在预热，请稍后重试");
            }
            SeckillActivity activity = requireActivity(activityId);
            if (SeckillActivityStatus.CLOSED.matches(activity.getStatus())) {
                throw bad("已关闭活动不能重新预热");
            }
            if (!activity.getEndAt().isAfter(LocalDateTime.now())) {
                throw bad("已结束活动不能预热");
            }
            List<SeckillItem> items = listItems(activityId);
            if (items.isEmpty()) {
                throw bad("请先配置秒杀商品");
            }
            for (SeckillItem item : items) {
                ProductSku sku = productSkuService.requireSku(item.getSkuId());
                if (!ProductStatus.ON_SHELF.matches(sku.getStatus())) {
                    throw bad("SKU " + item.getSkuId() + " 已下架");
                }
                // 预热即把活动库存从 available_stock 划入 locked_stock，普通订单不能再占用。
                // 固定业务号保证活动重复预热不会重复锁定同一批库存。
                inventoryStockService.lockStock(
                        item.getSkuId(), item.getTotalStock(), prepareBusinessNo(item.getId()));
            }
            activity.setStatus(SeckillActivityStatus.READY.getValue());
            if (!updateById(activity)) {
                throw internal("活动状态更新失败");
            }
            redisService.warmUp(activity, items);
            return toVO(activity, items);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw internal("活动预热被中断");
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /** 关闭数据库活动后同步禁用 Redis 元数据，已经获得资格的请求仍可继续完成下单。 */
    @Override
    @Transactional
    public void closeActivity(Long activityId) {
        SeckillActivity activity = requireActivity(activityId);
        List<SeckillItem> items = listItems(activityId);
        boolean draft = SeckillActivityStatus.DRAFT.matches(activity.getStatus());
        if (!SeckillActivityStatus.CLOSED.matches(activity.getStatus())) {
            activity.setStatus(SeckillActivityStatus.CLOSED.getValue());
            if (!updateById(activity)) {
                throw internal("活动关闭失败");
            }
        }
        // 必须先禁止新预扣，再判断 pending 计数，避免检查后又产生新的在途请求。
        redisService.disableItems(items);
        for (SeckillItem item : items) {
            if (draft && Integer.valueOf(1).equals(item.getStatus())) {
                // 草稿活动从未锁定库存，关闭时只禁用商品，绝不能从 locked_stock 释放数量。
                item.setStatus(0);
                if (!itemService.updateById(item)) {
                    throw internal("秒杀商品关闭失败");
                }
            } else {
                // READY 活动以及先前因 pending 未归零而未释放完的 CLOSED 活动都走幂等释放。
                releaseReservationIfSafe(item);
            }
        }
    }

    /** 到达结束时间后自动关闭活动，避免无人操作时预留库存长期停留在 locked_stock。 */
    @Override
    @Transactional
    public int closeExpiredActivities() {
        List<Long> activityIds = lambdaQuery()
                .select(SeckillActivity::getId)
                .eq(SeckillActivity::getStatus, SeckillActivityStatus.READY.getValue())
                .le(SeckillActivity::getEndAt, LocalDateTime.now())
                .list().stream().map(SeckillActivity::getId).toList();
        for (Long activityId : activityIds) {
            closeActivity(activityId);
        }
        return activityIds.size();
    }

    /**
     * 定时释放已关闭活动的剩余预留库存。
     *
     * <p>商品状态为 0 表示释放已完成；库存流水业务号提供第二层幂等保护。</p>
     */
    @Override
    @Transactional
    public int releaseClosedReservations() {
        List<Long> closedActivityIds = lambdaQuery()
                .select(SeckillActivity::getId)
                .eq(SeckillActivity::getStatus, SeckillActivityStatus.CLOSED.getValue())
                .list().stream().map(SeckillActivity::getId).toList();
        if (closedActivityIds.isEmpty()) {
            return 0;
        }
        List<SeckillItem> candidates = itemService.lambdaQuery()
                .in(SeckillItem::getActivityId, closedActivityIds)
                .eq(SeckillItem::getStatus, 1)
                .orderByAsc(SeckillItem::getId).list();
        int released = 0;
        for (SeckillItem item : candidates) {
            if (releaseReservationIfSafe(item)) {
                released++;
            }
        }
        return released;
    }

    /** 后台活动列表允许按生命周期状态筛选。 */
    @Override
    public List<SeckillActivityVO> listActivities(Integer status) {
        if (status != null && status != 0 && status != 1 && status != 2) {
            throw bad("活动状态不正确");
        }
        return lambdaQuery().eq(status != null, SeckillActivity::getStatus, status)
                .orderByDesc(SeckillActivity::getCreatedAt).list().stream()
                .map(activity -> toVO(activity, listItems(activity.getId()))).toList();
    }

    /** 前台只返回已预热且结束时间尚未到达的活动。 */
    @Override
    public List<SeckillActivityVO> listAvailableActivities() {
        LocalDateTime now = LocalDateTime.now();
        return lambdaQuery()
                .eq(SeckillActivity::getStatus, SeckillActivityStatus.READY.getValue())
                .gt(SeckillActivity::getEndAt, now)
                .orderByAsc(SeckillActivity::getStartAt).list().stream()
                .map(activity -> toVO(activity, listItems(activity.getId()))).toList();
    }

    /** 查询活动及其商品配置，用于后台详情。 */
    @Override
    public SeckillActivityVO getActivity(Long activityId) {
        SeckillActivity activity = requireActivity(activityId);
        return toVO(activity, listItems(activityId));
    }

    /** 查询活动商品，固定按主键排序以保持响应顺序稳定。 */
    private List<SeckillItem> listItems(Long activityId) {
        return itemService.lambdaQuery().eq(SeckillItem::getActivityId, activityId)
                .orderByAsc(SeckillItem::getId).list();
    }

    /** 校验活动 ID 并返回实体。 */
    private SeckillActivity requireActivity(Long activityId) {
        if (activityId == null || activityId <= 0) {
            throw bad("活动 ID 不正确");
        }
        SeckillActivity activity = getById(activityId);
        if (activity == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "秒杀活动不存在");
        }
        return activity;
    }

    /** 校验商品属于指定活动，阻止跨活动修改。 */
    private SeckillItem requireItem(Long activityId, Long itemId) {
        if (itemId == null || itemId <= 0) {
            throw bad("秒杀商品 ID 不正确");
        }
        SeckillItem item = itemService.getById(itemId);
        if (item == null || !activityId.equals(item.getActivityId())) {
            throw new BusinessException(ResultCode.NOT_FOUND, "秒杀商品不存在");
        }
        return item;
    }

    /** 活动一旦预热就冻结配置，只能执行关闭操作。 */
    private void requireDraft(SeckillActivity activity) {
        if (!SeckillActivityStatus.DRAFT.matches(activity.getStatus())) {
            throw bad("只有草稿活动可以修改");
        }
    }

    /** 保证时间窗口顺序正确且活动尚未结束。 */
    private void validateWindow(LocalDateTime startAt, LocalDateTime endAt) {
        if (startAt == null || endAt == null || !endAt.isAfter(startAt)) {
            throw bad("活动结束时间必须晚于开始时间");
        }
        if (!endAt.isAfter(LocalDateTime.now())) {
            throw bad("活动结束时间必须晚于当前时间");
        }
    }

    /** 聚合活动和商品实体为接口响应。 */
    private SeckillActivityVO toVO(SeckillActivity activity, List<SeckillItem> items) {
        return SeckillActivityVO.builder().activityId(activity.getId())
                .activityName(activity.getActivityName()).startAt(activity.getStartAt())
                .endAt(activity.getEndAt()).status(activity.getStatus())
                .items(items.stream().map(this::toVO).toList()).build();
    }

    /** 将活动商品实体转换为响应对象。 */
    private SeckillItemVO toVO(SeckillItem item) {
        return SeckillItemVO.builder().itemId(item.getId()).skuId(item.getSkuId())
                .seckillPrice(item.getSeckillPrice()).totalStock(item.getTotalStock())
                .soldStock(item.getSoldStock()).limitPerUser(item.getLimitPerUser())
                .status(item.getStatus()).build();
    }

    /**
     * 没有在途请求时释放“预热数量减已创建订单数量”的余额。
     *
     * <p>已创建订单占用的库存继续留在 locked_stock，后续由支付扣减或订单取消释放。</p>
     */
    private boolean releaseReservationIfSafe(SeckillItem item) {
        if (!Integer.valueOf(1).equals(item.getStatus()) || redisService.hasPendingRequests(item.getId())) {
            return false;
        }
        int remaining = item.getTotalStock() - item.getSoldStock();
        if (remaining < 0) {
            throw internal("秒杀商品销量超过预留库存");
        }
        if (remaining > 0) {
            inventoryStockService.releaseStock(
                    item.getSkuId(), remaining, closeBusinessNo(item.getId()));
        }
        item.setStatus(0);
        if (!itemService.updateById(item)) {
            throw internal("秒杀剩余库存释放状态保存失败");
        }
        return true;
    }

    /** 预热锁库存的幂等业务号。 */
    private String prepareBusinessNo(Long itemId) {
        return "SECKILL-PREPARE-" + itemId;
    }

    /** 活动关闭释放剩余库存的幂等业务号。 */
    private String closeBusinessNo(Long itemId) {
        return "SECKILL-CLOSE-" + itemId;
    }

    private BusinessException bad(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST, message);
    }

    private BusinessException internal(String message) {
        return new BusinessException(ResultCode.INTERNAL_ERROR, message);
    }
}
