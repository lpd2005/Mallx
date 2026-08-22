local key = KEYS[1]
local skuId = ARGV[1]
local addedQuantity = tonumber(ARGV[2])
local addedAt = tonumber(ARGV[3])
local maxQuantity = tonumber(ARGV[4])
local maxKinds = tonumber(ARGV[5])
local ttlSeconds = tonumber(ARGV[6])

local value = redis.call('HGET', key, skuId)
local item
if value then
    item = cjson.decode(value)
else
    if redis.call('HLEN', key) >= maxKinds then
        return -2
    end
    item = {quantity = 0, selected = true, addedAt = addedAt}
end

local newQuantity = tonumber(item.quantity) + addedQuantity
if newQuantity > maxQuantity then
    return -1
end

item.quantity = newQuantity
item.selected = true
redis.call('HSET', key, skuId, cjson.encode(item))
redis.call('EXPIRE', key, ttlSeconds)
return newQuantity
