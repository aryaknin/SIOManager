$ErrorActionPreference = "Stop"

$ProjectDirectory = Split-Path -Parent $PSScriptRoot
Set-Location $ProjectDirectory

.\mvnw.cmd clean package
Copy-Item "target\SIOManager-1.1.0-SNAPSHOT.jar" "target\package-input\" -Force
Copy-Item "demo-content" "target\package-input\demo-content" -Recurse -Force

$JpackageCommand = "jpackage"
if ($env:JAVA_HOME -and (Test-Path "$env:JAVA_HOME\bin\jpackage.exe")) {
    $JpackageCommand = "$env:JAVA_HOME\bin\jpackage.exe"
}

& $JpackageCommand `
    --type exe `
    --name SIOManager `
    --app-version 1.1.0 `
    --vendor "SIOManager" `
    --description "Espace de ressources et de revision pour BTS SIO" `
    --input target\package-input `
    --main-jar SIOManager-1.1.0-SNAPSHOT.jar `
    --main-class com.example.siomanager.Launcher `
    --java-options "--enable-native-access=javafx.graphics,javafx.web,org.xerial.sqlitejdbc" `
    --java-options '-Dsiomanager.sharedResourcesTemplate=$APPDIR/demo-content' `
    --dest target\installer `
    --win-menu `
    --win-shortcut `
    --win-dir-chooser

Write-Host "Installateur créé dans target\installer\"
