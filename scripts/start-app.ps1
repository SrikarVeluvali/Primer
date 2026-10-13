[CmdletBinding()]
param([switch]$Rebuild, [switch]$NoBrowser, [switch]$NoWait)

$ErrorActionPreference = 'Stop'
$taskRoot = (Resolve-Path -LiteralPath (Join-Path $PSScriptRoot '..')).Path
$taskBackend = Join-Path $taskRoot 'backend'
$taskFrontend = Join-Path $taskRoot 'frontend'
$taskJar = Join-Path $taskBackend 'target\primer-api-0.1.0.jar'
$taskLogs = Join-Path $taskRoot 'logs'
$taskPort = if ($env:SERVER_PORT) { [int]$env:SERVER_PORT } else { 8080 }
if ($taskPort -lt 1 -or $taskPort -gt 65535) { throw 'SERVER_PORT must be between 1 and 65535.' }
$taskAddress = if ($env:SERVER_ADDRESS -and $env:SERVER_ADDRESS -ne '0.0.0.0') { $env:SERVER_ADDRESS } else { '127.0.0.1' }
if ($taskAddress -eq '::') { $taskAddress = '::1' }
$taskHost = if ($taskAddress.Contains(':')) { "[$taskAddress]" } else { $taskAddress }
$taskAppUrl = "http://${taskHost}:$taskPort"
$taskOllamaUrl = if ($env:OLLAMA_URL) { $env:OLLAMA_URL.TrimEnd('/') } else { 'http://127.0.0.1:11434' }
$taskChatModel = if ($env:CHAT_MODEL) { $env:CHAT_MODEL } else { 'phi4-mini:latest' }
$taskEmbeddingModel = if ($env:EMBEDDING_MODEL) { $env:EMBEDDING_MODEL } else { 'nomic-embed-text' }
$taskApiProcess = $null
$taskOllamaProcess = $null
$taskStarted = $false
$taskWaitForExit = $false
$taskHealthSession = New-Object Microsoft.PowerShell.Commands.WebRequestSession
New-Item -ItemType Directory -Path $taskLogs -Force | Out-Null
$taskRun = Get-Date -Format 'yyyyMMdd-HHmmss'
$taskApiOut = Join-Path $taskLogs "primer-$taskRun-$PID.out.log"
$taskApiErr = Join-Path $taskLogs "primer-$taskRun-$PID.err.log"

function Test-Primer {
    try {
        $session = Invoke-RestMethod "$taskAppUrl/api/auth/session" -WebSession $taskHealthSession -TimeoutSec 2
        if (-not $session.csrfToken -or -not $session.PSObject.Properties['user']) { return $false }
        $page = Invoke-WebRequest $taskAppUrl -UseBasicParsing -TimeoutSec 2
        return $page.Content -match '<title>\s*Primer\b'
    } catch { return $false }
}

function Get-OllamaModels {
    try { return Invoke-RestMethod "$taskOllamaUrl/api/tags" -TimeoutSec 2 }
    catch { return $null }
}

function Test-Port([string]$Address, [int]$Port) {
    $client = New-Object System.Net.Sockets.TcpClient
    try { $client.Connect($Address, $Port); return $true }
    catch { return $false }
    finally { $client.Dispose() }
}

function Find-Ollama {
    $command = Get-Command ollama.exe -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    $installed = Join-Path $env:LOCALAPPDATA 'Programs\Ollama\ollama.exe'
    if (Test-Path -LiteralPath $installed) { return $installed }
    throw 'Install Ollama first: https://ollama.com/download'
}

function Find-Java {
    $candidates = @()
    if ($env:JAVA_HOME) { $candidates += Join-Path $env:JAVA_HOME 'bin\java.exe' }
    $candidates += Join-Path $env:ProgramFiles 'Java\jdk-21\bin\java.exe'
    $command = Get-Command java.exe -ErrorAction SilentlyContinue
    if ($command) { $candidates += $command.Source }
    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        if (-not (Test-Path -LiteralPath $candidate)) { continue }
        try {
            $version = & $candidate --version
            if ($LASTEXITCODE -eq 0 -and ($version -join ' ') -match '(?:openjdk|java)\s+(\d+)') {
                if ([int]$Matches[1] -ge 21) { return $candidate }
            }
        } catch { continue }
    }
    throw 'Install JDK 21 or newer and set JAVA_HOME to its installation directory.'
}

function Invoke-Checked([string]$Command, [string[]]$Arguments, [string]$Directory) {
    Push-Location -LiteralPath $Directory
    $savedPreference = $ErrorActionPreference
    try {
        # Windows PowerShell 5.1 treats native stderr as errors, including normal CLI progress.
        $ErrorActionPreference = 'Continue'
        & $Command @Arguments
        $commandExit = $LASTEXITCODE
    } finally {
        $ErrorActionPreference = $savedPreference
        Pop-Location
    }
    if ($commandExit -ne 0) { throw "$Command failed with exit code $commandExit. See the output above." }
}

function Test-BuildNeeded {
    if ($Rebuild -or -not (Test-Path -LiteralPath $taskJar)) { return $true }
    $builtAt = (Get-Item -LiteralPath $taskJar).LastWriteTimeUtc
    $inputs = @('backend\src', 'backend\pom.xml', 'frontend\src', 'frontend\public',
        'frontend\index.html', 'frontend\package.json', 'frontend\package-lock.json',
        'frontend\vite.config.ts', 'frontend\tsconfig.json', 'frontend\tsconfig.app.json', 'frontend\tsconfig.node.json')
    foreach ($input in $inputs) {
        $path = Join-Path $taskRoot $input
        if ((Test-Path -LiteralPath $path) -and
            (Get-ChildItem -LiteralPath $path -Recurse -File | Where-Object LastWriteTimeUtc -gt $builtAt | Select-Object -First 1)) {
            return $true
        }
    }
    return $false
}

try {
    Write-Host 'Starting Primer...' -ForegroundColor Cyan
    $taskAlreadyRunning = Test-Primer
    if ($taskAlreadyRunning -and $Rebuild) { throw "Primer is already running at $taskAppUrl. Stop it before using -Rebuild." }
    if (-not $taskAlreadyRunning -and (Test-Port $taskAddress $taskPort)) {
        throw "Port $taskPort is occupied by another application. Stop it or set SERVER_PORT to another port."
    }

    $taskModels = Get-OllamaModels
    if ($null -eq $taskModels) {
        $ollamaUri = [Uri]$taskOllamaUrl
        if ($ollamaUri.Host -notin @('localhost', '127.0.0.1', '::1', '[::1]')) {
            throw "Ollama is unavailable at $taskOllamaUrl. Start the configured server first."
        }
        if (Test-Port $ollamaUri.DnsSafeHost $ollamaUri.Port) { throw "The Ollama port is occupied, but its API is not responding at $taskOllamaUrl." }
        $taskOllamaExe = Find-Ollama
        $previousOllamaHost = $env:OLLAMA_HOST
        try {
            $env:OLLAMA_HOST = $ollamaUri.GetLeftPart([System.UriPartial]::Authority)
            $taskOllamaProcess = Start-Process -FilePath $taskOllamaExe -ArgumentList 'serve' -WindowStyle Hidden -PassThru `
                -RedirectStandardOutput (Join-Path $taskLogs "ollama-$taskRun-$PID.out.log") `
                -RedirectStandardError (Join-Path $taskLogs "ollama-$taskRun-$PID.err.log")
        } finally { $env:OLLAMA_HOST = $previousOllamaHost }
        Write-Host 'Waiting for local Ollama...'
        $deadline = (Get-Date).AddSeconds(60)
        do {
            if ($taskOllamaProcess.HasExited) { throw "Ollama exited. Check the logs in $taskLogs." }
            $taskModels = Get-OllamaModels
            if ($null -ne $taskModels) { break }
            Start-Sleep -Milliseconds 500
        } while ((Get-Date) -lt $deadline)
        if ($null -eq $taskModels) { throw "Ollama did not become ready. Check the logs in $taskLogs." }
    } else { Write-Host 'Ollama is already running.' }

    foreach ($model in (@($taskChatModel, $taskEmbeddingModel) | Select-Object -Unique)) {
        $modelName = if ($model.Contains(':')) { $model } else { "${model}:latest" }
        if ($taskModels.models.name -notcontains $modelName) {
            Write-Host "Downloading missing model: $modelName"
            $previousOllamaHost = $env:OLLAMA_HOST
            try {
                $env:OLLAMA_HOST = ([Uri]$taskOllamaUrl).GetLeftPart([System.UriPartial]::Authority)
                Invoke-Checked (Find-Ollama) @('pull', $model) $taskRoot
            } finally { $env:OLLAMA_HOST = $previousOllamaHost }
        }
    }

    if (-not $taskAlreadyRunning) {
        $taskJava = Find-Java
        if (Test-BuildNeeded) {
            $npm = Get-Command npm.cmd -ErrorAction SilentlyContinue
            $maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
            if (-not $npm -or -not $maven) { throw 'Building Primer requires Node.js/npm and Maven on PATH. Install them, then run this script again.' }
            $jdk = Split-Path (Split-Path $taskJava -Parent) -Parent
            if (Test-Path -LiteralPath (Join-Path $jdk 'bin\javac.exe')) { $env:JAVA_HOME = $jdk; $env:PATH = "$jdk\bin;$env:PATH" }
            Write-Host 'Building the frontend and packaged backend...'
            Invoke-Checked $npm.Source @('ci') $taskFrontend
            Invoke-Checked $npm.Source @('run', 'build') $taskFrontend
            # clean prevents old fingerprinted frontend assets remaining in the packaged application.
            Invoke-Checked $maven.Source @('-B', 'clean', 'package', '-DskipTests') $taskBackend
        }
        $env:SERVER_PORT = [string]$taskPort
        $taskApiProcess = Start-Process -FilePath $taskJava -ArgumentList @('-jar', "`"$taskJar`"") `
            -WorkingDirectory $taskBackend -WindowStyle Hidden -PassThru `
            -RedirectStandardOutput $taskApiOut -RedirectStandardError $taskApiErr
        Write-Host 'Waiting for Primer...'
        $deadline = (Get-Date).AddSeconds(120)
        do {
            if ($taskApiProcess.HasExited) { throw "Primer exited. Check $taskApiOut and $taskApiErr." }
            if (Test-Primer) { break }
            Start-Sleep -Milliseconds 500
        } while ((Get-Date) -lt $deadline)
        if (-not (Test-Primer)) { throw "Primer did not become ready. Check $taskApiOut and $taskApiErr." }
    } else { Write-Host 'Primer is already running; using the existing instance.' }

    $taskStarted = $true
    Write-Host "Primer is ready: $taskAppUrl" -ForegroundColor Green
    Write-Host "Logs: $taskLogs"
    if (-not $NoBrowser) { Start-Process -FilePath $taskAppUrl }
    if ($taskApiProcess -and -not $NoWait) {
        $taskWaitForExit = $true
        Write-Host 'Keep this window open. Press Ctrl+C to stop services started by this launcher.'
        while (-not $taskApiProcess.HasExited) { Start-Sleep -Milliseconds 500 }
        if ($taskApiProcess.ExitCode -ne 0) { throw "Primer stopped unexpectedly. Check $taskApiErr." }
    }
} catch {
    Write-Host "Primer could not start: $($_.Exception.Message)" -ForegroundColor Red
    exit 1
} finally {
    if (-not $taskStarted -or $taskWaitForExit) {
        foreach ($process in @($taskApiProcess, $taskOllamaProcess)) {
            if ($process -and -not $process.HasExited) { Stop-Process -Id $process.Id -ErrorAction SilentlyContinue }
        }
    }
}
