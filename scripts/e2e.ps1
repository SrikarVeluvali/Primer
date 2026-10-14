param([string]$BaseUrl = 'http://127.0.0.1:5173', [string]$PaperId = '1706.03762')
$ErrorActionPreference = 'Stop'
$runId = [Guid]::NewGuid().ToString('N').Substring(0, 12)
$qaPassword = [Guid]::NewGuid().ToString('N')
$alice = New-Object Microsoft.PowerShell.Commands.WebRequestSession
$bob = New-Object Microsoft.PowerShell.Commands.WebRequestSession
function Call-Folio($Session, $Method, $Path, $Body = $null) {
    $headers = @{}
    if ($Method -ne 'GET') {
        $csrfSession = Invoke-RestMethod -Uri "$BaseUrl/api/auth/session" -WebSession $Session
        $headers['X-CSRF-TOKEN'] = $csrfSession.csrfToken
    }
    $request = @{ Uri = "$BaseUrl/api$Path"; Method = $Method; WebSession = $Session; Headers = $headers; TimeoutSec = 360 }
    if ($null -ne $Body) { $request.ContentType = 'application/json'; $request.Body = ($Body | ConvertTo-Json -Compress) }
    return (Invoke-RestMethod @request)
}
$aliceUser = Call-Folio $alice 'POST' '/auth/register' @{name='Primer QA Alice';email="folio-qa-$runId-alice@example.test";password=$qaPassword}
$bobUser = Call-Folio $bob 'POST' '/auth/register' @{name='Primer QA Bob';email="folio-qa-$runId-bob@example.test";password=$qaPassword}
Write-Output "QA accounts created: $($aliceUser.id), $($bobUser.id)"
$connection = Call-Folio $alice 'GET' '/status'
if (-not $connection.connected) { throw $connection.message }
Write-Output "Ollama connected. Answer model: $($connection.chatModel)"
$paper = Call-Folio $alice 'POST' '/papers' @{url=$PaperId}
Write-Output "Import accepted: $($paper.id)"
$deadline = (Get-Date).AddMinutes(8)
$previousStage = ''
while ($paper.status -ne 'READY') {
    $paper = Call-Folio $alice 'GET' "/papers/$($paper.id)"
    if ($paper.stage -ne $previousStage) { Write-Output $paper.stage; $previousStage = $paper.stage }
    if ($paper.status -eq 'FAILED') { throw $paper.error }
    if ((Get-Date) -gt $deadline) { throw 'Paper indexing timed out.' }
    if ($paper.status -ne 'READY') { Start-Sleep -Seconds 2 }
}
Write-Output "Indexed $($paper.pageCount) pages, $($paper.chunkCount) passages."
$library = @(Call-Folio $bob 'GET' '/papers')
if (-not ($library | Where-Object { $_.id -eq $paper.id })) { throw 'Bob cannot see the global paper.' }
$turn = Call-Folio $alice 'POST' "/papers/$($paper.id)/chat" @{question='What is the main contribution of this paper? Give a concise answer with citations.';model=$connection.chatModel}
if (-not $turn.answer -or $turn.sources.Count -eq 0) { throw 'RAG returned no answer or sources.' }
if ($PaperId -eq '1706.03762') {
    if ($turn.answer -notmatch '(?i)Transformer' -or $turn.answer -match '(?i)random characters|nonsensical sequences|impossible.*summari') { throw 'Paper-summary regression: the answer did not describe the Transformer.' }
    if (-not ($turn.sources | Where-Object { $_.page -eq 1 })) { throw 'Paper-summary regression: no first-page abstract evidence was included.' }
    if ($turn.sources.text -match 'Law Law Law|The The The|<pad>') { throw 'Diagram glyphs leaked into summary evidence.' }
}
Write-Output "Answer received with $($turn.sources.Count) page-linked sources."
Write-Output $turn.answer
$bobHistory = @(Call-Folio $bob 'GET' "/papers/$($paper.id)/chat")
if ($bobHistory.Count -ne 0) { throw 'Private conversation leaked to Bob.' }
$aliceHistory = @(Call-Folio $alice 'GET' "/papers/$($paper.id)/chat")
if ($aliceHistory.Count -eq 0) { throw 'Conversation was not saved.' }
Call-Folio $alice 'POST' '/auth/logout'
Call-Folio $alice 'POST' '/auth/login' @{email=$aliceUser.email;password=$qaPassword} | Out-Null
$restored = @(Call-Folio $alice 'GET' "/papers/$($paper.id)/chat")
if ($restored.Count -ne $aliceHistory.Count) { throw 'Conversation was not restored after login.' }
Call-Folio $alice 'DELETE' "/papers/$($paper.id)/chat"
Call-Folio $alice 'POST' '/auth/logout'
Call-Folio $bob 'POST' '/auth/logout'
Write-Output 'PASS: import, local embeddings, local answer, global library, private history, logout, login, persistence, and private clear.'
Write-Output "Remove QA accounts from the local test database after stopping the server: $($aliceUser.id), $($bobUser.id)"
