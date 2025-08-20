#!/bin/bash

# Setup script for FitnessTracker project
# This script helps you set up your local.properties file

echo "🚀 Setting up FitnessTracker project configuration..."

# Check if local.properties already exists
if [ -f "local.properties" ]; then
    echo "⚠️  local.properties already exists!"
    echo "   If you want to start fresh, delete it first and run this script again."
    exit 1
fi

# Check if template exists
if [ ! -f "local.properties.template" ]; then
    echo "❌ local.properties.template not found!"
    echo "   Make sure you're running this script from the project root directory."
    exit 1
fi

# Copy template to local.properties
cp local.properties.template local.properties

echo "✅ Created local.properties from template"
echo ""
echo "📝 Next steps:"
echo "   1. Edit local.properties and add your actual API keys:"
echo "      - API_KEY: Your FitnessTracker API key (from course purchase)"
echo "      - MAPS_API_KEY: Your Google Maps API key (from Google Cloud Console)"
echo "      - sdk.dir: Your Android SDK path"
echo ""
echo "   2. Build the project: ./gradlew build"
echo ""
echo "🔑 Need help getting API keys?"
echo "   - FitnessTracker API: Access granted after course purchase"
echo "   - Google Maps API: Get from Google Cloud Console (instructions in course)"
echo ""
echo "📚 For detailed setup instructions, see README.md"
