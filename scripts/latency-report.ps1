# Per-route latency report from the API gateway's access log.
#
#   .\scripts\latency-report.ps1             # last 15 minutes
#   .\scripts\latency-report.ps1 -Since 1h
#
# Click through the app first (sign in, Home, a title, play, search, account), then run it.
# Ids and slugs are folded so /titles/signal-house and /titles/afterlight count as one route.

param([string]$Since = "15m")

$pattern = '"message":"(GET|POST|PUT|DELETE|PATCH) (\S+) -> (\d+|-) in (\d+) ms'

$rows = docker compose logs api-gateway --since $Since --no-log-prefix | ForEach-Object {
    if ($_ -match $pattern) {
        $route = $Matches[2]
        $route = $route -replace '/[0-9a-fA-F-]{36}(?=/|$)', '/{uuid}'
        $route = $route -replace '/\d+(?=/|$)', '/{id}'
        $route = $route -replace '/catalog/titles/(?!by-id)[^/]+', '/catalog/titles/{slug}'
        [pscustomobject]@{ Route = "$($Matches[1]) $route"; Status = $Matches[3]; Ms = [int]$Matches[4] }
    }
}

if (-not $rows) {
    Write-Host "No gateway requests in the last $Since. Use the app, then run this again."
    exit 0
}

$report = $rows | Group-Object Route | ForEach-Object {
    $ms = @($_.Group | ForEach-Object { $_.Ms } | Sort-Object)
    $p50 = $ms[[math]::Max(0, [math]::Ceiling($ms.Count * 0.50) - 1)]
    $p95 = $ms[[math]::Max(0, [math]::Ceiling($ms.Count * 0.95) - 1)]
    $errors = @($_.Group | Where-Object { $_.Status -match '^5' }).Count
    [pscustomobject]@{
        Route  = $_.Name
        Count  = $ms.Count
        P50ms  = $p50
        P95ms  = $p95
        MaxMs  = $ms[$ms.Count - 1]
        Errors = $errors
    }
}

$report | Sort-Object P95ms -Descending | Format-Table -AutoSize
Write-Host "P50 = typical request, P95 = slowest 5%. On a warmed-up local stack most routes should be well under 100 ms."
Write-Host "The first requests after a rebuild are slow while the JVMs warm up; run the report again after a minute of use."
