# Label Filtering Implementation

## Overview

This implementation addresses **Inconsistent Labeling Practices** in GitHub repositories by providing a comprehensive color-based label filtering system. The system ensures that only high-confidence, well-established defect types are included in the dataset for software defect prediction research.

## Problem Statement

### Inconsistent Labeling Practices

GitHub repositories rely on contributors to manually label issues, which leads to several problems:

1. **Different Labeling Conventions**: Contributors use varying labeling conventions across repositories
2. **Incomplete Labels**: Some issues have incomplete or missing labels
3. **Subjective Decisions**: Labeling decisions are subjective and vary by contributor
4. **Label Evolution**: Label terminology changes over time within the same project

## Mitigation Strategy

### 1. Color-Based Label Filtering ✅

We implemented a color-based label filtering system with **10 predefined colors** representing high-confidence defect types:

| Color Code | RGB | Defect Type |
|-----------|-----|-------------|
| `d93f0b` | Orange-red | Bug severity |
| `b60205` | Red | Critical bugs |
| `db755e` | Rust | Enhancement bugs |
| `4a4ea8` | Blue | Feature defects |
| `006b75` | Teal | Technical debt |
| `d4c5f9` | Light purple | UI/UX issues |
| `cfcfcf` | Light gray | Minor issues |
| `dddddd` | Very light gray | Documentation |
| `5319e7` | Purple | Performance |
| `e11d21` | Bright red | Security |

### 2. Database Foreign Key Constraints ✅

The database schema maintains referential integrity:

```sql
CREATE TABLE "LABEL" (
    "ISSUE_ID" INTEGER,
    "NAME" TEXT,
    "COLOR" TEXT,
    FOREIGN KEY("ISSUE_ID") REFERENCES "ISSUE"("ISSUE_ID")
);
```

This ensures:
- Labels are always associated with valid issues
- Cascading operations maintain consistency
- Orphaned labels are prevented

### 3. Incomplete/Untriaged Label Exclusion ✅

Issues with the following label keywords are automatically excluded:

- `waiting`, `pending`, `triage`
- `needs`, `question`, `invalid`
- `wontfix`, `duplicate`
- `help wanted`, `good first issue`

### 4. Single-Label vs Multi-Label Filtering ✅

**Single-Class Dataset**: Apply SQL filter to exclude issues with multiple labels
```sql
SELECT ISSUE_ID FROM LABEL
GROUP BY ISSUE_ID
HAVING COUNT(*) = 1
```

**Multi-Label Dataset**: Include issues with multiple valid labels based on configured thresholds

### 5. High-Importance Module Filtering ✅

For multiclass datasets, filter by modules with high importance in developer communities:

- **Spring Ecosystem**: spring-core, spring-boot, spring-web, spring-data
- **Data Access**: jdbc, jpa, hibernate
- **Networking**: tcp, udp, http, rest
- **Security**: security, authentication, authorization

## Implementation Components

### 1. LabelFilterConfig.java

Central configuration class managing all filtering rules:

```java
// Enable/disable filters
LabelFilterConfig.setEnableColorFiltering(true);
LabelFilterConfig.setEnableIncompleteFiltering(true);
LabelFilterConfig.setEnableModuleFiltering(false);
LabelFilterConfig.setEnforceSingleLabel(false);

// Configure thresholds
LabelFilterConfig.setMinLabelCount(1);
LabelFilterConfig.setMaxLabelCountForSingleClass(1);

// Get valid colors
Set<String> validColors = LabelFilterConfig.getValidLabelColors();

// Check if color is valid
boolean isValid = LabelFilterConfig.isValidLabelColor("d93f0b");

// Generate SQL filters
String colorSQL = LabelFilterConfig.getColorFilterSQL();
String excludedSQL = LabelFilterConfig.getExcludedLabelFilterSQL();
```

### 2. DBManager.java - Enhanced insertLabels()

Labels are filtered **during data collection** from GitHub:

```java
public static void insertLabels(GHIssue issue, Connection conn) {
    // First pass: filter labels based on configuration
    for (GHLabel label : labels) {
        // Apply color filtering
        if (!LabelFilterConfig.isValidLabelColor(label.getColor())) {
            System.out.println("[FILTERED] Invalid color");
            continue;
        }
        
        // Apply incomplete/untriaged filtering
        if (LabelFilterConfig.isExcludedLabel(label.getName())) {
            System.out.println("[FILTERED] Incomplete keyword");
            continue;
        }
        
        validLabels.add(label);
    }
    
    // Check label count requirements
    if (validLabelCount < LabelFilterConfig.getMinLabelCount()) {
        return; // Skip issue
    }
    
    // Check single-label constraint
    if (LabelFilterConfig.isSingleLabelEnforced() && 
        validLabelCount > LabelFilterConfig.getMaxLabelCountForSingleClass()) {
        return; // Skip issue
    }
    
    // Insert only valid labels
    for (GHLabel label : validLabels) {
        pstmt.setString(3, label.getColor());
        pstmt.executeUpdate();
    }
}
```

### 3. DBManager.java - getFilteredIssues()

Retrieve issues meeting all filter criteria:

```java
public static List<Issue> getFilteredIssues() {
    String query = 
        "SELECT DISTINCT i.ISSUE_ID, i.TITLE, i.PROCESSED_TITLES, i.PROCESSED_BODY " +
        "FROM ISSUE i " +
        "INNER JOIN LABEL l ON i.ISSUE_ID = l.ISSUE_ID " +
        "WHERE " + LabelFilterConfig.getColorFilterSQL() + 
        " AND " + LabelFilterConfig.getExcludedLabelFilterSQL();
    
    // Add single-label constraint if enabled
    if (LabelFilterConfig.isSingleLabelEnforced()) {
        query += " AND i.ISSUE_ID IN (" +
                "SELECT ISSUE_ID FROM LABEL " +
                "WHERE " + LabelFilterConfig.getColorFilterSQL() +
                " GROUP BY ISSUE_ID " +
                "HAVING COUNT(*) <= " + 
                LabelFilterConfig.getMaxLabelCountForSingleClass() +
                ")";
    }
    
    return executeQuery(query);
}
```

### 4. DBManager.java - Label Statistics

Analyze filtering impact:

```java
public static Map<String, Object> getLabelStatistics() {
    // Total labels
    // Labels by color (valid vs filtered)
    // Issues with valid labels
    // Label count distribution
    return stats;
}

public static void printLabelStatistics() {
    // Human-readable console output
    System.out.println("Total Labels: 1250");
    System.out.println("Valid Labels (by color): 980");
    System.out.println("Filtered Labels: 270 (21.6%)");
    
    // Color distribution with [VALID] or [FILTERED] markers
    // Label count distribution showing single vs multi-label issues
}
```

### 5. LabelFilterPanel.java

UI for configuration and validation:

**Features**:
- Enable/disable each filter type with checkboxes
- Configure min/max label counts with spinners
- Apply configuration button
- View statistics button (shows color distribution, filtering impact)
- Test filtered query button (previews filtered issues)
- Real-time statistics display in text area

**Layout**:
```
┌─────────────────────────────────────────────────┐
│ Label Filtering Configuration                   │
│ Mitigate inconsistent labeling practices        │
├─────────────────────────────────────────────────┤
│ ☑ Enable Color-Based Filtering                  │
│   (Only 10 predefined colors)                   │
│                                                  │
│ ☑ Exclude Incomplete/Untriaged Labels          │
│   (Filters 'waiting', 'pending', etc.)          │
│                                                  │
│ ☐ Enforce Single-Label Constraint              │
│                                                  │
│ Minimum Labels: [1 ▼]                           │
│ Max Labels (Single-Class): [1 ▼]               │
│                                                  │
│ ☐ Enable High-Importance Module Filtering      │
│   (Spring Core, JDBC, TCP/UDP, etc.)            │
│                                                  │
│ [Apply] [View Statistics] [Test Query]         │
├─────────────────────────────────────────────────┤
│ === LABEL STATISTICS ===                        │
│                                                  │
│ Total Labels: 1250                              │
│ Valid Labels: 980                               │
│ Filtered Labels: 270 (21.6%)                    │
│                                                  │
│ COLOR DISTRIBUTION:                             │
│   d93f0b: 234 [VALID]                          │
│   b60205: 189 [VALID]                          │
│   xyz123: 45 [FILTERED]                        │
│   ...                                           │
└─────────────────────────────────────────────────┘
```

## Usage Workflow

### Step 1: Configure Filters (Before Data Collection)

```java
// In LabelFilterPanel UI or programmatically:
LabelFilterConfig.setEnableColorFiltering(true);
LabelFilterConfig.setEnableIncompleteFiltering(true);
LabelFilterConfig.setEnforceSingleLabel(true); // For single-class dataset
LabelFilterConfig.setMinLabelCount(1);
LabelFilterConfig.setMaxLabelCountForSingleClass(1);
```

### Step 2: Fetch Data from GitHub

When you click "Perform Data Fetch" in ConfigurationPanel:

```
Processing Issue #1234...
  [FILTERED] Label 'waiting-for-response' excluded: incomplete/untriaged keyword
  [FILTERED] Label 'good first issue' excluded: incomplete/untriaged keyword
  [FILTERED] Label 'xyz123' excluded: invalid color (abc123)
  [SUCCESS] Inserted 1 valid label(s) for issue #1234
```

### Step 3: View Statistics

Click "View Statistics" in LabelFilterPanel:

```
=== LABEL STATISTICS ===

Total Labels: 1250
Valid Labels (by color): 980
Issues with Valid Labels: 845

COLOR DISTRIBUTION:
  d93f0b: 234 [VALID]
  b60205: 189 [VALID]
  abc123: 45 [FILTERED]

LABEL COUNT DISTRIBUTION:
  1 label(s): 623 issue(s)
  2 label(s): 178 issue(s)
  3 label(s): 44 issue(s)

=== FILTERING IMPACT ===
Filtered Labels: 270 (21.6%)
Retained Labels: 980 (78.4%)
```

### Step 4: Test Filtered Query

Click "Test Filtered Query" to preview results:

```
=== FILTERED QUERY RESULTS ===

Execution Time: 45 ms
Filtered Issues Found: 623

Sample Issues (first 10):
  #234: Fix memory leak in connection pool...
  #456: Add validation for null parameters...
  #789: Performance issue with large datasets...
  ...
```

### Step 5: Export Dataset

Use the filtered issues for CSV export:

```java
// Get only filtered issues
List<Issue> validIssues = DBManager.getFilteredIssues();

// Export to CSV
CSVHelper.exportDataset(validIssues, "dataset_filtered.csv");
```

## SQL Query Examples

### Get issues with exactly 1 valid label (Single-Class):

```sql
SELECT DISTINCT i.ISSUE_ID, i.TITLE, i.PROCESSED_TITLES
FROM ISSUE i
INNER JOIN LABEL l ON i.ISSUE_ID = l.ISSUE_ID
WHERE l.COLOR IN ('d93f0b','b60205','db755e','4a4ea8','006b75',
                  'd4c5f9','cfcfcf','dddddd','5319e7','e11d21')
  AND LOWER(l.NAME) NOT LIKE '%waiting%'
  AND LOWER(l.NAME) NOT LIKE '%pending%'
  AND LOWER(l.NAME) NOT LIKE '%triage%'
  AND i.ISSUE_ID IN (
      SELECT ISSUE_ID FROM LABEL
      WHERE COLOR IN ('d93f0b','b60205','db755e','4a4ea8','006b75',
                      'd4c5f9','cfcfcf','dddddd','5319e7','e11d21')
      GROUP BY ISSUE_ID
      HAVING COUNT(*) = 1
  )
ORDER BY i.ISSUE_ID;
```

### Get issues with 2-5 valid labels (Multi-Label):

```sql
SELECT DISTINCT i.ISSUE_ID, i.TITLE
FROM ISSUE i
INNER JOIN LABEL l ON i.ISSUE_ID = l.ISSUE_ID
WHERE l.COLOR IN ('d93f0b','b60205','db755e','4a4ea8','006b75',
                  'd4c5f9','cfcfcf','dddddd','5319e7','e11d21')
  AND i.ISSUE_ID IN (
      SELECT ISSUE_ID FROM LABEL
      WHERE COLOR IN ('d93f0b','b60205','db755e','4a4ea8','006b75',
                      'd4c5f9','cfcfcf','dddddd','5319e7','e11d21')
      GROUP BY ISSUE_ID
      HAVING COUNT(*) BETWEEN 2 AND 5
  );
```

## Benefits

### 1. Data Quality Improvement
- Eliminates noise from inconsistent labeling
- Ensures only high-confidence labels are used
- Reduces subjective labeling bias

### 2. Research Validity
- Consistent label types across all repositories
- Reproducible filtering criteria
- Documented mitigation strategy for threats to validity

### 3. Flexibility
- Configure filters per research needs (single-class vs multi-label)
- Enable/disable specific filter types
- Adjust thresholds dynamically

### 4. Transparency
- Real-time statistics show filtering impact
- Console logging tracks every filter decision
- SQL queries are visible and auditable

## Testing & Validation

### Unit Test Example:

```java
@Test
public void testColorFiltering() {
    LabelFilterConfig.setEnableColorFiltering(true);
    
    assertTrue(LabelFilterConfig.isValidLabelColor("d93f0b"));
    assertTrue(LabelFilterConfig.isValidLabelColor("b60205"));
    assertFalse(LabelFilterConfig.isValidLabelColor("abc123"));
}

@Test
public void testIncompleteFiltering() {
    LabelFilterConfig.setEnableIncompleteFiltering(true);
    
    assertTrue(LabelFilterConfig.isExcludedLabel("waiting-for-response"));
    assertTrue(LabelFilterConfig.isExcludedLabel("good first issue"));
    assertFalse(LabelFilterConfig.isExcludedLabel("bug"));
}
```

### Integration Test:

```java
@Test
public void testFilteredIssuesQuery() {
    // Setup test data
    insertTestIssueWithLabels(1, "d93f0b", "bug");           // Valid
    insertTestIssueWithLabels(2, "abc123", "unknown");       // Filtered (invalid color)
    insertTestIssueWithLabels(3, "b60205", "waiting");       // Filtered (incomplete)
    
    // Configure filters
    LabelFilterConfig.setEnableColorFiltering(true);
    LabelFilterConfig.setEnableIncompleteFiltering(true);
    
    // Execute query
    List<Issue> filtered = DBManager.getFilteredIssues();
    
    // Validate
    assertEquals(1, filtered.size());
    assertEquals(1, filtered.get(0).getId());
}
```

## Future Enhancements

1. **Machine Learning-Based Label Validation**: Train a model to predict label validity
2. **Cross-Repository Label Mapping**: Map similar labels across different repositories
3. **Temporal Label Evolution Tracking**: Track how label usage changes over time
4. **Custom Color Schemes**: Allow researchers to define custom valid color sets
5. **Export Filter Configuration**: Save/load filter configurations for reproducibility

## References

- GitHub Labels Documentation: https://docs.github.com/en/issues/using-labels-and-milestones-to-track-work/managing-labels
- Research Paper: [Your PhD Research Reference]
- Dataset Repository: [Your Dataset Location]

## Contact

For questions or issues with label filtering implementation:
- GitHub Issues: [Repository URL]
- Email: [Your Email]
- Documentation: See this file and inline code comments
