[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$repoRoot = $PSScriptRoot
$classes = Join-Path $repoRoot '.build\file-tests'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
& javac --release 11 -encoding UTF-8 -d $classes (Join-Path $repoRoot 'src\dev\eclipsememo\WorkspaceFiles.java') (Join-Path $repoRoot 'src\dev\eclipsememo\WorkspaceEntry.java') (Join-Path $repoRoot 'tests\WorkspaceFilesTest.java')
if ($LASTEXITCODE -ne 0) { throw 'File test compilation failed.' }
& java -cp $classes dev.eclipsememo.WorkspaceFilesTest (Join-Path $repoRoot '.build')
if ($LASTEXITCODE -ne 0) { throw 'File tests failed.' }
