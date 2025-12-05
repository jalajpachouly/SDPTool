"""
Prediction Report Generator for Multi-Label Classification

Generates comprehensive HTML reports for prediction runs,
showing detailed results, confidence scores, and analysis.
"""

from pathlib import Path
from datetime import datetime
from typing import Dict, List, Any
import json


class PredictionReportGenerator:
    """Generate comprehensive HTML reports for prediction results."""
    
    def __init__(self, results: Dict[str, Any], output_dir: str = None):
        """
        Initialize report generator.
        
        Args:
            results: Prediction results dictionary
            output_dir: Output directory for report (default: multilable-prediction/output/predictions)
        """
        self.results = results
        
        if output_dir is None:
            self.output_dir = Path("multilable-prediction/output/predictions")
        else:
            self.output_dir = Path(output_dir)
        
        self.output_dir.mkdir(parents=True, exist_ok=True)
        
        # Generate report filename
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        run_id = results.get('run_id', 'unknown')
        self.report_filename = f"prediction_report_{run_id}_{timestamp}.html"
        self.report_path = self.output_dir / self.report_filename
    
    def generate_report(self) -> str:
        """
        Generate HTML report.
        
        Returns:
            Path to generated report
        """
        html = self._generate_html()
        
        with open(self.report_path, 'w', encoding='utf-8') as f:
            f.write(html)
        
        print(f"\nPrediction report generated: {self.report_path}")
        return str(self.report_path)
    
    def _generate_html(self) -> str:
        """Generate complete HTML report."""
        return f"""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Prediction Report - {self.results['model_name']}</title>
    <style>
        {self._get_css()}
    </style>
</head>
<body>
    <div class="container">
        {self._generate_header()}
        {self._generate_summary()}
        {self._generate_predictions_section()}
        {self._generate_statistics()}
        {self._generate_footer()}
    </div>
</body>
</html>
"""
    
    def _get_css(self) -> str:
        """Get CSS styles for report."""
        return """
        * {
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }
        
        body {
            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 20px;
            color: #333;
        }
        
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            border-radius: 12px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.2);
            overflow: hidden;
        }
        
        .header {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 40px;
            text-align: center;
        }
        
        .header h1 {
            font-size: 2.5em;
            margin-bottom: 10px;
        }
        
        .header p {
            font-size: 1.1em;
            opacity: 0.9;
        }
        
        .section {
            padding: 30px 40px;
            border-bottom: 1px solid #e0e0e0;
        }
        
        .section:last-child {
            border-bottom: none;
        }
        
        .section-title {
            font-size: 1.8em;
            color: #667eea;
            margin-bottom: 20px;
            display: flex;
            align-items: center;
        }
        
        .section-title::before {
            content: '';
            width: 5px;
            height: 30px;
            background: #667eea;
            margin-right: 15px;
            border-radius: 3px;
        }
        
        .summary-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-top: 20px;
        }
        
        .summary-card {
            background: linear-gradient(135deg, #f5f7fa 0%, #c3cfe2 100%);
            padding: 20px;
            border-radius: 8px;
            text-align: center;
        }
        
        .summary-card h3 {
            font-size: 0.9em;
            color: #666;
            text-transform: uppercase;
            letter-spacing: 1px;
            margin-bottom: 10px;
        }
        
        .summary-card p {
            font-size: 2em;
            font-weight: bold;
            color: #667eea;
        }
        
        .prediction-card {
            background: #f8f9fa;
            border-left: 4px solid #667eea;
            padding: 20px;
            margin-bottom: 20px;
            border-radius: 8px;
        }
        
        .prediction-header {
            display: flex;
            justify-content: space-between;
            align-items: center;
            margin-bottom: 15px;
        }
        
        .prediction-index {
            font-size: 1.2em;
            font-weight: bold;
            color: #667eea;
        }
        
        .match-badge {
            padding: 5px 15px;
            border-radius: 20px;
            font-size: 0.85em;
            font-weight: bold;
        }
        
        .match-exact {
            background: #10b981;
            color: white;
        }
        
        .match-partial {
            background: #f59e0b;
            color: white;
        }
        
        .match-none {
            background: #ef4444;
            color: white;
        }
        
        .text-preview {
            background: white;
            padding: 15px;
            border-radius: 6px;
            margin: 15px 0;
            border: 1px solid #e0e0e0;
            font-family: 'Courier New', monospace;
            font-size: 0.9em;
            color: #555;
        }
        
        .labels-section {
            display: grid;
            grid-template-columns: 1fr 1fr;
            gap: 20px;
            margin-top: 15px;
        }
        
        .labels-column h4 {
            font-size: 1em;
            margin-bottom: 10px;
            color: #666;
        }
        
        .label-tag {
            display: inline-block;
            padding: 6px 12px;
            margin: 5px 5px 5px 0;
            border-radius: 6px;
            font-size: 0.85em;
            font-weight: 500;
        }
        
        .label-predicted {
            background: #667eea;
            color: white;
        }
        
        .label-truth {
            background: #10b981;
            color: white;
        }
        
        .confidence-bars {
            margin-top: 15px;
        }
        
        .confidence-item {
            margin-bottom: 10px;
        }
        
        .confidence-label {
            display: flex;
            justify-content: space-between;
            margin-bottom: 5px;
            font-size: 0.9em;
        }
        
        .confidence-bar {
            height: 8px;
            background: #e0e0e0;
            border-radius: 4px;
            overflow: hidden;
        }
        
        .confidence-fill {
            height: 100%;
            background: linear-gradient(90deg, #667eea 0%, #764ba2 100%);
            border-radius: 4px;
            transition: width 0.3s ease;
        }
        
        .stats-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 20px;
            margin-top: 20px;
        }
        
        .stat-card {
            background: #f8f9fa;
            padding: 20px;
            border-radius: 8px;
        }
        
        .stat-card h3 {
            font-size: 1.1em;
            color: #667eea;
            margin-bottom: 15px;
        }
        
        .stat-row {
            display: flex;
            justify-content: space-between;
            padding: 10px 0;
            border-bottom: 1px solid #e0e0e0;
        }
        
        .stat-row:last-child {
            border-bottom: none;
        }
        
        .footer {
            background: #f8f9fa;
            padding: 20px 40px;
            text-align: center;
            color: #666;
            font-size: 0.9em;
        }
        
        @media print {
            body {
                background: white;
                padding: 0;
            }
            
            .container {
                box-shadow: none;
            }
        }
        """
    
    def _generate_header(self) -> str:
        """Generate header section."""
        timestamp = datetime.now().strftime("%B %d, %Y at %H:%M:%S")
        return f"""
        <div class="header">
            <h1>🎯 Prediction Report</h1>
            <p>{self.results['model_name']} ({self.results['model_type']})</p>
            <p style="font-size: 0.95em; margin-top: 10px;">Generated on {timestamp}</p>
        </div>
        """
    
    def _generate_summary(self) -> str:
        """Generate summary section."""
        num_samples = self.results['num_samples']
        
        # Calculate statistics
        exact_matches = sum(1 for p in self.results['predictions'] if p.get('exact_match', False))
        has_ground_truth = any('ground_truth' in p for p in self.results['predictions'])
        
        avg_confidence = 0
        for pred in self.results['predictions']:
            confidences = list(pred['confidence_scores'].values())
            avg_confidence += sum(confidences) / len(confidences)
        avg_confidence /= num_samples if num_samples > 0 else 1
        
        summary_html = f"""
        <div class="section">
            <h2 class="section-title">Summary</h2>
            <div class="summary-grid">
                <div class="summary-card">
                    <h3>Total Samples</h3>
                    <p>{num_samples}</p>
                </div>
                <div class="summary-card">
                    <h3>Model Run ID</h3>
                    <p style="font-size: 1.2em;">{self.results['run_id']}</p>
                </div>
                <div class="summary-card">
                    <h3>Avg Confidence</h3>
                    <p>{avg_confidence:.2%}</p>
                </div>
        """
        
        if has_ground_truth:
            accuracy = exact_matches / num_samples if num_samples > 0 else 0
            summary_html += f"""
                <div class="summary-card">
                    <h3>Exact Match Accuracy</h3>
                    <p>{accuracy:.2%}</p>
                </div>
            """
        
        summary_html += """
            </div>
        </div>
        """
        
        return summary_html
    
    def _generate_predictions_section(self) -> str:
        """Generate predictions section."""
        predictions_html = """
        <div class="section">
            <h2 class="section-title">Detailed Predictions</h2>
        """
        
        for pred in self.results['predictions']:
            predictions_html += self._generate_prediction_card(pred)
        
        predictions_html += "</div>"
        return predictions_html
    
    def _generate_prediction_card(self, pred: Dict[str, Any]) -> str:
        """Generate a single prediction card."""
        # Determine match status
        match_badge = ""
        if 'ground_truth' in pred:
            if pred.get('exact_match', False):
                match_badge = '<span class="match-badge match-exact">✓ Exact Match</span>'
            elif pred.get('sample_accuracy', 0) > 0:
                match_badge = f'<span class="match-badge match-partial">Partial ({pred["sample_accuracy"]:.0%})</span>'
            else:
                match_badge = '<span class="match-badge match-none">✗ No Match</span>'
        
        # Header
        index = pred['sample_index'] + 1
        row_info = f" (Row {pred['dataset_row']})" if 'dataset_row' in pred else ""
        
        card_html = f"""
        <div class="prediction-card">
            <div class="prediction-header">
                <div class="prediction-index">Sample #{index}{row_info}</div>
                {match_badge}
            </div>
            
            <div class="text-preview">{pred['text']}</div>
            
            <div class="labels-section">
                <div class="labels-column">
                    <h4>Predicted Labels</h4>
                    <div>
        """
        
        # Predicted labels
        if pred['predicted_labels']:
            for label in pred['predicted_labels']:
                card_html += f'<span class="label-tag label-predicted">{label}</span>'
        else:
            card_html += '<span class="label-tag" style="background: #999; color: white;">None</span>'
        
        card_html += """
                    </div>
                </div>
        """
        
        # Ground truth (if available)
        if 'ground_truth' in pred:
            card_html += """
                <div class="labels-column">
                    <h4>Ground Truth</h4>
                    <div>
            """
            if pred['ground_truth']:
                for label in pred['ground_truth']:
                    card_html += f'<span class="label-tag label-truth">{label}</span>'
            else:
                card_html += '<span class="label-tag" style="background: #999; color: white;">None</span>'
            
            card_html += """
                    </div>
                </div>
            """
        
        card_html += "</div>"
        
        # Confidence scores
        card_html += """
            <div class="confidence-bars">
                <h4 style="margin-bottom: 15px; color: #666;">Confidence Scores</h4>
        """
        
        # Sort by confidence
        sorted_scores = sorted(
            pred['confidence_scores'].items(),
            key=lambda x: x[1],
            reverse=True
        )
        
        for label, score in sorted_scores:
            percentage = score * 100
            card_html += f"""
                <div class="confidence-item">
                    <div class="confidence-label">
                        <span>{label}</span>
                        <span><strong>{score:.2%}</strong></span>
                    </div>
                    <div class="confidence-bar">
                        <div class="confidence-fill" style="width: {percentage}%"></div>
                    </div>
                </div>
            """
        
        card_html += """
            </div>
        </div>
        """
        
        return card_html
    
    def _generate_statistics(self) -> str:
        """Generate statistics section."""
        # Calculate statistics
        label_predictions = {}
        for label in self.results['labels']:
            label_name = label.replace('type_', '')
            label_predictions[label_name] = 0
        
        for pred in self.results['predictions']:
            for label in pred['predicted_labels']:
                clean_label = label.replace(' (low confidence)', '')
                if clean_label in label_predictions:
                    label_predictions[clean_label] += 1
        
        stats_html = """
        <div class="section">
            <h2 class="section-title">Statistics</h2>
            <div class="stats-grid">
                <div class="stat-card">
                    <h3>Label Distribution</h3>
        """
        
        for label, count in sorted(label_predictions.items(), key=lambda x: x[1], reverse=True):
            percentage = count / self.results['num_samples'] * 100 if self.results['num_samples'] > 0 else 0
            stats_html += f"""
                    <div class="stat-row">
                        <span>{label}</span>
                        <span><strong>{count}</strong> ({percentage:.1f}%)</span>
                    </div>
            """
        
        stats_html += """
                </div>
            </div>
        </div>
        """
        
        return stats_html
    
    def _generate_footer(self) -> str:
        """Generate footer section."""
        return f"""
        <div class="footer">
            <p>Generated by SDPTool Prediction System</p>
            <p style="margin-top: 5px;">Report File: {self.report_filename}</p>
        </div>
        """


# Example usage
if __name__ == "__main__":
    # Example prediction results
    example_results = {
        'model_name': 'RandomForest',
        'model_type': 'traditional_ml',
        'run_id': 'RF_Best_20231205_143022',
        'num_samples': 2,
        'labels': ['type_bug', 'type_enhancement', 'type_documentation', 'type_blocker'],
        'predictions': [
            {
                'sample_index': 0,
                'dataset_row': 5,
                'text': 'Fix critical security vulnerability in authentication module...',
                'predicted_labels': ['bug', 'blocker'],
                'confidence_scores': {
                    'bug': 0.92,
                    'enhancement': 0.15,
                    'documentation': 0.08,
                    'blocker': 0.87
                },
                'ground_truth': ['bug', 'blocker'],
                'exact_match': True,
                'sample_accuracy': 1.0
            },
            {
                'sample_index': 1,
                'dataset_row': 10,
                'text': 'Add new feature for user profile customization...',
                'predicted_labels': ['enhancement'],
                'confidence_scores': {
                    'bug': 0.12,
                    'enhancement': 0.91,
                    'documentation': 0.25,
                    'blocker': 0.03
                },
                'ground_truth': ['enhancement', 'documentation'],
                'exact_match': False,
                'sample_accuracy': 0.5
            }
        ]
    }
    
    generator = PredictionReportGenerator(example_results)
    report_path = generator.generate_report()
    print(f"Example report generated: {report_path}")
