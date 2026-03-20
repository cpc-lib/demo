
local tokens_key = KEYS[1]
local ts_key     = KEYS[2]

local rate     = tonumber(ARGV[1] or "0")
local capacity = tonumber(ARGV[2] or "0")

if rate <= 0 then rate = 1 end
if capacity <= 0 then capacity = 1 end

local t = redis.call('TIME')
local now = t[1] * 1000 + math.floor(t[2] / 1000)

local tokens = tonumber(redis.call('get', tokens_key)) or capacity
local last   = tonumber(redis.call('get', ts_key))     or now

local interval = 1000 / rate
local delta    = now - last
if delta < 0 then delta = 0 end

local cycles = math.floor(delta / interval)
if cycles > 0 then
    tokens = math.min(capacity, tokens + cycles)
    last = last + cycles * interval
end

if tokens > 0 then
    tokens = tokens - 1
    redis.call('set', tokens_key, tokens)
    redis.call('set', ts_key, last)
    return 1
end

redis.call('set', tokens_key, tokens)
redis.call('set', ts_key, last)
return 0
