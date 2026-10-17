package com.mygame.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mygame.assets.Assets;

/**
 * Куст — объект, в котором можно прятаться и искать бутылки.
 * Под листьями куста могут быть спрятаны бутылки.
 */
public class Bush {

    public static final float INTERACT_RADIUS = 80f;
    public static final float SIZE = 70f;

    private final Assets assets;
    private final float x, y;
    private final boolean hasBottle;
    private boolean searched = false;

    public Bush(Assets assets, float x, float y, boolean hasBottle) {
        this.assets = assets;
        this.x = x;
        this.y = y;
        this.hasBottle = hasBottle;
    }

    public void draw(SpriteBatch batch) {
        float cx = getCenterX();
        float cy = getCenterY();
        float t = System.nanoTime() / 1_000_000_000f;

        // Куст — тёмно-зелёный круг с пульсацией
        float pulse = 0.04f * (float) Math.sin(t * 2f + x * 0.01f);
        batch.setColor(0.08f + pulse, 0.18f + pulse, 0.08f + pulse, 0.95f);
        batch.draw(assets.pixel, cx - SIZE / 2, cy - SIZE / 2, SIZE, SIZE);

        // Внутренние «листья»
        batch.setColor(0.12f, 0.28f, 0.12f, 0.9f);
        batch.draw(assets.pixel, cx - SIZE / 3, cy - SIZE / 3, SIZE * 0.66f, SIZE * 0.66f);

        // Центр
        batch.setColor(0.06f, 0.12f, 0.06f, 0.95f);
        batch.draw(assets.pixel, cx - 8, cy - 8, 16, 16);

        batch.setColor(Color.WHITE);
    }

    public float getCenterX() { return x + SIZE / 2f; }
    public float getCenterY() { return y + SIZE / 2f; }
    public boolean hasBottle() { return hasBottle; }
    public boolean isSearched() { return searched; }
    public void markSearched() { searched = true; }
}
