package com.mygame.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygame.MyGame;
import com.mygame.assets.Assets;

import java.util.ArrayList;
import java.util.List;

/**
 * Главное меню.
 *  - «Продолжить» показывается только если есть сохранение и ведёт на карту;
 *  - «Новая игра» запускает вступительную сценку и карту; если сохранение уже есть,
 *    сначала спрашивает подтверждение, чтобы случайно не стереть прогресс;
 *  - «Выйти» закрывает игру.
 * Управление: стрелки вверх/вниз или W/S, Enter/Пробел, либо мышь.
 */
public class MainMenuScreen extends ScreenAdapter {

    public static final float W = 1248f;
    public static final float H = 720f;

    private enum Action { CONTINUE, NEW_GAME, EXIT, CONFIRM_NEW, CANCEL }

    private final MyGame game;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private final GlyphLayout layout = new GlyphLayout();
    private final Vector2 mouse = new Vector2();
    private float lastMouseX = -1f;
    private float lastMouseY = -1f;

    private final List<Action> actions = new ArrayList<>();
    private final List<String> labels = new ArrayList<>();
    private final List<Rectangle> bounds = new ArrayList<>();
    private int selected = 0;
    private boolean confirming = false;

    private float time = 0f;
    private float fade = 0f;

    public MainMenuScreen(MyGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(W, H, camera);
        camera.position.set(W / 2f, H / 2f, 0f);
        camera.update();
        confirming = false;
        buildItems();
    }

    private void addItem(Action action, String label) {
        actions.add(action);
        labels.add(label);
    }

    /** Собирает список пунктов под текущее состояние (есть ли сохранение, спрашиваем ли подтверждение). */
    private void buildItems() {
        actions.clear();
        labels.clear();

        if (confirming) {
            addItem(Action.CONFIRM_NEW, "Да, начать заново");
            addItem(Action.CANCEL, "Отмена");
            selected = 1; // по умолчанию безопасный вариант
        } else {
            if (game.save.hasSave()) {
                addItem(Action.CONTINUE, "Продолжить");
            }
            addItem(Action.NEW_GAME, "Новая игра");
            addItem(Action.EXIT, "Выйти");
            selected = 0;
        }
        computeBounds();
    }

    private void computeBounds() {
        bounds.clear();
        BitmapFont font = game.assets.fontMenu;
        float top = H * 0.46f;
        float gap = 66f;
        for (int i = 0; i < labels.size(); i++) {
            layout.setText(font, labels.get(i));
            float x = (W - layout.width) / 2f;
            float textTop = top - i * gap;
            bounds.add(new Rectangle(x - 40f, textTop - layout.height - 16f,
                                     layout.width + 80f, layout.height + 32f));
        }
    }

    // ------------------------------------------------------------------

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1f / 30f);
        time += delta;
        fade = Math.min(1f, fade + delta * 1.6f);

        handleInput();
        draw();
    }

    private void handleInput() {
        mouse.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mouse);

        int n = actions.size();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) && confirming) {
            confirming = false;
            buildItems();
            return;
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.UP) || Gdx.input.isKeyJustPressed(Input.Keys.W)) {
            selected = (selected - 1 + n) % n;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN) || Gdx.input.isKeyJustPressed(Input.Keys.S)) {
            selected = (selected + 1) % n;
        }

        boolean moved = mouse.x != lastMouseX || mouse.y != lastMouseY;
        lastMouseX = mouse.x;
        lastMouseY = mouse.y;

        int hover = -1;
        for (int i = 0; i < n; i++) {
            if (bounds.get(i).contains(mouse)) hover = i;
        }
        if (moved && hover >= 0) {
            selected = hover;
        }

        boolean activate = Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                        || Gdx.input.isKeyJustPressed(Input.Keys.SPACE);
        if (Gdx.input.justTouched() && hover >= 0) {
            selected = hover;
            activate = true;
        }

        if (activate) {
            run(actions.get(selected));
        }
    }

    private void run(Action action) {
        switch (action) {
            case CONTINUE:
                game.setScreen(new MapScreen(game));
                break;
            case NEW_GAME:
                if (game.save.hasSave()) {
                    confirming = true;
                    buildItems();
                } else {
                    startNewGame();
                }
                break;
            case CONFIRM_NEW:
                startNewGame();
                break;
            case CANCEL:
                confirming = false;
                buildItems();
                break;
            case EXIT:
                Gdx.app.exit();
                break;
        }
    }

    private void startNewGame() {
        game.save.newGame();
        game.setScreen(new IntroScreen(game));
    }

    // ------------------------------------------------------------------

    private void draw() {
        Assets a = game.assets;

        Gdx.gl.glClearColor(0.03f, 0.03f, 0.04f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Фон
        game.batch.setColor(0.8f, 0.8f, 0.85f, 1f);
        game.batch.draw(a.mapBackground, 0, 0, W, H);

        // Нить еле заметно светится позади меню, шар у выбранного пункта
        game.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        game.batch.setColor(1f, 1f, 1f, 0.26f + 0.05f * (float) Math.sin(time * 1.2f));
        game.batch.draw(a.mapThread, 0, 0, W, H);

        Rectangle sel = bounds.get(selected);
        float orbSize = 92f + 8f * (float) Math.sin(time * 3f);
        float orbX = sel.x + 40f - 40f;
        float orbY = sel.y + sel.height / 2f;
        game.batch.setColor(1f, 1f, 1f, 0.95f);
        game.batch.draw(a.orb, orbX - orbSize / 2f, orbY - orbSize / 2f, orbSize, orbSize);

        game.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        game.batch.setColor(Color.WHITE);

        // Заголовок
        float pulse = 0.85f + 0.15f * (float) Math.sin(time * 2f);
        a.fontLarge.setColor(1f, 0.84f, 0f, pulse);
        layout.setText(a.fontLarge, "ГОД 19 → 20");
        a.fontLarge.draw(game.batch, layout, (W - layout.width) / 2f, H * 0.80f);

        a.fontMedium.setColor(0.85f, 0.85f, 0.88f, 1f);
        layout.setText(a.fontMedium, "маленькое приключение в воспоминаниях");
        a.fontMedium.draw(game.batch, layout, (W - layout.width) / 2f, H * 0.71f);

        if (confirming) {
            a.fontMedium.setColor(0.95f, 0.62f, 0.5f, 1f);
            layout.setText(a.fontMedium, "Сохранённый прогресс будет стёрт. Начать заново?");
            a.fontMedium.draw(game.batch, layout, (W - layout.width) / 2f, H * 0.58f);
        }

        // Пункты меню
        BitmapFont font = a.fontMenu;
        for (int i = 0; i < actions.size(); i++) {
            Rectangle r = bounds.get(i);
            if (i == selected) {
                font.setColor(1f, 0.84f, 0.3f, 1f);
            } else {
                font.setColor(0.72f, 0.72f, 0.78f, 1f);
            }
            layout.setText(font, labels.get(i));
            font.draw(game.batch, layout, (W - layout.width) / 2f, r.y + r.height - 16f);
        }

        // Подсказка внизу
        a.fontSmall.setColor(0.5f, 0.5f, 0.55f, 1f);
        layout.setText(a.fontSmall, "↑ ↓  выбрать      Enter  подтвердить");
        a.fontSmall.draw(game.batch, layout, (W - layout.width) / 2f, 40f);

        a.fontLarge.setColor(Color.WHITE);
        a.fontMedium.setColor(Color.WHITE);
        a.fontMenu.setColor(Color.WHITE);
        a.fontSmall.setColor(Color.WHITE);

        if (fade < 1f) {
            game.batch.setColor(0f, 0f, 0f, 1f - fade);
            game.batch.draw(a.pixel, 0, 0, W, H);
            game.batch.setColor(Color.WHITE);
        }

        game.batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }
}
