from PIL import Image, ImageDraw
import os

OUT = r"D:\kumao\kumao-uniapp\src\static\tabbar"
os.makedirs(OUT, exist_ok=True)

S = 4
BASE = 81
W = 6 * S
NORMAL = (140, 133, 124, 255)
ACTIVE = (245, 180, 91, 255)


def canvas():
    img = Image.new("RGBA", (BASE * S, BASE * S), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def save(img, name):
    img.resize((BASE, BASE), Image.LANCZOS).save(os.path.join(OUT, name))


def compass(color, name):
    img, d = canvas()
    c = BASE * S / 2
    r = 30 * S
    d.ellipse([c - r, c - r, c + r, c + r], outline=color, width=W)
    d.polygon([(c, c - 17 * S), (c + 7 * S, c), (c, c + 17 * S), (c - 7 * S, c)], fill=color)
    save(img, name)


def rank(color, name):
    img, d = canvas()
    bottom = 63 * S
    bars = [(14, 26), (31, 16), (48, 22)]
    for x, top in bars:
        d.rounded_rectangle([x * S, top * S, (x + 8) * S, bottom], radius=4 * S, fill=color)
    save(img, name)


def live(color, name):
    img, d = canvas()
    c = BASE * S / 2
    d.ellipse([c - 4 * S, c - 4 * S, c + 4 * S, c + 4 * S], fill=color)
    for rr in (18, 29):
        box = [c - rr * S, c - rr * S, c + rr * S, c + rr * S]
        d.arc(box, start=-52, end=52, fill=color, width=W)
        d.arc(box, start=128, end=232, fill=color, width=W)
    save(img, name)


def crown(color, name):
    img, d = canvas()
    pts = [(x * S, y * S) for x, y in [(15, 57), (19, 28), (32, 42), (40.5, 24), (49, 42), (62, 28), (66, 57)]]
    d.line(pts + [pts[0]], fill=color, width=W, joint="curve")
    d.line([(17 * S, 63 * S), (64 * S, 63 * S)], fill=color, width=W)
    save(img, name)


def user(color, name):
    img, d = canvas()
    c = BASE * S / 2
    r = 11 * S
    d.ellipse([c - r, 18 * S - r, c + r, 18 * S + r], outline=color, width=W)
    d.arc([11 * S, 40 * S, 70 * S, 92 * S], start=180, end=360, fill=color, width=W)
    save(img, name)


for color, suffix in ((NORMAL, ".png"), (ACTIVE, "-active.png")):
    compass(color, "discover" + suffix)
    rank(color, "rank" + suffix)
    live(color, "live" + suffix)
    crown(color, "vip" + suffix)
    user(color, "mine" + suffix)

print("done")

