#!/usr/bin/env bash

set -euo pipefail

source_directory="${1:-./documents}"
destination_directory="${2:-./sauvegardes}"

mkdir -p "$destination_directory"
cp -R "$source_directory" "$destination_directory"

echo "Sauvegarde terminée."
