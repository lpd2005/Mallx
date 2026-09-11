package com.mallx.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mallx.product.entity.ProductSearchOutbox;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

/** 商品搜索 Outbox 数据访问。 */
@Mapper
public interface ProductSearchOutboxMapper extends BaseMapper<ProductSearchOutbox> {

    /** 把待发送时间向后移动，防止多个调度线程同时发布同一条记录。 */
    @Update("""
            UPDATE product_search_outbox
            SET retry_count = retry_count + 1,
                next_retry_at = #{nextRetryAt},
                last_error = NULL
            WHERE id = #{id}
              AND status = 0
              AND next_retry_at <= #{now}
            """)
    int claim(@Param("id") Long id,
              @Param("now") LocalDateTime now,
              @Param("nextRetryAt") LocalDateTime nextRetryAt);

    /** 只有待发送事件才能被确认成功，避免迟到的回调覆盖最终状态。 */
    @Update("""
            UPDATE product_search_outbox
            SET status = 1,
                sent_at = #{sentAt},
                last_error = NULL
            WHERE id = #{id}
              AND status = 0
            """)
    int markSent(@Param("id") Long id, @Param("sentAt") LocalDateTime sentAt);

    /** 已经确认成功的事件不会再被迟到的失败回调改写。 */
    @Update("""
            UPDATE product_search_outbox
            SET last_error = #{reason}
            WHERE id = #{id}
              AND status = 0
            """)
    int markFailure(@Param("id") Long id, @Param("reason") String reason);
}
