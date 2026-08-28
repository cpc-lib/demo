-- Token Bucket (HASH: tokens, ts)
-- KEYS[1] = key
-- ARGV[1] = capacity
-- ARGV[2] = refillPerSecond
-- ARGV[3] = nowMillis
-- ARGV[4] = requestedTokens
local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refillPerSecond = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local req = tonumber(ARGV[4]) or 1

local data = redis.call('HMGET', key, 'tokens', 'ts')
local tokens = tonumber(data[1])
local ts = tonumber(data[2])

if tokens == nil then tokens = capacity end
if ts == nil then ts = now end

local deltaMillis = now - ts
if deltaMillis < 0 then deltaMillis = 0 end

local refill = (deltaMillis / 1000.0) * refillPerSecond
tokens = math.min(capacity, tokens + refill)

local allowed = 0
if tokens >= req then
  tokens = tokens - req
  allowed = 1
end

redis.call('HMSET', key, 'tokens', tokens, 'ts', now)
redis.call('PEXPIRE', key, math.floor((capacity / refillPerSecond) * 1000) + 5000)

local remaining = math.floor(tokens)
local reset = 0
if tokens < 1 and refillPerSecond > 0 then
  reset = math.floor(((1 - tokens) / refillPerSecond) * 1000)
end

return {allowed, remaining, reset}
