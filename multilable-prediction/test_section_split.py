import re
import sys

# Read the full log
with open(r'C:\wspace\SDPTool\multilable-prediction\output\reports\T8\log.txt', 'r', encoding='utf-8') as f:
    content = f.read()

# Split by sections as the generator does
unbalanced_match = re.search(r'Processing with Unbalanced Data\.(.*?)(?=Processing with Balanced Data\.|All processes completed successfully\.)', content, re.DOTALL)
balanced_match = re.search(r'Processing with Balanced Data\.(.*?)(?=All processes completed successfully\.)', content, re.DOTALL)

print(f"Unbalanced section found: {bool(unbalanced_match)}")
print(f"Balanced section found: {bool(balanced_match)}")

if unbalanced_match:
    section = unbalanced_match.group(1)
    print(f"\nUnbalanced section length: {len(section)} chars")
    
    # Try the error pattern
    error_pattern = r'----- Error Analysis for (\w+) -----\s*\nDetailed Misclassification Analysis for \w+:\s*\n={80,}\s*\n(.*?)(?=\n={80,}\s*\nHamming Loss for \w+:)'
    matches = list(re.finditer(error_pattern, section, re.DOTALL))
    print(f"Error analysis matches in unbalanced section: {len(matches)}")
    for match in matches:
        print(f"  - Model: {match.group(1)}, content length: {len(match.group(2))}")
