"""
Generates the Play Store feature graphic for ScholarSync.
Output: play-store-assets/feature-graphic.png (1024x500, RGB PNG).
"""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageFont

WIDTH, HEIGHT = 1024, 500

# Brand palette
NAVY = (30, 58, 95)         # #1E3A5F
NAVY_DEEP = (12, 24, 45)    # near-black navy
TEAL = (45, 212, 191)
GOLD = (245, 158, 11)
WHITE = (255, 255, 255)

OUT = Path(__file__).parent / "feature-graphic.png"
ICON_SRC = Path(__file__).parent.parent / "easyappicon-icons-1774672858562" / "android" / "playstore-icon.png"


def diagonal_gradient(w, h, top_left, bottom_right):
    img = Image.new("RGB", (w, h))
    px = img.load()
    max_d = w + h
    for y in range(h):
        for x in range(w):
            t = (x + y) / max_d
            px[x, y] = (
                int(top_left[0] * (1 - t) + bottom_right[0] * t),
                int(top_left[1] * (1 - t) + bottom_right[1] * t),
                int(top_left[2] * (1 - t) + bottom_right[2] * t),
            )
    return img


def load_font(size, bold=False):
    bold_paths = [
        "C:/Windows/Fonts/segoeuib.ttf",
        "C:/Windows/Fonts/arialbd.ttf",
    ]
    regular_paths = [
        "C:/Windows/Fonts/segoeui.ttf",
        "C:/Windows/Fonts/arial.ttf",
    ]
    for path in (bold_paths if bold else regular_paths):
        try:
            return ImageFont.truetype(path, size)
        except OSError:
            continue
    return ImageFont.load_default()


def draw_glow_circle(base, cx, cy, radius, color, alpha=60, blur=40):
    layer = Image.new("RGBA", base.size, (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    d.ellipse((cx - radius, cy - radius, cx + radius, cy + radius), fill=(*color, alpha))
    layer = layer.filter(ImageFilter.GaussianBlur(blur))
    base.alpha_composite(layer)


def draw_paper_card(base, x, y, w, h, accent_color, lines=4):
    """Draw a faux paper-card mockup at angle 0 (we tilt by paste later if needed)."""
    card = Image.new("RGBA", (w + 40, h + 40), (0, 0, 0, 0))
    d = ImageDraw.Draw(card)

    # Shadow
    shadow = Image.new("RGBA", card.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(shadow)
    sd.rounded_rectangle((20, 24, 20 + w, 24 + h), radius=18, fill=(0, 0, 0, 110))
    shadow = shadow.filter(ImageFilter.GaussianBlur(14))
    card.alpha_composite(shadow)

    # Card body
    d.rounded_rectangle((20, 20, 20 + w, 20 + h), radius=18, fill=(255, 255, 255, 245))

    # Accent strip (left edge)
    d.rounded_rectangle((20, 20, 28, 20 + h), radius=4, fill=accent_color)

    # Title bar
    d.rounded_rectangle((44, 38, 20 + w - 20, 56), radius=4, fill=(30, 58, 95, 230))

    # Author / meta line
    d.rounded_rectangle((44, 66, 20 + w - 80, 78), radius=3, fill=(180, 195, 215, 230))

    # Body text lines
    line_y = 96
    for i in range(lines):
        line_w = w - 50 - (i * 12 if i == lines - 1 else 0)
        d.rounded_rectangle((44, line_y, 44 + line_w - 30, line_y + 8), radius=3, fill=(200, 210, 225, 220))
        line_y += 18

    # AI summary chip
    chip_y = 20 + h - 32
    d.rounded_rectangle((44, chip_y, 44 + 96, chip_y + 22), radius=11, fill=(45, 212, 191, 230))
    chip_font = load_font(13, bold=True)
    d.text((54, chip_y + 3), "AI Summary", font=chip_font, fill=(15, 32, 55))

    base.alpha_composite(card, (x - 20, y - 20))


def main():
    # Background diagonal gradient navy → deep navy
    bg = diagonal_gradient(WIDTH, HEIGHT, NAVY, NAVY_DEEP).convert("RGBA")

    # Atmospheric glows
    draw_glow_circle(bg, 120, 100, 200, TEAL, alpha=55, blur=70)
    draw_glow_circle(bg, WIDTH - 180, HEIGHT - 80, 220, GOLD, alpha=45, blur=80)
    draw_glow_circle(bg, WIDTH // 2 + 80, 60, 140, (90, 140, 220), alpha=70, blur=60)

    draw = ImageDraw.Draw(bg)

    # ---- Decorative dotted grid (top-right corner) ----
    dot_color = (255, 255, 255, 35)
    for gx in range(WIDTH - 280, WIDTH - 20, 22):
        for gy in range(20, 160, 22):
            draw.ellipse((gx, gy, gx + 3, gy + 3), fill=dot_color)

    # ---- Left side text ----
    text_x = 60

    # Small label pill
    pill_font = load_font(15, bold=True)
    pill_text = "FOR RESEARCHERS  •  POWERED BY AI"
    pill_bbox = draw.textbbox((0, 0), pill_text, font=pill_font)
    pill_w = pill_bbox[2] - pill_bbox[0] + 28
    pill_h = 30
    draw.rounded_rectangle(
        (text_x, 70, text_x + pill_w, 70 + pill_h),
        radius=15,
        outline=(45, 212, 191, 200),
        width=2,
        fill=(45, 212, 191, 30),
    )
    draw.text((text_x + 14, 76), pill_text, font=pill_font, fill=TEAL)

    # Headline
    title_font = load_font(82, bold=True)
    draw.text((text_x, 118), "ScholarSync", font=title_font, fill=WHITE)

    # Subhead — two-tone
    sub_font = load_font(38, bold=True)
    draw.text((text_x, 232), "Discover research", font=sub_font, fill=(220, 230, 245))
    # second line
    draw.text((text_x, 278), "that matters", font=sub_font, fill=TEAL)
    # measure "that matters" to draw gold underline
    tm_bbox = draw.textbbox((text_x, 278), "that matters", font=sub_font)
    draw.rectangle((tm_bbox[0], tm_bbox[3] - 2, tm_bbox[2], tm_bbox[3] + 4), fill=GOLD)

    # Feature row with bullets
    feat_font = load_font(19, bold=False)
    feats = ["AI summaries", "Smart alerts", "Personalized feed"]
    fx = text_x
    fy = 372
    for i, f in enumerate(feats):
        # bullet dot
        draw.ellipse((fx, fy + 9, fx + 8, fy + 17), fill=GOLD if i == 0 else TEAL if i == 1 else (220, 230, 245))
        draw.text((fx + 16, fy), f, font=feat_font, fill=(220, 230, 245))
        bbox = draw.textbbox((fx + 16, fy), f, font=feat_font)
        fx = bbox[2] + 26

    # ---- Right side: stacked paper cards + app icon ----
    # Two faux paper cards behind the icon for depth
    draw_paper_card(bg, WIDTH - 360, 70, 240, 200, GOLD)
    draw_paper_card(bg, WIDTH - 280, 250, 240, 180, TEAL)

    # Icon card on top
    card_size = 200
    card_x = WIDTH - card_size - 90
    card_y = (HEIGHT - card_size) // 2 - 10

    # Glow ring around icon card
    glow = Image.new("RGBA", (card_size + 120, card_size + 120), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.rounded_rectangle((20, 20, card_size + 100, card_size + 100), radius=70, fill=(45, 212, 191, 70))
    glow = glow.filter(ImageFilter.GaussianBlur(20))
    bg.alpha_composite(glow, (card_x - 60, card_y - 60))

    # White rounded card
    card_layer = Image.new("RGBA", bg.size, (0, 0, 0, 0))
    cd = ImageDraw.Draw(card_layer)
    cd.rounded_rectangle(
        (card_x, card_y, card_x + card_size, card_y + card_size),
        radius=44,
        fill=WHITE,
    )
    bg.alpha_composite(card_layer)

    if ICON_SRC.exists():
        icon = Image.open(ICON_SRC).convert("RGBA")
        icon_target = card_size - 50
        icon = icon.resize((icon_target, icon_target), Image.LANCZOS)
        ix = card_x + (card_size - icon_target) // 2
        iy = card_y + (card_size - icon_target) // 2
        bg.alpha_composite(icon, (ix, iy))

    # Bottom-left corner: gold accent stripe
    draw.rectangle((0, HEIGHT - 6, 220, HEIGHT), fill=GOLD)

    OUT.parent.mkdir(parents=True, exist_ok=True)
    bg.convert("RGB").save(OUT, "PNG", optimize=True)
    print(f"Wrote {OUT} ({WIDTH}x{HEIGHT})")


if __name__ == "__main__":
    main()
