package com.mygame.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygame.MyGame;
import com.mygame.assets.Assets;
import com.mygame.levels.Levels;

import java.util.ArrayList;
import java.util.List;

/**
 * Босс-пакман для уровня 5 «Школа Актива».
 *
 * Классическая механика пакмана:
 * - Лабиринт со стенами, точками и усилителями
 * - Игрок (жёлтый пакман) ходит по лабиринту, собирает точки
 * - 4 призрака патрулируют и преследуют пакмана
 * - При усилителе призраки становятся уязвимыми — пакман может их съесть
 * - 3 жизни, счёт, режим отсчёта перед началом
 * - Когда все точки собраны — уровень пройден
 */
public class PacmanBossScreen extends ScreenAdapter {

    public static final float W = 1248f;
    public static final float H = 720f;

    // Размеры лабиринта (в клетках)
    private static final int COLS = 19;
    private static final int ROWS = 21;
    private static final float TILE = 32f;

    // Состояния игры
    private static final int WAITING = 0;
    private static final int COUNTDOWN = 1;
    private static final int PLAYING = 2;
    private static final int EATEN_PAUSE = 3;
    private static final int DYING = 4;
    private static final int PAUSE = 5;

    private final MyGame game;
    private OrthographicCamera camera;
    private FitViewport viewport;

    // Лабиринт: 0=стена, 1=точка, 2=пусто, 3=усилитель
    private final int[][] map = new int[ROWS][COLS];

    private float pacX, pacY;      // позиция в пикселях
    private int pacDir = 2;        // направление (1=вниз, 2=влево, 3=вверх, 11=вправо)
    private int pacDue = 2;        // запрашиваемое направление
    private int pacLives = 3;
    private int pacScore = 0;
    private int pacEaten = 0;      // съедено точек на этом уровне
    private int pacTotalDots;

    private int gameState = WAITING;
    private int tick = 0;
    private int timerStart = 0;

    private final List<Ghost> ghosts = new ArrayList<>();
    private final GlyphLayout layout = new GlyphLayout();

    private boolean finished = false;
    private float finishTimer = 0f;

    // Лабиринт классического пакмана (упрощённый)
    private static final int[][] MAZE = {
        {0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0},
        {0,1,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,1,0},
        {0,3,0,0,1,0,0,0,1,0,1,0,0,0,1,0,0,3,0},
        {0,1,0,0,1,0,0,0,1,0,1,0,0,0,1,0,0,1,0},
        {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
        {0,1,0,0,1,0,1,0,0,0,0,0,1,0,1,0,0,1,0},
        {0,1,1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,1,0},
        {0,0,0,0,1,0,0,0,1,0,1,0,0,0,1,0,0,0,0},
        {2,2,2,0,1,0,1,1,1,1,1,1,1,0,1,0,2,2,2},
        {0,0,0,0,1,0,1,0,0,3,0,0,1,0,1,0,0,0,0},
        {2,2,2,2,1,1,1,0,3,3,3,0,1,1,1,2,2,2,2},
        {0,0,0,0,1,0,1,0,0,0,0,0,1,0,1,0,0,0,0},
        {2,2,2,0,1,0,1,1,1,2,1,1,1,0,1,0,2,2,2},
        {0,0,0,0,1,0,1,0,0,0,0,0,1,0,1,0,0,0,0},
        {0,1,1,1,1,1,1,1,1,0,1,1,1,1,1,1,1,1,0},
        {0,1,0,0,1,0,0,0,1,0,1,0,0,0,1,0,0,1,0},
        {0,4,1,0,1,1,1,1,1,1,1,1,1,1,1,0,1,4,0},
        {0,0,1,0,1,0,1,0,0,0,0,0,1,0,1,0,1,0,0},
        {0,1,1,1,1,0,1,1,1,0,1,1,1,0,1,1,1,1,0},
        {0,1,0,0,0,0,0,0,1,0,1,0,0,0,0,0,0,1,0},
        {0,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,0},
    };

    public PacmanBossScreen(MyGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(W, H, camera);
        camera.position.set(W / 2f, H / 2f, 0f);
        camera.update();

        // Загружаем лабиринт
        int pacStartCol = 9, pacStartRow = 16;
        int dotCount = 0;
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                map[r][c] = MAZE[r][c];
                if (MAZE[r][c] == 1 || MAZE[r][c] == 3) dotCount++;
                if (MAZE[r][c] == 4) {
                    pacStartCol = c;
                    pacStartRow = r;
                }
            }
        }
        pacTotalDots = dotCount;

        pacX = pacStartCol * TILE + TILE / 2f;
        pacY = pacStartRow * TILE + TILE / 2f;
        pacDir = 2;
        pacDue = 2;

        // 4 призрака
        ghosts.clear();
        ghosts.add(new Ghost(9 * TILE + TILE / 2f, 10 * TILE + TILE / 2f, Color.RED, 0));
        ghosts.add(new Ghost(9 * TILE + TILE / 2f, 11 * TILE + TILE / 2f, Color.PINK, 1));
        ghosts.add(new Ghost(8 * TILE + TILE / 2f, 10 * TILE + TILE / 2f, Color.CYAN, 2));
        ghosts.add(new Ghost(10 * TILE + TILE / 2f, 10 * TILE + TILE / 2f, Color.ORANGE, 3));

        gameState = WAITING;
        tick = 0;
        timerStart = 0;

        game.assets.sfxChime.play(0.9f);
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1f / 30f);

        if (finished) {
            finishTimer += delta;
            if (finishTimer > 2f) {
                finishLevel();
            }
            draw(delta);
            return;
        }

        if (gameState != PAUSE) {
            tick++;
        }

        update(delta);
        draw(delta);
    }

    private void update(float delta) {
        // Обработка ввода
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            if (gameState == WAITING) {
                // Выход из пакмана на карту без завершения уровня
                game.setScreen(new com.mygame.screens.MapScreen(game));
                return;
            }
            if (gameState == PAUSE) {
                gameState = PLAYING;
            } else if (gameState == PLAYING || gameState == COUNTDOWN) {
                gameState = PAUSE;
            }
        }

        if (gameState != PAUSE) {
            boolean directionPressed = false;
            if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.A)) {
                pacDue = 2;
                directionPressed = true;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT) || Gdx.input.isKeyJustPressed(Input.Keys.D)) {
                pacDue = 11;
                directionPressed = true;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
                pacDue = 3;
                directionPressed = true;
            }
            if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
                pacDue = 1;
                directionPressed = true;
            }

            if (gameState == WAITING && directionPressed) {
                startLevel();
                pacDir = pacDue;
                gameState = PLAYING;
            }

            // Любая клавиша движения пропускает отсчет и сразу начинает игру
            if (gameState == COUNTDOWN && directionPressed) {
                gameState = PLAYING;
            }
        }

        if (gameState == PLAYING) {
            updatePacman(delta);
        }

        // Обновление призраков (только в PLAYING)
        if (gameState == PLAYING) {
            for (Ghost g : ghosts) {
                g.update(delta, pacX, pacY, map, TILE);
            }
        }

        // Проверка столкновений пакмана с призраками (только в PLAYING)
        if (gameState == PLAYING) {
            for (Ghost g : ghosts) {
                if (Math.abs(g.x - pacX) < 14f && Math.abs(g.y - pacY) < 14f) {
                    if (g.isVulnerable()) {
                        // Пакман съедает призрака
                        g.eat();
                        pacScore += 200;
                        game.assets.sfxChime.play(0.5f);
                    } else if (!g.isEaten()) {
                        // Призрак съедает пакмана
                        loseLife();
                    }
                }
            }
        }

        // Режимы игры
        if (gameState == COUNTDOWN) {
            int diff = 3 + (timerStart - tick) / 30;
            if (diff <= 0) {
                gameState = PLAYING;
            }
        } else if (gameState == DYING) {
            if (tick - timerStart > 60) { // 2 секунды
                loseLife();
            }
        } else if (gameState == EATEN_PAUSE) {
            if (tick - timerStart > 10) {
                gameState = PLAYING;
            }
        }

        // Проверка победы
        if (pacEaten >= pacTotalDots && !finished) {
            finished = true;
            finishTimer = 0f;
            game.assets.sfxChime.play(1f);
        }
    }

    private void updatePacman(float delta) {
        float speed = 150f; // Ещё быстрее

        // Пробуем сменить направление, только если на целой клетке
        if (pacDue != pacDir && onGridSquare(pacX, pacY)) {
            float testX = pacX;
            float testY = pacY;
            if (pacDue == 2) testX = pacX - TILE;
            if (pacDue == 11) testX = pacX + TILE;
            if (pacDue == 3) testY = pacY + TILE;
            if (pacDue == 1) testY = pacY - TILE;

            int tc = MathUtils.round(testX / TILE);
            int tr = MathUtils.round(testY / TILE);
            if (tc >= 0 && tc < COLS && tr >= 0 && tr < ROWS && map[tr][tc] != 0) {
                pacDir = pacDue;
            }
        }

        // Движение в текущем направлении
        float nx = pacX, ny = pacY;
        if (pacDir == 2) nx -= speed * delta;          // влево
        if (pacDir == 11) nx += speed * delta;         // вправо
        if (pacDir == 3) ny += speed * delta;          // вверх
        if (pacDir == 1) ny -= speed * delta;          // вниз

        // Проверка стен
        if (wallAt(nx, ny)) {
            pacDir = 4; // NONE - стоп
        } else {
            pacX = nx;
            pacY = ny;
        }

        // Телепорт через края лабиринта
        if (pacY > 10 * TILE && pacY < 11 * TILE) {
            if (pacX > 18 * TILE) pacX = 0.5f * TILE;
            if (pacX < 0) pacX = 18.5f * TILE;
        }

        // Сбор точки/усилителя
        if (onGridSquare(pacX, pacY)) {
            int col = MathUtils.round(pacX / TILE);
            int row = MathUtils.round(pacY / TILE);
            if (col >= 0 && col < COLS && row >= 0 && row < ROWS) {
                int cell = map[row][col];
                if (cell == 1) { // точка
                    map[row][col] = 2;
                    pacScore += 10;
                    pacEaten++;
                    game.assets.sfxChime.play(0.2f);
                } else if (cell == 3) { // усилитель
                    map[row][col] = 2;
                    pacScore += 50;
                    pacEaten++;
                    game.assets.sfxChime.play(0.4f);
                    for (Ghost g : ghosts) {
                        g.makeVulnerable(8f);
                    }
                }
            }
        }
    }

    private boolean wallAt(float x, float y) {
        int c = MathUtils.round(x / TILE);
        int r = MathUtils.round(y / TILE);
        if (c < 0 || c >= COLS || r < 0 || r >= ROWS) return true;
        return map[r][c] == 0;
    }

    private boolean onGridSquare(float x, float y) {
        return x % TILE < 2f || x % TILE > TILE - 2f;
    }

    private void loseLife() {
        pacLives--;
        if (pacLives <= 0) {
            // Игра окончена — возврат на карту без завершения уровня
            game.setScreen(new com.mygame.screens.MapScreen(game));
        } else {
            gameState = WAITING;
            timerStart = tick;
            pacX = 9 * TILE + TILE / 2f;
            pacY = 16 * TILE + TILE / 2f;
            pacDir = 2;
            pacDue = 2;
        }
    }

    private void startLevel() {
        pacX = 9 * TILE + TILE / 2f;
        pacY = 16 * TILE + TILE / 2f;
        pacDir = 2;
        pacDue = 2;
        for (Ghost g : ghosts) {
            g.reset();
        }
        timerStart = tick;
        gameState = COUNTDOWN;
    }

    private void draw(float delta) {
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float offsetX = (W - COLS * TILE) / 2f;
        float offsetY = (H - ROWS * TILE) / 2f + 40f;

        viewport.apply();
        camera.position.set(W / 2f, H / 2f, 0f);
        camera.update();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Стены лабиринта (синие)
        game.batch.setColor(0f, 0f, 1f, 1f);
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                if (map[r][c] == 0) {
                    game.batch.draw(game.assets.pixel, offsetX + c * TILE, offsetY + r * TILE, TILE, TILE);
                }
            }
        }

        // Точки и усилители
        for (int r = 0; r < ROWS; r++) {
            for (int c = 0; c < COLS; c++) {
                float cellX = offsetX + c * TILE;
                float cellY = offsetY + r * TILE;
                if (map[r][c] == 1) {
                    // Маленькая точка
                    game.batch.setColor(Color.WHITE);
                    game.batch.draw(game.assets.pacmanDot, cellX + TILE * 0.34f, cellY + TILE * 0.34f, TILE * 0.32f, TILE * 0.32f);
                } else if (map[r][c] == 3) {
                    // Увеличенный усилитель (мигающий)
                    float pulse = 0.5f + 0.5f * MathUtils.sin(tick * 0.3f);
                    game.batch.setColor(1f, 1f, 1f, pulse);
                    game.batch.draw(game.assets.pixel, cellX + TILE * 0.3f, cellY + TILE * 0.3f, TILE * 0.4f, TILE * 0.4f);
                }
            }
        }

        // Герой — кадры из пользовательского спрайт-листа. Для левого направления
        // используем кадры, где персонаж смотрит влево; для остальных — вправо.
        int playerFrame;
        if (pacDir == 2) playerFrame = 7 + (tick / 9) % 2;
        else if (pacDir == 11) playerFrame = 2 + (tick / 9) % 3;
        else if (pacDir == 3) playerFrame = 1;
        else if (pacDir == 1) playerFrame = 4;
        else playerFrame = (pacDir == 2) ? 7 : 2;
        game.batch.setColor(Color.WHITE);
        game.batch.draw(game.assets.pacmanPlayer[playerFrame],
            offsetX + pacX - 18, offsetY + pacY - 20, 36, 40);

        // Призраки
        for (Ghost g : ghosts) {
            g.draw(game.batch, game.assets, offsetX, offsetY, TILE, tick);
        }

        game.batch.setColor(Color.WHITE);
        game.batch.end();

        // HUD
        camera.position.set(W / 2f, H / 2f, 0f);
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        Assets a = game.assets;
        a.fontMedium.setColor(1f, 1f, 0.6f, 0.95f);
        layout.setText(a.fontMedium, "Счёт: " + pacScore + "   Жизни: " + pacLives);
        a.fontMedium.draw(game.batch, layout, 36f, H - 30f);

        layout.setText(a.fontMedium, "Точки: " + pacEaten + " / " + pacTotalDots);
        a.fontMedium.draw(game.batch, layout, W - 300f, H - 30f);

        // Состояние игры
        if (gameState == WAITING) {
            a.fontMenu.setColor(1f, 1f, 0f, 0.95f);
            layout.setText(a.fontMenu, "Нажмите любую клавишу движения для старта");
            a.fontMenu.draw(game.batch, layout, (W - layout.width) / 2f, H / 2f);
        } else if (gameState == COUNTDOWN) {
            int diff = 3 + (timerStart - tick) / 30;
            if (diff > 0) {
                a.fontMenu.setColor(1f, 1f, 0f, 0.95f);
                layout.setText(a.fontMenu, "Старт через: " + diff);
                a.fontMenu.draw(game.batch, layout, (W - layout.width) / 2f, H / 2f);
            }
        } else if (finished) {
            a.fontMenu.setColor(0f, 1f, 0f, 0.95f);
            layout.setText(a.fontMenu, "ПОБЕДА! Все бутылки и точки собраны!");
            a.fontMenu.draw(game.batch, layout, (W - layout.width) / 2f, H / 2f);
        }

        game.batch.end();
    }

    private void finishLevel() {
        game.save.completeLevel(5);
        if (5 >= Levels.COUNT - 1) {
            game.setScreen(new com.mygame.screens.EndScreen(game));
        } else {
            game.setScreen(new com.mygame.screens.MapScreen(game, 5));
        }
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }

    /** Призрак классического пакмана. */
    private static class Ghost {
        float x, y;
        float vulnerableTime = 0f;
        boolean eaten = false;
        final int spriteIndex;

        Ghost(float x, float y, Color color, int spriteIndex) {
            this.x = x;
            this.y = y;
            this.spriteIndex = spriteIndex;
        }

        void update(float delta, float pacX, float pacY, int[][] map, float tile) {
            if (eaten) {
                // Возвращаемся в центр лабиринта
                float targetX = 9 * tile + tile / 2f;
                float targetY = 10 * tile + tile / 2f;
                moveToward(targetX, targetY, delta, map, tile);
                if (Math.abs(x - targetX) < 4f && Math.abs(y - targetY) < 4f) {
                    eaten = false;
                    vulnerableTime = 0f;
                }
                return;
            }

            if (vulnerableTime > 0f) {
                vulnerableTime -= delta;
                // Уязвимый призрак убегает
                moveAway(pacX, pacY, delta, map, tile);
            } else {
                // Преследует пакмана
                moveToward(pacX, pacY, delta, map, tile);
            }
        }

        private void moveToward(float tx, float ty, float delta, int[][] map, float tile) {
            float dx = tx - x;
            float dy = ty - y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            float speed = 70f;

            if (dist > 0f) {
                float nx = x + (dx / dist) * speed * delta;
                float ny = y + (dy / dist) * speed * delta;
                if (!wallAt(nx, ny, map, tile)) {
                    x = nx;
                    y = ny;
                } else {
                    // Пробуем пойти по оси X или Y отдельно
                    if (!wallAt(x + (dx / dist) * speed * delta, y, map, tile)) {
                        x += (dx / dist) * speed * delta;
                    } else if (!wallAt(x, y + (dy / dist) * speed * delta, map, tile)) {
                        y += (dy / dist) * speed * delta;
                    }
                }
            }
        }

        private void moveAway(float px, float py, float delta, int[][] map, float tile) {
            float dx = x - px;
            float dy = y - py;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            float speed = 50f;

            if (dist > 0f) {
                float nx = x + (dx / dist) * speed * delta;
                float ny = y + (dy / dist) * speed * delta;
                if (!wallAt(nx, ny, map, tile)) {
                    x = nx;
                    y = ny;
                }
            }
        }

        private boolean wallAt(float x, float y, int[][] map, float tile) {
            int c = MathUtils.round(x / tile);
            int r = MathUtils.round(y / tile);
            if (c < 0 || c >= COLS || r < 0 || r >= ROWS) return true;
            return map[r][c] == 0;
        }

        void makeVulnerable(float seconds) {
            vulnerableTime = seconds;
        }

        void eat() {
            eaten = true;
            vulnerableTime = 0f;
        }

        boolean isVulnerable() {
            return vulnerableTime > 0f && !eaten;
        }

        boolean isEaten() {
            return eaten;
        }

        void reset() {
            eaten = false;
            vulnerableTime = 0f;
            x = 9 * TILE + TILE / 2f;
            y = 10 * TILE + TILE / 2f;
        }

        void draw(com.badlogic.gdx.graphics.g2d.SpriteBatch batch, Assets assets, float offsetX, float offsetY, float tile, int tick) {
            if (eaten) return;

            Texture sprite = isVulnerable()
                ? assets.pacmanPower[spriteIndex]
                : assets.pacmanGhosts[spriteIndex];
            batch.setColor(Color.WHITE);
            batch.draw(sprite, offsetX + x - 19, offsetY + y - 20, 38, 40);
        }
    }
}
