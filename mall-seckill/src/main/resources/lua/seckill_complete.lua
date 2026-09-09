-- 正式订单创建成功脚本。
-- KEYS[1] 请求状态Hash，KEYS[2] 待处理请求ZSet，KEYS[3] 商品待处理请求计数器。
-- ARGV[1] orderNo，ARGV[2] requestNo。
-- 返回：1表示本次完成状态转换，0表示请求已经是其他终态，保证重复消费幂等。

if redis.call('HGET', KEYS[1], 'status') ~= '0' then
    return 0
end

redis.call('HSET', KEYS[1], 'status', '1', 'orderNo', ARGV[1])
redis.call('ZREM', KEYS[2], ARGV[2])
local pending = redis.call('DECR', KEYS[3])
if pending <= 0 then
    redis.call('DEL', KEYS[3])
end
return 1
