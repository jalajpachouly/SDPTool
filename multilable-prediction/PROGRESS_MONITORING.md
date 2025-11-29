# Real-Time Progress Monitoring System

## Overview

The system now provides real-time visibility into Python ML experiment execution with dual progress bars and structured logging. Users can see exactly what's happening during model training instead of just "Launching Python".

## Architecture

### Two-Level Progress System

1. **Overall Progress Bar** - Shows high-level workflow stages (0-100%)
   - Data loading
   - Feature engineering
   - Model training (overall)
   - Results visualization
   - Report generation

2. **Task Progress Bar** - Shows detailed progress for individual models (0-100%)
   - Per-model training progress
   - Word cloud generation progress
   - Specific model evaluation stages

### Communication Protocol

#### Structured Log Markers

Python backend emits specially formatted log markers that Java UI parses in real-time:

```
[PROGRESS:overall:percent:message]
[TASK:model_name:percent:message]
```

**Examples:**
```
[PROGRESS:overall:10:Loading Unbalanced data...]
[PROGRESS:overall:25:Preparing features (TF-IDF, Chi2 selection)...]
[PROGRESS:overall:50:Training and evaluating models...]
[TASK:RandomForest:0:Training on dataset...]
[TASK:RandomForest:100:Evaluation complete]
[TASK:MLP:10:Running cross-validation...]
[TASK:MLP:90:Evaluating on test set...]
```

## Implementation Details

### Python Backend (configurable_main.py)

#### Progress Logging Functions

```python
def log_progress(percent, message):
    """Log overall progress with structured marker for UI parsing"""
    print(f"[PROGRESS:overall:{percent}:{message}]")
    print(f"  → {message}")

def log_task(model_name, percent, message):
    """Log task-specific progress for individual models"""
    print(f"[TASK:{model_name}:{percent}:{message}]")
    print(f"    {model_name}: {message}")
```

#### Progress Milestones

| Progress % | Stage | Description |
|------------|-------|-------------|
| 0% | Initialization | Starting experiment |
| 10% | Data Loading | Loading balanced/unbalanced data |
| 15% | Visualizations | Generating data distribution charts |
| 20-25% | Word Clouds | Processing word clouds for each label |
| 25% | Feature Engineering | TF-IDF and Chi2 feature selection |
| 50-80% | Model Training | Training traditional ML models |
| 80% | MLP Training | Training deep learning MLP model |
| 85% | CNN Training | Training CNN model |
| 90% | Results | Saving results and generating charts |
| 95% | Report | Generating HTML report |
| 98% | Finalization | Writing metadata |
| 100% | Complete | Experiment finished |

#### Task Progress for Models

Each model reports its own progress:
- **0%**: Training initialization
- **10%**: Cross-validation started
- **50%**: Cross-validation complete
- **60%**: Training on full dataset
- **90%**: Evaluating on test set
- **100%**: Evaluation complete

### Java UI (AITechniquePanel.java)

#### UI Components

```java
private JProgressBar runProgressBar;        // Overall workflow progress (0-100%)
private JLabel runStatusLabel;               // Overall status message
private JProgressBar taskProgressBar;        // Task-specific progress (0-100%)
private JLabel taskStatusLabel;              // Task-specific message
```

#### Layout

```
┌─────────────────────────────────────────┐
│ Overall: ████████░░░░░░░░░░░░░░ 50%    │
│ Status: Training and evaluating models  │
│                                         │
│ Task: RandomForest ██████████ 100%     │
│ Status: Evaluation complete             │
└─────────────────────────────────────────┘
```

#### Log Parsing

```java
private void parseAndUpdateProgress(String line) {
    // Parse [PROGRESS:overall:50:Training models...]
    if (line.contains("[PROGRESS:overall:")) {
        // Extract percent and message
        // Update runProgressBar and runStatusLabel
        // Reset taskProgressBar to 0
    }
    
    // Parse [TASK:RandomForest:75:Training...]
    else if (line.contains("[TASK:")) {
        // Extract model name, percent, message
        // Update taskProgressBar and taskStatusLabel
    }
}
```

#### SwingWorker Integration

```java
new SwingWorker<Void, String>() {
    @Override
    protected Void doInBackground() {
        // Execute Python script
        while ((line = reader.readLine()) != null) {
            publish(line);  // Send to process() in real-time
        }
        return null;
    }
    
    @Override
    protected void process(List<String> chunks) {
        for (String line : chunks) {
            parseAndUpdateProgress(line);  // Update UI immediately
        }
    }
}
```

## User Experience

### Before (No Progress Visibility)
```
Launching Python...
[Waits 10 minutes with no feedback]
Run complete!
```

### After (Real-Time Progress)
```
Overall: ████░░░░░░░░░░░░░░░░░░░░ 10%
Status: Loading Unbalanced data...
  Loaded data: 1000 train, 250 test samples

Overall: ██████░░░░░░░░░░░░░░░░░░ 25%
Status: Preparing features (TF-IDF, Chi2 selection)...
  Feature engineering complete: 500 features selected

Overall: ████████████░░░░░░░░░░░░ 50%
Status: Training and evaluating models...

Task: RandomForest ████████████████████ 100%
Status: Evaluation complete
  RandomForest → Avg F1: 0.8542, Avg Recall: 0.8321

Task: SVM ███████████░░░░░░░░░░░ 55%
Status: Training on dataset...
```

## Detailed Logging

### Metrics Displayed in Real-Time

For each model, the system logs:
- **Sample counts**: Training and test set sizes
- **Feature counts**: Number of selected features
- **Model metrics**: Average F1 score, Recall per model
- **Training stages**: Cross-validation, full training, evaluation

### Example Output
```
[PROGRESS:overall:50:Training and evaluating models...]
[TASK:RandomForest:0:Training on dataset...]
[TASK:RandomForest:100:Evaluation complete]
  RandomForest → Avg F1: 0.8542, Avg Recall: 0.8321

[TASK:LogisticRegression:0:Training on dataset...]
[TASK:LogisticRegression:100:Evaluation complete]
  LogisticRegression → Avg F1: 0.8234, Avg Recall: 0.8156
```

## Benefits

1. **Transparency**: Users know exactly what's happening at all times
2. **Confidence**: Progress bars show the system is working, not frozen
3. **Debugging**: Structured logs help identify where issues occur
4. **Performance Insight**: See which stages take longest
5. **User Engagement**: Real-time feedback keeps users informed

## Future Enhancements

### Potential Improvements

1. **Time Estimates**: Show estimated time remaining based on progress
2. **Error Highlighting**: Show failed tasks in red with detailed error messages
3. **Parallel Tracking**: Show multiple models training in parallel
4. **Historical Data**: Display previous run times for comparison
5. **Detailed Metrics**: Show epoch-level progress for deep learning models
6. **Cancellation**: Allow users to cancel long-running operations

### Enhanced Logging

```python
def log_task_detailed(model_name, percent, message, metrics=None):
    """Extended logging with real-time metrics"""
    print(f"[TASK:{model_name}:{percent}:{message}]")
    if metrics:
        print(f"    Accuracy: {metrics.get('accuracy', 'N/A')}")
        print(f"    Loss: {metrics.get('loss', 'N/A')}")
        print(f"    Epoch: {metrics.get('epoch', 'N/A')}")
```

## Troubleshooting

### Progress Not Updating

**Issue**: UI shows "Launching Python" but no progress updates

**Solutions**:
1. Check Python script is printing to stdout (not stderr)
2. Verify log markers are formatted correctly: `[PROGRESS:overall:percent:message]`
3. Ensure SwingWorker is calling `publish(line)` for each output line
4. Check `parseAndUpdateProgress()` is being called in `process()` method

### Progress Stuck at One Value

**Issue**: Progress bar stops updating mid-execution

**Solutions**:
1. Check Python script isn't crashing silently
2. Verify all code paths emit progress markers
3. Look for exceptions in Java console
4. Check if BufferedReader is blocking

### Progress Goes Backward

**Issue**: Progress percentage decreases instead of increasing

**Solutions**:
1. Ensure progress percentages are monotonically increasing
2. Don't reuse percentages for different stages
3. Reset task progress when starting new model

## Testing

### Manual Test Procedure

1. Open AITechniquePanel
2. Configure experiment settings
3. Click "Run"
4. Observe:
   - Overall progress bar updates through stages
   - Task progress bar updates per model
   - Status labels show current operation
   - Console shows structured log markers

### Validation Checklist

- [ ] Overall progress reaches 100%
- [ ] Task progress resets for each model
- [ ] Status messages are descriptive
- [ ] Logs include model metrics
- [ ] No UI freezing during execution
- [ ] Report generated at end
- [ ] COMPLETE.flag file created

## Code References

### Key Files

- **Python Backend**: `multilable-prediction/src/configurable_main.py`
  - `log_progress()` function (lines ~400-405)
  - `log_task()` function (lines ~407-410)
  - Progress markers throughout workflow

- **Java UI**: `src/main/java/com/phd/ui/AITechniquePanel.java`
  - `createActionBar()` - Progress bar layout (lines ~350-450)
  - `handleRun()` - SwingWorker implementation (lines ~800-900)
  - `parseAndUpdateProgress()` - Log parsing (lines ~950-1000)
  - `executePythonScript()` - Process execution (lines ~1000-1100)

### Key Methods

| Method | Purpose | Location |
|--------|---------|----------|
| `log_progress(percent, msg)` | Emit overall progress | configurable_main.py |
| `log_task(model, percent, msg)` | Emit task progress | configurable_main.py |
| `parseAndUpdateProgress(line)` | Parse structured logs | AITechniquePanel.java |
| `SwingWorker.publish(line)` | Stream output to UI | AITechniquePanel.java |
| `SwingWorker.process(chunks)` | Update UI in EDT | AITechniquePanel.java |

## Summary

The real-time progress monitoring system transforms the user experience from a black box to a transparent, informative workflow. Users now see:
- What stage is executing
- Which model is training
- Real-time metrics
- Estimated completion

This system maintains the existing configuration architecture while adding critical visibility into long-running ML experiments.
