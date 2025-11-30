# Main Threats to Validity in Dataset Construction Pipeline

## Overview
This document addresses the main threats to validity in our dataset construction pipeline and the mitigation strategies we implemented.

---

## 1. Inconsistent Labeling Practices

### Threat
GitHub repositories rely on contributors to manually label issues, which leads to several problems:
- Contributors use different labeling conventions across repositories
- Some issues have incomplete or missing labels
- Labeling decisions are subjective and vary by contributor
- Label terminology changes over time within the same project

### Mitigation
We implemented a color-based label filtering system to ensure consistency:
- Selected only labels with 10 specific predefined colors that represent high-confidence defect types
- Used database foreign key constraints to maintain referential integrity between issues and labels
- Excluded issues with incomplete or untriaged labels during data collection
- Applied SQL queries to filter out low-confidence labels before dataset creation

This approach ensures that only issues with consistent, well-established label types are included in our dataset.

---

## 2. Class Imbalance

### Threat
The dataset exhibits severe class imbalance across defect types:
- Critical defects like `type_blocker` are significantly underrepresented
- Common defects like `type_bug` and `type_documentation` dominate the dataset
- This imbalance causes models to be biased toward majority classes
- Minority defect types are poorly predicted, which is problematic since they are often the most critical

### Mitigation
We used Non-Negative Least Squares (NNLS) resampling for the multilabel dataset:
- NNLS analyzes how labels co-occur in the original data (e.g., "bug" often appears with "documentation")
- It uses mathematical optimization to determine how many samples to draw for each label
- The target is set to 600 samples per label to achieve balance
- Unlike other methods, NNLS preserves the natural relationships between labels
- Resampling uses actual defect reports (with replacement when needed) rather than creating synthetic data

This ensures balanced representation while maintaining realistic label combinations that reflect real-world software defects.

---

## 3. Data Quality Issues

### Threat
Raw GitHub issue data contains significant noise and inconsistencies:
- Code snippets and stack traces mixed with natural language descriptions
- HTML tags, special characters, and formatting artifacts
- Irrelevant comments that don't describe the defect
- Duplicate or redundant records
- Missing or incomplete issue descriptions

### Mitigation
We implemented a comprehensive 7-step automated preprocessing pipeline:

1. **Stop Word Removal**: Removes common English words that don't contribute to defect classification (using a curated list of stop words)

2. **Lower-casing**: Converts all text to lowercase for consistency

3. **Remove Incomplete and Redundant Data**: Filters out issues with missing critical information and eliminates duplicates

4. **Word Extraction**: Extracts meaningful words while filtering out code snippets, URLs, and special characters

5. **Data Enrichment and Discretization**: 
   - Calculates time-to-fix by measuring days between issue creation and closure
   - Extracts code change metrics (lines changed, packages affected, classes modified)
   - Computes change complexity scores (LOW/MEDIUM/HIGH) based on extent of modifications

6. **Data Labeling**: Validates and normalizes label assignments

7. **Data Validation**: Final validation step to ensure data integrity before dataset export

Each issue's title and body are processed through this pipeline, with cleaned versions stored separately from the raw data.

---

## 4. Limited Dataset Size

### Threat
Our dataset sizes are relatively modest:
- Multilabel dataset: 1,386 defect reports
- Multiclass dataset: 2,003 defect reports
- Small datasets increase risk of overfitting
- Models may memorize training examples rather than learning generalizable patterns
- Limited data reduces statistical power of evaluation

### Mitigation
We employed multiple strategies to maximize dataset utility:

**Stratified Splitting**:
- Used multilabel stratified splitting to maintain label distribution in train/test sets
- Ensures minority classes are represented in both training and test data
- 80/20 train-test split with stratification

**Rigorous Cross-Validation**:
- Traditional ML models: 10-fold cross-validation
- MLP model: 5-fold cross-validation  
- CNN model: 10-fold cross-validation
- Cross-validation provides robust performance estimates and reduces overfitting risk

**Intelligent Resampling**:
- NNLS balancing maximizes use of existing samples through strategic resampling
- Creates balanced training sets without simply discarding majority class samples

---

## 5. Domain-Specific Bias

### Threat
The dataset comes exclusively from the Spring Framework ecosystem:
- Spring Framework, Spring Boot, Spring Data projects
- All Java-based enterprise applications
- May not generalize to other technology stacks (Node.js, Python, Go, etc.)
- Domain-specific terminology and patterns
- Specific to enterprise Java development practices

### Mitigation
We took several steps to reduce bias and improve potential generalizability:

**Multi-Repository Collection**:
- Designed the tool to support configurable repository selection
- Users can specify repository name and record ranges through the UI
- Database schema supports data from multiple projects

**High-Quality Repository Selection**:
- Selected repositories with active development and diverse defect types
- Focused on projects with consistent labeling practices
- Chose repositories with substantial contributor activity

**Domain-Agnostic Feature Engineering**:
- Used TF-IDF vectorization which works across different technical domains
- Applied chi-square feature selection to identify discriminative features regardless of domain
- Selected top 50 features based on statistical relevance rather than domain knowledge

**Future Work Recommendations**:
We acknowledge this limitation and recommend:
- Expanding dataset to include non-Java ecosystems
- Cross-project validation across different technology stacks
- Testing model transferability to other software domains

---

## 6. Data Integrity and Consistency

### Threat
Database operations could lead to inconsistencies:
- Comments without corresponding parent issues
- Code changes referencing non-existent issues
- Orphaned labels after issue deletion
- Inconsistent database state during preprocessing

### Mitigation
We implemented strict database integrity controls:

**Schema-Level Constraints**:
- Foreign key relationships between all related tables
- Comments, labels, and code changes all reference parent issues via foreign keys
- Database automatically prevents orphaned records

**Ordered Deletion Scripts**:
- Clean-up scripts respect foreign key dependencies
- Delete child records (comments, labels) before parent records (issues)
- Ensures database can be safely reset between runs

---

## 7. Overfitting Risk for Deep Learning Models

### Threat
Deep learning models (CNN and MLP) are prone to overfitting on small datasets:
- Models may memorize training examples rather than learning patterns
- Poor generalization to unseen defects
- Unstable performance across different data samples

### Mitigation
We implemented multiple regularization techniques:

**Early Stopping**:
- Monitors validation loss during training
- Stops training when validation performance stops improving (patience of 5 epochs)
- Restores weights from the epoch with best validation performance
- Prevents excessive training that leads to overfitting

**Dropout Regularization**:
- Applied 50% dropout in neural network layers
- Randomly deactivates neurons during training
- Forces model to learn robust features rather than memorizing patterns

**Validation Split**:
- Reserved 20% of training data for validation during model training
- Provides independent performance monitoring during training
- Validation set never used for weight updates

---

## Conclusion

Our dataset construction pipeline addresses validity threats through multiple defensive layers:

1. **Automated preprocessing** reduces manual errors and ensures consistency
2. **NNLS resampling** balances the dataset while preserving label relationships
3. **Stratified cross-validation** provides robust performance estimates
4. **Database integrity constraints** prevent data inconsistencies
5. **Feature engineering** creates domain-agnostic representations
6. **Regularization techniques** prevent model overfitting
7. **Color-coded filtering** improves label quality

These strategies work together to create a reliable dataset despite the inherent challenges of working with open-source issue tracking data. While some threats (like domain-specific bias) cannot be completely eliminated, we have taken reasonable steps to mitigate their impact and documented these limitations for transparency.

**Future improvements** should focus on expanding to diverse technology ecosystems, increasing dataset size through additional repository collection, and conducting cross-domain validation studies to better understand model generalizability limits.
