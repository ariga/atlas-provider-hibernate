#!/bin/bash

# Script to publish gradle-plugin to local repository

set -e

SCRIPT_DIR="$(dirname "${BASH_SOURCE[0]}")"

echo "🔨 Publishing gradle-plugin..."

# Set version if not provided
if [ -z "$PROVIDER_VERSION" ]; then
    export PROVIDER_VERSION="0.0.0-SNAPSHOT"
fi

echo "📝 Using version: $PROVIDER_VERSION"
cd "$SCRIPT_DIR/../gradle-plugin"
./gradlew clean build publishAllPublicationsToLocalPluginRepositoryRepository
