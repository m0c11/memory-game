package com.mygame.world;

import com.badlogic.gdx.graphics.Color;
import com.mygame.assets.Assets;
import com.mygame.levels.LevelInfo;
import com.mygame.levels.Levels;

import java.util.Arrays;
import java.util.List;

/**
 * Фабрика комнат: по номеру уровня создаёт комнату.
 * Уровни 0, 1, 3 и 11 уже имеют свои комнаты, остальные пока заглушки
 * (с названием главы и заметкой о её фишке из лора).
 * Когда будешь делать новую главу — просто добавь case в createRoom().
 *
 * Цвет комнаты плавно идёт от серого к цветному по мере прохождения
 * (серые и цветные тайлы смешиваются): мир постепенно обретает цвет.
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

    /**
     * Локации уровня. У большинства уровней локация одна, но некоторые
     * (например, Студвесна) состоят из нескольких связанных областей —
     * зал со сценой, левая и правая кулисы.
     */
    public static List<Room> createRooms(int index, Assets assets) {
        switch (index) {
            case 1:  return studvesnaLocations(assets, index);
            default: return java.util.Collections.singletonList(createRoom(index, assets));
        }
    }

    // ------- Общие настройки пустой комнаты ----------------

    /** Насколько цвет уже вернулся в мир: 0 в начале игры, 1 в финале. */
    private static float colorAmountFor(int index) {
        return index / (float) (Levels.COUNT - 1);
    }

    private static Room base(Assets assets, String name, int index) {
        Room r = new Room(assets, COLS, ROWS);
        r.buildBorders();
        r.setName(name);
        r.setTint(Color.WHITE);
        r.setColorAmount(colorAmountFor(index));

        // Дверь в правой стене, ряды 7–8
        int doorCol = COLS - 1;
        r.openDoorTiles(doorCol, 7, 8);
        r.setDoor(
            doorCol * Room.TILE - 10,
            7 * Room.TILE,
            Room.TILE + 10,
            2 * Room.TILE
        );

        // Звезда сохранения у входа (тайл-колонка 2, середина комнаты)
        r.setSavePoint(120, ROWS / 2f * Room.TILE);
        return r;
    }

    // ------- Уровень 1: актовый зал и кулисы ----------------

    /**
     * Студвесна — это три связанных локации: сам зал со сценой
     * (лестница соединяет сцену и зрительный зал) и две кулисы —
     * слева раздевалка, справа подсобка.
     */
    private static List<Room> studvesnaLocations(Assets assets, int index) {
        return Arrays.asList(
            hall(assets, index),
            dressingRoom(assets, index),
            backstageRoom(assets, index)
        );
    }

    /** Зал: сцена сверху, зрительные ряды снизу, между ними — лестница. */
    private static Room hall(Assets assets, int index) {
        final float T = Room.TILE;
        Room r = new Room(assets, COLS, ROWS);
        r.buildBorders();
        r.setName("Актовый зал");
        r.setTint(Color.WHITE);
        r.setColorAmount(colorAmountFor(index));

        // Рампа между сценой и залом (строка 8). Пройти можно только по лестнице.
        for (int c = 1; c <= COLS - 2; c++) r.setWall(c, 8, true);
        for (int c = 11; c <= 14; c++) r.setWall(c, 8, false);

        // Проёмы за кулисы — в боковых стенах на уровне сцены.
        r.openDoorTiles(0, 11, 12);
        r.openDoorTiles(COLS - 1, 11, 12);

        // Ряды кресел: центральный проход (11..14) и боковые.
        for (int rr = 2; rr <= 4; rr++) {
            for (int c = 3; c <= 10; c++) r.setWall(c, rr, true);
            for (int c = 15; c <= 22; c++) r.setWall(c, rr, true);
        }

        // ---- Оформление ----
        // Деревянный настил сцены
        r.addAccentUnder(T, 9 * T, 24 * T, 5 * T, new Color(0.26f, 0.17f, 0.12f, 0.55f));
        // Передний край сцены затемняем по бокам от лестницы
        r.addAccentOver(T, 8 * T, 10 * T, T, new Color(0.05f, 0.04f, 0.07f, 0.85f));
        r.addAccentOver(15 * T, 8 * T, 10 * T, T, new Color(0.05f, 0.04f, 0.07f, 0.85f));
        // Лестница: светлее + ступени
        r.addAccentOver(11 * T, 8 * T, 4 * T, T, new Color(0.45f, 0.38f, 0.30f, 0.6f));
        for (int i = 1; i <= 3; i++) {
            r.addAccentOver(11 * T + 6, 8 * T + i * 11, 4 * T - 12, 2,
                            new Color(0.08f, 0.06f, 0.09f, 0.7f));
        }
        // Тёмно-красный занавес: задник сцены и верх
        r.addAccentOver(T, 13 * T, 24 * T, T, new Color(0.42f, 0.10f, 0.14f, 0.75f));
        r.addAccentOver(0, 14 * T, COLS * T, T, new Color(0.5f, 0.12f, 0.16f, 0.9f));
        // Боковые кулисы-занавеси
        r.addAccentOver(T, 9 * T, T, 5 * T, new Color(0.42f, 0.10f, 0.14f, 0.6f));
        r.addAccentOver(24 * T, 9 * T, T, 5 * T, new Color(0.42f, 0.10f, 0.14f, 0.6f));
        // Ряды кресел затемняем
        r.addAccentOver(3 * T, 2 * T, 8 * T, 3 * T, new Color(0.17f, 0.09f, 0.13f, 0.6f));
        r.addAccentOver(15 * T, 2 * T, 8 * T, 3 * T, new Color(0.17f, 0.09f, 0.13f, 0.6f));

        // ---- Проходы в кулисы ----
        r.addPortal(new Portal(0, 11 * T, T, 2 * T, 1, 200, 520));
        r.addPortal(new Portal(25 * T, 11 * T, T, 2 * T, 2, 1000, 520));

        // ---- Выход на сцену (откроется, когда всё собрано) ----
        r.setFinish(11 * T, 12 * T, 4 * T, 2 * T);

        // ---- Звезда сохранения и воспоминания ----
        r.setSavePoint(120, 300);
        r.addInteractable(new Interactable(assets, 600, 210,
            "Программа концерта, отпечатанная наспех.\n\"Студвесна. Наша весна.\""));
        r.addInteractable(new Interactable(assets, 140, 120,
            "Кресло в зале, где ты всегда садилась ближе к сцене.\nОтсюда всё казалось ближе."));
        r.addInteractable(new Interactable(assets, 1110, 120,
            "Афиша с твоим именем.\n\"Ещё вчера я боялась даже смотреть на неё.\""));
        r.addInteractable(new Interactable(assets, 240, 560,
            "Тяжёлый занавес кулис.\nЗа ним — целый мир, которого не видит зритель."));
        r.addInteractable(new Interactable(assets, 1040, 560,
            "Микрофон на стойке.\n\"Он кажется холодным, пока не заговоришь.\""));
        return r;
    }

    /** Левая кулиса — раздевалка с зеркалами и костюмами. */
    private static Room dressingRoom(Assets assets, int index) {
        final float T = Room.TILE;
        Room r = new Room(assets, COLS, ROWS);
        r.buildBorders();
        r.setName("Левая кулиса · раздевалка");
        r.setTint(Color.WHITE);
        r.setColorAmount(colorAmountFor(index));

        // Вешалки с костюмами и гримёрный столик.
        for (int c = 4; c <= 9; c++) r.setWall(c, 9, true);
        for (int c = 16; c <= 21; c++) r.setWall(c, 9, true);
        r.setWall(5, 5, true);
        r.setWall(6, 5, true);

        // Выход обратно на сцену — правая стена.
        r.openDoorTiles(COLS - 1, 11, 12);
        r.addPortal(new Portal(25 * T, 11 * T, T, 2 * T, 0, 150, 520));

        // Тёплый свет гримёрки и зеркала на дальней стене.
        r.addAccentUnder(0, 0, COLS * T, ROWS * T, new Color(0.22f, 0.11f, 0.11f, 0.22f));
        r.addAccentOver(3 * T, 14 * T + 9, 9 * T, 30, new Color(0.72f, 0.78f, 0.82f, 0.45f));
        r.addAccentOver(14 * T, 14 * T + 9, 9 * T, 30, new Color(0.72f, 0.78f, 0.82f, 0.45f));

        r.setSavePoint(120, 240);
        r.addInteractable(new Interactable(assets, 600, 420,
            "Пуанты и наколенники на скамье.\n\"Каждый вечер перед выходом — дрожь в коленях.\""));
        r.addInteractable(new Interactable(assets, 900, 520,
            "Костюм на вешалке, весь в блёстках.\n\"В нём я чувствовала себя другой — смелее.\""));
        return r;
    }

    /** Правая кулиса — подсобка с реквизитом. */
    private static Room backstageRoom(Assets assets, int index) {
        final float T = Room.TILE;
        Room r = new Room(assets, COLS, ROWS);
        r.buildBorders();
        r.setName("Правая кулиса · комната");
        r.setTint(Color.WHITE);
        r.setColorAmount(colorAmountFor(index));

        // Коробки и реквизит.
        r.setWall(8, 6, true);
        r.setWall(9, 6, true);
        r.setWall(8, 7, true);
        r.setWall(17, 4, true);
        r.setWall(18, 4, true);
        r.setWall(18, 5, true);

        // Выход обратно на сцену — левая стена.
        r.openDoorTiles(0, 11, 12);
        r.addPortal(new Portal(0, 11 * T, T, 2 * T, 0, 1100, 520));

        // Прохладный полумрак и стопки коробок.
        r.addAccentUnder(0, 0, COLS * T, ROWS * T, new Color(0.10f, 0.12f, 0.20f, 0.22f));
        r.addAccentOver(8 * T, 6 * T, 2 * T, 2 * T, new Color(0.35f, 0.26f, 0.16f, 0.7f));
        r.addAccentOver(17 * T, 4 * T, 2 * T, 2 * T, new Color(0.35f, 0.26f, 0.16f, 0.7f));

        r.setSavePoint(1120, 240);
        r.addInteractable(new Interactable(assets, 330, 300,
            "Одинокая лампа и стопка старых афиш.\n\"Здесь пахло пылью и будущим.\""));
        r.addInteractable(new Interactable(assets, 700, 560,
            "Стойка с реквизитом: шляпы, перья, блёстки.\n\"Мы примеряли всё это и смеялись до слёз.\""));
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
        // Старый вариант лабиринта оставлен на случай обращений, но
        // в игровой поток теперь идёт многоуровневая версия из studvesnaLocations().
        Room r = base(assets, Levels.ALL[index].title, index);

        for (int rr = 3; rr <= 6; rr++) r.setWall(7, rr, true);
        for (int rr = 8; rr <= 11; rr++) r.setWall(15, rr, true);

        r.addInteractable(new Interactable(assets, 260, 200,
            "Наколенники на стуле.\n\"Без них я бы не вышла на сцену.\""));
        r.addInteractable(new Interactable(assets, 500, 500,
            "Тейп лежит на столе рядом с бинтами.\n\"Каждый виток — это шаг к цели.\""));
        r.addInteractable(new Interactable(assets, 950, 400,
            "Энергетик на подоконнике.\n\"Горький, но необходимый.\""));
        r.addInteractable(new Interactable(assets, 220, 560,
            "Курилка. Здесь нашлось старое фото — вы смеётесь.\n\"Здесь я выдыхала перед самым важным.\""));

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
