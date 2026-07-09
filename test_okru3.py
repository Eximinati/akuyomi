import re
with open("D:/akuyomi/okru_v2.html", "r", encoding="utf-8") as f:
    html = f.read()

# Find hlsManifestUrl context
idx = html.find("hlsManifestUrl")
if idx >= 0:
    print("hlsManifestUrl context:")
    print(repr(html[idx-50:idx+200]))

# Check for videos array
for pattern in ['"videos"', "&quot;videos&quot;", "videos"]:
    idx2 = html.find(pattern)
    if idx2 >= 0:
        print(f"\nFound '{pattern}' at {idx2}:")
        print(repr(html[idx2-30:idx2+120]))
        break

# Find raw video URLs (m3u8, mp4) in the HTML
import re
for m in re.finditer(r"https?://[^\s\"\'<&]+\.(?:m3u8|mp4)[^\s\"\'<&]*", html):
    print(f"\nRaw video URL: {m.group()[:200]}")
