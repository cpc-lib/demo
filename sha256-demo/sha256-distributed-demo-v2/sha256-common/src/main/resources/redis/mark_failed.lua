if redis.call('EXISTS', KEYS[1]) == 0 then
    return 0
end
local lockValue = redis.call('GET', KEYS[2])
if lockValue and lockValue ~= ARGV[1] then
    return 0
end
local status = redis.call('HGET', KEYS[1], 'status')
if status == 'SUCCESS' or status == 'DEAD_LETTERED' then
    return 0
end
redis.call('HSET', KEYS[1], 'status', 'FAILED', 'error', ARGV[2], 'updatedAt', ARGV[3])
redis.call('EXPIRE', KEYS[1], tonumber(ARGV[4]))
if lockValue == ARGV[1] then redis.call('DEL', KEYS[2]) end
return 1
