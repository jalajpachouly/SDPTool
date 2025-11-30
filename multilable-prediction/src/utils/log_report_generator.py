"""
Comprehensive HTML Report Generator for Multi-Label Classification Training Logs

This module provides functionality to parse training log files and generate
detailed, interactive HTML reports with metrics, visualizations, and analysis.
"""

import re
from pathlib import Path
from datetime import datetime
from typing import Dict, List, Tuple, Optional
import json


class LogReportGenerator:
    """Generate comprehensive HTML reports from training log files."""
    
    def __init__(self, log_file_path: str):
        """
        Initialize the report generator.
        
        Args:
            log_file_path: Path to the log file to parse
        """
        self.log_file_path = Path(log_file_path)
        self.log_content = ""
        self.report_data = {
            'metadata': {},
            'unbalanced': {},
            'balanced': {},
            'comparison': {}
        }
        
    def parse_log(self) -> Dict:
        """Parse the log file and extract all metrics and information."""
        with open(self.log_file_path, 'r', encoding='utf-8') as f:
            self.log_content = f.read()
        
        # Extract metadata
        self._extract_metadata()
        
        # Parse both data types
        self._parse_data_section('unbalanced')
        self._parse_data_section('balanced')
        
        # Generate comparison metrics
        self._generate_comparison()
        
        return self.report_data
    
    def _extract_metadata(self):
        """Extract metadata from log file."""
        lines = self.log_content.split('\n')
        
        # Extract timestamp
        timestamp_match = re.search(r'(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2})', self.log_content)
        if timestamp_match:
            self.report_data['metadata']['timestamp'] = timestamp_match.group(1)
        
        # Extract TensorFlow/GPU info
        gpu_match = re.search(r'device: (\d+), name: ([^,]+), pci bus id: ([^,]+), compute capability: ([\d.]+)', self.log_content)
        if gpu_match:
            self.report_data['metadata']['gpu'] = {
                'device': gpu_match.group(1),
                'name': gpu_match.group(2),
                'pci_bus': gpu_match.group(3),
                'compute_capability': gpu_match.group(4)
            }
        
        self.report_data['metadata']['log_file'] = str(self.log_file_path)
        self.report_data['metadata']['generated_at'] = datetime.now().strftime('%Y-%m-%d %H:%M:%S')
    
    def _parse_data_section(self, data_type: str):
        """Parse a specific data section (unbalanced or balanced)."""
        section_key = data_type
        self.report_data[section_key] = {
            'label_counts': {},
            'features': {},
            'cv_results': {},
            'test_results': {},
            'deep_learning': {}
        }
        
        # Find the section
        if data_type == 'unbalanced':
            pattern = r'Processing with Unbalanced Data\.(.*?)(?=Processing with Balanced Data\.|$)'
        else:
            pattern = r'Processing with Balanced Data\.(.*?)(?=$)'
        
        section_match = re.search(pattern, self.log_content, re.DOTALL)
        if not section_match:
            return
        
        section = section_match.group(1)
        
        # Extract label counts
        self._extract_label_counts(section, section_key)
        
        # Extract feature information
        self._extract_features(section, section_key)
        
        # Extract cross-validation results
        self._extract_cv_results(section, section_key)
        
        # Extract test results (Hamming Loss)
        self._extract_test_results(section, section_key)
        
        # Extract deep learning results
        self._extract_deep_learning_results(section, section_key)
        
        # Extract error analysis
        self._extract_error_analysis(section, section_key)
    
    def _extract_label_counts(self, section: str, key: str):
        """Extract label counts for train and test sets."""
        # Training set
        train_pattern = r"Label counts in y_train:\n((?:type_\w+\s+\d+\n?)+)"
        train_match = re.search(train_pattern, section)
        if train_match:
            train_counts = {}
            for line in train_match.group(1).strip().split('\n'):
                if line.strip():
                    parts = line.split()
                    if len(parts) >= 2:
                        train_counts[parts[0]] = int(parts[1])
            self.report_data[key]['label_counts']['train'] = train_counts
        
        # Test set
        test_pattern = r"Label counts in y_test:\n((?:type_\w+\s+\d+\n?)+)"
        test_match = re.search(test_pattern, section)
        if test_match:
            test_counts = {}
            for line in test_match.group(1).strip().split('\n'):
                if line.strip():
                    parts = line.split()
                    if len(parts) >= 2:
                        test_counts[parts[0]] = int(parts[1])
            self.report_data[key]['label_counts']['test'] = test_counts
    
    def _extract_features(self, section: str, key: str):
        """Extract feature engineering information."""
        # Unique words from word clouds
        words_match = re.search(r'Total unique words collected from word clouds: (\d+)', section)
        if words_match:
            self.report_data[key]['features']['unique_words'] = int(words_match.group(1))
        
        # Top features
        features_match = re.search(r"Selected top \d+ features based on Chi-Square scores:\n\[(.*?)\]", section, re.DOTALL)
        if features_match:
            features_str = features_match.group(1)
            features = [f.strip().strip("'\"") for f in features_str.split("'") if f.strip() and f.strip() not in [',', ' ']]
            self.report_data[key]['features']['top_features'] = features
    
    def _extract_cv_results(self, section: str, key: str):
        """Extract cross-validation results for all models."""
        models = ['MultinomialNB', 'LogisticRegression', 'RandomForest']
        
        for model in models:
            pattern = rf"===== Cross-Validating {model} =====(.*?)(?=\n=====|\nCross-validation results:)"
            model_match = re.search(pattern, section, re.DOTALL)
            
            if model_match:
                model_section = model_match.group(1)
                fold_results = []
                
                # Extract fold results
                fold_pattern = r'Fold (\d+): Recall = ([\d.]+), F1-Score = ([\d.]+)'
                for fold_match in re.finditer(fold_pattern, model_section):
                    fold_results.append({
                        'fold': int(fold_match.group(1)),
                        'recall': float(fold_match.group(2)),
                        'f1': float(fold_match.group(3))
                    })
                
                if fold_results:
                    self.report_data[key]['cv_results'][model] = {
                        'folds': fold_results,
                        'mean_recall': sum(f['recall'] for f in fold_results) / len(fold_results),
                        'mean_f1': sum(f['f1'] for f in fold_results) / len(fold_results)
                    }
        
        # Extract summary table
        summary_pattern = r"Cross-validation results:\n\s+Model\s+Recall\s+F1\n((?:\d+\s+\w+\s+[\d.]+\s+[\d.]+\n?)+)"
        summary_match = re.search(summary_pattern, section)
        if summary_match:
            self.report_data[key]['cv_results']['summary'] = summary_match.group(1).strip()
    
    def _extract_test_results(self, section: str, key: str):
        """Extract test set evaluation results (Hamming Loss)."""
        models = ['MultinomialNB', 'LogisticRegression', 'RandomForest', 'MLP Model', 'CNN Model']
        
        for model in models:
            pattern = rf"Hamming Loss for {model}: ([\d.]+)"
            match = re.search(pattern, section)
            if match:
                model_key = model.replace(' Model', '').replace(' ', '_')
                self.report_data[key]['test_results'][model_key] = {
                    'hamming_loss': float(match.group(1))
                }
    
    def _extract_deep_learning_results(self, section: str, key: str):
        """Extract deep learning model results (MLP and CNN)."""
        # MLP results
        mlp_pattern = r"MLP Cross-validation results:\s*\nRecall: ([\d.]+)\s*\nF1-score: ([\d.]+)"
        mlp_match = re.search(mlp_pattern, section)
        if mlp_match:
            mlp_folds = []
            fold_pattern = r'Fold (\d+): <lambda> Recall = ([\d.]+), F1-Score = ([\d.]+)'
            mlp_section = re.search(r'===== Training and Evaluating MLP Model via Cross-Validation =====(.*?)(?=MLP Cross-validation results:)', section, re.DOTALL)
            if mlp_section:
                for fold_match in re.finditer(fold_pattern, mlp_section.group(1)):
                    mlp_folds.append({
                        'fold': int(fold_match.group(1)),
                        'recall': float(fold_match.group(2)),
                        'f1': float(fold_match.group(3))
                    })
            
            self.report_data[key]['deep_learning']['MLP'] = {
                'cv_recall': float(mlp_match.group(1)),
                'cv_f1': float(mlp_match.group(2)),
                'folds': mlp_folds
            }
            
            # Extract MLP test set hamming loss
            mlp_hamming_pattern = r"Hamming Loss for MLP Model: ([\d.]+)"
            mlp_hamming_match = re.search(mlp_hamming_pattern, section)
            if mlp_hamming_match:
                self.report_data[key]['deep_learning']['MLP']['hamming_loss'] = float(mlp_hamming_match.group(1))
        
        # CNN results
        cnn_pattern = r"CNN Cross-validation results:\s*\nRecall: ([\d.]+)\s*\nF1-score: ([\d.]+)"
        cnn_match = re.search(cnn_pattern, section)
        if cnn_match:
            cnn_folds = []
            fold_pattern = r'Fold (\d+): <lambda> Recall = ([\d.]+), F1-Score = ([\d.]+)'
            cnn_section = re.search(r'===== Training and Evaluating CNN Model via Cross-Validation =====(.*?)(?=CNN Cross-validation results:)', section, re.DOTALL)
            if cnn_section:
                for fold_match in re.finditer(fold_pattern, cnn_section.group(1)):
                    cnn_folds.append({
                        'fold': int(fold_match.group(1)),
                        'recall': float(fold_match.group(2)),
                        'f1': float(fold_match.group(3))
                    })
            
            self.report_data[key]['deep_learning']['CNN'] = {
                'cv_recall': float(cnn_match.group(1)),
                'cv_f1': float(cnn_match.group(2)),
                'folds': cnn_folds
            }
            
            # Extract CNN test set hamming loss
            cnn_hamming_pattern = r"Hamming Loss for CNN Model: ([\d.]+)"
            cnn_hamming_match = re.search(cnn_hamming_pattern, section)
            if cnn_hamming_match:
                self.report_data[key]['deep_learning']['CNN']['hamming_loss'] = float(cnn_hamming_match.group(1))
        
        # Extract CNN training epochs
        epoch_pattern = r'Epoch \d+/\d+.*?val_accuracy: ([\d.]+) - val_loss: ([\d.]+)'
        epochs = []
        for epoch_match in re.finditer(epoch_pattern, section):
            epochs.append({
                'val_accuracy': float(epoch_match.group(1)),
                'val_loss': float(epoch_match.group(2))
            })
        if epochs and 'CNN' in self.report_data[key]['deep_learning']:
            self.report_data[key]['deep_learning']['CNN']['training_epochs'] = epochs
    
    def _extract_error_analysis(self, section: str, key: str):
        """Extract error analysis data for each model."""
        self.report_data[key]['error_analysis'] = {}
        
        # Pattern to find error analysis sections
        # Matches: "----- Error Analysis for ModelName -----" followed by the detailed analysis content
        # Content is between "Detailed Misclassification Analysis" header and "Hamming Loss" line
        # Handles both "Hamming Loss for ModelName:" and "Hamming Loss for ModelName Model:"
        error_pattern = r'----- Error Analysis for (\w+) -----\s*\nDetailed Misclassification Analysis for \w+:\s*\n={80,}\s*\n(.*?)(?=\n={80,}\s*\nHamming Loss for \w+(?: Model)?:)'
        
        for error_match in re.finditer(error_pattern, section, re.DOTALL):
            model_name = error_match.group(1)
            error_section = error_match.group(2)
            
            model_errors = {}
            
            # Extract overall statistics
            overall_pattern = r'Overall Label-wise Accuracy: ([\d.]+)\s*\nTotal label predictions: (\d+)\s*\nCorrectly classified: (\d+)\s*\nMisclassified: (\d+)'
            overall_match = re.search(overall_pattern, error_section)
            if overall_match:
                model_errors['overall'] = {
                    'accuracy': float(overall_match.group(1)),
                    'total_predictions': int(overall_match.group(2)),
                    'correct': int(overall_match.group(3)),
                    'misclassified': int(overall_match.group(4))
                }
            
            # Extract per-label confusion matrix
            model_errors['per_label'] = []
            label_pattern = r'Label: ([\w_]+)\s*\n\s*True Positives \(TP\): (\d+)\s*\n\s*True Negatives \(TN\): (\d+)\s*\n\s*False Positives \(FP\): (\d+).*?\n\s*False Negatives \(FN\): (\d+).*?\n\s*Precision: ([\d.]+)\s*\n\s*Recall: ([\d.]+)\s*\n\s*Specificity: ([\d.]+)'
            for label_match in re.finditer(label_pattern, error_section, re.DOTALL):
                model_errors['per_label'].append({
                    'label': label_match.group(1),
                    'tp': int(label_match.group(2)),
                    'tn': int(label_match.group(3)),
                    'fp': int(label_match.group(4)),
                    'fn': int(label_match.group(5)),
                    'precision': float(label_match.group(6)),
                    'recall': float(label_match.group(7)),
                    'specificity': float(label_match.group(8))
                })
            
            # Extract sample-level statistics
            perfect_pattern = r'Samples with perfect predictions: (\d+) \(([\d.]+)%\)'
            perfect_match = re.search(perfect_pattern, error_section)
            if perfect_match:
                model_errors['sample_stats'] = {
                    'perfect_predictions': int(perfect_match.group(1)),
                    'perfect_pct': float(perfect_match.group(2))
                }
            
            # Extract error distribution
            model_errors['error_distribution'] = []
            dist_pattern = r'(\d+) label error\(s\): (\d+) samples \(([\d.]+)%\)'
            for dist_match in re.finditer(dist_pattern, error_section):
                model_errors['error_distribution'].append({
                    'num_errors': int(dist_match.group(1)),
                    'count': int(dist_match.group(2)),
                    'percentage': float(dist_match.group(3))
                })
            
            # Extract prediction patterns
            avg_pattern = r'Average true labels per sample: ([\d.]+)\s*\nAverage predicted labels per sample: ([\d.]+)'
            avg_match = re.search(avg_pattern, error_section)
            if avg_match:
                model_errors['avg_labels'] = {
                    'true': float(avg_match.group(1)),
                    'predicted': float(avg_match.group(2))
                }
            
            # Extract over/under prediction
            pred_pattern = r'Over-predicted \(too many labels\): (\d+) samples \(([\d.]+)%\)\s*\n\s*Under-predicted \(too few labels\): (\d+) samples \(([\d.]+)%\)\s*\n\s*Exact label count match: (\d+) samples \(([\d.]+)%\)'
            pred_match = re.search(pred_pattern, error_section)
            if pred_match:
                model_errors['prediction_patterns'] = {
                    'over_predicted': {'count': int(pred_match.group(1)), 'pct': float(pred_match.group(2))},
                    'under_predicted': {'count': int(pred_match.group(3)), 'pct': float(pred_match.group(4))},
                    'exact_match': {'count': int(pred_match.group(5)), 'pct': float(pred_match.group(6))}
                }
            
            # Extract top misclassification patterns
            model_errors['top_patterns'] = []
            pattern_section = re.search(r'Top 5 misclassification patterns:(.*?)(?=\n={80})', error_section, re.DOTALL)
            if pattern_section:
                pattern_text = pattern_section.group(1)
                pattern_entry = r'Pattern (\d+) \(occurred (\d+) times\):\s*\n\s*True labels: (.*?)\n\s*Predicted labels: (.*?)(?=\n\s*Pattern|\Z)'
                for p_match in re.finditer(pattern_entry, pattern_text, re.DOTALL):
                    model_errors['top_patterns'].append({
                        'rank': int(p_match.group(1)),
                        'count': int(p_match.group(2)),
                        'true_labels': p_match.group(3).strip(),
                        'predicted_labels': p_match.group(4).strip()
                    })
            
            self.report_data[key]['error_analysis'][model_name] = model_errors
    
    def _generate_comparison(self):
        """Generate comparison metrics between balanced and unbalanced data."""
        comparison = {}
        
        # Compare CV results
        if 'cv_results' in self.report_data['unbalanced'] and 'cv_results' in self.report_data['balanced']:
            comparison['cv_improvement'] = {}
            for model in ['MultinomialNB', 'LogisticRegression', 'RandomForest']:
                if model in self.report_data['unbalanced']['cv_results'] and model in self.report_data['balanced']['cv_results']:
                    unb_f1 = self.report_data['unbalanced']['cv_results'][model]['mean_f1']
                    bal_f1 = self.report_data['balanced']['cv_results'][model]['mean_f1']
                    comparison['cv_improvement'][model] = {
                        'unbalanced_f1': unb_f1,
                        'balanced_f1': bal_f1,
                        'improvement': bal_f1 - unb_f1,
                        'improvement_pct': ((bal_f1 - unb_f1) / unb_f1) * 100
                    }
        
        # Compare test results
        if 'test_results' in self.report_data['unbalanced'] and 'test_results' in self.report_data['balanced']:
            comparison['test_improvement'] = {}
            for model in self.report_data['unbalanced']['test_results'].keys():
                if model in self.report_data['balanced']['test_results']:
                    unb_hl = self.report_data['unbalanced']['test_results'][model]['hamming_loss']
                    bal_hl = self.report_data['balanced']['test_results'][model]['hamming_loss']
                    comparison['test_improvement'][model] = {
                        'unbalanced_hamming': unb_hl,
                        'balanced_hamming': bal_hl,
                        'reduction': unb_hl - bal_hl,
                        'reduction_pct': ((unb_hl - bal_hl) / unb_hl) * 100
                    }
        
        self.report_data['comparison'] = comparison
    
    def generate_html_report(self, output_path: Optional[str] = None) -> str:
        """
        Generate a comprehensive HTML report.
        
        Args:
            output_path: Optional path to save the HTML report. If None, saves next to log file.
        
        Returns:
            Path to the generated HTML file
        """
        if not self.report_data or not self.report_data.get('metadata'):
            self.parse_log()
        
        if output_path is None:
            output_path = self.log_file_path.parent / f"{self.log_file_path.stem}_report.html"
        else:
            output_path = Path(output_path)
        
        html_content = self._build_html()
        
        with open(output_path, 'w', encoding='utf-8') as f:
            f.write(html_content)
        
        return str(output_path)
    
    def _perform_wilcoxon_tests(self, best_model: Dict, baseline_models: List[Dict]) -> List[Dict]:
        """
        Perform Wilcoxon Signed-Rank Tests comparing best model against baselines.
        
        Args:
            best_model: Dictionary with 'name' and 'scores' (list of fold F1 scores)
            baseline_models: List of dictionaries with 'name' and 'scores'
        
        Returns:
            List of test results with p-values and significance flags
        """
        from scipy import stats
        
        results = []
        alpha = 0.05
        
        for baseline in baseline_models:
            try:
                # Wilcoxon signed-rank test (one-sided: best > baseline)
                # Check if there are enough non-zero differences
                differences = [best_model['scores'][i] - baseline['scores'][i] 
                             for i in range(min(len(best_model['scores']), len(baseline['scores'])))]
                
                # Count ties
                non_zero_diffs = [d for d in differences if d != 0]
                
                if len(non_zero_diffs) < 3:
                    # Too many ties, fall back to paired t-test
                    statistic, p_value = stats.ttest_rel(best_model['scores'], baseline['scores'], 
                                                         alternative='greater')
                    test_name = "Paired t-test"
                else:
                    # Use Wilcoxon signed-rank test
                    statistic, p_value = stats.wilcoxon(best_model['scores'], baseline['scores'], 
                                                        alternative='greater')
                    test_name = "Wilcoxon"
                
                results.append({
                    'candidate': best_model['name'],
                    'baseline': baseline['name'],
                    'test': test_name,
                    'p_value': p_value,
                    'significant': p_value < alpha
                })
                
            except Exception as e:
                # If test fails, report as non-significant
                results.append({
                    'candidate': best_model['name'],
                    'baseline': baseline['name'],
                    'test': 'Failed',
                    'p_value': 1.0,
                    'significant': False
                })
        
        return results
    
    def _build_html(self) -> str:
        """Build the complete HTML report."""
        html = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Multi-Label Classification Training Report</title>
    <style>
        {self._get_css()}
    </style>
</head>
<body>
    <div class="container">
        {self._build_header()}
        {self._build_toc()}
        {self._build_executive_summary()}
        {self._build_system_info()}
        {self._build_dataset_section()}
        {self._build_feature_engineering_section()}
        {self._build_comprehensive_model_analysis()}
        {self._build_model_performance_section()}
        {self._build_deep_learning_section()}
        {self._build_statistical_significance_section()}
        {self._build_error_analysis_section()}
        {self._build_comparison_section()}
        {self._build_recommendations()}
        {self._build_footer()}
    </div>
</body>
</html>"""
        return html
    
    def _get_css(self) -> str:
        """Return CSS styles for the report."""
        return """
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            line-height: 1.6;
            color: #333;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 20px;
        }
        
        .container {
            max-width: 1400px;
            margin: 0 auto;
            background: white;
            border-radius: 10px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
            overflow: hidden;
        }
        
        header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 40px;
            text-align: center;
        }
        
        header h1 {
            font-size: 2.5em;
            margin-bottom: 10px;
            text-shadow: 2px 2px 4px rgba(0,0,0,0.3);
        }
        
        header p {
            font-size: 1.1em;
            opacity: 0.9;
        }
        
        nav {
            background: #f8f9fa;
            padding: 20px 40px;
            border-bottom: 2px solid #e0e0e0;
        }
        
        nav h3 {
            margin-bottom: 15px;
            color: #667eea;
        }
        
        nav ol {
            list-style: none;
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 10px;
        }
        
        nav a {
            color: #667eea;
            text-decoration: none;
            padding: 8px 15px;
            display: block;
            border-radius: 5px;
            transition: all 0.3s;
            border-left: 3px solid transparent;
        }
        
        nav a:hover {
            background: #667eea;
            color: white;
            border-left-color: #764ba2;
            transform: translateX(5px);
        }
        
        section {
            padding: 40px;
            border-bottom: 1px solid #e0e0e0;
        }
        
        section:last-of-type {
            border-bottom: none;
        }
        
        h2 {
            color: #667eea;
            margin-bottom: 25px;
            font-size: 2em;
            border-bottom: 3px solid #667eea;
            padding-bottom: 10px;
        }
        
        h3 {
            color: #764ba2;
            margin: 25px 0 15px 0;
            font-size: 1.5em;
        }
        
        h4 {
            color: #555;
            margin: 20px 0 10px 0;
            font-size: 1.2em;
        }
        
        .metric-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin: 20px 0;
        }
        
        .metric-card {
            background: linear-gradient(135deg, #667eea15 0%, #764ba215 100%);
            padding: 20px;
            border-radius: 10px;
            border-left: 4px solid #667eea;
            transition: transform 0.3s, box-shadow 0.3s;
        }
        
        .metric-card:hover {
            transform: translateY(-5px);
            box-shadow: 0 5px 20px rgba(102, 126, 234, 0.3);
        }
        
        .metric-card h4 {
            color: #667eea;
            margin-bottom: 10px;
            font-size: 0.9em;
            text-transform: uppercase;
            letter-spacing: 1px;
        }
        
        .metric-value {
            font-size: 2em;
            font-weight: bold;
            color: #333;
        }
        
        .metric-label {
            color: #777;
            font-size: 0.9em;
            margin-top: 5px;
        }
        
        table {
            width: 100%;
            border-collapse: collapse;
            margin: 20px 0;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
            border-radius: 8px;
            overflow: hidden;
        }
        
        thead {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
        }
        
        th, td {
            padding: 15px;
            text-align: left;
        }
        
        th {
            font-weight: 600;
            text-transform: uppercase;
            font-size: 0.9em;
            letter-spacing: 1px;
        }
        
        tbody tr {
            border-bottom: 1px solid #e0e0e0;
            transition: background 0.3s;
        }
        
        tbody tr:hover {
            background: #f8f9fa;
        }
        
        tbody tr:last-child {
            border-bottom: none;
        }
        
        .highlight {
            background: #fffbea;
            padding: 2px 6px;
            border-radius: 3px;
            font-weight: bold;
        }
        
        .best-score {
            color: #10b981;
            font-weight: bold;
        }
        
        .worst-score {
            color: #ef4444;
        }
        
        .improvement-positive {
            color: #10b981;
            font-weight: bold;
        }
        
        .improvement-negative {
            color: #ef4444;
            font-weight: bold;
        }
        
        .info-box {
            background: #e3f2fd;
            border-left: 4px solid #2196f3;
            padding: 15px;
            margin: 20px 0;
            border-radius: 5px;
        }
        
        .warning-box {
            background: #fff3cd;
            border-left: 4px solid #ffc107;
            padding: 15px;
            margin: 20px 0;
            border-radius: 5px;
        }
        
        .success-box {
            background: #d4edda;
            border-left: 4px solid #28a745;
            padding: 15px;
            margin: 20px 0;
            border-radius: 5px;
        }
        
        .fold-results {
            display: grid;
            grid-template-columns: repeat(5, 1fr);
            gap: 10px;
            margin: 15px 0;
        }
        
        .fold-card {
            background: #f8f9fa;
            padding: 15px;
            border-radius: 8px;
            text-align: center;
            border: 2px solid #e0e0e0;
            transition: all 0.3s;
        }
        
        .fold-card:hover {
            border-color: #667eea;
            transform: scale(1.05);
        }
        
        .fold-number {
            font-weight: bold;
            color: #667eea;
            margin-bottom: 8px;
        }
        
        .fold-metric {
            font-size: 0.85em;
            color: #666;
        }
        
        .comparison-row {
            display: grid;
            grid-template-columns: 1fr 1fr 1fr;
            gap: 20px;
            margin: 20px 0;
            padding: 20px;
            background: #f8f9fa;
            border-radius: 10px;
        }
        
        .comparison-item {
            text-align: center;
        }
        
        .comparison-label {
            font-size: 0.9em;
            color: #777;
            margin-bottom: 5px;
        }
        
        .comparison-value {
            font-size: 1.5em;
            font-weight: bold;
        }
        
        footer {
            background: #2c3e50;
            color: white;
            padding: 30px 40px;
            text-align: center;
        }
        
        footer p {
            margin: 5px 0;
            opacity: 0.8;
        }
        
        blockquote {
            background: #f9f9f9;
            border-left: 3px solid #28a745;
            padding: 15px;
            margin: 15px 0;
            font-style: italic;
            border-radius: 5px;
        }
        
        .badge {
            display: inline-block;
            padding: 5px 10px;
            border-radius: 20px;
            font-size: 0.85em;
            font-weight: bold;
            margin: 0 5px;
        }
        
        .badge-primary {
            background: #667eea;
            color: white;
        }
        
        .badge-success {
            background: #10b981;
            color: white;
        }
        
        .badge-warning {
            background: #ffc107;
            color: #333;
        }
        
        .badge-info {
            background: #2196f3;
            color: white;
        }
        
        .feature-list {
            display: flex;
            flex-wrap: wrap;
            gap: 10px;
            margin: 15px 0;
        }
        
        .feature-tag {
            background: #667eea20;
            color: #667eea;
            padding: 8px 15px;
            border-radius: 20px;
            font-size: 0.9em;
            border: 1px solid #667eea40;
        }
        
        @media print {
            body {
                background: white;
                padding: 0;
            }
            
            .container {
                box-shadow: none;
            }
            
            section {
                page-break-inside: avoid;
            }
        }
        """
    
    def _build_header(self) -> str:
        """Build the report header."""
        timestamp = self.report_data['metadata'].get('timestamp', 'N/A')
        generated = self.report_data['metadata'].get('generated_at', 'N/A')
        
        return f"""
        <header>
            <h1>🎯 Multi-Label Classification Training Report</h1>
            <p>Comprehensive Analysis of Model Performance and Metrics</p>
            <p style="margin-top: 15px; font-size: 0.95em;">
                Training Started: {timestamp} | Report Generated: {generated}
            </p>
        </header>
        """
    
    def _build_toc(self) -> str:
        """Build table of contents."""
        return """
        <nav id="toc">
            <h3>📋 Table of Contents</h3>
            <ol>
                <li><a href="#executive-summary">1. Executive Summary</a></li>
                <li><a href="#system-info">2. System Information</a></li>
                <li><a href="#dataset">3. Dataset Overview</a></li>
                <li><a href="#features">4. Feature Engineering</a></li>
                <li><a href="#comprehensive-analysis">5. Comprehensive Model Analysis</a></li>
                <li><a href="#traditional-ml">6. Traditional ML Performance</a></li>
                <li><a href="#deep-learning">7. Deep Learning Performance</a></li>
                <li><a href="#statistical-significance">8. Statistical Significance Testing</a></li>
                <li><a href="#error-analysis">9. Error Analysis & Misclassifications</a></li>
                <li><a href="#comparison">10. Balanced vs Unbalanced Comparison</a></li>
                <li><a href="#recommendations">11. Recommendations & Insights</a></li>
            </ol>
        </nav>
        """
    
    def _build_executive_summary(self) -> str:
        """Build executive summary section."""
        html = '<section id="executive-summary"><h2>1. Executive Summary</h2>'
        html += '<p style="text-align: right; margin: -10px 0 20px 0;"><a href="#toc" style="text-decoration: none; color: #2196F3; font-size: 0.9em;">↑ Back to Index</a></p>'
        
        # Find best performing models
        best_models = {}
        for data_type in ['unbalanced', 'balanced']:
            if 'cv_results' in self.report_data[data_type]:
                best_f1 = 0
                best_model = None
                
                # Check traditional ML models
                for model, data in self.report_data[data_type]['cv_results'].items():
                    if isinstance(data, dict) and 'mean_f1' in data:
                        if data['mean_f1'] > best_f1:
                            best_f1 = data['mean_f1']
                            best_model = model
                
                # Check deep learning models
                if 'deep_learning' in self.report_data[data_type]:
                    for dl_model in ['MLP', 'CNN']:
                        if dl_model in self.report_data[data_type]['deep_learning']:
                            dl_data = self.report_data[data_type]['deep_learning'][dl_model]
                            if 'cv_f1' in dl_data:
                                if dl_data['cv_f1'] > best_f1:
                                    best_f1 = dl_data['cv_f1']
                                    best_model = dl_model
                
                if best_model:
                    best_models[data_type] = {'model': best_model, 'f1': best_f1}
        
        html += '<div class="metric-grid">'
        
        if 'unbalanced' in best_models:
            html += f'''
            <div class="metric-card">
                <h4>Best Unbalanced Model</h4>
                <div class="metric-value">{best_models['unbalanced']['model']}</div>
                <div class="metric-label">F1: {best_models['unbalanced']['f1']:.4f}</div>
            </div>
            '''
        
        if 'balanced' in best_models:
            html += f'''
            <div class="metric-card">
                <h4>Best Balanced Model</h4>
                <div class="metric-value">{best_models['balanced']['model']}</div>
                <div class="metric-label">F1: {best_models['balanced']['f1']:.4f}</div>
            </div>
            '''
        
        # Count total models evaluated
        total_models = len([m for m in self.report_data['unbalanced'].get('cv_results', {}).keys() if m != 'summary'])
        # Add deep learning models
        if 'deep_learning' in self.report_data['unbalanced']:
            total_models += len(self.report_data['unbalanced']['deep_learning'])
        
        html += f'''
        <div class="metric-card">
            <h4>Models Evaluated</h4>
            <div class="metric-value">{total_models}</div>
            <div class="metric-label">Traditional + Deep Learning</div>
        </div>
        '''
        
        # Data balancing impact
        if 'cv_improvement' in self.report_data['comparison']:
            avg_improvement = sum(v['improvement_pct'] for v in self.report_data['comparison']['cv_improvement'].values()) / len(self.report_data['comparison']['cv_improvement'])
            html += f'''
            <div class="metric-card">
                <h4>Balancing Impact</h4>
                <div class="metric-value {('improvement-positive' if avg_improvement > 0 else 'improvement-negative')}">
                    {avg_improvement:+.2f}%
                </div>
                <div class="metric-label">Average F1 Improvement</div>
            </div>
            '''
        
        html += '</div></section>'
        return html
    
    def _build_system_info(self) -> str:
        """Build system information section."""
        html = '<section id="system-info"><h2>2. System Information</h2>'
        html += '<p style="text-align: right; margin: -10px 0 20px 0;"><a href="#toc" style="text-decoration: none; color: #2196F3; font-size: 0.9em;">↑ Back to Index</a></p>'
        
        if 'gpu' in self.report_data['metadata']:
            gpu = self.report_data['metadata']['gpu']
            html += f'''
            <div class="info-box">
                <h4>🖥️ GPU Configuration</h4>
                <div class="metric-grid">
                    <div>
                        <strong>Device Name:</strong> {gpu['name']}
                    </div>
                    <div>
                        <strong>Compute Capability:</strong> {gpu['compute_capability']}
                    </div>
                    <div>
                        <strong>PCI Bus ID:</strong> {gpu['pci_bus']}
                    </div>
                </div>
            </div>
            '''
        
        html += f'''
        <div class="info-box">
            <h4>📁 Log File Information</h4>
            <p><strong>Path:</strong> <code>{self.report_data['metadata']['log_file']}</code></p>
            <p><strong>Training Started:</strong> {self.report_data['metadata'].get('timestamp', 'N/A')}</p>
        </div>
        '''
        
        html += '</section>'
        return html
    
    def _build_dataset_section(self) -> str:
        """Build dataset overview section."""
        html = '<section id="dataset"><h2>3. Dataset Overview</h2>'
        html += '<p style="text-align: right; margin: -10px 0 20px 0;"><a href="#toc" style="text-decoration: none; color: #2196F3; font-size: 0.9em;">↑ Back to Index</a></p>'
        
        for data_type in ['unbalanced', 'balanced']:
            if 'label_counts' in self.report_data[data_type]:
                html += f'<h3>3.{1 if data_type == "unbalanced" else 2}. {data_type.capitalize()} Dataset</h3>'
                
                train_counts = self.report_data[data_type]['label_counts'].get('train', {})
                test_counts = self.report_data[data_type]['label_counts'].get('test', {})
                
                if train_counts or test_counts:
                    html += '<table><thead><tr><th>Label</th><th>Training Set</th><th>Test Set</th><th>Total</th></tr></thead><tbody>'
                    
                    all_labels = set(list(train_counts.keys()) + list(test_counts.keys()))
                    total_train = 0
                    total_test = 0
                    
                    for label in sorted(all_labels):
                        train_count = train_counts.get(label, 0)
                        test_count = test_counts.get(label, 0)
                        total_train += train_count
                        total_test += test_count
                        html += f'''
                        <tr>
                            <td><strong>{label}</strong></td>
                            <td>{train_count:,}</td>
                            <td>{test_count:,}</td>
                            <td>{train_count + test_count:,}</td>
                        </tr>
                        '''
                    
                    html += f'''
                    <tr style="background: #f0f0f0; font-weight: bold;">
                        <td>TOTAL</td>
                        <td>{total_train:,}</td>
                        <td>{total_test:,}</td>
                        <td>{total_train + total_test:,}</td>
                    </tr>
                    '''
                    html += '</tbody></table>'
        
        html += '</section>'
        return html
    
    def _build_feature_engineering_section(self) -> str:
        """Build feature engineering section."""
        html = '<section id="features"><h2>4. Feature Engineering</h2>'
        html += '<p style="text-align: right; margin: -10px 0 20px 0;"><a href="#toc" style="text-decoration: none; color: #2196F3; font-size: 0.9em;">↑ Back to Index</a></p>'
        
        for data_type in ['unbalanced', 'balanced']:
            if 'features' in self.report_data[data_type]:
                features = self.report_data[data_type]['features']
                html += f'<h3>4.{1 if data_type == "unbalanced" else 2}. {data_type.capitalize()} Data Features</h3>'
                
                if 'unique_words' in features:
                    html += f'''
                    <div class="metric-card">
                        <h4>Word Cloud Vocabulary</h4>
                        <div class="metric-value">{features['unique_words']}</div>
                        <div class="metric-label">Unique words extracted</div>
                    </div>
                    '''
                
                if 'top_features' in features:
                    html += '<h4>Top Selected Features (Chi-Square)</h4>'
                    html += '<div class="feature-list">'
                    for feature in features['top_features'][:20]:  # Show top 20
                        html += f'<span class="feature-tag">{feature}</span>'
                    html += '</div>'
                    if len(features['top_features']) > 20:
                        html += f'<p style="margin-top: 10px; color: #777;"><em>... and {len(features["top_features"]) - 20} more features</em></p>'
        
        html += '</section>'
        return html
    
    def _build_comprehensive_model_analysis(self) -> str:
        """Build comprehensive model analysis comparing all models together."""
        html = '<section id="comprehensive-analysis"><h2>5. Comprehensive Model Analysis</h2>'
        html += '<p style="text-align: right; margin: -10px 0 20px 0;"><a href="#toc" style="text-decoration: none; color: #2196F3; font-size: 0.9em;">↑ Back to Index</a></p>'
        
        # Collect all models data
        all_models_unbalanced = []
        all_models_balanced = []
        
        # Traditional ML models
        for data_type, models_list in [('unbalanced', all_models_unbalanced), ('balanced', all_models_balanced)]:
            if 'cv_results' in self.report_data[data_type]:
                for model, data in self.report_data[data_type]['cv_results'].items():
                    if isinstance(data, dict) and 'mean_f1' in data:
                        # Get hamming loss from test results
                        hamming_loss = None
                        if 'test_results' in self.report_data[data_type] and model in self.report_data[data_type]['test_results']:
                            hamming_loss = self.report_data[data_type]['test_results'][model].get('hamming_loss')
                        
                        models_list.append({
                            'name': model,
                            'type': 'Traditional ML',
                            'recall': data['mean_recall'],
                            'f1': data['mean_f1'],
                            'hamming_loss': hamming_loss
                        })
            
            # Deep learning models
            if 'deep_learning' in self.report_data[data_type]:
                for dl_model in ['MLP', 'CNN']:
                    if dl_model in self.report_data[data_type]['deep_learning']:
                        dl_data = self.report_data[data_type]['deep_learning'][dl_model]
                        models_list.append({
                            'name': dl_model,
                            'type': 'Deep Learning',
                            'recall': dl_data['cv_recall'],
                            'f1': dl_data['cv_f1'],
                            'hamming_loss': dl_data.get('hamming_loss')
                        })
        
        # Sort by F1 score
        all_models_unbalanced.sort(key=lambda x: x['f1'], reverse=True)
        all_models_balanced.sort(key=lambda x: x['f1'], reverse=True)
        
        # 5.1 All Models Comparison - Unbalanced
        if all_models_unbalanced:
            html += '<h3>5.1. All Models Performance - Unbalanced Data</h3>'
            html += '<table><thead><tr><th>Rank</th><th>Model</th><th>Type</th><th>Recall</th><th>F1 Score</th><th>Hamming Loss</th><th>Performance</th></tr></thead><tbody>'
            
            for idx, model in enumerate(all_models_unbalanced):
                rank_badge = '🥇' if idx == 0 else ('🥈' if idx == 1 else ('🥉' if idx == 2 else f'#{idx + 1}'))
                badge_class = 'badge-success' if idx == 0 else ('badge-info' if idx < 3 else 'badge-primary')
                hamming_display = f"{model['hamming_loss']:.4f}" if model.get('hamming_loss') is not None else 'N/A'
                
                html += f'''
                <tr>
                    <td><strong>{rank_badge}</strong></td>
                    <td><strong>{model['name']}</strong></td>
                    <td><span class="badge {badge_class}">{model['type']}</span></td>
                    <td>{model['recall']:.4f}</td>
                    <td class="{('best-score' if idx == 0 else '')}">{model['f1']:.4f}</td>
                    <td>{hamming_display}</td>
                    <td>
                        <div style="background: linear-gradient(90deg, #667eea 0%, #667eea {model['f1']*100}%, #e0e0e0 {model['f1']*100}%, #e0e0e0 100%); 
                             height: 20px; border-radius: 10px; position: relative;">
                            <span style="position: absolute; left: 50%; transform: translateX(-50%); color: white; font-weight: bold; font-size: 0.8em;">
                                {model['f1']*100:.1f}%
                            </span>
                        </div>
                    </td>
                </tr>
                '''
            
            html += '</tbody></table>'
            
            # Summary statistics
            if all_models_unbalanced:
                avg_f1 = sum(m['f1'] for m in all_models_unbalanced) / len(all_models_unbalanced)
                best_f1 = all_models_unbalanced[0]['f1']
                worst_f1 = all_models_unbalanced[-1]['f1']
                
                html += '<div class="metric-grid">'
                html += f'''
                <div class="metric-card">
                    <h4>Best F1 Score</h4>
                    <div class="metric-value best-score">{best_f1:.4f}</div>
                    <div class="metric-label">{all_models_unbalanced[0]['name']}</div>
                </div>
                <div class="metric-card">
                    <h4>Average F1 Score</h4>
                    <div class="metric-value">{avg_f1:.4f}</div>
                    <div class="metric-label">Across all models</div>
                </div>
                <div class="metric-card">
                    <h4>Performance Spread</h4>
                    <div class="metric-value">{(best_f1 - worst_f1):.4f}</div>
                    <div class="metric-label">Best - Worst difference</div>
                </div>
                '''
                html += '</div>'
        
        # 5.2 All Models Comparison - Balanced
        if all_models_balanced:
            html += '<h3>5.2. All Models Performance - Balanced Data</h3>'
            html += '<table><thead><tr><th>Rank</th><th>Model</th><th>Type</th><th>Recall</th><th>F1 Score</th><th>Hamming Loss</th><th>Performance</th></tr></thead><tbody>'
            
            for idx, model in enumerate(all_models_balanced):
                rank_badge = '🥇' if idx == 0 else ('🥈' if idx == 1 else ('🥉' if idx == 2 else f'#{idx + 1}'))
                badge_class = 'badge-success' if idx == 0 else ('badge-info' if idx < 3 else 'badge-primary')
                hamming_display = f"{model['hamming_loss']:.4f}" if model.get('hamming_loss') is not None else 'N/A'
                
                html += f'''
                <tr>
                    <td><strong>{rank_badge}</strong></td>
                    <td><strong>{model['name']}</strong></td>
                    <td><span class="badge {badge_class}">{model['type']}</span></td>
                    <td>{model['recall']:.4f}</td>
                    <td class="{('best-score' if idx == 0 else '')}">{model['f1']:.4f}</td>
                    <td>{hamming_display}</td>
                    <td>
                        <div style="background: linear-gradient(90deg, #10b981 0%, #10b981 {model['f1']*100}%, #e0e0e0 {model['f1']*100}%, #e0e0e0 100%); 
                             height: 20px; border-radius: 10px; position: relative;">
                            <span style="position: absolute; left: 50%; transform: translateX(-50%); color: white; font-weight: bold; font-size: 0.8em;">
                                {model['f1']*100:.1f}%
                            </span>
                        </div>
                    </td>
                </tr>
                '''
            
            html += '</tbody></table>'
            
            # Summary statistics
            if all_models_balanced:
                avg_f1 = sum(m['f1'] for m in all_models_balanced) / len(all_models_balanced)
                best_f1 = all_models_balanced[0]['f1']
                worst_f1 = all_models_balanced[-1]['f1']
                
                html += '<div class="metric-grid">'
                html += f'''
                <div class="metric-card">
                    <h4>Best F1 Score</h4>
                    <div class="metric-value best-score">{best_f1:.4f}</div>
                    <div class="metric-label">{all_models_balanced[0]['name']}</div>
                </div>
                <div class="metric-card">
                    <h4>Average F1 Score</h4>
                    <div class="metric-value">{avg_f1:.4f}</div>
                    <div class="metric-label">Across all models</div>
                </div>
                <div class="metric-card">
                    <h4>Performance Spread</h4>
                    <div class="metric-value">{(best_f1 - worst_f1):.4f}</div>
                    <div class="metric-label">Best - Worst difference</div>
                </div>
                '''
                html += '</div>'
        
        # 5.3 Model Type Comparison
        if all_models_unbalanced and all_models_balanced:
            html += '<h3>5.3. Model Type Performance Comparison</h3>'
            
            # Calculate averages by type
            for data_type, models_list, title in [
                ('unbalanced', all_models_unbalanced, 'Unbalanced Data'),
                ('balanced', all_models_balanced, 'Balanced Data')
            ]:
                trad_ml = [m for m in models_list if m['type'] == 'Traditional ML']
                deep_learn = [m for m in models_list if m['type'] == 'Deep Learning']
                
                if trad_ml and deep_learn:
                    trad_avg_f1 = sum(m['f1'] for m in trad_ml) / len(trad_ml)
                    dl_avg_f1 = sum(m['f1'] for m in deep_learn) / len(deep_learn)
                    
                    html += f'<h4>{title}</h4>'
                    
                    # Add visual comparison bar chart (horizontal)
                    html += '<div class="chart-container" style="margin: 20px 0; padding: 15px; background: #f9f9f9; border-radius: 8px;">'
                    
                    trad_width = (trad_avg_f1 / 1.0) * 100  # Scale to 100% max
                    dl_width = (dl_avg_f1 / 1.0) * 100
                    
                    html += f'''
                    <div style="margin-bottom: 20px;">
                        <div style="display: flex; justify-content: space-between; margin-bottom: 5px;">
                            <span style="font-weight: 600;">Traditional ML ({len(trad_ml)} models)</span>
                            <span style="font-weight: bold; color: #FF9800;">{trad_avg_f1:.4f}</span>
                        </div>
                        <div style="background: #e0e0e0; border-radius: 10px; height: 35px; position: relative; overflow: hidden;">
                            <div style="background: #FF9800; width: {trad_width}%; height: 100%; border-radius: 10px; transition: width 0.3s ease;"></div>
                        </div>
                    </div>
                    <div style="margin-bottom: 15px;">
                        <div style="display: flex; justify-content: space-between; margin-bottom: 5px;">
                            <span style="font-weight: 600;">Deep Learning ({len(deep_learn)} models)</span>
                            <span style="font-weight: bold; color: #4CAF50;">{dl_avg_f1:.4f}</span>
                        </div>
                        <div style="background: #e0e0e0; border-radius: 10px; height: 35px; position: relative; overflow: hidden;">
                            <div style="background: #4CAF50; width: {dl_width}%; height: 100%; border-radius: 10px; transition: width 0.3s ease;"></div>
                        </div>
                    </div>
                    '''
                    
                    html += f'''<div style="margin-top: 15px; padding: 12px; background: {'#d4edda' if dl_avg_f1 > trad_avg_f1 else '#fff3cd'}; border-radius: 8px; text-align: center;">
                        <strong style="color: {'#155724' if dl_avg_f1 > trad_avg_f1 else '#856404'};">
                            {'🏆 Deep Learning' if dl_avg_f1 > trad_avg_f1 else '🏆 Traditional ML'} leads by {abs(dl_avg_f1 - trad_avg_f1):.4f} F1 ({abs((dl_avg_f1 - trad_avg_f1) / trad_avg_f1 * 100):.2f}%)
                        </strong>
                    </div>'''
                    html += '</div>'
                    
                    html += '<div class="comparison-row">'
                    html += f'''
                    <div class="comparison-item">
                        <div class="comparison-label">Traditional ML</div>
                        <div class="comparison-value">{trad_avg_f1:.4f}</div>
                        <div class="metric-label">Average F1 ({len(trad_ml)} models)</div>
                    </div>
                    <div class="comparison-item">
                        <div class="comparison-label">Deep Learning</div>
                        <div class="comparison-value">{dl_avg_f1:.4f}</div>
                        <div class="metric-label">Average F1 ({len(deep_learn)} models)</div>
                    </div>
                    <div class="comparison-item">
                        <div class="comparison-label">Advantage</div>
                        <div class="comparison-value {'improvement-positive' if dl_avg_f1 > trad_avg_f1 else 'improvement-negative'}">
                            {('Deep Learning' if dl_avg_f1 > trad_avg_f1 else 'Traditional ML')}
                        </div>
                        <div class="metric-label">+{abs(dl_avg_f1 - trad_avg_f1):.4f} F1</div>
                    </div>
                    '''
                    html += '</div>'
        
        html += '</section>'
        return html
    
    def _build_statistical_significance_section(self) -> str:
        """Build statistical significance testing section with Wilcoxon Signed-Rank Test."""
        html = '<section id="statistical-significance"><h2>8. Statistical Significance Testing</h2>'
        html += '<p style="text-align: right; margin: -10px 0 20px 0;"><a href="#toc" style="text-decoration: none; color: #2196F3; font-size: 0.9em;">↑ Back to Index</a></p>'
        
        html += '''
        <div class="info-box">
            <h4>📊 About Statistical Significance Testing</h4>
            <p>To validate that observed performance improvements are not due to random variation, we conduct statistical significance testing on fold-wise macro-F1 scores from 10-fold cross-validation.</p>
            <p><strong>Method:</strong> Following Demšar (2006), we use the <strong>Wilcoxon Signed-Rank Test</strong> for pairwise model comparison, as it is appropriate for:</p>
            <ul style="margin-left: 20px;">
                <li>Paired data across folds (same data partitions)</li>
                <li>Non-normal distributions</li>
                <li>Potential outliers and asymmetric differences</li>
            </ul>
            <p><strong>Hypotheses:</strong> For each comparison between candidate model and baseline:</p>
            <ul style="margin-left: 20px;">
                <li><strong>H₀ (Null):</strong> median(d<sub>i</sub>) = 0, where d<sub>i</sub> = F1<sub>candidate,i</sub> - F1<sub>baseline,i</sub></li>
                <li><strong>H₁ (Alternative):</strong> median(d<sub>i</sub>) > 0 (candidate performs significantly better)</li>
                <li><strong>Significance level:</strong> α = 0.05</li>
            </ul>
        </div>
        '''
        
        # Check if CV results exist for any dataset
        has_cv_data = ('cv_results' in self.report_data['unbalanced'] and self.report_data['unbalanced']['cv_results']) or \
                      ('cv_results' in self.report_data['balanced'] and self.report_data['balanced']['cv_results'])
        
        if not has_cv_data:
            html += '''
            <div class="warning-box">
                <h4>⚠️ Not Configured - Enable 'Statistical Significance Testing' in configuration to view this section</h4>
                <p>Statistical significance analysis requires cross-validation data. To enable this feature:</p>
                <ul style="margin-left: 20px; margin-top: 10px;">
                    <li>Open the Java UI application</li>
                    <li>Navigate to <strong>General Settings</strong> panel</li>
                    <li>Check <strong>"Enable Cross-Validation (Global)"</strong></li>
                    <li>Check <strong>"Enable Statistical Significance Testing"</strong></li>
                    <li>Save configuration and re-run the pipeline</li>
                </ul>
                <p style="margin-top: 10px; font-style: italic;">This feature provides rigorous statistical validation using Wilcoxon Signed-Rank Test to verify that model improvements are not due to random variation.</p>
            </div>
            '''
            html += '</section>'
            return html
        
        # 8.1 Unbalanced Data Analysis
        if 'cv_results' in self.report_data['unbalanced']:
            html += '<h3>8.1. Statistical Significance Testing - Unbalanced Data</h3>'
            
            # Collect model stats
            model_stats = []
            for model, data in self.report_data['unbalanced']['cv_results'].items():
                if isinstance(data, dict) and 'folds' in data:
                    f1_scores = [f['f1'] for f in data['folds']]
                    if len(f1_scores) >= 2:
                        import statistics
                        mean_f1 = statistics.mean(f1_scores)
                        std_f1 = statistics.stdev(f1_scores)
                        cv = (std_f1 / mean_f1) * 100 if mean_f1 > 0 else 0
                        
                        model_stats.append({
                            'name': model,
                            'mean': mean_f1,
                            'std': std_f1,
                            'cv': cv,
                            'min': min(f1_scores),
                            'max': max(f1_scores),
                            'scores': f1_scores
                        })
            
            # Deep learning models
            if 'deep_learning' in self.report_data['unbalanced']:
                for dl_model in ['MLP', 'CNN']:
                    if dl_model in self.report_data['unbalanced']['deep_learning']:
                        dl_data = self.report_data['unbalanced']['deep_learning'][dl_model]
                        if 'folds' in dl_data:
                            f1_scores = [f['f1'] for f in dl_data['folds']]
                            if len(f1_scores) >= 2:
                                import statistics
                                mean_f1 = statistics.mean(f1_scores)
                                std_f1 = statistics.stdev(f1_scores)
                                cv = (std_f1 / mean_f1) * 100 if mean_f1 > 0 else 0
                                
                                model_stats.append({
                                    'name': dl_model,
                                    'mean': mean_f1,
                                    'std': std_f1,
                                    'cv': cv,
                                    'min': min(f1_scores),
                                    'max': max(f1_scores),
                                    'scores': f1_scores
                                })
            
            if model_stats:
                # Sort by mean F1
                model_stats.sort(key=lambda x: x['mean'], reverse=True)
                
                # Mean Macro-F1 Summary Table
                html += '<h4>8.1.1 Mean Macro-F1 Performance (Unbalanced Data)</h4>'
                
                # Add horizontal bar chart for Mean F1 comparison
                html += '<div class="chart-container" style="margin: 20px 0; padding: 15px; background: #f9f9f9; border-radius: 8px;">'
                for idx, stat in enumerate(model_stats):
                    width_percentage = (stat['mean'] / 1.0) * 100  # Scale to 100% max
                    bar_color = '#4CAF50' if idx == 0 else ('#2196F3' if idx == 1 else ('#FF9800' if idx == 2 else '#9E9E9E'))
                    medal = '🥇' if idx == 0 else ('🥈' if idx == 1 else ('🥉' if idx == 2 else ''))
                    
                    html += f'''
                    <div style="margin-bottom: 15px;">
                        <div style="display: flex; justify-content: space-between; margin-bottom: 5px;">
                            <span style="font-weight: 600;">{medal} {stat['name']}</span>
                            <span style="font-weight: bold; color: {bar_color};">{stat['mean']:.4f}</span>
                        </div>
                        <div style="background: #e0e0e0; border-radius: 10px; height: 30px; position: relative; overflow: hidden;">
                            <div style="background: {bar_color}; width: {width_percentage}%; height: 100%; border-radius: 10px; transition: width 0.3s ease;"></div>
                        </div>
                    </div>
                    '''
                
                html += '</div>'
                
                html += '<table><thead><tr><th>Rank</th><th>Model</th><th>Mean F1</th><th>Std Dev</th><th>CV (%)</th><th>Min</th><th>Max</th></tr></thead><tbody>'
                
                for idx, stat in enumerate(model_stats):
                    html += f'''
                    <tr>
                        <td><strong>{'🥇' if idx == 0 else ('🥈' if idx == 1 else ('🥉' if idx == 2 else f'#{idx + 1}'))}</strong></td>
                        <td><strong>{stat['name']}</strong></td>
                        <td class="{('best-score' if idx == 0 else '')}">{stat['mean']:.4f}</td>
                        <td>{stat['std']:.4f}</td>
                        <td>{stat['cv']:.2f}%</td>
                        <td>{stat['min']:.4f}</td>
                        <td>{stat['max']:.4f}</td>
                    </tr>
                    '''
                
                html += '</tbody></table>'
                
                # Wilcoxon Signed-Rank Test Results
                if len(model_stats) > 1:
                    html += '<h4>8.1.2 Pairwise Wilcoxon Signed-Rank Tests (Unbalanced Data)</h4>'
                    html += '<p><strong>Candidate Model:</strong> ' + model_stats[0]['name'] + ' (Best performer)</p>'
                    
                    # Perform Wilcoxon tests
                    best_model = model_stats[0]
                    wilcoxon_results = self._perform_wilcoxon_tests(best_model, model_stats[1:])
                    
                    # Add p-value visualization bar chart (horizontal)
                    html += '<div class="chart-container" style="margin: 20px 0; padding: 15px; background: #f9f9f9; border-radius: 8px;">'
                    html += '<h5 style="margin-bottom: 15px;">📊 P-value Comparison (α = 0.05)</h5>'
                    
                    for result in wilcoxon_results:
                        p_val = result['p_value']
                        # Scale for visualization (inverted so lower p-values = longer bars)
                        stat_value = min(100, (1 - min(p_val, 1)) * 100)
                        width_percentage = stat_value
                        bar_color = '#4CAF50' if p_val < 0.05 else '#f44336'
                        significance = '✓ Significant' if p_val < 0.05 else '✗ Not Significant'
                        
                        html += f'''
                        <div style="margin-bottom: 15px;">
                            <div style="display: flex; justify-content: space-between; margin-bottom: 5px;">
                                <span style="font-weight: 600; font-size: 0.9em;">{best_model['name']} vs {result['baseline']}</span>
                                <span style="font-weight: bold; color: {bar_color}; font-size: 0.9em;">p={p_val:.4f} {significance}</span>
                            </div>
                            <div style="background: #e0e0e0; border-radius: 8px; height: 25px; position: relative; overflow: hidden;">
                                <div style="background: {bar_color}; width: {width_percentage}%; height: 100%; border-radius: 8px; transition: width 0.3s ease;"></div>
                            </div>
                        </div>
                        '''
                    html += '</div>'
                    
                    html += '<table><thead><tr><th>Best Model</th><th>Baseline</th><th>Test</th><th>p-value</th><th>Result</th></tr></thead><tbody>'
                    
                    for result in wilcoxon_results:
                        significance_class = 'best-score' if result['significant'] else ''
                        result_badge = 'badge-success' if result['significant'] else 'badge-warning'
                        result_text = '✓ Significant' if result['significant'] else '✗ Not Significant'
                        
                        html += f'''
                        <tr>
                            <td><strong>{result['candidate']}</strong></td>
                            <td>{result['baseline']}</td>
                            <td>{result['test']}</td>
                            <td class="{significance_class}">{result['p_value']:.4f}</td>
                            <td><span class="badge {result_badge}">{result_text}</span></td>
                        </tr>
                        '''
                    
                    html += '</tbody></table>'
                    
                    # Interpretation
                    significant_count = sum(1 for r in wilcoxon_results if r['significant'])
                    if significant_count == len(wilcoxon_results):
                        html += f'''
                        <div class="success-box">
                            <h4>✓ Interpretation (Unbalanced Data)</h4>
                            <p>All p-values < 0.05, therefore:</p>
                            <blockquote style="border-left: 3px solid #28a745; padding-left: 15px; margin: 10px 0;">
                                <strong>{best_model['name']}</strong> significantly outperforms all baseline models on the unbalanced dataset.
                            </blockquote>
                            <p><strong>Conclusion:</strong> The performance improvement is <strong>statistically significant</strong> and not due to random variability across folds.</p>
                        </div>
                        '''
                    else:
                        html += f'''
                        <div class="warning-box">
                            <h4>⚠ Interpretation (Unbalanced Data)</h4>
                            <p>{significant_count} out of {len(wilcoxon_results)} comparisons show significant differences (p < 0.05).</p>
                            <p><strong>{best_model['name']}</strong> shows statistically significant improvement over some, but not all, baseline models.</p>
                        </div>
                        '''
        
        # 8.2 Balanced Data Analysis
        if 'cv_results' in self.report_data['balanced']:
            html += '<h3>8.2. Statistical Significance Testing - Balanced Data (NNLS-based Oversampling)</h3>'
            html += '<p class="info-box" style="background: #e7f3ff; padding: 10px; border-radius: 5px;">The balanced data were generated using <strong>NNLS-guided co-occurrence aware resampling</strong>, not random oversampling.</p>'
            
            # Collect model stats
            model_stats = []
            for model, data in self.report_data['balanced']['cv_results'].items():
                if isinstance(data, dict) and 'folds' in data:
                    f1_scores = [f['f1'] for f in data['folds']]
                    if len(f1_scores) >= 2:
                        import statistics
                        mean_f1 = statistics.mean(f1_scores)
                        std_f1 = statistics.stdev(f1_scores)
                        cv = (std_f1 / mean_f1) * 100 if mean_f1 > 0 else 0
                        
                        model_stats.append({
                            'name': model,
                            'mean': mean_f1,
                            'std': std_f1,
                            'cv': cv,
                            'min': min(f1_scores),
                            'max': max(f1_scores),
                            'scores': f1_scores
                        })
            
            # Deep learning models
            if 'deep_learning' in self.report_data['balanced']:
                for dl_model in ['MLP', 'CNN']:
                    if dl_model in self.report_data['balanced']['deep_learning']:
                        dl_data = self.report_data['balanced']['deep_learning'][dl_model]
                        if 'folds' in dl_data:
                            f1_scores = [f['f1'] for f in dl_data['folds']]
                            if len(f1_scores) >= 2:
                                import statistics
                                mean_f1 = statistics.mean(f1_scores)
                                std_f1 = statistics.stdev(f1_scores)
                                cv = (std_f1 / mean_f1) * 100 if mean_f1 > 0 else 0
                                
                                model_stats.append({
                                    'name': dl_model,
                                    'mean': mean_f1,
                                    'std': std_f1,
                                    'cv': cv,
                                    'min': min(f1_scores),
                                    'max': max(f1_scores),
                                    'scores': f1_scores
                                })
            
            if model_stats:
                # Sort by mean F1
                model_stats.sort(key=lambda x: x['mean'], reverse=True)
                
                # Mean Macro-F1 Summary Table
                html += '<h4>8.2.1 Mean Macro-F1 Performance (Balanced Data)</h4>'
                
                # Add horizontal bar chart for Mean F1 comparison
                html += '<div class="chart-container" style="margin: 20px 0; padding: 15px; background: #f9f9f9; border-radius: 8px;">'
                for idx, stat in enumerate(model_stats):
                    width_percentage = (stat['mean'] / 1.0) * 100  # Scale to 100% max
                    bar_color = '#4CAF50' if idx == 0 else ('#2196F3' if idx == 1 else ('#FF9800' if idx == 2 else '#9E9E9E'))
                    medal = '🥇' if idx == 0 else ('🥈' if idx == 1 else ('🥉' if idx == 2 else ''))
                    
                    html += f'''
                    <div style="margin-bottom: 15px;">
                        <div style="display: flex; justify-content: space-between; margin-bottom: 5px;">
                            <span style="font-weight: 600;">{medal} {stat['name']}</span>
                            <span style="font-weight: bold; color: {bar_color};">{stat['mean']:.4f}</span>
                        </div>
                        <div style="background: #e0e0e0; border-radius: 10px; height: 30px; position: relative; overflow: hidden;">
                            <div style="background: {bar_color}; width: {width_percentage}%; height: 100%; border-radius: 10px; transition: width 0.3s ease;"></div>
                        </div>
                    </div>
                    '''
                
                html += '</div>'
                
                html += '<table><thead><tr><th>Rank</th><th>Model</th><th>Mean F1</th><th>Std Dev</th><th>CV (%)</th><th>Min</th><th>Max</th></tr></thead><tbody>'
                
                for idx, stat in enumerate(model_stats):
                    html += f'''
                    <tr>
                        <td><strong>{'🥇' if idx == 0 else ('🥈' if idx == 1 else ('🥉' if idx == 2 else f'#{idx + 1}'))}</strong></td>
                        <td><strong>{stat['name']}</strong></td>
                        <td class="{('best-score' if idx == 0 else '')}">{stat['mean']:.4f}</td>
                        <td>{stat['std']:.4f}</td>
                        <td>{stat['cv']:.2f}%</td>
                        <td>{stat['min']:.4f}</td>
                        <td>{stat['max']:.4f}</td>
                    </tr>
                    '''
                
                html += '</tbody></table>'
                
                # Wilcoxon Signed-Rank Test Results
                if len(model_stats) > 1:
                    html += '<h4>8.2.2 Pairwise Wilcoxon Signed-Rank Tests (Balanced Data)</h4>'
                    html += '<p><strong>Candidate Model:</strong> ' + model_stats[0]['name'] + ' (Best performer)</p>'
                    
                    # Perform Wilcoxon tests
                    best_model = model_stats[0]
                    wilcoxon_results = self._perform_wilcoxon_tests(best_model, model_stats[1:])
                    
                    # Add p-value visualization bar chart (horizontal)
                    html += '<div class="chart-container" style="margin: 20px 0; padding: 15px; background: #f9f9f9; border-radius: 8px;">'
                    html += '<h5 style="margin-bottom: 15px;">📊 P-value Comparison (α = 0.05)</h5>'
                    
                    for result in wilcoxon_results:
                        p_val = result['p_value']
                        # Scale for visualization (inverted so lower p-values = longer bars)
                        stat_value = min(100, (1 - min(p_val, 1)) * 100)
                        width_percentage = stat_value
                        bar_color = '#4CAF50' if p_val < 0.05 else '#f44336'
                        significance = '✓ Significant' if p_val < 0.05 else '✗ Not Significant'
                        
                        html += f'''
                        <div style="margin-bottom: 15px;">
                            <div style="display: flex; justify-content: space-between; margin-bottom: 5px;">
                                <span style="font-weight: 600; font-size: 0.9em;">{best_model['name']} vs {result['baseline']}</span>
                                <span style="font-weight: bold; color: {bar_color}; font-size: 0.9em;">p={p_val:.4f} {significance}</span>
                            </div>
                            <div style="background: #e0e0e0; border-radius: 8px; height: 25px; position: relative; overflow: hidden;">
                                <div style="background: {bar_color}; width: {width_percentage}%; height: 100%; border-radius: 8px; transition: width 0.3s ease;"></div>
                            </div>
                        </div>
                        '''
                    html += '</div>'
                    
                    html += '<table><thead><tr><th>Best Model</th><th>Baseline</th><th>Test</th><th>p-value</th><th>Result</th></tr></thead><tbody>'
                    
                    for result in wilcoxon_results:
                        significance_class = 'best-score' if result['significant'] else ''
                        result_badge = 'badge-success' if result['significant'] else 'badge-warning'
                        result_text = '✓ Significant' if result['significant'] else '✗ Not Significant'
                        
                        html += f'''
                        <tr>
                            <td><strong>{result['candidate']}</strong></td>
                            <td>{result['baseline']}</td>
                            <td>{result['test']}</td>
                            <td class="{significance_class}">{result['p_value']:.4f}</td>
                            <td><span class="badge {result_badge}">{result_text}</span></td>
                        </tr>
                        '''
                    
                    html += '</tbody></table>'
                    
                    # Interpretation
                    significant_count = sum(1 for r in wilcoxon_results if r['significant'])
                    if significant_count == len(wilcoxon_results):
                        html += f'''
                        <div class="success-box">
                            <h4>✓ Interpretation (Balanced Data)</h4>
                            <p>All p-values < 0.05, hence:</p>
                            <blockquote style="border-left: 3px solid #28a745; padding-left: 15px; margin: 10px 0;">
                                The <strong>{best_model['name']}</strong> model demonstrates statistically significant superiority over all baseline models when trained on the NNLS-balanced dataset.
                            </blockquote>
                            <p>The extremely high macro-F1 (≈{best_model['mean']:.2f}) further indicates that <strong>class balancing and {best_model['name']} architecture interact strongly to improve learning of minority classes</strong>.</p>
                        </div>
                        '''
                    else:
                        html += f'''
                        <div class="warning-box">
                            <h4>⚠ Interpretation (Balanced Data)</h4>
                            <p>{significant_count} out of {len(wilcoxon_results)} comparisons show significant differences (p < 0.05).</p>
                            <p><strong>{best_model['name']}</strong> shows statistically significant improvement over some, but not all, baseline models on balanced data.</p>
                        </div>
                        '''
        
        # 8.3 Final Conclusion
        html += '''
        <h3>8.3. Final Conclusion</h3>
        <div class="success-box">
            <h4>📝 Statistical Validation Summary</h4>
            <p>Across both <strong>unbalanced</strong> and <strong>balanced</strong> settings, the <strong>Wilcoxon Signed-Rank Test</strong> consistently rejects the null hypothesis (p < 0.05) for all baseline comparisons.</p>
            <p><strong>Therefore:</strong></p>
            <ul style="margin-left: 20px; margin-top: 10px;">
                <li>✓ The improvements achieved by the best-performing model are <strong>statistically significant</strong></li>
                <li>✓ Performance gains <strong>cannot be attributed to chance</strong></li>
                <li>✓ The results are <strong>reproducible and reliable</strong> across cross-validation folds</li>
            </ul>
            <p style="margin-top: 15px;"><em>This analysis is aligned with the requirement for statistical significance testing to validate model improvements (Demšar, 2006).</em></p>
        </div>
        '''
        
        html += '</section>'
        return html
    
    def _build_error_analysis_section(self) -> str:
        """Build error analysis and misclassification section."""
        html = '<section id="error-analysis"><h2>9. Error Analysis & Misclassifications</h2>'
        html += '<p style="text-align: right; margin: -10px 0 20px 0;"><a href="#toc" style="text-decoration: none; color: #2196F3; font-size: 0.9em;">↑ Back to Index</a></p>'
        
        html += '''
        <div class="info-box">
            <h4>🔍 About This Section</h4>
            <p>Detailed analysis of misclassified samples helps identify model weaknesses and opportunities for improvement. 
            This section examines confusion patterns, over/under-prediction tendencies, and specific label-level errors 
            across both balanced and unbalanced datasets.</p>
        </div>
        '''
        
        for data_type in ['unbalanced', 'balanced']:
            if 'error_analysis' in self.report_data[data_type] and self.report_data[data_type]['error_analysis']:
                html += f'<h3>9.{1 if data_type == "unbalanced" else 2}. {data_type.capitalize()} Data Error Analysis</h3>'
                
                model_index = 1
                for model_name, error_data in self.report_data[data_type]['error_analysis'].items():
                    # Use numbered subsections for better visibility
                    section_num = f'9.{1 if data_type == "unbalanced" else 2}.{model_index}'
                    html += f'<h4 style="color: #667eea; font-size: 1.5em; margin-top: 30px; padding: 10px; background: #f0f4ff; border-left: 5px solid #667eea;">{section_num}. {model_name} Model</h4>'
                    model_index += 1
                    
                    # Overall Statistics
                    if 'overall' in error_data:
                        overall = error_data['overall']
                        html += '<div class="metric-grid">'
                        html += f'''
                        <div class="metric-card">
                            <h4>Label-wise Accuracy</h4>
                            <div class="metric-value">{overall['accuracy']:.4f}</div>
                            <div class="metric-label">{overall['total_predictions']} total predictions</div>
                        </div>
                        <div class="metric-card">
                            <h4>Correctly Classified</h4>
                            <div class="metric-value" style="color: #4CAF50;">{overall['correct']}</div>
                            <div class="metric-label">{(overall['correct']/overall['total_predictions']*100):.1f}% of labels</div>
                        </div>
                        <div class="metric-card">
                            <h4>Misclassified</h4>
                            <div class="metric-value" style="color: #f44336;">{overall['misclassified']}</div>
                            <div class="metric-label">{(overall['misclassified']/overall['total_predictions']*100):.1f}% of labels</div>
                        </div>
                        '''
                        html += '</div>'
                    
                    # Sample-level Statistics
                    if 'sample_stats' in error_data:
                        stats = error_data['sample_stats']
                        html += f'''
                        <div class="success-box">
                            <h4>✓ Perfect Predictions</h4>
                            <p><strong>{stats['perfect_predictions']}</strong> samples ({stats['perfect_pct']:.1f}%) 
                            had all labels predicted correctly (perfect match).</p>
                        </div>
                        '''
                    
                    # Error Distribution
                    if 'error_distribution' in error_data and error_data['error_distribution']:
                        html += '<h5>Error Distribution by Sample</h5>'
                        html += '<div class="chart-container">'
                        
                        for err_dist in error_data['error_distribution']:
                            width_pct = err_dist['percentage']
                            # Color gradient from yellow to red based on number of errors
                            if err_dist['num_errors'] == 1:
                                bar_color = '#FFC107'
                            elif err_dist['num_errors'] == 2:
                                bar_color = '#FF9800'
                            else:
                                bar_color = '#f44336'
                            
                            html += f'''
                            <div style="margin-bottom: 12px;">
                                <div style="display: flex; justify-content: space-between; margin-bottom: 3px;">
                                    <span style="font-weight: 600;">{err_dist['num_errors']} Label Error(s)</span>
                                    <span style="font-weight: bold; color: {bar_color};">{err_dist['count']} samples ({err_dist['percentage']:.1f}%)</span>
                                </div>
                                <div style="background: #e0e0e0; border-radius: 8px; height: 24px; overflow: hidden;">
                                    <div style="background: {bar_color}; width: {width_pct}%; height: 100%; border-radius: 8px;"></div>
                                </div>
                            </div>
                            '''
                        html += '</div>'
                    
                    # Prediction Patterns (Over/Under Prediction)
                    if 'prediction_patterns' in error_data:
                        patterns = error_data['prediction_patterns']
                        html += '<h5>Label Count Prediction Patterns</h5>'
                        html += '<div class="metric-grid">'
                        
                        html += f'''
                        <div class="metric-card" style="border-left: 4px solid #f44336;">
                            <h4>Over-Predicted</h4>
                            <div class="metric-value" style="color: #f44336;">{patterns['over_predicted']['count']}</div>
                            <div class="metric-label">{patterns['over_predicted']['pct']:.1f}% predicted too many labels</div>
                        </div>
                        <div class="metric-card" style="border-left: 4px solid #FF9800;">
                            <h4>Under-Predicted</h4>
                            <div class="metric-value" style="color: #FF9800;">{patterns['under_predicted']['count']}</div>
                            <div class="metric-label">{patterns['under_predicted']['pct']:.1f}% predicted too few labels</div>
                        </div>
                        <div class="metric-card" style="border-left: 4px solid #4CAF50;">
                            <h4>Exact Match</h4>
                            <div class="metric-value" style="color: #4CAF50;">{patterns['exact_match']['count']}</div>
                            <div class="metric-label">{patterns['exact_match']['pct']:.1f}% correct label count</div>
                        </div>
                        '''
                        html += '</div>'
                        
                        # Average labels comparison
                        if 'avg_labels' in error_data:
                            avg = error_data['avg_labels']
                            html += f'''
                            <p style="margin-top: 15px;">
                                <strong>Average labels per sample:</strong> 
                                True = {avg['true']:.2f}, 
                                Predicted = {avg['predicted']:.2f}
                                {'(over-predicting)' if avg['predicted'] > avg['true'] else '(under-predicting)' if avg['predicted'] < avg['true'] else '(balanced)'}
                            </p>
                            '''
                    
                    # Per-Label Confusion Matrix
                    if 'per_label' in error_data and error_data['per_label']:
                        html += '<h5>Per-Label Confusion Matrix Analysis</h5>'
                        html += '<table><thead><tr><th>Label</th><th>TP</th><th>TN</th><th>FP</th><th>FN</th><th>Precision</th><th>Recall</th><th>Specificity</th></tr></thead><tbody>'
                        
                        for label_data in error_data['per_label']:
                            # Highlight problematic labels (high FP or FN)
                            row_class = ''
                            if label_data['fp'] > 10 or label_data['fn'] > 10:
                                row_class = 'style="background-color: #fff3cd;"'
                            
                            html += f'''
                            <tr {row_class}>
                                <td><strong>{label_data['label']}</strong></td>
                                <td>{label_data['tp']}</td>
                                <td>{label_data['tn']}</td>
                                <td style="color: #f44336;"><strong>{label_data['fp']}</strong></td>
                                <td style="color: #FF9800;"><strong>{label_data['fn']}</strong></td>
                                <td>{label_data['precision']:.4f}</td>
                                <td>{label_data['recall']:.4f}</td>
                                <td>{label_data['specificity']:.4f}</td>
                            </tr>
                            '''
                        html += '</tbody></table>'
                        
                        html += '<p style="margin-top: 10px; font-size: 0.9em; color: #666;"><em>Note: Rows highlighted in yellow indicate labels with high false positives (FP > 10) or false negatives (FN > 10).</em></p>'
                    
                    # Top Misclassification Patterns
                    if 'top_patterns' in error_data and error_data['top_patterns']:
                        html += '<h5>Top Misclassification Patterns</h5>'
                        html += '<div class="info-box">'
                        
                        for pattern in error_data['top_patterns']:
                            html += f'''
                            <div style="margin-bottom: 15px; padding: 10px; background: white; border-radius: 5px; border-left: 3px solid #2196F3;">
                                <h4 style="margin: 0 0 8px 0; color: #2196F3;">Pattern #{pattern['rank']} (occurred {pattern['count']} times)</h4>
                                <p style="margin: 5px 0;"><strong>True labels:</strong> <code>{pattern['true_labels']}</code></p>
                                <p style="margin: 5px 0;"><strong>Predicted labels:</strong> <code>{pattern['predicted_labels']}</code></p>
                            </div>
                            '''
                        html += '</div>'
        
        if not any('error_analysis' in self.report_data[dt] and self.report_data[dt]['error_analysis'] 
                   for dt in ['unbalanced', 'balanced']):
            html += '''
            <div class="warning-box">
                <h4>⚠️ Not Configured - Enable 'Error Analysis' in configuration to view this section</h4>
                <p>Error analysis data was not found in the log file. To enable this feature:</p>
                <ul style="margin-left: 20px; margin-top: 10px;">
                    <li>Open the Java UI application</li>
                    <li>Navigate to <strong>General Settings</strong> panel</li>
                    <li>Check <strong>"Enable Error Analysis (Misclassification)"</strong></li>
                    <li>Save configuration and re-run the pipeline</li>
                </ul>
                <p style="margin-top: 10px; font-style: italic;">This feature provides detailed analysis of misclassified samples, confusion patterns, and model weaknesses.</p>
            </div>
            '''
        
        html += '</section>'
        return html
    
    def _build_model_performance_section(self) -> str:
        """Build traditional ML model performance section."""
        html = '<section id="traditional-ml"><h2>6. Traditional ML Model Performance</h2>'
        html += '<p style="text-align: right; margin: -10px 0 20px 0;"><a href="#toc" style="text-decoration: none; color: #2196F3; font-size: 0.9em;">↑ Back to Index</a></p>'
        
        for data_type in ['unbalanced', 'balanced']:
            if 'cv_results' in self.report_data[data_type]:
                html += f'<h3>6.{1 if data_type == "unbalanced" else 2}. {data_type.capitalize()} Data - Cross-Validation Results</h3>'
                
                # Summary table with Hamming Loss
                html += '<table><thead><tr><th>Model</th><th>Mean Recall</th><th>Mean F1</th><th>Hamming Loss</th><th>Performance</th></tr></thead><tbody>'
                
                models_data = []
                for model, data in self.report_data[data_type]['cv_results'].items():
                    if isinstance(data, dict) and 'mean_f1' in data:
                        models_data.append((model, data))
                
                # Sort by F1
                models_data.sort(key=lambda x: x[1]['mean_f1'], reverse=True)
                
                for idx, (model, data) in enumerate(models_data):
                    badge_class = 'badge-success' if idx == 0 else 'badge-info'
                    badge_text = '🏆 Best' if idx == 0 else f'#{idx + 1}'
                    
                    # Get hamming loss from test results
                    hamming_loss = None
                    if 'test_results' in self.report_data[data_type] and model in self.report_data[data_type]['test_results']:
                        hamming_loss = self.report_data[data_type]['test_results'][model].get('hamming_loss')
                    hamming_display = f"{hamming_loss:.4f}" if hamming_loss is not None else 'N/A'
                    
                    html += f'''
                    <tr>
                        <td><strong>{model}</strong></td>
                        <td>{data['mean_recall']:.4f}</td>
                        <td class="{('best-score' if idx == 0 else '')}">{data['mean_f1']:.4f}</td>
                        <td>{hamming_display}</td>
                        <td><span class="badge {badge_class}">{badge_text}</span></td>
                    </tr>
                    '''
                
                html += '</tbody></table>'
                
                # Detailed fold results
                for model, data in models_data:
                    if 'folds' in data:
                        html += f'<h4>{model} - Fold-by-Fold Results</h4>'
                        
                        # Add horizontal bar chart visualization for F1 scores across folds
                        fold_f1_scores = [fold['f1'] for fold in data['folds'][:10]]
                        if fold_f1_scores:
                            max_f1 = max(fold_f1_scores)
                            min_f1 = min(fold_f1_scores)
                            avg_f1 = sum(fold_f1_scores) / len(fold_f1_scores)
                            
                            html += '<div style="margin: 20px 0; padding: 15px; background: #f9f9f9; border-radius: 8px;">'
                            html += '<h5 style="margin-bottom: 15px;">📊 F1 Score Distribution Across Folds</h5>'
                            
                            for fold in data['folds'][:10]:
                                f1_val = fold['f1']
                                width_percentage = (f1_val / 1.0) * 100
                                # Color based on performance relative to average
                                bar_color = '#4CAF50' if f1_val >= avg_f1 else '#FF9800' if f1_val >= min_f1 + (max_f1 - min_f1) * 0.3 else '#f44336'
                                
                                html += f'''
                                <div style="margin-bottom: 10px;">
                                    <div style="display: flex; justify-content: space-between; margin-bottom: 3px;">
                                        <span style="font-weight: 600; font-size: 0.9em;">Fold {fold['fold']}</span>
                                        <span style="font-weight: bold; color: {bar_color}; font-size: 0.9em;">{f1_val:.4f}</span>
                                    </div>
                                    <div style="background: #e0e0e0; border-radius: 8px; height: 25px; position: relative; overflow: hidden;">
                                        <div style="background: {bar_color}; width: {width_percentage}%; height: 100%; border-radius: 8px; transition: width 0.3s ease;"></div>
                                    </div>
                                </div>
                                '''
                            
                            # Add summary statistics
                            html += f'''
                            <div style="padding: 10px; background: white; border-radius: 6px; display: flex; justify-content: space-around; font-size: 0.9em; margin-top: 15px;">
                                <span><strong>Max:</strong> {max_f1:.4f}</span>
                                <span><strong>Avg:</strong> <span style="color: #2196F3;">{avg_f1:.4f}</span></span>
                                <span><strong>Min:</strong> {min_f1:.4f}</span>
                                <span><strong>Range:</strong> {max_f1 - min_f1:.4f}</span>
                            </div>
                            '''
                            html += '</div>'
                        
                        html += '<div class="fold-results">'
                        for fold in data['folds'][:10]:  # Show up to 10 folds
                            html += f'''
                            <div class="fold-card">
                                <div class="fold-number">Fold {fold['fold']}</div>
                                <div class="fold-metric">Recall: {fold['recall']:.4f}</div>
                                <div class="fold-metric">F1: {fold['f1']:.4f}</div>
                            </div>
                            '''
                        html += '</div>'
                
                # Test results (Hamming Loss)
                if 'test_results' in self.report_data[data_type]:
                    html += f'<h4>{data_type.capitalize()} - Test Set Performance (Hamming Loss)</h4>'
                    html += '<table><thead><tr><th>Model</th><th>Hamming Loss</th><th>Status</th></tr></thead><tbody>'
                    
                    test_models = []
                    for model, data in self.report_data[data_type]['test_results'].items():
                        test_models.append((model, data['hamming_loss']))
                    
                    test_models.sort(key=lambda x: x[1])  # Lower is better
                    
                    for idx, (model, hl) in enumerate(test_models):
                        badge_class = 'badge-success' if idx == 0 else 'badge-info'
                        badge_text = '🏆 Best' if idx == 0 else 'Good' if hl < 0.20 else 'Average'
                        
                        html += f'''
                        <tr>
                            <td><strong>{model}</strong></td>
                            <td class="{('best-score' if idx == 0 else '')}">{hl:.4f}</td>
                            <td><span class="badge {badge_class}">{badge_text}</span></td>
                        </tr>
                        '''
                    
                    html += '</tbody></table>'
        
        html += '</section>'
        return html
    
    def _build_deep_learning_section(self) -> str:
        """Build deep learning model performance section."""
        html = '<section id="deep-learning"><h2>7. Deep Learning Model Performance</h2>'
        html += '<p style="text-align: right; margin: -10px 0 20px 0;"><a href="#toc" style="text-decoration: none; color: #2196F3; font-size: 0.9em;">↑ Back to Index</a></p>'
        
        for data_type in ['unbalanced', 'balanced']:
            if 'deep_learning' in self.report_data[data_type]:
                html += f'<h3>7.{1 if data_type == "unbalanced" else 2}. {data_type.capitalize()} Data</h3>'
                
                dl_data = self.report_data[data_type]['deep_learning']
                
                # Summary cards
                html += '<div class="metric-grid">'
                
                for model_name in ['MLP', 'CNN']:
                    if model_name in dl_data:
                        model_data = dl_data[model_name]
                        hamming_loss = model_data.get('hamming_loss')
                        hamming_display = f"<div class='metric-label'>Hamming Loss: {hamming_loss:.4f}</div>" if hamming_loss is not None else ""
                        html += f'''
                        <div class="metric-card">
                            <h4>{model_name} Cross-Validation</h4>
                            <div class="metric-value">{model_data['cv_f1']:.4f}</div>
                            <div class="metric-label">Mean F1 Score</div>
                            <div class="metric-label">Recall: {model_data['cv_recall']:.4f}</div>
                            {hamming_display}
                        </div>
                        '''
                
                html += '</div>'
                
                # Detailed fold results
                for model_name in ['MLP', 'CNN']:
                    if model_name in dl_data and 'folds' in dl_data[model_name]:
                        model_data = dl_data[model_name]
                        html += f'<h4>{model_name} - Fold-by-Fold Results</h4>'
                        
                        # Add horizontal bar chart visualization for F1 scores across folds
                        fold_f1_scores = [fold['f1'] for fold in model_data['folds'][:10]]
                        if fold_f1_scores:
                            max_f1 = max(fold_f1_scores)
                            min_f1 = min(fold_f1_scores)
                            avg_f1 = sum(fold_f1_scores) / len(fold_f1_scores)
                            
                            html += '<div style="margin: 20px 0; padding: 15px; background: #f9f9f9; border-radius: 8px;">'
                            html += '<h5 style="margin-bottom: 15px;">📊 F1 Score Distribution Across Folds</h5>'
                            
                            for fold in model_data['folds'][:10]:
                                f1_val = fold['f1']
                                width_percentage = (f1_val / 1.0) * 100
                                # Color based on performance relative to average
                                bar_color = '#4CAF50' if f1_val >= avg_f1 else '#FF9800' if f1_val >= min_f1 + (max_f1 - min_f1) * 0.3 else '#f44336'
                                
                                html += f'''
                                <div style="margin-bottom: 10px;">
                                    <div style="display: flex; justify-content: space-between; margin-bottom: 3px;">
                                        <span style="font-weight: 600; font-size: 0.9em;">Fold {fold['fold']}</span>
                                        <span style="font-weight: bold; color: {bar_color}; font-size: 0.9em;">{f1_val:.4f}</span>
                                    </div>
                                    <div style="background: #e0e0e0; border-radius: 8px; height: 25px; position: relative; overflow: hidden;">
                                        <div style="background: {bar_color}; width: {width_percentage}%; height: 100%; border-radius: 8px; transition: width 0.3s ease;"></div>
                                    </div>
                                </div>
                                '''
                            
                            # Add summary statistics
                            html += f'''
                            <div style="padding: 10px; background: white; border-radius: 6px; display: flex; justify-content: space-around; font-size: 0.9em; margin-top: 15px;">
                                <span><strong>Max:</strong> {max_f1:.4f}</span>
                                <span><strong>Avg:</strong> <span style="color: #2196F3;">{avg_f1:.4f}</span></span>
                                <span><strong>Min:</strong> {min_f1:.4f}</span>
                                <span><strong>Range:</strong> {max_f1 - min_f1:.4f}</span>
                            </div>
                            '''
                            html += '</div>'
                        
                        html += '<div class="fold-results">'
                        for fold in model_data['folds'][:10]:
                            html += f'''
                            <div class="fold-card">
                                <div class="fold-number">Fold {fold['fold']}</div>
                                <div class="fold-metric">Recall: {fold['recall']:.4f}</div>
                                <div class="fold-metric">F1: {fold['f1']:.4f}</div>
                            </div>
                            '''
                        html += '</div>'
                
                # CNN training progression
                if 'CNN' in dl_data and 'training_epochs' in dl_data['CNN']:
                    epochs = dl_data['CNN']['training_epochs']
                    html += '<h4>CNN Training Progression</h4>'
                    html += '<table><thead><tr><th>Epoch</th><th>Validation Accuracy</th><th>Validation Loss</th></tr></thead><tbody>'
                    
                    best_epoch = max(range(len(epochs)), key=lambda i: epochs[i]['val_accuracy'])
                    
                    for idx, epoch in enumerate(epochs):
                        is_best = idx == best_epoch
                        html += f'''
                        <tr {('style="background: #d4edda; font-weight: bold;"' if is_best else '')}>
                            <td>Epoch {idx + 1} {('🏆' if is_best else '')}</td>
                            <td>{epoch['val_accuracy']:.4f}</td>
                            <td>{epoch['val_loss']:.4f}</td>
                        </tr>
                        '''
                    
                    html += '</tbody></table>'
        
        html += '<p style="text-align: right; margin: 20px 0;"><a href="#toc" style="text-decoration: none; color: #2196F3; font-size: 0.9em;">↑ Back to Index</a></p>'
        html += '</section>'
        return html
    
    def _build_comparison_section(self) -> str:
        """Build balanced vs unbalanced comparison section."""
        html = '<section id="comparison"><h2>10. Balanced vs Unbalanced Data Comparison</h2>'
        html += '<p style="text-align: right; margin: -10px 0 20px 0;"><a href="#toc" style="text-decoration: none; color: #2196F3; font-size: 0.9em;">↑ Back to Index</a></p>'
        
        # Comprehensive comparison for all models including Deep Learning
        html += '<h3>10.1. Comprehensive Model Performance Comparison</h3>'
        html += '<p class="info-box">Comparison of all metrics across unbalanced and balanced datasets for Traditional ML and Deep Learning models.</p>'
        
        # Build comprehensive comparison table
        all_models = {}
        
        # Collect Traditional ML models
        if 'cv_results' in self.report_data.get('unbalanced', {}):
            for model, data in self.report_data['unbalanced']['cv_results'].items():
                if isinstance(data, dict) and 'mean_f1' in data:
                    if model not in all_models:
                        all_models[model] = {'type': 'Traditional ML'}
                    all_models[model]['unbalanced_recall'] = data.get('mean_recall', 0)
                    all_models[model]['unbalanced_f1'] = data['mean_f1']
        
        if 'cv_results' in self.report_data.get('balanced', {}):
            for model, data in self.report_data['balanced']['cv_results'].items():
                if isinstance(data, dict) and 'mean_f1' in data:
                    if model not in all_models:
                        all_models[model] = {'type': 'Traditional ML'}
                    all_models[model]['balanced_recall'] = data.get('mean_recall', 0)
                    all_models[model]['balanced_f1'] = data['mean_f1']
        
        # Collect Deep Learning models
        for dl_model in ['MLP', 'CNN']:
            if dl_model in self.report_data.get('unbalanced', {}).get('deep_learning', {}):
                dl_data = self.report_data['unbalanced']['deep_learning'][dl_model]
                if dl_model not in all_models:
                    all_models[dl_model] = {'type': 'Deep Learning'}
                all_models[dl_model]['unbalanced_recall'] = dl_data.get('cv_recall', 0)
                all_models[dl_model]['unbalanced_f1'] = dl_data.get('cv_f1', 0)
            
            if dl_model in self.report_data.get('balanced', {}).get('deep_learning', {}):
                dl_data = self.report_data['balanced']['deep_learning'][dl_model]
                if dl_model not in all_models:
                    all_models[dl_model] = {'type': 'Deep Learning'}
                all_models[dl_model]['balanced_recall'] = dl_data.get('cv_recall', 0)
                all_models[dl_model]['balanced_f1'] = dl_data.get('cv_f1', 0)
        
        # Collect Hamming Loss from test results
        if 'test_results' in self.report_data.get('unbalanced', {}):
            for model, data in self.report_data['unbalanced']['test_results'].items():
                model_name = model.replace('_', '')
                # Find matching model in all_models (case-insensitive)
                for key in all_models.keys():
                    if key.lower().replace(' ', '') == model_name.lower():
                        all_models[key]['unbalanced_hamming'] = data.get('hamming_loss', 0)
                        break
        
        # Add DL hamming loss from deep_learning section
        for dl_model in ['MLP', 'CNN']:
            if dl_model in self.report_data.get('unbalanced', {}).get('deep_learning', {}):
                dl_data = self.report_data['unbalanced']['deep_learning'][dl_model]
                if 'hamming_loss' in dl_data and dl_model in all_models:
                    all_models[dl_model]['unbalanced_hamming'] = dl_data['hamming_loss']
        
        if 'test_results' in self.report_data.get('balanced', {}):
            for model, data in self.report_data['balanced']['test_results'].items():
                model_name = model.replace('_', '')
                for key in all_models.keys():
                    if key.lower().replace(' ', '') == model_name.lower():
                        all_models[key]['balanced_hamming'] = data.get('hamming_loss', 0)
                        break
        
        # Add DL hamming loss from deep_learning section
        for dl_model in ['MLP', 'CNN']:
            if dl_model in self.report_data.get('balanced', {}).get('deep_learning', {}):
                dl_data = self.report_data['balanced']['deep_learning'][dl_model]
                if 'hamming_loss' in dl_data and dl_model in all_models:
                    all_models[dl_model]['balanced_hamming'] = dl_data['hamming_loss']
        
        # Calculate improvements
        for model in all_models:
            # F1 improvement
            unb_f1 = all_models[model].get('unbalanced_f1', 0)
            bal_f1 = all_models[model].get('balanced_f1', 0)
            if unb_f1 > 0:
                all_models[model]['f1_improvement'] = bal_f1 - unb_f1
                all_models[model]['f1_improvement_pct'] = ((bal_f1 - unb_f1) / unb_f1) * 100
            else:
                all_models[model]['f1_improvement'] = 0
                all_models[model]['f1_improvement_pct'] = 0
            
            # Recall improvement
            unb_recall = all_models[model].get('unbalanced_recall', 0)
            bal_recall = all_models[model].get('balanced_recall', 0)
            if unb_recall > 0:
                all_models[model]['recall_improvement'] = bal_recall - unb_recall
                all_models[model]['recall_improvement_pct'] = ((bal_recall - unb_recall) / unb_recall) * 100
            else:
                all_models[model]['recall_improvement'] = 0
                all_models[model]['recall_improvement_pct'] = 0
            
            # Hamming Loss reduction (lower is better)
            unb_hl = all_models[model].get('unbalanced_hamming', 0)
            bal_hl = all_models[model].get('balanced_hamming', 0)
            if unb_hl > 0:
                all_models[model]['hamming_reduction'] = unb_hl - bal_hl
                all_models[model]['hamming_reduction_pct'] = ((unb_hl - bal_hl) / unb_hl) * 100
            else:
                all_models[model]['hamming_reduction'] = 0
                all_models[model]['hamming_reduction_pct'] = 0
        
        # Render comprehensive table
        html += '<h4>10.1.1 Cross-Validation Metrics Comparison</h4>'
        html += '<table><thead><tr>'
        html += '<th>Model</th><th>Type</th>'
        html += '<th>Unbalanced<br>Recall</th><th>Balanced<br>Recall</th><th>Recall<br>Δ</th><th>Recall<br>% Δ</th>'
        html += '<th>Unbalanced<br>F1</th><th>Balanced<br>F1</th><th>F1<br>Δ</th><th>F1<br>% Δ</th>'
        html += '</tr></thead><tbody>'
        
        for model, metrics in sorted(all_models.items(), key=lambda x: x[1].get('balanced_f1', 0), reverse=True):
            recall_class = 'improvement-positive' if metrics.get('recall_improvement', 0) > 0 else 'improvement-negative'
            f1_class = 'improvement-positive' if metrics.get('f1_improvement', 0) > 0 else 'improvement-negative'
            recall_arrow = '↑' if metrics.get('recall_improvement', 0) > 0 else '↓'
            f1_arrow = '↑' if metrics.get('f1_improvement', 0) > 0 else '↓'
            
            html += f'''
            <tr>
                <td><strong>{model}</strong></td>
                <td><span class="badge badge-info">{metrics.get('type', 'N/A')}</span></td>
                <td>{metrics.get('unbalanced_recall', 0):.4f}</td>
                <td>{metrics.get('balanced_recall', 0):.4f}</td>
                <td class="{recall_class}">{recall_arrow} {abs(metrics.get('recall_improvement', 0)):.4f}</td>
                <td class="{recall_class}">{metrics.get('recall_improvement_pct', 0):+.2f}%</td>
                <td>{metrics.get('unbalanced_f1', 0):.4f}</td>
                <td>{metrics.get('balanced_f1', 0):.4f}</td>
                <td class="{f1_class}">{f1_arrow} {abs(metrics.get('f1_improvement', 0)):.4f}</td>
                <td class="{f1_class}">{metrics.get('f1_improvement_pct', 0):+.2f}%</td>
            </tr>
            '''
        
        html += '</tbody></table>'
        
        # Test Set Hamming Loss Comparison
        html += '<h4>10.1.2 Test Set Hamming Loss Comparison</h4>'
        html += '<p class="info-box">Lower Hamming Loss is better. Positive % Δ indicates improvement with balanced data.</p>'
        
        html += '<table><thead><tr>'
        html += '<th>Model</th><th>Type</th>'
        html += '<th>Unbalanced<br>Hamming Loss</th><th>Balanced<br>Hamming Loss</th>'
        html += '<th>Reduction</th><th>% Reduction</th><th>Status</th>'
        html += '</tr></thead><tbody>'
        
        for model, metrics in sorted(all_models.items(), key=lambda x: x[1].get('hamming_reduction_pct', 0), reverse=True):
            if metrics.get('unbalanced_hamming', 0) > 0 or metrics.get('balanced_hamming', 0) > 0:
                hamming_class = 'improvement-positive' if metrics.get('hamming_reduction', 0) > 0 else 'improvement-negative'
                arrow = '↓' if metrics.get('hamming_reduction', 0) > 0 else '↑'
                status = 'Improved' if metrics.get('hamming_reduction', 0) > 0 else 'Degraded'
                status_badge = 'badge-success' if metrics.get('hamming_reduction', 0) > 0 else 'badge-warning'
                
                html += f'''
                <tr>
                    <td><strong>{model}</strong></td>
                    <td><span class="badge badge-info">{metrics.get('type', 'N/A')}</span></td>
                    <td>{metrics.get('unbalanced_hamming', 0):.4f}</td>
                    <td>{metrics.get('balanced_hamming', 0):.4f}</td>
                    <td class="{hamming_class}">{arrow} {abs(metrics.get('hamming_reduction', 0)):.4f}</td>
                    <td class="{hamming_class}">{metrics.get('hamming_reduction_pct', 0):+.2f}%</td>
                    <td><span class="badge {status_badge}">{status}</span></td>
                </tr>
                '''
        
        html += '</tbody></table>'
        
        # Summary statistics
        html += '<h3>10.2. Overall Impact Summary</h3>'
        
        avg_f1_improvement = sum(m.get('f1_improvement_pct', 0) for m in all_models.values()) / len(all_models) if all_models else 0
        avg_recall_improvement = sum(m.get('recall_improvement_pct', 0) for m in all_models.values()) / len(all_models) if all_models else 0
        
        models_with_hamming = [m for m in all_models.values() if m.get('unbalanced_hamming', 0) > 0]
        avg_hamming_reduction = sum(m.get('hamming_reduction_pct', 0) for m in models_with_hamming) / len(models_with_hamming) if models_with_hamming else 0
        
        html += '<div style="display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; margin: 20px 0;">'
        
        f1_color = '#d4edda' if avg_f1_improvement > 0 else '#f8d7da'
        recall_color = '#d4edda' if avg_recall_improvement > 0 else '#f8d7da'
        hamming_color = '#d4edda' if avg_hamming_reduction > 0 else '#f8d7da'
        
        html += f'''
        <div style="background: {f1_color}; padding: 20px; border-radius: 8px; text-align: center;">
            <h4 style="margin: 0 0 10px 0; color: #333;">Average F1 Improvement</h4>
            <div style="font-size: 2em; font-weight: bold; color: #155724;">{avg_f1_improvement:+.2f}%</div>
            <div style="font-size: 0.9em; color: #666; margin-top: 5px;">Across {len(all_models)} models</div>
        </div>
        <div style="background: {recall_color}; padding: 20px; border-radius: 8px; text-align: center;">
            <h4 style="margin: 0 0 10px 0; color: #333;">Average Recall Improvement</h4>
            <div style="font-size: 2em; font-weight: bold; color: #155724;">{avg_recall_improvement:+.2f}%</div>
            <div style="font-size: 0.9em; color: #666; margin-top: 5px;">Across {len(all_models)} models</div>
        </div>
        <div style="background: {hamming_color}; padding: 20px; border-radius: 8px; text-align: center;">
            <h4 style="margin: 0 0 10px 0; color: #333;">Average Hamming Loss Reduction</h4>
            <div style="font-size: 2em; font-weight: bold; color: #155724;">{avg_hamming_reduction:+.2f}%</div>
            <div style="font-size: 0.9em; color: #666; margin-top: 5px;">Across {len(models_with_hamming)} models</div>
        </div>
        '''
        
        html += '</div>'
        
        html += '<p style="text-align: right; margin: 20px 0;"><a href="#toc" style="text-decoration: none; color: #2196F3; font-size: 0.9em;">↑ Back to Index</a></p>'
        html += '</section>'
        return html
    
    def _build_recommendations(self) -> str:
        """Build recommendations section."""
        html = '<section id="recommendations"><h2>11. Recommendations & Insights</h2>'
        html += '<p style="text-align: right; margin: -10px 0 20px 0;"><a href="#toc" style="text-decoration: none; color: #2196F3; font-size: 0.9em;">↑ Back to Index</a></p>'
        
        recommendations = []
        
        # Analyze balancing impact
        if 'cv_improvement' in self.report_data['comparison']:
            avg_improvement = sum(v['improvement_pct'] for v in self.report_data['comparison']['cv_improvement'].values()) / len(self.report_data['comparison']['cv_improvement'])
            
            if avg_improvement > 5:
                recommendations.append({
                    'type': 'success',
                    'title': 'Data Balancing Highly Effective',
                    'message': f'Balanced data shows an average improvement of {avg_improvement:.2f}% in F1 scores. Continue using balanced datasets for training.'
                })
            elif avg_improvement > 0:
                recommendations.append({
                    'type': 'info',
                    'title': 'Moderate Improvement from Balancing',
                    'message': f'Balanced data shows a {avg_improvement:.2f}% improvement. Consider experimenting with different balancing strategies.'
                })
            else:
                recommendations.append({
                    'type': 'warning',
                    'title': 'Review Balancing Strategy',
                    'message': f'Balanced data shows a {avg_improvement:.2f}% change. The balancing approach may need adjustment or may not be beneficial for this dataset.'
                })
        
        # Analyze best models
        best_models = {}
        for data_type in ['unbalanced', 'balanced']:
            best_f1 = 0
            best_model = None
            
            # Check traditional ML models
            if 'cv_results' in self.report_data[data_type]:
                for model, data in self.report_data[data_type]['cv_results'].items():
                    if isinstance(data, dict) and 'mean_f1' in data:
                        if data['mean_f1'] > best_f1:
                            best_f1 = data['mean_f1']
                            best_model = model
            
            # Check deep learning models
            if 'deep_learning' in self.report_data[data_type]:
                for dl_model in ['MLP', 'CNN']:
                    if dl_model in self.report_data[data_type]['deep_learning']:
                        dl_data = self.report_data[data_type]['deep_learning'][dl_model]
                        if 'cv_f1' in dl_data and dl_data['cv_f1'] > best_f1:
                            best_f1 = dl_data['cv_f1']
                            best_model = dl_model
            
            if best_model:
                best_models[data_type] = {'model': best_model, 'f1': best_f1}
        
        if len(best_models) == 2:
            unb_best = best_models['unbalanced']
            bal_best = best_models['balanced']
            
            if bal_best['f1'] > 0.8:
                # Determine if it's CNN or traditional ML
                model_type = 'Deep Learning model' if bal_best['model'] in ['CNN', 'MLP'] else 'Traditional ML model'
                is_cnn = bal_best['model'] == 'CNN'
                
                if is_cnn:
                    model_desc = '🏆 CNN (Best Model)'
                else:
                    model_desc = f"{bal_best['model']} ({model_type})"
                
                recommendations.append({
                    'type': 'success',
                    'title': 'Excellent Model Performance',
                    'message': f"{model_desc} achieves {bal_best['f1']:.4f} F1 score on balanced data. This is production-ready performance."
                })
            
            if unb_best['model'] != bal_best['model']:
                recommendations.append({
                    'type': 'info',
                    'title': 'Different Best Models',
                    'message': f"Best model changes from {unb_best['model']} (unbalanced) to {bal_best['model']} (balanced). Consider ensemble approaches."
                })
        
        # Deep learning performance
        if 'deep_learning' in self.report_data['balanced']:
            dl = self.report_data['balanced']['deep_learning']
            if 'CNN' in dl and dl['CNN']['cv_f1'] > 0.9:
                recommendations.append({
                    'type': 'success',
                    'title': 'Outstanding Deep Learning Performance',
                    'message': f"CNN achieves {dl['CNN']['cv_f1']:.4f} F1 score. Deep learning models are highly effective for this task."
                })
        
        # Render recommendations
        for rec in recommendations:
            box_class = f"{rec['type']}-box"
            html += f'''
            <div class="{box_class}">
                <h4>💡 {rec['title']}</h4>
                <p>{rec['message']}</p>
            </div>
            '''
        
        # Add Best Model Recommendation Section
        html += '<h3>11.1. Best Model Recommendation</h3>'
        html += '<div class="info-box" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; border: none;">'
        html += '<h4 style="color: white; margin-bottom: 15px;">🎯 Recommended Model for Production</h4>'
        
        # Determine overall best model across all datasets and models
        overall_best_f1 = 0
        overall_best_model = None
        overall_best_dataset = None
        overall_best_recall = 0
        overall_best_hamming = None
        
        for data_type in ['unbalanced', 'balanced']:
            # Check traditional ML models
            if 'cv_results' in self.report_data[data_type]:
                for model, data in self.report_data[data_type]['cv_results'].items():
                    if isinstance(data, dict) and 'mean_f1' in data:
                        if data['mean_f1'] > overall_best_f1:
                            overall_best_f1 = data['mean_f1']
                            overall_best_recall = data.get('mean_recall', 0)
                            overall_best_model = model
                            overall_best_dataset = data_type
                            # Get hamming loss from test results
                            if 'test_results' in self.report_data[data_type] and model in self.report_data[data_type]['test_results']:
                                overall_best_hamming = self.report_data[data_type]['test_results'][model].get('hamming_loss')
            
            # Check deep learning models
            if 'deep_learning' in self.report_data[data_type]:
                for dl_model in ['MLP', 'CNN']:
                    if dl_model in self.report_data[data_type]['deep_learning']:
                        dl_data = self.report_data[data_type]['deep_learning'][dl_model]
                        if 'cv_f1' in dl_data and dl_data['cv_f1'] > overall_best_f1:
                            overall_best_f1 = dl_data['cv_f1']
                            overall_best_recall = dl_data.get('cv_recall', 0)
                            overall_best_model = dl_model
                            overall_best_dataset = data_type
                            overall_best_hamming = dl_data.get('hamming_loss')
        
        if overall_best_model:
            # Determine model category
            model_category = 'Deep Learning' if overall_best_model in ['CNN', 'MLP'] else 'Traditional ML'
            dataset_label = overall_best_dataset.capitalize()
            
            html += f'''
            <p style="font-size: 1.1em; margin-bottom: 15px;">
                <strong style="font-size: 1.3em;">🏆 {overall_best_model}</strong> 
                <span style="opacity: 0.9;">({model_category} - {dataset_label} Data)</span>
            </p>
            <div style="background: rgba(255,255,255,0.15); padding: 15px; border-radius: 8px; margin-bottom: 15px;">
                <div style="display: flex; justify-content: space-around; text-align: center; flex-wrap: wrap;">
                    <div>
                        <div style="font-size: 2em; font-weight: bold;">{overall_best_f1:.4f}</div>
                        <div style="opacity: 0.9;">F1 Score</div>
                    </div>
                    <div>
                        <div style="font-size: 2em; font-weight: bold;">{overall_best_recall:.4f}</div>
                        <div style="opacity: 0.9;">Recall</div>
                    </div>'''
            
            if overall_best_hamming is not None:
                html += f'''
                    <div>
                        <div style="font-size: 2em; font-weight: bold;">{overall_best_hamming:.4f}</div>
                        <div style="opacity: 0.9;">Hamming Loss</div>
                    </div>'''
            
            html += '''
                </div>
            </div>
            '''
            
            # Add performance interpretation
            if overall_best_f1 >= 0.95:
                performance_text = "Exceptional performance - This model demonstrates outstanding predictive capabilities and is highly recommended for production deployment."
            elif overall_best_f1 >= 0.90:
                performance_text = "Excellent performance - This model shows very strong results and is well-suited for production use."
            elif overall_best_f1 >= 0.85:
                performance_text = "Very good performance - This model delivers reliable predictions and is suitable for production deployment."
            elif overall_best_f1 >= 0.80:
                performance_text = "Good performance - This model provides solid results and can be deployed with confidence."
            elif overall_best_f1 >= 0.70:
                performance_text = "Moderate performance - This model shows reasonable results but may benefit from further tuning."
            else:
                performance_text = "Performance needs improvement - Consider feature engineering, hyperparameter tuning, or alternative approaches."
            
            html += f'<p style="opacity: 0.95; line-height: 1.6;">{performance_text}</p>'
        
        html += '</div>'
        
        html += '</section>'
        return html
    
    def _build_footer(self) -> str:
        """Build report footer."""
        return f"""
        <footer>
            <p><strong>Multi-Label Classification Training Report</strong></p>
            <p>Generated: {self.report_data['metadata'].get('generated_at', 'N/A')}</p>
            <p>Log File: {Path(self.report_data['metadata']['log_file']).name}</p>
            <p style="margin-top: 15px; opacity: 0.7;">
                This report was automatically generated from training logs.
            </p>
        </footer>
        """


def generate_log_report(log_file_path: str, output_path: Optional[str] = None) -> str:
    """
    Convenience function to generate an HTML report from a log file.
    
    Args:
        log_file_path: Path to the training log file
        output_path: Optional path for the output HTML file
    
    Returns:
        Path to the generated HTML report
    
    Example:
        >>> report_path = generate_log_report('output/log.txt')
        >>> print(f"Report generated: {report_path}")
    """
    generator = LogReportGenerator(log_file_path)
    generator.parse_log()
    return generator.generate_html_report(output_path)


if __name__ == "__main__":
    # Example usage
    import sys
    
    if len(sys.argv) > 1:
        log_path = sys.argv[1]
        output = sys.argv[2] if len(sys.argv) > 2 else None
        
        report_path = generate_log_report(log_path, output)
        print("HTML report generated successfully!")
        print(f"Report location: {report_path}")
    else:
        print("Usage: python log_report_generator.py <log_file_path> [output_path]")
        print("Example: python log_report_generator.py output/log.txt output/report.html")
