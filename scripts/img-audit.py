"""Audit /static image sizes referenced by the DB (local nginx)."""
import sys, urllib.request, collections

TSV = r"D:\kumao\output\image-urls.tsv"
rows = []
seen = {}
for line in open(TSV, encoding="utf-8"):
    line = line.strip()
    if not line:
        continue
    t, c, u = line.split("\t", 2)
    rows.append((t, c, u))
    seen.setdefault(u, []).append(f"{t}.{c}")

def head_size(url):
    req = urllib.request.Request("http://127.0.0.1" + url, method="HEAD")
    try:
        with urllib.request.urlopen(req, timeout=10) as r:
            return int(r.headers.get("Content-Length") or 0), r.headers.get("Content-Type")
    except Exception as e:
        return -1, str(e)

sizes = []
for u in seen:
    if u.lower().endswith((".mp4", ".mkv", ".mp3", ".flv", ".m3u8")):
        continue
    s, ct = head_size(u)
    sizes.append((s, u, ct, seen[u]))

sizes.sort(reverse=True)
total = sum(s for s, *_ in sizes if s > 0)
print(f"{len(sizes)} images, total {total/1048576:.1f} MB")
for s, u, ct, refs in sizes[:25]:
    print(f"{s/1024:9.1f} KB  {ct:<12} {u}  <- {','.join(refs)}")
with open(r"D:\kumao\output\image-sizes.tsv", "w", encoding="utf-8") as f:
    for s, u, ct, refs in sizes:
        f.write(f"{s}\t{u}\t{ct}\t{','.join(refs)}\n")
