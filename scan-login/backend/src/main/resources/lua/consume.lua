-- Return distinct success and replay markers; only the first consumer succeeds.
if redis.call('EXISTS', KEYS[1]) == 0 then return 'EXPIRED' end
local state = redis.call('HGET', KEYS[1], 'status')
if redis.call('HGET', KEYS[1], 'binding') ~= ARGV[1] then return 'FORBIDDEN' end
if state == 'CONSUMED' then return 'ALREADY_CONSUMED' end
if state ~= 'CONFIRMED' then return state or 'INVALID' end
if redis.call('HGET', KEYS[1], 'userId') ~= ARGV[2] then return 'FORBIDDEN' end
redis.call('HSET', KEYS[1], 'status', 'CONSUMED')
return 'CONSUMED'
