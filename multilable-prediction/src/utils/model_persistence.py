"""
Model Persistence Utility for Multi-Label Classification

This module provides functionality to save and load the best performing model
from a training run, along with metadata and preprocessing artifacts.
"""

import json
import pickle
from pathlib import Path
from datetime import datetime
from typing import Dict, Any, Optional, Tuple
import joblib
import numpy as np


class ModelPersistence:
    """Handle saving and loading of trained models with metadata."""
    
    def __init__(self, base_dir: str = "multilable-prediction/models"):
        """
        Initialize model persistence handler.
        
        Args:
            base_dir: Base directory for storing models
        """
        self.base_dir = Path(base_dir)
        self.base_dir.mkdir(parents=True, exist_ok=True)
    
    def generate_run_id(self, custom_name: Optional[str] = None) -> str:
        """
        Generate a unique run ID.
        
        Args:
            custom_name: Optional custom name prefix
            
        Returns:
            Run ID string (e.g., 'CNN_Best_20231205_143022' or '20231205_143022')
        """
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        if custom_name:
            # Sanitize custom name (remove special characters)
            safe_name = "".join(c if c.isalnum() or c in ('_', '-') else '_' for c in custom_name)
            return f"{safe_name}_{timestamp}"
        return timestamp
    
    def save_best_model(
        self,
        model: Any,
        model_name: str,
        model_type: str,
        metrics: Dict[str, float],
        vectorizer: Any,
        feature_selector: Optional[Any],
        tokenizer: Optional[Any],
        config: Dict[str, Any],
        run_id: Optional[str] = None,
        custom_name: Optional[str] = None
    ) -> str:
        """
        Save the best performing model with all necessary artifacts.
        
        Args:
            model: Trained model object
            model_name: Name of the model (e.g., 'RandomForest', 'CNN')
            model_type: Type ('traditional_ml' or 'deep_learning')
            metrics: Dictionary of performance metrics
            vectorizer: TfidfVectorizer or similar
            feature_selector: Chi-square selector (optional)
            tokenizer: Keras tokenizer for DL models (optional)
            config: Training configuration used
            run_id: Optional pre-generated run ID
            custom_name: Optional custom name for the model
            
        Returns:
            Path to saved model directory
        """
        # Generate run ID if not provided
        if run_id is None:
            run_id = self.generate_run_id(custom_name)
        
        # Create model directory
        model_dir = self.base_dir / run_id
        model_dir.mkdir(parents=True, exist_ok=True)
        
        # Prepare metadata
        metadata = {
            'run_id': run_id,
            'model_name': model_name,
            'model_type': model_type,
            'saved_at': datetime.now().isoformat(),
            'metrics': metrics,
            'config': config,
            'artifacts': {
                'model': 'model.pkl',
                'vectorizer': 'vectorizer.pkl',
                'feature_selector': 'feature_selector.pkl' if feature_selector else None,
                'tokenizer': 'tokenizer.pkl' if tokenizer else None
            }
        }
        
        # Save model
        if model_type == 'deep_learning':
            # Save Keras model
            model_path = model_dir / 'model.h5'
            model.save(str(model_path))
            metadata['artifacts']['model'] = 'model.h5'
        else:
            # Save sklearn model
            model_path = model_dir / 'model.pkl'
            joblib.dump(model, model_path)
        
        # Save vectorizer
        vectorizer_path = model_dir / 'vectorizer.pkl'
        joblib.dump(vectorizer, vectorizer_path)
        
        # Save feature selector (if exists)
        if feature_selector is not None:
            selector_path = model_dir / 'feature_selector.pkl'
            joblib.dump(feature_selector, selector_path)
        
        # Save tokenizer (if exists)
        if tokenizer is not None:
            tokenizer_path = model_dir / 'tokenizer.pkl'
            with open(tokenizer_path, 'wb') as f:
                pickle.dump(tokenizer, f)
        
        # Save metadata
        metadata_path = model_dir / 'metadata.json'
        with open(metadata_path, 'w') as f:
            json.dump(metadata, f, indent=2)
        
        # Save config copy
        config_path = model_dir / 'training_config.json'
        with open(config_path, 'w') as f:
            json.dump(config, f, indent=2)
        
        print(f"\n{'='*80}")
        print(f"MODEL SAVED SUCCESSFULLY")
        print(f"{'='*80}")
        print(f"Run ID: {run_id}")
        print(f"Model: {model_name}")
        print(f"Location: {model_dir}")
        print(f"Metrics:")
        for key, value in metrics.items():
            print(f"  {key}: {value:.4f}")
        print(f"{'='*80}\n")
        
        return str(model_dir)
    
    def load_model(self, run_id: str) -> Dict[str, Any]:
        """
        Load a saved model with all artifacts.
        
        Args:
            run_id: Run ID of the model to load
            
        Returns:
            Dictionary containing model, vectorizer, metadata, etc.
        """
        model_dir = self.base_dir / run_id
        
        if not model_dir.exists():
            raise ValueError(f"Model directory not found: {model_dir}")
        
        # Load metadata
        metadata_path = model_dir / 'metadata.json'
        with open(metadata_path, 'r') as f:
            metadata = json.load(f)
        
        # Load model
        model_type = metadata['model_type']
        if model_type == 'deep_learning':
            from tensorflow.keras.models import load_model
            model_path = model_dir / 'model.h5'
            model = load_model(str(model_path))
        else:
            model_path = model_dir / 'model.pkl'
            model = joblib.load(model_path)
        
        # Load vectorizer
        vectorizer_path = model_dir / 'vectorizer.pkl'
        vectorizer = joblib.load(vectorizer_path)
        
        # Load feature selector (if exists)
        feature_selector = None
        selector_path = model_dir / 'feature_selector.pkl'
        if selector_path.exists():
            feature_selector = joblib.load(selector_path)
        
        # Load tokenizer (if exists)
        tokenizer = None
        tokenizer_path = model_dir / 'tokenizer.pkl'
        if tokenizer_path.exists():
            with open(tokenizer_path, 'rb') as f:
                tokenizer = pickle.load(f)
        
        # Load config
        config_path = model_dir / 'training_config.json'
        with open(config_path, 'r') as f:
            config = json.load(f)
        
        print(f"\n{'='*80}")
        print(f"MODEL LOADED SUCCESSFULLY")
        print(f"{'='*80}")
        print(f"Run ID: {run_id}")
        print(f"Model: {metadata['model_name']}")
        print(f"Saved at: {metadata['saved_at']}")
        print(f"Metrics:")
        for key, value in metadata['metrics'].items():
            print(f"  {key}: {value:.4f}")
        print(f"{'='*80}\n")
        
        return {
            'model': model,
            'vectorizer': vectorizer,
            'feature_selector': feature_selector,
            'tokenizer': tokenizer,
            'metadata': metadata,
            'config': config
        }
    
    def list_models(self) -> list:
        """
        List all saved models with metadata.
        
        Returns:
            List of dictionaries containing model information
        """
        models = []
        
        if not self.base_dir.exists():
            return models
        
        for model_dir in sorted(self.base_dir.iterdir(), key=lambda x: x.stat().st_mtime, reverse=True):
            if model_dir.is_dir():
                metadata_path = model_dir / 'metadata.json'
                if metadata_path.exists():
                    with open(metadata_path, 'r') as f:
                        metadata = json.load(f)
                    models.append({
                        'run_id': metadata['run_id'],
                        'model_name': metadata['model_name'],
                        'model_type': metadata['model_type'],
                        'saved_at': metadata['saved_at'],
                        'metrics': metadata['metrics'],
                        'path': str(model_dir)
                    })
        
        return models
    
    def delete_model(self, run_id: str) -> bool:
        """
        Delete a saved model.
        
        Args:
            run_id: Run ID of the model to delete
            
        Returns:
            True if deleted successfully, False otherwise
        """
        model_dir = self.base_dir / run_id
        
        if not model_dir.exists():
            print(f"Model directory not found: {model_dir}")
            return False
        
        import shutil
        shutil.rmtree(model_dir)
        print(f"Model deleted: {run_id}")
        return True
    
    def find_best_model(self, metric: str = 'f1_score') -> Optional[Dict[str, Any]]:
        """
        Find the best model based on a specific metric.
        
        Args:
            metric: Metric to use for comparison (default: 'f1_score')
            
        Returns:
            Dictionary containing best model info or None
        """
        models = self.list_models()
        
        if not models:
            return None
        
        # Find model with best metric
        best_model = None
        best_score = -1
        
        for model in models:
            if metric in model['metrics']:
                score = model['metrics'][metric]
                if score > best_score:
                    best_score = score
                    best_model = model
        
        return best_model


# Example usage function
def example_save_model():
    """Example of how to save a model."""
    from sklearn.ensemble import RandomForestClassifier
    from sklearn.feature_extraction.text import TfidfVectorizer
    
    # Train model (simplified example)
    model = RandomForestClassifier()
    vectorizer = TfidfVectorizer()
    
    # Save model
    persistence = ModelPersistence()
    model_dir = persistence.save_best_model(
        model=model,
        model_name='RandomForest',
        model_type='traditional_ml',
        metrics={
            'precision': 0.8532,
            'recall': 0.8421,
            'f1_score': 0.8476,
            'hamming_loss': 0.0532
        },
        vectorizer=vectorizer,
        feature_selector=None,
        tokenizer=None,
        config={'test_size': 0.2, 'random_state': 42},
        custom_name='RF_Best'
    )
    print(f"Model saved to: {model_dir}")


if __name__ == "__main__":
    # Example: List all saved models
    persistence = ModelPersistence()
    models = persistence.list_models()
    
    print(f"\nFound {len(models)} saved models:\n")
    for model in models:
        print(f"Run ID: {model['run_id']}")
        print(f"Model: {model['model_name']}")
        print(f"Saved: {model['saved_at']}")
        print(f"F1 Score: {model['metrics'].get('f1_score', 'N/A')}")
        print("-" * 40)
