import urllib.request
import re
import json
import gzip
from collections import Counter

def fetch_url(url):
    req = urllib.request.Request(url, headers={
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36'
    })
    resp = urllib.request.urlopen(req, timeout=20)
    data = resp.read()
    # Check if gzipped
    if data[:2] == b'\x1f\x8b':
        data = gzip.decompress(data)
    return data.decode('utf-8', errors='replace')

def save_and_analyze(filename, html, label):
    with open(f'D:\\akuyomi\\{filename}', 'w', encoding='utf-8') as f:
        f.write(html)
    print(f'\n{"="*60}')
    print(f'=== {label} ===')
    print(f'HTML length: {len(html)}')
    return html

# 1. Homepage
html = fetch_url('https://web.archive.org/web/20250102033618/https://www.porn00.org/')
save_and_analyze('porn00_homepage.html', html, 'HOMEPAGE (landing)')

# 2. Latest videos listing
html = fetch_url('https://web.archive.org/web/20240917173428/https://www.porn00.org/latest-vids/')
save_and_analyze('porn00_latest.html', html, 'LATEST VIDEOS')

# 3. Video detail page
html = fetch_url('https://web.archive.org/web/20240917061542id_/https://www.porn00.org/video/sedona-reign-awesome-cock-hungry-milf/')
save_and_analyze('porn00_video.html', html, 'VIDEO DETAIL')

# 4. Second page of latest
html = fetch_url('https://web.archive.org/web/20240917173428/https://www.porn00.org/latest-vids/2/')
save_and_analyze('porn00_latest_page2.html', html, 'LATEST VIDEOS PAGE 2')

# 5. Categories
html = fetch_url('https://web.archive.org/web/20240917173428/https://www.porn00.org/categories-list/')
save_and_analyze('porn00_categories.html', html, 'CATEGORIES')

# 6. Popular videos
html = fetch_url('https://web.archive.org/web/20240917173428/https://www.porn00.org/popular-vids/')
save_and_analyze('porn00_popular.html', html, 'POPULAR VIDEOS')

# 7. Search page
html = fetch_url('https://web.archive.org/web/20240917173428/https://www.porn00.org/searching/test/')
save_and_analyze('porn00_search.html', html, 'SEARCH RESULTS')

print('\n\n=== DONE FETCHING ===')
