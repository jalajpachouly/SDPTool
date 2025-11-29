# Metrics Calculation Methodology

## 📊 How F1, Recall, and Other Metrics Are Calculated

This document explains **exactly** how metrics are calculated in both `main.py` and `configurable_main.py`.

---

## 🔍 Summary: What You're Seeing

When you see **F1 scores and Recall** in the results, here's what's being calculated:

| Stage | Metric Type | Calculation Method | What It Means |
|-------|-------------|-------------------|---------------|
| **Cross-Validation** | Macro F1 & Recall | `average='macro'` across all labels | Unweighted average across Bug, Enhancement, Question |
| **Final Test Set** | Per-Label F1 & Recall | Binary F1/Recall for each label | Individual performance for each label |
| **Visualization** | Average F1 | Mean of per-label F1 scores | Average performance across labels |

---

## 1️⃣ Cross-Validation Metrics (During Training)

### **Location:** `utils/evaluation.py`

#### **Traditional ML Models**

```python
def cross_validation_score_multilabel(classifier, X, y, n_splits=10):
    # ...
    for fold, (train_index, test_index) in enumerate(mskf.split(X, y), 1):
        # Train and predict
        classifier.fit(X_train_cv, y_train_cv)
        y_pred_cv = classifier.predict(X_test_cv)

        # Calculate MACRO-averaged scores
        recall = recall_score(y_test_cv, y_pred_cv, average='macro', zero_division=0)
        f1 = f1_score(y_test_cv, y_pred_cv, average='macro', zero_division=0)
        
        recall_scores.append(recall)
        f1_scores.append(f1)
    
    # Return average across folds
    avg_recall = np.mean(recall_scores)
    avg_f1 = np.mean(f1_scores)
```

**What `average='macro'` means:**
- Calculate F1/Recall **separately** for each label (Bug, Enhancement, Question)
- Take the **unweighted average** (all labels weighted equally)
- Formula: `(F1_bug + F1_enhancement + F1_question) / 3`

**Example:**
```
Fold 1:
  - Bug F1: 0.85
  - Enhancement F1: 0.78
  - Question F1: 0.92
  
Macro F1 = (0.85 + 0.78 + 0.92) / 3 = 0.85

Output: "Fold 1: F1-Score = 0.8500"
```

#### **Deep Learning Models (MLP/CNN)**

```python
def cross_validation_score_deep_learning(model_builder, X, y, n_splits=10, epochs=10, ...):
    # ...
    for fold, (train_index, test_index) in enumerate(mskf.split(X, y), 1):
        # Train model
        model = model_builder()
        model.fit(X_train_cv, y_train_cv, ...)
        
        # Predict and threshold
        y_pred_cv_prob = model.predict(X_test_cv)
        y_pred_cv = (y_pred_cv_prob >= 0.5).astype(int)
        
        # Calculate MACRO-averaged scores (same as traditional ML)
        recall = recall_score(y_test_cv, y_pred_cv, average='macro', zero_division=0)
        f1 = f1_score(y_test_cv, y_pred_cv, average='macro', zero_division=0)
```

**Identical to traditional ML** - uses `average='macro'`.

---

## 2️⃣ Final Test Set Evaluation (Per-Label Metrics)

### **Location:** `utils/evaluation.py`

#### **Traditional ML Models**

```python
def evaluate_classifier(clf, clf_name, X_train, y_train, X_test, y_test, label_names):
    clf.fit(X_train, y_train)
    predictions = clf.predict(X_test)
    
    metrics = []
    n_labels = y_test.shape[1]  # 3 labels: Bug, Enhancement, Question
    
    # Calculate PER-LABEL metrics (NOT macro-averaged)
    for label_idx in range(n_labels):
        y_true_label = y_test[:, label_idx]
        y_pred_label = predictions[:, label_idx]
        
        # Binary classification metrics for this specific label
        recall = recall_score(y_true_label, y_pred_label, zero_division=0)
        f1 = f1_score(y_true_label, y_pred_label, zero_division=0)
        
        metrics.append({
            'Model': clf_name,
            'Label': label_names[label_idx],  # e.g., "Bug"
            'Recall': recall,                  # Recall for Bug only
            'F1': f1,                          # F1 for Bug only
            'Hamming Loss': hamming_loss_value
        })
```

**Key difference:** No `average='macro'` here! Each label gets its **own binary F1/Recall**.

**Example Output:**
```
Model: MultinomialNB, Label: Bug,         Recall: 0.82, F1: 0.85
Model: MultinomialNB, Label: Enhancement, Recall: 0.75, F1: 0.78
Model: MultinomialNB, Label: Question,    Recall: 0.88, F1: 0.92
```

#### **Deep Learning Models (MLP/CNN)**

```python
def evaluate_deep_learning_model(model, X_test, y_test, model_name, label_names):
    y_pred_prob = model.predict(X_test)
    y_pred = (y_pred_prob >= 0.5).astype(int)
    
    metrics = []
    n_labels = y_test.shape[1]
    
    # Same per-label approach as traditional ML
    for label_idx in range(n_labels):
        y_true_label = y_test[:, label_idx]
        y_pred_label = y_pred[:, label_idx]
        
        recall = recall_score(y_true_label, y_pred_label, zero_division=0)
        f1 = f1_score(y_true_label, y_pred_label, zero_division=0)
        
        metrics.append({
            'Model': model_name,
            'Label': label_names[label_idx],
            'Recall': recall,
            'F1': f1,
            'Hamming Loss': hamming_loss_value
        })
```

**Identical approach** - per-label binary metrics.

---

## 3️⃣ Visualization Aggregation

### **Location:** `configurable_main.py`

When creating bar charts and summary tables:

```python
def plot_model_summary_bar(df_results, data_type, output_dir):
    # df_results contains per-label metrics (3 rows per model)
    # Group by model and take MEAN of F1 across labels
    summary = df_results.groupby('Model')['F1'].mean().reset_index()
    
    # Plot the average F1
    sns.barplot(x='Model', y='F1', data=summary)
    ax.set_title(f'Average F1 by Model ({data_type})')
```

**What `.mean()` does:**
- Takes the 3 per-label F1 scores for each model
- Calculates arithmetic mean: `(F1_bug + F1_enhancement + F1_question) / 3`
- This is **equivalent to macro-averaging**

**Example:**
```
Original df_results:
  Model          Label        F1
  MultinomialNB  Bug          0.85
  MultinomialNB  Enhancement  0.78
  MultinomialNB  Question     0.92

After groupby().mean():
  Model          F1
  MultinomialNB  0.85  <-- (0.85 + 0.78 + 0.92) / 3
```

---

## 🎯 Complete Flow Example: CNN with Balanced Data

### **Phase 1: Cross-Validation (Training Phase)**

```
====================================
CNN - Balanced Data
====================================
Running cross-validation (10 folds)...

Fold 1: build_cnn_model Recall = 0.8234, F1-Score = 0.8456
       ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
       This is MACRO F1: average of (Bug F1 + Enhancement F1 + Question F1) / 3

Fold 2: build_cnn_model Recall = 0.8156, F1-Score = 0.8389
...
Fold 10: build_cnn_model Recall = 0.8345, F1-Score = 0.8512

Average CV Recall: 0.8267
Average CV F1: 0.8423
       ^^^^^^^^^^^^^^^^
       Average of the 10 fold-wise macro F1 scores
```

### **Phase 2: Final Test Set Evaluation**

```
===== Evaluating CNN Model =====

Training on full training set...
Epoch 1/20 ...
...
Epoch 20/20 ...

Evaluating on test set...

Results saved to CSV:
  Model  Label        Recall  F1     Hamming Loss
  CNN    Bug          0.87    0.89   0.052
  CNN    Enhancement  0.81    0.83   0.052
  CNN    Question     0.93    0.94   0.052
         ^^^^^^^^^^^^^^^^^^^^^^^^^^^
         Per-label binary metrics (NOT macro-averaged)
```

### **Phase 3: Visualization**

```python
# Aggregate the 3 rows for CNN:
summary = df.groupby('Model')['F1'].mean()

# Result:
# CNN: (0.89 + 0.83 + 0.94) / 3 = 0.8867

# Bar chart shows: CNN F1 = 0.89
```

---

## 📈 Different Averaging Methods (Explanation)

scikit-learn supports multiple averaging methods for multi-label metrics:

| Method | Formula | When to Use | Used in This Project? |
|--------|---------|-------------|----------------------|
| **macro** | `(F1_label1 + F1_label2 + ... + F1_labelN) / N` | When all labels equally important | ✅ **YES** (CV phase) |
| **micro** | Calculate metrics globally by counting total TP, FP, FN | When label imbalance matters | ❌ No |
| **weighted** | Weighted average based on label support | When labels have different importance | ❌ No |
| **samples** | Calculate metrics per sample, then average | For subset accuracy | ❌ No |
| **binary** | Treats as single-label binary (per-label) | For individual label evaluation | ✅ **YES** (final test) |

---

## 🔄 Why Two Different Approaches?

### **Cross-Validation (Macro Averaging)**
- **Purpose:** Quick assessment during training
- **Benefit:** Single score per fold (easier to compare)
- **Limitation:** Hides per-label performance differences

### **Final Test Set (Per-Label Metrics)**
- **Purpose:** Detailed analysis of each label
- **Benefit:** Shows exactly which labels perform well/poorly
- **Limitation:** More data to analyze (3x rows)

### **Visualization (Mean of Per-Label)**
- **Purpose:** Summarize per-label metrics into single score
- **Result:** Mathematically equivalent to macro averaging
- **Benefit:** Easy comparison across models

---

## 🧮 Mathematical Equivalence

**Cross-Validation Macro F1:**
```python
sklearn.metrics.f1_score(y_true, y_pred, average='macro')
# = (F1_bug + F1_enhancement + F1_question) / 3
```

**Final Test + Visualization Mean:**
```python
# Step 1: Calculate per-label F1 (binary)
f1_bug = f1_score(y_true[:, 0], y_pred[:, 0])
f1_enhancement = f1_score(y_true[:, 1], y_pred[:, 1])
f1_question = f1_score(y_true[:, 2], y_pred[:, 2])

# Step 2: Average in visualization
mean_f1 = (f1_bug + f1_enhancement + f1_question) / 3
```

**Result:** `mean_f1` = macro F1 (mathematically identical)

---

## 📊 What You See in Results Files

### **experiment_results.csv**

```csv
Model,Label,Recall,F1,Hamming Loss
MultinomialNB,Bug,0.8234,0.8456,0.052
MultinomialNB,Enhancement,0.7891,0.8123,0.052
MultinomialNB,Question,0.8567,0.8789,0.052
LogisticRegression,Bug,0.8345,0.8567,0.048
...
```

**Each row = per-label binary metric**

### **cv_results.csv**

```csv
Model,Data Type,Avg Recall,Avg F1
MultinomialNB,Balanced,0.8231,0.8456
MultinomialNB,Unbalanced,0.7923,0.8134
LogisticRegression,Balanced,0.8345,0.8578
...
```

**Each row = macro-averaged CV score**

### **Charts**

- **model_f1_summary_balanced.png**: Shows `.groupby('Model')['F1'].mean()` = macro F1
- **f1_comparison.png**: Side-by-side comparison using same mean calculation

---

## 🎯 Summary for Your CNN Performance Question

When you saw **CNN with NNLS balancing at 94% F1**, that was likely the:

1. **Macro F1 during cross-validation** (average across folds)
2. **OR** the **mean of per-label F1** scores from final test set

Both are mathematically equivalent:
- CV: `average='macro'` directly
- Final: Mean of binary F1 scores per label

**To verify which metric was 94%, check:**
- Console output during CV: `Fold X: F1-Score = 0.94XX`
- OR `experiment_results.csv`: Average F1 across Bug/Enhancement/Question rows

---

## 🔧 How to Get Other Averaging Methods

If you want to add **micro** or **weighted** F1:

### **Option 1: Modify `evaluation.py`**

```python
def cross_validation_score_multilabel(classifier, X, y, n_splits=10):
    # ... existing code ...
    
    # Add micro and weighted
    recall_micro = recall_score(y_test_cv, y_pred_cv, average='micro', zero_division=0)
    f1_micro = f1_score(y_test_cv, y_pred_cv, average='micro', zero_division=0)
    f1_weighted = f1_score(y_test_cv, y_pred_cv, average='weighted', zero_division=0)
    
    return {
        'Recall': avg_recall,           # macro
        'F1': avg_f1,                   # macro
        'F1_micro': np.mean(f1_micro_scores),
        'F1_weighted': np.mean(f1_weighted_scores)
    }
```

### **Option 2: Add to Configuration**

Update `main_config.json`:

```json
"evaluation": {
    "averaging_methods": ["macro", "micro", "weighted"],
    "primary_metric": "macro"
}
```

---

## 📝 Conclusion

**Current Implementation:**
- ✅ Cross-validation: **Macro F1/Recall** (unweighted average across labels)
- ✅ Final test: **Per-label binary F1/Recall** (individual label performance)
- ✅ Visualizations: **Mean of per-label F1** (equivalent to macro)

**All reported F1 scores are macro-averaged** - treating Bug, Enhancement, and Question equally.

This is appropriate for multi-label classification where all labels are equally important.
