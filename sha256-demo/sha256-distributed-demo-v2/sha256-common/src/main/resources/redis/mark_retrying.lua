if redis.call('GET', KEYS[2]) ~= ARGV[1] then
    return 0
end
local status = redis.call('HGET', KEYS[1], 'status')
if status == 'SUCCESS' or status == 'FAILED' or status == 'DEAD_LETTERED' then
    redis.call('DEL', KEYS[2])
    return 0
end
local retryCount = tonumber(redis.call('HGET', KEYS[1], 'retryCount') or '0') + 1
redis.call('HSET', KEYS[1],
    'status', 'RETRYING',
    'retryCount', tostring(retryCount),
    'error', ARGV[2],
    'updatedAt', ARGV[3])
redis.call('EXPIRE', KEYS[1], tonumber(ARGV[4]))
redis.call('DEL', KEYS[2])
return retryCount
