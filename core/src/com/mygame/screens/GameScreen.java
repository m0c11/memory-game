package com.mygame.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygame.MyGame;
import com.mygame.assets.Assets;
import com.mygame.effects.ParticleSystem;
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

    /** Насколько цветным уровень начинается (воспоминания ещё не собраны). */
    private static final float COLOR_START = 0.12f;

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

    // Эффекты
    private ParticleSystem particles;
    private float dustTimer = 0f;
    private final java.util.ArrayList<FloatText> floatTexts = new java.util.ArrayList<>();

    // Подсветка интерактива рядом с игроком
    private float interactHighlightAlpha = 0f;

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
        particles = new ParticleSystem(game.assets.orb);

        if (!rooms.isEmpty()) {
            savedColorAmount = rooms.get(0).getColorAmount();
        }
        if (distractionLevel) {
            // В этой главе цвет возвращается по мере собранных воспоминаний
            savedColorAmount = COLOR_START;
        }

        if (distractionLevel) {
            meter = new DistractionMeter();
            player.setSpeedScale(DISTRACTION_SPEED);
            dialogue.show(game.assets.girlPortrait,
                          "Актовый зал перед Студвесной.\n" +
                          "WASD — идти, Z — взять предмет (или сохранить у звёздочки).\n" +
                          "Поднимись на сцену по лестнице, загляни в левую и правую кулисы.\n" +
                          "Собери все воспоминания — тогда выйдешь на сцену.\n" +
                          "Шкала тревоги: как заполнится — жми ПРОБЕЛ.");
        } else if (levelIndex == 0) {
            // Приветствие только в самом первом уровне
            dialogue.show(game.assets.girlPortrait,
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
            // Искры при сборе предмета
            if (particles != null) {
                particles.burst(target.getCenterX(), target.getCenterY(),
                              new Color(1f, 0.9f, 0.5f, 1f), 12);
            }
            // Плавающий текст
            if (particles != null) {
                addFloatText(target.getCenterX(), target.getCenterY() + 16f,
                    "Воспоминание найдено");
            }
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

        // Каждое найденное воспоминание добавляет цвета всему уровню
        if (distractionLevel) {
            float target = collectedColor();
            savedColorAmount += (target - savedColorAmount) * Math.min(1f, delta * 2f);
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

        // Обновление частиц
        if (particles != null) {
            particles.update(delta);
            // Пыль в воздухе
            dustTimer -= delta;
            if (dustTimer <= 0f) {
                dustTimer = 0.15f;
                float px = player.getCenterX() + MathUtils.random(-200f, 200f);
                float py = player.getCenterY() + MathUtils.random(-150f, 150f);
                particles.dust(px, py);
            }
        }

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

        // Обновление анимации дверей во всех локациях
        if (rooms != null) {
            for (Room r : rooms) {
                r.updateDoor(delta);
                r.updateLights(delta);
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
            // Запускаем анимацию открытия двери перед переходом
            if (curRoom != null) {
                curRoom.setDoorOpenTarget(1f);
                curRoom.updateDoor(delta);
            }
            if (Levels.hasBoss(levelIndex)) {
                game.setScreen(new BossScreen(game, levelIndex));
            } else {
                finishLevel();
            }
            return true;
        }

        // Обновление плавающих текстов
        updateFloatTexts(delta);

        // Подсветка ближайшего интерактива
        updateInteractableHighlight(delta);

        dialogueWasVisible = dialogue.isVisible();
        return false;
    }

    /** Цвет уровня: от COLOR_START до 1 по доле использованных предметов во всех локациях. */
    private float collectedColor() {
        int used = 0;
        int total = 0;
        if (rooms != null) {
            for (Room r : rooms) {
                used += r.getUsedCount();
                total += r.getTotalCount();
            }
        }
        float progress = total > 0 ? used / (float) total : 0f;
        return COLOR_START + (1f - COLOR_START) * progress;
    }

    private void startOverwhelmed() {
        overwhelmedPhase = 1;
        dialogue.show(game.assets.girlPortrait,
                      "Слёзы подступают. Ещё чуть-чуть — и я сорвусь.");
    }

    private void play(Sound sound, float volume) {
        if (sound != null) sound.play(volume);
    }

    private void finishLevel() {
        // Конфетти при завершении
        if (particles != null) {
            particles.confetti(player.getCenterX(), player.getCenterY());
        }
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

        drawPlayerShadow();
        player.draw(game.batch);
        game.batch.setColor(Color.WHITE);

        // Частицы
        if (particles != null) {
            particles.draw(game.batch);
        }

        // Плавающие тексты (над игрой, под UI)
        drawFloatTexts(game.batch);

        // Серый налёт при срыве (ч/б)
        if (grayT > 0.01f) {
            game.batch.setColor(0.32f, 0.32f, 0.35f, grayT * 0.6f);
            game.batch.draw(game.assets.pixel, 0, 0, W, H);
            game.batch.setColor(Color.WHITE);
        }

        // Атмосфера в духе Undertale: прожектор и темнота по краям
        drawAtmosphere();

        // Концертный свет (над сценой, аддитивно) — рисуем ПОСЛЕ атмосферы, чтобы не затемнялось
        drawLights();

        // Подсветка ближайшего интерактива, если игрок рядом — тоже после атмосферы
        drawInteractableHighlight();

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

    /** Мягкая тень под героиней — добавляет глубину. */
    private void drawPlayerShadow() {
        float cx = player.getCenterX();
        float cy = player.getCenterY() - Player.HEIGHT / 2f + 2f;
        float shadowW = 26f;
        float shadowH = 8f;

        // Рисуем тень как тёмный эллипс (два слоя для мягкости)
        game.batch.setColor(0f, 0f, 0f, 0.25f);
        game.batch.draw(game.assets.pixel, cx - shadowW / 2f, cy - shadowH / 2f, shadowW, shadowH);
        game.batch.setColor(0f, 0f, 0f, 0.15f);
        game.batch.draw(game.assets.pixel, cx - shadowW / 2f - 2f, cy - shadowH / 2f - 1f, shadowW + 4f, shadowH + 2f);
        game.batch.setColor(Color.WHITE);
    }

    /** Стробоскопный свет над сценой — круги мигают и скачут по сцене. */
    private void drawLights() {
        Room room = currentRoom();
        if (room == null) return;
        for (Room.LightCue c : room.getLightCues()) {
            // Круг виден только в течение flashDuration после каждого интервала
            float flashPhase = (c.flashTimer % c.flashInterval) / c.flashInterval;
            if (flashPhase > c.flashDuration / c.flashInterval) continue;

            float alpha = 0.5f + 0.4f * (1f - flashPhase / (c.flashDuration / c.flashInterval));
            game.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
            game.batch.setColor(c.color.r, c.color.g, c.color.b, alpha);
            game.batch.draw(game.assets.light, c.x - c.w / 2f, c.y - c.h / 2f, c.w, c.h);
            game.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
            game.batch.setColor(Color.WHITE);
        }
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

    // ---------- Плавающий текст при сборе предметов ----------

    private static class FloatText {
        float x, y;
        String text;
        float life = 1.2f;
        float vy = -40f;
        float alpha = 1f;

        FloatText(float x, float y, String text) {
            this.x = x;
            this.y = y;
            this.text = text;
        }

        void update(float delta) {
            life -= delta;
            y += vy * delta;
            vy += 20f * delta; // легкое ускорение вверх
            alpha = Math.max(0f, life / 1.2f);
        }

        boolean isDead() {
            return life <= 0f;
        }

        void draw(SpriteBatch batch, BitmapFont font) {
            if (life <= 0f) return;
            font.setColor(1f, 0.9f, 0.5f, alpha);
            GlyphLayout gl = new GlyphLayout();
            gl.setText(font, text);
            font.draw(batch, text, x - gl.width / 2f, y);
            font.setColor(Color.WHITE);
        }
    }

    private void addFloatText(float x, float y, String text) {
        floatTexts.add(new FloatText(x, y, text));
    }

    private void updateFloatTexts(float delta) {
        java.util.Iterator<FloatText> it = floatTexts.iterator();
        while (it.hasNext()) {
            FloatText ft = it.next();
            ft.update(delta);
            if (ft.isDead()) it.remove();
        }
    }

    private void drawFloatTexts(SpriteBatch batch) {
        for (FloatText ft : floatTexts) {
            ft.draw(batch, game.assets.fontMedium);
        }
    }

    /** Рисует мягкое свечение вокруг ближайшего интерактива, если игрок рядом. */
    private void drawInteractableHighlight() {
        Room room = currentRoom();
        if (room == null) return;
        float px = player.getCenterX();
        float py = player.getCenterY();

        Interactable target = room.findClosestInteractable(px, py);
        if (target == null || target.isUsed()) return;

        float d = distance(px, py, target.getCenterX(), target.getCenterY());
        if (d > Interactable.INTERACT_RADIUS + 20f) return;

        // Яркость свечения зависит от расстояния
        float glowAlpha = Math.max(0f, 1f - d / (Interactable.INTERACT_RADIUS + 20f));
        float pulse = 0.6f + 0.4f * (float) Math.abs(Math.sin(roomNameTimer * 5f));
        glowAlpha *= pulse;

        float size = 44f + 8f * (float) Math.sin(roomNameTimer * 4f);
        game.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        game.batch.setColor(0.5f, 0.9f, 1f, glowAlpha * 0.6f);
        game.batch.draw(game.assets.orb, target.getCenterX() - size / 2f,
                        target.getCenterY() - size / 2f, size, size);
        game.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        game.batch.setColor(Color.WHITE);
    }

    /** Обновляет значение подсветки интерактива (для будущих эффектов звука). */
    private void updateInteractableHighlight(float delta) {
        Room room = currentRoom();
        if (room == null) return;
        float px = player.getCenterX();
        float py = player.getCenterY();

        Interactable target = room.findClosestInteractable(px, py);
        float targetAlpha = 0f;
        if (target != null && !target.isUsed()) {
            float d = distance(px, py, target.getCenterX(), target.getCenterY());
            if (d < Interactable.INTERACT_RADIUS) {
                targetAlpha = 0.5f;
            }
        }
        interactHighlightAlpha += (targetAlpha - interactHighlightAlpha) * Math.min(1f, delta * 5f);
    }
}
