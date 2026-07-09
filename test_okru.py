import requests, re
headers = {"User-Agent": "Mozilla/5.0", "Referer": "https://animepahe.ch/"}
r = requests.get("https://ok.ru/videoembed/15057247865490", headers=headers, timeout=15)
print(f"Status: {r.status_code}, Size: {len(r.text)}")
hls = re.findall(r'"hlsManifestUrl"\s*:\s*"([^"]+)"', r.text)
print(f"hlsManifestUrl: {len(hls)}")
for h in hls:
    print(f"  {h[:200]}")
vids = re.findall(r'"videos"\s*:\s*\[([\s\S]*?)\]', r.text)
print(f"videos arrays: {len(vids)}")
if vids:
    names = re.findall(r'"name"\s*:\s*"([^"]+)"', vids[0])
    urls = re.findall(r'"url"\s*:\s*"([^"]+)"', vids[0])
    print(f"  quality levels: {names}")
    for n, u in zip(names, urls):
        print(f"    {n}: {u[:100]}...")
