-- Only scanning mobile user may confirm, once; consumed cannot confirm
if redis.call('EXISTS', KEYS[1]) == 0 then return 'EXPIRED' end
local state = redis.call('HGET', KEYS[1], 'status')
local owner = redis.call('HGET', KEYS[1], 'userId')
if owner ~= ARGV[1] then return 'FORBIDDEN' end
if state == 'SCANNED' then
  redis.call('HSET', KEYS[1], 'status', 'CONFIRMED')
  return 'CONFIRMED'
end
if state == 'CONFIRMED' then return 'CONFIRMED' end
return 'FORBIDDEN'
