param(
  [Switch]$Build,
  [Parameter(ValueFromRemainingArguments = $true)]
  [string[]]$ExtraArgs
)

function Test-Engine {
  docker info *> $null
  return $LASTEXITCODE -eq 0
}

if (-not (Test-Engine)) {
  $docker = 'C:\Program Files\Docker\Docker\Docker Desktop.exe'
  if ((Test-Path $docker) -and -not (Get-Process -Name 'Docker Desktop' -ErrorAction SilentlyContinue)) {
    Start-Process -FilePath $docker | Out-Null
    Write-Host 'Starting Docker Desktop...'
  }
  for ($i = 0; $i -lt 24; $i++) {
    Start-Sleep -Seconds 5
    if (Test-Engine) { break }
  }
  if (-not (Test-Engine)) {
    Write-Error 'Docker engine did not become ready. Start Docker Desktop manually and retry.'
    exit 1
  }
  Write-Host 'Docker engine ready.'
}

$argsList = @()
if ($Build) { $argsList += '--build' }
$argsList += $ExtraArgs
& docker compose up @argsList
exit $LASTEXITCODE