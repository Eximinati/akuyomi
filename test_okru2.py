import requests
headers = {"User-Agent": "Mozilla/5.0", "Referer": "https://animepahe.ch/"}
r = requests.get("https://ok.ru/videoembed/15057247865490", headers=headers, timeout=15)
html = r.text
# Save for inspection
with open("D:/akuyomi/okru_v2.html", "w", encoding="utf-8") as f:
    f.write(html)
print(f"Size: {len(html)}")

# Search for hlsManifestUrl with different patterns
import re
for pattern in [r"hlsManifestUrl", r"hls_url", r"m3u8", r"video\.m3u8", r"\.m3u8"]:
    matches = re.findall(pattern, html, re.IGNORECASE)
    print(f"Pattern '{pattern}': {len(matches)} matches")

# Search for "videos" key
for pattern in [r'"videos"\s*:', r'"videoUrl"', r'"manifest"', r'"playlist"', r'"src"']:
    matches = re.findall(pattern, html)
    print(f"Pattern '{pattern}': {len(matches)} matches")

# Look at data attributes
print("\n--- data-movie-options ---")
for m in re.finditer(r'data-movie-options="([^"]*)"', html):
    val = m.group(1)
    print(f"length: {len(val)}, starts: {val[:200]}")

print("\n--- data-options ---")
for m in re.finditer(r'data-options="([^"]*)"', html):
    val = m.group(1)
    print(f"length: {len(val)}, starts: {val[:200]}")

# Check what's in flashvars
print("\n--- flashvars ---")
for m in re.finditer(r'flashvars', html):
    start = max(0, m.start() - 50)
    end = min(len(html), m.end() + 100)
    print(html[start:end])
    print("---")
