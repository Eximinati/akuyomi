import re
with open("D:/akuyomi/okru_v2.html", "r", encoding="utf-8") as f:
    html = f.read()

# Pattern: hlsManifestUrl + optional quote (backslash + &quot;) + colon + optional quote + URL + closing quote
p1 = r'hlsManifestUrl\\?&quot;\s*:\s*\\?&quot;(https?://.+?)\\?&quot;'
for m in re.finditer(p1, html, re.DOTALL):
    url = m.group(1)
    url_clean = url.replace("\\\\u0026", "&").replace("\\u0026", "&").replace("\\/", "/")
    print(f"hlsManifestUrl URL:")
    print(f"  raw: {url[:120]}")
    print(f"  cleaned: {url_clean[:120]}")
    print()

# Pattern: videos array with name/url
p2 = r'videos\\?&quot;\s*:\s*\[(.*?)\]'
for m in re.finditer(p2, html, re.DOTALL):
    content = m.group(1)
    print(f"videos array found, length: {len(content)}")
    
    # Extract {name, url} objects using simpler pattern
    name_p = r'name\\?&quot;\s*:\s*\\?&quot;([^"\\&]+?)\\?&quot;'
    url_p = r'url\\?&quot;\s*:\s*\\?&quot;(https?://.+?)\\?&quot;'
    
    names = re.findall(name_p, content)
    urls = re.findall(url_p, content)
    
    print(f"  {len(names)} names, {len(urls)} urls")
    for n, u in zip(names, urls):
        u_clean = u.replace("\\\\u0026", "&").replace("\\u0026", "&").replace("\\/", "/")
        print(f"  {n}: {u_clean[:120]}")
