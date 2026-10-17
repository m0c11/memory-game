package com.mygame.world;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.mygame.assets.Assets;

/**
 * Читает оформление комнаты из JSON (assets/rooms/*.json) и добавляет его в Room.
 *
 * Файлы создаёт скрипт tools/build_stage_layouts.py (там же превью каждой комнаты).
 * Формат: {"items": [ {kind, layer, x, y, w, h, tex | color}, ... ]}
 *   kind  — "sprite" (картинка из assets/stage) или "rect" (цветной прямоугольник)
 *   layer — "under" (под стенами: пол, ковёр) или "over" (поверх: стены, кресла, реквизит)
 *   x, y  — левый нижний угол в пикселях комнаты (ось Y вверх), w, h — размер
 *           (для sprite можно null — тогда берётся размер картинки)
 *   color — RRGGBB или RRGGBBAA
 *
 * Столкновения (стены, кресла, ящики) задаются в RoomFactory, а не здесь:
 * этот класс отвечает только за картинку.
 */
public final class RoomLayout {

    private RoomLayout() {}

    /** @return true, если оформление загружено; иначе комната остаётся со стандартными тайлами. */
    public static boolean apply(Room room, Assets assets, String path) {
        try {
            JsonValue root = new JsonReader().parse(Gdx.files.internal(path));
            JsonValue items = root.get("items");
            if (items == null) return false;

            room.setThemed(true);
            for (JsonValue it = items.child; it != null; it = it.next) {
                boolean over = "over".equals(it.getString("layer", "over"));
                float x = it.getFloat("x");
                float y = it.getFloat("y");

                if ("rect".equals(it.getString("kind", "sprite"))) {
                    room.addDecorRect(x, y, it.getFloat("w"), it.getFloat("h"),
                                      Color.valueOf(it.getString("color")), over);
                } else {
                    Texture[] pair = assets.stagePair(it.getString("tex"));
                    JsonValue wv = it.get("w");
                    JsonValue hv = it.get("h");
                    float w = (wv != null && !wv.isNull()) ? wv.asFloat() : pair[1].getWidth();
                    float h = (hv != null && !hv.isNull()) ? hv.asFloat() : pair[1].getHeight();
                    room.addDecorSprite(pair, x, y, w, h, over);
                }
            }
            return true;
        } catch (Exception e) {
            Gdx.app.error("RoomLayout", "Не удалось загрузить оформление " + path, e);
            room.setThemed(false);
            return false;
        }
    }
}
