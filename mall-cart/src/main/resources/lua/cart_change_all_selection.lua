local key = KEYS[1]
local selected = ARGV[1] == '1'
local ttlSeconds = tonumber(ARGV[2])
local entries = redis.call('HGETALL', key)
local count = 0

for index = 1, #entries, 2 do
    local item = cjson.decode(entries[index + 1])
    item.selected = selected
    redis.call('HSET', key, entries[index], cjson.encode(item))
    count = count + 1
end

if count > 0 then
    redis.call('EXPIRE', key, ttlSeconds)
end
return count
