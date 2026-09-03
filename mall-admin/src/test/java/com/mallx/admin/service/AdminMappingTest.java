package com.mallx.admin.service;

import static org.junit.jupiter.api.Assertions.*;

import cn.hutool.core.bean.BeanUtil;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mallx.admin.dto.AdminPageQuery;
import com.mallx.admin.support.AdminPages;
import com.mallx.admin.vo.AdminAccountVO;
import com.mallx.auth.entity.AuthAccount;
import com.mallx.common.exception.BusinessException;
import com.mallx.inventory.entity.InventoryStock;
import com.mallx.inventory.vo.InventoryStockVO;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;

/** 验证BeanUtil映射、密码隔离和统计XML解析；不代替真实MySQL执行。 */
class AdminMappingTest {
    @Test
    void beanCopyDoesNotExposePasswordHash() throws Exception {
        AuthAccount account = new AuthAccount();
        account.setId(1L);
        account.setPhone("13800000000");
        account.setPasswordHash("DO_NOT_EXPOSE");
        AdminAccountVO view = BeanUtil.toBean(account, AdminAccountVO.class);
        String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(view);
        assertTrue(json.contains("13800000000"));
        assertFalse(json.contains("password"));
        assertFalse(json.contains("DO_NOT_EXPOSE"));
    }

    @Test
    void copiesBuilderOnlyExistingInventoryView() {
        InventoryStock stock = new InventoryStock();
        stock.setSkuId(9L);
        stock.setAvailableStock(0);
        Page<InventoryStock> page = new Page<>(1, 20, 1);
        page.setRecords(List.of(stock));
        InventoryStockVO view = AdminPages.result(page, InventoryStockVO.class).getRecords().get(0);
        assertEquals(9L, view.getSkuId());
        assertEquals(0, view.getAvailableStock());
    }

    @Test
    void invalidPageSizeIsRejected() {
        AdminPageQuery query = new AdminPageQuery();
        query.setSize(101);
        assertThrows(BusinessException.class, () -> AdminPages.page(query));
        query.setSize(20);
        query.setCurrent(0);
        assertThrows(BusinessException.class, () -> AdminPages.page(query));
    }

    @Test
    void statisticsXmlParsesAndUsesBoundParameters() throws Exception {
        Configuration configuration = new Configuration();
        String resource = "mapper/AdminStatisticsMapper.xml";
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(resource)) {
            assertNotNull(input);
            new XMLMapperBuilder(input, configuration, resource, configuration.getSqlFragments())
                    .parse();
        }
        Map<String, Object> parameters =
                Map.of(
                        "start",
                        LocalDateTime.of(2026, 9, 1, 0, 0),
                        "end",
                        LocalDateTime.of(2026, 9, 3, 0, 0),
                        "limit",
                        10);
        for (String name : List.of("selectSummary", "selectTopProducts")) {
            String sql =
                    configuration
                            .getMappedStatement(
                                    "com.mallx.admin.mapper.AdminStatisticsMapper." + name)
                            .getBoundSql(parameters)
                            .getSql();
            assertNotNull(CCJSqlParserUtil.parse(sql));
            assertFalse(sql.contains("${"));
            assertTrue(sql.contains("?"));
        }
    }
}
