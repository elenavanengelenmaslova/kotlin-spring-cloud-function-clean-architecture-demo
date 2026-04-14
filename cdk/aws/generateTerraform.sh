#!/bin/bash
set -e

# Force Java 21 for this build
export JAVA_HOME=/Users/e.maslova/Library/Java/JavaVirtualMachines/openjdk-21.0.1/Contents/Home

# Step 1: Go to project root (this assumes script is in scripts/)
cd "$(dirname "$0")/../.."

# Step 2: Build the Lambda JAR
echo "🔨 Building Lambda JAR..."
./gradlew clean :infra-aws:build :infra-aws:shadowJar --no-build-cache

# Step 3: Generate Terraform files
echo "📦 Generating Terraform files from CDK..."
cd cdk/aws
cdktf get
cdktf synth

echo "✅ Terraform generated in: cdk/aws/cdktf.out/stacks"

# Step 4: Clean up not needed files
echo "Cleaning"

rm -rf src/main/java
rm -rf src/main/resources

echo "✅ Done!"
