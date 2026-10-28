param(
    [string]$BaseUrl = "http://localhost:8080"
)

$ErrorActionPreference = "Stop"
$base = $BaseUrl.TrimEnd("/")

Write-Host "Checking health at $base ..."
$health = Invoke-RestMethod -Uri "$base/actuator/health"
if ($health.status -ne "UP") {
    throw "Health check failed: $($health | ConvertTo-Json -Compress)"
}

Write-Host "Creating a demo task with an Idempotency-Key ..."
$body = @{ type = "DEMO"; payload = "hello from PowerShell smoke test" } | ConvertTo-Json
$idempotencyKey = "smoke-$([Guid]::NewGuid())"
$headers = @{ "Idempotency-Key" = $idempotencyKey }
$createdResponse = Invoke-WebRequest -Method Post -Uri "$base/api/tasks" -Headers $headers -ContentType "application/json" -Body $body
$created = $createdResponse.Content | ConvertFrom-Json
if ($created.status -ne "PENDING" -or -not $created.id) {
    throw "Task creation failed: $($created | ConvertTo-Json -Compress)"
}
if ($createdResponse.Headers["Idempotency-Replayed"] -ne "false") {
    throw "First idempotent request was unexpectedly replayed"
}

Write-Host "Replaying the same request ..."
$replayResponse = Invoke-WebRequest -Method Post -Uri "$base/api/tasks" -Headers $headers -ContentType "application/json" -Body $body
$replayed = $replayResponse.Content | ConvertFrom-Json
if ($replayResponse.Headers["Idempotency-Replayed"] -ne "true" -or $replayed.id -ne $created.id) {
    throw "Idempotency replay failed"
}

Write-Host "Reading task $($created.id) ..."
$task = Invoke-RestMethod -Uri "$base/api/tasks/$($created.id)"
if ($task.id -ne $created.id) {
    throw "Task read-back failed"
}

Write-Host "Smoke test passed. Task id: $($created.id)" -ForegroundColor Green
