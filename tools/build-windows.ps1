# Builds Qatar4u TV on Windows and writes dist\Qatar4uTV-<version>.apk and dist\Qatar4uTV_Rollback.zip.
#   powershell -ExecutionPolicy Bypass -File tools\build-windows.ps1 [-Packages C:\path\packages.txt]
# Needs the Android SDK (Android Studio installs it) and a JDK 17 or newer (Android Studio's own is used).
param([string]$Packages = "")
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (-not $env:JAVA_HOME -or -not (Test-Path "$env:JAVA_HOME\bin\java.exe")) {
    foreach ($j in "C:\Program Files\Android\Android Studio\jbr", "C:\Program Files\Android\Android Studio\jre") {
        if (Test-Path "$j\bin\java.exe") { $env:JAVA_HOME = $j; break }
    }
}
if (-not $env:JAVA_HOME) { throw "No JDK found. Install Android Studio or set JAVA_HOME." }

$sdk = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { "$env:LOCALAPPDATA\Android\Sdk" }
if (-not (Test-Path "$sdk\platforms")) { throw "No Android SDK at $sdk. Set ANDROID_HOME." }
"sdk.dir=$($sdk -replace '\\','/')" | Out-File -Encoding ascii local.properties

& .\gradlew.bat --no-daemon testDebugUnitTest assembleRelease
if ($LASTEXITCODE -ne 0) { throw "Gradle failed ($LASTEXITCODE)" }

$version = (Select-String -Path app\build.gradle.kts -Pattern 'versionName = "([^"]+)"').Matches[0].Groups[1].Value
New-Item -ItemType Directory -Force dist | Out-Null
Copy-Item app\build\outputs\apk\release\app-release.apk "dist\Qatar4uTV-$version.apk" -Force

# Rollback package: the docs from rollback\ plus the package list taken before any change.
$tmp = Join-Path $env:TEMP "Qatar4uTV_Rollback"
Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
New-Item -ItemType Directory $tmp | Out-Null
Copy-Item rollback\* $tmp
if ($Packages -and (Test-Path $Packages)) { Copy-Item $Packages "$tmp\Installed_Packages.txt" -Force }
elseif (-not (Test-Path "$tmp\Installed_Packages.txt")) { "Run: adb shell pm list packages -f > Installed_Packages.txt" | Out-File -Encoding utf8 "$tmp\Installed_Packages.txt" }
Compress-Archive -Path "$tmp\*" -DestinationPath dist\Qatar4uTV_Rollback.zip -Force

Get-ChildItem dist | Format-Table Name, Length
