# Database Configuration Guide

## Overview

The SDPTool uses **SQLite** as its database. The database stores GitHub issue data including:
- Issues, labels, comments
- Pull requests, reviewers, file changes
- Code changes and metadata
- Processing configurations

## What Happens Without a Database?

### **Before Fix (Original Code):**
1. ❌ Database path was **hardcoded** to `C:/DB/SDP1.db` 
2. ❌ Configuration `dbLocation` was **ignored**
3. ❌ If database didn't exist, SQLite created an **empty file** with no tables
4. ❌ Application would crash with "no such table: ISSUE" errors

### **After Fix (Current Code):**
1. ✅ Database path now uses the **configured location**
2. ✅ If directory doesn't exist, it's **created automatically**
3. ✅ If database doesn't exist, SQLite creates the file
4. ✅ If tables don't exist, they're **created automatically** from schema
5. ✅ Proper error logging and fallback to default location

---

## Configuration Options

### **Option 1: Use UI Configuration (Recommended)**

1. Launch the application: `mvn exec:java`
2. Go to **Configuration** tab
3. Click **"Browse..."** next to Database File
4. Select or create a new `.db` file (e.g., `C:\DB\MyProject.db`)
5. Click **"Save Configuration"**
6. The database will be created automatically with all tables

### **Option 2: Use Properties File**

Edit `src/main/resources/database.properties`:

```properties
# Change default database location
database.location=D:/MyData/sdp.db

# Enable auto-creation (default: true)
database.auto.create=true

# Enable auto-schema initialization (default: true)
database.auto.init.schema=true
```

### **Option 3: Manual SQLite Database**

If you want to create the database manually:

```bash
# Install SQLite (if not installed)
# Download from: https://www.sqlite.org/download.html

# Create database
sqlite3 C:\DB\SDP1.db

# Run schema from SQL file
.read src/main/resources/create-data-set.sql

# Verify tables
.tables

# Exit
.quit
```

---

## Database Location Requirements

### **Default Location:**
```
C:\DB\SDP1.db
```

### **Custom Location Requirements:**
- Use absolute paths (e.g., `C:\Users\username\data\mydb.db`)
- Parent directory will be created if it doesn't exist
- File extension should be `.db`
- Avoid spaces in path (use underscores: `my_database.db`)

### **Valid Examples:**
```
✅ C:\DB\SDP1.db
✅ D:\Projects\GitHub\data.db
✅ C:\Users\john\Documents\sdp_research.db
```

### **Invalid Examples:**
```
❌ DB\SDP1.db              (relative path - may fail)
❌ C:\My Data\sdp.db       (space in path - may cause issues)
❌ \\network\share\db.db   (network paths not tested)
```

---

## Database Schema

The database contains **14 tables**:

### Core Tables:
- **ISSUE** - GitHub issues (title, reporter, dates, body)
- **LABEL** - Issue labels (bug, enhancement, etc.)
- **COMMENTS** - Issue comments with processed text
- **CODE** - Code changes associated with issues
- **CODE_DETAILS** - Metrics (lines, classes, authors, complexity)

### Pull Request Tables:
- **PULL_RQ** - Pull request metadata
- **pr_review_comments** - PR review comments
- **pr_reviewers** - PR reviewers
- **pr_files_changed** - Files modified in PRs
- **TEAMS** - Teams involved in PRs

### Supporting Tables:
- **AUTHORS** - Code authors
- **CLASSES** - Java classes modified
- **PACKAGES** - Java packages affected
- **CONFIGURATION** - Saved configurations

---

## Automatic Database Initialization

The updated `Connect.java` now includes:

### **1. Directory Creation:**
```java
File parentDir = dbFile.getParentFile();
if (parentDir != null && !parentDir.exists()) {
    parentDir.mkdirs(); // Creates C:\DB\ if it doesn't exist
}
```

### **2. Schema Auto-Creation:**
```java
private static void initializeDatabaseSchema(Connection conn) {
    // Checks if ISSUE table exists
    // If not, creates all 14 tables automatically
}
```

### **3. Logging:**
```
Connecting to database: jdbc:sqlite:C:/DB/SDP1.db
Database schema not found. Creating tables...
Database schema created successfully!
```

---

## Troubleshooting

### **Problem: "Database is locked"**
**Cause:** Another application (SQLite Browser, VS Code extension) has the database open

**Solution:**
```bash
# Close all SQLite tools
# Or delete lock file
del C:\DB\SDP1.db-journal
```

### **Problem: "Unable to open database file"**
**Cause:** Permission issues or invalid path

**Solution:**
```bash
# Check directory permissions
icacls C:\DB

# Or use different location
C:\Users\<YourUsername>\AppData\Local\SDPTool\data.db
```

### **Problem: "No such table: ISSUE"**
**Cause:** Schema initialization failed

**Solution:**
```bash
# Delete existing empty database
del C:\DB\SDP1.db

# Restart application - schema will be recreated
mvn exec:java
```

### **Problem: Configuration not saved**
**Cause:** CONFIGURATION table doesn't exist yet

**Solution:**
1. Connect to any database first (creates schema)
2. Then save configuration
3. Or manually run `create-data-set.sql`

---

## Testing Database Connection

### **Test 1: Via Application**
1. Launch: `mvn exec:java`
2. Configuration tab → Set database path
3. Click "Populate SDP Dataset"
4. Check console for: "Connecting to database: jdbc:sqlite:..."

### **Test 2: Manual SQLite**
```bash
# Open database
sqlite3 C:\DB\SDP1.db

# Check tables
.tables
# Should show: AUTHORS, CLASSES, CODE, COMMENTS, CONFIGURATION, etc.

# Count records
SELECT COUNT(*) FROM ISSUE;

# View recent issues
SELECT ISSUE_ID, TITLE FROM ISSUE LIMIT 5;
```

### **Test 3: Programmatic**
```java
public static void main(String[] args) {
    Connection conn = Connect.getConnection("C:/DB/test.db");
    if (conn != null) {
        System.out.println("✅ Database connected!");
        Connect.closeConnection(conn);
    } else {
        System.out.println("❌ Connection failed!");
    }
}
```

---

## Migration from Old Version

If you have an existing database from the old hardcoded version:

### **Option A: Use Existing Database**
```
1. Note current location: C:\DB\SDP1.db
2. In UI, browse to: C:\DB\SDP1.db
3. Save configuration
4. Continue using existing data
```

### **Option B: Move Database**
```bash
# Copy to new location
copy C:\DB\SDP1.db D:\Projects\MyResearch\sdp_data.db

# Update UI configuration to new path
```

### **Option C: Merge Databases**
```sql
-- Attach old database
ATTACH DATABASE 'C:\DB\old.db' AS old;

-- Copy data
INSERT INTO ISSUE SELECT * FROM old.ISSUE;
INSERT INTO LABEL SELECT * FROM old.LABEL;
-- ... repeat for other tables

-- Detach
DETACH DATABASE old;
```

---

## Best Practices

### **For Development:**
```
✅ Use local path: C:\DB\dev_sdp.db
✅ Small record ranges (1-100) for testing
✅ Keep backup: copy C:\DB\dev_sdp.db C:\DB\backup\
```

### **For Production/Research:**
```
✅ Use dedicated directory: D:\Research\Data\sdp_production.db
✅ Regular backups before large data fetches
✅ Use version control for database snapshots
✅ Document record ranges in configuration
```

### **For Multiple Projects:**
```
SDPTool/
  data/
    project1_spring.db
    project2_angular.db
    project3_react.db
```

Configure each in UI and save separately.

---

## Performance Tips

### **SQLite Optimization:**
```sql
-- Enable Write-Ahead Logging (WAL mode)
PRAGMA journal_mode=WAL;

-- Increase cache size (in KB)
PRAGMA cache_size=10000;

-- Synchronous mode for speed
PRAGMA synchronous=NORMAL;
```

Add to `Connect.java` after connection:
```java
try (Statement stmt = conn.createStatement()) {
    stmt.execute("PRAGMA journal_mode=WAL");
    stmt.execute("PRAGMA cache_size=10000");
}
```

### **Batch Inserts:**
Already implemented in `DBManager.java` - uses prepared statements.

---

## Advanced Configuration

### **Using Environment Variables:**

```java
// In Connect.java
String dbLocation = System.getenv("SDP_DATABASE_PATH");
if (dbLocation == null) {
    dbLocation = Configuration.getConfig().getDbLocation();
}
```

Set in Windows:
```cmd
setx SDP_DATABASE_PATH "D:\MyData\sdp.db"
```

### **Using System Properties:**

```bash
mvn exec:java -Ddb.path=D:/Data/custom.db
```

```java
String dbLocation = System.getProperty("db.path", 
                    Configuration.getConfig().getDbLocation());
```

---

## Summary

| Feature | Status | Notes |
|---------|--------|-------|
| Auto-create directory | ✅ | Creates `C:\DB\` if missing |
| Auto-create database | ✅ | SQLite creates `.db` file |
| Auto-init schema | ✅ | Creates all 14 tables |
| UI configuration | ✅ | Browse and select path |
| Configuration persistence | ✅ | Saved in CONFIGURATION table |
| Error handling | ✅ | Logs and fallbacks |
| Default location | ✅ | `C:\DB\SDP1.db` |

**You can now run the application without manually creating the database!** 🎉
