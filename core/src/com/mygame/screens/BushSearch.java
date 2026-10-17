package com.mygame.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.mygame.MyGame;
import com.mygame.assets.Assets;
import com.mygame.world.Bush;

import java.util.ArrayList;
import java.util.List;

/**
 * Оверлей поиска внутри куста.
 *
 * Рисуется поверх основного экрана с сильно размытым фоном.
 * Игрок кликает мышкой по листьям, чтобы убрать их.
 * Под некоторыми листьями спрятаны бутылки.
 * Чем дольше ищешь — тем выше шанс, что организатор найдёт.
 */
public class BushSearch {

    public static final float W = 1248f;
    public static final float H = 720f;

    private static final float RISK_DELAY = 8f;
    private static final float RISK_RATE = 0.03f;

    private final MyGame game;
    private final Bush bush;
    private final Runnable onExit;
    private final com.badlogic.gdx.utils.viewport.Viewport viewport;

    private List<Leaf> leaves = new ArrayList<>();
    private int bottlesFound = 0;
    private float time = 0f;
    private boolean caught = false;
    private float caughtTimer = 0f;
    private boolean finished = false;
    private final GlyphLayout layout = new GlyphLayout();
    private final Vector2 touch = new Vector2();

    private static class Leaf {
        float x, y, w, h;
        boolean hasBottle;
        boolean removed;

        Leaf(float x, float y, boolean hasBottle) {
            this.x = x;
            this.y = y;
            this.w = 90;
            this.h = 70;
            this.hasBottle = hasBottle;
            this.removed = false;
        }
    }

    public BushSearch(MyGame game, Bush bush, com.badlogic.gdx.utils.viewport.Viewport viewport, Runnable onExit) {
        this.game = game;
        this.bush = bush;
        this.viewport = viewport;
        this.onExit = onExit;
        createLeaves();
    }

    private void createLeaves() {
        leaves.clear();
        int totalLeaves = 10 + MathUtils.random(0, 5);
        int bottles = bush.hasBottle() ? 1 : 0;

        for (int i = 0; i < totalLeaves; i++) {
            float x = MathUtils.random(100, W - 100);
            float y = MathUtils.random(100, H - 100);
            boolean hasBottle = (i < bottles);
            leaves.add(new Leaf(x, y, hasBottle));
        }
    }

    /** @return true, если поиск завершён и оверлей нужно убрать. */
    public boolean update(float delta) {
        if (finished) {
            caughtTimer += delta;
            if (caughtTimer > 1.5f) {
                return true;
            }
            return false;
        }

        time += delta;

        // Риск быть найденным
        if (time > RISK_DELAY && !caught) {
            float chance = RISK_RATE * (time - RISK_DELAY);
            if (MathUtils.random() < chance * delta) {
                caught = true;
                caughtTimer = 0f;
                game.assets.sfxThud.play(0.9f);
            }
        }

        // Клик по листьям
        if (Gdx.input.isTouched()) {
            touch.set(Gdx.input.getX(), Gdx.input.getY());
            viewport.unproject(touch);

            for (Leaf leaf : leaves) {
                if (!leaf.removed && touch.x > leaf.x && touch.x < leaf.x + leaf.w
                    && touch.y > leaf.y && touch.y < leaf.y + leaf.h) {
                    leaf.removed = true;
                    if (leaf.hasBottle) {
                        bottlesFound++;
                        game.assets.sfxChime.play(0.7f);
                    }
                }
            }
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            finished = true;
        }

        return false;
    }

    public void draw(SpriteBatch batch) {
        Assets a = game.assets;

        // Размытый фон — сильно затемняем основной экран
        batch.setColor(0f, 0f, 0f, 0.75f);
        batch.draw(a.pixel, 0, 0, W, H);
        batch.setColor(Color.WHITE);

        // Рисуем листья
        for (Leaf leaf : leaves) {
            if (leaf.removed) continue;

            // Лист
            batch.setColor(0.15f, 0.35f, 0.15f, 0.95f);
            batch.draw(a.pixel, leaf.x, leaf.y, leaf.w, leaf.h);

            // Прожilки
            batch.setColor(0.1f, 0.25f, 0.1f, 0.8f);
            batch.draw(a.pixel, leaf.x + leaf.w * 0.2f, leaf.y + leaf.h * 0.3f, leaf.w * 0.6f, 2);
            batch.draw(a.pixel, leaf.x + leaf.w * 0.2f, leaf.y + leaf.h * 0.5f, leaf.w * 0.6f, 2);
            batch.draw(a.pixel, leaf.x + leaf.w * 0.2f, leaf.y + leaf.h * 0.7f, leaf.w * 0.6f, 2);
        }

        batch.setColor(Color.WHITE);

        // HUD
        a.fontMedium.setColor(1f, 0.9f, 0.6f, 0.95f);
        layout.setText(a.fontMedium, "Бутылки: " + bottlesFound);
        a.fontMedium.draw(batch, layout, 36f, H - 30f);

        a.fontSmall.setColor(0.7f, 0.7f, 0.75f, 0.9f);
        layout.setText(a.fontSmall, "Клик — убрать лист. ESC — выйти.");
        a.fontSmall.draw(batch, layout, (W - layout.width) / 2f, 26f);

        if (caught) {
            a.fontMenu.setColor(1f, 0.3f, 0.3f, 0.95f);
            layout.setText(a.fontMenu, "ТЕБЯ НАШЛИ!");
            a.fontMenu.draw(batch, layout, (W - layout.width) / 2f, H / 2f);
        }

        a.fontMedium.setColor(Color.WHITE);
        a.fontSmall.setColor(Color.WHITE);
        a.fontMenu.setColor(Color.WHITE);
    }

    public int getBottlesFound() {
        return bottlesFound;
    }

    public boolean isCaught() {
        return caught;
    }

    public void finish() {
        finished = true;
    }

    public void exit() {
        onExit.run();
    }
}
