#!/usr/bin/env python3
"""
Собирает оформление трёх локаций уровня «Студвесна» (актовый зал, левая и правая кулисы)
из тайлов и реквизита (assets/stage/). Пишет:
  assets/rooms/hall.json, dressing.json, backstage.json   — читает игра (RoomLayout.java)
  tools/preview/*.png                                      — превью каждой локации

ВАЖНО: столкновения (стены, кресла, ящики) задаются в RoomFactory.java, а здесь — только
картинка. Поэтому ниже есть зеркало стен для проверки совпадения (см. COLLISION).
Координаты: пиксели, начало внизу слева (как в игре), тайл = 48 px, комната 26x15 тайлов.
Запуск из корня проекта:  python3 tools/build_stage_layouts.py
"""
import json
import os
from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
STAGE = os.path.join(ROOT, "assets", "stage")
ROOMS = os.path.join(ROOT, "assets", "rooms")
PREVIEW = os.path.join(ROOT, "tools", "preview")
T = 48
COLS, ROWS = 26, 15
W, H = COLS * T, ROWS * T


class Layout:
    def __init__(self):
        self.items = []

    def sprite(self, name, x, y, w=None, h=None, layer="over"):
        self.items.append({"kind": "sprite", "tex": name, "x": x, "y": y,
                           "w": w, "h": h, "layer": layer})

    def tile(self, name, col, row, layer="over", cols=1, rows=1):
        self.sprite(name, col * T, row * T, cols * T, rows * T, layer)

    def rect(self, x, y, w, h, color, layer="under"):
        self.items.append({"kind": "rect", "x": x, "y": y, "w": w, "h": h,
                           "color": color, "layer": layer})

    def fill(self, names, c0, r0, cols, rows, layer="under", pattern=None):
        for r in range(r0, r0 + rows):
            for c in range(c0, c0 + cols):
                i = (c + r) % len(names) if pattern is None else pattern(c, r) % len(names)
                self.tile(names[i], c, r, layer)

    def save(self, name):
        os.makedirs(ROOMS, exist_ok=True)
        with open(os.path.join(ROOMS, name + ".json"), "w", encoding="utf-8") as f:
            json.dump({"items": self.items}, f, ensure_ascii=False, indent=0)


# ---------------------------------------------------------------- зал
def hall():
    L = Layout()
    # Зрительный зал: паркет (две близкие по тону ёлочки в шахматном порядке)
    L.fill(["floor_parquet", "floor_parquet2"], 1, 1, 24, 7)
    # Сцена: деревянный настил
    L.fill(["stage_a", "stage_b", "stage_c", "stage_d"], 1, 9, 24, 5,
           pattern=lambda c, r: (c + (r % 2) * 2))
    # Передний край сцены; лестница (4 тайла по центру) — настил со ступенями
    for c in range(1, 25):
        if 11 <= c <= 14:
            L.tile("stage_a" if c % 2 else "stage_b", c, 8, "under")
        else:
            L.tile("stage_lip_a" if c % 2 else "stage_lip_b", c, 8, "under")
    for i in range(1, 4):
        L.rect(11 * T + 4, 8 * T + i * 12, 4 * T - 8, 2, "0d0809b3", "under")
    L.rect(11 * T, 8 * T, 4 * T, T, "ffd9a01f", "under")
    # Задник сцены (виден под лямбрекеном) и проёмы в кулисы
    L.rect(T, 14 * T, 24 * T, T, "2a1214", "under")
    L.rect(0, 11 * T, T, 2 * T, "120c0e", "under")
    L.rect(25 * T, 11 * T, T, 2 * T, "120c0e", "under")

    # Стены: низ (панели + два окна), бока, верх
    for c in range(COLS):
        name = "window" if c in (6, 19) else ("wall_wain_a" if c % 2 else "wall_wain_b")
        L.tile(name, c, 0, "over")
    for r in list(range(1, 11)) + [13]:
        for c in (0, 25):
            L.tile("wall_mid" if r % 2 else "wall_trim", c, r, "over")
    L.tile("wall_top", 0, 14)
    L.tile("wall_top", 25, 14)
    # Афиша на правой стене
    L.tile("frame", 25, 3)

    # Занавес сцены: верхний лямбрекен и боковые драпировки
    L.tile("curt_tl", 1, 14)
    for c in range(2, 24):
        L.tile("curt_top", c, 14)
    L.tile("curt_tr", 24, 14)
    for r, name in ((13, "curt_l1"), (12, "curt_l2"), (11, "curt_l3")):
        L.tile(name, 1, r)
    for r, name in ((13, "curt_r1"), (12, "curt_r2"), (11, "curt_r3")):
        L.tile(name, 24, r)

    # Кресла (обратные стороны, зрители смотрят на сцену)
    seats = ["seat_a", "seat_b", "seat_c"]
    for r in range(2, 5):
        for c in list(range(3, 11)) + list(range(15, 23)):
            L.tile(seats[(c + r) % 3], c, r)

    # Реквизит
    L.sprite("prop_exit", 56, 0)
    L.sprite("prop_journal", 586, 200)
    L.sprite("prop_mic", 1030, 530)
    L.sprite("prop_spot", 238, 566)
    L.sprite("prop_spot", 790, 566)
    L.sprite("prop_cables", 930, 520, layer="under")
    return L


# ---------------------------------------------------------------- левая кулиса
def dressing():
    L = Layout()
    L.fill(["floor_parquet", "floor_parquet2"], 1, 1, 24, 13)
    L.rect(T, T, 24 * T, 13 * T, "1c0f0a73", "under")   # приглушаем тон пола
    # Ковёр
    L.rect(10 * T, 4 * T, 6 * T, 4 * T, "5a1620", "under")
    L.rect(10 * T + 6, 4 * T + 6, 6 * T - 12, 4 * T - 12, "7a2230", "under")
    L.rect(10 * T + 14, 4 * T + 14, 6 * T - 28, 4 * T - 28, "5a1620", "under")
    L.rect(25 * T, 11 * T, T, 2 * T, "120c0e", "under")

    for c in range(COLS):
        L.tile("wall_wain_a" if c % 2 else "wall_wain_b", c, 0)
        L.tile("wall_top", c, 14)
    for r in range(1, 14):
        L.tile("wall_mid" if r % 2 else "wall_trim", 0, r)
        if r not in (11, 12):
            L.tile("wall_mid" if r % 2 else "wall_trim", 25, r)
    # Зеркала на задней стене
    for c in (4, 6, 8, 10, 13, 15, 17, 19):
        L.tile("frame", c, 14)
        L.rect(c * T + 8, 14 * T + 8, T - 16, T - 16, "b8cbe099", "over")

    # Вешалки с костюмами (на клетках-стенах) и перекладины
    for c0 in (4, 16):
        L.rect(c0 * T, 9 * T + 40, 6 * T, 5, "3a2412", "over")
        for i in range(6):
            L.tile(["drape_a", "drape_b", "drape_c"][i % 3], c0 + i, 9)
    for c0 in (4, 16):
        for dx in (0, 6 * T - 6):
            L.rect(c0 * T + dx, 9 * T - 2, 6, 44, "2a190c", "over")
    # Гримёрные столики под зеркалами
    for c0, n in ((4, 7), (13, 7)):
        L.rect(c0 * T, 13 * T, n * T, 20, "5b3a22", "over")
        L.rect(c0 * T, 13 * T + 18, n * T, 3, "8a5e3a", "over")
    # Скамья
    L.sprite("bench", 5 * T, 5 * T, 2 * T, 52)
    return L


# ---------------------------------------------------------------- правая кулиса
def backstage():
    L = Layout()
    L.fill(["stage_a", "stage_b", "stage_c", "stage_d"], 1, 1, 24, 13,
           pattern=lambda c, r: (c + (r % 2) * 2))
    L.rect(0, 11 * T, T, 2 * T, "120c0e", "under")

    for c in range(COLS):
        L.tile("wall_wain_a" if c % 2 else "wall_wain_b", c, 0)
        L.tile("window" if c in (5, 20) else "wall_top", c, 14)
    for r in range(1, 14):
        if r not in (11, 12):
            L.tile("wall_mid" if r % 2 else "wall_trim", 0, r)
        L.tile("wall_mid" if r % 2 else "wall_trim", 25, r)

    # Ящики ровно на клетках-стенах
    for (c, r) in [(8, 6), (9, 6), (8, 7), (17, 4), (18, 4), (18, 5)]:
        L.sprite("prop_crate", c * T - 2, r * T - 4, 52, 54)
    # Стойка с костюмами
    L.rect(13 * T, 11 * T + 40, 3 * T, 5, "3a2412", "over")
    for i in range(3):
        L.tile(["drape_a", "drape_b", "drape_c"][i], 13 + i, 11)
    # Лампа и афиши, кабели
    for dx in (0, 3 * T - 6):
        L.rect(13 * T + dx, 11 * T - 2, 6, 44, "2a190c", "over")
    L.sprite("prop_spot", 196, 262)
    L.sprite("prop_journal", 316, 262)
    L.sprite("prop_cables", 880, 420, layer="under")
    return L


# ---------------------------------------------------------------- зеркало для проверки
def walls_hall():
    s = set()
    for c in range(COLS):
        s |= {(c, 0), (c, ROWS - 1)}
    for r in range(ROWS):
        s |= {(0, r), (COLS - 1, r)}
    for c in range(1, COLS - 1):
        s.add((c, 8))
    for c in range(11, 15):
        s.discard((c, 8))
    for r in (11, 12):
        s.discard((0, r)); s.discard((COLS - 1, r))
    for r in range(2, 5):
        for c in list(range(3, 11)) + list(range(15, 23)):
            s.add((c, r))
    return s


def walls_dressing():
    s = set()
    for c in range(COLS):
        s |= {(c, 0), (c, ROWS - 1)}
    for r in range(ROWS):
        s |= {(0, r), (COLS - 1, r)}
    for c in list(range(4, 10)) + list(range(16, 22)):
        s.add((c, 9))
    s |= {(5, 5), (6, 5)}
    for r in (11, 12):
        s.discard((COLS - 1, r))
    return s


def walls_backstage():
    s = set()
    for c in range(COLS):
        s |= {(c, 0), (c, ROWS - 1)}
    for r in range(ROWS):
        s |= {(0, r), (COLS - 1, r)}
    s |= {(8, 6), (9, 6), (8, 7), (17, 4), (18, 4), (18, 5)}
    for r in (11, 12):
        s.discard((0, r))
    return s


# Интерактивные предметы (левый нижний угол + 11 = центр), портал, звезда — для превью
MARKS = {
    "hall": {"items": [(586, 210), (100, 150), (1130, 150), (100, 560), (1040, 560)],
             "save": (120, 300), "finish": (11 * T, 12 * T, 4 * T, 2 * T)},
    "dressing": {"items": [(268, 292), (880, 492)], "save": (120, 240), "finish": None},
    "backstage": {"items": [(330, 300), (690, 486)], "save": (1120, 240), "finish": None},
}


# ---------------------------------------------------------------- превью
_cache = {}


def load(name, grey=False):
    key = (name, grey)
    if key not in _cache:
        _cache[key] = Image.open(os.path.join(STAGE, "grey" if grey else "color", name + ".png")).convert("RGBA")
    return _cache[key]


def hexcolor(s):
    s = s.lstrip("#")
    r, g, b = int(s[0:2], 16), int(s[2:4], 16), int(s[4:6], 16)
    a = int(s[6:8], 16) if len(s) >= 8 else 255
    return r, g, b, a


def render(layout, walls, marks, color_amount=1.0, debug=False):
    img = Image.new("RGBA", (W, H), (14, 10, 10, 255))

    def draw_layer(layer):
        for it in layout.items:
            if it["layer"] != layer:
                continue
            if it["kind"] == "rect":
                r, g, b, a = hexcolor(it["color"])
                ov = Image.new("RGBA", (it["w"], it["h"]), (r, g, b, a))
                img.alpha_composite(ov, (it["x"], H - it["y"] - it["h"]))
            else:
                tex = load(it["tex"])
                w = it["w"] or tex.width
                h = it["h"] or tex.height
                if (w, h) != tex.size:
                    tex = tex.resize((w, h), Image.NEAREST if it["tex"].startswith("prop_") else Image.LANCZOS)
                img.alpha_composite(tex, (int(it["x"]), int(H - it["y"] - h)))

    draw_layer("under")
    draw_layer("over")
    d = ImageDraw.Draw(img)
    if debug:
        for (c, r) in walls:
            d.rectangle([c * T, H - (r + 1) * T, (c + 1) * T - 1, H - r * T - 1], outline=(255, 40, 40, 255))
    for (x, y) in marks["items"]:
        cx, cy = x + 11, H - (y + 11)
        d.ellipse([cx - 14, cy - 14, cx + 14, cy + 14], outline=(255, 230, 120, 255), width=3)
    sx, sy = marks["save"]
    d.text((sx, H - sy - 20), "*", fill=(255, 255, 160, 255))
    if marks["finish"]:
        fx, fy, fw, fh = marks["finish"]
        d.rectangle([fx, H - fy - fh, fx + fw, H - fy], outline=(120, 255, 160, 255), width=2)
    return img.convert("RGB")


def main():
    os.makedirs(PREVIEW, exist_ok=True)
    rooms = {"hall": (hall(), walls_hall()), "dressing": (dressing(), walls_dressing()),
             "backstage": (backstage(), walls_backstage())}
    for name, (layout, walls) in rooms.items():
        layout.save(name)
        render(layout, walls, MARKS[name], debug=False).save(os.path.join(PREVIEW, name + ".png"))
        render(layout, walls, MARKS[name], debug=True).save(os.path.join(PREVIEW, name + "_collision.png"))
    print("готово: assets/rooms/*.json и tools/preview/*.png")


if __name__ == "__main__":
    main()
