<#
.SYNOPSIS
  Build and test a Minecraft mod on a Windows desktop (Emilio's PC "Quark").
  Defaults fit Villager Reroller (Stonecutter, versions\<v>); other mods pass -ModName and the
  -TestTask/-JarTask/-JarGlob/-GameTestTask/-HarnessTask/-RunDir parameters.

.DESCRIPTION
  Uses a portable JDK and a Gradle home on D: (no global installs, no admin).
  Steps (each can be skipped):
    1. git fetch + check out the branch (hard-resets this checkout only to origin/<Branch>).
    2. JUnit tests        :<TestVersion>:test
    3. Compile checks     :<v>:compileJava for each -CompileVersions entry
    4. Mod jar            :<Version>:build  -> copied to -OutDir and %USERPROFILE%\Downloads
    5. Client gametests   :<Version>:runClientGameTest  (opens a real Minecraft window)
    6. Tick harness       :<Version>:runClient -Pharness=true  (survival reroll loop)
  Screenshots land in -OutDir\screens. Every Java process started from this checkout or
  Gradle home is stopped at the end, so nothing keeps running on the desktop.

.EXAMPLE
  powershell -NoProfile -ExecutionPolicy Bypass -File scripts\quark-test.ps1 -Branch codex/ui-ux-v3
#>
param(
    [string]$ModName = "villager-reroller",
    [string]$Branch = "",
    [string]$Version = "26.3",
    [string]$TestVersion = "1.21.11",
    [string[]]$CompileVersions = @("26.3", "1.21.11", "1.20.1"),
    [string]$Jdk = "D:\dev\jdks\jdk-25.0.5+8",
    [string]$Jdk21 = "",
    [string]$Jdk17 = "",
    [string]$GradleHome = "D:\dev\gradle-home",
    [string]$OutDir = "D:\dev\vr-test-out",
    [int]$TimeoutScale = 8,
    [int]$GameTestMinutes = 40,
    # Gradle task paths. Empty = the Villager Reroller defaults for -Version.
    [string]$TestTask = "",
    [string]$JarTask = "",
    [string]$JarGlob = "",
    [string]$GameTestTask = "",
    [string]$HarnessTask = "",
    [string]$RunDir = "",
    # Only one Minecraft client on this PC at a time, shared by every mod's test script.
    [string]$McLock = "D:\dev\mc-test.lock",
    [int]$McLockStaleMinutes = 30,
    [switch]$SkipGit,
    [switch]$SkipTests,
    [switch]$SkipCompile,
    [switch]$SkipJar,
    [switch]$SkipGameTest,
    [switch]$SkipHarness,
    [switch]$NoDownloadsCopy,
    # Run in the hidden SSH session instead of on the desktop (the client is then invisible).
    [switch]$Hidden,
    # Internal: set when the scheduled task re-launches this script inside the desktop session.
    [switch]$InSession,
    [switch]$NoClickThrough
)

$ErrorActionPreference = "Stop"
$Repo = Split-Path -Parent $PSScriptRoot
Set-Location $Repo
New-Item -ItemType Directory -Force -Path $OutDir, "$OutDir\screens", "$OutDir\logs" | Out-Null

# SSH sessions on Windows have no desktop, so a client started there runs but cannot be seen.
# Unless -Hidden is given, re-run this script as a one-shot interactive scheduled task in the
# logged-in user's session, wait for it, print its summary, and delete the task.
if (-not $InSession -and -not $Hidden) {
    $task = "McTest-" + ($ModName -replace "[^A-Za-z0-9_.-]", "_")
    $done = Join-Path $OutDir "done.flag"
    Remove-Item $done, (Join-Path $OutDir "summary.json") -ErrorAction SilentlyContinue
    $pass = @()
    foreach ($kv in $PSBoundParameters.GetEnumerator()) {
        if ($kv.Value -is [System.Management.Automation.SwitchParameter]) {
            if ($kv.Value.IsPresent) { $pass += "-$($kv.Key)" }
        } elseif ($kv.Value -is [array]) {
            $pass += "-$($kv.Key)"; $pass += ($kv.Value -join ",")
        } else {
            $pass += "-$($kv.Key)"; $pass += "`"$($kv.Value)`""
        }
    }
    $argLine = "-NoProfile -ExecutionPolicy Bypass -WindowStyle Hidden -File `"$PSCommandPath`" -InSession " + ($pass -join " ")
    $action = New-ScheduledTaskAction -Execute "powershell.exe" -Argument $argLine -WorkingDirectory $Repo
    $principal = New-ScheduledTaskPrincipal -UserId ([System.Security.Principal.WindowsIdentity]::GetCurrent().Name) -LogonType Interactive -RunLevel Limited
    $settings = New-ScheduledTaskSettingsSet -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries -ExecutionTimeLimit (New-TimeSpan -Hours 3)
    Unregister-ScheduledTask -TaskName $task -Confirm:$false -ErrorAction SilentlyContinue
    Register-ScheduledTask -TaskName $task -Action $action -Principal $principal -Settings $settings | Out-Null
    Start-ScheduledTask -TaskName $task
    Write-Host "started on the desktop as scheduled task $task; logs in $OutDir\logs"
    try {
        while (-not (Test-Path $done)) {
            Start-Sleep -Seconds 10
            $state = (Get-ScheduledTask -TaskName $task).State
            if ($state -ne "Running" -and -not (Test-Path $done)) {
                Start-Sleep -Seconds 5
                if (-not (Test-Path $done)) { Write-Host "task stopped without finishing ($state)"; break }
            }
        }
    } finally {
        Unregister-ScheduledTask -TaskName $task -Confirm:$false -ErrorAction SilentlyContinue
    }
    if (Test-Path (Join-Path $OutDir "summary.json")) { Get-Content (Join-Path $OutDir "summary.json") }
    return
}
Start-Transcript -Path (Join-Path $OutDir "logs\run.log") -Force | Out-Null

$env:JAVA_HOME = $Jdk
$env:GRADLE_USER_HOME = $GradleHome
$env:Path = "$Jdk\bin;$env:Path"
$installs = @($Jdk, $Jdk21, $Jdk17) | Where-Object { $_ -and (Test-Path $_) }
$CompileVersions = @($CompileVersions | ForEach-Object { $_ -split ',' } | Where-Object { $_ })
$common = @("--console=plain", "--no-daemon", "--configure-on-demand", "--no-configuration-cache",
            "-Porg.gradle.java.installations.paths=$($installs -join ',')",
            "-PtimeoutScale=$TimeoutScale")
$results = [ordered]@{}
if (-not $TestTask) { $TestTask = ":${TestVersion}:test" }
if (-not $JarTask) { $JarTask = ":${Version}:build" }
if (-not $JarGlob) { $JarGlob = "$Repo\versions\$Version\build\libs\*.jar" }
if (-not $GameTestTask) { $GameTestTask = ":${Version}:runClientGameTest" }
if (-not $HarnessTask) { $HarnessTask = ":${Version}:runClient" }
if (-not $RunDir) { $RunDir = if ($Version -eq "1.21.11") { "$Repo\run" } else { "$Repo\run\$Version" } }

# D:\dev\mc-test.lock holds "<mod> <time>" while a test client is open. Wait for a fresh lock
# held by another mod; take over a lock older than $McLockStaleMinutes.
$script:haveLock = $false
function Enter-McLock {
    while (Test-Path $McLock) {
        $age = (Get-Date) - (Get-Item $McLock).LastWriteTime
        if ($age.TotalMinutes -ge $McLockStaleMinutes) { Write-Host "    stale lock ($([int]$age.TotalMinutes) min): $(Get-Content $McLock -Raw)"; break }
        Write-Host "    waiting for $McLock : $(Get-Content $McLock -Raw)"
        Start-Sleep -Seconds 30
    }
    Set-Content -Path $McLock -Value "$ModName $(Get-Date -Format s) pid $PID"
    $script:haveLock = $true
}
function Exit-McLock {
    if ($script:haveLock -and (Test-Path $McLock) -and ((Get-Content $McLock -Raw) -match "pid $PID")) { Remove-Item $McLock -Force }
    $script:haveLock = $false
}

function Step([string]$name, [string[]]$tasks, [int]$minutes = 30) {
    $log = Join-Path $OutDir "logs\$name.log"
    Write-Host "==> $name : $($tasks -join ' ')"
    $gradleArgs = $common + $tasks
    $p = Start-Process -FilePath "$Repo\gradlew.bat" -ArgumentList $gradleArgs -NoNewWindow -PassThru `
        -RedirectStandardOutput $log -RedirectStandardError "$log.err"
    $null = $p.Handle  # cache the handle so ExitCode is readable after WaitForExit
    if (-not $p.WaitForExit($minutes * 60 * 1000)) {
        Write-Host "    timeout after $minutes min, stopping"
        taskkill /T /F /PID $p.Id *> $null
        Stop-RepoJava
        $results[$name] = "TIMEOUT"
        return
    }
    $p.WaitForExit()
    $ok = ($p.ExitCode -eq 0) -or (Select-String -Path $log -Pattern '^BUILD SUCCESSFUL' -Quiet)
    $results[$name] = if ($ok) { "PASS" } else { "FAIL ($($p.ExitCode))" }
    Write-Host "    $($results[$name])  log: $log"
}

# The test client must stay silent and keep running while it does not have focus.
function Set-Muted {
    $dir = $RunDir
    New-Item -ItemType Directory -Force -Path $dir | Out-Null
    $opts = Join-Path $dir "options.txt"
    $lines = @()
    if (Test-Path $opts) { $lines = @(Get-Content $opts | Where-Object { $_ -notmatch '^(soundCategory_master|pauseOnLostFocus):' }) }
    $lines += "soundCategory_master:0.0"
    $lines += "pauseOnLostFocus:false"
    Set-Content -Path $opts -Value $lines
}

function Stop-RepoJava {
    # Only JVMs started from this checkout. The Gradle home may be shared with other mods' runs.
    $needles = @($Repo) | ForEach-Object { $_.ToLower() }
    Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" | Where-Object {
        $cl = "$($_.CommandLine)".ToLower()
        ($needles | Where-Object { $cl.Contains($_) }).Count -gt 0
    } | ForEach-Object {
        Write-Host "    stopping java pid $($_.ProcessId)"
        Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue
    }
}

$stopLock = Join-Path $OutDir "lock.stop"
Remove-Item $stopLock -ErrorAction SilentlyContinue
if ($InSession) {
    $lockArgs = @("-NoProfile", "-ExecutionPolicy", "Bypass", "-WindowStyle", "Hidden", "-File",
        "`"$PSScriptRoot\quark-window-lock.ps1`"", "-Repo", "`"$Repo`"", "-StopFile", "`"$stopLock`"", "-ParentPid", $PID,
        "-LogFile", "`"$OutDir\logs\window-lock.log`"", "-LiveShot", "`"$OutDir\logs\live.png`"")
    if ($NoClickThrough) { $lockArgs += "-NoClickThrough" }
    Start-Process powershell.exe -ArgumentList $lockArgs -WindowStyle Hidden | Out-Null
}

try {
    if (-not $SkipGit -and $Branch) {
        git fetch --quiet origin $Branch
        git checkout --quiet -B $Branch "origin/$Branch"
    }
    $results["commit"] = (git rev-parse --short HEAD)

    if (-not $SkipTests) { Step "junit" @($TestTask) }
    if (-not $SkipCompile) { Step "compile" ($CompileVersions | ForEach-Object { ":${_}:compileJava" }) }
    if (-not $SkipJar) {
        Step "jar" @($JarTask, "-x", "test")
        $jar = Get-ChildItem $JarGlob -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -notmatch '(sources|dev)' } | Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($jar) {
            Copy-Item $jar.FullName $OutDir -Force
            if (-not $NoDownloadsCopy) { Copy-Item $jar.FullName "$env:USERPROFILE\Downloads\" -Force }
            $results["jarFile"] = $jar.Name
        }
    }
    if (-not $SkipGameTest) {
        Set-Muted
        Enter-McLock
        try { Step "gametest" @($GameTestTask, "-PartifactsDir=$OutDir\screens\gametest") $GameTestMinutes }
        finally { Stop-RepoJava; Exit-McLock }
    }
    if (-not $SkipHarness) {
        Set-Muted
        Enter-McLock
        try { Step "harness" @($HarnessTask, "-Pharness=true", "-PartifactsDir=$OutDir\screens\harness") $GameTestMinutes }
        finally { Stop-RepoJava; Exit-McLock }
        $res = Get-ChildItem "$OutDir\screens\harness" -Recurse -Filter harness-result.txt -ErrorAction SilentlyContinue | Select-Object -First 1
        if ($res) { $results["harnessResult"] = [string](@(Get-Content $res.FullName -TotalCount 1)[0]) }
    }
} finally {
    Stop-RepoJava
    Exit-McLock
    New-Item -ItemType File -Force -Path $stopLock | Out-Null
    $results | ConvertTo-Json | Set-Content (Join-Path $OutDir "summary.json")
    if ($InSession) { New-Item -ItemType File -Force -Path (Join-Path $OutDir "done.flag") | Out-Null; Stop-Transcript | Out-Null }
    Write-Host ""
    $results.GetEnumerator() | ForEach-Object { Write-Host ("{0,-14} {1}" -f $_.Key, $_.Value) }
}
