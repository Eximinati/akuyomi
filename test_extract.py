import requests
import re
import base64

headers = {
    "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    "Accept-Language": "en-US,en;q=0.5",
    "Referer": "https://animepahe.ch",
}

# Step 1: Fetch episode page
print("=== Step 1: Fetch episode page ===")
r = requests.get("https://animepahe.ch/tomb-raider-king-episode-1-english-subbed/", headers=headers)
print(f"Status: {r.status_code}, Size: {len(r.text)}")

# Step 2: Check if .gov-multipart exists
print("\n=== Step 2: Find .gov-the-embed elements ===")
from bs4 import BeautifulSoup
soup = BeautifulSoup(r.text, 'html.parser')
embeds = soup.select('.gov-multipart .gov-the-embed')
print(f"Found {len(embeds)} embed elements")
for i, el in enumerate(embeds):
    onclick = el.get('onclick', '')
    print(f"  [{i}] onclick starts with: {onclick[:80]}...")
    
    # Step 3: Extract base64
    match = re.search(r"putMi\s*\([^,]+,\s*'([^']+)'", onclick)
    if match:
        b64 = match.group(1)
        print(f"  [{i}] base64 length: {len(b64)}")
        
        # Step 4: Decode base64
        try:
            decoded = base64.b64decode(b64).decode('utf-8')
            print(f"  [{i}] decoded: {decoded[:120]}...")
            
            # Step 5: Extract iframe src
            src_match = re.search(r'src\s*=\s*["\']([^"\']+)["\']', decoded)
            if src_match:
                iframe_src = src_match.group(1)
                print(f"  [{i}] iframe src: {iframe_src}")
                
                # Step 6: Fetch iframe URL
                print(f"  [{i}] Fetching iframe...")
                ir = requests.get(iframe_src, headers=headers, timeout=15)
                print(f"  [{i}] iframe status: {ir.status_code}, Size: {len(ir.text)}")
                
                # Step 7: Search for video URLs
                # Check for HLS manifest
                hls_matches = re.findall(r'"hlsManifestUrl"\s*:\s*"([^"]+)"', ir.text)
                print(f"  [{i}] hlsManifestUrl matches: {len(hls_matches)}")
                for hls in hls_matches:
                    print(f"    HLS: {hls[:100]}...")
                
                # Check for videos array
                vid_matches = re.findall(r'"videos"\s*:\s*\[([\s\S]*?)\]', ir.text)
                print(f"  [{i}] videos array matches: {len(vid_matches)}")
                
                # Check for m3u8 URLs
                m3u8_urls = re.findall(r'https?://[^\s"\'<>]+\.m3u8[^\s"\'<>]*', ir.text)
                print(f"  [{i}] raw m3u8 URLs: {len(m3u8_urls)}")
                
                # Check for JWPlayer
                jw_sources = re.findall(r'sources:\s*\[', ir.text)
                print(f"  [{i}] JWPlayer sources: {len(jw_sources)}")
                
                # Check for video tags
                video_tags = re.findall(r'<video[^>]*src="([^"]+)"', ir.text)
                print(f"  [{i}] video tags: {len(video_tags)}")
                for vt in video_tags:
                    print(f"    Video src: {vt[:100]}")
                
                # Check for iframes in the response
                iframes = re.findall(r'<iframe[^>]+src="([^"]+)"', ir.text)
                print(f"  [{i}] iframes in response: {len(iframes)}")
                for ifr in iframes:
                    print(f"    Iframe: {ifr[:100]}")
        except Exception as e:
            print(f"  [{i}] ERROR: {e}")
    else:
        print(f"  [{i}] No base64 match in onclick")

# Also check #embed_holder iframe
print("\n=== Step Extra: Pre-loaded iframe in #embed_holder ===")
embed_iframe = soup.select_one('#embed_holder iframe')
if embed_iframe:
    src = embed_iframe.get('src', '')
    print(f"Embed iframe src: {src}")
    print(f"Fetching...")
    ir = requests.get(src, headers=headers, timeout=15)
    print(f"Status: {ir.status_code}, Size: {len(ir.text)}")
    hls = re.findall(r'"hlsManifestUrl"\s*:\s*"([^"]+)"', ir.text)
    print(f"hlsManifestUrl: {hls}")
    m3u8 = re.findall(r'https?://[^\s"\'<>]+\.m3u8[^\s"\'<>]*', ir.text)
    print(f"m3u8 URLs: {m3u8}")
else:
    print("No iframe found in #embed_holder")

# Check for download section
print("\n=== Step Extra: Download links ===")
dl_links = soup.select('.dlbox a[href]')
for dl in dl_links:
    print(f"  Download: {dl.get('href')}")
