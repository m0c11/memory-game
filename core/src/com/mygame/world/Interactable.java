package com.mygame.world;

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mygame.assets.Assets;

/**
 * Объект, с которым можно взаимодействовать через E.
 * Выглядит как мягко пульсирующий огонёк. После использования гаснет почти полностью.
 */
public class Interactable {

    public static final float INTERACT_RADIUS = 70f;
    public static final float SIZE = 22f;

    private final Assets assets;
    private final float x, y;
    private final String text;
    private boolean used = false;

    public Interactable(Assets assets, float x, float y, String text) {
        this.assets = assets;
        this.x = x;
        this.y = y;
        this.text = text;
    }

    public void draw(SpriteBatch batch) {
        float cx = getCenterX();
        float cy = getCenterY();
        float t = System.nanoTime() / 1_000_000_000f;

        // Свечение рисуем аддитивно, чёрное на картинке становится прозрачным
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        if (used) {
            float size = 44f;
            batch.setColor(1f, 1f, 1f, 0.18f);
            batch.draw(assets.orb, cx - size / 2f, cy - size / 2f, size, size);
        } else {
            float size = 96f + 10f * (float) Math.sin(t * 3f + x * 0.01f);
            batch.setColor(1f, 1f, 1f, 0.95f);
            batch.draw(assets.orb, cx - size / 2f, cy - size / 2f, size, size);
        }
        batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setColor(1f, 1f, 1f, 1f);
    }

    public void use() { used = true; }
    public boolean isUsed() { return used; }
    public String getText() { return text; }
    public float getCenterX() { return x + SIZE / 2f; }
    public float getCenterY() { return y + SIZE / 2f; }
}
