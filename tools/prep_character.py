#!/usr/bin/env python3
"""
Нарезает лист героини (tools/source/character_sheet.png, фон #FF00FF) на кадры.

Результат (запуск из корня проекта: python3 tools/prep_character.py):
  assets/chars/girl/  — idle_front.png, idle_side.png, walk_0..8.png, portrait.png
Пиксельный вариант пробовал: на 32x40 лицо теряется, поэтому оставлен «мягкий» рисунок.
Все кадры ходьбы и стойки имеют одинаковый размер холста и общую «линию земли»,
поэтому анимация не дёргается. Нужны: pip install pillow numpy scipy
"""
import os
import numpy as np
from PIL import Image
from scipy import ndimage

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SHEET = os.path.join(ROOT, "tools", "source", "character_sheet.png")
OUT = os.path.join(ROOT, "assets", "chars")

CANVAS_W, CANVAS_H = 96, 120      # холст кадра (в игре рисуется 64x80)
CHAR_H = 100                      # высота героини внутри холста
FEET_PAD = 6                      # отступ от ног до низа холста

def key(rgb):
    """Убираем фон #FF00FF и розовую кайму."""
    a = rgb.astype(float)
    r, g, b = a[:, :, 0], a[:, :, 1], a[:, :, 2]
    m = np.minimum(r, b) - g
    alpha = np.clip(1 - (m - 35) / 110, 0, 1)
    solid = ndimage.binary_erosion(alpha > 0.5, iterations=1)
    alpha = ndimage.gaussian_filter(solid.astype(float), 0.7)
    spill = np.clip(m, 0, None) * (m > 0)
    r = np.clip(r - spill * 0.85, 0, 255)
    b = np.clip(b - spill * 0.85, 0, 255)
    return np.dstack([r, g, b, alpha * 255]).astype(np.uint8)

def components(sheet_rgb):
    a = sheet_rgb.astype(int)
    r, g, b = a[:, :, 0], a[:, :, 1], a[:, :, 2]
    mag = (r > 170) & (b > 170) & (g < 140) & (r - g > 90) & (b - g > 90)
    fg = ndimage.binary_opening(~mag, structure=np.ones((3, 3)))
    lab, _ = ndimage.label(fg, structure=np.ones((3, 3)))
    boxes = []
    for i, sl in enumerate(ndimage.find_objects(lab), 1):
        area = (lab[sl] == i).sum()
        if area > 1200:
            boxes.append((sl[1].start, sl[0].start, sl[1].stop, sl[0].stop))  # x0,y0,x1,y1
    return boxes

def to_canvas(rgba_full, box, scale):
    """Кадр -> холст CANVAS_W x CANVAS_H: ноги на линии земли, голова над центром."""
    x0, y0, x1, y1 = box
    crop = Image.fromarray(rgba_full[y0:y1, x0:x1], "RGBA")
    w, h = crop.size
    crop = crop.resize((max(1, round(w * scale)), max(1, round(h * scale))), Image.LANCZOS)
    arr = np.array(crop)
    # опорная точка по X: центр масс верхней части тела (голова и плечи не «ездят» при шаге)
    top = arr[: max(1, int(arr.shape[0] * 0.45)), :, 3] > 40
    cols = np.where(top.any(axis=0))[0]
    ax = (cols.min() + cols.max()) / 2.0 if len(cols) else arr.shape[1] / 2.0
    canvas = Image.new("RGBA", (CANVAS_W, CANVAS_H), (0, 0, 0, 0))
    px = int(round(CANVAS_W / 2.0 - ax))
    py = CANVAS_H - FEET_PAD - crop.size[1]
    canvas.alpha_composite(crop, (px, py)) if px >= 0 and py >= 0 else canvas.paste(crop, (px, py), crop)
    return canvas

def main():
    sheet = Image.open(SHEET).convert("RGB")
    rgb = np.array(sheet)
    rgba = key(rgb)
    boxes = components(rgb)

    def in_band(y0, y1, min_h=60):
        return sorted([b for b in boxes if y0 <= b[1] < y1 and (b[3] - b[1]) >= min_h and b[0] < 800],
                      key=lambda b: b[0])

    idles = in_band(0, 100)                 # 4 стойки
    walks = in_band(190, 260)               # 9 кадров ходьбы
    assert len(idles) == 4 and len(walks) == 9, (len(idles), len(walks))

    idle_scale = CHAR_H / (idles[0][3] - idles[0][1])
    walk_scale = CHAR_H / 104.0             # общий масштаб для всех кадров ходьбы (сохраняем «подпрыгивание»)

    frames = {
        "idle_front": to_canvas(rgba, idles[0], idle_scale),
        "idle_front2": to_canvas(rgba, idles[1], idle_scale),
        "idle_front3": to_canvas(rgba, idles[2], idle_scale),
        "idle_side": to_canvas(rgba, idles[3], idle_scale),
    }
    for i, b in enumerate(walks):
        frames["walk_%d" % i] = to_canvas(rgba, b, walk_scale)

    # Портрет для диалога: голова и плечи с большого кадра справа
    p = Image.fromarray(rgba[30:222, 808:1000], "RGBA").resize((256, 256), Image.LANCZOS)

    d = os.path.join(OUT, "girl")
    os.makedirs(d, exist_ok=True)
    for name, img in frames.items():
        img.save(os.path.join(d, name + ".png"))
    p.save(os.path.join(d, "portrait.png"))
    print("готово ->", OUT, "| кадры:", ", ".join(frames))

if __name__ == "__main__":
    main()
