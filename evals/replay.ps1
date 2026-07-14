# Sends the eval questions to the running app to generate traces for online evals.
# Usage:
#   .\evals\replay.ps1                  # send all questions
#   .\evals\replay.ps1 -Ids Q01,Q13     # send a subset
#   .\evals\replay.ps1 -DelaySeconds 5  # slow down for Bedrock rate limits
param(
    [string]$BaseUrl = "http://localhost:8080",
    [string[]]$Ids,
    [int]$DelaySeconds = 2
)

$dataset = Get-Content -Raw (Join-Path $PSScriptRoot "dataset.json") | ConvertFrom-Json
$questions = @($dataset.questions)
if ($Ids) { $questions = @($questions | Where-Object { $Ids -contains $_.id }) }

Write-Host "Sending $($questions.Count) question(s) to $BaseUrl/api/chat`n"

foreach ($q in $questions) {
    Write-Host "[$($q.id)] ($($q.category)) $($q.question)" -ForegroundColor Cyan
    try {
        $body = $q.question | ConvertTo-Json  # the endpoint expects a JSON-encoded string, same as the UI sends
        $response = Invoke-RestMethod -Uri "$BaseUrl/api/chat" -Method Post -ContentType "application/json" -Body $body
        $preview = ($response -replace "`r?`n", " ")
        if ($preview.Length -gt 160) { $preview = $preview.Substring(0, 160) + "..." }
        Write-Host "  -> $preview`n" -ForegroundColor Gray
    } catch {
        Write-Host "  -> FAILED: $($_.Exception.Message)`n" -ForegroundColor Red
    }
    if ($DelaySeconds -gt 0) { Start-Sleep -Seconds $DelaySeconds }
}

Write-Host "Done. Traces should now be visible in Phoenix (localhost:6006), Langfuse, and LangSmith."
