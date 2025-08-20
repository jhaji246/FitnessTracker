#!/bin/bash

# Package refactoring script for FitnessTracker project
# This script refactors all package names from com.plcoding to com.avi

echo "🔄 Starting package refactoring from com.plcoding to com.avi..."

# Function to refactor files
refactor_file() {
    local file="$1"
    if [[ -f "$file" ]]; then
        echo "  Refactoring: $file"
        # Replace package declarations
        sed -i '' 's/package com\.plcoding/package com.avi/g' "$file"
        # Replace import statements
        sed -i '' 's/import com\.plcoding/import com.avi/g' "$file"
        # Replace any remaining references in strings or comments
        sed -i '' 's/com\.plcoding/com.avi/g' "$file"
    fi
}

# Function to refactor directories
refactor_directory() {
    local dir="$1"
    if [[ -d "$dir" ]]; then
        echo "📁 Processing directory: $dir"
        
        # Find all Java and Kotlin files
        find "$dir" -name "*.kt" -o -name "*.java" | while read -r file; do
            refactor_file "$file"
        done
        
        # Find all XML files that might contain package references
        find "$dir" -name "*.xml" | while read -r file; do
            if [[ -f "$file" ]]; then
                echo "  Refactoring XML: $file"
                sed -i '' 's/com\.plcoding/com.avi/g' "$file"
            fi
        done
        
        # Find all Gradle files
        find "$dir" -name "*.gradle" -o -name "*.gradle.kts" | while read -r file; do
            if [[ -f "$file" ]]; then
                echo "  Refactoring Gradle: $file"
                sed -i '' 's/com\.plcoding/com.avi/g' "$file"
            fi
        done
    fi
}

# Main refactoring process
echo "🔍 Starting comprehensive package refactoring..."

# Refactor main source directories
refactor_directory "app/src"
refactor_directory "analytics/src"
refactor_directory "auth/src"
refactor_directory "core/src"
refactor_directory "run/src"
refactor_directory "wear/src"

# Refactor build-logic
refactor_directory "build-logic"

# Refactor test directories
refactor_directory "app/src/test"
refactor_directory "analytics/src/test"
refactor_directory "auth/src/test"
refactor_directory "core/src/test"
refactor_directory "run/src/test"
refactor_directory "wear/src/test"

echo "✅ Package refactoring completed!"
echo ""
echo "📝 Next steps:"
echo "   1. Review the changes to ensure they're correct"
echo "   2. Update any hardcoded package references that might have been missed"
echo "   3. Clean and rebuild the project: ./gradlew clean build"
echo ""
echo "⚠️  Important: After refactoring, you may need to:"
echo "   - Update any hardcoded package references in strings or resources"
echo "   - Update any database schemas or migration files"
echo "   - Update any Firebase or other service configurations"
echo "   - Update any deep link configurations"
