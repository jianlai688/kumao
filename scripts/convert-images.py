"""Convert oversized /static images to WebP, upload to MinIO, emit SQL to repoint DB rows."""
import io, json, os, sys, urllib.request
from PIL import Image

import minio_s3 as s3

BASE = "http://127.0.0.1"
BUCKET = "kumao"
TSV = r"D:\kumao\output\image-sizes.tsv"
OUT_JSON = r"D:\kumao\output\image-webp-map.json"
OUT_SQL = r"D:\kumao\output\image-webp-update.sql"

MIN_BYTES = 60 * 1024          # only bother above 60 KB
MAX_SIDE = {
    "avatar": 512,
    "cover": 800,
    "banner": 1920,
    "ad-cover": 800,
}
DEFAULT_MAX = 1200
QUALITY = 82


def target_side(url):
    for k, v in MAX_SIDE.items():
        if f"/{k}/" in url:
            return v
    return DEFAULT_MAX


def main(apply=False):
    rows = []
    for line in open(TSV, encoding="utf-8"):
        parts = line.rstrip("\n").split("\t")
        if len(parts) < 4:
            continue
        size, url, ctype, refs = int(parts[0]), parts[1], parts[2], parts[3]
        if size < MIN_BYTES:
            continue
        if not url.lower().endswith((".png", ".jpg", ".jpeg")):
            continue
        if "/id-card/" in url or "/lyric/" in url:
            continue
        rows.append((size, url, refs))

    print(f"{len(rows)} images selected, {sum(r[0] for r in rows)/1048576:.1f} MB")
    mapping, updates = [], []
    total_before = total_after = 0

    for size, url, refs in rows:
        try:
            with urllib.request.urlopen(BASE + url, timeout=60) as r:
                raw = r.read()
        except Exception as e:
            print(f"  !! download failed {url}: {e}")
            continue

        try:
            im = Image.open(io.BytesIO(raw))
            im.load()
        except Exception as e:
            print(f"  !! decode failed {url}: {e}")
            continue

        before = im.size
        side = target_side(url)
        if max(im.size) > side:
            im.thumbnail((side, side), Image.LANCZOS)
        if im.mode not in ("RGB", "RGBA"):
            im = im.convert("RGBA" if "A" in im.getbands() else "RGB")
        if im.mode == "RGBA":
            bg = Image.new("RGB", im.size, (255, 255, 255))
            bg.paste(im, mask=im.split()[-1])
            im = bg

        buf = io.BytesIO()
        im.save(buf, "WEBP", quality=QUALITY, method=6)
        data = buf.getvalue()

        key = url[len("/static/" + BUCKET + "/"):]
        new_key = os.path.splitext(key)[0] + ".webp"
        new_url = "/static/" + BUCKET + "/" + new_key

        total_before += len(raw)
        total_after += len(data)
        print(f"{(len(raw)/1024):8.1f} KB -> {(len(data)/1024):7.1f} KB  {before} -> {im.size}  {url}")

        if apply:
            status, _ = s3.put_object(BUCKET, new_key, data, "image/webp")
            if status != 200:
                print(f"  !! upload failed {status} {new_key}")
                continue

        mapping.append({"old": url, "new": new_url, "object_key": new_key,
                        "bytes_before": len(raw), "bytes_after": len(data)})

        for ref in refs.split(","):
            t, c = ref.split(".")
            updates.append((t, c, url, new_url))

    print(f"\nTOTAL {total_before/1048576:.2f} MB -> {total_after/1048576:.2f} MB "
          f"({100*total_after/max(total_before,1):.1f}%)")

    if not apply:
        return

    json.dump(mapping, open(OUT_JSON, "w", encoding="utf-8"), ensure_ascii=False, indent=1)

    with open(OUT_SQL, "w", encoding="utf-8") as f:
        for t, c, old, new in updates:
            if t == "file_record" and c == "url":
                continue
            f.write(f"UPDATE `{t}` SET `{c}`='{new}' WHERE `{c}`='{old}';\n")
        for m in mapping:
            old, new = m["old"], m["new"]
            key = m["object_key"]
            size = m["bytes_after"]
            f.write(
                "UPDATE `file_record` SET "
                f"`url`='{new}', `object_key`='{key}', `content_type`='image/webp', `size`={size} "
                f"WHERE `url`='{old}';\n"
            )
    print(f"wrote {OUT_JSON} and {OUT_SQL} ({len(updates)} refs)")


if __name__ == "__main__":
    main(apply="--apply" in sys.argv)
