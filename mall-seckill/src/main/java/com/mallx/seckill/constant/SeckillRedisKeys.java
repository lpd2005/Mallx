package com.mallx.seckill.constant;

/** 秒杀 Redis Key 统一定义；库存只在 Redis 中原子操作，不进入 Caffeine。 */
public final class SeckillRedisKeys {
    /** 版本号放入前缀，后续结构不兼容时可以平滑切换新版本。 */
    private static final String PREFIX = "mallx:seckill:v1:";

    /** 按最近投递时间排序的待处理请求 ZSet。 */
    public static final String PENDING_REQUESTS = PREFIX + "pending-requests";
    /** 多实例恢复任务使用的 Redisson 锁。 */
    public static final String RECOVERY_LOCK = PREFIX + "lock:pending-recovery";

    /** 秒杀商品活动元数据 Hash。 */
    public static String item(Long itemId) {
        return PREFIX + "item:" + itemId;
    }

    /** 秒杀商品剩余资格库存 String。 */
    public static String stock(Long itemId) {
        return PREFIX + "stock:" + itemId;
    }

    /** 用户累计购买数量 Hash，field 为用户 ID。 */
    public static String purchased(Long itemId) {
        return PREFIX + "purchased:" + itemId;
    }

    /** 尚未形成最终结果的请求数量，用于关闭活动后安全释放剩余预留库存。 */
    public static String pendingCount(Long itemId) {
        return PREFIX + "pending-count:" + itemId;
    }

    /** 异步下单请求状态及下单参数 Hash。 */
    public static String request(String requestNo) {
        return PREFIX + "request:" + requestNo;
    }

    /** 串行处理同一个请求重复消息的细粒度 Redisson 锁。 */
    public static String requestLock(String requestNo) {
        return PREFIX + "lock:request:" + requestNo;
    }

    /** 防止多实例同时预热同一活动的 Redisson 锁。 */
    public static String warmupLock(Long activityId) {
        return PREFIX + "lock:warmup:" + activityId;
    }

    /** 常量类不允许实例化。 */
    private SeckillRedisKeys() {}
}
