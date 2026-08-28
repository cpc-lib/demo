-- Fixed Window Counter
-- KEYS[1] = key
-- ARGV[1] = limit
-- ARGV[2] = windowMillis
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local windowMillis = tonumber(ARGV[2])

local current = redis.call('INCR', key)
if current == 1 then
  redis.call('PEXPIRE', key, windowMillis)
end

local ttl = redis.call('PTTL', key)
if ttl < 0 then ttl = windowMillis end

if current <= limit then
  return {1, limit - current, ttl}
else
  return {0, 0, ttl}
end
