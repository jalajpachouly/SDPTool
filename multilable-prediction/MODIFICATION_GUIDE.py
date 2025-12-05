"""
Model Saving Integration for main.py

Add these imports at the top of main.py (after existing imports):
"""

# Add to imports section (around line 50):
from utils.model_persistence import ModelPersistence

"""
Add this function before the main() function (around line 295):
"""

def save_best_model_from_results(
    combined_results,
    all_trained_models,
    vectorizer,
    feature_selector,
    experiment_name,
    ui_config
):
    """
    Identify and save the best performing model from training results.
    
    Args:
        combined_results: List of result dictionaries from all models
        all_trained_models: Dictionary mapping model names to trained model objects
        vectorizer: TfidfVectorizer used for feature extraction
        feature_selector: Chi-square selector (if used)
        experiment_name: Name of the experiment/run
        ui_config: Configuration dictionary
    """
    if not combined_results:
        print("\n[INFO] No results available, skipping model save")
        return
    
    # Convert to DataFrame for easier analysis
    df_results = pd.DataFrame(combined_results)
    
    # Find best model based on F1 Score
    best_idx = df_results['F1 Score'].idxmax()
    best_result = df_results.loc[best_idx]
    best_model_name = best_result['Model']
    
    print(f"\n{'='*80}")
    print(f"IDENTIFYING BEST MODEL FOR PERSISTENCE")
    print(f"{'='*80}")
    print(f"Best Model: {best_model_name}")
    print(f"F1 Score: {best_result['F1 Score']:.4f}")
    print(f"Precision: {best_result['Precision']:.4f}")
    print(f"Recall: {best_result['Recall']:.4f}")
    print(f"Hamming Loss: {best_result['Hamming Loss']:.4f}")
    print(f"{'='*80}\n")
    
    # Get the actual model object
    if best_model_name not in all_trained_models:
        print(f"[WARNING] Model '{best_model_name}' not found in trained models dictionary")
        return
    
    best_model = all_trained_models[best_model_name]
    
    # Determine model type
    model_type = 'deep_learning' if best_model_name in ['MLP', 'CNN'] else 'traditional_ml'
    
    # Prepare metrics dictionary
    metrics = {
        'precision': float(best_result['Precision']),
        'recall': float(best_result['Recall']),
        'f1_score': float(best_result['F1 Score']),
        'hamming_loss': float(best_result['Hamming Loss'])
    }
    
    # Get tokenizer if CNN model
    tokenizer = all_trained_models.get('tokenizer', None) if best_model_name == 'CNN' else None
    
    # Save the model
    persistence = ModelPersistence()
    
    try:
        model_dir = persistence.save_best_model(
            model=best_model,
            model_name=best_model_name,
            model_type=model_type,
            metrics=metrics,
            vectorizer=vectorizer,
            feature_selector=feature_selector if feature_selector else None,
            tokenizer=tokenizer,
            config=ui_config,
            run_id=experiment_name,
            custom_name=None  # Will use experiment_name as run_id
        )
        print(f"[SUCCESS] Best model saved successfully to: {model_dir}")
    except Exception as e:
        print(f"[ERROR] Failed to save model: {e}")
        import traceback
        traceback.print_exc()


"""
Modify the main() function to track trained models.
Around line 440, after defining classifiers, add:
"""

# Track all trained models for persistence
all_trained_models = {}

"""
After each model is trained and evaluated (around lines 475, 485, 495 for traditional ML):
For MultinomialNB (after line 475):
"""
classifiers_to_run.append((clf_nb, 'MultinomialNB'))
all_trained_models['MultinomialNB'] = clf_nb  # ADD THIS LINE

"""
For LogisticRegression (after line 483):
"""
classifiers_to_run.append((clf_lr, 'LogisticRegression'))
all_trained_models['LogisticRegression'] = clf_lr  # ADD THIS LINE

"""
For RandomForest (after line 491):
"""
classifiers_to_run.append((clf_rf, 'RandomForest'))
all_trained_models['RandomForest'] = clf_rf  # ADD THIS LINE

"""
For MLP (after the model is trained, around line 542):
"""
mlp_results = evaluate_deep_learning_model(...)
all_trained_models['MLP'] = deep_learning_model  # ADD THIS LINE

"""
For CNN (after the model is trained and before the tokenizer is used, around line 570):
Store tokenizer first:
"""
all_trained_models['tokenizer'] = tokenizer  # ADD THIS LINE (before CNN training)

"""
After CNN is trained (around line 598):
"""
cnn_results = evaluate_deep_learning_model(...)
all_trained_models['CNN'] = cnn_model  # ADD THIS LINE

"""
Finally, after combined_results are created (around line 605), add the save call:
"""

# Combine Results from all enabled models
combined_results = traditional_ml_results + mlp_results + cnn_results

if not combined_results:
    print("\n[WARNING] No models were enabled. No results to display.")
    return

# SAVE BEST MODEL - ADD THIS BLOCK
save_best_model_from_results(
    combined_results=combined_results,
    all_trained_models=all_trained_models,
    vectorizer=vector,  # 'vector' is the variable name from prepare_data()
    feature_selector=None,  # Add chi2 selector if you saved it during prepare_data
    experiment_name=experiment_name,
    ui_config=ui_config
)

# Continue with existing code...
df_results = pd.DataFrame(combined_results)
...


"""
That's it! The modifications are minimal and focused:
1. Import ModelPersistence
2. Add save_best_model_from_results() function
3. Track trained models in all_trained_models dict
4. Call save_best_model_from_results() after training

The best model will be automatically saved based on F1 score.
"""
