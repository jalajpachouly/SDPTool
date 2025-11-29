# Running configurable_main.py in Google Colab

## 📋 Overview
This guide provides step-by-step instructions to run the `configurable_main.py` script in Google Colab with custom JSON configuration files.

---

## 🚀 Step-by-Step Instructions

### **Step 1: Upload Your Project to Google Drive**

1. Compress your `multilable-prediction` folder into a ZIP file
2. Go to [Google Drive](https://drive.google.com)
3. Create a new folder (e.g., `SDPTool`)
4. Upload the ZIP file or directly upload the entire `multilable-prediction` folder
5. Make sure your folder structure looks like:
   ```
   SDPTool/
   └── multilable-prediction/
       ├── src/
       │   ├── configurable_main.py
       │   └── utils/
       ├── data/
       │   └── dataset.csv
       ├── configs/
       │   ├── quick_test.json
       │   └── traditional_ml_only.json
       ├── main_config.json
       └── requirements.txt
   ```

---

### **Step 2: Create a New Colab Notebook**

1. Go to [Google Colab](https://colab.research.google.com)
2. Click **File → New Notebook**
3. Rename it (e.g., "Multi-Label Classification Experiment")
4. Change runtime to **GPU** (optional but recommended):
   - Click **Runtime → Change runtime type**
   - Select **GPU** or **TPU**
   - Click **Save**

---

### **Step 3: Mount Google Drive**

Add this as your **first cell** and run it:

```python
from google.colab import drive
drive.mount('/content/drive')
```

**Expected output:**
```
Mounted at /content/drive
```

Click the link, authorize access, and paste the code.

---

### **Step 4: Navigate to Project Directory**

```python
import os

# Change this path to match YOUR Google Drive structure
PROJECT_PATH = '/content/drive/MyDrive/SDPTool/multilable-prediction'

# Navigate to project directory
os.chdir(PROJECT_PATH)

# Verify you're in the right place
!pwd
!ls -la
```

**Expected output:**
```
/content/drive/MyDrive/SDPTool/multilable-prediction
total ...
drwxr-xr-x  src
drwxr-xr-x  data
drwxr-xr-x  configs
-rw-r--r--  main_config.json
-rw-r--r--  requirements.txt
...
```

---

### **Step 5: Install Required Packages**

```python
# Install all dependencies
!pip install -q pandas==1.5.3 numpy==1.23.5 matplotlib==3.7.2 seaborn==0.12.2
!pip install -q nltk==3.8.1 imbalanced-learn==0.10.1 scipy==1.10.1
!pip install -q scikit-learn==1.2.2 iterative-stratification==0.1.9
!pip install -q tensorflow==2.12.0 wordcloud==1.9.4

# Download NLTK data
import nltk
nltk.download('stopwords', quiet=True)
nltk.download('punkt', quiet=True)

print("✅ All packages installed successfully!")
```

**Alternative:** If you have a `requirements.txt`:

```python
!pip install -q -r requirements.txt
import nltk
nltk.download('stopwords', quiet=True)
nltk.download('punkt', quiet=True)
```

---

### **Step 6: Verify Data Files**

```python
import os
import pandas as pd

# Check if dataset exists
dataset_path = 'data/dataset.csv'
if os.path.exists(dataset_path):
    df = pd.read_csv(dataset_path)
    print(f"✅ Dataset found: {len(df)} rows")
    print(f"Columns: {df.columns.tolist()}")
else:
    print("❌ Dataset not found! Check your path.")
```

---

### **Step 7: Create or Modify Configuration File**

You can use existing configs or create a new one:

#### **Option A: Use Existing Config**

```python
# List available configs
!ls configs/

# View a config file
!cat configs/quick_test.json
```

#### **Option B: Create Custom Config in Colab**

```python
import json

# Create custom configuration
custom_config = {
    "experiment_name": "Colab Experiment - Balanced CNN",
    "output_directory": "output",
    
    "data": {
        "dataset_path": "data/dataset.csv",
        "random_state": 42,
        "test_size": 0.2,
        "sample_size": None,
        "run_balanced": True,
        "run_unbalanced": False,  # Run only balanced to save time
        "balanced_target_count": 600
    },
    
    "feature_engineering": {
        "use_feature_selection": True,
        "top_k": 50,
        "tfidf": {
            "ngram_range": [1, 2],  # Use bigrams
            "min_df": 1
        }
    },
    
    "visualizations": {
        "enabled": True,
        "description_length": False,
        "class_distribution": False,
        "correlation_matrix": False,
        "label_frequency": False,
        "word_clouds": False,
        "top_features": False,
        "f1_scores": True,  # Only show final F1 scores
        "all_metrics_boxplot": False,
        "nb_metrics": False
    },
    
    "models": {
        "traditional_ml": {
            "enabled": False  # Skip traditional ML to focus on CNN
        },
        "deep_learning": {
            "enabled": True,
            "mlp": {
                "enabled": False  # Skip MLP
            },
            "cnn": {
                "enabled": True,
                "run_cross_validation": True,
                "cv_n_splits": 5,  # Reduced from 10 to save time
                "cv_epochs": 10,
                "epochs": 50,  # Increased from 20
                "batch_size": 32,
                "early_stopping_patience": 10,
                "max_words": 5000,
                "max_len": 100
            }
        }
    }
}

# Save to file
with open('configs/colab_cnn_test.json', 'w') as f:
    json.dump(custom_config, f, indent=2)

print("✅ Custom config created: configs/colab_cnn_test.json")
```

---

### **Step 8: Run the Experiment**

#### **Method A: Command Line Execution**

```python
# Run with default config
!python src/configurable_main.py

# OR run with custom config
!python src/configurable_main.py --config configs/colab_cnn_test.json

# OR run with inline config path
!python src/configurable_main.py --config main_config.json
```

#### **Method B: Import and Run Directly (Better for Debugging)**

```python
import sys
import os

# Add src to path
sys.path.insert(0, 'src')

# Import the main function
from configurable_main import main

# Run with config file path
config_path = 'configs/colab_cnn_test.json'
main(config_path)
```

#### **Method C: Run with Modified sys.argv (If configurable_main.py uses argparse)**

```python
import sys
sys.argv = ['configurable_main.py', '--config', 'configs/colab_cnn_test.json']

# Now run the script
!python src/configurable_main.py
```

---

### **Step 9: Monitor Progress**

The script will output progress updates:

```
====================================
Configuration Summary
====================================
Experiment Name: Colab Experiment - Balanced CNN
...

====================================
Starting Data Loading
====================================
Loading dataset from data/dataset.csv...
Dataset loaded: 5000 samples
...

====================================
CNN - Balanced Data
====================================
Running cross-validation (5 folds)...
Fold 1/5 - Mean F1: 0.8234
...
```

---

### **Step 10: View Results**

```python
import pandas as pd
import matplotlib.pyplot as plt

# Load results CSV
results_df = pd.read_csv('output/experiment_results.csv')
print(results_df)

# Plot F1 scores
plt.figure(figsize=(10, 6))
results_df.plot(x='Model', y='F1 Score', kind='bar')
plt.title('Model Performance Comparison')
plt.ylabel('F1 Score')
plt.xticks(rotation=45)
plt.tight_layout()
plt.show()
```

---

### **Step 11: Download Results**

```python
from google.colab import files

# Download results CSV
files.download('output/experiment_results.csv')

# Download CV results
files.download('output/cv_results.csv')

# Download all output files as ZIP
!zip -r output_results.zip output/
files.download('output_results.zip')
```

---

## 🔧 Troubleshooting

### **Issue 1: Module Not Found Error**

```python
# If you get "ModuleNotFoundError: No module named 'utils'"
import sys
sys.path.insert(0, '/content/drive/MyDrive/SDPTool/multilable-prediction/src')
```

### **Issue 2: NLTK Data Not Found**

```python
import nltk
nltk.download('all')  # Download all NLTK data (takes a few minutes)
```

### **Issue 3: Out of Memory**

```python
# Reduce dataset size in config
"data": {
    "sample_size": 2000,  # Use only 2000 samples
    ...
}

# OR reduce batch size
"cnn": {
    "batch_size": 16,  # Reduce from 32
    ...
}
```

### **Issue 4: Slow Execution**

```python
# Disable cross-validation
"cnn": {
    "run_cross_validation": False,
    ...
}

# Disable visualizations
"visualizations": {
    "enabled": False
}
```

---

## 📝 Quick Test Configuration

For a **fast test run** (< 5 minutes):

```json
{
  "experiment_name": "Quick Colab Test",
  "data": {
    "dataset_path": "data/dataset.csv",
    "sample_size": 1000,
    "run_balanced": true,
    "run_unbalanced": false,
    "balanced_target_count": 300
  },
  "visualizations": {
    "enabled": false
  },
  "models": {
    "traditional_ml": {
      "enabled": true,
      "run_cross_validation": false,
      "multinomial_nb": {"enabled": true}
    },
    "deep_learning": {
      "enabled": false
    }
  }
}
```

---

## 💡 Pro Tips

1. **Use GPU Runtime**: Deep learning models (MLP/CNN) run 10-50x faster with GPU
2. **Save Checkpoints**: Colab sessions timeout after 12 hours - save results frequently
3. **Test First**: Run with small `sample_size` (1000-2000) before full dataset
4. **Monitor RAM**: Check **Runtime → Manage Sessions** to see memory usage
5. **Enable TPU**: For very large datasets, TPU is faster than GPU

---

## 📊 Example Full Workflow Cell

Copy-paste this complete cell to run everything:

```python
# === CELL 1: Setup ===
from google.colab import drive
drive.mount('/content/drive')

import os
os.chdir('/content/drive/MyDrive/SDPTool/multilable-prediction')

!pip install -q -r requirements.txt
import nltk
nltk.download('stopwords', quiet=True)

# === CELL 2: Create Config ===
import json
config = {
    "experiment_name": "Colab CNN Test",
    "data": {
        "dataset_path": "data/dataset.csv",
        "run_balanced": True,
        "run_unbalanced": False,
        "balanced_target_count": 600
    },
    "visualizations": {"enabled": False},
    "models": {
        "traditional_ml": {"enabled": False},
        "deep_learning": {
            "enabled": True,
            "mlp": {"enabled": False},
            "cnn": {
                "enabled": True,
                "cv_n_splits": 5,
                "epochs": 50,
                "early_stopping_patience": 10
            }
        }
    }
}
with open('configs/colab_test.json', 'w') as f:
    json.dump(config, f, indent=2)

# === CELL 3: Run Experiment ===
!python src/configurable_main.py --config configs/colab_test.json

# === CELL 4: View Results ===
import pandas as pd
results = pd.read_csv('output/experiment_results.csv')
print(results)
```

---

## 🎯 Summary

1. ✅ Mount Google Drive
2. ✅ Navigate to project folder
3. ✅ Install dependencies
4. ✅ Create/modify JSON config
5. ✅ Run `python src/configurable_main.py --config <config.json>`
6. ✅ Download results

**Estimated Time:**
- Setup: 2-3 minutes
- Quick test: 5-10 minutes
- Full experiment: 30-60 minutes (depending on models enabled)

---

## 📞 Need Help?

Check the console output for detailed error messages. Most issues are:
- Wrong file paths
- Missing NLTK data
- Insufficient memory (reduce sample_size or batch_size)
