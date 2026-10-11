param(
    [string]$Destination
)

$ErrorActionPreference = "Stop"

$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$AppGradle = Join-Path $Root "app\build.gradle.kts"

if (-not $Destination) {
    $Destination = Join-Path $Root "vex-screen-crawler"
}

if (-not (Get-Command adb -ErrorAction SilentlyContinue)) {
    throw "adb is not on PATH."
}

$GradleText = Get-Content -Raw $AppGradle
$PackageMatch = [regex]::Match(
    $GradleText,
    'applicationId\s*=\s*"([^"]+)"'
)

if (-not $PackageMatch.Success) {
    throw "Could not derive applicationId from $AppGradle"
}

$PackageId = $PackageMatch.Groups[1].Value
$Remote = "/sdcard/Android/data/$PackageId/files/vex-screen-crawler"
$Serial = $env:ANDROID_SERIAL

if (-not $Serial) {
    $Devices = @(
        (& adb devices) |
            Select-Object -Skip 1 |
            ForEach-Object {
                if ($_ -match '^(\S+)\s+device$') {
                    $Matches[1]
                }
            }
    )

    if ($Devices.Count -eq 0) {
        throw "No connected adb device/emulator is ready."
    }

    if ($Devices.Count -gt 1) {
        Write-Host "Multiple adb targets are connected:"
        $Devices | ForEach-Object { Write-Host "  $_" }
        Write-Host ""
        Write-Host "Choose the target once, then rerun:"
        Write-Host '  $env:ANDROID_SERIAL = "<serial>"'
        Write-Host '  .\tools\pull-vex-screen-crawler.ps1'
        exit 2
    }

    $Serial = $Devices[0]
}

& adb -s $Serial shell "test -d '$Remote'" | Out-Null
if ($LASTEXITCODE -ne 0) {
    throw @"
No ScreenCrawler artifacts found on $Serial at:
  $Remote

Run VexScreenCrawlerInstrumentedTest directly first; an explicit crawler run must produce screenshot evidence.
"@
}

New-Item -ItemType Directory -Force -Path $Destination | Out-Null

Write-Host "Pulling ScreenCrawler evidence"
Write-Host "  device: $Serial"
Write-Host "  from:   $Remote"
Write-Host "  to:     $Destination"

& adb -s $Serial pull "$Remote/." $Destination
if ($LASTEXITCODE -ne 0) {
    throw "adb pull failed."
}

Write-Host ""
Write-Host "Done: $Destination"
