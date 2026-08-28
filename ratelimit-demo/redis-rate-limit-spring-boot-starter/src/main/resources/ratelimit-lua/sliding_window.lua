-- Sliding Window Log with ZSET
-- KEYS[1] = key
-- ARGV[1] = limit
-- ARGV[2] = windowMillis
-- ARGV[3] = nowMillis
local key = KEYS[1]
local limit = tonumber(ARGV[1])
local windowMillis = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local start = now - windowMillis
redis.call('ZREMRANGEBYSCORE', key, 0, start)
local cnt = redis.call('ZCARD', key)

if cnt < limit then
  redis.call('ZADD', key, now, tostring(now))
  redis.call('PEXPIRE', key, windowMillis + 1000)
  local remaining = limit - (cnt + 1)
  return {1, remaining, windowMillis}
else
  local oldest = redis.call('ZRANGE', key, 0, 0, 'WITHSCORES')
  local reset = windowMillis
  if oldest ~= false and oldest[2] ~= nil then
    reset = (tonumber(oldest[2]) + windowMillis) - now
    if reset < 0 then reset = 0 end
  end
  return {0, 0, reset}
end
