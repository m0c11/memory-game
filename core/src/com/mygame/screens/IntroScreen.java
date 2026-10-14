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
import com.mygame.screens.MapScreen;

/**
 * ЗАГЛУШКА вступительной сценки: несколько строк текста на чёрном экране.
 * Потом сюда встанет настоящая сценка (картинки/анимация/видео от нейросети).
 * После неё запускается карта с эффектом «пробуждения».
 * Пропустить: Enter / Пробел / Esc / клик.
 */
public class IntroScreen extends ScreenAdapter {

    private static final float W = 1248f;
    private static final float H = 720f;

    private static final String[] LINES = {
        "Она забыла этот год.",
        "Остались только нить и клубок.",
        "Нужно вспомнить всё."
    };
    private static final float LINE_TIME = 3.6f;

    private final MyGame game;
    private OrthographicCamera camera;
    private FitViewport viewport;
    private final GlyphLayout layout = new GlyphLayout();
    private float time = 0f;

    public IntroScreen(MyGame game) {
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
        delta = Math.min(delta, 1f / 30f);
        time += delta;

        boolean skip = Gdx.input.isKeyJustPressed(Input.Keys.ENTER)
                    || Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                    || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)
                    || Gdx.input.justTouched();
        int index = (int) (time / LINE_TIME);
        if (skip || index >= LINES.length) {
            game.setScreen(MapScreen.afterIntro(game));
            return;
        }

        float t = time - index * LINE_TIME;
        float alpha;
        if (t < 1f) alpha = t;
        else if (t > LINE_TIME - 1f) alpha = LINE_TIME - t;
        else alpha = 1f;

        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        game.assets.fontLarge.setColor(1f, 0.93f, 0.78f, alpha);
        layout.setText(game.assets.fontLarge, LINES[index]);
        game.assets.fontLarge.draw(game.batch, layout, (W - layout.width) / 2f, H * 0.55f);

        game.assets.fontSmall.setColor(0.4f, 0.4f, 0.45f, 1f);
        layout.setText(game.assets.fontSmall, "(здесь будет вступительная сценка)    Enter — пропустить");
        game.assets.fontSmall.draw(game.batch, layout, (W - layout.width) / 2f, 40f);

        game.assets.fontLarge.setColor(Color.WHITE);
        game.assets.fontSmall.setColor(Color.WHITE);
        game.batch.end();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }
}
