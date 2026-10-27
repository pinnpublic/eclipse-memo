[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$EclipseHome,
    [string]$JdkHome = $env:JAVA_HOME
)
$ErrorActionPreference = 'Stop'
$repoRoot = $PSScriptRoot
$manifest = Get-Content -LiteralPath (Join-Path $repoRoot 'META-INF\MANIFEST.MF') -Encoding UTF8 -Raw
$versionMatch = [regex]::Match($manifest, '(?m)^Bundle-Version: (\d+\.\d+\.\d+)\r?$')
if (!$versionMatch.Success) { throw 'Expected a numeric major.minor.patch Bundle-Version.' }
$version = $versionMatch.Groups[1].Value

& (Join-Path $repoRoot 'build.ps1') -EclipseHome $EclipseHome -JdkHome $JdkHome
$marketplaceOutput = Join-Path $repoRoot 'dist\marketplace'
& (Join-Path $repoRoot 'build-update-site.ps1') -EclipseHome $EclipseHome -JdkHome $JdkHome -OutputDirectory $marketplaceOutput

$releaseDirectory = Join-Path $repoRoot ('dist\release\' + $version)
New-Item -ItemType Directory -Force -Path $releaseDirectory | Out-Null
$releaseFiles = @(
    (Join-Path $repoRoot "dist\dev.eclipsememo_$version.jar"),
    (Join-Path $repoRoot "dist\dev.eclipsememo_$version.jar.sha256"),
    (Join-Path $repoRoot "dist\eclipse-workspace-memo-$version.zip"),
    (Join-Path $marketplaceOutput "eclipse-workspace-memo-update-site-$version.zip")
)
$checksumLines = foreach ($releaseFile in $releaseFiles) {
    Copy-Item -LiteralPath $releaseFile -Destination $releaseDirectory -Force
    $fileName = Split-Path -Leaf $releaseFile
    $hash = (Get-FileHash -LiteralPath (Join-Path $releaseDirectory $fileName) -Algorithm SHA256).Hash.ToLowerInvariant()
    "$hash  $fileName"
}
[IO.File]::WriteAllLines((Join-Path $releaseDirectory 'SHA256SUMS'), $checksumLines, [Text.UTF8Encoding]::new($false))
Write-Output "Release v$version is ready in $releaseDirectory"
Write-Output 'Upload the files in that folder as assets of the matching GitHub Release.'
