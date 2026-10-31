-- Atomic scan: first authenticated phone locks QR to its user
if redis.call('EXISTS', KEYS[1]) == 0 then return 'EXPIRED' end
local state = redis.call('HGET', KEYS[1], 'status')
local owner = redis.call('HGET', KEYS[1], 'userId')
if state == 'WAIT_SCAN' then
  redis.call('HSET', KEYS[1], 'status', 'SCANNED', 'userId', ARGV[1])
  return 'SCANNED'
end
if state == 'SCANNED' and owner == ARGV[1] then return 'SCANNED' end
return 'FORBIDDEN'
