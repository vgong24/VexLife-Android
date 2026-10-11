param(
    [string]$Destination
)

$ErrorActionPreference = "Stop"

$Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$BuildRoot = Join-Path $Root "app\build"

if (-not $Destination) {
    $Destination = Join-Path $Root "vex-screen-crawler"
}

if (-not (Test-Path $BuildRoot)) {
    throw @"
app/build does not exist yet.
Run VexScreenCrawlerInstrumentedTest first.
"@
}

$Sources = @(
    Get-ChildItem -Path $BuildRoot -Directory -Recurse -Filter "vex-screen-crawler" -ErrorAction SilentlyContinue |
        Select-Object -ExpandProperty FullName
)

if ($Sources.Count -eq 0) {
    throw @"
No returned ScreenCrawler outputs found under:
  $BuildRoot

Run VexScreenCrawlerInstrumentedTest, then filter Logcat with:
  tag:VexScreenCrawler
and confirm COMPLETE ... screenshots=<n>.
"@
}

New-Item -ItemType Directory -Force -Path $Destination | Out-Null

foreach ($Source in $Sources) {
    Write-Host "Collecting ScreenCrawler output:"
    Write-Host "  $Source"
    Copy-Item -Recurse -Force (Join-Path $Source "*") $Destination
}

Write-Host ""
Write-Host "Done: $Destination"
Write-Host "Manifests:"
Get-ChildItem -Path $Destination -Recurse -Filter "manifest.jsonl" |
    ForEach-Object { Write-Host "  $($_.FullName)" }
