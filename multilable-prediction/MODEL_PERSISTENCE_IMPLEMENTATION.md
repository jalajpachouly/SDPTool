# Model Persistence and Prediction System Implementation Guide

## Overview
This implementation adds model persistence and prediction capabilities to SDPTool's multilabel classification pipeline.

## Features Implemented

### 1. Model Persistence (`model_persistence.py`)
- **Location**: `multilable-prediction/src/utils/model_persistence.py`
- **Features**:
  - Save best performing model with metadata
  - Auto-generate or custom run IDs
  - Store all artifacts (model, vectorizer, feature selector, tokenizer)
  - Load saved models with all preprocessing components
  - List all saved models with metrics
  - Delete models
  - Find best model by metric

### 2. Prediction Engine (`predict_with_model.py`)
- **Location**: `multilable-prediction/src/predict_with_model.py`
- **Features**:
  - Load saved models and make predictions
  - **Multiple input modes**:
    - **Text mode**: Predict single text input
    - **CSV mode**: Predict entire CSV file
    - **Rows mode**: Predict specific rows from original dataset
  - Confidence scores for each label
  - Feature importance explanation (for traditional ML)
  - Ground truth comparison (if available)
  - Command-line interface

### 3. Prediction Report Generator (`prediction_report_generator.py`)
- **Location**: `multilable-prediction/src/utils/prediction_report_generator.py`
- **Features**:
  - Beautiful HTML reports with:
    - Summary statistics
    - Individual predictions with confidence scores
    - Ground truth comparison (if available)
    - Label distribution statistics
    - Color-coded match indicators
    - Confidence visualization bars
  - Professional styling for presentations
  - Print-friendly format

### 4. Main.py Integration (`MODIFICATION_GUIDE.py`)
- **Location**: `multilable-prediction/MODIFICATION_GUIDE.py`
- **Features**:
  - Automatic best model selection (based on F1 score)
  - Save model after training completes
  - Track all trained models
  - Minimal code changes required

## Directory Structure

```
multilable-prediction/
├── src/
│   ├── main.py (needs modification)
│   ├── predict_with_model.py (NEW)
│   └── utils/
│       ├── model_persistence.py (NEW)
│       ├── prediction_report_generator.py (NEW)
│       └── ... (existing files)
├── models/ (NEW - auto-created)
│   └── <run_id>/
│       ├── model.pkl or model.h5
│       ├── vectorizer.pkl
│       ├── feature_selector.pkl (optional)
│       ├── tokenizer.pkl (optional)
│       ├── metadata.json
│       └── training_config.json
└── output/
    └── predictions/ (NEW - auto-created)
        └── prediction_report_<run_id>_<timestamp>.html
```

## Usage Examples

### Training and Saving Model

```bash
# Train models (existing process)
python multilable-prediction/src/main.py

# Best model is automatically saved to multilable-prediction/models/
# with run ID from experiment_name in ui_config.json
```

### Making Predictions

#### Mode 1: Predict from Text
```bash
python multilable-prediction/src/predict_with_model.py \
    <run_id> \
    --mode text \
    --text "Fix critical bug in authentication module" \
    --explain

# Output: Prediction with confidence scores and feature importance
```

#### Mode 2: Predict from CSV File
```bash
python multilable-prediction/src/predict_with_model.py \
    <run_id> \
    --mode csv \
    --csv data/new_defects.csv \
    --output results.json

# Output: JSON file with predictions for all rows
```

#### Mode 3: Predict Specific Dataset Rows
```bash
python multilable-prediction/src/predict_with_model.py \
    <run_id> \
    --mode rows \
    --dataset data/dataset.csv \
    --rows "1,5,10,15-20" \
    --output predictions.json

# Output: Predictions with ground truth comparison
```

### List Saved Models

```python
from utils.model_persistence import ModelPersistence

persistence = ModelPersistence()
models = persistence.list_models()

for model in models:
    print(f"{model['run_id']}: {model['model_name']} - F1: {model['metrics']['f1_score']:.4f}")
```

### Generate Prediction Report

```python
from predict_with_model import ModelPredictor
from utils.prediction_report_generator import PredictionReportGenerator

# Make predictions
predictor = ModelPredictor(run_id='RF_Best_20231205_143022')
results = predictor.predict_from_dataset_rows(
    dataset_path='data/dataset.csv',
    row_numbers=[1, 5, 10, 15, 20]
)

# Generate HTML report
report_gen = PredictionReportGenerator(results)
report_path = report_gen.generate_report()
print(f"Report generated: {report_path}")
```

## Java UI Integration (PredictionPanel.java)

### Proposed UI Structure

```
┌─────────────────────────────────────────────────────────┐
│  Prediction Panel                                        │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌─────────────┬──────────────────────────────────┐    │
│  │ Train New   │ Run Existing Model               │    │
│  │ Model       │                                   │    │
│  └─────────────┴──────────────────────────────────┘    │
│                                                          │
│  TAB 1: Train New Model                                 │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Model Name: [_______________________]           │  │
│  │  □ Use custom name                                │  │
│  │                                                    │  │
│  │  [Start Training] [View Progress]                 │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  TAB 2: Run Existing Model                              │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Select Model:                                     │  │
│  │  [Dropdown: List of saved models with metrics]    │  │
│  │                                                    │  │
│  │  Prediction Mode:                                  │  │
│  │  ○ Manual Text Input                               │  │
│  │  ○ CSV File Upload                                 │  │
│  │  ○ Dataset Row Numbers                             │  │
│  │                                                    │  │
│  │  [Mode-specific inputs]                            │  │
│  │                                                    │  │
│  │  [Run Prediction] [Generate Report]                │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  Results Section:                                        │
│  ┌──────────────────────────────────────────────────┐  │
│  │  Sample 1: [bug, blocker] (Confidence: 92%, 87%) │  │
│  │  Sample 2: [enhancement] (Confidence: 91%)         │  │
│  │  ...                                               │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
│  [Open Report] [Export Results]                          │
└─────────────────────────────────────────────────────────┘
```

### Key Java Components to Add

1. **Model Selector Dropdown**
   ```java
   JComboBox<String> modelSelector = new JComboBox<>();
   // Populate from multilable-prediction/models/ directory
   ```

2. **Prediction Mode Radio Buttons**
   ```java
   ButtonGroup modeGroup = new ButtonGroup();
   JRadioButton textMode = new JRadioButton("Manual Text");
   JRadioButton csvMode = new JRadioButton("CSV File");
   JRadioButton rowsMode = new JRadioButton("Dataset Rows");
   ```

3. **Python Script Execution**
   ```java
   ProcessBuilder pb = new ProcessBuilder(
       "python",
       "multilable-prediction/src/predict_with_model.py",
       runId,
       "--mode", mode,
       "--dataset", datasetPath,
       "--rows", rowNumbers,
       "--output", outputPath
   );
   ```

4. **Results Display**
   ```java
   // Parse JSON output and display in JTable or custom components
   JSONObject results = new JSONObject(outputJson);
   JSONArray predictions = results.getJSONArray("predictions");
   ```

## API Reference

### ModelPersistence Class

```python
persistence = ModelPersistence(base_dir="multilable-prediction/models")

# Save model
model_dir = persistence.save_best_model(
    model=trained_model,
    model_name='RandomForest',
    model_type='traditional_ml',
    metrics={'f1_score': 0.8476, ...},
    vectorizer=vectorizer,
    feature_selector=None,
    tokenizer=None,
    config=config_dict,
    custom_name='RF_Best'
)

# Load model
artifacts = persistence.load_model(run_id='RF_Best_20231205_143022')
model = artifacts['model']
vectorizer = artifacts['vectorizer']

# List models
models = persistence.list_models()

# Find best
best = persistence.find_best_model(metric='f1_score')
```

### ModelPredictor Class

```python
predictor = ModelPredictor(run_id='RF_Best_20231205_143022')

# Predict from text
results = predictor.predict(["Bug in login system", "Add dark mode"])

# Predict from CSV
results = predictor.predict_from_csv('new_data.csv')

# Predict specific rows
results = predictor.predict_from_dataset_rows(
    dataset_path='data/dataset.csv',
    row_numbers=[1, 5, 10]
)

# Get explanation
explanation = predictor.explain_prediction("Critical security issue")
```

### PredictionReportGenerator Class

```python
generator = PredictionReportGenerator(
    results=prediction_results,
    output_dir='output/predictions'
)

report_path = generator.generate_report()
# Opens in browser automatically
```

## Testing Checklist

- [ ] Train a model and verify it's saved in `models/` directory
- [ ] List saved models using `model_persistence.py`
- [ ] Load a saved model and verify all artifacts
- [ ] Predict single text input
- [ ] Predict from CSV file
- [ ] Predict specific dataset rows
- [ ] Generate HTML prediction report
- [ ] Verify report opens in browser
- [ ] Test with traditional ML model (RF, LR, MNB)
- [ ] Test with deep learning model (MLP, CNN)
- [ ] Verify confidence scores are reasonable
- [ ] Verify ground truth comparison (if available)
- [ ] Test Java UI integration (after implementation)

## Troubleshooting

### Issue: Model not found
**Solution**: Check run_id matches directory name in `multilable-prediction/models/`

### Issue: Prediction fails
**Solution**: Ensure dataset has 'report' column or specify `--text-column`

### Issue: Low confidence scores
**Solution**: Check if model was trained on similar data

### Issue: CNN tokenizer error
**Solution**: Verify tokenizer was saved during training (check metadata.json)

## Next Steps

1. Apply modifications to `main.py` using `MODIFICATION_GUIDE.py`
2. Test model saving during training
3. Test prediction scripts
4. Implement Java UI tabs in `PredictionPanel.java`
5. Add model management features (delete, rename)
6. Add model comparison features
7. Add batch prediction support
8. Add confidence threshold configuration

## Dependencies

All required dependencies are already in `requirements.txt`:
- scikit-learn
- tensorflow/keras
- pandas
- numpy
- joblib

No additional installations needed!

## Support

For issues or questions, refer to:
- `MODIFICATION_GUIDE.py` for integration steps
- Individual module docstrings for API details
- Example usage in `__main__` blocks of each module
