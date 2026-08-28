$ErrorActionPreference = 'Stop'
docker compose up -d --build
Write-Host 'Frontend: http://localhost:3000'
Write-Host 'Backend : http://localhost:8088/api/dashboard'
Write-Host 'XXL-JOB : http://localhost:8080 (admin / 123456)'
Write-Host 'Create handler dailyPointRewardJob; route=SHARDING_BROADCAST; block=SERIAL_EXECUTION; cron=0 0 1 * * ?'
