# SDPTool Build and Run Commands

## Prerequisites

### 1. Java Development Kit (JDK)
- **Required Version**: JDK 11 or higher
- **Check Installation**:
  ```powershell
  java -version
  ```
- **Download**: https://adoptium.net/ (Eclipse Temurin recommended)

### 2. Apache Maven
- **Required Version**: Maven 3.6+ (3.9.11 installed at `C:\apache-maven-3.9.11`)
- **Check Installation**:
  ```powershell
  C:\apache-maven-3.9.11\bin\mvn.cmd -version
  ```
- **Download**: https://maven.apache.org/download.cgi

### 3. Python (for ML/DL components)
- **Required Version**: Python 3.8+
- **Check Installation**:
  ```powershell
  python --version
  ```
- **Install Dependencies**:
  ```powershell
  cd multilable-prediction
  pip install -r requirements.txt
  ```

---

## Build Commands

### Clean Build (Recommended)
Removes previous build artifacts and creates a fresh build:

```powershell
cd "C:\Users\Admin\Downloads\jalaj\thesis\SDPTool-main\SDPTool-main"
C:\apache-maven-3.9.11\bin\mvn.cmd clean package
```

**What it does:**
- `clean`: Deletes the `target/` directory
- `package`: Compiles code, runs tests, and creates JAR files

### Build Without Tests (Faster)
Skip tests during build:

```powershell
cd "C:\Users\Admin\Downloads\jalaj\thesis\SDPTool-main\SDPTool-main"
C:\apache-maven-3.9.11\bin\mvn.cmd clean package -DskipTests
```

### Compile Only (No JAR)
Just compile without packaging:

```powershell
cd "C:\Users\Admin\Downloads\jalaj\thesis\SDPTool-main\SDPTool-main"
C:\apache-maven-3.9.11\bin\mvn.cmd compile
```

### Clean Only
Remove build artifacts:

```powershell
cd "C:\Users\Admin\Downloads\jalaj\thesis\SDPTool-main\SDPTool-main"
C:\apache-maven-3.9.11\bin\mvn.cmd clean
```

---

## Run Commands

### Option 1: Run with Maven (Development)
Run directly using Maven:

```powershell
cd "C:\Users\Admin\Downloads\jalaj\thesis\SDPTool-main\SDPTool-main"
C:\apache-maven-3.9.11\bin\mvn.cmd exec:java
```

**Main Class**: `com.phd.ui.Home` (defined in pom.xml)

### Option 2: Run JAR File (Production)
Run the compiled JAR with all dependencies:

```powershell
cd "C:\Users\Admin\Downloads\jalaj\thesis\SDPTool-main\SDPTool-main"
java -jar target\SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Option 3: Run Simple JAR
Run the basic JAR (requires classpath):

```powershell
cd "C:\Users\Admin\Downloads\jalaj\thesis\SDPTool-main\SDPTool-main"
java -cp "target\SDPTool-1.0-SNAPSHOT.jar;target\lib\*" com.phd.ui.Home
```

---

## Build Artifacts

After successful build, you'll find these files in `target/` directory:

| File | Description | Size (approx) |
|------|-------------|---------------|
| `SDPTool-1.0-SNAPSHOT.jar` | Main application JAR (without dependencies) | ~50 KB |
| `SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar` | **Standalone JAR with all libraries** | ~15 MB |
| `classes/` | Compiled .class files | - |
| `maven-archiver/` | Maven metadata | - |
| `maven-status/` | Build status | - |

**Recommended for Distribution**: `SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar`

---

## Quick Start Script

Create a convenient PowerShell script: `build-and-run.ps1`

```powershell
# build-and-run.ps1
$MAVEN_HOME = "C:\apache-maven-3.9.11"
$PROJECT_DIR = "C:\Users\Admin\Downloads\jalaj\thesis\SDPTool-main\SDPTool-main"

Write-Host "Building SDPTool..." -ForegroundColor Cyan
Set-Location $PROJECT_DIR
& "$MAVEN_HOME\bin\mvn.cmd" clean package -DskipTests

if ($LASTEXITCODE -eq 0) {
    Write-Host "`nBuild successful! Starting SDPTool..." -ForegroundColor Green
    java -jar "target\SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar"
} else {
    Write-Host "`nBuild failed!" -ForegroundColor Red
}
```

**Run it:**
```powershell
.\build-and-run.ps1
```

---

## Adding Maven to PATH (Optional but Recommended)

To use `mvn` command directly without full path:

### Temporary (Current Session)
```powershell
$env:Path += ";C:\apache-maven-3.9.11\bin"
mvn -version
```

### Permanent (System-wide)
1. Open System Properties → Environment Variables
2. Add to System PATH: `C:\apache-maven-3.9.11\bin`
3. Restart PowerShell
4. Test: `mvn -version`

---

## Python ML/DL Model Training

### Train Multilabel Model
```powershell
cd "C:\Users\Admin\Downloads\jalaj\thesis\SDPTool-main\SDPTool-main\multilable-prediction"
python src\main.py
```

### Predict with Saved Model
```powershell
cd "C:\Users\Admin\Downloads\jalaj\thesis\SDPTool-main\SDPTool-main\multilable-prediction"

# Predict from text
python src\predict_with_model.py <run_id> --mode text --text "Fix critical bug"

# Predict from CSV
python src\predict_with_model.py <run_id> --mode csv --csv data\new_data.csv

# Predict specific rows
python src\predict_with_model.py <run_id> --mode rows --dataset data\dataset.csv --rows "1,5,10"
```

---

## Troubleshooting

### Issue: "mvn command not found"
**Solution**: Use full path `C:\apache-maven-3.9.11\bin\mvn.cmd` or add Maven to PATH

### Issue: "JAVA_HOME not set"
**Solution**: 
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-11.0.XX"
```

### Issue: Build fails with dependency errors
**Solution**: 
```powershell
C:\apache-maven-3.9.11\bin\mvn.cmd clean install -U
# -U forces update of dependencies
```

### Issue: "Class not found" when running JAR
**Solution**: Use the `-jar-with-dependencies.jar` version

### Issue: Out of memory during build
**Solution**: 
```powershell
$env:MAVEN_OPTS = "-Xmx1024m"
C:\apache-maven-3.9.11\bin\mvn.cmd clean package
```

---

## Development Workflow

### 1. Make Code Changes
Edit Java files in `src/main/java/`

### 2. Rebuild
```powershell
C:\apache-maven-3.9.11\bin\mvn.cmd clean package -DskipTests
```

### 3. Test
```powershell
java -jar target\SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### 4. Debug
Add debug flag:
```powershell
java -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 -jar target\SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar
```

---

## CI/CD Pipeline (Future)

### GitHub Actions Workflow
```yaml
name: Build SDPTool
on: [push]
jobs:
  build:
    runs-on: windows-latest
    steps:
      - uses: actions/checkout@v2
      - uses: actions/setup-java@v2
        with:
          java-version: '11'
      - run: mvn clean package
      - uses: actions/upload-artifact@v2
        with:
          name: SDPTool-JAR
          path: target/*.jar
```

---

## Project Structure

```
SDPTool-main/
├── src/
│   └── main/
│       ├── java/com/phd/
│       │   ├── ui/          (Swing GUI panels)
│       │   ├── domain/      (Data models)
│       │   ├── db/          (Database layer)
│       │   ├── config/      (Configuration)
│       │   ├── issue/       (GitHub API)
│       │   └── data/        (Data processing)
│       └── resources/
├── multilable-prediction/   (Python ML/DL)
├── target/                  (Build output)
├── pom.xml                  (Maven configuration)
└── README.md
```

---

## Maven Lifecycle Phases

Common phases you can use:

- `mvn validate` - Validate project structure
- `mvn compile` - Compile source code
- `mvn test` - Run unit tests
- `mvn package` - Create JAR files
- `mvn verify` - Run integration tests
- `mvn install` - Install JAR to local Maven repository
- `mvn deploy` - Deploy to remote repository
- `mvn clean` - Remove target directory
- `mvn site` - Generate project documentation

---

## Performance Tips

### Faster Builds
```powershell
# Skip tests and javadoc
C:\apache-maven-3.9.11\bin\mvn.cmd clean package -DskipTests -Dmaven.javadoc.skip=true

# Parallel builds (use multiple CPU cores)
C:\apache-maven-3.9.11\bin\mvn.cmd -T 4 clean package

# Offline mode (no dependency updates)
C:\apache-maven-3.9.11\bin\mvn.cmd -o package
```

### Memory Settings
```powershell
$env:MAVEN_OPTS = "-Xms512m -Xmx2048m -XX:MaxPermSize=512m"
```

---

## Version Information

- **Project**: SDPTool 1.0-SNAPSHOT
- **Java**: 11 (source/target)
- **Maven**: 3.9.11
- **Main Class**: com.phd.ui.Home
- **Artifact**: SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar

---

## Quick Reference

```powershell
# Clean and build
C:\apache-maven-3.9.11\bin\mvn.cmd clean package

# Run application
java -jar target\SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar

# Check JAR contents
jar tf target\SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar | Select-String "com.phd"

# View dependencies
C:\apache-maven-3.9.11\bin\mvn.cmd dependency:tree

# Update dependencies
C:\apache-maven-3.9.11\bin\mvn.cmd versions:display-dependency-updates
```

---

**Last Updated**: December 5, 2025  
**Build System**: Apache Maven 3.9.11  
**Java Version**: JDK 11+
