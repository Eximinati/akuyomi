import re

# Analyze the latest videos page for video card structure
with open('D:\\akuyomi\\porn00_latest.html', 'r', encoding='utf-8') as f:
    html = f.read()

# Find all items (video cards)
items = re.findall(r'<div class="item[^"]*"[^>]*>.*?</div>\s*</div>\s*</div>\s*</div>', html, re.DOTALL)
print(f'Items found: {len(items)}')

# Extract full first item
first_item_match = re.search(r'<div class="item[^"]*"[^>]*>.*?</div>\s*</div>\s*(?=<|\s)', html, re.DOTALL)
if first_item_match:
    print('\n=== First item (full) ===')
    print(first_item_match.group(0))

# Find the list-videos container
list_videos = re.search(r'<div class="list-videos">.*?</div>\s*<!--.*?list-videos', html, re.DOTALL)
if list_videos:
    content = list_videos.group(0)
    print(f'\nList videos length: {len(content)}')

# Find navigation URLs
nav_urls = re.findall(r'<a[^>]*href=["\']([^"\']*latest-vids/\d+/[^"\']*)["\']', html)
print(f'\nPagination URLs: {len(nav_urls)}')
for nu in nav_urls[:10]:
    print(f'  {nu}')

# Find categories on the categories page
print('\n\n=== CATEGORIES PAGE ===')
with open('D:\\akuyomi\\porn00_categories.html', 'r', encoding='utf-8') as f:
    cats_html = f.read()

# Category links
cat_links = re.findall(r'<a[^>]*href=["\']([^"\']+/category-name/[^"\']+)["\'][^>]*>(.*?)</a>', cats_html, re.DOTALL)
print(f'Category links: {len(cat_links)}')
for href, text in cat_links[:10]:
    clean_text = re.sub(r'<[^>]+>', '', text).strip()
    print(f'  {clean_text} -> {href}')

# Also find the first 3 video cards in categories page  
video_items = re.findall(r'<div class="item[^"]*"[^>]*>.*?</div>\s*</div>\s*(?=<|\s)', cats_html, re.DOTALL)
print(f'\nVideo items in categories page: {len(video_items)}')

# Pornstars page
print('\n\n=== POPULAR VIDEOS PAGE ===')
with open('D:\\akuyomi\\porn00_popular.html', 'r', encoding='utf-8') as f:
    pop_html = f.read()

# Check for popular-vids pagination
pop_pages = re.findall(r'href=["\']([^"\']*popular-vids/\d+/[^"\']*)["\']', pop_html)
print(f'Popular pages: {len(pop_pages)}')
for pp in pop_pages[:5]:
    print(f'  {pp}')
