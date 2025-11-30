---
tags: [machine-learning, multilabel-classification, data-balancing, defect-prediction]
aliases: [NNLS vs SMOTE, Data Balancing Comparison]
created: 2025-11-30
---

# NNLS vs. SMOTE for Multilabel Defect Prediction

> [!info] Overview
> Comparison of Non-Negative Least Squares (NNLS) and Synthetic Minority Over-sampling Technique (SMOTE) for handling class imbalance in multilabel defect prediction tasks.

## Key Technical Differences

### 1. Approach to Resampling

> [!note] SMOTE (Synthetic Minority Over-sampling Technique)
> Generates synthetic samples by interpolating between existing minority class samples in feature space using k-nearest neighbors. It treats each label independently, creating new feature vectors without considering relationships between labels.

> [!success] NNLS (Non-Negative Least Squares Resampling)
> Uses label co-occurrence patterns from the training data to determine optimal sample counts per label. It solves the optimization problem: `nnls(cond_prob_matrix, target_counts)` where `cond_prob_matrix[i,j]` represents the conditional probability of label j appearing when label i is present.

### 2. Label Correlation Preservation

> [!warning] SMOTE Limitation
> SMOTE creates synthetic samples independently for each label. For example, if "bug" and "documentation" co-occur 80% of the time in real data, SMOTE may create samples where this relationship is lost, leading to unrealistic label combinations.

> [!example] NNLS Implementation Approach
> NNLS builds a conditional probability matrix that captures label co-occurrence patterns from the training data. The matrix is normalized by diagonal elements to represent conditional probabilities. The resampling process then uses non-negative least squares optimization to determine optimal sample counts per label while preserving these relationships. Samples are drawn from existing data with replacement when necessary, ensuring authentic defect report combinations.

### 3. Handling Multilabel Data

| Aspect | SMOTE | NNLS |
|--------|-------|------|
| **Label Dependencies** | Ignores co-occurrence patterns | Explicitly preserves via conditional probability matrix |
| **Multidimensional Labels** | Treats as independent binary problems | Respects multidimensional nature of defects |
| **Sample Source** | Creates synthetic feature vectors | Resamples from existing data with label-aware weights |
| **Text Compatibility** | Requires numerical features | Works with any feature type (text, numerical) |

## Why NNLS Was Preferred

### 1. Real-World Label Dependencies

> [!tip] Label Co-occurrence in Software Defects
> In software defect data, labels frequently co-occur:
> - A "bug" requiring code changes often needs "documentation" updates
> - "Enhancement" requests may be "critical" priority
> - NNLS ensures: If 85% of "blocker" defects also have "bug" label in training data, resampled data maintains ~85% co-occurrence

### 2. Avoids Synthetic Noise in Text Data
- SMOTE interpolation makes sense for numerical features (e.g., temperature, price)
- Text feature interpolation can create nonsensical combinations
- NNLS resamples actual defect reports, maintaining linguistic coherence

### 3. Optimization-Based Balancing
NNLS solves a constrained least squares problem: it minimizes the difference between the desired target counts and the predicted sample distribution, subject to non-negativity constraints. This mathematical optimization ensures that the sampling strategy respects the inherent label structure while achieving balanced representation across all defect types.

### 4. Empirical Performance from Reports

> [!success] Performance Results (Section 10 Analysis)
> 
> **CNN Model with NNLS Balancing:**
> - F1 Score: 0.7575 (Unbalanced) → 0.9423 (Balanced) = **+24.40% improvement**
> - Recall: 0.7606 (Unbalanced) → 0.9416 (Balanced) = **+23.80% improvement**  
> - Hamming Loss: 0.2115 (Unbalanced) → 0.1389 (Balanced) = **-34.33% reduction**
> 
> **Average Across All Models:**
> - F1 improvement: +10-25%
> - Hamming Loss reduction: 20-40%

## Implementation Approach

### SMOTE Strategy:
SMOTE requires treating each label independently, iterating through each label column and applying oversampling separately. This approach fundamentally destroys inter-label correlations since each label is balanced without awareness of other labels' distributions.

### NNLS Strategy:
NNLS takes a holistic approach by first constructing a conditional probability matrix from all labels simultaneously, then using optimization to determine sample counts that balance the dataset while maintaining the discovered label relationships. The target count (typically 600 samples per label) is achieved through intelligent resampling from existing authentic defect reports.

## Conclusion

> [!summary] Why NNLS is Superior for Multilabel Defect Prediction
> 
> NNLS was preferred over SMOTE because it:
> 
> 1. **Preserves label co-occurrence patterns** through conditional probability matrix
> 2. **Maintains data authenticity** by resampling existing records rather than interpolating
> 3. **Optimizes sampling** mathematically using least squares optimization
> 4. **Achieves superior empirical results** with 20-40% metric improvements
> 5. **Handles text-based features** naturally without feature space interpolation
> 
> SMOTE remains effective for single-label numerical problems but cannot account for the multidimensional, correlated nature of multilabel defect classification where defects commonly have overlapping characteristics (e.g., critical bugs requiring documentation updates).

## Mathematical Formulation

> [!note] NNLS Optimization Problem
> ```
> minimize: ||C × x - t||²
> subject to: x ≥ 0
> 
> where:
>   C = conditional probability matrix (n_labels × n_labels)
>   C[i,j] = P(label_j | label_i) 
>   x = optimal number of samples to draw per label
>   t = target count vector (desired samples per label)
> ```

### Co-occurrence Matrix Construction:
The co-occurrence matrix is computed as the matrix product of the transposed label matrix with itself (Y^T × Y), where Y represents the binary label matrix with dimensions (n_samples × n_labels). Each element CoOccurrence[i,j] represents the number of samples where both label_i and label_j are simultaneously present.

### Conditional Probability Normalization:
The conditional probability matrix is obtained by normalizing each column of the co-occurrence matrix by its diagonal element: C[i,j] = CoOccurrence[i,j] / CoOccurrence[i,i]. This represents the probability that label_j appears given that label_i is present in a defect report.

## Benefits for Software Defect Prediction

> [!check] Key Advantages
> 1. **Realistic Defect Combinations**: Maintains patterns like "bug + critical" or "enhancement + documentation"
> 2. **Domain Knowledge Preservation**: Label correlations reflect actual software development practices
> 3. **Text Data Compatibility**: Works with TF-IDF features without creating invalid word combinations
> 4. **Balanced Label Distribution**: Achieves target counts while respecting dependencies
> 5. **Reproducibility**: Mathematical optimization produces consistent results

---

## Related Topics
- [[Machine Learning]]
- [[Multilabel Classification]]
- [[Class Imbalance]]
- [[Software Defect Prediction]]
