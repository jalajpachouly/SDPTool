# SDPTool Build and Run Script
# Quick build and run script for SDPTool

$ErrorActionPreference = "Stop"

# Configuration
$MAVEN_HOME = "C:\apache-maven-3.9.11"
$PROJECT_DIR = "C:\Users\Admin\Downloads\jalaj\thesis\SDPTool-main\SDPTool-main"
$MAVEN_CMD = "$MAVEN_HOME\bin\mvn.cmd"

# Colors
function Write-ColorOutput($ForegroundColor, $Message) {
    Write-Host $Message -ForegroundColor $ForegroundColor
}

# Banner
Write-ColorOutput Cyan @"
╔═══════════════════════════════════════════╗
║         SDPTool Build & Run Script        ║
║    Software Defect Prediction Tool        ║
╚═══════════════════════════════════════════╝
"@

# Check Maven
Write-ColorOutput Yellow "`n[1/4] Checking Maven..."
if (-not (Test-Path $MAVEN_CMD)) {
    Write-ColorOutput Red "ERROR: Maven not found at $MAVEN_CMD"
    Write-ColorOutput Yellow "Please install Maven or update the path in this script"
    exit 1
}
Write-ColorOutput Green "✓ Maven found: $MAVEN_HOME"

# Check Java
Write-ColorOutput Yellow "`n[2/4] Checking Java..."
try {
    $javaVersion = java -version 2>&1 | Select-Object -First 1
    Write-ColorOutput Green "✓ Java found: $javaVersion"
} catch {
    Write-ColorOutput Red "ERROR: Java not found. Please install JDK 11+"
    exit 1
}

# Navigate to project
Write-ColorOutput Yellow "`n[3/4] Building project..."
Set-Location $PROJECT_DIR

# Build
Write-ColorOutput Cyan "Running: mvn clean package -DskipTests"
Write-ColorOutput Gray "This may take a few minutes on first build..."

try {
    & $MAVEN_CMD clean package -DskipTests
    
    if ($LASTEXITCODE -ne 0) {
        throw "Build failed with exit code $LASTEXITCODE"
    }
    
    Write-ColorOutput Green "`n✓ Build successful!"
    
    # Check artifacts
    $jarFile = Get-Item "target\SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar" -ErrorAction Stop
    $jarSizeMB = [math]::Round($jarFile.Length / 1MB, 2)
    
    Write-ColorOutput Green @"
    
Build Artifacts:
  File: SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar
  Size: $jarSizeMB MB
  Location: $($jarFile.FullName)
"@
    
    # Run application
    Write-ColorOutput Yellow "`n[4/4] Starting SDPTool..."
    Write-ColorOutput Cyan "Running: java -jar target\SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar"
    Write-ColorOutput Gray "`nPress Ctrl+C to stop the application`n"
    
    java -jar "target\SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar"
    
} catch {
    Write-ColorOutput Red "`n✗ Error: $_"
    Write-ColorOutput Yellow "`nBuild failed. Check the error messages above."
    Write-ColorOutput Yellow "Common issues:"
    Write-ColorOutput Gray "  - Network issues downloading dependencies"
    Write-ColorOutput Gray "  - Java version mismatch (need JDK 11+)"
    Write-ColorOutput Gray "  - Compilation errors in source code"
    exit 1
}

Write-ColorOutput Green "`nSDPTool closed successfully!"
