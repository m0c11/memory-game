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
import com.mygame.ui.DialogueBox;
import com.mygame.world.Interactable;
import com.mygame.world.Room;
import com.mygame.world.RoomFactory;

/**
 * Главный игровой экран: комната, игрок, диалоги, переходы между комнатами.
 */
public class GameScreen extends ScreenAdapter {

    public static final float W = 1248f;
    public static final float H = 720f;

    private final MyGame game;
    private OrthographicCamera camera;
    private FitViewport viewport;

    private Player player;
    private Room room;
    private DialogueBox dialogue;
    private int roomIndex = 0;

    // Для плавного появления названия комнаты сверху
    private float roomNameTimer = 0f;
    private final GlyphLayout layout = new GlyphLayout();

    public GameScreen(MyGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(W, H, camera);
        camera.position.set(W / 2f, H / 2f, 0f);
        camera.update();

        dialogue = new DialogueBox(game.assets);
        loadRoom(0);

        // Приветственный диалог
        dialogue.show("Ты просыпаешься в мире, где нет ни одного цвета.\n" +
                "Что-то важное потерялось... попробуй вспомнить.");
    }

    private void loadRoom(int index) {
        roomIndex = index;
        room = RoomFactory.createRoom(index, game.assets);
        player = new Player(200, H / 2f - Player.HEIGHT / 2f, game.assets);
        roomNameTimer = 0f;
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1f / 30f);

        handleInput();
        update(delta);
        draw();
    }

    private void handleInput() {
        if (dialogue.isVisible()) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.E)
                    || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                    || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
                dialogue.hide();
            }
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MainMenuScreen(game));
            dispose();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            Interactable target = room.findClosestInteractable(player.getCenterX(), player.getCenterY());
            if (target != null && !target.isUsed()) {
                target.use();
                dialogue.show(target.getText());
            }
        }
    }

    private void update(float delta) {
        roomNameTimer += delta;

        if (!dialogue.isVisible()) {
            player.update(delta, room);
        }
        dialogue.update(delta);

        // Переход только если комната "собрана"
        if (!dialogue.isVisible()
                && room.isComplete()
                && room.isPlayerAtDoor(player)) {
            nextRoom();
        }
    }

    private void nextRoom() {
        int next = roomIndex + 1;
        if (next >= RoomFactory.ROOM_COUNT) {
            game.setScreen(new EndScreen(game));
            dispose();
            return;
        }
        loadRoom(next);
    }

    private void draw() {
        Gdx.gl.glClearColor(0.02f, 0.02f, 0.03f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        camera.update();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Применяем "цвет" комнаты ко всей сцене
        Color tint = room.getTint();
        game.batch.setColor(tint);
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