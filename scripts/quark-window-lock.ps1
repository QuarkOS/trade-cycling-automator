<#
  Keeps the test Minecraft client visible but not interactable on the desktop.
  Runs in the user's interactive session (started by quark-test.ps1). For every Minecraft
  window owned by a java process from this checkout it:
    - disables the window (EnableWindow false): mouse and keyboard input are ignored,
    - adds WS_EX_NOACTIVATE so it never takes focus, and (default) WS_EX_LAYERED|WS_EX_TRANSPARENT
      with full opacity so clicks pass through to whatever is underneath,
    - gives focus back to the window that had it before the client appeared.
  Exits when -StopFile exists or -ParentPid is gone.
#>
param(
    [string]$Repo,
    [string]$StopFile,
    [int]$ParentPid = 0,
    [switch]$NoClickThrough,
    [string]$LogFile = ""
)
function Log([string]$m) { if ($LogFile) { Add-Content -Path $LogFile -Value ("{0:HH:mm:ss} {1}" -f (Get-Date), $m) } }

Add-Type @"
using System;
using System.Runtime.InteropServices;
public static class VrWin {
    [DllImport("user32.dll")] public static extern IntPtr GetForegroundWindow();
    [DllImport("user32.dll")] public static extern bool SetForegroundWindow(IntPtr h);
    [DllImport("user32.dll")] public static extern bool EnableWindow(IntPtr h, bool enable);
    [DllImport("user32.dll", EntryPoint="GetWindowLongPtrW")] public static extern IntPtr GetLong(IntPtr h, int i);
    [DllImport("user32.dll", EntryPoint="SetWindowLongPtrW")] public static extern IntPtr SetLong(IntPtr h, int i, IntPtr v);
    [DllImport("user32.dll")] public static extern bool SetLayeredWindowAttributes(IntPtr h, uint key, byte alpha, uint flags);
    [DllImport("user32.dll")] public static extern bool SetWindowPos(IntPtr h, IntPtr after, int x, int y, int cx, int cy, uint flags);
}
"@

$GWL_EXSTYLE = -20
$WS_EX_LAYERED = 0x80000; $WS_EX_TRANSPARENT = 0x20; $WS_EX_NOACTIVATE = 0x8000000
$HWND_BOTTOM = [IntPtr]1
$done = @{}
$lastUser = [VrWin]::GetForegroundWindow()
$needle = $Repo.ToLower()

while ($true) {
    if ($StopFile -and (Test-Path $StopFile)) { break }
    if ($ParentPid -and -not (Get-Process -Id $ParentPid -ErrorAction SilentlyContinue)) { break }

    $ours = @(Get-CimInstance Win32_Process -Filter "Name='java.exe' OR Name='javaw.exe'" |
        Where-Object { "$($_.CommandLine)".ToLower().Contains($needle) } | ForEach-Object { $_.ProcessId })
    $fg = [VrWin]::GetForegroundWindow()
    $fgIsOurs = $false
    foreach ($procId in $ours) {
        $p = Get-Process -Id $procId -ErrorAction SilentlyContinue
        if (-not $p -or $p.MainWindowHandle -eq [IntPtr]::Zero) { continue }
        $h = $p.MainWindowHandle
        if ($h -eq $fg) { $fgIsOurs = $true }
        if (-not $done.ContainsKey("$h")) {
            $ex = [VrWin]::GetLong($h, $GWL_EXSTYLE).ToInt64() -bor $WS_EX_NOACTIVATE
            if (-not $NoClickThrough) { $ex = $ex -bor $WS_EX_LAYERED -bor $WS_EX_TRANSPARENT }
            [void][VrWin]::SetLong($h, $GWL_EXSTYLE, [IntPtr]$ex)
            if (-not $NoClickThrough) { [void][VrWin]::SetLayeredWindowAttributes($h, 0, 255, 2) }
            [void][VrWin]::EnableWindow($h, $false)
            $done["$h"] = $true
            $ok = ([VrWin]::GetLong($h, $GWL_EXSTYLE).ToInt64() -band $WS_EX_NOACTIVATE) -ne 0
            Log "locked window $h pid $procId title '$($p.MainWindowTitle)' noactivate=$ok"
        }
    }
    if ($fgIsOurs -and $lastUser -ne [IntPtr]::Zero) {
        # The client grabbed focus when it opened. Hand it back to the user's window.
        [void][VrWin]::SetForegroundWindow($lastUser)
        Log "gave focus back to $lastUser"
    } elseif (-not $fgIsOurs -and $fg -ne [IntPtr]::Zero) {
        $lastUser = $fg
    }
    Start-Sleep -Milliseconds 250
}
