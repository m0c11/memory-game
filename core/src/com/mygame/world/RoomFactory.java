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

    /** Количество домиков на уровне 5. */
    public static final int HORROR_HOUSE_COUNT = 4;

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

        // ---- Оформление из JSON (assets/rooms/hall.json) ----
        RoomLayout.apply(r, assets, "rooms/hall.json");

        // Концертный свет над сценой (цветные прожекторы, "настраивают свет")
        r.addLightCue(new Room.LightCue(13 * T, 13.5f * T, 200, 160,
            new Color(1f, 0.3f, 0.3f, 1f)));   // красный
        r.addLightCue(new Room.LightCue(7 * T, 13.5f * T, 180, 140,
            new Color(0.3f, 0.5f, 1f, 1f)));    // синий
        r.addLightCue(new Room.LightCue(10 * T, 13f * T, 220, 180,
            new Color(1f, 0.9f, 0.4f, 1f)));    // тёплый белый
        r.addLightCue(new Room.LightCue(16 * T, 13.2f * T, 160, 120,
            new Color(0.8f, 0.3f, 1f, 1f)));    // фиолетовый

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
        RoomLayout.apply(r, assets, "rooms/dressing.json");

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
        RoomLayout.apply(r, assets, "rooms/backstage.json");

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

    // ==================== УРОВЕНЬ 5: ШКОЛА АКТИВА (ХОРРОР) ====================

    /**
     * База отдыха — это основная локация уровня 5 (большой открытый мир).
     * Домики — отдельные комнаты, в которые заходит игрок через порталы на базе.
     */

    /** Все домики уровня 5. */
    public static List<Room> horrorHouses(Assets assets, int index) {
        List<Room> houses = new java.util.ArrayList<>();
        for (int h = 0; h < HORROR_HOUSE_COUNT; h++) {
            houses.add(horrorHouse(assets, index, h));
        }
        return houses;
    }

    /** Один домик: комната с кроватями, бутылками под кроватями и дверью выхода. */
    public static Room horrorHouse(Assets assets, int index, int houseNum) {
        Room r = base(assets, Levels.ALL[index].title + " · домик " + (houseNum + 1), index);
        final float T = Room.TILE;

        // ---------- ОБЛИК ДОМИКА: стены ----------
        houseWalls(r, houseNum, T);

        // Проходы в стенах (дверные проёмы между кроватями)
        houseWallDoors(r, houseNum, T);

        // ---------- Дверь выхода из домика (внизу комнаты) ----------
        r.setDoor(12 * T, T, 2 * T, T);

        // ---------- НАПОЛНЕНИЕ: бутылки под кроватями ----------
        // houseItems(assets, r, houseNum, T);  // убрано для теста — только 1 бутылка на базе

        return r;
    }

    // ------- Облик домиков (стены) -------

    private static void houseWalls(Room r, int houseNum, float T) {
        switch (houseNum) {
            case 0: houseWallsA(r, T); break;
            case 1: houseWallsB(r, T); break;
            case 2: houseWallsC(r, T); break;
            case 3: houseWallsD(r, T); break;
        }
    }

    private static void houseWallsA(Room r, float T) {
        // Кровати по периметру
        for (int c = 2; c <= 8; c++) r.setWall(c, 3, true);
        for (int c = 17; c <= 23; c++) r.setWall(c, 3, true);
        for (int c = 2; c <= 8; c++) r.setWall(c, 11, true);
        for (int c = 17; c <= 23; c++) r.setWall(c, 11, true);
        for (int rr = 4; rr <= 10; rr++) r.setWall(2, rr, true);
        for (int rr = 4; rr <= 10; rr++) r.setWall(23, rr, true);
    }

    private static void houseWallsB(Room r, float T) {
        // Только верхний и нижний ряд
        for (int c = 2; c <= 8; c++) r.setWall(c, 3, true);
        for (int c = 17; c <= 23; c++) r.setWall(c, 3, true);
        for (int c = 2; c <= 8; c++) r.setWall(c, 11, true);
        for (int c = 17; c <= 23; c++) r.setWall(c, 11, true);
    }

    private static void houseWallsC(Room r, float T) {
        // Только боковые ряды + пара в центре
        for (int rr = 4; rr <= 10; rr++) r.setWall(2, rr, true);
        for (int rr = 4; rr <= 10; rr++) r.setWall(23, rr, true);
        for (int c = 6; c <= 9; c++) r.setWall(c, 5, true);
        for (int c = 6; c <= 9; c++) r.setWall(c, 9, true);
    }

    private static void houseWallsD(Room r, float T) {
        // Минималистично: пара кроватей в углу
        for (int c = 3; c <= 5; c++) r.setWall(c, 4, true);
        for (int c = 3; c <= 5; c++) r.setWall(c, 5, true);
        for (int rr = 9; rr <= 11; rr++) r.setWall(19, rr, true);
        for (int rr = 9; rr <= 11; rr++) r.setWall(20, rr, true);
    }

    // ------- Проходы в стенах -------

    private static void houseWallDoors(Room r, int houseNum, float T) {
        switch (houseNum) {
            case 0:
                r.openDoorTiles(5, 3, 3);
                r.openDoorTiles(20, 3, 3);
                r.openDoorTiles(5, 11, 11);
                r.openDoorTiles(20, 11, 11);
                r.openDoorTiles(2, 6, 6);
                r.openDoorTiles(23, 6, 6);
                break;
            case 1:
                r.openDoorTiles(5, 3, 3);
                r.openDoorTiles(20, 3, 3);
                r.openDoorTiles(5, 11, 11);
                r.openDoorTiles(20, 11, 11);
                break;
            case 2:
                r.openDoorTiles(2, 5, 5);
                r.openDoorTiles(23, 5, 5);
                r.openDoorTiles(7, 5, 5);
                r.openDoorTiles(7, 9, 9);
                break;
            case 3:
                r.openDoorTiles(4, 4, 4);
                r.openDoorTiles(19, 9, 9);
                break;
        }
    }

    // ------- Наполнение домиков (предметы) -------

    private static void houseItems(Assets assets, Room r, int houseNum, float T) {
        switch (houseNum) {
            case 0: houseItemsA(assets, r, T); break;
            case 1: houseItemsB(assets, r, T); break;
            case 2: houseItemsC(assets, r, T); break;
            case 3: houseItemsD(assets, r, T); break;
        }
    }

    private static void houseItemsA(Assets assets, Room r, float T) {
        r.addInteractable(new Interactable(assets, 3 * T + 24, 4 * T + 24,
            "Под кроватью — бутылка!\n«На чёрный день»."));
        r.addInteractable(new Interactable(assets, 7 * T + 24, 4 * T + 24,
            "Бутылка под кроватью.\n«На всякий случай»."));
        r.addInteractable(new Interactable(assets, 3 * T + 24, 10 * T + 24,
            "Ещё одна бутылка.\n«Запас никогда не помешает»."));
        r.addInteractable(new Interactable(assets, 7 * T + 24, 10 * T + 24,
            "Бутылка найдена!\n«Тихо, все спят»."));
    }

    private static void houseItemsB(Assets assets, Room r, float T) {
        r.addInteractable(new Interactable(assets, 18 * T + 24, 4 * T + 24,
            "Под кроватью — бутылка.\n«Для храбрости»."));
        r.addInteractable(new Interactable(assets, 22 * T + 24, 4 * T + 24,
            "Бутылка под кроватью.\n«Секретный запас»."));
        r.addInteractable(new Interactable(assets, 18 * T + 24, 10 * T + 24,
            "Ещё одна!\n«Чем больше, тем лучше»."));
    }

    private static void houseItemsC(Assets assets, Room r, float T) {
        r.addInteractable(new Interactable(assets, 4 * T + 24, 5 * T + 24,
            "Под кроватью — бутылка!\n«На чёрный день»."));
        r.addInteractable(new Interactable(assets, 19 * T + 24, 5 * T + 24,
            "Бутылка под кроватью.\n«На всякий случай»."));
        r.addInteractable(new Interactable(assets, 4 * T + 24, 9 * T + 24,
            "Ещё одна бутылка.\n«Запас никогда не помешает»."));
        r.addInteractable(new Interactable(assets, 19 * T + 24, 9 * T + 24,
            "Бутылка найдена!\n«Тихо, все спят»."));
    }

    private static void houseItemsD(Assets assets, Room r, float T) {
        r.addInteractable(new Interactable(assets, 15 * T + 24, 4 * T + 24,
            "Под кроватью — бутылка.\n«Для храбрости»."));
        r.addInteractable(new Interactable(assets, 20 * T + 24, 4 * T + 24,
            "Бутылка под кроватью.\n«Секретный запас»."));
        r.addInteractable(new Interactable(assets, 15 * T + 24, 10 * T + 24,
            "Ещё одна!\n«Чем больше, тем лучше»."));
        r.addInteractable(new Interactable(assets, 20 * T + 24, 10 * T + 24,
            "Последняя бутылка!\n«Теперь можно и выпить»."));
    }
}
