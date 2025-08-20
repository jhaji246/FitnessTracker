#!/bin/bash

# Directory renaming script for FitnessTracker project
# This script renames directories from com/plcoding to com/avi

echo "🔄 Starting directory structure renaming..."

# Function to rename directories recursively
rename_directories() {
    local base_dir="$1"
    
    if [[ -d "$base_dir" ]]; then
        echo "📁 Processing: $base_dir"
        
        # Find all com/plcoding directories and rename them to com/avi
        find "$base_dir" -type d -path "*/com/plcoding" | while read -r old_path; do
            new_path="${old_path%/plcoding}/avi"
            echo "  Renaming: $old_path -> $new_path"
            
            # Create parent directory if it doesn't exist
            mkdir -p "$(dirname "$new_path")"
            
            # Move the directory
            mv "$old_path" "$new_path"
        done
    fi
}

# Main renaming process
echo "🔍 Starting directory renaming..."

# Rename directories in main source directories
rename_directories "app/src"
rename_directories "analytics/src"
rename_directories "auth/src"
rename_directories "core/src"
rename_directories "run/src"
rename_directories "wear/src"

# Rename directories in build-logic
rename_directories "build-logic"

echo "✅ Directory renaming completed!"
echo ""
echo "📝 Next steps:"
echo "   1. Clean the project: ./gradlew clean"
echo "   2. Rebuild the project: ./gradlew build"
echo ""
echo "⚠️  Important: After renaming directories, you may need to:"
echo "   - Update any hardcoded file paths in your IDE"
echo "   - Refresh your IDE project view"
echo "   - Update any import statements that reference the old paths"
