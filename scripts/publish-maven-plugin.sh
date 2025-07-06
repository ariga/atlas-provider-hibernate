#!/bin/bash

# Script to publish maven-plugin to local repository

set -e

# Get the script directory to ensure we're working from the correct location
SCRIPT_DIR="$(dirname "${BASH_SOURCE[0]}")"

echo "🔨 Publishing maven-plugin..."

# Set version if not provided
if [ -z "$PROVIDER_VERSION" ]; then
    export PROVIDER_VERSION="0.0.0-SNAPSHOT"
fi

echo "📝 Using version: $PROVIDER_VERSION"
cd "$SCRIPT_DIR/../maven-plugin"
mvn clean deploy -Plocal -Dprovider.version="$PROVIDER_VERSION"
