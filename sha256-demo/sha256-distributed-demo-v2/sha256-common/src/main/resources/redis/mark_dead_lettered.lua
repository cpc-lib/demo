if redis.call('EXISTS', KEYS[1]) == 0 then
    return 0
end
local status = redis.call('HGET', KEYS[1], 'status')
if status == 'SUCCESS' then
    return 0
end
redis.call('HSET', KEYS[1], 'status', 'DEAD_LETTERED', 'error', ARGV[1], 'updatedAt', ARGV[2])
redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3]))
redis.call('DEL', KEYS[2])
return 1
