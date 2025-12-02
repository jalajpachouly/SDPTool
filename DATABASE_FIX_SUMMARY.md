# Database Fixes - Change Summary

## Problem Identified

Your Java code had a critical bug in `Connect.java` where:

1. **Database path was hardcoded:**
   ```java
   String url = "jdbc:sqlite:C:/DB/SDP1.db"; // Always C:/DB/SDP1.db
   ```

2. **Configuration parameter ignored:**
   ```java
   public static Connection getConnection(String dbLocation) {
       // dbLocation parameter was NEVER used!
   ```

3. **No database initialization:**
   - If `C:/DB/SDP1.db` didn't exist, SQLite would create an empty file
   - No tables would exist
   - All INSERT operations would fail with "no such table" errors

---

## What Happens Now

### ✅ **Fixed Issues:**

1. **Database path now uses configuration:**
   ```java
   String url = "jdbc:sqlite:" + dbLocation; // Uses actual parameter
   ```

2. **Auto-creates directory if missing:**
   ```java
   File parentDir = dbFile.getParentFile();
   if (parentDir != null && !parentDir.exists()) {
       parentDir.mkdirs(); // Creates C:\DB\ automatically
   }
   ```

3. **Auto-creates schema if missing:**
   ```java
   private static void initializeDatabaseSchema(Connection conn) {
       // Checks if ISSUE table exists
       // If not, creates all 14 tables from schema
   }
   ```

4. **Better error handling:**
   ```java
   if (dbLocation == null || dbLocation.trim().isEmpty()) {
       dbLocation = "C:/DB/SDP1.db";
       System.out.println("Warning: No database location provided, using default");
   }
   ```

---

## Files Modified

### `src/main/java/com/phd/db/Connect.java`

**Added imports:**
```java
import java.io.File;           // For file operations
import java.sql.Statement;      // For executing SQL
import java.sql.ResultSet;      // For checking table existence
```

**Modified `getConnection()` method:**
- Now uses the `dbLocation` parameter instead of ignoring it
- Creates parent directory if it doesn't exist
- Calls `initializeDatabaseSchema()` to create tables

**Added `initializeDatabaseSchema()` method:**
- Checks if `ISSUE` table exists using database metadata
- If not, creates all 14 tables using CREATE TABLE IF NOT EXISTS
- Includes all tables: ISSUE, LABEL, COMMENTS, CODE, PULL_RQ, etc.

---

## Files Created

### `src/main/resources/database.properties`
Optional properties file for configuring database defaults:
```properties
database.location=C:/DB/SDP1.db
database.auto.create=true
database.auto.init.schema=true
```

### `DATABASE_CONFIGURATION.md`
Comprehensive guide covering:
- What happens with/without a database
- Configuration options (UI, properties, manual)
- Database schema overview
- Troubleshooting common issues
- Testing procedures
- Migration from old version
- Performance tips

---

## How to Use

### **Option 1: Let It Auto-Create (Easiest)**

1. Launch application:
   ```bash
   mvn exec:java
   ```

2. Go to **Configuration** tab

3. Database File field can be:
   - Left empty → defaults to `C:/DB/SDP1.db`
   - Browse to select location → e.g., `D:\Data\myproject.db`

4. Click **"Save Configuration"**

5. Click **"Populate SDP Dataset"**

6. Console output:
   ```
   Connecting to database: jdbc:sqlite:C:/DB/SDP1.db
   Created database directory: C:\DB
   Database schema not found. Creating tables...
   Database schema created successfully!
   Processing Id: 1
   Processing Id: 2
   ...
   ```

### **Option 2: Pre-Create with SQL**

If you prefer manual control:

```bash
# Create directory
mkdir C:\DB

# Create database and schema
sqlite3 C:\DB\SDP1.db < src\main\resources\create-data-set.sql

# Verify
sqlite3 C:\DB\SDP1.db ".tables"
```

Then use the database in the application.

---

## Testing the Fix

### **Test Scenario 1: No Database Exists**

**Before Fix:**
```
❌ Application crashes with "no such table: ISSUE"
❌ Or silently fails with NullPointerException
```

**After Fix:**
```
✅ Directory C:\DB created
✅ File SDP1.db created  
✅ Schema with 14 tables created
✅ Data fetching succeeds
```

### **Test Scenario 2: Custom Database Location**

**Before Fix:**
```
❌ UI setting ignored - always uses C:\DB\SDP1.db
❌ If C:\DB doesn't exist → crash
```

**After Fix:**
```
✅ UI setting: D:\Projects\research.db
✅ Directory D:\Projects created if needed
✅ Database created at D:\Projects\research.db
✅ Schema initialized
```

### **Test Scenario 3: Directory Doesn't Exist**

**Before Fix:**
```
❌ SQLException: unable to open database file
❌ Application fails to start data fetching
```

**After Fix:**
```
✅ Parent directory created automatically
✅ Console: "Created database directory: D:\NewFolder"
✅ Database and schema created successfully
```

---

## Configuration Flow

```
User Action (UI)
    ↓
dbLocation saved to Configuration object
    ↓
FetchData.getData() calls Connect.getConnection(dbLocation)
    ↓
Connect.getConnection():
  1. Validates dbLocation (use default if null/empty)
  2. Creates parent directory if missing
  3. Connects to SQLite (creates .db file if needed)
  4. Calls initializeDatabaseSchema()
  5. Returns Connection object
    ↓
DBManager.insertIssue(issue, conn)
DBManager.insertLabels(issue, conn)
DBManager.insertComments(issue, conn)
    ↓
Data successfully inserted ✅
```

---

## Verification Steps

### **1. Check the Code Changes**

```bash
# View modified file
cat src\main\java\com\phd\db\Connect.java
```

Look for:
- `String url = "jdbc:sqlite:" + dbLocation;` (not hardcoded)
- `initializeDatabaseSchema(conn);` call
- `private static void initializeDatabaseSchema()` method

### **2. Compile the Project**

```bash
mvn clean compile
```

Should compile without errors.

### **3. Run and Test**

```bash
mvn exec:java
```

1. Configuration tab
2. Set "Database File": `C:\Temp\test.db` (new location)
3. Set "Repository Name": `owner/repo`
4. Set "Record From": 1, "Record To": 5
5. Click "Populate SDP Dataset"

Check console:
```
Connecting to database: jdbc:sqlite:C:/Temp/test.db
Created database directory: C:\Temp
Database schema not found. Creating tables...
Database schema created successfully!
Processing Id: 1
```

### **4. Verify Database**

```bash
sqlite3 C:\Temp\test.db

sqlite> .tables
AUTHORS         COMMENTS        LABEL           PULL_RQ         pr_reviewers
CLASSES         CONFIGURATION   PACKAGES        TEAMS
CODE            ISSUE           pr_files_changed  pr_review_comments
CODE_DETAILS

sqlite> SELECT COUNT(*) FROM ISSUE;
5

sqlite> .quit
```

---

## Key Improvements

| Aspect | Before | After |
|--------|--------|-------|
| **Path Configuration** | Hardcoded `C:/DB/SDP1.db` | Uses actual dbLocation parameter |
| **Directory Handling** | Manual creation required | Auto-created with `mkdirs()` |
| **Schema Initialization** | Manual SQL execution | Auto-created on first connection |
| **Error Handling** | Silent failures, crashes | Logging + fallback to defaults |
| **Configuration Respect** | UI setting ignored | UI setting properly used |
| **User Experience** | Requires manual setup | Zero-configuration operation |

---

## Summary

**Your Java code will now:**

✅ Use the database path configured in the UI (not hardcoded)  
✅ Create the database directory if it doesn't exist  
✅ Create the database file automatically (SQLite)  
✅ Create all 14 tables automatically if missing  
✅ Log connection and initialization steps  
✅ Handle errors gracefully with fallbacks  
✅ Work out-of-the-box without manual database setup  

**You can now run the application and populate data without any manual database preparation!**

See `DATABASE_CONFIGURATION.md` for comprehensive usage guide.
