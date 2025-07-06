#!/bin/bash

# Script to publish hibernate-provider library to local repository

set -e

SCRIPT_DIR="$(dirname "${BASH_SOURCE[0]}")"

echo "🔨 Publishing hibernate-provider library..."

# Set version if not provided
if [ -z "$PROVIDER_VERSION" ]; then
    export PROVIDER_VERSION="0.0.0-SNAPSHOT"
fi

echo "📝 Using version: $PROVIDER_VERSION"
cd "$SCRIPT_DIR/../hibernate-provider" 
./gradlew clean build publishAllPublicationsToLocalPluginRepositoryRepository
