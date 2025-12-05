# Quick Start Guide for SDPTool

## ⚡ Fastest Way to Build and Run

### Option 1: PowerShell Script (Recommended)
```powershell
cd "C:\Users\Admin\Downloads\jalaj\thesis\SDPTool-main\SDPTool-main"
.\build-and-run.ps1
```

### Option 2: Batch File
```cmd
cd "C:\Users\Admin\Downloads\jalaj\thesis\SDPTool-main\SDPTool-main"
build-and-run.bat
```

### Option 3: Manual Commands
```powershell
# Build
cd "C:\Users\Admin\Downloads\jalaj\thesis\SDPTool-main\SDPTool-main"
C:\apache-maven-3.9.11\bin\mvn.cmd clean package -DskipTests

# Run
java -jar target\SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar
```

---

## 📁 Files Created

| File | Purpose | Usage |
|------|---------|-------|
| `BUILD_AND_RUN.md` | Complete documentation | Reference guide |
| `build-and-run.ps1` | PowerShell automation script | `.\build-and-run.ps1` |
| `build-and-run.bat` | Batch automation script | `build-and-run.bat` |
| `QUICK_START.md` | This file - quick reference | - |

---

## 🎯 What Gets Built

After successful build, find these in `target/` directory:

- **`SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar`** ← Use this one!
  - Standalone executable JAR (~15 MB)
  - Includes all dependencies
  - Ready to distribute

- `SDPTool-1.0-SNAPSHOT.jar`
  - Main JAR only (~50 KB)
  - Requires external libraries

---

## 🔧 Common Commands

### Build Commands
```powershell
# Full build with tests
C:\apache-maven-3.9.11\bin\mvn.cmd clean package

# Fast build (skip tests)
C:\apache-maven-3.9.11\bin\mvn.cmd clean package -DskipTests

# Clean only
C:\apache-maven-3.9.11\bin\mvn.cmd clean

# Just compile
C:\apache-maven-3.9.11\bin\mvn.cmd compile
```

### Run Commands
```powershell
# Run with Maven (development)
C:\apache-maven-3.9.11\bin\mvn.cmd exec:java

# Run JAR (production)
java -jar target\SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar
```

---

## 🚀 First Time Setup

1. **Check Java**:
   ```powershell
   java -version
   # Should show Java 11 or higher
   ```

2. **Check Maven**:
   ```powershell
   C:\apache-maven-3.9.11\bin\mvn.cmd -version
   ```

3. **Build** (first time takes longer):
   ```powershell
   cd "C:\Users\Admin\Downloads\jalaj\thesis\SDPTool-main\SDPTool-main"
   C:\apache-maven-3.9.11\bin\mvn.cmd clean package
   ```

4. **Run**:
   ```powershell
   java -jar target\SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar
   ```

---

## 🐍 Python ML Components

### Setup Python Environment
```powershell
cd multilable-prediction
pip install -r requirements.txt
```

### Train Models
```powershell
cd multilable-prediction
python src\main.py
```

### Make Predictions
```powershell
# From text
python src\predict_with_model.py <run_id> --mode text --text "Fix bug"

# From CSV
python src\predict_with_model.py <run_id> --mode csv --csv data\new.csv

# From specific rows
python src\predict_with_model.py <run_id> --mode rows --dataset data\dataset.csv --rows "1,5,10"
```

---

## ❗ Troubleshooting

### "mvn command not found"
Use full path: `C:\apache-maven-3.9.11\bin\mvn.cmd`

### "JAVA_HOME not set"
```powershell
$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-11.0.XX"
```

### Build fails
```powershell
# Force update dependencies
C:\apache-maven-3.9.11\bin\mvn.cmd clean install -U
```

### Can't run JAR
Make sure you're using the `-jar-with-dependencies.jar` version

---

## 📊 Build Time Estimates

- **First build**: 2-5 minutes (downloads dependencies)
- **Subsequent builds**: 30-60 seconds
- **Incremental builds**: 10-20 seconds

---

## 💡 Pro Tips

1. **Faster builds**: Use `-DskipTests` flag
2. **Parallel builds**: Use `-T 4` flag (4 threads)
3. **Offline mode**: Use `-o` if dependencies already downloaded
4. **Clean slate**: Delete `target/` folder if issues occur

---

## 📦 Distribution

To share SDPTool with others, distribute:

```
SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar
```

They can run it with:
```powershell
java -jar SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar
```

No other files needed! (except for Python ML if using that feature)

---

## 📚 More Information

- Full documentation: `BUILD_AND_RUN.md`
- Model persistence docs: `multilable-prediction\MODEL_PERSISTENCE_IMPLEMENTATION.md`
- Modification guide: `multilable-prediction\MODIFICATION_GUIDE.py`

---

**Quick Access Command** (bookmark this):
```powershell
cd "C:\Users\Admin\Downloads\jalaj\thesis\SDPTool-main\SDPTool-main"; C:\apache-maven-3.9.11\bin\mvn.cmd clean package -DskipTests; java -jar target\SDPTool-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Copy-paste this into PowerShell for one-command build-and-run!
