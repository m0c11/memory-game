package com.mygame.world;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mygame.assets.Assets;

/**
 * Объект, с которым можно взаимодействовать через E.
 * После использования становится "выключенным" и показывает более бледную текстуру.
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
        if (used) {
            batch.draw(assets.interactableUsed, x, y, SIZE, SIZE);
        } else {
            batch.draw(assets.interactable, x, y, SIZE, SIZE);
        }
    }

    public void use() { used = true; }
    public boolean isUsed() { return used; }
    public String getText() { return text; }
    public float getCenterX() { return x + SIZE / 2f; }
    public float getCenterY() { return y + SIZE / 2f; }
}