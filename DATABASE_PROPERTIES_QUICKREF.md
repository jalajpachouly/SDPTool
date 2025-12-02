# Database Configuration - Quick Reference

## 🎯 What Changed

**Before:** Database path was hardcoded to `C:/DB/SDP1.db` ❌  
**Now:** Database path loaded from `database.properties` with user override support ✅

---

## 📁 Default Configuration

**File:** `database.properties` (created in project directory)

```properties
database.location=./data/sdp.db    # Relative path - portable!
```

**Result:** Database created at `<ProjectDir>/data/sdp.db`

---

## 🖥️ How to Use

### Option 1: Use Default (No Action Needed)
1. Launch app: `mvn exec:java`
2. Go to Configuration tab
3. Database field shows: `C:\wspace\SDPTool\data\sdp.db`
4. Click "Populate SDP Dataset" → Database auto-created ✅

### Option 2: Override via UI
1. Configuration tab → Click **Browse...**
2. Select location: e.g., `D:\MyData\research.db`
3. Click **Save Configuration**
4. Properties file updated automatically ✅
5. Next launch uses your custom location

### Option 3: Edit Properties Manually
Edit `database.properties`:
```properties
database.location=D:/MyResearch/project.db
```
Restart app → new location used

---

## 📝 Key Features

| Feature | Description |
|---------|-------------|
| **Default Path** | `./data/sdp.db` (relative to project) |
| **Auto-load** | UI shows default on startup |
| **User Override** | Browse or manual edit |
| **Auto-save** | Changes written to properties file |
| **Portable** | Relative paths work on any machine |
| **Smart Browse** | Opens at current location, adds .db extension |

---

## 🔧 Files Involved

```
SDPTool/
├── src/main/resources/
│   └── database.properties              ← Template (never modified)
├── database.properties                  ← Active (user overrides)
├── data/
│   └── sdp.db                           ← Default database
└── src/main/java/com/phd/
    ├── config/
    │   └── DatabaseProperties.java      ← NEW: Property manager
    ├── db/
    │   └── Connect.java                 ← UPDATED: Uses properties
    └── ui/
        └── ConfigurationPanel.java      ← UPDATED: Load/save
```

---

## 💡 Common Scenarios

### Scenario 1: Different Projects
```properties
# Project 1
database.location=./data/project1.db

# Project 2  
database.location=./data/project2.db
```

### Scenario 2: Shared Team Setup
```properties
# Commit this - works for everyone
database.location=./shared_data/team.db
```

### Scenario 3: Fixed Location
```properties
# Won't move even if project moves
database.location=D:/Databases/production.db
```

---

## ⚙️ API Usage

```java
// Get database location (resolves relative paths)
String location = DatabaseProperties.getDatabaseLocation();

// Update location (saves to properties file)
DatabaseProperties.setDatabaseLocation("D:/NewPath/db.db");

// Get raw property value (may be relative)
String raw = DatabaseProperties.getRawDatabaseLocation();

// Reload from disk
DatabaseProperties.reload();
```

---

## 🐛 Troubleshooting

**Properties not saving?**
- Check working directory is writable
- Look for console error: "Failed to save database properties"

**Database in wrong location?**
- Check: `System.getProperty("user.dir")` 
- Use absolute path if relative behavior unclear

**Multiple databases created?**
- Ensure "Save Configuration" clicked after changing path
- Check `database.properties` content

---

## 📚 Full Documentation

- **[PROPERTY_FILE_CONFIGURATION.md](PROPERTY_FILE_CONFIGURATION.md)** - Complete guide
- **[DATABASE_CONFIGURATION.md](DATABASE_CONFIGURATION.md)** - Database setup
- **[DATABASE_FIX_README.md](DATABASE_FIX_README.md)** - Quick start

---

## ✅ Summary

**Default:** `./data/sdp.db` (portable relative path)  
**Override:** Browse in UI → auto-saved to properties  
**Result:** Zero manual setup, works everywhere! 🚀
