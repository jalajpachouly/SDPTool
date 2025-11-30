import re
import json

# Simulate the extraction
report_data = {'unbalanced': {}, 'balanced': {}}

with open(r'C:\wspace\SDPTool\multilable-prediction\output\reports\T8\log.txt', 'r', encoding='utf-8') as f:
    content = f.read()

# Split sections
unbalanced_match = re.search(r'Processing with Unbalanced Data\.(.*?)(?=Processing with Balanced Data\.|All processes completed successfully\.)', content, re.DOTALL)

section = unbalanced_match.group(1)
key = 'unbalanced'

report_data[key]['error_analysis'] = {}

# Pattern to find error analysis sections
error_pattern = r'----- Error Analysis for (\w+) -----\s*\nDetailed Misclassification Analysis for \w+:\s*\n={80,}\s*\n(.*?)(?=\n={80,}\s*\nHamming Loss for \w+:)'

for error_match in re.finditer(error_pattern, section, re.DOTALL):
    model_name = error_match.group(1)
    error_section = error_match.group(2)
    
    print(f"\n=== Processing {model_name} ===")
    print(f"Error section length: {len(error_section)} chars")
    
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
        print(f"Overall stats: {model_errors['overall']}")
    else:
        print("Overall stats: NOT FOUND")
    
    report_data[key]['error_analysis'][model_name] = model_errors

print(f"\n=== Final Data Structure ===")
print(json.dumps(report_data, indent=2))
