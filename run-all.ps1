# ===================================================================
# BIBLIOTECH CIRCULATION PLATFORM - ONE-CLICK LAUNCH SCRIPT (POWERSHELL)
# Course: 24SDCS03A - SOA Programming & Microservices (Project PS021)
# ===================================================================

$ErrorActionPreference = "Stop"
$root = $PSScriptRoot

Write-Host ""
Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host "  BIBLIOTECH CIRCULATION SYSTEMS - MICROSERVICES PLATFORM LAUNCHER " -ForegroundColor Yellow
Write-Host "  Course: 24SDCS03A | Project PS021 | KL University CSE            " -ForegroundColor White
Write-Host "==================================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Build Maven JARs if target folders are missing
$eurekaJar = "$root\eureka-server\target\eureka-server-1.0.0.jar"
if (-not (Test-Path $eurekaJar)) {
    Write-Host "[1/7] Building multi-module Maven packages..." -ForegroundColor Yellow
    mvn clean package -DskipTests
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Maven build failed. Please resolve compilation issues." -ForegroundColor Red
        exit 1
    }
    Write-Host "Maven build completed successfully!" -ForegroundColor Green
} else {
    Write-Host "[1/7] Pre-compiled JARs detected. Skipping full rebuild." -ForegroundColor Green
}

# Helper to start service in separate console window
function Start-ServiceWindow($name, $dir, $port) {
    Write-Host "Launching $name on port $port..." -ForegroundColor Cyan
    $jarPath = "$root\$dir\target\$dir-1.0.0.jar"
    Start-Process -FilePath "java" -ArgumentList "-jar", "`"$jarPath`"" -WorkingDirectory "$root\$dir"
}

# 2. Launch Eureka Server (Port 8761)
Write-Host ""
Write-Host "[2/7] Starting Eureka Service Discovery (:8761)..." -ForegroundColor Yellow
Start-ServiceWindow "eureka-server" "eureka-server" 8761

Write-Host "Waiting 12 seconds for Eureka Server to initialize..." -ForegroundColor Gray
Start-Sleep -Seconds 12

# 3. Launch Core Domain Microservices
Write-Host ""
Write-Host "[3/7] Starting auth-service (:8081)..." -ForegroundColor Yellow
Start-ServiceWindow "auth-service" "auth-service" 8081

Write-Host "[4/7] Starting book-service (:8082)..." -ForegroundColor Yellow
Start-ServiceWindow "book-service" "book-service" 8082

Write-Host "[5/7] Starting fine-service (:8084)..." -ForegroundColor Yellow
Start-ServiceWindow "fine-service" "fine-service" 8084

Write-Host "Waiting 8 seconds for data services to bind..." -ForegroundColor Gray
Start-Sleep -Seconds 8

Write-Host "[6/7] Starting rental-service (:8083 - OpenFeign client)..." -ForegroundColor Yellow
Start-ServiceWindow "rental-service" "rental-service" 8083

# 4. Launch API Gateway (Port 8080)
Write-Host ""
Write-Host "[7/7] Starting api-gateway (:8080)..." -ForegroundColor Yellow
Start-ServiceWindow "api-gateway" "api-gateway" 8080

Write-Host "Waiting 10 seconds for Gateway and Eureka discovery heartbeat..." -ForegroundColor Gray
Start-Sleep -Seconds 10

# 5. Open Web Frontend Dashboard
$frontendPath = "$root\frontend\index.html"
Write-Host ""
Write-Host "Opening Bibliotech Frontend Dashboard in default browser..." -ForegroundColor Green
Start-Process $frontendPath

Write-Host ""
Write-Host "==================================================================" -ForegroundColor Green
Write-Host "  ALL BIBLIOTECH PLATFORM SERVICES ARE NOW RUNNING!               " -ForegroundColor Yellow
Write-Host "==================================================================" -ForegroundColor Green
Write-Host "  * Web Frontend UI:     $frontendPath" -ForegroundColor White
Write-Host "  * API Gateway:         http://localhost:8080" -ForegroundColor White
Write-Host "  * Eureka Dashboard:    http://localhost:8761" -ForegroundColor White
Write-Host "  * Auth Service:        http://localhost:8081" -ForegroundColor White
Write-Host "  * Book Service:        http://localhost:8082" -ForegroundColor White
Write-Host "  * Rental Service:      http://localhost:8083" -ForegroundColor White
Write-Host "  * Fine Service:        http://localhost:8084" -ForegroundColor White
Write-Host "------------------------------------------------------------------" -ForegroundColor Cyan
Write-Host "  DEMO TEST ACCOUNTS:                                             " -ForegroundColor White
Write-Host "  - Student 1: 2400030661 / pass123 (Musuluri Sravanthi - Lead)  " -ForegroundColor Gray
Write-Host "  - Student 2: 2400033191 / pass123 (Malisetty Naga Sai Nikitha) " -ForegroundColor Gray
Write-Host "  - Student 3: 2400033157 / pass123 (Karri Venkata Lakshmi Khyati)" -ForegroundColor Gray
Write-Host "  - Librarian: librarian  / lib123                                " -ForegroundColor Gray
Write-Host "  - Admin:     admin      / admin123                              " -ForegroundColor Gray
Write-Host "------------------------------------------------------------------" -ForegroundColor Cyan
Write-Host "  To shut down all services, run: .\stop-all.ps1                  " -ForegroundColor Yellow
Write-Host "==================================================================" -ForegroundColor Green
Write-Host ""
