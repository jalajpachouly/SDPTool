# Model Persistence and Prediction Usage Guide

This guide explains how to use the model persistence and prediction features through the UI configuration.

## Overview

The system now supports:
1. **Automatic model persistence** - Save the best performing model after training
2. **Prediction mode** - Load a saved model and make predictions without retraining

## Configuration Options

### Model Persistence

Add this section to your `ui_config.json` or `main_config.json`:

```json
{
  "model_persistence": {
    "enabled": true,
    "save_best_model": true,
    "custom_model_name": "MyExperiment_Run1",
    "selection_metric": "macro_f1",
    "models_directory": "multilable-prediction/models"
  }
}
```

**Parameters:**
- `enabled` (boolean): Enable/disable model persistence feature
- `save_best_model` (boolean): Whether to save the best model after training
- `custom_model_name` (string, optional): Custom prefix for the model directory name
- `selection_metric` (string): Metric to use for selecting best model
  - Options: `"macro_f1"`, `"micro_f1"`, `"macro_recall"`, `"micro_recall"`, `"hamming_loss"`
  - Default: `"macro_f1"`
- `models_directory` (string): Base directory for saving models

### Prediction Mode

Add this section to run predictions with a saved model:

```json
{
  "prediction": {
    "enabled": true,
    "run_id": "CNN_Best_20231205_143022",
    "mode": "interactive",
    "input_file": null,
    "row_numbers": null
  }
}
```

**Parameters:**
- `enabled` (boolean): Set to `true` to run prediction mode (skips training)
- `run_id` (string): The run ID of the saved model to use
- `mode` (string): Prediction mode
  - `"interactive"`: Interactive text input mode
  - `"csv"`: Batch prediction from CSV file
  - `"row"`: Predict specific rows from the dataset
- `input_file` (string): CSV file path (required for `csv` mode)
- `row_numbers` (array): List of row numbers (required for `row` mode, e.g., `[0, 5, 10]`)

## Usage Examples

### Example 1: Training with Model Persistence

**ui_config.json:**
```json
{
  "experiment_name": "RandomForest_Experiment",
  "model_persistence": {
    "enabled": true,
    "save_best_model": true,
    "selection_metric": "macro_f1"
  },
  "prediction": {
    "enabled": false
  },
  "models": {
    "traditional_ml": {
      "random_forest": {
        "enabled": true
      }
    }
  }
}
```

**Run training:**
```powershell
cd multilable-prediction
python src/main.py
```

**Output:**
- Training runs normally
- Best model is automatically saved to `multilable-prediction/models/<ModelName>_<Timestamp>/`
- Console shows: `[SUCCESS] Best model saved successfully to: multilable-prediction/models/RandomForest_20231205_143022`

### Example 2: Interactive Prediction

**ui_config.json:**
```json
{
  "experiment_name": "Prediction_Session",
  "prediction": {
    "enabled": true,
    "run_id": "RandomForest_20231205_143022",
    "mode": "interactive"
  }
}
```

**Run prediction:**
```powershell
cd multilable-prediction
python src/main.py
```

**Interactive session:**
```
Enter bug report description (or 'quit' to exit): Memory leak in authentication module causes server crash
Predictions: ['Corrective', 'Perfective']

Enter bug report description (or 'quit' to exit): quit
```

### Example 3: Batch Prediction from CSV

**ui_config.json:**
```json
{
  "prediction": {
    "enabled": true,
    "run_id": "CNN_Best_20231205_150000",
    "mode": "csv",
    "input_file": "data/new_bugs.csv"
  }
}
```

**Run prediction:**
```powershell
python src/main.py
```

### Example 4: Predict Specific Dataset Rows

**ui_config.json:**
```json
{
  "prediction": {
    "enabled": true,
    "run_id": "MLP_20231205_120000",
    "mode": "row",
    "row_numbers": [0, 5, 10, 15, 20]
  }
}
```

## Finding Saved Models

### Location
All models are saved in: **`multilable-prediction/models/`**

### Directory Structure
```
multilable-prediction/models/
├── RandomForest_20231205_143022/
│   ├── model.pkl              # Trained model
│   ├── vectorizer.pkl         # TF-IDF vectorizer
│   ├── feature_selector.pkl   # Chi-square selector
│   ├── metadata.json          # Model info and metrics
│   └── training_config.json   # Configuration used
├── CNN_Best_20231205_150000/
│   ├── model.h5               # Keras model
│   ├── vectorizer.pkl
│   ├── tokenizer.pkl          # Keras tokenizer
│   ├── metadata.json
│   └── training_config.json
└── MLP_20231205_120000/
    └── ...
```

### List Available Models
```powershell
# Windows PowerShell
Get-ChildItem "multilable-prediction\models" -Directory | Select-Object Name

# View model metadata
Get-Content "multilable-prediction\models\<run_id>\metadata.json" | ConvertFrom-Json
```

### View Model Metrics
```powershell
Get-Content "multilable-prediction\models\RandomForest_20231205_143022\metadata.json"
```

**Example output:**
```json
{
  "run_id": "RandomForest_20231205_143022",
  "model_name": "RandomForest",
  "model_type": "traditional_ml",
  "saved_at": "2023-12-05T14:30:22",
  "metrics": {
    "macro_f1": 0.8523,
    "micro_f1": 0.8734,
    "macro_recall": 0.8412,
    "micro_recall": 0.8701,
    "hamming_loss": 0.1234
  }
}
```

## Command Line Prediction (Alternative)

You can also use the standalone prediction script:

```powershell
# Interactive mode
python src/predict_with_model.py --run_id RandomForest_20231205_143022 --mode interactive

# Batch CSV prediction
python src/predict_with_model.py --run_id CNN_Best_20231205_150000 --mode csv --input new_bugs.csv

# Predict specific rows
python src/predict_with_model.py --run_id MLP_20231205_120000 --mode row --rows 0 5 10
```

## Tips

1. **Finding the best run_id**: Check the console output after training - it shows the saved model path
2. **Custom naming**: Use `custom_model_name` to organize experiments (e.g., "Experiment_Balanced_Data_v2")
3. **Metric selection**: Use `macro_f1` for balanced evaluation across all labels
4. **Transferring models**: Copy the entire model directory to another machine to use the model there

## Troubleshooting

### Error: "Prediction mode enabled but 'run_id' not specified"
**Solution**: Add the `run_id` field in your prediction config

### Error: "Model directory not found"
**Solution**: Check that the `run_id` exists in `multilable-prediction/models/`

### Error: "Failed to load model"
**Solution**: Ensure all model artifacts (.pkl, .h5 files) are present in the model directory
