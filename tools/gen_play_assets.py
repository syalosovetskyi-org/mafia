#!/usr/bin/env python3
"""Генерация графических ассетов Google Play для приложения «Мафия».

Выход:
  - store/play/icon.png (512x512 PNG)
  - store/play/featureGraphic.png (1024x500 PNG)
  - копии в store/play/metadata/{uk,en-US,ru-RU}/images/
"""

import math
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))
STORE_PLAY = os.path.join(ROOT, "store", "play")
LOCALES = ["uk", "en-US", "ru-RU"]

def create_icon():
    """Иконка для Google Play (512x512, 32-bit PNG)."""
    S = 512
    img = Image.new("RGBA", (S, S), (26, 18, 32, 255))
    draw = ImageDraw.Draw(img)

    # Мягкий радиальный свет в центре
    for r in range(256, 0, -4):
        alpha = int(35 * (1 - r / 256.0))
        draw.ellipse([256 - r, 256 - r, 256 + r, 256 + r], fill=(68, 34, 52, alpha))

    # Тонкая виньетка
    vignette = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    vd = ImageDraw.Draw(vignette)
    vd.ellipse([-50, -50, S + 50, S + 50], outline=(10, 8, 14, 200), width=60)
    vignette = vignette.filter(ImageFilter.GaussianBlur(30))
    img.alpha_composite(vignette)

    # Контурная подсветка (rim light) шляпы
    glow = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    gd.ellipse([100, 240, 412, 360], fill=(210, 70, 85, 120)) # поля
    gd.polygon([(180, 260), (200, 150), (312, 150), (332, 260)], fill=(210, 70, 85, 120)) # тулья
    glow = glow.filter(ImageFilter.GaussianBlur(16))
    img.alpha_composite(glow)

    # Шляпа федора (стиль noir, как в лаунчере и артах)
    hat = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    hd = ImageDraw.Draw(hat)

    # Поля шляпы
    hd.ellipse([110, 246, 402, 340], fill=(232, 220, 200, 255))
    # Тень под тульей
    hd.ellipse([160, 250, 352, 290], fill=(160, 148, 130, 255))

    # Тулья шляпы
    hd.polygon([(194, 258), (208, 156), (304, 156), (318, 258)], fill=(232, 220, 200, 255))
    # Верх тульи с характерной вмятиной
    hd.ellipse([208, 144, 304, 168], fill=(216, 204, 184, 255))
    hd.ellipse([236, 148, 276, 164], fill=(180, 168, 150, 255))

    # Красная лента на шляпе (#B03A48)
    hd.polygon([(197, 234), (202, 210), (310, 210), (315, 234)], fill=(176, 58, 72, 255))
    # Блик на ленте
    hd.line([(220, 212), (235, 232)], fill=(220, 90, 105, 255), width=4)

    hat.alpha_composite(glow)
    img.alpha_composite(hat)

    # Тонкий внутренний бордюр для завершенности
    draw = ImageDraw.Draw(img)
    draw.rounded_rectangle([16, 16, S - 16, S - 16], radius=24, outline=(200, 170, 110, 40), width=2)

    return img.convert("RGB")

def create_feature_graphic():
    """Баннер Google Play (1024x500 PNG)."""
    W, H = 1024, 500
    img = Image.new("RGB", (W, H), (16, 12, 20))
    draw = ImageDraw.Draw(img)

    # Фоновые световые пятна (нуар-атмосфера)
    for r in range(350, 0, -8):
        alpha = int(40 * (1 - r / 350.0))
        # Тёплый красный свет слева (мафия)
        draw.ellipse([180 - r, 250 - r, 180 + r, 250 + r], fill=(int(80 * (1 - r / 350.0)), int(20 * (1 - r / 350.0)), int(28 * (1 - r / 350.0))))
        # Холодный синий свет справа (шериф)
        draw.ellipse([840 - r, 250 - r, 840 + r, 250 + r], fill=(int(18 * (1 - r / 350.0)), int(28 * (1 - r / 350.0)), int(65 * (1 - r / 350.0))))

    overlay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)

    # Силуэт Мафии слева
    # Плечи и плащ
    od.ellipse([60, 280, 340, 560], fill=(28, 24, 30, 255))
    # Голова
    od.ellipse([150, 190, 250, 300], fill=(150, 118, 104, 255))
    # Поля шляпы
    od.ellipse([110, 200, 290, 245], fill=(22, 18, 22, 255))
    # Тулья
    od.polygon([(160, 215), (170, 150), (230, 150), (240, 215)], fill=(22, 18, 22, 255))
    od.rectangle([(163, 200), (237, 212)], fill=(150, 34, 44, 255)) # красная лента
    # Огонёк сигары
    od.ellipse([238, 260, 246, 268], fill=(255, 140, 50, 255))

    # Силуэт Шерифа справа
    od.ellipse([684, 280, 964, 560], fill=(30, 36, 48, 255))
    od.ellipse([774, 190, 874, 300], fill=(190, 155, 135, 255))
    # Фуражка
    od.ellipse([750, 200, 898, 235], fill=(20, 24, 32, 255))
    od.pieslice([760, 145, 888, 245], 180, 360, fill=(34, 42, 58, 255))
    # Звезда шерифа
    cx, cy, r = 824, 175, 14
    pts = []
    for i in range(10):
        ang = -math.pi / 2 + i * math.pi / 5
        rad = r if i % 2 == 0 else r * 0.45
        pts.append((cx + rad * math.cos(ang), cy + rad * math.sin(ang)))
    od.polygon(pts, fill=(232, 190, 90, 255))

    # Золотой жетон на груди
    cx, cy, r = 770, 360, 20
    pts = []
    for i in range(10):
        ang = -math.pi / 2 + i * math.pi / 5
        rad = r if i % 2 == 0 else r * 0.45
        pts.append((cx + rad * math.cos(ang), cy + rad * math.sin(ang)))
    od.polygon(pts, fill=(232, 190, 90, 220))

    img.paste(Image.alpha_composite(Image.new("RGBA", (W, H), (0, 0, 0, 0)), overlay).convert("RGB"), (0, 0), overlay)

    # Виньетка по краям баннера
    vign = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    vd = ImageDraw.Draw(vign)
    for x in range(120):
        a = int(240 * (1 - x / 120.0))
        vd.line([(x, 0), (x, H)], fill=(10, 8, 12, a))
        vd.line([(W - 1 - x, 0), (W - 1 - x, H)], fill=(10, 8, 12, a))
    for y in range(80):
        a = int(220 * (1 - y / 80.0))
        vd.line([(0, y), (W, y)], fill=(10, 8, 12, a))
        vd.line([(0, H - 1 - y), (W, H - 1 - y)], fill=(10, 8, 12, a))
    img.paste(vign.convert("RGB"), (0, 0), vign)

    # Центральная плашка с логотипом / надписью
    cdraw = ImageDraw.Draw(img)

    # Карточка по центру
    card_w, card_h = 360, 280
    cx, cy = W // 2, H // 2
    x1, y1 = cx - card_w // 2, cy - card_h // 2
    x2, y2 = cx + card_w // 2, cy + card_h // 2

    # Полупрозрачная подложка карточки
    card = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    card_d = ImageDraw.Draw(card)
    card_d.rounded_rectangle([x1, y1, x2, y2], radius=18, fill=(20, 16, 26, 230), outline=(214, 172, 84, 180), width=2)
    card_d.rounded_rectangle([x1 + 6, y1 + 6, x2 - 6, y2 - 6], radius=14, outline=(214, 172, 84, 60), width=1)

    # Иконка шляпы вверху карточки
    hat_y = y1 + 52
    card_d.ellipse([cx - 50, hat_y + 12, cx + 50, hat_y + 40], fill=(232, 220, 200, 255))
    card_d.polygon([(cx - 28, hat_y + 18), (cx - 20, hat_y - 20), (cx + 20, hat_y - 20), (cx + 28, hat_y + 18)], fill=(232, 220, 200, 255))
    card_d.polygon([(cx - 25, hat_y + 12), (cx - 21, hat_y + 2), (cx + 21, hat_y + 2), (cx + 25, hat_y + 12)], fill=(176, 58, 72, 255))

    img.paste(card.convert("RGB"), (0, 0), card)

    # Отрисовка текста
    fdraw = ImageDraw.Draw(img)

    # Пробуем загрузить системный шрифт или fallback на default
    font_large = None
    font_sub = None
    for font_path in [
        "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf",
        "/usr/share/fonts/truetype/freefont/FreeSansBold.ttf",
        "/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf",
    ]:
        if os.path.exists(font_path):
            try:
                font_large = ImageFont.truetype(font_path, 46)
                font_sub = ImageFont.truetype(font_path, 15)
                break
            except Exception:
                pass

    title_text = "МАФІЯ"
    sub_text = "ДОДАТОК-ВЕДУЧИЙ • MODERATOR"

    if font_large:
        # Тень текста
        fdraw.text((cx - 78 + 2, y1 + 130 + 2), title_text, font=font_large, fill=(0, 0, 0, 220))
        # Золотой заголовок
        fdraw.text((cx - 78, y1 + 130), title_text, font=font_large, fill=(245, 218, 140))
    else:
        fdraw.text((cx - 30, y1 + 140), "MAFIA", fill=(245, 218, 140))

    if font_sub:
        fdraw.text((cx - 130 + 1, y1 + 196 + 1), sub_text, font=font_sub, fill=(0, 0, 0, 200))
        fdraw.text((cx - 130, y1 + 196), sub_text, font=font_sub, fill=(190, 180, 200))
    else:
        fdraw.text((cx - 50, y1 + 196), "MODERATOR", fill=(190, 180, 200))

    # Золотые разделительные точки
    fdraw.ellipse([cx - 90, y1 + 240, cx - 86, y1 + 244], fill=(214, 172, 84))
    fdraw.ellipse([cx, y1 + 240, cx + 4, y1 + 244], fill=(214, 172, 84))
    fdraw.ellipse([cx + 90, y1 + 240, cx + 94, y1 + 244], fill=(214, 172, 84))

    return img

def main():
    os.makedirs(STORE_PLAY, exist_ok=True)

    print("Генерация icon.png...")
    icon = create_icon()
    icon_path = os.path.join(STORE_PLAY, "icon.png")
    icon.save(icon_path, "PNG", optimize=True)
    print(f"  Сохранено: {icon_path} ({icon.size[0]}x{icon.size[1]}, {os.path.getsize(icon_path) // 1024} KB)")

    print("Генерация featureGraphic.png...")
    feat = create_feature_graphic()
    feat_path = os.path.join(STORE_PLAY, "featureGraphic.png")
    feat.save(feat_path, "PNG", optimize=True)
    print(f"  Сохранено: {feat_path} ({feat.size[0]}x{feat.size[1]}, {os.path.getsize(feat_path) // 1024} KB)")

    # Распространение по локалям
    for loc in LOCALES:
        loc_images = os.path.join(STORE_PLAY, "metadata", loc, "images")
        os.makedirs(loc_images, exist_ok=True)
        loc_icon = os.path.join(loc_images, "icon.png")
        loc_feat = os.path.join(loc_images, "featureGraphic.png")
        icon.save(loc_icon, "PNG", optimize=True)
        feat.save(loc_feat, "PNG", optimize=True)
        print(f"  Скопировано в {loc_images}/")

if __name__ == "__main__":
    main()
