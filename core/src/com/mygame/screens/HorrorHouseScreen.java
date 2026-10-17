package com.mygame.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygame.MyGame;
import com.mygame.assets.Assets;
import com.mygame.entities.Player;
import com.mygame.levels.Levels;
import com.mygame.ui.DialogueBox;
import com.mygame.world.Interactable;
import com.mygame.world.Room;
import com.mygame.world.RoomFactory;

import java.util.function.IntConsumer;

/**
 * Экран внутри домика на базе отдыха уровня 5 «Школа Актива».
 *
 * Отдельная комната с кроватями, под которыми спрятаны бутылки.
 * Игрок заходит из базы отдыха через портал и может выйти обратно.
 * Бутылки найденные в домике плюсуются к общему счетчику из базы.
 */
public class HorrorHouseScreen extends ScreenAdapter {

    public static final float W = 1248f;
    public static final float H = 720f;

    private final MyGame game;
    private final int houseIndex;
    private final IntConsumer onExit; // вызывается с итоговым количеством бутылок
    private final int bottlesTotal; // общее количество бутылок на уровне (из базы)

    private OrthographicCamera camera;
    private FitViewport viewport;

    private Player player;
    private Room room;
    private DialogueBox dialogue;

    private int bottlesFoundInHouse = 0; // бутылки, найденные в ЭТОМ домике за визит
    private final int baseBottlesFound; // бутылки, найденные на БАЗЕ до входа в домик
    private boolean finished = false;
    private float roomNameTimer = 0f;
    private final GlyphLayout layout = new GlyphLayout();

    /**
     * @param game          ссылка на игру
     * @param houseIndex    номер домика (0..3)
     * @param onExit        обратный вызов при выходе из домика (передаёт итоговый счетчик бутылок)
     * @param baseBottlesFound сколько бутылок уже найдено на БАЗЕ до входа в этот домик
     * @param bottlesTotal  общее количество бутылок на всём уровне 5
     */
    public HorrorHouseScreen(MyGame game, int houseIndex, IntConsumer onExit,
                             int baseBottlesFound, int bottlesTotal) {
        this.game = game;
        this.houseIndex = houseIndex;
        this.onExit = onExit;
        this.baseBottlesFound = baseBottlesFound;
        this.bottlesTotal = bottlesTotal;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(W, H, camera);
        camera.position.set(W / 2f, H / 2f, 0f);
        camera.update();

        dialogue = new DialogueBox(game.assets);

        // Создаём комнату домика через RoomFactory
        room = RoomFactory.horrorHouse(game.assets, 5, houseIndex);

        // Спавн игрока в центре комнаты
        float startX = W / 2f - Player.WIDTH / 2f;
        float startY = H / 2f - Player.HEIGHT / 2f;
        player = new Player(startX, startY, game.assets);

        roomNameTimer = 0f;

        int totalFound = baseBottlesFound + bottlesFoundInHouse;
        dialogue.show(game.assets.girlPortrait,
                      "Домик №" + (houseIndex + 1) + ".\n" +
                      "Здесь спрятаны бутылки под кроватями.\n" +
                      "WASD — ходить, E — взять предмет.\n" +
                      "Выход — дверь внизу комнаты или ESC.");
    }

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1f / 30f);

        if (finished) {
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
            // Выход из домика обратно на базу отдыха — передаём итоговый счетчик
            onExit.accept(baseBottlesFound + bottlesFoundInHouse);
            return true;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            interact();
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
        if (room == null) return;
        float px = player.getCenterX();
        float py = player.getCenterY();

        Interactable target = room.findClosestInteractable(px, py);
        if (target != null && !target.isUsed()) {
            target.use();
            bottlesFoundInHouse++;
            int totalFound = baseBottlesFound + bottlesFoundInHouse;
            dialogue.show("Бутылка найдена! " + totalFound + " / " + bottlesTotal);
            game.assets.sfxChime.play(0.7f);
            // Все бутылки найдены — запускаем босс-пакман
            if (totalFound >= bottlesTotal) {
                finished = true;
                onExit.accept(totalFound);
                game.setScreen(new PacmanBossScreen(game));
            }
        }

        // Проверка двери выхода (внизу комнаты)
        Rectangle door = room.getDoorBounds();
        if (door != null && door.overlaps(player.getBounds())) {
            finished = true;
            onExit.accept(baseBottlesFound + bottlesFoundInHouse);
        }
    }

    private boolean update(float delta) {
        roomNameTimer += delta;

        if (!dialogue.isVisible()) {
            player.update(delta, room);
        }

        dialogue.update(delta);

        return false;
    }

    private void draw(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.08f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        camera.position.set(W / 2f, H / 2f, 0f);
        camera.update();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        if (room != null) {
            room.draw(game.batch);
        }
        player.draw(game.batch);

        game.batch.end();

        // HUD
        camera.position.set(W / 2f, H / 2f, 0f);
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        Assets a = game.assets;
        a.fontMedium.setColor(1f, 0.9f, 0.6f, 0.95f);
        int totalFound = baseBottlesFound + bottlesFoundInHouse;
        layout.setText(a.fontMedium, "Бутылки: " + totalFound + " / " + bottlesTotal);
        a.fontMedium.draw(game.batch, layout, 36f, H - 30f);

        a.fontSmall.setColor(0.6f, 0.6f, 0.65f, 0.8f);
        layout.setText(a.fontSmall, "WASD — ходить, E — взять, ESC — выйти из домика");
        a.fontSmall.draw(game.batch, layout, (W - layout.width) / 2f, 26f);

        dialogue.draw(game.batch, W, H);
        game.batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }
}
