local key = KEYS[1]
local skuId = ARGV[1]
local quantity = tonumber(ARGV[2])
local maxQuantity = tonumber(ARGV[3])
local ttlSeconds = tonumber(ARGV[4])

local value = redis.call('HGET', key, skuId)
if not value then
    return -3
end
if quantity > maxQuantity then
    return -1
end

local item = cjson.decode(value)
item.quantity = quantity
redis.call('HSET', key, skuId, cjson.encode(item))
redis.call('EXPIRE', key, ttlSeconds)
return quantity
