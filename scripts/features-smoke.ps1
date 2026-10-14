param([string]$BaseUrl='http://127.0.0.1:5173',[Parameter(Mandatory=$true)][string]$Email,[Parameter(Mandatory=$true)][string]$Password)
# Run only with a disposable QA account. This replaces its own learning plan.
$ErrorActionPreference='Stop'
$qaSession=New-Object Microsoft.PowerShell.Commands.WebRequestSession
function Call-Primer($Method,$Path,$Body=$null) {
    $headers=@{}
    if($Method -ne 'GET') {$csrf=Invoke-RestMethod -Uri "$BaseUrl/api/auth/session" -WebSession $qaSession;$headers['X-CSRF-TOKEN']=$csrf.csrfToken}
    $args=@{Uri="$BaseUrl/api$Path";Method=$Method;WebSession=$qaSession;Headers=$headers;TimeoutSec=360}
    if($null -ne $Body){$args.ContentType='application/json';$args.Body=$Body|ConvertTo-Json -Depth 15 -Compress}
    return(Invoke-RestMethod @args)
}
$account=Call-Primer 'POST' '/auth/login' @{email=$Email;password=$Password}
Write-Output "QA account: $($account.id)"
$paper=@(Call-Primer 'GET' '/papers')|Where-Object arxivId -eq '1706.03762'|Select-Object -First 1
if(-not $paper){throw 'Import the example paper before this smoke test.'}
$base="/papers/$($paper.id)"
Call-Primer 'PUT' "$base/notes" @{text='# QA notes: private to this account'}|Out-Null
$note=Call-Primer 'GET' "$base/notes"
if($note.text -notmatch 'QA notes'){throw 'Notes did not persist.'}
Write-Output 'Notes saved and restored.'
$theme=@{id='qa-ocean';name='QA Ocean';mode='dark';background='#101b28';surface='#1a2838';panel='#162330';text='#e5edf6';muted='#adbdcd';accent='#84bbdd';border='#31485a'}
Call-Primer 'PUT' '/preferences' @{activeTheme='qa-ocean';themes=@($theme)}|Out-Null
$prefs=Call-Primer 'GET' '/preferences'
if($prefs.activeTheme -ne 'qa-ocean'){throw 'Custom theme did not persist.'}
Write-Output 'Custom theme saved and restored.'
Write-Output 'Generating a real questionnaire with local Ollama…'
$learning=Call-Primer 'POST' "$base/learn/assessment" @{model='phi4-mini:latest'}
Write-Output "Questionnaire: $($learning.assessment.topics.Count) familiarity ratings."
$answers=@{};$confidence=@{};$index=0
foreach($topic in $learning.assessment.topics){if($topic.question -or $topic.correctOption){throw 'Questionnaire contains a quiz instead of familiarity concepts.'};$confidence[$topic.id]=if($index -eq 0){0}else{2};$index++}
Write-Output 'Generating a personalized roadmap and fetching public learning resources…'
$learning=Call-Primer 'POST' "$base/learn/plan" @{model='phi4-mini:latest';answers=@{answers=$answers;confidence=$confidence;goal='Understand this paper and explain its central ideas.';minutesPerDay=30;preference='mixed'}}
if($learning.plan.roadmap.Count -lt 4){throw 'Roadmap is incomplete.'}
Write-Output "Roadmap: $($learning.plan.roadmap.Count) stages; prerequisites: $($learning.plan.prerequisites.Count); resources: $($learning.plan.resources.Count)."
$learning.plan.resources|Select-Object type,title,origin,url|ConvertTo-Json -Depth 3
$learning=Call-Primer 'PUT' "$base/learn/progress" @{completedSteps=@($learning.plan.roadmap[0].id)}
$restored=Call-Primer 'GET' "$base/learn"
if($restored.completedSteps.Count -ne 1){throw 'Progress did not persist.'}
Write-Output 'Learning progress saved and restored.'
$profile=Call-Primer 'GET' '/knowledge'
if($profile.tags.Count -lt $learning.assessment.topics.Count){throw 'Knowledge ratings did not persist.'}
$recommendations=@(Call-Primer 'GET' '/recommendations')
if($recommendations.Count -eq 0){throw 'No recommendations matched the learned topics.'}
Write-Output "Private knowledge tags: $($profile.tags.Count); matching papers: $($recommendations.Count)."
$highlight=Call-Primer 'POST' "$base/annotations" @{page=1;text='The dominant sequence transduction models are based on complex recurrent or convolutional neural networks.';color='amber';rectangles=@(@{x=0.1;y=0.2;width=0.4;height=0.02})}
$restoredHighlights=@(Call-Primer 'GET' "$base/annotations")
if(-not ($restoredHighlights|Where-Object id -eq $highlight.id)){throw 'Highlight did not persist.'}
$turn=Call-Primer 'POST' "$base/annotations/$($highlight.id)/explain" @{model='phi4-mini:latest'}
if($turn.sources[0].text -ne $highlight.text -or $turn.sources[0].page -ne 1){throw 'Explanation is not grounded in the highlight.'}
Write-Output 'PDF highlight saved and explained with its exact passage and page context.'
Call-Primer 'POST' '/auth/logout'|Out-Null
Write-Output 'PASS: notes, themes, familiarity questionnaire, knowledge tags, recommendations, roadmap, resources, highlights, explanation, and progress.'
