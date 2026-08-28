$BaseUrl = if ($env:BASE_URL) { $env:BASE_URL } else { "http://127.0.0.1:8080" }

Write-Host "== Unsafe baseline =="
Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/demo/concurrent-unsafe?productId=1001&initialStock=20&requests=20" | ConvertTo-Json

foreach ($provider in @("redisson", "zookeeper", "mysql")) {
    Write-Host "== $provider =="
    Invoke-RestMethod -Method Post -Uri "$BaseUrl/api/demo/concurrent/$provider?productId=1001&initialStock=20&requests=20" | ConvertTo-Json
}
