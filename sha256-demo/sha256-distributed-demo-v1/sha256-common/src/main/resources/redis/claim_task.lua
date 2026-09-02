if redis.call('EXISTS', KEYS[1]) == 0 then
    return -2
end
local status = redis.call('HGET', KEYS[1], 'status')
if status == 'SUCCESS' or status == 'FAILED' or status == 'DEAD_LETTERED' then
    return -3
end
local locked = redis.call('SET', KEYS[2], ARGV[1], 'NX', 'PX', tonumber(ARGV[2]))
if not locked then
    return 0
end
redis.call('HSET', KEYS[1], 'status', 'RUNNING', 'error', '', 'updatedAt', ARGV[3])
redis.call('EXPIRE', KEYS[1], tonumber(ARGV[4]))
return 1
