# Makefile for Atlas Hibernate Provider

# Default version
PROVIDER_VERSION ?= 0.0.0-SNAPSHOT

# Colors for output
YELLOW = \033[1;33m
GREEN = \033[1;32m
BLUE = \033[1;34m
NC = \033[0m # No Color

.PHONY: help publish-all publish-library publish-gradle-plugin publish-maven-plugin clean

# Default target
help:
	@echo "$(BLUE)Atlas Hibernate Provider Build Commands$(NC)"
	@echo ""
	@echo "$(YELLOW)Publishing:$(NC)"
	@echo "  make publish-all        - Publish all components (library, gradle-plugin, maven-plugin)"
	@echo "  make publish-library    - Publish hibernate-provider library only"
	@echo "  make publish-gradle     - Publish gradle-plugin only"
	@echo "  make publish-maven      - Publish maven-plugin only"
	@echo ""
	@echo "$(YELLOW)Environment Variables:$(NC)"
	@echo "  PROVIDER_VERSION        - Set version (default: $(PROVIDER_VERSION))"
	@echo ""
	@echo "$(YELLOW)Example:$(NC)"
	@echo "  PROVIDER_VERSION=1.0.0 make publish-all"

# Publish library only
publish-library-locally:
	@echo "$(GREEN)Publishing hibernate-provider library with version: $(PROVIDER_VERSION)$(NC)"
	@PROVIDER_VERSION=$(PROVIDER_VERSION) ./scripts/publish-library-locally.sh

# Publish gradle plugin only (depends on library)
publish-gradle-locally: publish-library-locally
	@echo "$(GREEN)Publishing gradle-plugin with version: $(PROVIDER_VERSION)$(NC)"
	@PROVIDER_VERSION=$(PROVIDER_VERSION) ./scripts/publish-gradle-plugin-locally.sh
	@echo "🧪 To test the plugin:"
	@echo "  Gradle: cd examples/with_local_plugin_repository && ./gradlew schema"

# Publish maven plugin only
publish-maven-locally: publish-library-locally
	@echo "$(GREEN)Publishing maven-plugin with version: $(PROVIDER_VERSION)$(NC)"
	@PROVIDER_VERSION=$(PROVIDER_VERSION) ./scripts/publish-maven-plugin-locally.sh
	@echo "🧪 To test the plugin:"
	@echo "  Maven: cd examples/maven_project_example/allowed_types && mvn -q compile hibernate-provider:schema"

# Publish all components (depends on all individual targets)
publish-locally: publish-library-locally publish-gradle-locally publish-maven-locally
	@echo "$(GREEN)✅ All components published to local repository!$(NC)"

build:
	@echo "$(YELLOW)Building all components...$(NC)"
	@cd hibernate-provider && mvn clean package
	@cd gradle-plugin && ./gradlew build
	@cd maven-plugin && mvn clean install
	@echo "$(GREEN)All components built successfully!$(NC)"

# Clean all build artifacts and local repository
clean:
	@echo "$(YELLOW)Cleaning build artifacts...$(NC)"
	@rm -rf .local-plugin-repository
	@cd hibernate-provider && mvn clean
	@cd gradle-plugin && ./gradlew clean
	@cd maven-plugin && mvn clean
	@echo "$(GREEN)All build artifacts cleaned!$(NC)"
