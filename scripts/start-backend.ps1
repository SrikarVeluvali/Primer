$ErrorActionPreference = 'Stop'
if (-not (Get-Command java -ErrorAction SilentlyContinue)) { throw 'Install JDK 21 and set JAVA_HOME first.' }
if (-not (Get-Command mvn -ErrorAction SilentlyContinue)) { throw 'Install Maven 3.6.3 or newer first.' }
Set-Location -LiteralPath (Join-Path $PSScriptRoot '..\backend')
& mvn.cmd spring-boot:run
exit $LASTEXITCODE
