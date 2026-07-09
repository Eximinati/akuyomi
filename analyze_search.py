import re

with open('D:\\akuyomi\\porn00_latest.html', 'r', encoding='utf-8') as f:
    html = f.read()

# Find site search (not Wayback toolbar)
# The site has: <input type="text" name="q" placeholder="Search" value=""/>
# Find the form containing this
search_forms = re.findall(r'<form[^>]*>(.*?)</form>', html, re.DOTALL)
print('=== Site search forms ===')
for sf in search_forms:
    if 'q' in sf and 'Search' in sf:
        # This is likely the site search
        action = re.search(r'action=["\']([^"\']+)["\']', sf)
        method = re.search(r'method=["\']([^"\']+)["\']', sf)
        print(f'Action: {action.group(1) if action else "N/A"}')
        print(f'Method: {method.group(1) if method else "N/A"}')
        print(f'HTML: {sf[:300]}')

# Also look for simple search input
search_inputs = re.findall(r'<input[^>]*name=["\']q["\'][^>]*>', html)
for si in search_inputs:
    print(f'\nSearch input: {si}')
    # Get enclosing form
    idx = html.find(si)
    before = html[max(0,idx-500):idx]
    form_start = before.rfind('<form')
    if form_start >= 0:
        form_html = html[form_start:idx+len(si)+100]
        print(f'Enclosing form start: {form_html[:300]}')

# Most importantly - check the actual working URL
# From the Wayback search result earlier: 
# https://www.porn00.org/searching/Vid%C3%A9os
# This suggests the search URL pattern is /searching/{query}/
# But there's also a form with action /searching/ and input name q

print('\n=== Search URL patterns ===')
# Search for searching in path
search_refs = re.findall(r'href=["\']([^"\']*/searching[^"\']*)["\']', html)
for sr in search_refs:
    print(f'  {sr}')

# Check video detail page for search action
with open('D:\\akuyomi\\porn00_video.html', 'r', encoding='utf-8') as f:
    vhtml = f.read()

search_forms_v = re.findall(r'<form[^>]*>(.*?)</form>', vhtml, re.DOTALL)
print('\n=== Video page - search forms ===')
for sf in search_forms_v:
    if 'q' in sf or 'search' in sf.lower():
        action = re.search(r'action=["\']([^"\']+)["\']', sf)
        method = re.search(r'method=["\']([^"\']+)["\']', sf)
        print(f'Action: {action.group(1) if action else "N/A"}')
        print(f'Method: {method.group(1) if method else "N/A"}')
        print(f'HTML: {sf[:300]}')
        print()
