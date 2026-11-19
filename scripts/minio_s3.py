"""Minimal S3 client (SigV4) for MinIO - stdlib only."""
import hashlib, hmac, datetime, urllib.request, urllib.parse

HOST = "127.0.0.1:9000"
ENDPOINT = f"http://{HOST}"
ACCESS = "minioadmin"
SECRET = "minioadmin"
REGION = "us-east-1"
SERVICE = "s3"


def _sign(method, path, body=b"", content_type=None, extra_headers=None):
    # path like /kumao/a/b.webp
    canonical_uri = urllib.parse.quote(path, safe="/~")
    payload_hash = hashlib.sha256(body).hexdigest()
    now = datetime.datetime.now(datetime.timezone.utc)
    amzdate = now.strftime("%Y%m%dT%H%M%SZ")
    datestamp = now.strftime("%Y%m%d")

    headers = {"host": HOST, "x-amz-content-sha256": payload_hash, "x-amz-date": amzdate}
    if content_type:
        headers["content-type"] = content_type
    if extra_headers:
        headers.update({k.lower(): v for k, v in extra_headers.items()})

    items = sorted(headers.items())
    canonical_headers = "".join(f"{k}:{v}\n" for k, v in items)
    signed_headers = ";".join(k for k, _ in items)

    canonical_request = "\n".join(
        [method, canonical_uri, "", canonical_headers, signed_headers, payload_hash]
    )
    scope = f"{datestamp}/{REGION}/{SERVICE}/aws4_request"
    string_to_sign = "\n".join(
        ["AWS4-HMAC-SHA256", amzdate, scope, hashlib.sha256(canonical_request.encode()).hexdigest()]
    )
    k = ("AWS4" + SECRET).encode()
    for msg in (datestamp, REGION, SERVICE, "aws4_request"):
        k = hmac.new(k, msg.encode(), hashlib.sha256).digest()
    signature = hmac.new(k, string_to_sign.encode(), hashlib.sha256).hexdigest()

    auth = (
        f"AWS4-HMAC-SHA256 Credential={ACCESS}/{scope}, "
        f"SignedHeaders={signed_headers}, Signature={signature}"
    )
    out = {k: v for k, v in headers.items() if k != "host"}
    out["Authorization"] = auth
    out["Host"] = HOST
    return out


def put_object(bucket, key, data, content_type="application/octet-stream"):
    path = f"/{bucket}/{key}"
    headers = _sign("PUT", path, data, content_type)
    req = urllib.request.Request(ENDPOINT + path, data=data, headers=headers, method="PUT")
    with urllib.request.urlopen(req, timeout=60) as r:
        return r.status, r.headers.get("ETag")


def head_object(bucket, key):
    path = f"/{bucket}/{key}"
    headers = _sign("HEAD", path)
    req = urllib.request.Request(ENDPOINT + path, headers=headers, method="HEAD")
    with urllib.request.urlopen(req, timeout=30) as r:
        return r.status, dict(r.headers)
