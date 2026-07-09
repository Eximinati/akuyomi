import re
with open("D:/akuyomi/okru_v2.html", "r", encoding="utf-8") as f:
    html = f.read()

# Test parseHlsManifest with entity-aware regex
hls_regex = r'hlsManifestUrl\s*:\s*(?:\\?&quot;|\")\s*((?:https?://)[^\s&"]+?)(?:\\?&quot;|"|\s|$)'
for m in re.finditer(hls_regex, html):
    url = m.group(1)
    print(f"hlsManifestUrl found:")
    print(f"  raw: {url[:150]}...")
    url = url.replace("\\\\u0026", "&").replace("\\u0026", "&").replace("\\/", "/")
    print(f"  cleaned: {url[:150]}...")

# Test parseOkRuVideos with entity-aware regex
vids_regex = r'videos\s*:\s*\[\s*((?:\{[^}]*\},?\s*)*)\]'
# Simpler: just find the name and url pairs
name_regex = r'name\s*:\s*(?:\\?&quot;|\")([^"\\&]+?)(?:\\?&quot;|")'
url_regex = r'url\s*:\s*(?:\\?&quot;|\")((?:https?://)[^\s"&\\]+?)(?:\\?&quot;|")'

# Find the videos section
videos_section = re.search(r'videos\s*:\s*\[([\s\S]*?)\]', html)
if videos_section:
    print(f"\nvideos section found, length: {len(videos_section.group(1))}")
    names = name_regex.findall(videos_section.group(1))
    urls = url_regex.findall(videos_section.group(1))
    print(f"  names: {names}")
    print(f"  urls: {len(urls)} found")
    for n, u in zip(names, urls[:5]):
        u_clean = u.replace("\\\\u0026", "&").replace("\\u0026", "&").replace("\\/", "/")
        print(f"  {n}: {u_clean[:100]}")
else:
    print("\nNo videos section found")
