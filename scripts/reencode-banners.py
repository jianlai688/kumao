"""Second pass: re-encode banner webp with smaller dimensions/quality (same object keys)."""
import io, json, urllib.request
from PIL import Image
import minio_s3 as s3

BASE = "http://127.0.0.1"
BUCKET = "kumao"
mapping = json.load(open(r"D:\kumao\output\image-webp-map.json", encoding="utf-8"))

for m in mapping:
    if "/banner/" not in m["old"]:
        continue
    with urllib.request.urlopen(BASE + m["old"], timeout=60) as r:
        raw = r.read()
    im = Image.open(io.BytesIO(raw)).convert("RGB")
    im.thumbnail((1440, 1440), Image.LANCZOS)
    buf = io.BytesIO()
    im.save(buf, "WEBP", quality=74, method=6)
    data = buf.getvalue()
    s3.put_object(BUCKET, m["object_key"], data, "image/webp")
    print(f"{m['old'].split('/')[-1]}: {len(raw)/1024:.0f}KB -> {m['bytes_after']/1024:.0f}KB -> {len(data)/1024:.0f}KB  {im.size}")
    m["bytes_after"] = len(data)

json.dump(mapping, open(r"D:\kumao\output\image-webp-map.json", "w", encoding="utf-8"), ensure_ascii=False, indent=1)
