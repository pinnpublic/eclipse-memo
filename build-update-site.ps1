[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$EclipseHome,
    [string]$JdkHome = $env:JAVA_HOME,
    [string]$OutputDirectory = (Join-Path $PSScriptRoot 'dist')
)
$ErrorActionPreference = 'Stop'
$repoRoot = $PSScriptRoot
$OutputDirectory = [IO.Path]::GetFullPath($OutputDirectory)
New-Item -ItemType Directory -Force -Path $OutputDirectory | Out-Null
$javaTool = if ($JdkHome) { Join-Path $JdkHome 'bin\java.exe' } else { (Get-Command java).Source }
$jarTool = Join-Path (Split-Path $javaTool) 'jar.exe'
$plugins = Join-Path $EclipseHome 'plugins'
$mainJar = Join-Path $repoRoot 'dist\dev.eclipsememo_1.0.1.jar'
if (!(Test-Path -LiteralPath $mainJar)) { throw 'Run build.ps1 first.' }
$work = Join-Path $repoRoot ('.build\p2-' + [Guid]::NewGuid().ToString('N'))
$stage = Join-Path $work 'stage'
$repository = Join-Path $work 'repository'
$configuration = Join-Path $work 'configuration'
New-Item -ItemType Directory -Force -Path (Join-Path $stage 'plugins'), (Join-Path $stage 'features'), $repository, $configuration | Out-Null
Copy-Item -LiteralPath $mainJar -Destination (Join-Path $stage 'plugins')
$featureJar = Join-Path $stage 'features\dev.eclipsememo.feature_1.0.1.jar'
& $jarTool --create --file $featureJar -C (Join-Path $repoRoot 'feature') feature.xml -C $repoRoot LICENSE
if ($LASTEXITCODE -ne 0) { throw 'Feature packaging failed.' }

function ConvertTo-FileUri([string]$Path) { [Uri]::new([IO.Path]::GetFullPath($Path)).AbsoluteUri }
$bundleInfo = Join-Path $configuration 'bundles.info'
$bundles = Get-Content -LiteralPath (Join-Path $EclipseHome 'configuration\org.eclipse.equinox.simpleconfigurator\bundles.info') -Encoding UTF8 |
    ForEach-Object {
        if ($_.StartsWith('#') -or !$_.Trim()) { $_ }
        else {
            $fields = $_ -split ','
            if (!($fields[2] -match '^[a-z]+:')) { $fields[2] = ConvertTo-FileUri (Join-Path $EclipseHome $fields[2]) }
            $fields -join ','
        }
    }
[IO.File]::WriteAllLines($bundleInfo, $bundles, [Text.UTF8Encoding]::new($false))
$framework = Get-ChildItem -LiteralPath $plugins -Filter 'org.eclipse.osgi_*.jar' | Select-Object -First 1
$configurator = Get-ChildItem -LiteralPath $plugins -Filter 'org.eclipse.equinox.simpleconfigurator_*.jar' | Select-Object -First 1
$configLines = @(
    ('osgi.framework=' + (ConvertTo-FileUri $framework.FullName)),
    ('osgi.bundles=reference:' + (ConvertTo-FileUri $configurator.FullName) + '@1:start'),
    ('org.eclipse.equinox.simpleconfigurator.configUrl=' + (ConvertTo-FileUri $bundleInfo)),
    'osgi.bundles.defaultStartLevel=4',
    'org.eclipse.update.reconcile=false',
    'osgi.configuration.cascaded=false',
    'osgi.framework.extensions='
)
[IO.File]::WriteAllLines((Join-Path $configuration 'config.ini'), $configLines, [Text.UTF8Encoding]::new($false))
$launcher = Get-ChildItem -LiteralPath $plugins -Filter 'org.eclipse.equinox.launcher_*.jar' | Select-Object -First 1
$launcherArgs = @('-jar', $launcher.FullName, '-install', $EclipseHome,
    '-configuration', $configuration, '-nosplash', '-consoleLog')
$repositoryUri = ConvertTo-FileUri $repository
& $javaTool @launcherArgs -application org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher -metadataRepository $repositoryUri -artifactRepository $repositoryUri -source $stage -publishArtifacts -compress
if ($LASTEXITCODE -ne 0) { throw 'p2 feature/bundle publishing failed.' }
& $javaTool @launcherArgs -application org.eclipse.equinox.p2.publisher.CategoryPublisher -metadataRepository $repositoryUri -categoryDefinition (ConvertTo-FileUri (Join-Path $repoRoot 'feature\category.xml')) -compress
if ($LASTEXITCODE -ne 0) { throw 'p2 category publishing failed.' }
Copy-Item -LiteralPath (Join-Path $repoRoot 'feature\category.xml') -Destination (Join-Path $repository 'category.xml')
Copy-Item -LiteralPath (Join-Path $repoRoot 'LICENSE') -Destination $repository
$zipName = 'eclipse-workspace-memo-update-site-1.0.1.zip'
$zipPath = Join-Path $work $zipName
# Windows PowerShell Compress-Archive writes backslashes into ZIP member names.
# p2 resolves artifact URLs with '/', so package with the JDK ZIP/JAR writer.
& $jarTool --create --no-manifest --file $zipPath -C $repository .
if ($LASTEXITCODE -ne 0) { throw 'Update-site ZIP packaging failed.' }
$entries = & $jarTool --list --file $zipPath
foreach ($entry in @('plugins/dev.eclipsememo_1.0.1.jar', 'features/dev.eclipsememo.feature_1.0.1.jar')) {
    if ($entries -cnotcontains $entry) { throw "Missing exact ZIP entry: $entry" }
}
if ($entries | Where-Object { $_.Contains('\') }) { throw 'ZIP member names must use forward slashes.' }

# Listing validates metadata only. Also collect both actual artifacts via p2.
$archiveUri = 'jar:' + (ConvertTo-FileUri $zipPath) + '!/'
# Windows PowerShell 5.1 wraps native stderr as ErrorRecord objects. Java's
# startup logging must not abort capture before its exit code can be checked.
$previousErrorPreference = $ErrorActionPreference
try {
    $ErrorActionPreference = 'Continue'
    $listOutput = & $javaTool @launcherArgs -application org.eclipse.equinox.p2.director -repository $archiveUri -list 2>&1
    $listExit = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $previousErrorPreference
}
$listOutput | ForEach-Object { Write-Output $_ }
if ($listExit -ne 0 -or !(($listOutput -join "`n") -match 'dev\.eclipsememo\.feature\.feature\.group')) {
    throw 'p2 could not read the installable feature from the finished ZIP.'
}
$mirror = Join-Path $work 'collected-artifacts'
& $javaTool @launcherArgs -application org.eclipse.equinox.p2.artifact.repository.mirrorApplication -source $archiveUri -destination (ConvertTo-FileUri $mirror) -verbose
if ($LASTEXITCODE -ne 0) { throw 'p2 could not collect the artifacts from the finished ZIP.' }
foreach ($entry in @('plugins/dev.eclipsememo_1.0.1.jar', 'features/dev.eclipsememo.feature_1.0.1.jar')) {
    $collected = Join-Path $mirror $entry
    if (!(Test-Path -LiteralPath $collected)) { throw "p2 did not collect: $entry" }
    if ((Get-FileHash -LiteralPath $collected -Algorithm SHA256).Hash -ne
        (Get-FileHash -LiteralPath (Join-Path $repository $entry) -Algorithm SHA256).Hash) {
        throw "Collected artifact differs from its source: $entry"
    }
}
$siteDirectory = Join-Path $OutputDirectory 'update-site'
New-Item -ItemType Directory -Force -Path $siteDirectory | Out-Null
Get-ChildItem -LiteralPath $repository -Force | ForEach-Object {
    Copy-Item -LiteralPath $_.FullName -Destination $siteDirectory -Recurse -Force
}
$publishedZip = Join-Path $OutputDirectory $zipName
Copy-Item -LiteralPath $zipPath -Destination $publishedZip -Force
Write-Output "Built and verified $publishedZip"
Write-Output "Publish the contents of $siteDirectory to your HTTPS update-site URL."
