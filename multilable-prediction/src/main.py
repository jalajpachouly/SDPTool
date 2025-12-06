"""
Multi-Label Classification for Bug Reports

This refactored script uses modular utilities for better code organization.
Now supports feature flags via ui_config.json for model execution control.
"""

# Standard library imports
import json
from pathlib import Path
import warnings
warnings.filterwarnings('ignore')

# Third-party imports
import pandas as pd
import numpy as np
import nltk
import seaborn as sns
import matplotlib.pyplot as plt
from wordcloud import WordCloud
from iterstrat.ml_stratifiers import MultilabelStratifiedShuffleSplit, MultilabelStratifiedKFold
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.feature_selection import chi2
from sklearn.metrics import recall_score, f1_score, hamming_loss
from scipy.optimize import nnls
from tensorflow.keras.preprocessing.text import Tokenizer
from tensorflow.keras.preprocessing.sequence import pad_sequences

# Sklearn imports
from sklearn.multioutput import ClassifierChain
from sklearn.naive_bayes import MultinomialNB
from sklearn.linear_model import LogisticRegression
from sklearn.ensemble import RandomForestClassifier

# Keras imports
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Dropout, Embedding, Conv1D, GlobalMaxPooling1D
from tensorflow.keras.callbacks import EarlyStopping

# Local imports
from utils.config import LABELS, DATASET_PATH, TrainingConfig
from utils.data_loading import load_data, load_data_balanced
from utils.feature_engineering import prepare_data, prepare_data_for_deep_learning
from utils.models import build_mlp_model, build_cnn_model
from utils.evaluation import (
    cross_validation_score_multilabel,
    cross_validation_score_deep_learning,
    evaluate_classifier,
    evaluate_deep_learning_model
)
from utils.model_persistence import ModelPersistence
from utils.visualization import (
    visualize_word_cloud,
    visualize_description_length,
    visualize_class_distribution,
    visualize_correlation_matrix,
    visualize_f1_scores,
    plot_top_features
)

# Initialize NLTK resources
nltk.download('wordnet', quiet=True)
nltk.download('stopwords', quiet=True)


# =============================================================================
# DEFAULT CONFIGURATION CONSTANTS
# =============================================================================

# Feature Engineering Defaults
DEFAULT_TOP_K = 50
DEFAULT_TOP_K_PLOT = 20
DEFAULT_USE_WORDCLOUD_VOCABULARY = True

# Traditional ML Model Defaults
DEFAULT_MULTINOMIAL_NB_ENABLED = True
DEFAULT_LOGISTIC_REGRESSION_ENABLED = True
DEFAULT_LOGISTIC_REGRESSION_MAX_ITER = 10000
DEFAULT_RANDOM_FOREST_ENABLED = True
DEFAULT_RANDOM_FOREST_N_ESTIMATORS = 100
DEFAULT_RANDOM_FOREST_RANDOM_STATE = 42
DEFAULT_USE_CLASSIFIER_CHAIN = True

# MLP Deep Learning Defaults
DEFAULT_MLP_ENABLED = True
DEFAULT_MLP_CV_N_SPLITS = 5
DEFAULT_MLP_EPOCHS = 50
DEFAULT_MLP_BATCH_SIZE = 32
DEFAULT_MLP_VALIDATION_SPLIT = 0.2
DEFAULT_MLP_EARLY_STOPPING_PATIENCE = 5

# CNN Deep Learning Defaults
DEFAULT_CNN_ENABLED = True
DEFAULT_CNN_CV_N_SPLITS = 10
DEFAULT_CNN_CV_EPOCHS = 10
DEFAULT_CNN_CV_BATCH_SIZE = 32
DEFAULT_CNN_EPOCHS = 20
DEFAULT_CNN_BATCH_SIZE = 32
DEFAULT_CNN_MAX_WORDS = 5000
DEFAULT_CNN_MAX_LEN = 100
DEFAULT_CNN_EMBEDDING_DIM = 100
DEFAULT_CNN_EARLY_STOPPING_PATIENCE = 5

# Cross-Validation Defaults
DEFAULT_RUN_CROSS_VALIDATION = True

# Analysis Defaults
DEFAULT_ENABLE_ERROR_ANALYSIS = False
DEFAULT_ENABLE_STATISTICAL_SIGNIFICANCE = False

# Visualization Defaults
DEFAULT_VISUALIZATIONS_ENABLED = True
DEFAULT_WORDCLOUDS_ENABLED = True
DEFAULT_DESCRIPTION_LENGTH_ENABLED = True
DEFAULT_CLASS_DISTRIBUTION_ENABLED = True
DEFAULT_CORRELATION_MATRIX_ENABLED = True
DEFAULT_TOP_FEATURES_ENABLED = True
DEFAULT_F1_SCORES_ENABLED = True

# Data Processing Defaults
DEFAULT_RUN_BALANCED = True
DEFAULT_RUN_UNBALANCED = True


def load_ui_config(config_path='configs/ui_config.json'):
    """
    Load UI configuration from JSON file.
    If file doesn't exist or has errors, return None to use defaults.
    """
    config_file = Path(config_path)
    if not config_file.exists():
        print(f"[INFO] UI config file not found: {config_path}")
        print(f"[INFO] Using default configuration constants")
        return None
    
    try:
        with open(config_file, 'r', encoding='utf-8') as f:
            config = json.load(f)
        print(f"[OK] Loaded UI configuration from: {config_path}")
        return config
    except Exception as e:
        print(f"[WARNING] Error loading UI config: {e}")
        print(f"[INFO] Using default configuration constants")
        return None


def get_config_value(ui_config, path, default_value):
    """
    Safely extract nested configuration value from ui_config.
    If not found, use default and log it.
    
    Args:
        ui_config: Loaded JSON config dict (or None)
        path: Dot-separated path like 'models.traditional_ml.random_forest.enabled'
        default_value: Default to use if not found
    
    Returns:
        Configuration value or default
    """
    if ui_config is None:
        return default_value
    
    keys = path.split('.')
    value = ui_config
    
    try:
        for key in keys:
            value = value[key]
        return value
    except (KeyError, TypeError):
        print(f"[INFO] Config '{path}' not found, using default: {default_value}")
        return default_value


def analyze_misclassifications(y_test, y_pred, label_names, model_name):
    """
    Perform detailed analysis of misclassified samples.
    
    Parameters:
    - y_test (ndarray): True labels (n_samples x n_labels)
    - y_pred (ndarray): Predicted labels (n_samples x n_labels)
    - label_names (list): List of label names
    - model_name (str): Name of the model
    """
    n_samples, n_labels = y_test.shape
    
    print(f"\nDetailed Misclassification Analysis for {model_name}:")
    print("=" * 80)
    
    # Overall statistics
    total_labels = n_samples * n_labels
    correctly_classified = np.sum(y_test == y_pred)
    misclassified = total_labels - correctly_classified
    accuracy = correctly_classified / total_labels
    
    print(f"\nOverall Label-wise Accuracy: {accuracy:.4f}")
    print(f"Total label predictions: {total_labels}")
    print(f"Correctly classified: {correctly_classified}")
    print(f"Misclassified: {misclassified}")
    
    # Per-label confusion analysis
    print(f"\nPer-Label Confusion Matrix Analysis:")
    print("-" * 80)
    
    for label_idx, label_name in enumerate(label_names):
        y_true_label = y_test[:, label_idx]
        y_pred_label = y_pred[:, label_idx]
        
        # Calculate confusion matrix components
        tp = np.sum((y_true_label == 1) & (y_pred_label == 1))
        tn = np.sum((y_true_label == 0) & (y_pred_label == 0))
        fp = np.sum((y_true_label == 0) & (y_pred_label == 1))
        fn = np.sum((y_true_label == 1) & (y_pred_label == 0))
        
        precision = tp / (tp + fp) if (tp + fp) > 0 else 0.0
        recall = tp / (tp + fn) if (tp + fn) > 0 else 0.0
        specificity = tn / (tn + fp) if (tn + fp) > 0 else 0.0
        
        print(f"\nLabel: {label_name}")
        print(f"  True Positives (TP): {tp}")
        print(f"  True Negatives (TN): {tn}")
        print(f"  False Positives (FP): {fp} - Model incorrectly predicted this label")
        print(f"  False Negatives (FN): {fn} - Model missed this label")
        print(f"  Precision: {precision:.4f}")
        print(f"  Recall: {recall:.4f}")
        print(f"  Specificity: {specificity:.4f}")
    
    # Sample-level error patterns
    print(f"\nSample-level Error Patterns:")
    print("-" * 80)
    
    # Count errors per sample
    errors_per_sample = np.sum(y_test != y_pred, axis=1)
    samples_with_errors = np.sum(errors_per_sample > 0)
    perfect_predictions = n_samples - samples_with_errors
    
    print(f"Samples with perfect predictions: {perfect_predictions} ({perfect_predictions/n_samples*100:.2f}%)")
    print(f"Samples with at least one error: {samples_with_errors} ({samples_with_errors/n_samples*100:.2f}%)")
    
    # Distribution of errors
    print(f"\nError Distribution:")
    for num_errors in range(1, n_labels + 1):
        count = np.sum(errors_per_sample == num_errors)
        if count > 0:
            print(f"  {num_errors} label error(s): {count} samples ({count/n_samples*100:.2f}%)")
    
    # Multi-label specific patterns
    print(f"\nMulti-label Prediction Patterns:")
    print("-" * 80)
    
    # Average number of labels per sample
    true_labels_per_sample = np.sum(y_test, axis=1)
    pred_labels_per_sample = np.sum(y_pred, axis=1)
    
    print(f"Average true labels per sample: {np.mean(true_labels_per_sample):.2f}")
    print(f"Average predicted labels per sample: {np.mean(pred_labels_per_sample):.2f}")
    
    # Over-prediction and under-prediction
    over_predicted = np.sum(pred_labels_per_sample > true_labels_per_sample)
    under_predicted = np.sum(pred_labels_per_sample < true_labels_per_sample)
    exact_match = np.sum(pred_labels_per_sample == true_labels_per_sample)
    
    print(f"\nLabel Count Prediction:")
    print(f"  Over-predicted (too many labels): {over_predicted} samples ({over_predicted/n_samples*100:.2f}%)")
    print(f"  Under-predicted (too few labels): {under_predicted} samples ({under_predicted/n_samples*100:.2f}%)")
    print(f"  Exact label count match: {exact_match} samples ({exact_match/n_samples*100:.2f}%)")
    
    # Label co-occurrence errors
    print(f"\nMost Common Misclassification Patterns:")
    print("-" * 80)
    
    # Find samples with misclassifications
    misclassified_mask = errors_per_sample > 0
    if np.sum(misclassified_mask) > 0:
        misclass_y_test = y_test[misclassified_mask]
        misclass_y_pred = y_pred[misclassified_mask]
        
        # Count specific error patterns (first 5 most common)
        error_patterns = {}
        for i in range(len(misclass_y_test)):
            true_labels = tuple(np.where(misclass_y_test[i] == 1)[0])
            pred_labels = tuple(np.where(misclass_y_pred[i] == 1)[0])
            pattern_key = (true_labels, pred_labels)
            error_patterns[pattern_key] = error_patterns.get(pattern_key, 0) + 1
        
        # Sort by frequency
        sorted_patterns = sorted(error_patterns.items(), key=lambda x: x[1], reverse=True)[:5]
        
        print("Top 5 misclassification patterns:")
        for idx, ((true_labels, pred_labels), count) in enumerate(sorted_patterns, 1):
            true_label_names = [label_names[i] for i in true_labels] if true_labels else ['None']
            pred_label_names = [label_names[i] for i in pred_labels] if pred_labels else ['None']
            print(f"  Pattern {idx} (occurred {count} times):")
            print(f"    True labels: {', '.join(true_label_names)}")
            print(f"    Predicted labels: {', '.join(pred_label_names)}")
    
    print("=" * 80)


def save_best_model_from_results(
    combined_results,
    trained_models,
    vectorizer,
    feature_selector,
    tokenizer_dict,
    ui_config,
    cv_results=None
):
    """
    Identify and save the best performing model from training results.
    Uses CV Mean F1 as selection criterion (same as HTML report).
    
    Args:
        combined_results: List of result dictionaries from all models (test results)
        trained_models: Dictionary mapping model names to trained model objects
        vectorizer: TfidfVectorizer used
        feature_selector: Chi-square selector (if used)
        tokenizer_dict: Dictionary mapping model names to tokenizers (for DL models)
        ui_config: UI configuration dictionary
        cv_results: DataFrame with CV results (Model, Recall, F1 columns)
    """
    if not combined_results:
        print("[INFO] No models to save (no results available)")
        return
    
    # Get model persistence configuration
    model_persistence_config = ui_config.get('model_persistence', {})
    persistence_enabled = model_persistence_config.get('enabled', True)
    save_best = model_persistence_config.get('save_best_model', True)
    
    if not persistence_enabled or not save_best:
        print("[INFO] Model persistence is disabled in configuration")
        return
    
    custom_name = model_persistence_config.get('custom_model_name', None)
    
    print(f"\n{'='*80}")
    print(f"IDENTIFYING BEST MODEL FOR PERSISTENCE")
    print(f"{'='*80}")
    print(f"Selection criterion: CV Mean F1 (Fixed) - Same as HTML Report")
    
    # Use CV results if available (same as HTML report logic)
    if cv_results is not None and not cv_results.empty:
        print("\nUsing Cross-Validation F1 scores (same as HTML report):")
        print(cv_results.to_string(index=False))
        
        # Find model with highest CV Mean F1
        best_idx = cv_results['F1'].idxmax()
        best_result = cv_results.loc[best_idx]
        model_name = best_result['Model']
        best_f1 = best_result['F1']
        
        print(f"\nBest model: {model_name}")
        print(f"Best CV Mean F1: {best_f1:.4f}")
    else:
        print("\n[WARNING] CV results not available, falling back to test set Macro F1")
        # Fallback: aggregate test results
        df_results = pd.DataFrame(combined_results)
        
        # Group by Model and calculate mean metrics
        model_metrics = df_results.groupby('Model').agg({
            'Recall': 'mean',
            'F1': 'mean',
            'Hamming Loss': 'first'
        }).reset_index()
        
        model_metrics.columns = ['Model', 'Macro Recall', 'Macro F1', 'Hamming Loss']
        
        print("\nModel Performance Summary (Test Set):")
        print(model_metrics.to_string(index=False))
        
        # Use Macro F1 from test set
        best_idx = model_metrics['Macro F1'].idxmax()
        best_result = model_metrics.loc[best_idx]
        model_name = best_result['Model']
        best_f1 = best_result['Macro F1']
        
        print(f"\nBest model: {model_name}")
        print(f"Best Test Macro F1: {best_f1:.4f}")
    
    # Get the trained model object
    if model_name not in trained_models:
        print(f"[WARNING] Model '{model_name}' not found in trained models dictionary")
        print(f"Available models: {list(trained_models.keys())}")
        return
    
    model = trained_models[model_name]
    
    # Determine model type
    model_type = 'deep_learning' if model_name in ['MLP', 'CNN'] else 'traditional_ml'
    
    # Get tokenizer if deep learning model
    tokenizer = tokenizer_dict.get(model_name, None)
    
    # Prepare metrics dictionary (column names differ between CV and test results)
    if cv_results is not None and not cv_results.empty:
        # CV results have columns: Model, Recall, F1
        metrics = {
            'cv_mean_f1': float(best_f1),  # Used for selection
            'macro_f1': float(best_f1),
            'macro_recall': float(best_result['Recall']),
            'hamming_loss': 0.0  # Not available in CV results
        }
    else:
        # Test results have columns: Model, Macro Recall, Macro F1, Hamming Loss
        metrics = {
            'cv_mean_f1': float(best_f1),  # Used for selection
            'macro_f1': float(best_result['Macro F1']),
            'macro_recall': float(best_result['Macro Recall']),
            'hamming_loss': float(best_result['Hamming Loss'])
        }
    
    # Save the model
    persistence = ModelPersistence()
    
    try:
        model_dir = persistence.save_best_model(
            model=model,
            model_name=model_name,
            model_type=model_type,
            metrics=metrics,
            vectorizer=vectorizer,
            feature_selector=feature_selector,
            tokenizer=tokenizer,
            config=ui_config,
            custom_name=custom_name
        )
        
        print(f"[SUCCESS] Best model saved successfully to: {model_dir}")
        
    except Exception as e:
        print(f"[ERROR] Failed to save model: {e}")
        import traceback
        traceback.print_exc()


def main(data_type='Unbalanced'):
    """
    Main function to execute data processing, model training, evaluation, and visualization.
    Now supports feature flags from ui_config.json.

    Parameters:
    - data_type (str): Type of data to process ('Unbalanced' or 'Balanced').
    """
    # Load UI configuration
    ui_config = load_ui_config()
    
    # Extract configuration values with defaults
    print("\n" + "="*80)
    print(f"CONFIGURATION FOR {data_type.upper()} DATA")
    print("="*80)
    
    # Feature Engineering Config
    top_k = get_config_value(ui_config, 'feature_engineering.top_k', DEFAULT_TOP_K)
    top_k_plot = get_config_value(ui_config, 'feature_engineering.top_k_plot', DEFAULT_TOP_K_PLOT)
    use_wordcloud_vocab = get_config_value(ui_config, 'feature_engineering.use_wordcloud_vocabulary', DEFAULT_USE_WORDCLOUD_VOCABULARY)
    
    # Traditional ML Config
    enable_multinomial_nb = get_config_value(ui_config, 'models.traditional_ml.multinomial_nb.enabled', DEFAULT_MULTINOMIAL_NB_ENABLED)
    multinomial_nb_use_chain = get_config_value(ui_config, 'models.traditional_ml.multinomial_nb.use_classifier_chain', DEFAULT_USE_CLASSIFIER_CHAIN)
    
    enable_logistic_regression = get_config_value(ui_config, 'models.traditional_ml.logistic_regression.enabled', DEFAULT_LOGISTIC_REGRESSION_ENABLED)
    logistic_max_iter = get_config_value(ui_config, 'models.traditional_ml.logistic_regression.max_iter', DEFAULT_LOGISTIC_REGRESSION_MAX_ITER)
    logistic_use_chain = get_config_value(ui_config, 'models.traditional_ml.logistic_regression.use_classifier_chain', DEFAULT_USE_CLASSIFIER_CHAIN)
    
    enable_random_forest = get_config_value(ui_config, 'models.traditional_ml.random_forest.enabled', DEFAULT_RANDOM_FOREST_ENABLED)
    rf_n_estimators = get_config_value(ui_config, 'models.traditional_ml.random_forest.n_estimators', DEFAULT_RANDOM_FOREST_N_ESTIMATORS)
    rf_random_state = get_config_value(ui_config, 'models.traditional_ml.random_forest.random_state', DEFAULT_RANDOM_FOREST_RANDOM_STATE)
    rf_use_chain = get_config_value(ui_config, 'models.traditional_ml.random_forest.use_classifier_chain', DEFAULT_USE_CLASSIFIER_CHAIN)
    
    # MLP Config
    enable_mlp = get_config_value(ui_config, 'models.deep_learning.mlp.enabled', DEFAULT_MLP_ENABLED)
    mlp_cv_n_splits = get_config_value(ui_config, 'models.deep_learning.mlp.cv_n_splits', DEFAULT_MLP_CV_N_SPLITS)
    mlp_epochs = get_config_value(ui_config, 'models.deep_learning.mlp.epochs', DEFAULT_MLP_EPOCHS)
    mlp_batch_size = get_config_value(ui_config, 'models.deep_learning.mlp.batch_size', DEFAULT_MLP_BATCH_SIZE)
    mlp_validation_split = get_config_value(ui_config, 'models.deep_learning.mlp.validation_split', DEFAULT_MLP_VALIDATION_SPLIT)
    mlp_early_stopping_patience = get_config_value(ui_config, 'models.deep_learning.mlp.early_stopping_patience', DEFAULT_MLP_EARLY_STOPPING_PATIENCE)
    
    # CNN Config
    enable_cnn = get_config_value(ui_config, 'models.deep_learning.cnn.enabled', DEFAULT_CNN_ENABLED)
    cnn_cv_n_splits = get_config_value(ui_config, 'models.deep_learning.cnn.cv_n_splits', DEFAULT_CNN_CV_N_SPLITS)
    cnn_cv_epochs = get_config_value(ui_config, 'models.deep_learning.cnn.cv_epochs', DEFAULT_CNN_CV_EPOCHS)
    cnn_cv_batch_size = get_config_value(ui_config, 'models.deep_learning.cnn.cv_batch_size', DEFAULT_CNN_CV_BATCH_SIZE)
    cnn_epochs = get_config_value(ui_config, 'models.deep_learning.cnn.epochs', DEFAULT_CNN_EPOCHS)
    cnn_batch_size = get_config_value(ui_config, 'models.deep_learning.cnn.batch_size', DEFAULT_CNN_BATCH_SIZE)
    cnn_max_words = get_config_value(ui_config, 'models.deep_learning.cnn.max_words', DEFAULT_CNN_MAX_WORDS)
    cnn_max_len = get_config_value(ui_config, 'models.deep_learning.cnn.max_len', DEFAULT_CNN_MAX_LEN)
    cnn_embedding_dim = get_config_value(ui_config, 'models.deep_learning.cnn.embedding_dim', DEFAULT_CNN_EMBEDDING_DIM)
    cnn_early_stopping_patience = get_config_value(ui_config, 'models.deep_learning.cnn.early_stopping_patience', DEFAULT_CNN_EARLY_STOPPING_PATIENCE)
    
    # Cross-Validation Config (global flag)
    run_cross_validation = get_config_value(ui_config, 'models.traditional_ml.run_cross_validation', DEFAULT_RUN_CROSS_VALIDATION)
    
    # Analysis Config (global flags)
    enable_error_analysis = get_config_value(ui_config, 'analysis.enable_error_analysis', DEFAULT_ENABLE_ERROR_ANALYSIS)
    enable_statistical_significance = get_config_value(ui_config, 'analysis.enable_statistical_significance', DEFAULT_ENABLE_STATISTICAL_SIGNIFICANCE)
    
    # Visualization Config
    viz_enabled = get_config_value(ui_config, 'visualizations.enabled', DEFAULT_VISUALIZATIONS_ENABLED)
    viz_wordclouds = get_config_value(ui_config, 'visualizations.word_clouds', DEFAULT_WORDCLOUDS_ENABLED)
    viz_description_length = get_config_value(ui_config, 'visualizations.description_length', DEFAULT_DESCRIPTION_LENGTH_ENABLED)
    viz_class_distribution = get_config_value(ui_config, 'visualizations.class_distribution', DEFAULT_CLASS_DISTRIBUTION_ENABLED)
    viz_correlation_matrix = get_config_value(ui_config, 'visualizations.correlation_matrix', DEFAULT_CORRELATION_MATRIX_ENABLED)
    viz_top_features = get_config_value(ui_config, 'visualizations.top_features', DEFAULT_TOP_FEATURES_ENABLED)
    viz_f1_scores = get_config_value(ui_config, 'visualizations.f1_scores', DEFAULT_F1_SCORES_ENABLED)
    
    print("\n[MODELS ENABLED]")
    print(f"  MultinomialNB: {enable_multinomial_nb}")
    print(f"  LogisticRegression: {enable_logistic_regression}")
    print(f"  RandomForest: {enable_random_forest}")
    print(f"  MLP: {enable_mlp}")
    print(f"  CNN: {enable_cnn}")
    print(f"  Cross-Validation: {run_cross_validation}")
    print(f"  Error Analysis: {enable_error_analysis}")
    print(f"  Statistical Significance: {enable_statistical_significance}")
    print(f"  Visualizations: {viz_enabled}")
    print("="*80 + "\n")
    
    config = TrainingConfig()
    
    # Load Data
    csv_path = str(DATASET_PATH)
    try:
        if data_type == 'Balanced':
            X_train_df, X_test_df, y_train_df, y_test_df = load_data_balanced(csv_path, LABELS)
        else:
            X_train_df, X_test_df, y_train_df, y_test_df = load_data(csv_path, LABELS)
    except Exception as e:
        print(f"Error loading data: {e}")
        return

    # Visualizations (feature-flagged)
    if viz_enabled:
        if viz_description_length:
            visualize_description_length(X_train_df, data_type)
        if viz_class_distribution:
            visualize_class_distribution(y_train_df, y_test_df, data_type)
        if viz_correlation_matrix:
            visualize_correlation_matrix(y_train_df, data_type)

    # Word Clouds and Vocabulary Collection (feature-flagged)
    wordcloud_vocab = []
    if viz_enabled and viz_wordclouds and use_wordcloud_vocab:
        vocab_set = set()
        for label in LABELS:
            top_words = visualize_word_cloud(X_train_df, y_train_df, label)
            vocab_set.update(top_words)
        wordcloud_vocab = list(vocab_set)
        print(f"\nTotal unique words collected from word clouds: {len(wordcloud_vocab)}")
    elif use_wordcloud_vocab:
        print("[INFO] Wordcloud vocabulary requested but wordclouds disabled, using None")
        wordcloud_vocab = None
    else:
        wordcloud_vocab = None

    # Prepare Data with Vocabulary
    try:
        X_train_tfidf, X_test_tfidf, selected_features, chi2_scores_max, vector, selected_indices = prepare_data(
            X_train_df, X_test_df, y_train_df, top_k=top_k, vocabulary=wordcloud_vocab
        )
    except Exception as e:
        print(f"Error during data preparation: {e}")
        return

    if X_train_tfidf.shape[1] == 0:
        raise ValueError("No features were selected.")

    # Convert Labels to NumPy Arrays
    y_train_np = y_train_df.to_numpy()
    y_test_np = y_test_df.to_numpy()
    label_names = y_test_df.columns.tolist()

    # Plot Top Features (feature-flagged)
    if viz_enabled and viz_top_features:
        plot_top_features(selected_features, chi2_scores_max, data_type, top_k_plot=top_k_plot)

    # =============================================================================
    # TRADITIONAL ML MODELS (Feature-Flagged)
    # =============================================================================
    
    traditional_ml_results = []
    trained_models = {}  # Track all trained models for persistence
    tokenizer_dict = {}  # Track tokenizers for deep learning models
    classifiers_to_run = []
    
    # Define Classifiers based on feature flags
    if enable_multinomial_nb:
        if multinomial_nb_use_chain:
            clf_nb = ClassifierChain(MultinomialNB())
        else:
            clf_nb = MultinomialNB()
        classifiers_to_run.append((clf_nb, 'MultinomialNB'))
        print("[INFO] MultinomialNB enabled")
    
    if enable_logistic_regression:
        if logistic_use_chain:
            clf_lr = ClassifierChain(LogisticRegression(max_iter=logistic_max_iter))
        else:
            clf_lr = LogisticRegression(max_iter=logistic_max_iter)
        classifiers_to_run.append((clf_lr, 'LogisticRegression'))
        print(f"[INFO] LogisticRegression enabled (max_iter={logistic_max_iter})")
    
    if enable_random_forest:
        if rf_use_chain:
            clf_rf = ClassifierChain(RandomForestClassifier(n_estimators=rf_n_estimators, random_state=rf_random_state))
        else:
            clf_rf = RandomForestClassifier(n_estimators=rf_n_estimators, random_state=rf_random_state)
        classifiers_to_run.append((clf_rf, 'RandomForest'))
        print(f"[INFO] RandomForest enabled (n_estimators={rf_n_estimators})")

    # Cross-Validation for Traditional Models (feature-flagged)
    cv_results_df = None  # Store CV results for model selection
    if run_cross_validation and classifiers_to_run:
        print("\n" + "="*80)
        print("CROSS-VALIDATION FOR TRADITIONAL ML MODELS")
        print("="*80)
        meth_cv = []
        for clf, model_name in classifiers_to_run:
            print(f"\n===== Cross-Validating {model_name} =====")
            cv_scores = cross_validation_score_multilabel(clf, X_train_tfidf, y_train_np)
            meth_cv.append({'Model': model_name, 'Recall': cv_scores['Recall'], 'F1': cv_scores['F1']})
        cv_results_df = pd.DataFrame(meth_cv)
        print("\nCross-validation results:")
        print(cv_results_df[['Model', 'Recall', 'F1']])

    # Evaluate Classifiers on Test Set
    if classifiers_to_run:
        print("\n" + "="*80)
        print("TEST SET EVALUATION FOR TRADITIONAL ML MODELS")
        print("="*80)
        for clf, model_name in classifiers_to_run:
            print(f"\n===== Evaluating {model_name} on Test Set =====")
            results = evaluate_classifier(clf, model_name, X_train_tfidf, y_train_np, X_test_tfidf, y_test_np, label_names, 
                                        enable_error_analysis=enable_error_analysis, 
                                        analyze_misclassifications_func=analyze_misclassifications)
            traditional_ml_results.extend(results)
            # Store trained model for persistence
            trained_models[model_name] = clf

    # =============================================================================
    # MLP DEEP LEARNING MODEL (Feature-Flagged)
    # =============================================================================
    
    mlp_results = []
    if enable_mlp:
        print("\n" + "="*80)
        print("MLP DEEP LEARNING MODEL")
        print("="*80)
        
        # Cross-Validation
        if run_cross_validation:
            print("\n===== Training and Evaluating MLP Model via Cross-Validation =====")
            deep_learning_cv_scores = cross_validation_score_deep_learning(
                lambda: build_mlp_model(X_train_tfidf.shape[1], y_train_np.shape[1]),
                X_train_tfidf.toarray(), y_train_np, 
                n_splits=mlp_cv_n_splits, 
                epochs=mlp_epochs, 
                batch_size=mlp_batch_size
            )
            print(f"\nMLP Cross-validation results:")
            print(f"Recall: {deep_learning_cv_scores['Recall']:.4f}")
            print(f"F1-score: {deep_learning_cv_scores['F1']:.4f}")

        # Train MLP Model on Entire Training Set
        print("\n===== Training MLP Model on Entire Training Set =====")
        deep_learning_model = build_mlp_model(input_dim=X_train_tfidf.shape[1], output_dim=y_train_np.shape[1])
        early_stop = EarlyStopping(monitor='val_loss', patience=mlp_early_stopping_patience, restore_best_weights=True)

        deep_learning_model.fit(
            X_train_tfidf.toarray(),
            y_train_np,
            epochs=mlp_epochs,
            batch_size=mlp_batch_size,
            validation_split=mlp_validation_split,
            callbacks=[early_stop],
            verbose=0
        )

        # Evaluate MLP Model on Test Set
        mlp_results = evaluate_deep_learning_model(deep_learning_model, X_test_tfidf, y_test_np, 'MLP', label_names, 
                                                   enable_error_analysis=enable_error_analysis,
                                                   analyze_misclassifications_func=analyze_misclassifications)
        # Store trained model for persistence
        trained_models['MLP'] = deep_learning_model
        tokenizer_dict['MLP'] = None  # MLP doesn't use tokenizer
    else:
        print("\n[INFO] MLP model disabled by feature flag")

    # =============================================================================
    # CNN DEEP LEARNING MODEL (Feature-Flagged)
    # =============================================================================
    
    cnn_results = []
    if enable_cnn:
        print("\n" + "="*80)
        print("CNN DEEP LEARNING MODEL")
        print("="*80)
        
        # Prepare Data for CNN
        X_train_dl, X_test_dl, tokenizer = prepare_data_for_deep_learning(
            X_train_df['report'], X_test_df['report'], max_words=cnn_max_words, max_len=cnn_max_len
        )

        # Parameters for CNN
        vocab_size = min(len(tokenizer.word_index) + 1, cnn_max_words)
        embedding_dim = cnn_embedding_dim
        max_len = X_train_dl.shape[1]
        output_dim = y_train_np.shape[1]

        # Cross-Validation for CNN Model
        if run_cross_validation:
            print("\n===== Training and Evaluating CNN Model via Cross-Validation =====")
            cnn_cv_scores = cross_validation_score_deep_learning(
                lambda: build_cnn_model(vocab_size, embedding_dim, max_len, output_dim),
                X_train_dl, y_train_np, n_splits=cnn_cv_n_splits, epochs=cnn_cv_epochs, batch_size=cnn_cv_batch_size
            )
            print(f"\nCNN Cross-validation results:")
            print(f"Recall: {cnn_cv_scores['Recall']:.4f}")
            print(f"F1-score: {cnn_cv_scores['F1']:.4f}")

        # Train CNN Model on Entire Training Set
        print("\n===== Training CNN Model on Entire Training Set =====")
        cnn_model = build_cnn_model(vocab_size, embedding_dim, max_len, output_dim)
        early_stop = EarlyStopping(monitor='val_loss', patience=cnn_early_stopping_patience, restore_best_weights=True)

        cnn_model.fit(
            X_train_dl, y_train_np,
            epochs=cnn_epochs, batch_size=cnn_batch_size,
            validation_split=0.2,
            callbacks=[early_stop],
            verbose=1
        )

        # Evaluate CNN Model on Test Set
        cnn_results = evaluate_deep_learning_model(cnn_model, X_test_dl, y_test_np, 'CNN', label_names, 
                                                   enable_error_analysis=enable_error_analysis,
                                                   analyze_misclassifications_func=analyze_misclassifications)
        # Store trained model and tokenizer for persistence
        trained_models['CNN'] = cnn_model
        tokenizer_dict['CNN'] = tokenizer
    else:
        print("\n[INFO] CNN model disabled by feature flag")

    # =============================================================================
    # COMBINE RESULTS AND VISUALIZATION
    # =============================================================================
    
    # Combine Results from all enabled models
    combined_results = traditional_ml_results + mlp_results + cnn_results
    
    if not combined_results:
        print("\n[WARNING] No models were enabled. No results to display.")
        return
    
    df_results = pd.DataFrame(combined_results)
    df_results['Hamming Loss'] = pd.to_numeric(df_results['Hamming Loss'], errors='coerce')

    # Visualization of Results (feature-flagged)
    if viz_enabled and viz_f1_scores:
        sns.set(style="whitegrid")
        visualize_f1_scores(df_results, data_type)

    # =============================================================================
    # MODEL PERSISTENCE (Feature-Flagged)
    # =============================================================================
    
    # Save best model if persistence is enabled
    save_best_model_from_results(
        combined_results=combined_results,
        cv_results=cv_results_df,  # Pass CV results for model selection
        trained_models=trained_models,
        vectorizer=vector,  # Fixed: use 'vector' from prepare_data()
        feature_selector=selected_indices,  # Pass selected feature indices for prediction
        tokenizer_dict=tokenizer_dict,
        ui_config=ui_config
    )

    print("\nAll processes completed successfully.")


if __name__ == "__main__":
    import sys
    from pathlib import Path
    from datetime import datetime
    
    # Load UI configuration first to get experiment_name
    ui_config = load_ui_config()
    experiment_name = ui_config.get('experiment_name', 'default_run')
    
    # Check if prediction mode is enabled
    prediction_config = ui_config.get('prediction', {})
    prediction_enabled = prediction_config.get('enabled', False)
    
    if prediction_enabled:
        # Run prediction mode using existing saved model
        print("\n" + "="*80)
        print("PREDICTION MODE")
        print("="*80)
        
        run_id = prediction_config.get('run_id')
        mode = prediction_config.get('mode', 'interactive')
        input_file = prediction_config.get('input_file')
        row_numbers = prediction_config.get('row_numbers')
        
        if not run_id:
            print("[ERROR] Prediction mode enabled but 'run_id' not specified in config")
            print("Please set 'prediction.run_id' in your config file")
            sys.exit(1)
        
        print(f"Loading model: {run_id}")
        print(f"Prediction mode: {mode}")
        
        # Import predictor
        from predict_with_model import ModelPredictor
        
        try:
            predictor = ModelPredictor(run_id)
            
            if mode == 'interactive':
                predictor.run_interactive_mode()
            elif mode == 'csv' and input_file:
                predictor.predict_from_csv(input_file)
            elif mode == 'rows' and row_numbers:
                predictor.predict_from_rows(row_numbers)
            else:
                print(f"[ERROR] Invalid prediction mode or missing parameters")
                print(f"Mode: {mode}, Input file: {input_file}, Rows: {row_numbers}")
                sys.exit(1)
                
        except Exception as e:
            print(f"[ERROR] Prediction failed: {e}")
            import traceback
            traceback.print_exc()
            sys.exit(1)
        
        sys.exit(0)  # Exit after prediction
    
    # Training mode (existing code)
    # Setup output directory with new structure: output/reports/<Run_ID>
    base_output_dir = Path(__file__).parent.parent / 'output' / 'reports' / experiment_name
    base_output_dir.mkdir(parents=True, exist_ok=True)
    log_path = base_output_dir / 'log.txt'
    
    print(f"[INFO] Run ID: {experiment_name}")
    print(f"[INFO] Output directory: {base_output_dir.absolute()}")
    
    # Redirect stdout to both console and log file
    class TeeOutput:
        def __init__(self, *files):
            self.files = files
        def write(self, text):
            for f in self.files:
                f.write(text)
                f.flush()
        def flush(self):
            for f in self.files:
                f.flush()
    
    log_file = open(log_path, 'w', encoding='utf-8')
    original_stdout = sys.stdout
    sys.stdout = TeeOutput(original_stdout, log_file)
    
    try:
        # Check which data types to run
        
        run_unbalanced = get_config_value(ui_config, 'data.run_unbalanced', DEFAULT_RUN_UNBALANCED)
        run_balanced = get_config_value(ui_config, 'data.run_balanced', DEFAULT_RUN_BALANCED)
        
        training_start_time = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
        
        print("\n" + "="*80)
        print("STARTING MULTI-LABEL CLASSIFICATION PIPELINE")
        print("="*80)
        print(f"Training Started: {training_start_time}")
        print(f"Run Unbalanced Data: {run_unbalanced}")
        print(f"Run Balanced Data: {run_balanced}")
        print("="*80)
        
        if run_unbalanced:
            print("\nProcessing with Unbalanced Data.")
            main(data_type='Unbalanced')
        else:
            print("\n[INFO] Unbalanced data processing disabled by feature flag")
        
        if run_balanced:
            print("\n---------------------------------------------------------")
            print("\nProcessing with Balanced Data.")
            main(data_type='Balanced')
        else:
            print("\n[INFO] Balanced data processing disabled by feature flag")
        
        if not run_unbalanced and not run_balanced:
            print("\n[WARNING] Both data types are disabled. No processing performed.")
        
        print("\n" + "="*80)
        print("PIPELINE COMPLETED")
        print("="*80)
    except Exception as pipeline_error:
        print("\n" + "="*80)
        print("PIPELINE FAILED WITH ERROR")
        print("="*80)
        print(f"Error: {pipeline_error}")
        import traceback
        traceback.print_exc()
        print("="*80)
    finally:
        # Restore stdout and close log file
        sys.stdout = original_stdout
        log_file.close()
    
    # Generate HTML report from log
    try:
        from utils.log_report_generator import generate_log_report
        
        report_path = base_output_dir / 'report.html'
        
        if log_path.exists():
            generate_log_report(str(log_path), str(report_path))
            print("HTML report generated successfully!")
            print(f"Report location: {report_path.absolute()}")
            print(f"Run ID: {experiment_name}")
        else:
            print(f"[WARNING] Log file not found at {log_path}")
    except ZeroDivisionError as e:
        print(f"[WARNING] Could not generate HTML report: Insufficient data for report generation.")
        print(f"[INFO] Enable Cross-Validation in ui_config.json to generate comprehensive reports.")
    except Exception as e:
        print(f"[WARNING] Could not generate HTML report: {e}")
        import traceback
        traceback.print_exc()
    
    # Create completion flag file for UI status tracking
    try:
        completion_flag = base_output_dir / 'COMPLETE.flag'
        completion_flag.touch()
        print(f"[INFO] Run marked as completed: {completion_flag}")
    except Exception as e:
        print(f"[WARNING] Could not create completion flag: {e}")
    
    # Create metadata.json for UI display
    try:
        # Collect enabled models
        enabled_models = []
        if get_config_value(ui_config, 'models.traditional_ml.multinomial_nb.enabled', DEFAULT_MULTINOMIAL_NB_ENABLED):
            enabled_models.append('MultinomialNB')
        if get_config_value(ui_config, 'models.traditional_ml.logistic_regression.enabled', DEFAULT_LOGISTIC_REGRESSION_ENABLED):
            enabled_models.append('LogisticRegression')
        if get_config_value(ui_config, 'models.traditional_ml.random_forest.enabled', DEFAULT_RANDOM_FOREST_ENABLED):
            enabled_models.append('RandomForest')
        if get_config_value(ui_config, 'models.deep_learning.mlp.enabled', DEFAULT_MLP_ENABLED):
            enabled_models.append('MLP')
        if get_config_value(ui_config, 'models.deep_learning.cnn.enabled', DEFAULT_CNN_ENABLED):
            enabled_models.append('CNN')
        
        # Collect data types
        data_types = []
        if get_config_value(ui_config, 'data.run_unbalanced', DEFAULT_RUN_UNBALANCED):
            data_types.append('Unbalanced')
        if get_config_value(ui_config, 'data.run_balanced', DEFAULT_RUN_BALANCED):
            data_types.append('Balanced')
        
        metadata = {
            'run_name': experiment_name,
            'timestamp': training_start_time,
            'status': 'Completed',
            'models': enabled_models,
            'data_types': data_types,
            'problem_type': 'multi_label'
        }
        
        metadata_path = base_output_dir / 'metadata.json'
        with open(metadata_path, 'w', encoding='utf-8') as f:
            json.dump(metadata, f, indent=2, ensure_ascii=False)
        print(f"[INFO] Metadata saved: {metadata_path}")
    except Exception as e:
        print(f"[WARNING] Could not create metadata file: {e}")


# =============================================================================
# LEGACY FUNCTIONS (Kept for backward compatibility)
# =============================================================================
def build_conditional_prob_matrix(df, labels):
    """
    Build a conditional probability matrix for label co-occurrence.

    Parameters:
    - df (pd.DataFrame): DataFrame containing the labels.
    - labels (list of str): List of label column names.

    Returns:
    - cooc_norm (np.ndarray): Normalized co-occurrence matrix.
    """
    cooc = df[labels].values.T.dot(df[labels].values)
    cooc_norm = cooc.copy().astype(np.float32)
    for i in range(cooc_norm.shape[0]):
        cooc_norm[:, i] /= cooc[i, i]
    return cooc_norm

def nnls_sample(df, labels, target_count, cond_prob):
    """
    Perform stratified sampling to balance the dataset based on label co-occurrence.

    Parameters:
    - df (pd.DataFrame): DataFrame containing the data and labels.
    - labels (list of str): List of label column names.
    - target_count (int): Desired number of samples per label.
    - cond_prob (np.ndarray): Conditional probability matrix from build_conditional_prob_matrix.

    Returns:
    - sampled_df (pd.DataFrame): The resampled DataFrame.
    """
    target_counts = np.array([target_count for _ in labels])
    optimal_samples, residuals = nnls(cond_prob, target_counts)
    optimal_samples = np.ceil(optimal_samples).astype(np.int32)
    df_subs = []
    for i, label in enumerate(labels):
        sub_df = df[df[label] == 1]
        df_subs.append(sub_df.sample(optimal_samples[i],
                                     replace=len(sub_df) < optimal_samples[i]))
    sampled_df = pd.concat(df_subs)
    return sampled_df

def load_data(csv_path: str):
    """
    Load the dataset from a CSV file and split it into training and testing sets using stratified splitting.

    Parameters:
    - csv_path (str): Path to the CSV file containing the dataset.

    Returns:
    - X_train (pd.DataFrame): Training set features.
    - X_test (pd.DataFrame): Testing set features.
    - y_train (pd.DataFrame): Training set labels.
    - y_test (pd.DataFrame): Testing set labels.
    """
    # Load the dataset from a CSV file
    data = pd.read_csv(csv_path)

    # Check if required columns exist
    required_columns = ['report', 'type_blocker', 'type_bug', 'type_documentation', 'type_enhancement']
    missing_columns = [col for col in required_columns if col not in data.columns]
    if missing_columns:
        raise ValueError(f"The following required columns are missing in the dataset: {missing_columns}")

    # Feature column (text data)
    X = data[['report']]

    # Label columns (multi-label targets)
    y = data[['type_blocker', 'type_bug', 'type_documentation', 'type_enhancement']]

    # Initialize the stratified shuffle split
    msss = MultilabelStratifiedShuffleSplit(n_splits=1, test_size=0.2, random_state=42)

    # Perform the split
    for train_index, test_index in msss.split(X, y):
        X_train = X.iloc[train_index].reset_index(drop=True)
        X_test = X.iloc[test_index].reset_index(drop=True)
        y_train = y.iloc[train_index].reset_index(drop=True)
        y_test = y.iloc[test_index].reset_index(drop=True)

    # Print label counts to check for class imbalance
    print("Label counts in y_train:")
    print(y_train.sum())
    print("\nLabel counts in y_test:")
    print(y_test.sum())
    return X_train, X_test, y_train, y_test

def load_data_balanced(csv_path: str):
    """
    Load the dataset from a CSV file, balance it, and split into training and testing sets.

    Parameters:
    - csv_path (str): Path to the CSV file containing the dataset.

    Returns:
    - X_train (pd.DataFrame): Training set features.
    - X_test (pd.DataFrame): Testing set features.
    - y_train (pd.DataFrame): Training set labels.
    - y_test (pd.DataFrame): Testing set labels.
    """
    # Load the dataset from a CSV file
    data = pd.read_csv(csv_path)

    # Build conditional probability matrix and perform NNLS sampling
    cooc_norm = build_conditional_prob_matrix(data, LABELS)
    resampled_df = nnls_sample(data, LABELS, 600, cooc_norm)
    data = resampled_df

    # Check if required columns exist
    required_columns = ['report', 'type_blocker', 'type_bug', 'type_documentation', 'type_enhancement']
    missing_columns = [col for col in required_columns if col not in data.columns]
    if missing_columns:
        raise ValueError(f"The following required columns are missing in the dataset: {missing_columns}")

    # Feature column (text data)
    X = data[['report']]

    # Label columns (multi-label targets)
    y = data[['type_blocker', 'type_bug', 'type_documentation', 'type_enhancement']]

    # Initialize the stratified shuffle split
    msss = MultilabelStratifiedShuffleSplit(n_splits=1, test_size=0.2, random_state=42)

    # Perform the split
    for train_index, test_index in msss.split(X, y):
        X_train = X.iloc[train_index].reset_index(drop=True)
        X_test = X.iloc[test_index].reset_index(drop=True)
        y_train = y.iloc[train_index].reset_index(drop=True)
        y_test = y.iloc[test_index].reset_index(drop=True)

    # Print label counts to check for class imbalance
    print("Label counts in y_train:")
    print(y_train.sum())
    print("\nLabel counts in y_test:")
    print(y_test.sum())
    return X_train, X_test, y_train, y_test

def prepare_data(X_train: pd.DataFrame, X_test: pd.DataFrame, y_train: pd.DataFrame, top_k=50, vocabulary=None):
    """
    Convert text data to TF-IDF features and perform feature selection using the Chi-Square test.

    Parameters:
    - X_train (pd.DataFrame): Training set features.
    - X_test (pd.DataFrame): Testing set features.
    - y_train (pd.DataFrame): Training set labels.
    - top_k (int): Number of top features to select based on Chi-Square scores.
    - vocabulary (list or None): Predefined vocabulary to use for TF-IDF vectorizer.

    Returns:
    - X_train_selected (sparse matrix): Selected training set features.
    - X_test_selected (sparse matrix): Selected testing set features.
    - selected_features (np.array): Names of the selected features.
    - chi2_scores_max (np.array): Maximum Chi-Square scores for each feature.
    - vector (TfidfVectorizer): The fitted TF-IDF vectorizer.
    """
    # Initialize the TF-IDF vectorizer
    vector = TfidfVectorizer(
        ngram_range=(1, 1),
        analyzer='word',
        stop_words='english',
        strip_accents='unicode',
        use_idf=True,
        min_df=1,
        vocabulary=vocabulary  # Predefined vocabulary
    )

    # Apply TF-IDF on the 'report' column
    X_train_tfidf = vector.fit_transform(X_train['report'])
    X_test_tfidf = vector.transform(X_test['report'])

    # Feature selection using Chi-Square test
    chi2_scores = []
    for i in range(y_train.shape[1]):
        chi2_score_values, p_value = chi2(X_train_tfidf, y_train.iloc[:, i])
        chi2_scores.append(chi2_score_values)

    # Aggregate Chi-Square scores across labels by taking the maximum score for each feature
    chi2_scores_max = np.max(np.array(chi2_scores), axis=0)

    # Select top K features based on Chi-Square scores
    if top_k > len(chi2_scores_max):
        top_k = len(chi2_scores_max)
    selected_indices = np.argsort(chi2_scores_max)[::-1][:top_k]
    selected_indices = selected_indices.astype(int)
    X_train_selected = X_train_tfidf[:, selected_indices]
    X_test_selected = X_test_tfidf[:, selected_indices]

    # Retrieve selected feature names
    selected_features = np.array(vector.get_feature_names_out())[selected_indices]
    print(f"\nSelected top {top_k} features based on Chi-Square scores:")
    print(selected_features[:20])

    return X_train_selected, X_test_selected, selected_features, chi2_scores_max, vector

def prepare_data_for_deep_learning(X_train_texts, X_test_texts, max_words=5000, max_len=100):
    """
    Tokenize and pad text data for deep learning models.

    Parameters:
    - X_train_texts: Training text data.
    - X_test_texts: Testing text data.
    - max_words: Maximum number of words to consider in the vocabulary.
    - max_len: Maximum length of sequences after padding.

    Returns:
    - X_train_pad: Padded training sequences.
    - X_test_pad: Padded testing sequences.
    - tokenizer: Fitted Keras tokenizer.
    """
    tokenizer = Tokenizer(num_words=max_words, oov_token='')
    tokenizer.fit_on_texts(X_train_texts)

    X_train_seq = tokenizer.texts_to_sequences(X_train_texts)
    X_test_seq = tokenizer.texts_to_sequences(X_test_texts)

    X_train_pad = pad_sequences(X_train_seq, maxlen=max_len, padding='post', truncating='post')
    X_test_pad = pad_sequences(X_test_seq, maxlen=max_len, padding='post', truncating='post')

    return X_train_pad, X_test_pad, tokenizer

def prepare_data_for_transformer(X_texts, tokenizer, max_len=100):
    """
    Tokenize text data for the Transformer model.

    Parameters:
    - X_texts: List or Series of text data.
    - tokenizer: Pretrained tokenizer from Hugging Face.
    - max_len: Maximum sequence length.

    Returns:
    - input_ids: Token IDs for each text.
    - attention_masks: Attention masks for each text.
    """
    encodings = tokenizer.batch_encode_plus(
        X_texts.tolist(),
        max_length=max_len,
        padding='max_length',
        truncation=True,
        return_attention_mask=True,
        return_tensors='tf'
    )

    return encodings['input_ids'], encodings['attention_mask']

# ====================================
# Visualization Functions
# ====================================

def visualize_description_length(X_train: pd.DataFrame, data_type: str):
    """
    Visualize the distribution of description lengths in the given DataFrame.

    Parameters:
    - X_train (pd.DataFrame): DataFrame containing the 'report' column.
    - data_type (str): String indicating the type of data (e.g., 'Balanced').
    """
    sns.set(style="darkgrid")
    X_train['report'] = X_train['report'].astype(str)
    description_len = X_train['report'].str.len()
    plt.figure(figsize=(10, 6))
    sns.histplot(description_len, kde=False, bins=20, color="steelblue")
    plt.xlabel('Description Length')
    plt.ylabel('Frequency')
    plt.title('Description Length Distribution')
    plt.tight_layout()
    plt.savefig(f'description_length_distribution_{data_type}.png')
    plt.close()
    print(f"Description length distribution plot saved as 'description_length_distribution_{data_type}.png'.")

def visualize_class_distribution(y_train: pd.DataFrame, y_test: pd.DataFrame, data_type: str, save_path='class_distribution.png'):
    """
    Visualize the distribution of classes within each label for both training and test datasets.

    Parameters:
    - y_train (pd.DataFrame): Training set labels.
    - y_test (pd.DataFrame): Testing set labels.
    - data_type (str): String indicating the type of data (e.g., 'Balanced').
    - save_path (str): Filename for saving the plot.
    """
    labels = y_train.columns.tolist()
    bar_width = 0.2
    bars1 = [sum(y_train[label] == 1) for label in labels]
    bars2 = [sum(y_train[label] == 0) for label in labels]
    bars3 = [sum(y_test[label] == 1) for label in labels]
    bars4 = [sum(y_test[label] == 0) for label in labels]

    r1 = np.arange(len(bars1))
    r2 = [x + bar_width for x in r1]
    r3 = [x + bar_width for x in r2]
    r4 = [x + bar_width for x in r3]

    plt.figure(figsize=(12, 8))
    plt.bar(r1, bars1, color='steelblue', width=bar_width, label='Train Labeled = 1')
    plt.bar(r2, bars2, color='lightsteelblue', width=bar_width, label='Train Labeled = 0')
    plt.bar(r3, bars3, color='darkorange', width=bar_width, label='Test Labeled = 1')
    plt.bar(r4, bars4, color='navajowhite', width=bar_width, label='Test Labeled = 0')

    plt.xlabel('Labels', fontweight='bold')
    plt.xticks([r + bar_width * 1.5 for r in range(len(bars1))], labels, rotation=45)
    plt.legend()
    plt.title('Distribution of Classes within Each Label')
    plt.tight_layout()
    plt.savefig(f'{data_type}_{save_path}')
    plt.close()
    print(f"Class distribution plot saved as '{data_type}_{save_path}'.")

def visualize_word_cloud(X_train: pd.DataFrame, y_train: pd.DataFrame, token: str, max_words=50):
    """
    Visualize the most common words contributing to the token and return top words.

    Parameters:
    - X_train (pd.DataFrame): Training set features.
    - y_train (pd.DataFrame): Training set labels.
    - token (str): The label to visualize word cloud for.
    - max_words (int): Number of top words to return.

    Returns:
    - top_words (list): List of top words based on frequency.
    """
    description_context = X_train.join(y_train)
    description_context = description_context[description_context[token] == 1]
    if description_context.empty:
        print(f"No instances for label '{token}'; skipping word cloud.")
        return []

    description_text = description_context['report']
    combined_text = ' '.join(description_text)
    wordcloud = WordCloud(width=1600, height=800, max_font_size=200, background_color='white').generate(combined_text)
    plt.figure(figsize=(15, 10))
    plt.imshow(wordcloud.recolor(colormap="Blues"), interpolation='bilinear')
    plt.axis("off")
    plt.title(f"Most Common Words Associated with '{token}' Defects", size=20)
    plt.tight_layout()
    plt.savefig(f'wordcloud_{token}.png')
    plt.close()
    print(f"Word cloud for '{token}' saved as 'wordcloud_{token}.png'.")

    # Extract word frequencies from the word cloud
    word_freq = wordcloud.words_
    # Sort words by frequency
    sorted_words = sorted(word_freq.items(), key=lambda x: x[1], reverse=True)
    # Get top N words
    top_words = [word for word, freq in sorted_words[:max_words]]
    return top_words

def visualize_f1_scores(methods: pd.DataFrame,data_type: str):
    """
    Visualize F1 score results through a box plot with jittered points.

    Parameters:
    - methods (pd.DataFrame): DataFrame containing evaluation metrics.
    """
    plt.figure(figsize=(12, 8))
    ax = sns.boxplot(x='Model', y='F1', data=methods, palette="Blues")
    sns.stripplot(x='Model', y='F1', data=methods, size=8, jitter=True,
                  edgecolor="gray", linewidth=2, palette="Blues")
    ax.set_xticklabels(ax.get_xticklabels(), rotation=20)
    plt.title('F1 Score Distribution by Model')
    plt.ylabel('F1 Score')
    plt.xlabel('Model')
    plt.tight_layout()
    plt.savefig(f'f1_score_distribution_{data_type}.png')
    plt.close()
    print("F1 score distribution plot saved as 'f1_score_distribution.png'.")

def visualize_all_metrics_boxplot(methods: pd.DataFrame, data_type: str):
    """
    Create a box plot comparing Recall, F1-score, and Hamming Loss across all models.

    Parameters:
    - methods (pd.DataFrame): DataFrame containing evaluation metrics.
    - data_type (str): String indicating the type of data (e.g., 'Balanced').
    """
    # Melt the DataFrame to have Metrics in a single column
    metrics_melted = methods.melt(id_vars=['Model', 'Label'], value_vars=['Recall', 'F1', 'Hamming Loss'],
                                  var_name='Metric', value_name='Score')

    plt.figure(figsize=(14, 8))
    sns.boxplot(x='Metric', y='Score', hue='Model', data=metrics_melted, palette="Set2")
    sns.stripplot(x='Metric', y='Score', hue='Model', data=metrics_melted, dodge=True,
                  color='gray', alpha=0.6, size=5, jitter=True)
    plt.title('Comparison of Evaluation Metrics Across Models')
    plt.xlabel('Evaluation Metric')
    plt.ylabel('Score')
    # Handle legends to avoid duplication
    handles, labels = plt.gca().get_legend_handles_labels()
    by_label = dict(zip(labels, handles))
    plt.legend(by_label.values(), by_label.keys(), title='Model', bbox_to_anchor=(1.05, 1), loc='upper left')
    plt.tight_layout()
    plt.savefig(f'all_metrics_comparison_boxplot_{data_type}.png')
    plt.close()
    print(f"All metrics comparison box plot saved as 'all_metrics_comparison_boxplot_{data_type}.png'.")

def visualize_nb_metrics(methods: pd.DataFrame, data_type: str):
    """
    Create a bar graph of F1 and Recall across each label for Multinomial Naive Bayes.

    Parameters:
    - methods (pd.DataFrame): DataFrame containing evaluation metrics.
    - data_type (str): String indicating the type of data (e.g., 'Balanced').
    """
    print("Plot for Multinomial Naive Bayes regression")
    m2 = methods[methods.Model == 'MultinomialNB'].copy()
    if m2.empty:
        print("No data available for MultinomialNB metrics; skipping plot.")
        return
    m2.set_index(["Label"], inplace=True)
    ax = m2[['Recall', 'F1']].plot(figsize=(16, 8), kind='bar', title='Multinomial Naive Bayes Metrics by Label',
                                   rot=60, ylim=(0.0, 1), colormap='tab10')
    plt.ylabel('Score')
    plt.xlabel('Labels')
    plt.tight_layout()
    plt.savefig(f'mnb_metrics_per_label_{data_type}.png')
    plt.close()
    print(f"Multinomial Naive Bayes metrics per label plot saved as 'mnb_metrics_per_label_{data_type}.png'.")

def visualize_correlation_matrix(y_train: pd.DataFrame, data_type: str):
    """
    Visualize the cross-correlation matrix across labels in the given DataFrame.

    Parameters:
    - y_train (pd.DataFrame): Training set labels.
    - data_type (str): String indicating the type of data (e.g., 'Balanced').
    """
    label_columns = y_train.columns.tolist()
    if not label_columns:
        print("No label columns found in the DataFrame for correlation matrix; skipping plot.")
        return

    train_corr = y_train.copy()
    if train_corr.empty:
        print("No data available for correlation matrix; skipping plot.")
        return

    corr = train_corr.corr()
    plt.figure(figsize=(10, 8))
    sns.heatmap(corr, annot=True, cmap="Blues", fmt=".2f",
                xticklabels=corr.columns.values, yticklabels=corr.columns.values)
    plt.title('Correlation Matrix of Labels')
    plt.tight_layout()
    plt.savefig(f'label_correlation_matrix_{data_type}.png')
    plt.close()
    print(f"Label correlation matrix plot saved as 'label_correlation_matrix_{data_type}.png'.")

def visualize_label_frequency(y_train: pd.DataFrame, data_type: str):
    """
    Visualize the frequency of specified labels in the given DataFrame.

    Parameters:
    - y_train (pd.DataFrame): Training set labels.
    - data_type (str): String indicating the type of data (e.g., 'Balanced').
    """
    labels = ["type_bug", "type_documentation", "type_enhancement"]
    # Filter labels that exist in the DataFrame
    present_labels = [label for label in labels if label in y_train.columns]
    if not present_labels:
        print("No specified labels found in the DataFrame; skipping label frequency plot.")
        return

    label_count = y_train[present_labels].sum()
    plt.figure(figsize=(10, 6))
    sns.barplot(x=label_count.index, y=label_count.values, color="steelblue")
    plt.title('Labels Frequency')
    plt.xlabel('Labels')
    plt.ylabel('Frequency')
    plt.xticks(rotation=45)
    plt.tight_layout()
    plt.savefig(f'label_frequency_{data_type}.png')
    plt.close()
    print(f"Label frequency plot saved as 'label_frequency_{data_type}.png'.")

def plot_top_features(selected_features, chi2_scores_max, data_type: str, top_k_plot=20):
    """
    Plot the top features based on Chi-Square scores.

    Parameters:
    - selected_features (np.array): Array of selected feature names.
    - chi2_scores_max (np.array): Maximum Chi-Square scores for each feature.
    - data_type (str): String indicating the type of data (e.g., 'Balanced').
    - top_k_plot (int): Number of top features to plot.
    """
    top_features = selected_features[:top_k_plot]
    selected_indices = np.argsort(chi2_scores_max)[::-1][:top_k_plot]
    selected_indices = selected_indices.astype(int)
    top_scores = chi2_scores_max[selected_indices[:top_k_plot]]
    # Plot the top features
    plt.figure(figsize=(12, 8))
    sns.barplot(x=top_scores, y=top_features)
    plt.title(f'Top {top_k_plot} Features Based on Chi-Square Scores')
    plt.xlabel('Chi-Square Score')
    plt.ylabel('Feature')
    plt.tight_layout()
    plt.savefig(f'chi2_features_{data_type}.png')
    plt.close()

    print(f"\nTop {top_k_plot} features have been plotted and saved as 'chi2_features_{data_type}.png'.")

# ====================================
# Model Training and Evaluation
# ====================================

def cross_validation_score_multilabel(classifier, X, y, n_splits=10):
    """
    Perform cross-validation and compute average Recall and F1-score.

    Parameters:
    - classifier: The classifier to evaluate.
    - X (sparse matrix or ndarray): Feature matrix.
    - y (ndarray): Label matrix.
    - n_splits (int): Number of cross-validation splits.

    Returns:
    - dict: Dictionary containing average Recall and F1-score.
    """
    mskf = MultilabelStratifiedKFold(n_splits=n_splits, shuffle=True, random_state=42)

    recall_scores = []
    f1_scores = []

    for fold, (train_index, test_index) in enumerate(mskf.split(X, y), 1):
        X_train_cv, X_test_cv = X[train_index], X[test_index]
        y_train_cv, y_test_cv = y[train_index], y[test_index]

        classifier.fit(X_train_cv, y_train_cv)
        y_pred_cv = classifier.predict(X_test_cv)

        # Compute recall and F1 score
        recall = recall_score(y_test_cv, y_pred_cv, average='macro', zero_division=0)
        f1 = f1_score(y_test_cv, y_pred_cv, average='macro', zero_division=0)

        recall_scores.append(recall)
        f1_scores.append(f1)

        print(f"Fold {fold}: Recall = {recall:.4f}, F1-Score = {f1:.4f}")

    avg_recall = np.mean(recall_scores)
    avg_f1 = np.mean(f1_scores)

    return {'Recall': avg_recall, 'F1': avg_f1}

def cross_validation_score_deep_learning(model_builder, X, y, n_splits=10, epochs=10, batch_size=32):
    """
    Perform cross-validation for a Deep Learning model and compute average Recall and F1-score.

    Parameters:
    - model_builder: Function to build the model.
    - X (ndarray): Feature matrix.
    - y (ndarray): Label matrix.
    - n_splits (int): Number of cross-validation splits.
    - epochs (int): Number of training epochs.
    - batch_size (int): Batch size for training.

    Returns:
    - dict: Dictionary containing average Recall and F1-score.
    """
    mskf = MultilabelStratifiedKFold(n_splits=n_splits, shuffle=True, random_state=42)

    recall_scores = []
    f1_scores = []

    for fold, (train_index, test_index) in enumerate(mskf.split(X, y), 1):
        X_train_cv, X_test_cv = X[train_index], X[test_index]
        y_train_cv, y_test_cv = y[train_index], y[test_index]

        model = model_builder()

        early_stop = EarlyStopping(monitor='val_loss', patience=3, restore_best_weights=True)

        model.fit(
            X_train_cv,
            y_train_cv,
            epochs=epochs,
            batch_size=batch_size,
            validation_data=(X_test_cv, y_test_cv),
            callbacks=[early_stop],
            verbose=0
        )

        y_pred_cv_prob = model.predict(X_test_cv)
        y_pred_cv = (y_pred_cv_prob >= 0.5).astype(int)

        # Compute recall and F1 score
        recall = recall_score(y_test_cv, y_pred_cv, average='macro', zero_division=0)
        f1 = f1_score(y_test_cv, y_pred_cv, average='macro', zero_division=0)

        recall_scores.append(recall)
        f1_scores.append(f1)

        print(f"Fold {fold}: {model_builder.__name__} Recall = {recall:.4f}, F1-Score = {f1:.4f}")

    avg_recall = np.mean(recall_scores)
    avg_f1 = np.mean(f1_scores)

    return {'Recall': avg_recall, 'F1': avg_f1}

def evaluate_classifier(clf, clf_name, X_train, y_train, X_test, y_test, label_names, enable_error_analysis=False):
    """
    Train the classifier, make predictions, and evaluate performance.

    Parameters:
    - clf: The classifier to evaluate.
    - clf_name (str): Name of the classifier.
    - X_train (sparse matrix or ndarray): Training feature matrix.
    - y_train (ndarray): Training labels.
    - X_test (sparse matrix or ndarray): Testing feature matrix.
    - y_test (ndarray): Testing labels.
    - label_names (list): List of label names.
    - enable_error_analysis (bool): Whether to perform detailed error analysis.

    Returns:
    - list of dicts: List containing evaluation metrics for each label.
    """
    print(f"\n===== Evaluating {clf_name} =====")

    # Fit the model
    clf.fit(X_train, y_train)

    # Make predictions
    predictions = clf.predict(X_test)

    # Calculate Hamming Loss
    hamming_loss_value = hamming_loss(y_test, predictions)

    # Perform detailed error analysis (if enabled)
    if enable_error_analysis:
        try:
            print(f"\n----- Error Analysis for {clf_name} -----")
            analyze_misclassifications(y_test, predictions, label_names, clf_name)
        except Exception as e:
            print(f"[WARNING] Error analysis failed for {clf_name}: {e}")
    else:
        print(f"[INFO] Error analysis disabled for {clf_name}")

    # Calculate metrics for each label
    metrics = []
    n_labels = y_test.shape[1]

    for label_idx in range(n_labels):
        y_true_label = y_test[:, label_idx]
        y_pred_label = predictions[:, label_idx]

        recall = recall_score(y_true_label, y_pred_label, zero_division=0)
        f1 = f1_score(y_true_label, y_pred_label, zero_division=0)

        metrics.append({
            'Model': clf_name,
            'Label': label_names[label_idx],
            'Recall': recall,
            'F1': f1,
            'Hamming Loss': hamming_loss_value
        })

    # Print Hamming Loss
    print(f"Hamming Loss for {clf_name}: {hamming_loss_value}")

    return metrics

def evaluate_deep_learning_model(model, X_test, y_test, model_name, label_names, enable_error_analysis=False):
    """
    Evaluate the Deep Learning model on the test set.

    Parameters:
    - model: The trained Keras model.
    - X_test (sparse matrix or ndarray): Testing feature matrix.
    - y_test (ndarray): Testing labels.
    - model_name (str): Name of the model for reporting.
    - label_names (list): List of label names.
    - enable_error_analysis (bool): Whether to perform detailed error analysis.

    Returns:
    - list of dicts: List containing evaluation metrics for each label.
    """
    print(f"\n===== Evaluating {model_name} Model =====")

    if hasattr(X_test, "toarray"):
        X_test = X_test.toarray()
    # Make predictions
    y_pred_prob = model.predict(X_test)
    y_pred = (y_pred_prob >= 0.5).astype(int)

    # Calculate Hamming Loss
    hamming_loss_value = hamming_loss(y_test, y_pred)

    # Perform detailed error analysis (if enabled)
    if enable_error_analysis:
        try:
            print(f"\n----- Error Analysis for {model_name} -----")
            analyze_misclassifications(y_test, y_pred, label_names, model_name)
        except Exception as e:
            print(f"[WARNING] Error analysis failed for {model_name}: {e}")
    else:
        print(f"[INFO] Error analysis disabled for {model_name}")

    # Calculate metrics for each label
    metrics = []
    n_labels = y_test.shape[1]

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

    # Print Hamming Loss
    print(f"Hamming Loss for {model_name} Model: {hamming_loss_value}")

    return metrics

def build_deep_learning_model(input_dim, output_dim):
    """
    Build and compile a Multilayer Perceptron (MLP) model for multi-label classification.

    Parameters:
    - input_dim (int): Number of input features.
    - output_dim (int): Number of output labels.

    Returns:
    - model (Sequential): Compiled Keras model.
    """
    model = Sequential()
    model.add(Dense(256, input_dim=input_dim, activation='relu'))
    model.add(Dropout(0.5))
    model.add(Dense(128, activation='relu'))
    model.add(Dropout(0.5))
    model.add(Dense(output_dim, activation='sigmoid'))  # Sigmoid for multi-label classification

    model.compile(loss='binary_crossentropy', optimizer='adam', metrics=['accuracy'])
    return model

def build_cnn_model():
    """
    Build and compile a CNN model for text classification.

    Returns:
    - model: Compiled Keras model.
    """
    global vocab_size, embedding_dim, max_len, output_dim
    model = Sequential()
    model.add(Embedding(input_dim=vocab_size, output_dim=embedding_dim, input_length=max_len))
    model.add(Conv1D(128, 5, activation='relu'))
    model.add(GlobalMaxPooling1D())
    model.add(Dense(128, activation='relu'))
    model.add(Dropout(0.5))
    model.add(Dense(output_dim, activation='sigmoid'))  # Sigmoid activation for multi-label classification

    model.compile(loss='binary_crossentropy', optimizer='adam', metrics=['accuracy'])

    return model

# ====================================
# Main Execution
# ====================================

def main(data_type='Unbalanced'):
    """
    Main function to execute data processing, model training, evaluation, and visualization.

    Parameters:
    - data_type (str): Type of data to process ('Unbalanced' or 'Balanced').
    """
    # ----------------------------
    # Load Data
    # ----------------------------
    csv_path = str(DATASET_PATH)
    try:
        if data_type == 'Balanced':
            X_train_df, X_test_df, y_train_df, y_test_df = load_data_balanced(csv_path)
        else:
            X_train_df, X_test_df, y_train_df, y_test_df = load_data(csv_path)
    except Exception as e:
        print(f"Error loading data: {e}")
        return

    # ----------------------------
    # Visualizations
    # ----------------------------
    visualize_description_length(X_train_df, data_type)
    visualize_class_distribution(y_train_df, y_test_df, data_type)
    visualize_correlation_matrix(y_train_df, data_type)
    visualize_label_frequency(y_train_df, data_type)

    # ----------------------------
    # Word Clouds and Vocabulary Collection
    # ----------------------------
    vocab_set = set()  # Initialize an empty set to collect unique words

    for label in LABELS:
        top_words = visualize_word_cloud(X_train_df, y_train_df, label)
        vocab_set.update(top_words)

    wordcloud_vocab = list(vocab_set)
    print(f"\nTotal unique words collected from word clouds: {len(wordcloud_vocab)}")

    # ----------------------------
    # Prepare Data with Vocabulary
    # ----------------------------
    top_k = 50  # Number of top features to select based on Chi-Square scores
    try:
        X_train_tfidf, X_test_tfidf, selected_features, chi2_scores_max, vector = prepare_data(
            X_train_df, X_test_df, y_train_df, top_k=top_k, vocabulary=wordcloud_vocab
        )
    except Exception as e:
        print(f"Error during data preparation: {e}")
        return

    # ----------------------------
    # Check for Selected Features
    # ----------------------------
    if X_train_tfidf.shape[1] == 0:
        raise ValueError("No features were selected. Consider reducing the 'top_k' parameter or using alternative feature selection methods.")

    # ----------------------------
    # Convert Labels to NumPy Arrays
    # ----------------------------
    y_train_np = y_train_df.to_numpy()
    y_test_np = y_test_df.to_numpy()

    # Get label names
    label_names = y_test_df.columns.tolist()

    # Plot Top Features
    plot_top_features(selected_features, chi2_scores_max, data_type, top_k_plot=20)

    # ----------------------------
    # Define Classifiers
    # ----------------------------
    clf1 = ClassifierChain(MultinomialNB())
    clf2 = ClassifierChain(LogisticRegression(max_iter=10000))
    clf3 = ClassifierChain(RandomForestClassifier(n_estimators=100, random_state=42))

    # ----------------------------
    # Cross-Validation for Traditional Models
    # ----------------------------
    meth_cv = []
    for clf, model_name in zip([clf1, clf2, clf3], ['MultinomialNB', 'LogisticRegression', 'RandomForest']):
        print(f"\n===== Cross-Validating {model_name} =====")
        cv_scores = cross_validation_score_multilabel(clf, X_train_tfidf, y_train_np)
        meth_cv.append({'Model': model_name, 'Recall': cv_scores['Recall'], 'F1': cv_scores['F1']})
    meth_cv = pd.DataFrame(meth_cv)
    print("\nCross-validation results:")
    print(meth_cv[['Model', 'Recall', 'F1']])

    # ----------------------------
    # Evaluate Classifiers on Test Set
    # ----------------------------
    results_nb = evaluate_classifier(clf1, 'MultinomialNB', X_train_tfidf, y_train_np, X_test_tfidf, y_test_np, label_names)
    results_lr = evaluate_classifier(clf2, 'LogisticRegression', X_train_tfidf, y_train_np, X_test_tfidf, y_test_np, label_names)
    results_rf = evaluate_classifier(clf3, 'RandomForest', X_train_tfidf, y_train_np, X_test_tfidf, y_test_np, label_names)

    # ----------------------------
    # Cross-Validation for Deep Learning Model
    # ----------------------------
    print("\n===== Training and Evaluating Deep Learning Model via Cross-Validation =====")
    deep_learning_cv_scores = cross_validation_score_deep_learning(
        lambda: build_deep_learning_model(X_train_tfidf.shape[1], y_train_np.shape[1]),
        X_train_tfidf.toarray(), y_train_np, n_splits=10, epochs=100, batch_size=16
    )
    print("\nDeep Learning Cross-validation results:")
    print(f"Recall: {deep_learning_cv_scores['Recall']:.4f}")
    print(f"F1-score: {deep_learning_cv_scores['F1']:.4f}")

    # ----------------------------
    # Train Deep Learning Model on Entire Training Set
    # ----------------------------
    print("\n===== Training Deep Learning Model on Entire Training Set =====")
    deep_learning_model = build_deep_learning_model(input_dim=X_train_tfidf.shape[1], output_dim=y_train_np.shape[1])

    early_stop = EarlyStopping(monitor='val_loss', patience=5, restore_best_weights=True)

    deep_learning_model.fit(
        X_train_tfidf.toarray(),
        y_train_np,
        epochs=100,
        batch_size=16,
        validation_split=0.2,
        callbacks=[early_stop],
        verbose=0
    )

    # ----------------------------
    # Evaluate Deep Learning Model on Test Set
    # ----------------------------
    results_dl = evaluate_deep_learning_model(deep_learning_model, X_test_tfidf, y_test_np, 'MLP', label_names)

    # ----------------------------
    # Prepare Data for CNN
    # ----------------------------
    X_train_dl, X_test_dl, tokenizer = prepare_data_for_deep_learning(
        X_train_df['report'],
        X_test_df['report'],
        max_words=5000,
        max_len=100
    )

    # Parameters for CNN
    global vocab_size, embedding_dim, max_len, output_dim
    vocab_size = min(len(tokenizer.word_index) + 1, 5000)
    embedding_dim = 100
    max_len = X_train_dl.shape[1]
    output_dim = y_train_np.shape[1]

    # ----------------------------
    # Cross-Validation for CNN Model
    # ----------------------------
    print("\n===== Training and Evaluating CNN Model via Cross-Validation =====")
    cnn_cv_scores = cross_validation_score_deep_learning(
        build_cnn_model, X_train_dl, y_train_np, n_splits=10, epochs=10, batch_size=32
    )
    print("\nCNN Cross-validation results:")
    print(f"Recall: {cnn_cv_scores['Recall']:.4f}")
    print(f"F1-score: {cnn_cv_scores['F1']:.4f}")

    # ----------------------------
    # Train CNN Model on Entire Training Set
    # ----------------------------
    print("\n===== Training CNN Model on Entire Training Set =====")
    cnn_model = build_cnn_model()

    early_stop = EarlyStopping(monitor='val_loss', patience=5, restore_best_weights=True)

    cnn_model.fit(
        X_train_dl,
        y_train_np,
        epochs=20,
        batch_size=32,
        validation_split=0.2,
        callbacks=[early_stop],
        verbose=1
    )

    # ----------------------------
    # Evaluate CNN Model on Test Set
    # ----------------------------
    results_cnn = evaluate_deep_learning_model(cnn_model, X_test_dl, y_test_np, 'CNN', label_names)

    # ----------------------------
    # Combine Results
    # ----------------------------
    combined_results = results_nb + results_lr + results_rf + results_dl + results_cnn
    df_results = pd.DataFrame(combined_results)

    # Convert 'Hamming Loss' to numeric
    df_results['Hamming Loss'] = pd.to_numeric(df_results['Hamming Loss'], errors='coerce')

    # ----------------------------
    # Visualization of Results
    # ----------------------------
    sns.set(style="whitegrid")

    # Box plot for F1 Score Distribution
    visualize_f1_scores(df_results,data_type)

    # Box plot comparing Recall, F1-score, and Hamming Loss across all models
    visualize_all_metrics_boxplot(df_results, data_type)

    # Bar plots for Multinomial Naive Bayes metrics
    visualize_nb_metrics(df_results, data_type)

    print("\nAll visualization processes completed successfully. Plots have been saved.")
