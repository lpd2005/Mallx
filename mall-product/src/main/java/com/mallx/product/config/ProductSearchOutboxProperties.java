package com.mallx.product.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/** 商品搜索 Outbox 发布参数。 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "mallx.search.outbox")
public class ProductSearchOutboxProperties {
    @Min(1)
    private int batchSize = 100;

    @Min(1)
    private long retryDelaySeconds = 10;
}
