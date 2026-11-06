"""Верификация ассетов Google Play для приложения «Мафия»."""
import os
import re
from PIL import Image

STORE_DIR = "store/play"
LOCALES = ["uk", "en-US", "ru-RU"]

def check_images():
    icon_path = os.path.join(STORE_DIR, "icon.png")
    feat_path = os.path.join(STORE_DIR, "featureGraphic.png")

    assert os.path.exists(icon_path), f"Missing {icon_path}"
    assert os.path.exists(feat_path), f"Missing {feat_path}"

    with Image.open(icon_path) as img:
        assert img.size == (512, 512), f"icon.png must be 512x512, got {img.size}"
        assert os.path.getsize(icon_path) <= 1024 * 1024, "icon.png must be <= 1MB"
        print(f"PASS: {icon_path} is {img.size}, {os.path.getsize(icon_path) // 1024} KB")

    with Image.open(feat_path) as img:
        assert img.size == (1024, 500), f"featureGraphic.png must be 1024x500, got {img.size}"
        assert os.path.getsize(feat_path) <= 15 * 1024 * 1024, "featureGraphic.png must be <= 15MB"
        print(f"PASS: {feat_path} is {img.size}, {os.path.getsize(feat_path) // 1024} KB")

    for loc in LOCALES:
        loc_icon = os.path.join(STORE_DIR, "metadata", loc, "images", "icon.png")
        loc_feat = os.path.join(STORE_DIR, "metadata", loc, "images", "featureGraphic.png")
        assert os.path.exists(loc_icon), f"Missing {loc_icon}"
        assert os.path.exists(loc_feat), f"Missing {loc_feat}"
        print(f"PASS: {loc} images present and verified")

def check_texts():
    latin_i_regex = re.compile(r"([а-яА-ЯёЁіїєґІЇЄҐ]*[iI][а-яА-ЯёЁіїєґІЇЄҐ]+|[а-яА-ЯёЁіїєґІЇЄҐ]+[iI][а-яА-ЯёЁіїєґІЇЄҐ]*)")

    for loc in LOCALES:
        loc_dir = os.path.join(STORE_DIR, "metadata", loc)
        title_file = os.path.join(loc_dir, "title.txt")
        short_file = os.path.join(loc_dir, "short-description.txt")
        full_file = os.path.join(loc_dir, "full-description.txt")

        assert os.path.exists(title_file), f"Missing {title_file}"
        assert os.path.exists(short_file), f"Missing {short_file}"
        assert os.path.exists(full_file), f"Missing {full_file}"

        with open(title_file, "r", encoding="utf-8") as f:
            title = f.read().strip()
            assert len(title) > 0, f"{title_file} is empty"
            assert len(title) <= 30, f"{title_file} length {len(title)} > 30: '{title}'"
            print(f"PASS: {loc} title ({len(title)}/30 chars): '{title}'")

        with open(short_file, "r", encoding="utf-8") as f:
            short = f.read().strip()
            assert len(short) > 0, f"{short_file} is empty"
            assert len(short) <= 80, f"{short_file} length {len(short)} > 80: '{short}'"
            print(f"PASS: {loc} short-description ({len(short)}/80 chars): '{short}'")

        with open(full_file, "r", encoding="utf-8") as f:
            full = f.read().strip()
            assert len(full) > 0, f"{full_file} is empty"
            assert len(full) <= 4000, f"{full_file} length {len(full)} > 4000"
            print(f"PASS: {loc} full-description ({len(full)}/4000 chars)")

        if loc == "uk":
            assert not latin_i_regex.search(title), f"Latin i in uk title: {title}"
            assert not latin_i_regex.search(short), f"Latin i in uk short: {short}"
            assert not latin_i_regex.search(full), f"Latin i in uk full description"
            print("PASS: uk texts verified for correct Cyrillic i")

def check_docs():
    assert os.path.exists(os.path.join(STORE_DIR, "data-safety.md")), "Missing data-safety.md"
    assert os.path.exists(os.path.join(STORE_DIR, "SCREENSHOTS.md")), "Missing SCREENSHOTS.md"
    print("PASS: data-safety.md and SCREENSHOTS.md present")

if __name__ == "__main__":
    check_images()
    check_texts()
    check_docs()
    print("\nВСЕ ПРОВЕРКИ ПРОЙДЕНЫ УСПЕШНО!")
