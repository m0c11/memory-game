#!/usr/bin/env python3
"""Нарезает tools/source/pacman_sprite_sheet.png на PNG-спрайты босса Pac-Man.
Запуск из корня проекта: python3 tools/prep_pacman_assets.py
Зависимость: python3 -m pip install pillow numpy
"""
from pathlib import Path
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "tools" / "source" / "pacman_sprite_sheet.png"
OUT = ROOT / "assets" / "pacman"
SHEET = np.asarray(Image.open(SOURCE).convert("RGB"))

def cut(box, destination):
    x0, y0, x1, y1 = box
    rgb = SHEET[y0:y1, x0:x1].copy()
    # На исходном листе шахматный фон нарисован пикселями, а не задан альфа-каналом.
    # Убираем нейтральные светло-серые клетки, оставляя цветные детали и тёмный контур.
    high, low = rgb.max(axis=2), rgb.min(axis=2)
    background = ((high - low) < 17) & (low > 145)
    alpha = np.where(background, 0, 255).astype(np.uint8)
    rgba = Image.fromarray(np.dstack((rgb, alpha)), "RGBA")
    bounds = rgba.getchannel("A").getbbox()
    if bounds:
        rgba = rgba.crop(bounds)
    destination.parent.mkdir(parents=True, exist_ok=True)
    rgba.save(destination)

def main():
    OUT.mkdir(parents=True, exist_ok=True)
    # Девять кадров героя: движение вправо и влево.
    player_x = [20, 135, 250, 365, 480, 595, 710, 825, 940]
    for i, x in enumerate(player_x):
        cut((x, 48, x + 100, 155), OUT / f"player_{i}.png")
    for i, x in enumerate([1090, 1195, 1300]):
        cut((x, 48, x + 100, 155), OUT / f"player_death_{i}.png")

    # Четыре уникальных персонажа-призрака и кадры их смерти.
    rows = [(260, 365), (415, 520), (565, 675), (715, 820)]
    frame_x = [20, 125, 230, 335, 440, 545, 650]
    for ghost, (top, bottom) in enumerate(rows):
        for frame, x in enumerate(frame_x):
            cut((x, top, x + 95, bottom), OUT / f"ghost_{ghost}_{frame}.png")
        for frame, x in enumerate([760, 865, 970, 1050]):
            cut((x, top, x + 95, bottom), OUT / f"ghost_{ghost}_death_{frame}.png")
        cut((1260, top, 1450, top + 145), OUT / f"ghost_{ghost}_power.png")

    for i, x in enumerate([20, 65, 110, 155, 200, 245, 290, 335, 380, 425, 470]):
        cut((x, 880, x + 38, 925), OUT / f"dot_{i}.png")
    for i, x in enumerate([565, 610, 655, 700, 745, 790, 835]):
        cut((x, 875, x + 40, 960), OUT / f"bottle_{i}.png")
    print(f"Готово: спрайты созданы в {OUT}")

if __name__ == "__main__":
    main()
