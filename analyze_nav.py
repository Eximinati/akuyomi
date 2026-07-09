import re, sys

def safe(text):
    return text.encode('ascii', 'replace').decode()

with open('D:\\akuyomi\\porn00_latest.html', 'r', encoding='utf-8') as f:
    html = f.read()

# Find navigation menu
print('=== Navigation menu links ===')
nav_links = re.findall(r'<li[^>]*>.*?<a\s+href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html, re.DOTALL)
for href, text in nav_links[:20]:
    clean_text = re.sub(r'<[^>]+>', '', text).strip()
    if clean_text:
        print(f'  {safe(clean_text)[:50]} -> {href}')

# Find all top-level links
print('\n=== Nav bar links ===')
nav_bar = re.search(r'<ul[^>]*class=["\'].*?nav.*?["\'][^>]*>.*?</ul>', html, re.DOTALL | re.IGNORECASE)
if nav_bar:
    links = re.findall(r'<a\s+href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', nav_bar.group(0), re.DOTALL)
    for href, text in links[:20]:
        clean_text = re.sub(r'<[^>]+>', '', text).strip()
        if clean_text:
            print(f'  {safe(clean_text)[:50]} -> {href}')

# Links with video/category/star patterns
print('\n=== Content links ===')
all_links = re.findall(r'href=["\']([^"\']+)["\']', html)
unique_links = sorted(set(all_links))
for link in unique_links:
    if any(x in link for x in ['/video/', '/category-name/', '/star-name/', '/tags-name/', '/latest-vids/', '/popular-vids/', '/top-vids/', '/searching/', '/videos-all/', '/categories-list/', '/pornstars-list/', '/tags-list/']):
        print(f'  {link}')

# Pagination block
print('\n=== Pagination block ===')
page_refs = re.findall(r'href=["\']([^"\']*/latest-vids/(\d+)/)["\']', html)
for ref, num in page_refs[:15]:
    print(f'  Page {num}: {ref}')

# Search form
print('\n=== Search form ===')
search_form = re.search(r'<form[^>]*action=["\']([^"\']+)["\'][^>]*>.*?<input[^>]*name=["\']([^"\']+)["\'][^>]*>.*?</form>', html, re.DOTALL)
if search_form:
    print(f'  Action: {search_form.group(1)}')
    print(f'  Input name: {search_form.group(2)}')
