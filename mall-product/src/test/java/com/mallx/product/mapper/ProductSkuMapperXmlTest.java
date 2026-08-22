package com.mallx.product.mapper;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** 商品购物车批量查询XML映射完整性测试。 */
class ProductSkuMapperXmlTest {

    @Test
    void cartProductQueryXmlShouldBeParsed() throws Exception {
        MybatisConfiguration configuration = new MybatisConfiguration();
        ClassPathResource resource = new ClassPathResource("mapper/ProductSkuMapper.xml");
        try (InputStream inputStream = resource.getInputStream()) {
            XMLMapperBuilder builder = new XMLMapperBuilder(inputStream, configuration,
                    resource.getPath(), configuration.getSqlFragments());
            builder.parse();
        }

        assertTrue(configuration.hasStatement(
                "com.mallx.product.mapper.ProductSkuMapper.selectCartProducts", false));
    }
}
