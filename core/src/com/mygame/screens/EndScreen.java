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

public class EndScreen extends ScreenAdapter {

    private static final float W = 1248f;
    private static final float H = 720f;

    private final MyGame game;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private final GlyphLayout layout = new GlyphLayout();
    private float time = 0f;

    public EndScreen(MyGame game) {
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
                || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MainMenuScreen(game));
            dispose();
            return;
        }

        Gdx.gl.glClearColor(0.05f, 0.05f, 0.07f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        float a = Math.min(1f, time / 2f);
        game.assets.fontLarge.setColor(1f, 0.84f, 0f, a);
        layout.setText(game.assets.fontLarge, "Ты вспомнила всё.");
        game.assets.fontLarge.draw(game.batch, layout, (W - layout.width) / 2f, H * 0.62f);

        game.assets.fontMedium.setColor(0.9f, 0.9f, 0.9f, a);
        layout.setText(game.assets.fontMedium, "Год 19 → 20 — восстановлен.");
        game.assets.fontMedium.draw(game.batch, layout, (W - layout.width) / 2f, H * 0.50f);

        float blink = 0.4f + 0.6f * (float) Math.abs(Math.sin(time * 2.5));
        game.assets.fontMedium.setColor(1f, 1f, 1f, blink);
        layout.setText(game.assets.fontMedium, "[ ПРОБЕЛ ] в меню");
        game.assets.fontMedium.draw(game.batch, layout, (W - layout.width) / 2f, H * 0.22f);

        game.assets.fontLarge.setColor(Color.WHITE);
        game.assets.fontMedium.setColor(Color.WHITE);

        game.batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }
}