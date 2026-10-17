package com.mygame.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygame.MyGame;
import com.mygame.assets.Assets;
import com.mygame.entities.Player;
import com.mygame.levels.Levels;
import com.mygame.ui.DialogueBox;
import com.mygame.world.Bush;
import com.mygame.world.RoomFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * База отдыха «Веневитино» — основная локация уровня 5 «Школа Актива».
 *
 * Это большой открытый мир (камера следует за игроком, можно уходить за границы экрана).
 * На базе есть:
 * - Кусты — можно спрятаться (R) и искать бутылки (E, открывает оверлей поиска)
 * - Домики — здания с дверями, в которые можно заходить (HorrorHouseScreen)
 * - Организаторы — патрулируют базу, при касании показывается сценка и герой вытаскивается к домику
 *
 * Бутылки найденные и в кустах на базе, и в домиках — плюсуются к общему счетчику.
 * При возврате из домика герой оказывается у этого домика снаружи, прогресс сохраняется.
 */
public class HorrorBaseScreen extends ScreenAdapter {

    public static final float W = 1248f;
    public static final float H = 720f;

    // Размер мира (большой открытый мир, больше чем экран)
    private static final float WORLD_W = 2400f;
    private static final float WORLD_H = 2400f;

    private final MyGame game;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private Player player;
    private final DialogueBox dialogue;

    // Кусты на базе
    private List<Bush> bushes = new ArrayList<>();

    // Домики (записи о дверях и куда ведут)
    private List<HouseEntry> houseEntries = new ArrayList<>();

    // Организаторы (враги)
    private List<Organizer> organizers = new ArrayList<>();

    // Общий счетчик бутылок (на базе + в домиках)
    private int bottlesFound = 0;
    private int bottlesTotal;

    // Оверлей поиска в кусте
    private BushSearch bushSearch = null;

    // Состояние
    private boolean caught = false;
    private float caughtTimer = 0f;
    private final GlyphLayout layout = new GlyphLayout();
    private final Vector2 mousePos = new Vector2();
    private Vector2 flashlightDir = new Vector2(1, 0);
    private float flicker = 1f;

    // Прятание в кусте
    private boolean inBush = false;
    private Bush currentBush = null;

    // Флаг инициализации — чтобы не сбрасывать всё при возврате из домика
    private boolean initialized = false;

    /** Запись о домике: дверь на базе и номер домика. */
    private static class HouseEntry {
        Rectangle bounds;   // дверь в домик (на базе)
        int houseIndex;     // какой домик открывать
        float spawnX, spawnY; // где появиться на базе при выходе из домика
        String name;

        HouseEntry(float x, float y, float w, float h, int houseIndex, float spawnX, float spawnY, String name) {
            this.bounds = new Rectangle(x, y, w, h);
            this.houseIndex = houseIndex;
            this.spawnX = spawnX;
            this.spawnY = spawnY;
            this.name = name;
        }
    }

    /** Организатор — патрулирует по базе. */
    private static class Organizer {
        float x, y;
        final float minX, maxX, minY, maxY;
        final float speed;
        boolean active = true;
        private float time = 0f;

        Organizer(float minX, float minY, float maxX, float maxY, float speed) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.speed = speed;
            this.x = minX;
            this.y = minY;
        }

        void update(float delta) {
            if (!active) return;
            time += delta * speed;
            float t = (float) Math.sin(time);
            x = minX + (maxX - minX) * (t * 0.5f + 0.5f);
            y = minY + (maxY - minY) * (float) Math.sin(time * 0.5f) * 0.3f;
        }

        void draw(com.badlogic.gdx.graphics.g2d.SpriteBatch batch, Assets assets) {
            batch.setColor(0.15f, 0.1f, 0.2f, 0.9f);
            batch.draw(assets.pixel, x - 16, y - 32, 32, 64);
            batch.setColor(Color.WHITE);
        }
    }

    public HorrorBaseScreen(MyGame game) {
        this.game = game;
        this.dialogue = new DialogueBox(game.assets);
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(W, H, camera);
        camera.position.set(W / 2f, H / 2f, 0f);
        camera.update();

        if (!initialized) {
            // Первый запуск — создаём всё
            player = new Player(WORLD_W / 2f, WORLD_H / 2f, game.assets);
            createBushes();
            createHouses();
            createOrganizers();

            // Подсчёт всего бутылок (кусты на базе + бутылки в домиках)
            bottlesTotal = 0;
            for (Bush bush : bushes) {
                if (bush.hasBottle()) bottlesTotal++;
            }
            for (int h = 0; h < RoomFactory.HORROR_HOUSE_COUNT; h++) {
                bottlesTotal += RoomFactory.horrorHouse(game.assets, 5, h).getTotalCount();
            }

            initialized = true;
        } else {
            // Возвращаемся из домика — просто перемещаем камеру на игрока
            camera.position.set(player.getCenterX(), player.getCenterY(), 0f);
            camera.update();
        }

        dialogue.show(game.assets.girlPortrait,
                      "База отдыха «Веневитино». Ночь.\n" +
                      "Все спят, но организаторы делают обходы.\n" +
                      "Найди все бутылки, не попадись организаторам.\n" +
                      "WASD — ходить, E — поиск в кусте / взять предмет.\n" +
                      "R — спрятаться в куст.\n" +
                      "Заходи в домики через двери. Фонарик — за курсором.");
    }

    private void createBushes() {
        bushes.clear();
        // Для теста: один куст с бутылкой рядом со стартом героя (центр мира)
        bushes.add(new Bush(game.assets, WORLD_W / 2f, WORLD_H / 2f, true));
        // Остальные кусты без бутылок
        bushes.add(new Bush(game.assets, 600, 500, false));
        bushes.add(new Bush(game.assets, 900, 200, false));
        bushes.add(new Bush(game.assets, 1200, 700, false));
        bushes.add(new Bush(game.assets, 1500, 300, false));
        bushes.add(new Bush(game.assets, 1800, 600, false));
        bushes.add(new Bush(game.assets, 200, 1500, false));
        bushes.add(new Bush(game.assets, 500, 1800, false));
        bushes.add(new Bush(game.assets, 1000, 1500, false));
        bushes.add(new Bush(game.assets, 1700, 1800, false));
        bushes.add(new Bush(game.assets, 2100, 1500, false));
        bushes.add(new Bush(game.assets, 2200, 900, false));
    }

    private void createHouses() {
        houseEntries.clear();
        // 4 домика на базе, каждый — отдельное здание с дверью
        // Домик 1 (левый верх)
        houseEntries.add(new HouseEntry(400, 400, 60, 60, 0, 430, 480, "Домик 1"));
        // Домик 2 (правый верх)
        houseEntries.add(new HouseEntry(1600, 400, 60, 60, 1, 1630, 480, "Домик 2"));
        // Домик 3 (левый низ)
        houseEntries.add(new HouseEntry(400, 1600, 60, 60, 2, 430, 1680, "Домик 3"));
        // Домик 4 (правый низ)
        houseEntries.add(new HouseEntry(1600, 1600, 60, 60, 3, 1630, 1680, "Домик 4"));
    }

    private void createOrganizers() {
        organizers.clear();
        // Организаторы патрулируют разные зоны базы
        organizers.add(new Organizer(200, 200, 700, 200, 1.0f));
        organizers.add(new Organizer(1500, 200, 2000, 200, 1.2f));
        organizers.add(new Organizer(200, 1500, 700, 1500, 0.9f));
        organizers.add(new Organizer(1500, 1500, 2000, 1500, 1.1f));
        organizers.add(new Organizer(900, 900, 1500, 900, 1.3f));
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1f / 30f);

        if (caught) {
            caughtTimer += delta;
            if (caughtTimer > 3f) {
                // После scènes — вытаскиваем героя к входу в случайный домик
                caught = false;
                caughtTimer = 0f;
                teleportToHouseEntrance();
            }
            draw(delta);
            return;
        }

        if (handleInput()) return;
        if (update(delta)) return;
        draw(delta);
    }

    private boolean handleInput() {
        if (dialogue.isVisible()) {
            if (confirmPressed()) dialogue.advance();
            return false;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (dialogue.isVisible()) {
                dialogue.advance();
            }
            return false;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            interact();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            toggleBushHide();
        }
        return false;
    }

    private boolean confirmPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.Z)
            || Gdx.input.isKeyJustPressed(Input.Keys.E)
            || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
    }

    private void interact() {
        if (player == null) return;
        float px = player.getCenterX();
        float py = player.getCenterY();

        // 1. Проверяем кусты
        Bush nearestBush = null;
        float nearestDist = Bush.INTERACT_RADIUS;
        for (Bush bush : bushes) {
            if (bush.isSearched()) continue;
            float d = distance(px, py, bush.getCenterX(), bush.getCenterY());
            if (d < nearestDist) {
                nearestDist = d;
                nearestBush = bush;
            }
        }

        if (nearestBush != null) {
            // Открываем поиск в кусте (оверлей)
            nearestBush.markSearched();
            if (nearestBush.hasBottle()) {
                bottlesFound++;
                dialogue.show("Бутылка найдена в кусте! " + bottlesFound + " / " + bottlesTotal);
                game.assets.sfxChime.play(0.7f);
                // Все бутылки найдены — запускаем босс-пакман
                if (bottlesFound >= bottlesTotal) {
                    game.setScreen(new PacmanBossScreen(game));
                }
            } else {
                dialogue.show("В кусте ничего нет...");
            }
            return;
        }

        // 2. Проверяем входы в домики
        for (HouseEntry entry : houseEntries) {
            if (entry.bounds.overlaps(player.getBounds())) {
                // Заходим в домик — передаём текущий счетчик бутылок и общий total
                java.util.function.IntConsumer onHouseExit = (finalBottles) -> {
                    // Возвращаемся на базу у входа в этот домик
                    bottlesFound = finalBottles;
                    player.setPosition(entry.spawnX, entry.spawnY);
                    game.setScreen(this);
                };
                game.setScreen(new HorrorHouseScreen(game, entry.houseIndex, onHouseExit, bottlesFound, bottlesTotal));
                return;
            }
        }
    }

    /** Спрятаться в куст / выйти из куста по кнопке R. */
    private void toggleBushHide() {
        if (player == null) return;
        float px = player.getCenterX();
        float py = player.getCenterY();

        if (!inBush) {
            // Ищем ближайший неосмотренный куст
            Bush nearest = null;
            float nearestDist = Bush.INTERACT_RADIUS;
            for (Bush bush : bushes) {
                if (bush.isSearched()) continue;
                float d = distance(px, py, bush.getCenterX(), bush.getCenterY());
                if (d < nearestDist) {
                    nearestDist = d;
                    nearest = bush;
                }
            }
            if (nearest != null) {
                inBush = true;
                currentBush = nearest;
                dialogue.show("Вы спрятались в кусте.\nОрганизаторы вас не видят.\nНажмите R, чтобы выйти.");
                game.assets.sfxChime.play(0.5f);
            } else {
                dialogue.show("Рядом нет куста, в котором можно спрятаться.");
            }
        } else {
            inBush = false;
            dialogue.show("Вы вышли из куста.");
            currentBush = null;
            game.assets.sfxChime.play(0.5f);
        }
    }

    private boolean update(float delta) {
        // Обновляем камеру (следует за игроком, ограничиваем границами мира)
        float camX = MathUtils.clamp(player.getCenterX(), W / 2f, WORLD_W - W / 2f);
        float camY = MathUtils.clamp(player.getCenterY(), H / 2f, WORLD_H - H / 2f);
        camera.position.set(camX, camY, 0f);
        camera.update();

        if (!dialogue.isVisible() && !caught) {
            player.update(delta, null); // на базе нет коллизий с комнатой, ходим свободно
        }

        dialogue.update(delta);

        // Обновляем организаторов
        for (Organizer org : organizers) {
            if (org.active) {
                org.update(delta);
            }
        }

        // Проверка касания организатором — не ловят, если игрок в кусте
        if (!caught && !dialogue.isVisible() && !inBush) {
            for (Organizer org : organizers) {
                float dist = distance(player.getCenterX(), player.getCenterY(), org.x, org.y);
                if (dist < 60f) {
                    caught = true;
                    caughtTimer = 0f;
                    dialogue.show("Организатор тебя нашёл!\n«А что тут не спим?»\nТебя вытащили к домику.");
                    game.assets.sfxThud.play(0.9f);
                    break;
                }
            }
        }

        return false;
    }

    private void teleportToHouseEntrance() {
        // Вытаскиваем героя к входу в случайный домик
        if (!houseEntries.isEmpty()) {
            HouseEntry entry = houseEntries.get(MathUtils.random(houseEntries.size() - 1));
            player.setPosition(entry.spawnX, entry.spawnY);
        }
    }

    private void draw(float delta) {
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.03f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Рисуем кусты
        for (Bush bush : bushes) {
            bush.draw(game.batch);
        }

        // Рисуем домики (просто прямоугольники с дверями)
        game.batch.setColor(0.2f, 0.15f, 0.1f, 0.9f);
        for (HouseEntry entry : houseEntries) {
            game.batch.draw(game.assets.pixel, entry.bounds.x, entry.bounds.y, entry.bounds.width, entry.bounds.height);
            // Дверь
            game.batch.setColor(0.8f, 0.6f, 0.3f, 0.9f);
            game.batch.draw(game.assets.pixel, entry.bounds.x + 20, entry.bounds.y + 20, 20, 20);
            game.batch.setColor(0.2f, 0.15f, 0.1f, 0.9f);
        }
        game.batch.setColor(Color.WHITE);

        // Рисуем организаторов
        for (Organizer org : organizers) {
            if (org.active) {
                org.draw(game.batch, game.assets);
            }
        }

        // Игрок не рисуется, если спрятался в кусте
        if (!inBush) {
            player.draw(game.batch);
        }

        game.batch.end();

        // HUD (в экранных координатах)
        camera.position.set(W / 2f, H / 2f, 0f);
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        Assets a = game.assets;
        a.fontMedium.setColor(1f, 0.9f, 0.6f, 0.95f);
        layout.setText(a.fontMedium, "Бутылки: " + bottlesFound + " / " + bottlesTotal);
        a.fontMedium.draw(game.batch, layout, 36f, H - 30f);

        a.fontSmall.setColor(0.6f, 0.6f, 0.65f, 0.8f);
        if (inBush) {
            layout.setText(a.fontSmall, "Вы в кусте (R — выйти). E — поиск, WASD — ходить.");
        } else {
            layout.setText(a.fontSmall, "WASD — ходить, E — поиск/взять, R — спрятаться в куст, фонарик — за курсором");
        }
        a.fontSmall.draw(game.batch, layout, (W - layout.width) / 2f, 26f);

        // Координаты для отладки
        a.fontSmall.setColor(0.4f, 0.4f, 0.5f, 0.7f);
        layout.setText(a.fontSmall, String.format("X=%.0f Y=%.0f", player.getCenterX(), player.getCenterY()));
        a.fontSmall.draw(game.batch, layout, 36f, 56f);

        dialogue.draw(game.batch, W, H);
        game.batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }
}
