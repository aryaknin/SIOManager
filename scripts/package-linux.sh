#!/usr/bin/env bash

set -euo pipefail

PACKAGE_TYPE="${1:-app-image}"
if [[ "$PACKAGE_TYPE" != "app-image" && "$PACKAGE_TYPE" != "deb" ]]; then
    echo "Usage: $0 [app-image|deb]" >&2
    exit 1
fi

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIRECTORY="$(cd "$SCRIPT_DIRECTORY/.." && pwd)"
cd "$PROJECT_DIRECTORY"

./mvnw clean package
cp target/SIOManager-1.1.0-SNAPSHOT.jar target/package-input/
cp -R demo-content target/package-input/demo-content

JPACKAGE_COMMAND="jpackage"
if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/jpackage" ]]; then
    JPACKAGE_COMMAND="$JAVA_HOME/bin/jpackage"
fi

if ! command -v "$JPACKAGE_COMMAND" >/dev/null 2>&1; then
    echo "jpackage est introuvable. Utilisez un JDK 25 complet et définissez JAVA_HOME." >&2
    exit 1
fi

JPACKAGE_OPTIONS=(
    --type "$PACKAGE_TYPE"
    --name SIOManager
    --app-version 1.1.0
    --vendor "SIOManager"
    --description "Espace de ressources et de révision pour BTS SIO"
    --input target/package-input
    --main-jar SIOManager-1.1.0-SNAPSHOT.jar
    --main-class com.example.siomanager.Launcher
    --java-options "--enable-native-access=javafx.graphics,javafx.web,org.xerial.sqlitejdbc"
    --java-options "-Dsiomanager.sharedResourcesTemplate=\$APPDIR/demo-content"
    --dest target/installer
)

if [[ "$PACKAGE_TYPE" == "deb" ]]; then
    JPACKAGE_OPTIONS+=(--linux-menu-group "Education" --linux-shortcut)
fi

"$JPACKAGE_COMMAND" "${JPACKAGE_OPTIONS[@]}"

echo "Paquet créé dans target/installer/"
