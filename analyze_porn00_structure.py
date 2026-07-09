import re
import json

def analyze(filename, label):
    with open(f'D:\\akuyomi\\{filename}', 'r', encoding='utf-8') as f:
        html = f.read()
    
    print(f'\n{"="*70}')
    print(f'=== {label} ===')
    print(f'{"="*70}')
    
    # Extract page title
    m = re.search(r'<title>([^<]+)</title>', html)
    if m:
        print(f'Title: {m.group(1)}')
    
    # Look for search form
    search_forms = re.findall(r'<form[^>]*search[^>]*>.*?</form>', html, re.DOTALL | re.IGNORECASE)
    if search_forms:
        print(f'Search forms found: {len(search_forms)}')
        for sf in search_forms[:3]:
            action = re.search(r'action=[\"\']([^\"\']+)[\"\']', sf)
            method = re.search(r'method=[\"\']([^\"\']+)[\"\']', sf)
            inputs = re.findall(r'<input[^>]+>', sf)
            print(f'  Action: {action.group(1) if action else "N/A"}')
            print(f'  Method: {method.group(1) if method else "N/A"}')
            print(f'  Inputs: {len(inputs)}')
            for inp in inputs:
                name = re.search(r'name=[\"\']([^\"\']+)[\"\']', inp)
                typ = re.search(r'type=[\"\']([^\"\']+)[\"\']', inp)
                ph = re.search(r'placeholder=[\"\']([^\"\']+)[\"\']', inp)
                print(f'    <input name={name.group(1) if name else "?"} type={typ.group(1) if typ else "?"} placeholder={ph.group(1) if ph else "?"}>')
    
    # Search input
    search_inputs = re.findall(r'<input[^>]*search[^>]*>', html, re.IGNORECASE)
    print(f'Search inputs: {len(search_inputs)}')
    for si in search_inputs:
        print(f'  {si[:200]}')
    
    # Video cards
    video_blocks = re.findall(r'<div[^>]*class=[\"\'][^\"\']*video[^\"\']*[\"\']>.*?</div>\s*(?=<|$)', html, re.DOTALL | re.IGNORECASE)
    print(f'\nVideo blocks (div.video): {len(video_blocks)}')
    
    # Also look for video links with thumbnails
    video_links = re.findall(r'<a\s+href=[\"\'](/video/[^\"\']+)[\"\'][^>]*>.*?</a>', html, re.DOTALL | re.IGNORECASE)
    print(f'Video links (/video/...): {len(video_links)}')
    if video_links:
        print(f'  First: {video_links[0][:100]}')
        print(f'  Last: {video_links[-1][:100]}')
    
    # Thumbnails
    imgs = re.findall(r'<img[^>]*src=[\"\']([^\"\']+)[\"\'][^>]*>', html)
    print(f'\nImages: {len(imgs)}')
    
    # Pagination
    pagination = re.findall(r'<a[^>]*class=[\"\']?[^\"\']*page-link[^\"\']*[\"\']?[^>]*href=[\"\']([^\"\']+)[\"\']>(\d+)</a>', html)
    if not pagination:
        pagination = re.findall(r'class=[\"\']pagination[\"\']', html)
        print(f'Pagination class present: {"YES" if pagination else "NO"}')
        # Look for page number patterns
        page_links = re.findall(r'href=[\"\']([^\"\']*/latest-vids/\d+/[^\"\']*)[\"\']', html)
        if page_links:
            print(f'Page links (latest-vids/N): {len(page_links)}')
            for pl in page_links[:5]:
                print(f'  {pl}')
        # Next/prev
        next_link = re.findall(r'rel=[\"\']next[\"\']\s*href=[\"\']([^\"\']+)[\"\']', html)
        if next_link:
            print(f'Next link (rel=next): {next_link[0]}')
        # Look for "Next" text in links
        next_links = re.findall(r'<a[^>]*href=[\"\']([^\"\']+)[\"\']>[^<]*Next[^<]*</a>', html)
        if next_links:
            print(f'Next text links: {next_links[:3]}')
    else:
        print(f'Pagination links: {pagination[:10]}')
    
    # Page numbers in URL pattern
    page_nums = re.findall(r'/latest-vids/(\d+)/', html)
    if page_nums:
        unique_pages = sorted(set(page_nums), key=int)
        print(f'Page numbers found: {unique_pages[:10]}')
    
    # Video player detection
    print(f'\n--- VIDEO PLAYER ---')
    # iframes
    iframes = re.findall(r'<iframe[^>]+src=[\"\']([^\"\']+)[\"\']', html, re.IGNORECASE)
    print(f'iframes: {len(iframes)}')
    for ifr in iframes[:5]:
        print(f'  {ifr[:150]}')
    
    # video tags
    video_tags = re.findall(r'<video[^>]*>', html, re.IGNORECASE)
    print(f'<video> tags: {len(video_tags)}')
    
    # source tags
    sources = re.findall(r'<source[^>]+src=[\"\']([^\"\']+)[\"\']', html, re.IGNORECASE)
    print(f'<source> tags: {len(sources)}')
    for s in sources[:5]:
        print(f'  {s}')
    
    # Look for video URLs
    mp4s = re.findall(r'(https?://[^\"\'\s<>]+\.mp4[^\"\'\s<>]*)', html, re.IGNORECASE)
    print(f'MP4 URLs: {len(mp4s)}')
    for m in mp4s[:5]:
        print(f'  {m}')
    
    m3u8s = re.findall(r'(https?://[^\"\'\s<>]+\.m3u8[^\"\'\s<>]*)', html, re.IGNORECASE)
    print(f'M3U8 URLs: {len(m3u8s)}')
    for m in m3u8s[:5]:
        print(f'  {m}')
    
    # JavaScript video players
    player_scripts = ['jwplayer', 'flowplayer', 'videojs', 'plyr', 'video.js', 'player']
    for ps in player_scripts:
        if ps in html.lower():
            print(f'Player detected: {ps}')
    
    # Look for player initialization JS
    player_init = re.findall(r'(jwplayer|flowplayer|videojs|plyr)\([^)]*\)\s*\.\s*(setup|play)', html, re.IGNORECASE)
    if player_init:
        print(f'Player init: {player_init}')
    
    # Look for file/config objects in JS
    file_configs = re.findall(r'(?:file|src|url)\s*:\s*[\"\']([^\"\']+(?:mp4|m3u8|webm)[^\"\']*)[\"\']', html, re.IGNORECASE)
    print(f'File/config references: {len(file_configs)}')
    for fc in file_configs[:5]:
        print(f'  {fc[:200]}')
    
    # Look for JSON data in scripts
    scripts = re.findall(r'<script[^>]*>(.*?)</script>', html, re.DOTALL | re.IGNORECASE)
    print(f'\nScript tags: {len(scripts)}')
    for i, s in enumerate(scripts):
        s_stripped = s.strip()
        if len(s_stripped) > 50:
            # Check for JSON-like content
            if s_stripped.startswith('{') or s_stripped.startswith('['):
                print(f'  Script {i}: starts with JSON-like content ({len(s_stripped)} chars)')
                print(f'    First 200: {s_stripped[:200]}')
            # Check for video-related vars
            if any(word in s.lower() for word in ['video', 'player', 'src', 'mp4', 'm3u8', 'source']):
                if len(s_stripped) < 2000:
                    print(f'  Script {i} (video-related, {len(s_stripped)} chars): {s_stripped[:300]}')
    
    # Categories panel in homepage
    ul_categorias = re.findall(r'<ul[^>]*id=[\"\']categorias[\"\']>', html, re.IGNORECASE)
    print(f'\nCategories (ul#categorias): {len(ul_categorias)}')
    
    # Menu structure
    nav_links = re.findall(r'href=[\"\'](/[^\"\']+)[\"\']>\s*([^<]+)\s*</a>', html)
    print(f'\nNav links:')
    for href, text in nav_links[:20]:
        print(f'  {href} -> {text.strip()[:40]}')
    
    print(f'\n{"="*70}')

# Analyze each file
analyze('porn00_homepage.html', 'HOMEPAGE')
analyze('porn00_latest.html', 'LATEST VIDEOS')
analyze('porn00_video.html', 'VIDEO DETAIL PAGE')
analyze('porn00_latest_page2.html', 'LATEST PAGE 2')
analyze('porn00_categories.html', 'CATEGORIES')
analyze('porn00_popular.html', 'POPULAR VIDEOS')
