if redis.call('EXISTS', KEYS[1]) == 1 then
    return 0
end
redis.call('HSET', KEYS[1],
    'taskId', ARGV[1],
    'originalFilename', ARGV[2],
    'storageKey', ARGV[3],
    'storageBucket', ARGV[4],
    'totalBytes', ARGV[5],
    'processedBytes', '0',
    'progress', '0',
    'status', 'QUEUED',
    'sha256', '',
    'error', '',
    'broker', ARGV[6],
    'retryCount', '0',
    'createdAt', ARGV[7],
    'updatedAt', ARGV[7])
redis.call('EXPIRE', KEYS[1], tonumber(ARGV[8]))
return 1
