import re

with open('D:\\akuyomi\\porn00_homepage.html', 'r', encoding='utf-8') as f:
    html = f.read()

# Look for all links - the homepage is a landing page
links = re.findall(r'<a\s+[^>]*href=["\']([^"\']+)["\'][^>]*>([^<]+)</a>', html)
print('=== All links on homepage ===')
for href, text in links:
    print(f'  {text.strip()} -> {href}')

# Search for the actual content
print('\n=== Body content ===')
body = re.search(r'<body[^>]*>(.*)</body>', html, re.DOTALL)
if body:
    content = body.group(1)
    # Remove scripts
    content = re.sub(r'<script[^>]*>.*?</script>', '', content, flags=re.DOTALL)
    content = re.sub(r'<style[^>]*>.*?</style>', '', content, flags=re.DOTALL)
    print(content[:2000])
    
# Check for categories navigation
print('\n=== ID-based searches ===')
ids = ['categorias', 'menu', 'nav', 'categories', 'header']
for id_name in ids:
    matches = re.findall(r'<[^>]+id=["\']' + id_name + '["\'][^>]*>', html, re.IGNORECASE)
    if matches:
        print(f'  id="{id_name}": found')

# Check for the entry link to latest-vids
print('\n=== Entry point check ===')
if '/latest-vids/' in html:
    print('  Has link to /latest-vids/')

# Full video card structure from latest-vids
print('\n\n=== FULL VIDEO CARD STRUCTURE ===')
with open('D:\\akuyomi\\porn00_latest.html', 'r', encoding='utf-8') as f:
    latest = f.read()

# Find the list-videos block
list_videos_match = re.search(r'<div class="list-videos">.*?(<div class="item.*?</div>\s*</div>)\s*</div>\s*<div class="pagination"', latest, re.DOTALL)
if list_videos_match:
    items_block = list_videos_match.group(0)
    print(items_block[:3000])
    print('...[truncated]...')
else:
    # Just find all items
    items = re.findall(r'<div class="item[^"]*"[^>]*>.*?</div>\s*</div>', latest, re.DOTALL)
    print(f'Items: {len(items)}')
    if items:
        print(items[0][:2000])
