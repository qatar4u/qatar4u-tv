# Installs dist\Qatar4uTV-*.apk on the tablet over ADB and makes it the Home app.
#   powershell -ExecutionPolicy Bypass -File tools\install-windows.ps1 [-Adb C:\platform-tools\adb.exe] [-NoHome]
param([string]$Adb = "adb", [switch]$NoHome)
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$apk = Get-ChildItem "$root\dist\Qatar4uTV-*.apk" | Sort-Object LastWriteTime | Select-Object -Last 1
if (-not $apk) { throw "No APK in dist. Run tools\build-windows.ps1 first." }
$state = & $Adb get-state 2>$null
if ($state -ne "device") { throw "No tablet over ADB. Check the cable and allow USB debugging on the tablet." }
"Home before: " + (& $Adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | Select-Object -Last 1)
& $Adb install -r $apk.FullName
if (-not $NoHome) {
    & $Adb shell cmd package set-home-activity net.qatar4u.tv/.MainActivity
    & $Adb shell input keyevent KEYCODE_HOME
}
"Home now: " + (& $Adb shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME | Select-Object -Last 1)
