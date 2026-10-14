package com.mygame.world;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mygame.assets.Assets;

/**
 * Точка сохранения в духе Undertale: тёплая четырёхлучевая звёздочка,
 * мягко пульсирующая на полу. Подойди и нажми Z — прогресс сохранится,
 * а на экране появится «Оставайся решительной».
 */
public class SavePoint {

    public static final float INTERACT_RADIUS = 66f;

    private final Assets assets;
    private final float x, y;

    public SavePoint(Assets assets, float x, float y) {
        this.assets = assets;
        this.x = x;
        this.y = y;
    }

    public void draw(SpriteBatch batch) {
        float cx = getCenterX();
        float cy = getCenterY();
        float t = System.nanoTime() / 1_000_000_000f;

        // Тёплый ореол — аддитивно
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        float glow = 120f + 12f * (float) Math.sin(t * 2.4f);
        batch.setColor(1f, 0.95f, 0.6f, 0.5f);
        batch.draw(assets.orb, cx - glow / 2f, cy - glow / 2f, glow, glow);
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        // Сама звезда
        float size = 46f + 4f * (float) Math.sin(t * 3f);
        batch.setColor(1f, 0.98f, 0.78f, 1f);
        batch.draw(assets.saveStar, cx - size / 2f, cy - size / 2f, size, size);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public float getCenterX() { return x; }
    public float getCenterY() { return y; }
}
