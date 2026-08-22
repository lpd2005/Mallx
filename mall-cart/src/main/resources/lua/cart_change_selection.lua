local key = KEYS[1]
local skuId = ARGV[1]
local selected = ARGV[2] == '1'
local ttlSeconds = tonumber(ARGV[3])

local value = redis.call('HGET', key, skuId)
if not value then
    return -3
end

local item = cjson.decode(value)
item.selected = selected
redis.call('HSET', key, skuId, cjson.encode(item))
redis.call('EXPIRE', key, ttlSeconds)
return 1
