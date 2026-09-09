-- 确定性业务失败补偿脚本。
-- KEYS[1] 请求状态Hash，KEYS[2] 资格库存String，KEYS[3] 用户累计购买Hash。
-- KEYS[4] 待处理请求ZSet，KEYS[5] 商品待处理请求计数器。
-- ARGV[1] quantity，ARGV[2] userId，ARGV[3] failReason，ARGV[4] requestNo。
-- 返回：1表示本次完成补偿，0表示请求不再是处理中，防止重复归还库存。

if redis.call('HGET', KEYS[1], 'status') ~= '0' then
    return 0
end

-- 库存和用户限购计数必须同时恢复。
local quantity = tonumber(ARGV[1])
redis.call('INCRBY', KEYS[2], quantity)
local remaining = redis.call('HINCRBY', KEYS[3], ARGV[2], -quantity)
if remaining <= 0 then
    redis.call('HDEL', KEYS[3], ARGV[2])
end
redis.call('HSET', KEYS[1], 'status', '2', 'failReason', ARGV[3])
redis.call('ZREM', KEYS[4], ARGV[4])
local pending = redis.call('DECR', KEYS[5])
if pending <= 0 then
    redis.call('DEL', KEYS[5])
end
return 1
