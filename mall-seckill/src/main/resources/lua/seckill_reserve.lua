-- 秒杀资格预扣脚本。
-- KEYS[1] 商品元数据Hash，KEYS[2] 资格库存String，KEYS[3] 用户累计购买Hash。
-- KEYS[4] 请求状态Hash，KEYS[5] 待处理请求ZSet。
-- KEYS[6] 当前秒杀商品的待处理请求计数器。
-- ARGV[1] userId，ARGV[2] quantity，ARGV[3] addressId，ARGV[4] itemId。
-- ARGV[5] 当前毫秒时间戳，ARGV[6] 请求TTL毫秒，ARGV[7] requestNo。
-- 返回码：0成功，1商品不存在/未开放，2未开始，3已结束，4售罄，5超限购，6请求重复。

-- 校验活动元数据以及时间窗口。
if redis.call('EXISTS', KEYS[1]) == 0 then
    return 1
end
if redis.call('HGET', KEYS[1], 'status') ~= '1' then
    return 1
end

local now = tonumber(ARGV[5])
if now < tonumber(redis.call('HGET', KEYS[1], 'startAt')) then
    return 2
end
if now >= tonumber(redis.call('HGET', KEYS[1], 'endAt')) then
    return 3
end
-- requestNo 已经登记时拒绝重复执行，避免同一请求重复扣减。
if redis.call('EXISTS', KEYS[4]) == 1 then
    return 6
end

-- 用户累计购买量和本次数量合并校验，实现跨请求限购。
local quantity = tonumber(ARGV[2])
local bought = tonumber(redis.call('HGET', KEYS[3], ARGV[1]) or '0')
local limit = tonumber(redis.call('HGET', KEYS[1], 'limitPerUser'))
if bought + quantity > limit then
    return 5
end

-- 缺失的库存Key视为不可售，不从数据库临时回源。
local stock = tonumber(redis.call('GET', KEYS[2]) or '-1')
if stock < quantity then
    return 4
end

-- 从这里开始一次性提交资格扣减、限购计数、请求快照和恢复索引。
redis.call('DECRBY', KEYS[2], quantity)
redis.call('HINCRBY', KEYS[3], ARGV[1], quantity)
redis.call('PEXPIRE', KEYS[3], ARGV[6])
redis.call('HSET', KEYS[4],
    'status', '0',
    'userId', ARGV[1],
    'quantity', ARGV[2],
    'addressId', ARGV[3],
    'activityId', redis.call('HGET', KEYS[1], 'activityId'),
    'itemId', ARGV[4],
    'skuId', redis.call('HGET', KEYS[1], 'skuId'),
    'seckillPrice', redis.call('HGET', KEYS[1], 'seckillPrice'),
    'dispatchAttempts', '0')
redis.call('PEXPIRE', KEYS[4], ARGV[6])
redis.call('ZADD', KEYS[5], now, ARGV[7])
redis.call('INCR', KEYS[6])
redis.call('PEXPIRE', KEYS[6], ARGV[6])
return 0
