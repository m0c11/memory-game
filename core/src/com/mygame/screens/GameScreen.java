package com.mygame.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygame.MyGame;
import com.mygame.entities.Player;
import com.mygame.levels.Levels;
import com.mygame.ui.DialogueBox;
import com.mygame.world.Interactable;
import com.mygame.world.Room;
import com.mygame.world.RoomFactory;

/**
 * Игровой экран одного уровня: комната, игрок, диалоги.
 * Когда все предметы собраны и игрок вошёл в дверь, уровень засчитывается
 * и игра возвращается на карту (после последнего уровня — на финальный экран).
 */
public class GameScreen extends ScreenAdapter {

    public static final float W = 1248f;
    public static final float H = 720f;

    private final MyGame game;
    private final int levelIndex;

    private OrthographicCamera camera;
    private FitViewport viewport;

    private Player player;
    private Room room;
    private DialogueBox dialogue;

    // Для плавного появления названия комнаты сверху
    private float roomNameTimer = 0f;
    private final GlyphLayout layout = new GlyphLayout();

    public GameScreen(MyGame game, int levelIndex) {
        this.game = game;
        this.levelIndex = levelIndex;
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

        // Приветствие только в самом первом уровне
        if (levelIndex == 0) {
            dialogue.show("Ты просыпаешься в мире, где нет ни одного цвета.\n" +
                          "Что-то важное потерялось... попробуй вспомнить.");
        }
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1f / 30f);

        if (handleInput()) return;
        if (update(delta)) return;
        draw();
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

        if (!dialogue.isVisible()) {
            player.update(delta, room);
        }
        dialogue.update(delta);

        // Выход из уровня только если комната "собрана"
        if (!dialogue.isVisible()
            && room.isComplete()
            && room.isPlayerAtDoor(player)) {
            finishLevel();
            return true;
        }
        return false;
    }

    private void finishLevel() {
        game.save.completeLevel(levelIndex);
        if (levelIndex >= Levels.COUNT - 1) {
            game.setScreen(new EndScreen(game));
        } else {
            game.setScreen(new MapScreen(game, levelIndex));
        }
    }

    private void draw() {
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.03f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        camera.update();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Применяем "цвет" комнаты ко всей сцене
        game.batch.setColor(room.getTint());
        room.draw(game.batch);
        player.draw(game.batch);
        game.batch.setColor(Color.WHITE);

        // Название комнаты сверху (появляется и гаснет)
        drawRoomName();

        game.batch.end();

        // Диалог рисуем поверх без тонирования
        game.batch.begin();
        dialogue.draw(game.batch, W, H);
        game.batch.end();
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
