-- KEYS[1]=qr key; ARGV: binding hash, verification code, ttl seconds
redis.call('HSET', KEYS[1],
  'status', 'WAIT_SCAN', 'binding', ARGV[1], 'code', ARGV[2], 'userId', '')
redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3]))
return 'CREATED'
