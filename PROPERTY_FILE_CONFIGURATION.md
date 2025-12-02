# Property File Based Database Configuration

## Overview

The database configuration now uses a **property file system** with these features:

✅ **Default relative path** (`./data/sdp.db`) stored in `database.properties`  
✅ **Auto-loaded in UI** on application start  
✅ **User can override** via UI Browse button or manual edit  
✅ **Automatic save** when user changes location - written back to `database.properties`  
✅ **Relative or absolute paths** supported  

---

## How It Works

### 1. Default Configuration (database.properties)

**Location:** `src/main/resources/database.properties`

```properties
# Default database location - relative path
database.location=./data/sdp.db

# Auto-create database if not exists
database.auto.create=true

# Auto-create schema tables if not exists
database.auto.init.schema=true
```

**Relative Path Behavior:**
- `./data/sdp.db` → Creates `data` folder in your project directory
- If project is at `C:\wspace\SDPTool\`, database will be at `C:\wspace\SDPTool\data\sdp.db`

---

### 2. Property Loading Flow

```
Application Start
    ↓
DatabaseProperties class loads
    ↓
Check: database.properties in working directory?
    ├─ Yes → Load from working directory (user override)
    └─ No  → Load from classpath (default)
          ↓
          Copy to working directory for future edits
    ↓
Convert relative paths to absolute
    ↓
Display in UI Configuration tab
```

---

### 3. User Override Flow

```
User opens Configuration tab
    ↓
Database File field shows: C:\wspace\SDPTool\data\sdp.db
    ↓
User clicks "Browse..." or manually edits
    ↓
User selects: D:\MyResearch\project.db
    ↓
User clicks "Save Configuration"
    ↓
DatabaseProperties.setDatabaseLocation() called
    ↓
database.properties file UPDATED in working directory:
    database.location=D:\MyResearch\project.db
    ↓
Confirmation dialog shown
    ↓
Next time app starts → D:\MyResearch\project.db is default
```

---

## File Locations

### During Development (Maven):
```
SDPTool/
├── src/main/resources/
│   └── database.properties          ← Original (never modified)
├── database.properties              ← Created in working directory (user overrides)
└── data/
    └── sdp.db                        ← Default database location
```

### After Running:
```
SDPTool/
├── database.properties              ← User's overridden properties
│   # Updated content:
│   # database.location=D:/MyResearch/project.db
├── data/
│   └── sdp.db                        ← If using default
└── D:/MyResearch/
    └── project.db                    ← If user overrode
```

---

## Usage Examples

### Example 1: First Time User (Uses Default)

1. Launch app: `mvn exec:java`
2. Configuration tab opens
3. Database File shows: `C:\wspace\SDPTool\data\sdp.db`
4. Click "Populate SDP Dataset"
5. Console:
   ```
   Loaded default database properties from classpath
   Created properties file in working directory: C:\wspace\SDPTool\database.properties
   No database location provided, using property file default: C:\wspace\SDPTool\data\sdp.db
   Created database directory: C:\wspace\SDPTool\data
   Database schema created successfully!
   ```

### Example 2: User Overrides Location

1. Launch app
2. Configuration tab → Database File: `C:\wspace\SDPTool\data\sdp.db`
3. Click "Browse..."
4. Navigate to `D:\Research\` 
5. Type filename: `github_issues.db`
6. Click Save
7. Field now shows: `D:\Research\github_issues.db`
8. Click "Save Configuration"
9. Dialog appears: 
   ```
   Database location updated successfully!
   New location: D:\Research\github_issues.db
   Saved to: C:\wspace\SDPTool\database.properties
   ```
10. `database.properties` file updated:
    ```properties
    database.location=D:\\Research\\github_issues.db
    ```
11. Next app launch → `D:\Research\github_issues.db` is default

### Example 3: Keep Relative Path

User wants database to stay with project (portable):

1. Configuration tab → Database File
2. Manually edit to: `./mydata/research.db`
3. Click "Save Configuration"
4. Properties updated:
   ```properties
   database.location=./mydata/research.db
   ```
5. Database created at: `C:\wspace\SDPTool\mydata\research.db`
6. If project moved to `D:\NewLocation\SDPTool\`, database will be at `D:\NewLocation\SDPTool\mydata\research.db`

---

## API Reference

### DatabaseProperties Class

#### Key Methods:

```java
// Get database location (converts relative to absolute)
String location = DatabaseProperties.getDatabaseLocation();
// Returns: C:\wspace\SDPTool\data\sdp.db

// Get raw value from properties (may be relative)
String raw = DatabaseProperties.getRawDatabaseLocation();
// Returns: ./data/sdp.db

// Update location and save to file
boolean success = DatabaseProperties.setDatabaseLocation("D:/NewPath/db.db");

// Reload properties from disk
DatabaseProperties.reload();

// Get properties file path
String path = DatabaseProperties.getPropertiesFilePath();
// Returns: C:\wspace\SDPTool\database.properties

// Get default location
String defaultPath = DatabaseProperties.getDefaultDatabaseLocation();
// Returns: ./data/sdp.db
```

### Connect Class Changes

```java
// Old behavior:
Connection conn = Connect.getConnection(null);
// Used hardcoded: C:/DB/SDP1.db ❌

// New behavior:
Connection conn = Connect.getConnection(null);
// Uses DatabaseProperties.getDatabaseLocation() ✅
// Returns connection to: ./data/sdp.db (relative to project)
```

---

## Configuration Tab Features

### UI Enhancements:

1. **Auto-load Default:**
   - On tab open, database field populated from properties file
   - Tooltip shows raw property value

2. **Smart Browse Dialog:**
   - Opens at current database location's directory
   - Allows save (create new file)
   - Auto-adds `.db` extension if missing

3. **Save Confirmation:**
   - Dialog shows when location saved to properties
   - Displays absolute path and properties file location

4. **Visual Feedback:**
   ```
   Database File: [D:\Research\project.db           ] [Browse...]
   Tooltip: "Default from database.properties: ./data/sdp.db"
   ```

---

## Path Types Supported

### Relative Paths (Portable):
```properties
# Relative to project directory
database.location=./data/sdp.db
database.location=./databases/production.db
database.location=../shared/team_data.db
```

**Pros:** Project portable, works on any machine  
**Cons:** Database moves if project moves

### Absolute Paths (Fixed):
```properties
# Windows absolute
database.location=C:/DB/SDP1.db
database.location=D:/Research/Data/github_issues.db

# Unix/Linux absolute
database.location=/home/user/databases/sdp.db
database.location=/var/data/research/project.db
```

**Pros:** Database stays in one place  
**Cons:** Not portable, different per machine

---

## Advanced Scenarios

### Scenario 1: Team Shared Configuration

**Problem:** Team wants same relative structure  
**Solution:** Commit `database.properties` with relative path

```properties
database.location=./shared_data/team.db
```

Each developer gets database at their local `shared_data/team.db`

### Scenario 2: Per-Environment Configuration

**Development:**
```properties
database.location=./data/dev.db
```

**Production:**
```properties
database.location=D:/Production/Data/sdp.db
```

Use different `database.properties` files per environment.

### Scenario 3: Multiple Projects

```
SDPTool/
├── database.properties → database.location=./data/project1.db
└── data/project1.db

# User overrides for project 2
# Edit properties manually:
database.location=./data/project2.db
```

### Scenario 4: Network Drive (Caution)

```properties
# Windows network path
database.location=//SERVER/Share/databases/sdp.db

# Mapped drive
database.location=Z:/databases/sdp.db
```

**Note:** SQLite on network drives can have locking issues.

---

## Troubleshooting

### Problem: Properties Not Saving

**Symptom:** User overrides in UI, but next launch uses default

**Check:**
```bash
# Is properties file in working directory?
dir database.properties

# Check permissions
icacls database.properties
```

**Solution:**
- Ensure working directory is writable
- Run as administrator if needed
- Check console for "Failed to save database properties"

### Problem: Relative Path Not Resolved

**Symptom:** Database created in unexpected location

**Debug:**
```java
System.out.println("Working directory: " + System.getProperty("user.dir"));
System.out.println("Absolute path: " + DatabaseProperties.getDatabaseLocation());
```

**Solution:**
- Verify working directory: should be project root
- Use absolute path if relative behavior unclear

### Problem: Two Databases Created

**Symptom:** Data split between default and custom locations

**Cause:** User set custom path but didn't save configuration

**Solution:**
1. Check `database.properties` content
2. Ensure "Save Configuration" clicked
3. Migrate data if needed:
   ```bash
   copy .\data\sdp.db D:\Research\project.db
   ```

---

## Migration Guide

### From Hardcoded to Property File

**Old Code:**
```java
String url = "jdbc:sqlite:C:/DB/SDP1.db"; // Hardcoded
```

**New Code:**
```java
String dbLocation = DatabaseProperties.getDatabaseLocation();
String url = "jdbc:sqlite:" + dbLocation;
```

### Migrating Existing Data

If you have data in old location:

```bash
# Copy existing database
copy C:\DB\SDP1.db .\data\sdp.db

# Or update properties to use old location
echo database.location=C:/DB/SDP1.db > database.properties
```

---

## Summary

| Feature | Implementation |
|---------|---------------|
| **Default Location** | `./data/sdp.db` (relative path) |
| **Properties File** | `database.properties` in working directory |
| **UI Auto-load** | ✅ Loads default on tab open |
| **User Override** | ✅ Browse or manual edit |
| **Automatic Save** | ✅ Written back to properties file |
| **Path Types** | ✅ Relative or absolute |
| **Portable** | ✅ Relative paths work across machines |
| **Feedback** | ✅ Confirmation dialog on save |

**Next Steps:**
1. Launch app: `mvn exec:java`
2. Check Configuration tab → Database File field
3. See default: `C:\wspace\SDPTool\data\sdp.db`
4. Override if needed via Browse
5. Click Save Configuration → properties updated
6. Database created automatically on first use

✅ **Zero manual setup required!** 🎉
