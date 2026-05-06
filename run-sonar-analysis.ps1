$SONAR_HOST = "http://localhost:9000"
$SONAR_TOKEN = $env:SONAR_TOKEN
$ROOT = "D:\ConnectHub-Backend"

if (-not $SONAR_TOKEN) {
    Write-Host "ERROR: SONAR_TOKEN is not set. Run: `$env:SONAR_TOKEN = 'your_token'" -ForegroundColor Red
    exit 1
}

$SERVICES = @("eureka-service","auth-service","room-service","message-service","media-service","presence-service","notification-service","websocket-handler","website-controller","api-gateway")

$pass = 0
$fail = 0
$skip = 0

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " ConnectHub SonarQube Analysis" -ForegroundColor Cyan
Write-Host " Host: $SONAR_HOST" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan

foreach ($svc in $SERVICES) {
    $svcDir = Join-Path $ROOT $svc
    Write-Host ""
    Write-Host ">> Analysing: $svc" -ForegroundColor Yellow

    if (-not (Test-Path $svcDir)) {
        Write-Host "   SKIPPED - directory not found" -ForegroundColor DarkYellow
        $skip++
        continue
    }

    if (-not (Test-Path (Join-Path $svcDir "pom.xml"))) {
        Write-Host "   SKIPPED - no pom.xml found" -ForegroundColor DarkYellow
        $skip++
        continue
    }

    Push-Location $svcDir

    Write-Host "   [1/2] Running tests and coverage..." -ForegroundColor Gray
    mvn clean verify "-Dmaven.test.failure.ignore=true" -q

    Write-Host "   [2/2] Sending to SonarQube..." -ForegroundColor Gray
    mvn sonar:sonar "-Dsonar.host.url=$SONAR_HOST" "-Dsonar.token=$SONAR_TOKEN" -q

    if ($LASTEXITCODE -eq 0) {
        $key = if ($svc -eq "website-controller") { "com.connecthub:connecthub-web" } else { "com.connecthub:$svc" }
        Write-Host "   OK - $SONAR_HOST/dashboard?id=$key" -ForegroundColor Green
        $pass++
    } else {
        Write-Host "   FAILED" -ForegroundColor Red
        $fail++
    }

    Pop-Location
}

Write-Host ""
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host " Done: $pass passed, $fail failed, $skip skipped" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
