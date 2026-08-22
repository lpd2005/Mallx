local key = KEYS[1]
local ttlSeconds = tonumber(ARGV[1])
local entries = redis.call('HGETALL', key)
local removed = 0

for index = 1, #entries, 2 do
    local item = cjson.decode(entries[index + 1])
    if item.selected == true then
        redis.call('HDEL', key, entries[index])
        removed = removed + 1
    end
end

if redis.call('HLEN', key) == 0 then
    redis.call('DEL', key)
else
    redis.call('EXPIRE', key, ttlSeconds)
end
return removed
