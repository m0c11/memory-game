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
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygame.MyGame;
import com.mygame.assets.Assets;
import com.mygame.entities.Player;
import com.mygame.levels.Levels;
import com.mygame.mechanics.DistractionMeter;
import com.mygame.ui.DialogueBox;
import com.mygame.world.Interactable;
import com.mygame.world.Portal;
import com.mygame.world.Room;
import com.mygame.world.RoomFactory;
import com.mygame.world.SavePoint;

import java.util.List;

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
    private List<Room> rooms;
    private int roomIndex;
    private DialogueBox dialogue;

    private final boolean distractionLevel;
    private DistractionMeter meter;
    private float portalCooldown = 0f;

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
        rooms = RoomFactory.createRooms(levelIndex, game.assets);
        roomIndex = 0;
        float startX = 200f;
        float startY = H / 2f - Player.HEIGHT / 2f;
        if (levelIndex == 1 && !rooms.isEmpty()) {
            // В актовом зале начинаем в зрительном зале снизу.
            startX = 576f;
            startY = 60f;
        }
        player = new Player(startX, startY, game.assets);
        roomNameTimer = 0f;

        if (!rooms.isEmpty()) {
            savedColorAmount = rooms.get(0).getColorAmount();
        }

        if (distractionLevel) {
            meter = new DistractionMeter();
            player.setSpeedScale(DISTRACTION_SPEED);
            dialogue.show(game.assets.yarnBall,
                          "Актовый зал перед Студвесной.\n" +
                          "WASD — идти, Z — взять предмет (или сохранить у звёздочки).\n" +
                          "Поднимись на сцену по лестнице, загляни в левую и правую кулисы.\n" +
                          "Собери все воспоминания — тогда выйдешь на сцену.\n" +
                          "Шкала тревоги: как заполнится — жми ПРОБЕЛ.");
        } else if (levelIndex == 0) {
            // Приветствие только в самом первом уровне
            dialogue.show(game.assets.yarnBall,
                          "Ты просыпаешься в мире, где нет ни одного цвета.\n" +
                          "Что-то важное потерялось... попробуй вспомнить.\n" +
                          "Собери предметы (Z) и найди звёздочку сохранения.");
        }
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1f / 30f);

        if (handleInput()) return;
        if (update(delta)) return;
        draw(delta);
    }

    /** Подтверждение в духе Undertale: Z (а также E / Enter / Пробел). */
    private boolean confirmPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.Z)
            || Gdx.input.isKeyJustPressed(Input.Keys.E)
            || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
            || Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
    }

    /** Взаимодействие с объектами и звёздочкой. */
    private boolean interactPressed() {
        return Gdx.input.isKeyJustPressed(Input.Keys.Z)
            || Gdx.input.isKeyJustPressed(Input.Keys.E);
    }

    /** @return true, если экран был сменён и дальше рисовать не нужно. */
    private boolean handleInput() {
        if (dialogue.isVisible()) {
            if (confirmPressed()) dialogue.advance();
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

        if (interactPressed()) {
            interact();
        }
        return false;
    }

    /** Выбирает ближайшую цель: звёздочка сохранения или предмет. */
    private Room currentRoom() {
        if (rooms == null || rooms.isEmpty()) return null;
        if (roomIndex < 0) roomIndex = 0;
        if (roomIndex >= rooms.size()) roomIndex = rooms.size() - 1;
        return rooms.get(roomIndex);
    }

    private void switchRoom(Portal p) {
        if (rooms == null || p == null) return;
        if (p.targetRoom < 0 || p.targetRoom >= rooms.size()) return;
        roomIndex = p.targetRoom;
        player.setPosition(p.spawnX, p.spawnY);
        portalCooldown = 0.45f;
        roomNameTimer = 0f;
        if (distractionLevel) {
            Room cur = currentRoom();
            if (cur != null && savedColorAmount >= 0f) {
                cur.setColorAmount(savedColorAmount * (1f - grayT));
            }
        }
        play(game.assets.sfxStep, 0.5f);
    }

    private void interact() {
        Room room = currentRoom();
        if (room == null) return;
        float px = player.getCenterX();
        float py = player.getCenterY();

        SavePoint sp = room.getSavePoint();
        float dSave = sp != null ? distance(px, py, sp.getCenterX(), sp.getCenterY()) : Float.MAX_VALUE;

        Interactable target = room.findClosestInteractable(px, py);
        float dInteract = target != null
            ? distance(px, py, target.getCenterX(), target.getCenterY())
            : Float.MAX_VALUE;

        if (sp != null && dSave <= dInteract && dSave <= SavePoint.INTERACT_RADIUS) {
            game.save.saveCheckpoint(levelIndex);
            dialogue.show("Звёздочка теплится в ладонях.\nСохранено. Оставайся решительной.");
            play(game.assets.sfxChime, 0.7f);
        } else if (target != null && !target.isUsed()) {
            target.use();
            dialogue.show(target.getText());
        }
    }

    private float distance(float x1, float y1, float x2, float y2) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        return (float) Math.sqrt(dx * dx + dy * dy);
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
            Room roomCur = currentRoom();
            if (roomCur != null) {
                roomCur.setColorAmount(savedColorAmount * (1f - grayT));
            }
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

        Room curRoom = currentRoom();
        if (!visible && !(distractionLevel && meter.isFrozen())) {
            if (curRoom != null) {
                player.update(delta, curRoom);
            }
        }
        dialogue.update(delta);

        if (owTimer > 0f) owTimer -= delta;

        // Переключение между локациями уровня по порталам
        if (!dialogue.isVisible() && portalCooldown > 0f) {
            portalCooldown -= delta;
        }
        if (!dialogue.isVisible() && portalCooldown <= 0f && curRoom != null) {
            Portal portal = curRoom.portalAt(player.getBounds());
            if (portal != null) {
                switchRoom(portal);
                return false;
            }
        }

        // Проверка, собраны ли все воспоминания во всех локациях
        boolean allComplete = true;
        if (rooms != null) {
            for (Room r : rooms) {
                if (!r.isComplete()) {
                    allComplete = false;
                    break;
                }
            }
        } else if (curRoom != null) {
            allComplete = curRoom.isComplete();
        }

        // Выход из уровня
        boolean atExit = false;
        if (curRoom != null) {
            Rectangle fin = curRoom.getFinishBounds();
            if (fin != null) {
                atExit = fin.overlaps(player.getBounds());
            } else {
                atExit = curRoom.isPlayerAtDoor(player);
            }
        }

        if (!dialogue.isVisible()
            && overwhelmedPhase == 0
            && allComplete
            && atExit) {
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
        dialogue.show(game.assets.yarnBall,
                      "Слёзы подступают. Ещё чуть-чуть — и я сорвусь.");
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
        Room drawRoom = currentRoom();
        if (drawRoom != null) {
            game.batch.setColor(drawRoom.getTint());
            drawRoom.draw(game.batch);
        }
        player.draw(game.batch);
        game.batch.setColor(Color.WHITE);

        // Серый налёт при срыве (ч/б)
        if (grayT > 0.01f) {
            game.batch.setColor(0.32f, 0.32f, 0.35f, grayT * 0.6f);
            game.batch.draw(game.assets.pixel, 0, 0, W, H);
            game.batch.setColor(Color.WHITE);
        }

        // Атмосфера в духе Undertale: прожектор и темнота по краям
        drawAtmosphere();

        drawRoomName();

        game.batch.end();

        // Интерфейс без тряски
        camera.position.set(W / 2f, H / 2f, 0f);
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        if (distractionLevel) drawDistractionHud(delta);
        drawSavePrompt();
        drawPixelFrame();
        dialogue.draw(game.batch, W, H);
        game.batch.end();
    }

    /**
     * Мягкий прожектор вокруг героини: маска света растянута на весь экран,
     * центр прозрачный, края уходят в темноту.
     */
    private void drawAtmosphere() {
        Assets a = game.assets;
        float cx = player.getCenterX();
        float cy = player.getCenterY();

        // Радиус подбираем так, чтобы дальний угол экрана уходил в темноту,
        // а вокруг героини оставалось светлое пятно.
        float far = 0f;
        far = Math.max(far, distance(cx, cy, 0f, 0f));
        far = Math.max(far, distance(cx, cy, W, 0f));
        far = Math.max(far, distance(cx, cy, 0f, H));
        far = Math.max(far, distance(cx, cy, W, H));
        float r = far * 1.12f;

        game.batch.setColor(Color.WHITE);
        game.batch.draw(a.light, cx - r, cy - r, r * 2f, r * 2f);
    }

    /** Тёмная пиксельная рамка-виньетка по краям экрана. */
    private void drawPixelFrame() {
        Assets a = game.assets;
        float t = 12f;
        game.batch.setColor(0.02f, 0.02f, 0.04f, 0.92f);
        game.batch.draw(a.pixel, 0f, 0f, W, t);
        game.batch.draw(a.pixel, 0f, H - t, W, t);
        game.batch.draw(a.pixel, 0f, 0f, t, H);
        game.batch.draw(a.pixel, W - t, 0f, t, H);

        game.batch.setColor(0.4f, 0.4f, 0.48f, 0.35f);
        float l = 1.5f;
        game.batch.draw(a.pixel, t, t, W - 2f * t, l);
        game.batch.draw(a.pixel, t, H - t - l, W - 2f * t, l);
        game.batch.draw(a.pixel, t, t, l, H - 2f * t);
        game.batch.draw(a.pixel, W - t - l, t, l, H - 2f * t);
        game.batch.setColor(Color.WHITE);
    }

    /** Подсказка «[Z] сохранить», когда героиня стоит у звёздочки. */
    private void drawSavePrompt() {
        Room room = currentRoom();
        SavePoint sp = room != null ? room.getSavePoint() : null;
        if (sp == null) return;
        float d = distance(player.getCenterX(), player.getCenterY(), sp.getCenterX(), sp.getCenterY());
        if (d > SavePoint.INTERACT_RADIUS) return;

        Assets a = game.assets;
        float blink = 0.55f + 0.45f * (float) Math.abs(Math.sin(roomNameTimer * 3f));
        a.fontMedium.setColor(1f, 0.9f, 0.55f, blink);
        layout.setText(a.fontMedium, "[ Z ]  сохранить");
        a.fontMedium.draw(game.batch, layout, (W - layout.width) / 2f, 120f);
        a.fontMedium.setColor(Color.WHITE);
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
            : "WASD — идти. Z — взять предмет. Шкала полна — ПРОБЕЛ.";
        a.fontSmall.setColor(0.7f, 0.7f, 0.76f, 0.9f);
        layout.setText(a.fontSmall, hint);
        a.fontSmall.draw(game.batch, layout, (W - layout.width) / 2f, 26f);
        a.fontSmall.setColor(Color.WHITE);
    }

    private void drawRoomName() {
        Room room = currentRoom();
        if (room == null || room.getName() == null) return;
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
