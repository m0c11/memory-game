package com.mygame.world;

import com.badlogic.gdx.graphics.Color;
import com.mygame.assets.Assets;

/**
 * Фабрика комнат. Первые три главы заданы здесь как пример.
 * Когда появится реальная карта игры — просто добавляй новые кейсы.
 */
public class RoomFactory {

    public static final int COLS = 26;
    public static final int ROWS = 15;
    public static final int ROOM_COUNT = 4;

    public static Room createRoom(int index, Assets assets) {
        switch (index) {
            case 0: return roomAwakening(assets);
            case 1: return roomStudvesna(assets);
            case 2: return roomCamp(assets);
            case 3: return roomFinal(assets);
            default: return roomAwakening(assets);
        }
    }

    // ------- Общие настройки пустой комнаты ----------------

    private static Room base(Assets assets, String name, Color tint) {
        Room r = new Room(assets, COLS, ROWS);
        r.buildBorders();
        r.setName(name);
        r.setTint(tint);

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

    private static Room roomAwakening(Assets assets) {
        Room r = base(assets, "Пробуждение",
                new Color(0.45f, 0.45f, 0.52f, 1f));

        r.addInteractable(new Interactable(assets, 350, 380,
                "Ты не помнишь этот год.\nВсё вокруг — чёрно-белое."));
        r.addInteractable(new Interactable(assets, 750, 300,
                "Чтобы вернуть цвет, нужно вспомнить всё, что было.\nНачни с малого — найди предметы вокруг."));

        return r;
    }

    private static Room roomStudvesna(Assets assets) {
        Room r = base(assets, "Подготовка к Студвесне",
                new Color(1.0f, 0.95f, 0.85f, 1f));

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

    private static Room roomCamp(Assets assets) {
        Room r = base(assets, "Лагерь",
                new Color(0.92f, 1.0f, 0.88f, 1f));

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

    private static Room roomFinal(Assets assets) {
        Room r = base(assets, "Ты помнишь",
                new Color(1.0f, 0.98f, 0.9f, 1f));

        r.addInteractable(new Interactable(assets, 620, 360,
                "Ты собрала весь год.\nСвет постепенно возвращается."));

        return r;
    }
}