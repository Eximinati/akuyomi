import urllib.request, re, sys

url = sys.argv[1]
resp = urllib.request.urlopen(url, timeout=30)
html = resp.read().decode('utf-8', errors='replace')

# Show the episode listing area
# Find content area likely containing episode items
idx = html.find('listupd')
if idx >= 0:
    snippet = html[idx:idx+3000]
    print("=== listupd section ===")
    print(snippet)
else:
    # Search for episode keywords
    for m in re.finditer(r'<(div|li)[^>]+class="([^"]*)"[^>]*>.*?\b(?:Ep|episode)\b.*?</\1>', html, re.DOTALL | re.IGNORECASE):
        cls = m.group(2)
        print(f"Class: {cls}")
        print(m.group(0)[:300])
        print("---")
