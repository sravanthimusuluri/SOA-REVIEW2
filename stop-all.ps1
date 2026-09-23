# ===================================================================
# BIBLIOTECH CIRCULATION PLATFORM - SHUTDOWN SCRIPT (POWERSHELL)
# ===================================================================

$ports = @(8761, 8080, 8081, 8082, 8083, 8084)

Write-Host "Stopping all Bibliotech Microservices..." -ForegroundColor Yellow

foreach ($port in $ports) {
    try {
        $netstat = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
        if ($netstat) {
            foreach ($conn in $netstat) {
                $pidToKill = $conn.OwningProcess
                if ($pidToKill -and $pidToKill -ne 0) {
                    Write-Host "Stopping process PID $pidToKill listening on port $port..." -ForegroundColor Cyan
                    Stop-Process -Id $pidToKill -Force -ErrorAction SilentlyContinue
                }
            }
        } else {
            Write-Host "Port $port is free." -ForegroundColor DarkGray
        }
    } catch {
        # ignore lookup failures
    }
}

Write-Host "All Bibliotech microservices have been terminated." -ForegroundColor Green
