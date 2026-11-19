# Generate and persist a random secret.
# Returns the existing value when the file already exists, otherwise creates it.
# Used by deploy\start-public.bat to create JWT_SECRET; the file lives under
# .secrets\ which is git-ignored.
param(
    [Parameter(Mandatory = $true)][string]$Path,
    [int]$Length = 48
)

$ErrorActionPreference = 'Stop'

if (Test-Path -LiteralPath $Path) {
    $existing = (Get-Content -LiteralPath $Path -Raw -ErrorAction Stop).Trim()
    if ($existing) {
        Write-Output $existing
        return
    }
}

$dir = Split-Path -Parent $Path
if ($dir -and -not (Test-Path -LiteralPath $dir)) {
    New-Item -ItemType Directory -Path $dir -Force | Out-Null
}

$bytes = New-Object byte[] $Length
$rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$rng.GetBytes($bytes)
$secret = [Convert]::ToBase64String($bytes) -replace '[+/=]', ''
if ($secret.Length -gt $Length) {
    $secret = $secret.Substring(0, $Length)
}

Set-Content -LiteralPath $Path -Value $secret -NoNewline -Encoding ascii
Write-Output $secret
