import re

# Read a portion of the log
with open(r'C:\wspace\SDPTool\multilable-prediction\output\reports\T8\log.txt', 'r', encoding='utf-8') as f:
    content = f.read()

# Test the pattern
error_pattern = r'----- Error Analysis for (\w+) -----\s*\nDetailed Misclassification Analysis for \w+:\s*\n={80,}\s*\n(.*?)(?=\n={80,}\s*\nHamming Loss for \w+:)'

matches = list(re.finditer(error_pattern, content, re.DOTALL))
print(f"Found {len(matches)} matches")

for i, match in enumerate(matches[:2], 1):
    print(f"\n=== Match {i} ===")
    print(f"Model: {match.group(1)}")
    print(f"Content length: {len(match.group(2))} chars")
    print(f"First 200 chars of content:\n{match.group(2)[:200]}")
