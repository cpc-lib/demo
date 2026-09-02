if redis.call('GET', KEYS[2]) ~= ARGV[1] then
    return 0
end
if redis.call('HGET', KEYS[1], 'status') ~= 'RUNNING' then
    return 0
end
local total = redis.call('HGET', KEYS[1], 'totalBytes') or '0'
redis.call('HSET', KEYS[1],
    'status', 'SUCCESS',
    'processedBytes', total,
    'progress', '100',
    'sha256', ARGV[2],
    'error', '',
    'updatedAt', ARGV[3])
redis.call('EXPIRE', KEYS[1], tonumber(ARGV[4]))
redis.call('DEL', KEYS[2])
return 1
