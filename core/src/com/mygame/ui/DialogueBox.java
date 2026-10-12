package com.mygame.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.mygame.assets.Assets;

/**
 * Всплывающая диалоговая плашка снизу экрана.
 * Показывает текст и подсказку "[E] продолжить".
 */
public class DialogueBox {

    private final Assets assets;
    private final GlyphLayout layout = new GlyphLayout();

    private String text = null;
    private float alpha = 0f;
    private float targetAlpha = 0f;

    public DialogueBox(Assets assets) {
        this.assets = assets;
    }

    public void show(String text) {
        this.text = text;
        this.targetAlpha = 1f;
    }

    public void hide() {
        this.targetAlpha = 0f;
    }

    public boolean isVisible() {
        return alpha > 0.01f;
    }

    public void update(float delta) {
        alpha += (targetAlpha - alpha) * Math.min(1f, delta * 10f);
        if (targetAlpha == 0f && alpha < 0.01f) {
            alpha = 0f;
            text = null;
        }
    }

    public void draw(SpriteBatch batch, float worldWidth, float worldHeight) {
        if (text == null || alpha < 0.01f) return;

        float boxW = worldWidth * 0.78f;
        float boxH = 140f;
        float boxX = (worldWidth - boxW) / 2f;
        float boxY = 36f;

        // фон
        batch.setColor(0f, 0f, 0f, 0.85f * alpha);
        batch.draw(assets.pixel, boxX, boxY, boxW, boxH);

        // рамка
        batch.setColor(1f, 0.84f, 0f, alpha);
        float b = 2f;
        batch.draw(assets.pixel, boxX,                    boxY,                    boxW, b);
        batch.draw(assets.pixel, boxX,                    boxY + boxH - b,         boxW, b);
        batch.draw(assets.pixel, boxX,                    boxY,                    b,    boxH);
        batch.draw(assets.pixel, boxX + boxW - b,         boxY,                    b,    boxH);
        batch.setColor(Color.WHITE);

        // основной текст
        BitmapFont font = assets.fontMedium;
        font.setColor(1f, 1f, 1f, alpha);
        layout.setText(font, text, Color.WHITE, boxW - 60f, Align.left, true);
        font.draw(batch, layout, boxX + 30f, boxY + boxH - 24f);

        // подсказка
        BitmapFont hint = assets.fontSmall;
        hint.setColor(0.75f, 0.75f, 0.75f, alpha);
        hint.draw(batch, "[E] продолжить", boxX + boxW - 180f, boxY + 22f);

        hint.setColor(Color.WHITE);
        font.setColor(Color.WHITE);
    }
}