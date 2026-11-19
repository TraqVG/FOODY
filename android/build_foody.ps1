$ErrorActionPreference='Continue'
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.11.10-hotspot'
$root='C:\GitHub\Smart-Restaurant-Ordering\android'
Set-Location $root
'START' | Set-Content "$root\build-status.txt"
& "$root\gradlew.bat" :app:assembleDebug --no-daemon --stacktrace 2>&1 |
    Out-File "$root\build-log.txt" -Encoding UTF8
("EXIT="+$LASTEXITCODE) | Set-Content "$root\build-status.txt"
