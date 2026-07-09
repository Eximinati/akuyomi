import re

with open('D:\\akuyomi\\porn00_video.html', 'r', encoding='utf-8') as f:
    html = f.read()

# Find the pageContext variable
print("=== pageContext ===")
m = re.search(r'var pageContext\s*=\s*({[^;]+})', html)
if m:
    print(m.group(1))

# Find all MP4 references
print("\n=== All MP4 references ===")
mp4s = re.findall(r'[^"\' ]*\.mp4[^"\' ]*', html)
for mp4 in mp4s:
    print(f'  {mp4}')

# Find get_file references  
print("\n=== All get_file references ===")
get_files = re.findall(r'[^"\' ]*get_file[^"\' ]*', html)
for gf in get_files:
    print(f'  {gf}')

# Find contents/videos_screenshots references
print("\n=== Screenshot references ===")
screenshots = re.findall(r'[^"\' ]*contents/videos_screenshots[^"\' ]*', html)
for ss in screenshots:
    print(f'  {ss}')

# Find all script tags that might contain player data
print("\n=== Relevant scripts ===")
scripts = re.findall(r'<script[^>]*>(.*?)</script>', html, re.DOTALL | re.IGNORECASE)
for i, sc in enumerate(scripts):
    stripped = sc.strip()
    if any(x in stripped.lower() for x in ['get_file', 'player', 'video', 'mp4', 'source', 'pagecontext', 'jwplayer', 'flowplayer', 'videojs']):
        print(f'\n--- Script {i} (len={len(stripped)}) ---')
        # Truncate to 1000 chars for readability
        if len(stripped) > 1500:
            print(stripped[:1500])
            print('...[truncated]...')
            print(stripped[-500:])
        else:
            print(stripped)

# Find the video player div
print("\n\n=== Player div ===")
player_divs = re.findall(r'<div[^>]*player[^>]*>', html, re.IGNORECASE)
for pd in player_divs:
    print(f'  {pd[:300]}')
    # Get surrounding context
    idx = html.find(pd)
    if idx > 0:
        ctx = html[idx:idx+2000]
        print(f'  Context: {ctx[:500]}')

# Find video containers for latest page
print("\n\n=== Video card structure ===")
with open('D:\\akuyomi\\porn00_latest.html', 'r', encoding='utf-8') as f:
    latest_html = f.read()

# Find video cards
video_cards = re.findall(r'<div[^>]*class="[^"]*video[^"]*"[^>]*>.*?</div>\s*', latest_html, re.DOTALL | re.IGNORECASE)
print(f'Number of video cards found: {len(video_cards)}')

if video_cards:
    # Show first video card in detail
    print('\nFirst video card HTML:')
    print(video_cards[0][:1500])
    print('...[truncated]...')

    # Show second card for comparison
    if len(video_cards) > 1:
        print('\nSecond video card HTML:')
        print(video_cards[1][:1500])
        print('...[truncated]...')

# Also look for the video links
print("\n\n=== Video links ===")
video_links = re.findall(r'<a\s+href=["\']([^"\']+/video/[^"\']+)["\']', latest_html)
for vl in video_links[:3]:
    print(f'  {vl}')

# Find the actual card container
card_containers = re.findall(r'class=["\']video["\']', latest_html)
print(f'\nclass="video" occurrences: {len(card_containers)}')

# Look for thumbnail and title  
thumbnails = re.findall(r'<img[^>]*src=["\'](https://[^"\']+\.jpg)["\']', latest_html)
print(f'\nThumbnails: {len(thumbnails)}')
for t in thumbnails[:5]:
    print(f'  {t}')
