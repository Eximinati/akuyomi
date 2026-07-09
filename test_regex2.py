import re
with open("D:/akuyomi/okru_v2.html", "r", encoding="utf-8") as f:
    html = f.read()

# Find the exact hlsManifestUrl location and show surrounding chars with hex
idx = html.find("hlsManifestUrl")
if idx >= 0:
    segment = html[idx-10:idx+200]
    print("hlsManifestUrl context (repr):")
    print(repr(segment))
    print("\nChar by char around the key:")
    for i, ch in enumerate(segment):
        if ch in ('"', '&', '\\', ':', ' ', '\n', '\r', '\t'):
            print(f"  [{i}] U+{ord(ch):04X} '{ch}'")

# Check for &quot; near hlsManifestUrl
before = html[idx-30:idx]
print(f"\nBefore hlsManifestUrl: {repr(before)}")

# Check what's between hlsManifestUrl and the URL
after = html[idx+len("hlsManifestUrl"):idx+len("hlsManifestUrl")+60]
print(f"\nAfter hlsManifestUrl: {repr(after)}")

# Simpler: find the video.m3u8 URL
m3u8_idx = html.find("video.m3u8")
if m3u8_idx >= 0:
    print(f"\n\nm3u8 at offset {m3u8_idx}")
    print(repr(html[m3u8_idx-20:m3u8_idx+200]))
