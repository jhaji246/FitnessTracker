#!/bin/bash

# Final rename updates script for FitnessTracker project
# This script updates all remaining Runique references to FitnessTracker

echo "🔄 Starting final rename updates..."

# Update remaining Runique references in source files
echo "📝 Updating remaining references in source files..."

# Update package names in remaining files
find . -path "./build*" -prune -o -path "./.gradle*" -prune -o -path "./.kotlin*" -prune -o -name "*.kt" -print | xargs sed -i '' 's/runique/fitnesstracker/g'
find . -path "./build*" -prune -o -path "./.gradle*" -prune -o -path "./.kotlin*" -prune -o -name "*.java" -print | xargs sed -i '' 's/runique/fitnesstracker/g'

# Update XML files
find . -path "./build*" -prune -o -path "./.gradle*" -prune -o -path "./.kotlin*" -prune -o -name "*.xml" -print | xargs sed -i '' 's/runique/fitnesstracker/g'

# Update Gradle files
find . -path "./build*" -prune -o -path "./.gradle*" -prune -o -path "./.kotlin*" -prune -o -name "*.gradle*" -print | xargs sed -i '' 's/runique/fitnesstracker/g'

# Update .idea project name
if [ -f ".idea/.name" ]; then
    echo "  Updating .idea project name..."
    echo "FitnessTracker" > .idea/.name
fi

echo "✅ Final rename updates completed!"
echo ""
echo "📝 Next steps:"
echo "   1. Clean the project: ./gradlew clean"
echo "   2. Rebuild the project: ./gradlew build"
echo ""
echo "🎯 Project has been renamed from 'Runique' to 'FitnessTracker'!"
echo "   - Application ID: com.avi.fitnesstracker"
echo "   - Package structure: com.avi.*"
echo "   - Deep link scheme: fitnesstracker://"
echo "   - App name: FitnessTracker"
