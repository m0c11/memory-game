package com.mygame.world;

import com.badlogic.gdx.graphics.Color;
import com.mygame.assets.Assets;
import com.mygame.levels.LevelInfo;
import com.mygame.levels.Levels;
import com.mygame.world.Interactable;
import com.mygame.world.Room;

/**
 * Фабрика комнат: по номеру уровня создаёт комнату.
 * Уровни 0, 1, 3 и 11 уже имеют свои комнаты, остальные пока заглушки
 * (с названием главы и заметкой о её фишке из лора).
 * Когда будешь делать новую главу — просто добавь case в createRoom().
 *
 * Цвет сцены (tint) плавно идёт от серого к тёплому по мере прохождения:
 * мир постепенно обретает цвет.
 */
public class RoomFactory {

    public static final int COLS = 26;
    public static final int ROWS = 15;

    public static Room createRoom(int index, Assets assets) {
        switch (index) {
            case 0:  return roomAwakening(assets, index);
            case 1:  return roomStudvesna(assets, index);
            case 3:  return roomCamp(assets, index);
            case 11: return roomFinal(assets, index);
            default: return roomPlaceholder(assets, index);
        }
    }

    // ------- Общие настройки пустой комнаты ----------------

    private static Color progressTint(int index) {
        float t = index / (float) (Levels.COUNT - 1);
        return new Color(0.45f + 0.55f * t, 0.45f + 0.53f * t, 0.52f + 0.40f * t, 1f);
    }

    private static Room base(Assets assets, String name, int index) {
        Room r = new Room(assets, COLS, ROWS);
        r.buildBorders();
        r.setName(name);
        r.setTint(progressTint(index));

        // Дверь в правой стене, ряды 7–8
        int doorCol = COLS - 1;
        r.openDoorTiles(doorCol, 7, 8);
        r.setDoor(
                doorCol * Room.TILE - 10,
                7 * Room.TILE,
                Room.TILE + 10,
                2 * Room.TILE
        );
        return r;
    }

    // ------- Конкретные комнаты ----------------------------

    private static Room roomAwakening(Assets assets, int index) {
        Room r = base(assets, Levels.ALL[index].title, index);

        r.addInteractable(new Interactable(assets, 350, 380,
                "Ты не помнишь этот год.\nВсё вокруг — чёрно-белое."));
        r.addInteractable(new Interactable(assets, 750, 300,
                "Чтобы вернуть цвет, нужно вспомнить всё, что было.\nНачни с малого — найди предметы вокруг."));

        return r;
    }

    private static Room roomStudvesna(Assets assets, int index) {
        Room r = base(assets, Levels.ALL[index].title, index);

        // Немного внутренних стен для «лабиринта»
        for (int rr = 3; rr <= 6; rr++) r.setWall(7, rr, true);
        for (int rr = 8; rr <= 11; rr++) r.setWall(15, rr, true);

        r.addInteractable(new Interactable(assets, 260, 200,
                "Наколенники на стуле.\n\"Без них я бы не вышел на сцену.\""));
        r.addInteractable(new Interactable(assets, 500, 500,
                "Тейп, бинты, чей-то недопитый чай.\n\"Каждый виток — шаг к цели.\""));
        r.addInteractable(new Interactable(assets, 950, 400,
                "Энергетик на подоконнике.\n\"Горький, но необходимый.\""));

        return r;
    }

    private static Room roomCamp(Assets assets, int index) {
        Room r = base(assets, Levels.ALL[index].title, index);

        // «Деревья» внутри комнаты
        int[][] trees = {{5,4},{6,10},{11,3},{12,11},{18,5},{19,10},{22,7}};
        for (int[] t : trees) r.setWall(t[0], t[1], true);

        r.addInteractable(new Interactable(assets, 320, 550,
                "Костёр. Здесь вы просидели до утра\nи говорили обо всём на свете."));
        r.addInteractable(new Interactable(assets, 600, 240,
                "Рация.\n\"Первый, первый, ответь...\""));
        r.addInteractable(new Interactable(assets, 900, 550,
                "Гитара у палатки.\nПомнишь ту песню, что пели хором?"));

        return r;
    }

    private static Room roomFinal(Assets assets, int index) {
        Room r = base(assets, Levels.ALL[index].title, index);

        r.addInteractable(new Interactable(assets, 620, 360,
                "Ты собрала весь год.\nСвет постепенно возвращается."));

        return r;
    }

    /** Заглушка для глав и ключей, которые ещё не сделаны. */
    private static Room roomPlaceholder(Assets assets, int index) {
        LevelInfo info = Levels.ALL[index];
        Room r = base(assets, info.title, index);

        if (info.chapter) {
            r.addInteractable(new Interactable(assets, 400, 360,
                    "Здесь будет глава «" + info.title + "».\nФишка: " + info.mechanic + "."));
            r.addInteractable(new Interactable(assets, 800, 300,
                    "Сюда добавим предметы главы\nи диалоги из лора."));
        } else {
            r.addInteractable(new Interactable(assets, 500, 360,
                    info.title + ": милый момент ваших отношений.\nЗдесь будет воспоминание."));
        }
        return r;
    }
}
