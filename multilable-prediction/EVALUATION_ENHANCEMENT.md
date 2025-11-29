# Evaluation Logic Enhancement: Explicit Macro Scores

## Overview
Updated `configurable_main.py` to explicitly calculate, store, and display **macro-averaged scores** for all models. Previously, macro scores were calculated implicitly during visualization (`groupby().mean()`). Now they are first-class data in the results CSV and comparison charts.

---

## What Changed

### 1. **New Function: `calculate_macro_scores()`**
**Location:** Lines ~213-237

Calculates macro-averaged metrics from per-label results:
- **Input:** List of per-label result dictionaries from a single model
- **Output:** Dictionary with `Label='MACRO_AVERAGE'` containing:
  - `Macro Recall`: Average recall across all labels
  - `Macro F1`: Average F1-score across all labels
  - `Macro Hamming Loss`: Average hamming loss across all labels

**Example:**
```python
per_label = [
    {'Model': 'CNN', 'Label': 'Bug', 'Recall': 0.95, 'F1': 0.97, 'Hamming Loss': 0.01},
    {'Model': 'CNN', 'Label': 'Feature', 'Recall': 0.98, 'F1': 0.99, 'Hamming Loss': 0.005},
    # ... other labels
]
macro = calculate_macro_scores(per_label)
# Result: {'Model': 'CNN', 'Label': 'MACRO_AVERAGE', 'Recall': 0.9979, 'F1': 0.9979, ...}
```

---

### 2. **Macro Score Integration After Each Model Evaluation**

**Locations:**
- Traditional ML models: ~Lines 620-625
- MLP model: ~Lines 675-681
- CNN model: ~Lines 754-760

**What Happens:**
After each model's evaluation:
1. Get per-label results from `evaluate_classifier()` or `evaluate_deep_learning_model()`
2. Calculate macro scores using `calculate_macro_scores()`
3. Append macro score row to `all_results` list
4. Print "Macro F1" instead of "Avg F1" in console

**Example Console Output (Before):**
```
  CNN → Avg F1: 0.9979
```

**Example Console Output (After):**
```
  CNN → Macro F1: 0.9979, Macro Recall: 0.9979
```

---

### 3. **Results CSV Now Includes Macro Rows**

**Location:** ~Line 764 (DataFrame creation)

The `results_{data_type}.csv` file now contains:
- **Per-label rows** (one per label per model)
- **Macro average rows** with `Label='MACRO_AVERAGE'`

**Example CSV:**
```csv
Model,Label,Recall,F1,Hamming Loss
CNN,Bug,0.9500,0.9700,0.0100
CNN,Feature,0.9800,0.9900,0.0050
CNN,MACRO_AVERAGE,0.9979,0.9979,0.0021
MLP,Bug,0.9200,0.9400,0.0150
MLP,Feature,0.9600,0.9750,0.0080
MLP,MACRO_AVERAGE,0.9650,0.9680,0.0095
```

---

### 4. **Updated Visualization Functions**

#### **`plot_model_summary_bar()` - Lines 238-271**
- **Before:** Calculated macro via `df.groupby('Model')['F1'].mean()`
- **After:** Filters for `Label=='MACRO_AVERAGE'` rows
- **Chart Title:** "Average F1 by Model" → **"Macro F1-Score by Model"**
- **Y-Axis Label:** "Average F1" → **"Macro F1-Score"**

#### **`plot_combined_model_comparison()` - Lines 311-343**
- **Before:** Used `groupby().mean()` for balanced vs. unbalanced comparison
- **After:** Filters for `Label=='MACRO_AVERAGE'` rows
- **Chart Title:** "Average F1 by Model" → **"Macro F1-Score by Model"**
- **Y-Axis Label:** "Average F1" → **"Macro F1-Score"**

#### **`plot_metric_comparisons()` - Lines 347-387**
- **Before:** Calculated average via `groupby()[metric].mean()`
- **After:** Filters for `Label=='MACRO_AVERAGE'` rows
- **Chart Titles:** "Average {metric}" → **"Macro {metric}"**
- **Y-Axis Labels:** "{metric}" → **"Macro {metric}"**

**All functions include fallback:** If no `MACRO_AVERAGE` rows exist (e.g., old results), fall back to `groupby().mean()`.

---

### 5. **New Function: `generate_model_ranking_table()`**
**Location:** Lines 274-308

Creates a ranking table comparing models by macro F1-score:
- Filters for `Label=='MACRO_AVERAGE'` rows
- Sorts models by F1-score (descending)
- Adds rank numbers (1 = best)
- Returns:
  - `ranking`: DataFrame with Rank, Model, F1, Recall, Hamming Loss
  - `best_model`: Name of top-performing model
  - `best_f1`: F1-score of best model

**Example Output:**
```python
{
    'ranking': DataFrame([
        {'Rank': 1, 'Model': 'CNN', 'F1': 0.9979, 'Recall': 0.9979, 'Hamming Loss': 0.0021},
        {'Rank': 2, 'Model': 'MLP', 'F1': 0.9680, 'Recall': 0.9650, 'Hamming Loss': 0.0095},
        {'Rank': 3, 'Model': 'RandomForest', 'F1': 0.8950, ...}
    ]),
    'best_model': 'CNN',
    'best_f1': 0.9979
}
```

---

### 6. **Enhanced HTML Report**

**Location:** ~Lines 880-920

#### **New Section 3.1: Model Rankings by Macro F1-Score**
- Shows ranking table for each data type (Balanced, Unbalanced)
- Displays: Rank, Model, Macro F1, Macro Recall, Hamming Loss
- Highlights best model with explicit text: "**Best Model:** CNN with Macro F1 = 0.9979"

#### **Section 3.2: Statistical Significance Testing**
- **Best model determined from final test set results** (from Section 3.1 ranking)
- Uses fold-wise macro-F1 scores from cross-validation for statistical tests
- Performs paired Wilcoxon signed-rank test (or paired t-test fallback)
- Compares best model (from test set) against all other models using their CV fold metrics
- Shows p-values and identifies significant differences (p < 0.05)
- **Key improvement:** Now uses test set best model as baseline, not CV best model
  - **Why this matters:** A model might have best CV performance but lower test performance (or vice versa)
  - **Example:** RandomForest might have stable CV scores (e.g., 0.89 mean) but CNN achieves higher test set score (0.997)
  - **New behavior:** Statistical tests compare CNN (test set winner) vs others using CV fold-wise metrics

**Example HTML Output:**
```html
<h3>3.1 Model Rankings by Macro F1-Score</h3>
<h4>Balanced Data</h4>
<table border='1'>
  <tr><th>Rank</th><th>Model</th><th>Macro F1</th><th>Macro Recall</th><th>Hamming Loss</th></tr>
  <tr><td>1</td><td><strong>CNN</strong></td><td>0.9979</td><td>0.9979</td><td>0.0021</td></tr>
  <tr><td>2</td><td><strong>MLP</strong></td><td>0.9680</td><td>0.9650</td><td>0.0095</td></tr>
  ...
</table>
<p><strong>Best Model:</strong> CNN with Macro F1 = 0.9979</p>

<h3>3.2 Statistical Significance Testing</h3>
<h4>Balanced Data</h4>
<p>Paired tests on fold-wise macro-F1 (Wilcoxon signed-rank; fallback to paired t-test for tied folds).</p>
<table border='1'>
  <tr><th>Best Model</th><th>Baseline</th><th>Test</th><th>p-value</th></tr>
  <tr><td>CNN</td><td>MLP</td><td>Wilcoxon</td><td>0.0023</td></tr>
  <tr><td>CNN</td><td>RandomForest</td><td>Wilcoxon</td><td>0.0001</td></tr>
</table>
<p>CNN outperformed MLP, RandomForest (p &lt; 0.05).</p>
```

#### **Updated Section 3.3: Detailed Metrics**
- **Before:** Showed all rows (per-label + macro mixed)
- **After:** Filters out `MACRO_AVERAGE` rows to show only per-label details
- Title changed: "Metric Table" → **"Detailed Metrics (All Models, All Labels)"**

#### **Updated Section 3.4: F1 Score Trends**
- Shows per-label F1 scores over time/across labels
- Filters out `MACRO_AVERAGE` rows to show label-level details only
- Title updated: "F1 Score Comparison" → **"F1 Score Comparison (Per Label)"**

#### **Updated Chart Captions**
- Section 3.4: "Average F1" → **"Macro F1-Score"**
- Section 3.5: "Average F1 by Model" → **"Macro F1-Score by Model"**

---

## Important: Cross-Validation vs. Test Set Performance

### **Why Rankings and Significance Testing Now Align**

**Previous Behavior (Potential Confusion):**
- **Section 3.1 Ranking:** Based on final test set macro-F1 → "CNN is best (0.9979)"
- **Section 3.2 Significance:** Based on CV fold mean → "RandomForest is best (0.89 CV mean)"
- **Problem:** Two different "best models" causing confusion

**New Behavior (Consistent):**
- **Section 3.1 Ranking:** Based on final test set macro-F1 → "CNN is best (0.9979)"
- **Section 3.2 Significance:** Uses CNN as baseline, compares via CV fold-wise metrics
- **Result:** Statistical tests confirm if the test set winner is also significantly better in CV

### **Example Scenario**

**Imagine these results:**

| Model | CV Mean F1 | Test Set Macro F1 |
|-------|------------|-------------------|
| RandomForest | 0.8900 | 0.8950 |
| MLP | 0.8850 | 0.9680 |
| CNN | 0.8800 | 0.9979 |

**Old behavior:**
- Ranking would show: CNN > MLP > RandomForest (based on test)
- Significance would compare vs. RandomForest (based on CV mean)
- Confusing: "Why is RandomForest the baseline if CNN ranks #1?"

**New behavior:**
- Ranking shows: CNN > MLP > RandomForest (based on test)
- Significance compares vs. CNN using CV fold-wise scores
- Clear: "CNN is best on test set. Is it significantly better than others in CV folds?"

### **Why This Matters**

1. **Consistency:** Same model shown as "best" in both ranking and significance testing
2. **Interpretability:** Easy to understand "CNN is best, and here's statistical evidence from CV"
3. **Avoids confusion:** No more "RandomForest winning" when CNN has higher test scores
4. **Proper workflow:** Test set determines winner, CV fold-wise data validates statistical significance

---

## Benefits

### ✅ **Transparency**
Macro scores are now explicitly calculated and visible in:
- CSV files (separate rows)
- Console output during execution
- HTML report (ranking table)
- All comparison charts

### ✅ **Accuracy**
No hidden calculations in visualization layer. Macro scores calculated once from per-label results, then reused everywhere.

### ✅ **Comparison Clarity**
New ranking table makes it obvious which model performs best:
```
Rank 1: CNN - Macro F1 = 0.9979
Rank 2: MLP - Macro F1 = 0.9680
Rank 3: RandomForest - Macro F1 = 0.8950
```

### ✅ **Consistency**
All charts and tables use the same macro calculation method (arithmetic mean of per-label scores).

### ✅ **Backward Compatibility**
All visualization functions include fallback to `groupby().mean()` if `MACRO_AVERAGE` rows don't exist (for old result files).

---

## Verification

### **Check CSV Output**
After running `configurable_main.py`:
```bash
cat output/reports/T5_*/results_balanced.csv | grep MACRO_AVERAGE
```
**Expected Output:**
```
CNN,MACRO_AVERAGE,0.9979,0.9979,0.0021
MLP,MACRO_AVERAGE,0.9680,0.9650,0.0095
```

### **Check Console Output**
Look for:
```
  CNN → Macro F1: 0.9979, Macro Recall: 0.9979
  MLP → Macro F1: 0.9680, Macro Recall: 0.9650
```

### **Check HTML Report**
Open `output/reports/T5_*/report.html` and verify:
1. **Section 3.1** shows model ranking table with ranks 1, 2, 3, ...
2. **Best Model** text appears below ranking table
3. **Section 3.2** shows per-label details only (no MACRO_AVERAGE rows)
4. **Chart titles** say "Macro F1-Score" instead of "Average F1"

---

## Example: Full Workflow

1. **Run Evaluation:**
   ```bash
   python configurable_main.py configs/quick_test.json
   ```

2. **Console Shows Macro Scores:**
   ```
   Evaluating RandomForest...
     RandomForest → Macro F1: 0.8950, Macro Recall: 0.8820
   
   Training MLP...
     MLP → Macro F1: 0.9680, Macro Recall: 0.9650
   
   Training CNN...
     CNN → Macro F1: 0.9979, Macro Recall: 0.9979
   ```

3. **CSV Contains Macro Rows:**
   ```csv
   RandomForest,MACRO_AVERAGE,0.8820,0.8950,0.0320
   MLP,MACRO_AVERAGE,0.9650,0.9680,0.0095
   CNN,MACRO_AVERAGE,0.9979,0.9979,0.0021
   ```

4. **HTML Report Shows Ranking:**
   ```
   Rank 1: CNN - Macro F1 = 0.9979
   Rank 2: MLP - Macro F1 = 0.9680
   Rank 3: RandomForest - Macro F1 = 0.8950
   
   Best Model: CNN with Macro F1 = 0.9979
   ```

5. **Charts Display Macro Scores:**
   - `model_f1_summary_balanced.png` → Title: "Macro F1-Score by Model"
   - `model_f1_comparison.png` → Title: "Macro F1-Score by Model (Balanced vs Unbalanced)"

---

## Technical Details

### **Macro Calculation Formula**
```python
macro_f1 = (f1_label1 + f1_label2 + ... + f1_labelN) / N
macro_recall = (recall_label1 + recall_label2 + ... + recall_labelN) / N
```

**Example with 14 labels:**
- Label 1: F1 = 0.98
- Label 2: F1 = 0.99
- ...
- Label 14: F1 = 1.00
- **Macro F1 = (0.98 + 0.99 + ... + 1.00) / 14 = 0.9979**

This is the standard **macro-averaging** approach for multi-label classification, giving equal weight to each label regardless of frequency.

---

## Related Documentation
- **METRICS_CALCULATION_EXPLAINED.md** - Explains macro vs. micro averaging
- **CNN_MACRO_AVERAGE_ANALYSIS.md** - Analysis of CNN achieving 99.79% macro F1
- **MODEL_COMPARISON.md** - Parameter comparison across all models

---

## Summary
The evaluation logic now:
1. ✅ Explicitly calculates macro scores after each model evaluation
2. ✅ Stores macro scores as separate rows with `Label='MACRO_AVERAGE'`
3. ✅ Displays macro scores in console, CSV, and HTML report
4. ✅ Updates all charts to show "Macro F1-Score" and "Macro Recall"
5. ✅ Adds ranking table comparing models by macro performance (test set)
6. ✅ Makes it clear which model is best with explicit "Best Model" text
7. ✅ **Statistical significance testing now uses test set best model as baseline**
8. ✅ F1 Score Trends chart filters out macro rows to show per-label details
9. ✅ **Eliminates confusion between CV and test set "winners"**

**Result:** Full transparency into model performance with explicit macro-averaged metrics throughout the entire evaluation pipeline. Statistical significance testing now aligns with test set rankings, using the test set winner as the baseline for CV fold-wise comparisons.
