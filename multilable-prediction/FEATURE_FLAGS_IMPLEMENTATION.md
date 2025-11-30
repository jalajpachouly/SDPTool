# Feature Flags Implementation for main.py

## Overview
Implemented comprehensive feature flag system in `main.py` similar to `configurable_main.py`, allowing full control over model execution, visualizations, and data processing through `configs/ui_config.json`.

## Changes Made

### 1. Configuration Constants (Lines 55-105)
Created default constants for all configurable parameters:

**Feature Engineering:**
- `DEFAULT_TOP_K = 50` - Number of top features to select
- `DEFAULT_TOP_K_PLOT = 20` - Number of features to plot
- `DEFAULT_USE_WORDCLOUD_VOCABULARY = True` - Use wordcloud vocabulary

**Traditional ML Models:**
- `DEFAULT_MULTINOMIAL_NB_ENABLED = True`
- `DEFAULT_LOGISTIC_REGRESSION_ENABLED = True`
- `DEFAULT_LOGISTIC_REGRESSION_MAX_ITER = 10000`
- `DEFAULT_RANDOM_FOREST_ENABLED = True`
- `DEFAULT_RANDOM_FOREST_N_ESTIMATORS = 100`
- `DEFAULT_RANDOM_FOREST_RANDOM_STATE = 42`
- `DEFAULT_USE_CLASSIFIER_CHAIN = True`

**MLP Deep Learning:**
- `DEFAULT_MLP_ENABLED = True`
- `DEFAULT_MLP_CV_N_SPLITS = 5`
- `DEFAULT_MLP_EPOCHS = 50`
- `DEFAULT_MLP_BATCH_SIZE = 32`
- `DEFAULT_MLP_VALIDATION_SPLIT = 0.2`
- `DEFAULT_MLP_EARLY_STOPPING_PATIENCE = 5`

**CNN Deep Learning:**
- `DEFAULT_CNN_ENABLED = True`
- `DEFAULT_CNN_CV_N_SPLITS = 10`
- `DEFAULT_CNN_CV_EPOCHS = 10`
- `DEFAULT_CNN_CV_BATCH_SIZE = 32`
- `DEFAULT_CNN_EPOCHS = 20`
- `DEFAULT_CNN_BATCH_SIZE = 32`
- `DEFAULT_CNN_MAX_WORDS = 5000`
- `DEFAULT_CNN_MAX_LEN = 100`
- `DEFAULT_CNN_EMBEDDING_DIM = 100`
- `DEFAULT_CNN_EARLY_STOPPING_PATIENCE = 5`

**Cross-Validation:**
- `DEFAULT_RUN_CROSS_VALIDATION = True` - Global CV flag

**Visualizations:**
- `DEFAULT_VISUALIZATIONS_ENABLED = True`
- `DEFAULT_WORDCLOUDS_ENABLED = True`
- `DEFAULT_DESCRIPTION_LENGTH_ENABLED = True`
- `DEFAULT_CLASS_DISTRIBUTION_ENABLED = True`
- `DEFAULT_CORRELATION_MATRIX_ENABLED = True`
- `DEFAULT_TOP_FEATURES_ENABLED = True`
- `DEFAULT_F1_SCORES_ENABLED = True`

**Data Processing:**
- `DEFAULT_RUN_BALANCED = True`
- `DEFAULT_RUN_UNBALANCED = True`

### 2. Configuration Loading Functions

**`load_ui_config(config_path='configs/ui_config.json')`**
- Loads JSON configuration file
- Returns None if file not found (uses defaults)
- Logs clear messages about config status

**`get_config_value(ui_config, path, default_value)`**
- Safely extracts nested config values using dot notation
- Falls back to default if not found
- **Logs [INFO] message clearly when using defaults**

### 3. Main Function Refactoring

**Feature-Flagged Sections:**

1. **Visualizations** (Lines 243-263)
   - Description length: `visualizations.description_length`
   - Class distribution: `visualizations.class_distribution`
   - Correlation matrix: `visualizations.correlation_matrix`
   - Word clouds: `visualizations.word_clouds`
   - Top features: `visualizations.top_features`
   - F1 scores: `visualizations.f1_scores`

2. **Traditional ML Models** (Lines 277-311)
   - Each model (NB, LR, RF) can be individually enabled/disabled
   - Classifier chain usage configurable
   - Hyperparameters read from config
   - Cross-validation controlled by global flag

3. **MLP Deep Learning** (Lines 341-371)
   - Enabled by `models.deep_learning.mlp.enabled`
   - All hyperparameters configurable
   - CV splits, epochs, batch size from config

4. **CNN Deep Learning** (Lines 377-425)
   - Enabled by `models.deep_learning.cnn.enabled`
   - All CNN-specific params configurable
   - Tokenizer settings (max_words, max_len) from config

5. **Results Combination** (Lines 437-450)
   - Only combines results from enabled models
   - Shows warning if no models enabled

### 4. Entry Point (__main__)

Updated to respect data type feature flags:
- `data.run_unbalanced` - Controls unbalanced data processing
- `data.run_balanced` - Controls balanced data processing
- Clear status messages showing what will run
- Warning if both disabled

## Configuration File Compatibility

The implementation uses the **EXACT same JSON structure** as `ui_config.json`:

```json
{
  "models": {
    "traditional_ml": {
      "random_forest": { "enabled": true, "n_estimators": 100 },
      "logistic_regression": { "enabled": true, "max_iter": 10000 },
      "multinomial_nb": { "enabled": true },
      "run_cross_validation": false
    },
    "deep_learning": {
      "mlp": { "enabled": false, "epochs": 100, ... },
      "cnn": { "enabled": false, "epochs": 20, ... }
    }
  },
  "visualizations": {
    "enabled": true,
    "word_clouds": false,
    "description_length": true,
    ...
  },
  "feature_engineering": {
    "top_k": 50,
    "top_k_plot": 20,
    ...
  },
  "data": {
    "run_balanced": true,
    "run_unbalanced": false
  }
}
```

## Behavior

### Config Not Found
- Uses all default constants
- Logs: `[INFO] UI config file not found: configs/ui_config.json`
- Logs: `[INFO] Using default configuration constants`

### Config Value Missing
- Uses default for that specific value
- Logs: `[INFO] Config 'path.to.value' not found, using default: <value>`

### Config Found and Valid
- Logs: `[OK] Loaded UI configuration from: configs/ui_config.json`
- Uses values from JSON, defaults not logged

## Model Execution Flow

1. **Load config** at start of main()
2. **Extract all values** with defaults
3. **Print configuration summary** showing which models enabled
4. **Execute only enabled models** with configured parameters
5. **Combine results** from enabled models only
6. **Visualize** only if visualization flags enabled

## Key Features

- **All hardcoded values moved to constants**
- **JSON overrides defaults when provided**
- **Clear, visible logging** of defaults usage
- **Every feature is flag-based** (models, CV, visualizations)
- **Global CV flag** controls all model cross-validation
- **No approach changes** - same logic, just configurable
- **Backward compatible** - works without config file

## Testing

To test different configurations:

1. **All models disabled:**
   ```json
   {
     "models": {
       "traditional_ml": { "random_forest": { "enabled": false }, ... },
       "deep_learning": { "mlp": { "enabled": false }, "cnn": { "enabled": false } }
     }
   }
   ```
   Result: Shows warning, no models run

2. **Only Random Forest:**
   ```json
   {
     "models": {
       "traditional_ml": {
         "random_forest": { "enabled": true },
         "logistic_regression": { "enabled": false },
         "multinomial_nb": { "enabled": false }
       },
       "deep_learning": { "mlp": { "enabled": false }, "cnn": { "enabled": false } }
     }
   }
   ```
   Result: Only RF trains and evaluates

3. **No visualizations:**
   ```json
   {
     "visualizations": { "enabled": false }
   }
   ```
   Result: Skips all visualization steps

4. **No config file:**
   - Delete or rename `configs/ui_config.json`
   - Result: Uses all defaults, all models run

## Console Output Example

```
================================================================================
STARTING MULTI-LABEL CLASSIFICATION PIPELINE
================================================================================
Run Unbalanced Data: False
Run Balanced Data: True
================================================================================

[INFO] Unbalanced data processing disabled by feature flag

---------------------------------------------------------

Processing with Balanced Data.

================================================================================
CONFIGURATION FOR BALANCED DATA
================================================================================

[MODELS ENABLED]
  MultinomialNB: True
  LogisticRegression: True
  RandomForest: True
  MLP: False
  CNN: False
  Cross-Validation: False
  Visualizations: True
================================================================================

[INFO] MLP model disabled by feature flag
[INFO] CNN model disabled by feature flag

================================================================================
PIPELINE COMPLETED
================================================================================
```

## Files Modified

- **main.py**: Complete refactoring with feature flags
  - Added: Import statements (json, sys, Path)
  - Added: 50+ default constants
  - Added: load_ui_config() function
  - Added: get_config_value() function
  - Modified: main() function - completely feature-flagged
  - Modified: __main__ block - respects data type flags

## No Breaking Changes

- Original logic preserved completely
- Same model training approach
- Same evaluation methods
- Same visualization functions
- Only difference: execution is now controllable via JSON
