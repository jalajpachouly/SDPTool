# Multi-label Traditional ML Hyperparameters (Random Forest, Logistic Regression, Multinomial NB)

This covers the multi-label tabs in the UI for the classic models.

## Shared toggles
- **Enable \<model\>**: Turns the model on/off for this run.
- **Use classifier chain**: Wraps the base classifier in a classifier chain to capture label dependencies (common for multi-label tasks).
- **Run cross-validation**: When checked, performs K-fold CV with the shared CV folds value; otherwise trains once on the full training split.
- **CV folds (traditional ML)** (`cv_n_splits`): Number of folds (K) for CV. Applies to all enabled traditional models.

## Random Forest
- **Trees** (`n_estimators`): Number of trees in the forest. More trees can improve stability but cost more time.
- **Random state** (`random_state`): Seed for reproducibility of bootstrapping and feature sampling.

## Logistic Regression
- **Max iterations** (`max_iter`): Upper bound on optimization iterations. Increase if the solver hasn’t converged at the default.

## Multinomial NB
- (No numeric hyperparameters in the UI). Uses the shared toggles above:
  - **Use classifier chain**: Whether to chain NB classifiers.
  - **Run cross-validation**: Whether to evaluate with K-fold CV.
