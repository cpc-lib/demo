if redis.call('GET', KEYS[2]) ~= ARGV[1] then
    return 0
end
if redis.call('HGET', KEYS[1], 'status') ~= 'RUNNING' then
    return 0
end
local oldProcessed = tonumber(redis.call('HGET', KEYS[1], 'processedBytes') or '0')
local newProcessed = tonumber(ARGV[2])
if newProcessed < oldProcessed then
    return 0
end
local oldProgress = tonumber(redis.call('HGET', KEYS[1], 'progress') or '0')
local newProgress = tonumber(ARGV[3])
if newProgress < oldProgress then
    newProgress = oldProgress
end
redis.call('HSET', KEYS[1], 'processedBytes', tostring(newProcessed), 'progress', tostring(newProgress), 'updatedAt', ARGV[4])
redis.call('EXPIRE', KEYS[1], tonumber(ARGV[5]))
redis.call('PEXPIRE', KEYS[2], tonumber(ARGV[6]))
return 1
