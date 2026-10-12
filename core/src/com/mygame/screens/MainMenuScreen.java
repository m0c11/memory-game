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

public class MainMenuScreen extends ScreenAdapter {

    public static final float W = 1248f;
    public static final float H = 720f;

    private final MyGame game;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private final GlyphLayout layout = new GlyphLayout();
    private float time = 0f;

    public MainMenuScreen(MyGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(W, H, camera);
        camera.position.set(W / 2f, H / 2f, 0f);
        camera.update();
    }

    @Override
    public void render(float delta) {
        time += delta;

        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.setScreen(new GameScreen(game));
            dispose();
            return;
        }

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Пульсация заголовка
        float pulse = 0.85f + 0.15f * (float) Math.sin(time * 2.0);

        game.assets.fontLarge.setColor(1f, 0.84f, 0f, pulse);
        layout.setText(game.assets.fontLarge, "ГОД 19 → 20");
        game.assets.fontLarge.draw(game.batch, layout, (W - layout.width) / 2f, H * 0.72f);

        game.assets.fontMedium.setColor(0.85f, 0.85f, 0.85f, 1f);
        layout.setText(game.assets.fontMedium, "маленькое приключение в воспоминаниях");
        game.assets.fontMedium.draw(game.batch, layout, (W - layout.width) / 2f, H * 0.62f);

        float blink = 0.4f + 0.6f * (float) Math.abs(Math.sin(time * 2.5));
        game.assets.fontMedium.setColor(1f, 1f, 1f, blink);
        layout.setText(game.assets.fontMedium, "[ ПРОБЕЛ ] начать");
        game.assets.fontMedium.draw(game.batch, layout, (W - layout.width) / 2f, H * 0.34f);

        game.assets.fontSmall.setColor(0.5f, 0.5f, 0.5f, 1f);
        layout.setText(game.assets.fontSmall,
                "WASD — ходить    E — взаимодействовать    ESC — выйти в меню");
        game.assets.fontSmall.draw(game.batch, layout, (W - layout.width) / 2f, 40f);

        // сброс цветов
        game.assets.fontLarge.setColor(Color.WHITE);
        game.assets.fontMedium.setColor(Color.WHITE);
        game.assets.fontSmall.setColor(Color.WHITE);

        game.batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }
}