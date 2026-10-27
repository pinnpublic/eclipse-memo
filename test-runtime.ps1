[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)][string]$EclipseHome,
    [Parameter(Mandatory = $true)][string]$JdkHome,
    [string]$Label = 'eclipse',
    [string]$ReuseConfiguration,
    [string]$RuntimeBase = (Join-Path $PSScriptRoot '.build'),
    [switch]$UseInstalledPlugin,
    [switch]$KeepOpen
)
$ErrorActionPreference = 'Stop'
$repoRoot = $PSScriptRoot
$pluginDir = Join-Path $EclipseHome 'plugins'
$runtimeRoot = Join-Path $RuntimeBase ('runtime-' + $Label + '-' + [DateTime]::Now.ToString('yyyyMMdd-HHmmss'))
$configuration = Join-Path $runtimeRoot 'configuration'
if ($ReuseConfiguration) { $configuration = [IO.Path]::GetFullPath($ReuseConfiguration) }
$workspace = Join-Path $runtimeRoot 'workspace'
$classes = Join-Path $runtimeRoot 'test-classes'
New-Item -ItemType Directory -Force -Path $configuration, $workspace, $classes | Out-Null
$platformClasspath = (Get-ChildItem -LiteralPath $pluginDir -Filter '*.jar' |
    Where-Object { $_.Name -match '^(org\.eclipse\.(core|equinox|osgi|jface|swt|ui|text|jdt)|org\.osgi)' } |
    ForEach-Object { $_.FullName.Replace('\', '/') }) -join ';'
$mainJar = Join-Path $runtimeRoot 'dev.eclipsememo_1.0.2.jar'
$pluginSource = if ($UseInstalledPlugin) { Join-Path $pluginDir 'dev.eclipsememo_1.0.2.jar' }
    else { Join-Path $repoRoot 'dist\dev.eclipsememo_1.0.2.jar' }
Copy-Item -LiteralPath $pluginSource -Destination $mainJar
$argsForCompiler = @('--release', '11', '-encoding', 'UTF-8', '-d', ('"' + $classes.Replace('\', '/') + '"'),
    '-classpath', ('"' + $mainJar.Replace('\', '/') + ';' + $platformClasspath + '"'))
$argsForCompiler += Get-ChildItem -LiteralPath (Join-Path $repoRoot 'tests\plugin\src') -Filter '*.java' -Recurse |
    ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' }
$argFile = Join-Path $runtimeRoot 'test-javac.args'
[IO.File]::WriteAllLines($argFile, $argsForCompiler, [Text.UTF8Encoding]::new($false))
& (Join-Path $JdkHome 'bin\javac.exe') "@$argFile"
if ($LASTEXITCODE -ne 0) { throw 'Runtime test compilation failed.' }
$testJar = Join-Path $runtimeRoot 'dev.eclipsememo.tests_1.0.0.jar'
$testPluginRoot = Join-Path $repoRoot 'tests\plugin'
& (Join-Path $JdkHome 'bin\jar.exe') --create --file $testJar --manifest (Join-Path $testPluginRoot 'META-INF\MANIFEST.MF') -C $classes . -C $testPluginRoot plugin.xml
if ($LASTEXITCODE -ne 0) { throw 'Runtime test packaging failed.' }
function ConvertTo-FileUri([string]$Path) { return [Uri]::new([IO.Path]::GetFullPath($Path)).AbsoluteUri }
$bundleLines = Get-Content -LiteralPath (Join-Path $EclipseHome 'configuration\org.eclipse.equinox.simpleconfigurator\bundles.info') -Encoding UTF8 |
    ForEach-Object {
        if ($_.StartsWith('#') -or !$_.Trim()) { $_ }
        else {
            $fields = $_ -split ','
            if ($fields[0] -eq 'dev.eclipsememo.tests') { return }
            if ($fields[0] -eq 'dev.eclipsememo' -and !$UseInstalledPlugin) { return }
            if (!($fields[2] -match '^[a-z]+:')) { $fields[2] = ConvertTo-FileUri (Join-Path $EclipseHome $fields[2]) }
            $fields -join ','
        }
    }
if ($UseInstalledPlugin) {
    $installedEntries = @($bundleLines | Where-Object { $_ -match '^dev\.eclipsememo,' })
    if ($installedEntries.Count -ne 1 -or $installedEntries[0] -notmatch '^dev\.eclipsememo,1\.0\.2,') {
        throw 'The installed bundles.info must enable exactly one Memo 1.0.2 bundle.'
    }
} else { $bundleLines += 'dev.eclipsememo,1.0.2,' + (ConvertTo-FileUri $mainJar) + ',4,false' }
$bundleLines += 'dev.eclipsememo.tests,1.0.0,' + (ConvertTo-FileUri $testJar) + ',4,false'
$bundleInfo = Join-Path $configuration 'bundles.info'
[IO.File]::WriteAllLines($bundleInfo, $bundleLines, [Text.UTF8Encoding]::new($false))
$framework = Get-ChildItem -LiteralPath $pluginDir -Filter 'org.eclipse.osgi_*.jar' | Select-Object -First 1
$configurator = Get-ChildItem -LiteralPath $pluginDir -Filter 'org.eclipse.equinox.simpleconfigurator_*.jar' | Select-Object -First 1
$configLines = @(
    ('osgi.framework=' + (ConvertTo-FileUri $framework.FullName)),
    ('osgi.bundles=reference:' + (ConvertTo-FileUri $configurator.FullName) + '@1:start'),
    ('org.eclipse.equinox.simpleconfigurator.configUrl=' + (ConvertTo-FileUri $bundleInfo)),
    'osgi.bundles.defaultStartLevel=4',
    'eclipse.application=org.eclipse.ui.ide.workbench',
    'eclipse.product=org.eclipse.platform.ide',
    'org.eclipse.update.reconcile=false',
    'osgi.configuration.cascaded=false',
    'osgi.framework.extensions='
    'osgi.checkConfiguration=true'
)
[IO.File]::WriteAllLines((Join-Path $configuration 'config.ini'), $configLines, [Text.UTF8Encoding]::new($false))
$launcher = Get-ChildItem -LiteralPath $pluginDir -Filter 'org.eclipse.equinox.launcher_*.jar' | Select-Object -First 1
$resultFile = Join-Path $runtimeRoot 'results.txt'
Write-Output "Test workspace: $workspace"
Write-Output "Result file: $resultFile"
try {
    $ErrorActionPreference = 'Continue'
    & (Join-Path $JdkHome 'bin\java.exe') '-DworkspaceMemo.test.realWorkbench=true' "-DworkspaceMemo.test.result=$resultFile" "-DworkspaceMemo.test.keepOpen=$($KeepOpen.IsPresent.ToString().ToLowerInvariant())" -Xmx1536m -jar $launcher.FullName -install $EclipseHome -configuration $configuration -data $workspace -application org.eclipse.ui.ide.workbench -clean -consoleLog -nosplash
    $runtimeExit = $LASTEXITCODE
} finally { $ErrorActionPreference = 'Stop' }
if (Test-Path -LiteralPath $resultFile) {
    $testResultLines = Get-Content -LiteralPath $resultFile -Encoding UTF8
    $testResultLines
    if (!($testResultLines -match '^PASS:') -or ($testResultLines -match '^FAIL:')) { $runtimeExit = 1 }
} else { $runtimeExit = 1 }
if ($runtimeExit -ne 0) { throw "Runtime tests failed with exit code $runtimeExit. See $runtimeRoot" }
