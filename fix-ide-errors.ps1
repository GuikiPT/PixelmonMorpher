# IntelliJ IDEA Quick Fix Script for Pixelmon Dependency
# Run this script if you're seeing "Cannot resolve symbol" errors

Write-Host "=== Pixelmon Morpher - IntelliJ Fix Script ===" -ForegroundColor Cyan
Write-Host ""

# Check if Pixelmon JAR exists
Write-Host "1. Checking Pixelmon JAR..." -ForegroundColor Yellow
if (Test-Path "libs\Pixelmon-1.21.1-9.3.13-universal.jar") {
    $jarInfo = Get-Item "libs\Pixelmon-1.21.1-9.3.13-universal.jar"
    $sizeMB = [math]::Round($jarInfo.Length / 1MB, 2)
    Write-Host "   [OK] Found: $($jarInfo.Name) ($sizeMB MB)" -ForegroundColor Green
} else {
    Write-Host "   [ERROR] Pixelmon JAR not found in libs/ folder!" -ForegroundColor Red
    Write-Host "   Please download and place it in libs/ folder" -ForegroundColor Red
    exit 1
}

# Check if JAR is in run/mods
Write-Host ""
Write-Host "2. Checking runtime mods folder..." -ForegroundColor Yellow
if (Test-Path "run\mods\Pixelmon-1.21.1-9.3.13-universal.jar") {
    Write-Host "   [OK] Pixelmon JAR found in run/mods/" -ForegroundColor Green
} else {
    Write-Host "   [WARN] Pixelmon JAR not in run/mods/ - copying..." -ForegroundColor Yellow
    Copy-Item "libs\Pixelmon-1.21.1-9.3.13-universal.jar" "run\mods\" -Force
    Write-Host "   [OK] Copied to run/mods/" -ForegroundColor Green
}

# Stop Gradle daemon
Write-Host ""
Write-Host "3. Stopping Gradle daemon..." -ForegroundColor Yellow
$stopOutput = .\gradlew --stop 2>&1
Write-Host "   [OK] Gradle daemon stopped" -ForegroundColor Green

# Clean build
Write-Host ""
Write-Host "4. Running clean build (this may take a minute)..." -ForegroundColor Yellow
$buildOutput = .\gradlew clean build --refresh-dependencies --quiet
if ($LASTEXITCODE -eq 0) {
    Write-Host "   [OK] Build successful!" -ForegroundColor Green
} else {
    Write-Host "   [ERROR] Build failed! Check output above." -ForegroundColor Red
    exit 1
}

# Instructions for IntelliJ
Write-Host ""
Write-Host "=== Next Steps in IntelliJ IDEA ===" -ForegroundColor Cyan
Write-Host ""
Write-Host "Now do ONE of the following:" -ForegroundColor Yellow
Write-Host ""
Write-Host "Option 1 (Recommended):" -ForegroundColor White
Write-Host "  1. Open Gradle tool window (right sidebar)" -ForegroundColor Gray
Write-Host "  2. Click the refresh button at the top" -ForegroundColor Gray
Write-Host "  3. Wait for indexing to complete" -ForegroundColor Gray
Write-Host ""
Write-Host "Option 2 (If Option 1 doesn't work):" -ForegroundColor White
Write-Host "  1. File -> Invalidate Caches..." -ForegroundColor Gray
Write-Host "  2. Check all boxes" -ForegroundColor Gray
Write-Host "  3. Click 'Invalidate and Restart'" -ForegroundColor Gray
Write-Host "  4. Wait for IntelliJ to restart and re-index" -ForegroundColor Gray
Write-Host ""
Write-Host "After this, all 'Cannot resolve symbol' errors should be gone!" -ForegroundColor Green
Write-Host ""
Write-Host "=== Script Complete ===" -ForegroundColor Cyan
