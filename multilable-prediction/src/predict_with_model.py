"""
Prediction Script for Saved Multi-Label Classification Models

This script loads a saved model and makes predictions on new data,
supporting multiple input modes: row numbers, CSV files, and manual text input.
"""

import json
import sys
from pathlib import Path
from typing import List, Dict, Any, Optional, Union
import argparse

import pandas as pd
import numpy as np

# Local imports
from utils.model_persistence import ModelPersistence
from utils.config import LABELS

# TensorFlow imports - only loaded when needed for deep learning models
# Lazy import to avoid DLL errors on systems without proper TensorFlow setup
pad_sequences = None


class ModelPredictor:
    """Handle predictions using saved models."""
    
    def __init__(self, run_id: str):
        """
        Initialize predictor with a saved model.
        
        Args:
            run_id: Run ID of the saved model to use
        """
        self.persistence = ModelPersistence()
        self.model_artifacts = self.persistence.load_model(run_id)
        
        self.model = self.model_artifacts['model']
        self.vectorizer = self.model_artifacts['vectorizer']
        self.feature_selector = self.model_artifacts['feature_selector']
        self.tokenizer = self.model_artifacts['tokenizer']
        self.metadata = self.model_artifacts['metadata']
        self.config = self.model_artifacts['config']
        
        # Get label names from config or use default
        self.labels = self.config.get('labels', LABELS)
        
        # Determine model type
        self.model_type = self.metadata['model_type']
        self.model_name = self.metadata['model_name']
        
        print(f"Loaded model: {self.model_name} ({self.model_type})")
    
    def preprocess_text(self, texts: List[str]) -> Union[np.ndarray, np.ndarray]:
        """
        Preprocess text for prediction based on model type.
        
        Args:
            texts: List of text strings to preprocess
            
        Returns:
            Preprocessed features ready for prediction
        """
        if self.model_type == 'deep_learning':
            if 'CNN' in self.model_name:
                # Use tokenizer for CNN
                if self.tokenizer is None:
                    raise ValueError("Tokenizer not found for CNN model")
                
                # Lazy import TensorFlow only when needed
                global pad_sequences
                if pad_sequences is None:
                    try:
                        from tensorflow.keras.preprocessing.sequence import pad_sequences as _pad_sequences
                        pad_sequences = _pad_sequences
                    except ImportError as e:
                        raise ImportError(
                            "TensorFlow is required for CNN models but failed to load. "
                            "Please install Microsoft Visual C++ Redistributable and restart. "
                            "See: https://www.tensorflow.org/install/errors"
                        ) from e
                
                # Get max_len from config
                max_len = self.config.get('feature_engineering', {}).get('cnn', {}).get('max_len', 100)
                
                # Tokenize and pad
                sequences = self.tokenizer.texts_to_sequences(texts)
                X_padded = pad_sequences(sequences, maxlen=max_len, padding='post')
                return X_padded
            else:
                # MLP: Use TF-IDF
                X_tfidf = self.vectorizer.transform(texts)
                
                # Apply feature selection if exists (feature_selector is array of indices)
                if self.feature_selector is not None:
                    X_tfidf = X_tfidf[:, self.feature_selector]
                
                return X_tfidf.toarray()
        else:
            # Traditional ML: Use TF-IDF
            X_tfidf = self.vectorizer.transform(texts)
            
            # Apply feature selection if exists (feature_selector is array of indices)
            if self.feature_selector is not None:
                X_tfidf = X_tfidf[:, self.feature_selector]
            
            return X_tfidf
    
    def predict(self, texts: List[str], return_probabilities: bool = True) -> Dict[str, Any]:
        """
        Make predictions on new texts.
        
        Args:
            texts: List of defect report texts
            return_probabilities: Whether to return prediction probabilities
            
        Returns:
            Dictionary containing predictions and metadata
        """
        # Preprocess
        X_preprocessed = self.preprocess_text(texts)
        
        # Predict
        if return_probabilities and hasattr(self.model, 'predict_proba'):
            # For traditional ML models with predict_proba
            try:
                y_proba = self.model.predict_proba(X_preprocessed)
                # Convert to multi-label format
                predictions = (y_proba > 0.5).astype(int)
            except:
                # Fallback to predict
                predictions = self.model.predict(X_preprocessed)
                y_proba = predictions.astype(float)
        else:
            # For deep learning models or models without predict_proba
            y_proba = self.model.predict(X_preprocessed)
            predictions = (y_proba > 0.5).astype(int)
        
        # Format results
        results = {
            'model_name': self.model_name,
            'model_type': self.model_type,
            'run_id': self.metadata['run_id'],
            'num_samples': len(texts),
            'labels': self.labels,
            'predictions': []
        }
        
        for idx, text in enumerate(texts):
            sample_result = {
                'sample_index': idx,
                'text': text[:200] + '...' if len(text) > 200 else text,  # Truncate for display
                'predicted_labels': [],
                'confidence_scores': {}
            }
            
            # Get predicted labels and confidences
            for label_idx, label in enumerate(self.labels):
                label_name = label.replace('type_', '')
                
                is_predicted = predictions[idx][label_idx] == 1
                confidence = float(y_proba[idx][label_idx])
                
                sample_result['confidence_scores'][label_name] = confidence
                
                if is_predicted:
                    sample_result['predicted_labels'].append(label_name)
            
            # If no labels predicted, add "None" or find highest confidence
            if not sample_result['predicted_labels']:
                # Find label with highest confidence
                max_conf_idx = np.argmax(y_proba[idx])
                max_conf_label = self.labels[max_conf_idx].replace('type_', '')
                sample_result['predicted_labels'] = [f"{max_conf_label} (low confidence)"]
            
            results['predictions'].append(sample_result)
        
        return results
    
    def predict_from_csv(self, csv_path: str, text_column: str = 'report') -> Dict[str, Any]:
        """
        Predict from a CSV file.
        
        Args:
            csv_path: Path to CSV file
            text_column: Name of column containing text data
            
        Returns:
            Prediction results
        """
        df = pd.read_csv(csv_path)
        
        if text_column not in df.columns:
            raise ValueError(f"Column '{text_column}' not found in CSV")
        
        texts = df[text_column].astype(str).tolist()
        results = self.predict(texts)
        
        # Add original indices
        for idx, pred in enumerate(results['predictions']):
            pred['csv_row'] = idx
        
        results['source'] = f"CSV file: {csv_path}"
        results['total_rows'] = len(df)
        
        return results
    
    def predict_from_dataset_rows(
        self,
        dataset_path: str,
        row_numbers: List[int],
        text_column: str = 'report'
    ) -> Dict[str, Any]:
        """
        Predict specific rows from the original dataset.
        
        Args:
            dataset_path: Path to original dataset CSV
            row_numbers: List of row numbers (0-indexed)
            text_column: Name of column containing text data
            
        Returns:
            Prediction results
        """
        df = pd.read_csv(dataset_path)
        
        if text_column not in df.columns:
            raise ValueError(f"Column '{text_column}' not found in dataset")
        
        # Validate row numbers
        valid_rows = [r for r in row_numbers if 0 <= r < len(df)]
        if len(valid_rows) != len(row_numbers):
            invalid = [r for r in row_numbers if r not in valid_rows]
            print(f"Warning: Invalid row numbers (out of range): {invalid}")
        
        # Extract rows
        selected_df = df.iloc[valid_rows]
        texts = selected_df[text_column].astype(str).tolist()
        
        results = self.predict(texts)
        
        # Add dataset row numbers and ground truth if available
        label_columns = [col for col in df.columns if col.startswith('type_')]
        
        for idx, (pred, row_num) in enumerate(zip(results['predictions'], valid_rows)):
            pred['dataset_row'] = row_num
            
            # Add ground truth if labels exist
            if label_columns:
                ground_truth = []
                for col in label_columns:
                    if df.iloc[row_num][col] == 1:
                        ground_truth.append(col.replace('type_', ''))
                pred['ground_truth'] = ground_truth
                
                # Calculate accuracy for this sample
                predicted_set = set(pred['predicted_labels'])
                truth_set = set(ground_truth)
                
                if truth_set:
                    correct = len(predicted_set & truth_set)
                    pred['sample_accuracy'] = correct / len(truth_set) if truth_set else 0.0
                    pred['exact_match'] = predicted_set == truth_set
        
        results['source'] = f"Dataset rows: {valid_rows}"
        results['dataset_path'] = dataset_path
        
        return results
    
    def explain_prediction(self, text: str, top_n: int = 10) -> Dict[str, Any]:
        """
        Provide explanation for prediction (feature importance).
        
        Args:
            text: Input text
            top_n: Number of top features to show
            
        Returns:
            Dictionary with prediction and explanation
        """
        # Get prediction
        results = self.predict([text])
        prediction = results['predictions'][0]
        
        # Get feature importance (for traditional ML models)
        explanation = {
            'prediction': prediction,
            'top_features': []
        }
        
        if self.model_type == 'traditional_ml':
            # Transform text
            X_tfidf = self.vectorizer.transform([text])
            
            # Get feature names
            feature_names = self.vectorizer.get_feature_names_out()
            
            # Get non-zero features
            feature_indices = X_tfidf.nonzero()[1]
            feature_scores = X_tfidf.toarray()[0]
            
            # Sort by TF-IDF score
            sorted_indices = sorted(
                feature_indices,
                key=lambda idx: feature_scores[idx],
                reverse=True
            )[:top_n]
            
            explanation['top_features'] = [
                {
                    'feature': feature_names[idx],
                    'tfidf_score': float(feature_scores[idx])
                }
                for idx in sorted_indices
            ]
        
        return explanation


def parse_row_numbers(row_string: str) -> List[int]:
    """
    Parse row numbers from string (supports ranges).
    Examples: "1,5,10", "1-5,10,15-20"
    
    Args:
        row_string: String with row numbers
        
    Returns:
        List of row numbers
    """
    rows = []
    parts = row_string.split(',')
    
    for part in parts:
        part = part.strip()
        if '-' in part:
            # Range
            start, end = part.split('-')
            rows.extend(range(int(start), int(end) + 1))
        else:
            # Single number
            rows.append(int(part))
    
    return sorted(set(rows))  # Remove duplicates and sort


def main():
    """Main entry point for command-line usage."""
    parser = argparse.ArgumentParser(description='Make predictions using saved model')
    parser.add_argument('run_id', help='Run ID of the saved model')
    parser.add_argument('--mode', choices=['text', 'csv', 'rows'], required=True,
                        help='Prediction mode')
    parser.add_argument('--text', help='Text to predict (for text mode)')
    parser.add_argument('--csv', help='Path to CSV file (for csv mode)')
    parser.add_argument('--dataset', help='Path to original dataset (for rows mode)')
    parser.add_argument('--rows', help='Row numbers to predict (for rows mode), e.g., "1,5,10-15"')
    parser.add_argument('--output', help='Output file path for results (JSON)')
    parser.add_argument('--explain', action='store_true', help='Show prediction explanation')
    parser.add_argument('--json', action='store_true', help='Output results in JSON format')
    
    args = parser.parse_args()
    
    # Initialize predictor
    predictor = ModelPredictor(args.run_id)
    
    # Make predictions based on mode
    if args.mode == 'text':
        if not args.text:
            print("Error: --text required for text mode")
            return
        
        if args.explain:
            results = predictor.explain_prediction(args.text)
        else:
            results = predictor.predict([args.text])
    
    elif args.mode == 'csv':
        if not args.csv:
            print("Error: --csv required for csv mode")
            return
        results = predictor.predict_from_csv(args.csv)
    
    elif args.mode == 'rows':
        if not args.dataset or not args.rows:
            print("Error: --dataset and --rows required for rows mode")
            return
        
        row_numbers = parse_row_numbers(args.rows)
        results = predictor.predict_from_dataset_rows(args.dataset, row_numbers)
    
    # Output as JSON if requested
    if args.json:
        print("JSON_OUTPUT_START")
        print(json.dumps(results, indent=2, ensure_ascii=False))
        print("JSON_OUTPUT_END")
        return
    
    # Print results
    print(f"\n{'='*80}")
    print(f"PREDICTION RESULTS")
    print(f"{'='*80}")
    print(f"Model: {results['model_name']}")
    print(f"Run ID: {results['run_id']}")
    print(f"Number of samples: {results['num_samples']}")
    print(f"{'='*80}\n")
    
    for pred in results['predictions']:
        print(f"\nSample #{pred['sample_index'] + 1}:")
        if 'dataset_row' in pred:
            print(f"  Dataset Row: {pred['dataset_row']}")
        print(f"  Text: {pred['text']}")
        print(f"  Predicted Labels: {', '.join(pred['predicted_labels']) if pred['predicted_labels'] else 'None'}")
        print(f"  Confidence Scores:")
        for label, score in pred['confidence_scores'].items():
            print(f"    {label}: {score:.4f}")
        
        if 'ground_truth' in pred:
            print(f"  Ground Truth: {', '.join(pred['ground_truth']) if pred['ground_truth'] else 'None'}")
            print(f"  Exact Match: {pred.get('exact_match', False)}")
            print(f"  Sample Accuracy: {pred.get('sample_accuracy', 0):.4f}")
        
        if 'top_features' in pred.get('prediction', {}):
            print(f"  Top Features:")
            for feat in pred['prediction']['top_features'][:5]:
                print(f"    {feat['feature']}: {feat['tfidf_score']:.4f}")
        
        print("-" * 80)
    
    # Save to file if requested
    if args.output:
        with open(args.output, 'w', encoding='utf-8') as f:
            json.dump(results, f, indent=2, ensure_ascii=False)
        print(f"\nResults saved to: {args.output}")
    
    # Wait for user input before closing (only in non-JSON mode)
    if not args.json:
        print("\nPress any key to close...")
        try:
            import msvcrt
            msvcrt.getch()
        except:
            input()


if __name__ == "__main__":
    main()
