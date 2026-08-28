$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080/api/articles"
$createBody = @{
  title = "Redis + Kafka delayed publish smoke test"
  content = "This article should be published automatically."
} | ConvertTo-Json

$article = Invoke-RestMethod -Method Post -Uri $baseUrl -ContentType "application/json" -Body $createBody
Write-Host "Created article id=$($article.id), status=$($article.status)"

$publishAt = [DateTimeOffset]::UtcNow.AddSeconds(15).ToString("o")
$scheduleBody = @{ publishAt = $publishAt } | ConvertTo-Json
$scheduled = Invoke-RestMethod -Method Post -Uri "$baseUrl/$($article.id)/schedule" -ContentType "application/json" -Body $scheduleBody
Write-Host "Scheduled at $publishAt, version=$($scheduled.scheduleVersion)"

$deadline = [DateTimeOffset]::UtcNow.AddSeconds(45)
do {
  Start-Sleep -Seconds 2
  $current = Invoke-RestMethod -Method Get -Uri "$baseUrl/$($article.id)"
  Write-Host "Current status=$($current.status)"
  if ($current.status -eq "PUBLISHED") {
    Write-Host "SMOKE TEST PASSED"
    exit 0
  }
} while ([DateTimeOffset]::UtcNow -lt $deadline)

throw "SMOKE TEST FAILED: article was not published before timeout"
