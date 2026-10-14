package com.mygame.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygame.MyGame;
import com.mygame.assets.Assets;
import com.mygame.entities.Player;
import com.mygame.levels.Levels;
import com.mygame.mechanics.DistractionMeter;
import com.mygame.ui.DialogueBox;
import com.mygame.world.Interactable;
import com.mygame.world.Room;
import com.mygame.world.RoomFactory;

/**
 * Игровой экран одного уровня: комната, игрок, диалоги.
 * Когда все предметы собраны и игрок вошёл в дверь, уровень засчитывается
 * и игра возвращается на карту (после последнего уровня — на финальный экран).
 *
 * Уровень 1 («Подготовка к студвесне») играется по особой механике
 * DistractionMeter: пока героиня собирает предметы, заполняется тревожная
 * шкала — когда она полна, нужно успеть нажать ПРОБЕЛ, иначе спотыкаешься.
 */
public class GameScreen extends ScreenAdapter {

    public static final float W = 1248f;
    public static final float H = 720f;

    /** Уровень, где включается «тревожная шкала». */
    private static final int DISTRACTION_LEVEL = 1;
    /** Скорость героини в этой главе (доля от обычной). */
    private static final float DISTRACTION_SPEED = 0.85f;

    private static final float SHAKE_TIME = 0.4f;
    private static final float SHAKE_MAG = 14f;

    private final MyGame game;
    private final int levelIndex;

    private OrthographicCamera camera;
    private FitViewport viewport;

    private Player player;
    private Room room;
    private DialogueBox dialogue;

    private final boolean distractionLevel;
    private DistractionMeter meter;

    // Обратная связь механики
    private float shakeTimer = 0f;
    private float owTimer = 0f;
    private boolean dialogueWasVisible = false;

    // Событие «сорвалась»: 0 — нет, 1 — первая реплика, 2 — поддержка
    private int overwhelmedPhase = 0;
    private float savedColorAmount = 0f;
    private float grayT = 0f;

    // Для плавного появления названия комнаты сверху
    private float roomNameTimer = 0f;
    private final GlyphLayout layout = new GlyphLayout();

    public GameScreen(MyGame game, int levelIndex) {
        this.game = game;
        this.levelIndex = levelIndex;
        this.distractionLevel = levelIndex == DISTRACTION_LEVEL;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(W, H, camera);
        camera.position.set(W / 2f, H / 2f, 0f);
        camera.update();

        dialogue = new DialogueBox(game.assets);
        room = RoomFactory.createRoom(levelIndex, game.assets);
        player = new Player(200, H / 2f - Player.HEIGHT / 2f, game.assets);
        roomNameTimer = 0f;

        savedColorAmount = room.getColorAmount();

        if (distractionLevel) {
            meter = new DistractionMeter();
            player.setSpeedScale(DISTRACTION_SPEED);
            dialogue.show("Подготовка к Студвесне. Сцена уже близко.\n" +
                          "Иди: WASD или стрелки. Собери все предметы (E — взять).\n" +
                          "Следи за шкалой тревоги: как заполнится — жми ПРОБЕЛ.\n" +
                          "Промедлишь — споткнёшься.");
        } else if (levelIndex == 0) {
            // Приветствие только в самом первом уровне
            dialogue.show("Ты просыпаешься в мире, где нет ни одного цвета.\n" +
                          "Что-то важное потерялось... попробуй вспомнить.");
        }
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1f / 30f);

        if (handleInput()) return;
        if (update(delta)) return;
        draw(delta);
    }

    /** @return true, если экран был сменён и дальше рисовать не нужно. */
    private boolean handleInput() {
        if (dialogue.isVisible()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.E)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                dialogue.hide();
            }
            return false;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            // Выход на карту без засчитывания уровня
            game.setScreen(new MapScreen(game));
            return true;
        }

        if (distractionLevel && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            meter.press();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            Interactable target = room.findClosestInteractable(player.getCenterX(), player.getCenterY());
            if (target != null && !target.isUsed()) {
                target.use();
                dialogue.show(target.getText());
            }
        }
        return false;
    }

    /** @return true, если экран был сменён и дальше рисовать не нужно. */
    private boolean update(float delta) {
        roomNameTimer += delta;

        boolean visible = dialogue.isVisible();

        // Переходы между репликами события «сорвалась»
        if (overwhelmedPhase == 1 && dialogueWasVisible && !visible) {
            overwhelmedPhase = 2;
            dialogue.show("Он берёт тебя за руку.\n«Я рядом. Дыши.»");
            play(game.assets.sfxChime, 0.7f);
            visible = dialogue.isVisible();
        } else if (overwhelmedPhase == 2 && dialogueWasVisible && !visible) {
            overwhelmedPhase = 0;
        }

        // Плавное «расцвечивание» обратно после срыва
        float grayTarget = overwhelmedPhase > 0 ? 1f : 0f;
        grayT += (grayTarget - grayT) * Math.min(1f, delta * 2.5f);
        if (distractionLevel) {
            room.setColorAmount(savedColorAmount * (1f - grayT));
        }

        // Механика тревожной шкалы (на паузе, пока открыт диалог)
        if (distractionLevel && !visible) {
            meter.update(delta);
            if (meter.success) {
                play(game.assets.sfxChime, 0.4f);
            }
            if (meter.stumble) {
                play(game.assets.sfxThud, 0.9f);
                shakeTimer = SHAKE_TIME;
                owTimer = 1.1f;
            }
            if (meter.brokeDown) {
                startOverwhelmed();
            }
            meter.clearEvents();
        }

        if (!visible && !(distractionLevel && meter.isFrozen())) {
            player.update(delta, room);
        }
        dialogue.update(delta);

        if (owTimer > 0f) owTimer -= delta;

        // Выход из уровня: комната собрана, героиня у двери.
        if (!dialogue.isVisible()
            && overwhelmedPhase == 0
            && room.isComplete()
            && room.isPlayerAtDoor(player)) {
            if (Levels.hasBoss(levelIndex)) {
                game.setScreen(new BossScreen(game, levelIndex));
            } else {
                finishLevel();
            }
            return true;
        }

        dialogueWasVisible = dialogue.isVisible();
        return false;
    }

    private void startOverwhelmed() {
        overwhelmedPhase = 1;
        dialogue.show("Слёзы подступают. Ещё чуть-чуть — и я сорвусь.");
    }

    private void play(Sound sound, float volume) {
        if (sound != null) sound.play(volume);
    }

    private void finishLevel() {
        game.save.completeLevel(levelIndex);
        if (levelIndex >= Levels.COUNT - 1) {
            game.setScreen(new EndScreen(game));
        } else {
            game.setScreen(new MapScreen(game, levelIndex));
        }
    }

    private void draw(float delta) {
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.03f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Тряска камеры при спотыкании
        float sx = 0f;
        float sy = 0f;
        if (shakeTimer > 0f) {
            shakeTimer -= delta;
            float k = Math.max(0f, shakeTimer / SHAKE_TIME);
            sx = MathUtils.random(-1f, 1f) * SHAKE_MAG * k;
            sy = MathUtils.random(-1f, 1f) * SHAKE_MAG * k;
        }

        viewport.apply();
        camera.position.set(W / 2f + sx, H / 2f + sy, 0f);
        camera.update();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Применяем "цвет" комнаты ко всей сцене
        game.batch.setColor(room.getTint());
        room.draw(game.batch);
        player.draw(game.batch);
        game.batch.setColor(Color.WHITE);

        // Серый налёт при срыве (ч/б)
        if (grayT > 0.01f) {
            game.batch.setColor(0.32f, 0.32f, 0.35f, grayT * 0.6f);
            game.batch.draw(game.assets.pixel, 0, 0, W, H);
            game.batch.setColor(Color.WHITE);
        }

        drawRoomName();

        game.batch.end();

        // Интерфейс без тряски
        camera.position.set(W / 2f, H / 2f, 0f);
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        if (distractionLevel) drawDistractionHud(delta);
        dialogue.draw(game.batch, W, H);
        game.batch.end();
    }

    // ---------- элементы интерфейса механики ----------

    private void drawDistractionHud(float delta) {
        Assets a = game.assets;
        float cx = player.getCenterX();
        float cy = player.getCenterY();

        // Тревожная шкала сверху по центру
        float bw = 340f;
        float bh = 20f;
        float bx = (W - bw) / 2f;
        float by = H - 74f;

        game.batch.setColor(0f, 0f, 0f, 0.55f);
        game.batch.draw(a.pixel, bx - 3f, by - 3f, bw + 6f, bh + 6f);

        float ratio = meter.getCharge();
        boolean full = meter.isFull();
        float r = MathUtils.lerp(0.45f, 1f, ratio);
        float g = MathUtils.lerp(0.85f, 0.28f, ratio);
        float b = MathUtils.lerp(0.55f, 0.3f, ratio);
        float pulse = full ? 0.6f + 0.4f * (float) Math.abs(Math.sin(roomNameTimer * 9f)) : 1f;
        game.batch.setColor(r * pulse, g * pulse, b * pulse, 0.95f);
        game.batch.draw(a.pixel, bx, by, bw * ratio, bh);
        game.batch.setColor(Color.WHITE);

        // Подпись над шкалой
        a.fontSmall.setColor(full ? 1f : 0.8f, full ? 0.85f : 0.8f, 0.85f, 0.95f);
        String label = full ? "ШКАЛА ПОЛНА — ЖМИ ПРОБЕЛ!" : "Тревога";
        layout.setText(a.fontSmall, label);
        a.fontSmall.draw(game.batch, layout, (W - layout.width) / 2f, by + bh + 8f);
        a.fontSmall.setColor(Color.WHITE);

        // Три «сердечка»-ошибки
        float pipW = 16f;
        float gap = 6f;
        float px = (W - (DistractionMeter.MISTAKES_TO_BREAK * pipW
                   + (DistractionMeter.MISTAKES_TO_BREAK - 1) * gap)) / 2f;
        float pipY = by - 14f;
        int mistakes = meter.getMistakes();
        for (int i = 0; i < DistractionMeter.MISTAKES_TO_BREAK; i++) {
            if (i < mistakes) game.batch.setColor(0.95f, 0.35f, 0.3f, 0.95f);
            else              game.batch.setColor(1f, 1f, 1f, 0.18f);
            game.batch.draw(a.pixel, px + i * (pipW + gap), pipY, pipW, 5f);
        }
        game.batch.setColor(Color.WHITE);

        // Всплывающее «Ой!» над героиней
        if (owTimer > 0f) {
            float alpha = Math.min(1f, owTimer / 1.1f);
            float rise = (1.1f - owTimer) * 42f;
            a.fontMenu.setColor(1f, 0.78f, 0.5f, alpha);
            layout.setText(a.fontMenu, "Ой!");
            a.fontMenu.draw(game.batch, layout, cx - layout.width / 2f, cy + 72f + rise);
            a.fontMenu.setColor(Color.WHITE);
        }

        // Подсказка снизу
        String hint = meter.hasSupport()
            ? "Он рядом. Дышать стало легче."
            : "WASD — идти. E — взять предмет. Шкала полна — ПРОБЕЛ.";
        a.fontSmall.setColor(0.7f, 0.7f, 0.76f, 0.9f);
        layout.setText(a.fontSmall, hint);
        a.fontSmall.draw(game.batch, layout, (W - layout.width) / 2f, 26f);
        a.fontSmall.setColor(Color.WHITE);
    }

    private void drawRoomName() {
        if (room.getName() == null) return;
        float a = 1f - Math.min(1f, Math.max(0f, (roomNameTimer - 2.0f) / 1.5f));
        if (a <= 0.01f) return;

        game.assets.fontMedium.setColor(0.95f, 0.95f, 0.95f, a);
        layout.setText(game.assets.fontMedium, room.getName());
        game.assets.fontMedium.draw(game.batch, layout, (W - layout.width) / 2f, H - 30f);
        game.assets.fontMedium.setColor(Color.WHITE);
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }
}
