#!/usr/bin/env python3
"""
Готовит ассеты уровня «Студвесна» из двух листов в tools/source/:
  theater_tileset.png   — тайлы театра (пол, стены, занавес, кресла)
  backstage_props.png   — реквизит (микрофон, прожектор, ящик, дверь EXIT, ...)

Результат: assets/stage/color/*.png и assets/stage/grey/*.png (серый «двойник»,
как у остальных тайлов игры: цвет возвращается по мере воспоминаний).
Запуск из корня проекта:  python3 tools/prep_stage_assets.py
Нужны: pip install pillow numpy scipy
"""
import os
import numpy as np
from PIL import Image
from scipy import ndimage

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "tools", "source")
OUT = os.path.join(ROOT, "assets", "stage")
TILE = 48

# ---------------------------------------------------------------- тайлы
def key_checkerboard(path):
    """Лист пришёл с нарисованной «шахматкой» вместо прозрачности — убираем её."""
    a = np.array(Image.open(path).convert("RGB")).astype(int)
    r, g, b = a[:, :, 0], a[:, :, 1], a[:, :, 2]
    neutral = (abs(r - g) < 12) & (abs(g - b) < 12) & (r > 150)
    opaque = ~neutral
    dark = (abs(r - g) < 12) & (abs(g - b) < 12) & (r < 75)
    lines = ndimage.binary_opening(dark, structure=np.ones((1, 25))) | \
            ndimage.binary_opening(dark, structure=np.ones((25, 1)))
    opaque &= ~ndimage.binary_dilation(lines, iterations=1)
    lab, n = ndimage.label(opaque, structure=np.ones((3, 3)))
    sizes = ndimage.sum(opaque, lab, range(1, n + 1))
    keep = np.zeros(n + 1, bool)
    keep[1:] = sizes >= 60
    opaque = keep[lab]
    rgba = np.dstack([a.astype(np.uint8), (opaque * 255).astype(np.uint8)])
    return Image.fromarray(rgba, "RGBA")

YS = [9, 76, 144, 211, 279, 349, 419, 488, 558]
BLOCKS = {"A": [0, 68, 136, 204, 272],
          "B": [273, 307, 375, 443, 511, 580, 648, 716],
          "D": [751, 819, 887, 955, 1023]}

# имя -> (блок, колонка, строка)
TILES = {
    "floor_parquet": ("A", 0, 0), "floor_parquet2": ("A", 2, 0),
    "floor_planks": ("A", 3, 1),
    "stage_a": ("B", 1, 0), "stage_b": ("B", 2, 0),
    "stage_c": ("B", 1, 1), "stage_d": ("B", 2, 1),
    "stage_lip_a": ("B", 1, 2), "stage_lip_b": ("B", 2, 2),
    "wall_top": ("D", 0, 0), "wall_mid": ("D", 1, 1), "wall_trim": ("D", 1, 2),
    "wall_wain_a": ("D", 2, 3), "wall_wain_b": ("D", 3, 3),
    "window": ("D", 2, 2), "frame": ("D", 3, 2),
    "curt_tl": ("B", 4, 0), "curt_top": ("B", 5, 0), "curt_tr": ("B", 6, 0),
    "curt_l1": ("B", 4, 1), "curt_l2": ("B", 4, 2), "curt_l3": ("B", 4, 3),
    "curt_r1": ("B", 6, 1), "curt_r2": ("B", 6, 2), "curt_r3": ("B", 6, 3),
    "drape_a": ("B", 3, 5), "drape_b": ("B", 4, 5), "drape_c": ("B", 5, 5),
    "seat_a": ("D", 0, 5), "seat_b": ("D", 1, 5), "seat_c": ("D", 2, 5),
    "bench": ("D", 1, 7),
}

# ---------------------------------------------------------------- реквизит
PROPS_GRID_X = [42, 281, 525, 767, 1003]
PROPS_GRID_Y = [42, 279, 517]
# имя -> (ряд, колонка, «пиксельная» высота, высота на экране)
PROPS = {
    "prop_mic": (0, 0, 44, 96), "prop_spot": (0, 1, 40, 104),
    "prop_crate": (0, 2, 34, 78), "prop_exit": (0, 3, 44, 120),
    "prop_tassel": (1, 0, 40, 64), "prop_cables": (1, 1, 36, 96),
    "prop_journal": (1, 2, 26, 44),
}

def grey_twin(img):
    """Серый вариант в палитре остальных серых тайлов игры (холодный серо-голубой)."""
    a = np.array(img.convert("RGBA")).astype(float)
    lum = a[:, :, 0] * .299 + a[:, :, 1] * .587 + a[:, :, 2] * .114
    out = np.dstack([lum * 0.89, lum * 1.03, lum * 1.11, a[:, :, 3]])
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), "RGBA")

def save_pair(name, img):
    img.save(os.path.join(OUT, "color", name + ".png"))
    grey_twin(img).save(os.path.join(OUT, "grey", name + ".png"))

def cut_prop(sheet, row, col):
    x0, x1 = PROPS_GRID_X[col] + 3, PROPS_GRID_X[col + 1] - 3
    y0, y1 = PROPS_GRID_Y[row] + 3, PROPS_GRID_Y[row + 1] - 3
    t = sheet[y0:y1, x0:x1]
    bg = np.median(np.concatenate([t[:6].reshape(-1, 3), t[-6:].reshape(-1, 3)]), axis=0)
    sat = t.max(axis=2) - t.min(axis=2)
    close = (np.abs(t - bg).max(axis=2) < 38) & (sat < 22)
    lab, _ = ndimage.label(close)
    border = set(np.unique(np.concatenate([lab[0], lab[-1], lab[:, 0], lab[:, -1]]))) - {0}
    alpha = ~np.isin(lab, list(border))
    alpha = ndimage.binary_opening(alpha, structure=np.ones((2, 2)))
    lab2, n2 = ndimage.label(alpha)
    if n2:
        sizes = ndimage.sum(alpha, lab2, range(1, n2 + 1))
        alpha = np.isin(lab2, [i + 1 for i, s in enumerate(sizes) if s > 150])
    img = Image.fromarray(np.dstack([t.astype(np.uint8), (alpha * 255).astype(np.uint8)]), "RGBA")
    bb = img.getbbox()
    return img.crop(bb) if bb else img

def cut_cables(sheet, row, col):
    """На листе кабели лежат на квадрате досок. Оставляем только тёмные кабели, без квадрата."""
    x0, x1 = PROPS_GRID_X[col] + 3, PROPS_GRID_X[col + 1] - 3
    y0, y1 = PROPS_GRID_Y[row] + 3, PROPS_GRID_Y[row + 1] - 3
    t = sheet[y0:y1, x0:x1]
    lum = t[:, :, 0] * .299 + t[:, :, 1] * .587 + t[:, :, 2] * .114
    alpha = lum < 62
    alpha = ndimage.binary_closing(alpha, structure=np.ones((3, 3)))
    lab, n = ndimage.label(alpha)
    if n:
        sizes = ndimage.sum(alpha, lab, range(1, n + 1))
        alpha = np.isin(lab, [i + 1 for i, s in enumerate(sizes) if s > 120])
    img = Image.fromarray(np.dstack([t.astype(np.uint8), (alpha * 255).astype(np.uint8)]), "RGBA")
    bb = img.getbbox()
    return img.crop(bb) if bb else img

def pixelate(img, small_h, display_h, colors=30):
    """Мультяшный реквизит переводим в пиксель-арт, чтобы он стоял в одном стиле с тайлами."""
    w, h = img.size
    sw = max(1, round(w * small_h / h))
    small = img.resize((sw, small_h), Image.BOX)
    a = small.getchannel("A").point(lambda v: 255 if v > 110 else 0)
    rgb = small.convert("RGB").quantize(colors=colors, method=Image.MEDIANCUT).convert("RGB")
    out = Image.merge("RGBA", (*rgb.split(), a))
    return out.resize((round(sw * display_h / small_h), display_h), Image.NEAREST)

def main():
    os.makedirs(os.path.join(OUT, "color"), exist_ok=True)
    os.makedirs(os.path.join(OUT, "grey"), exist_ok=True)

    atlas = key_checkerboard(os.path.join(SRC, "theater_tileset.png"))
    for name, (blk, c, r) in TILES.items():
        xs = BLOCKS[blk]
        tile = atlas.crop((xs[c] + 2, YS[r] + 2, xs[c + 1] - 2, YS[r + 1] - 2))
        save_pair(name, tile.resize((TILE, TILE), Image.LANCZOS))

    sheet = np.array(Image.open(os.path.join(SRC, "backstage_props.png")).convert("RGB")).astype(int)
    for name, (r, c, sh, dh) in PROPS.items():
        src = cut_cables(sheet, r, c) if name == "prop_cables" else cut_prop(sheet, r, c)
        save_pair(name, pixelate(src, sh, dh))
    print("готово:", len(TILES), "тайлов и", len(PROPS), "предметов ->", OUT)

if __name__ == "__main__":
    main()
