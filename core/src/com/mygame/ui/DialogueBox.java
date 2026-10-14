package com.mygame.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.Align;
import com.mygame.assets.Assets;

/**
 * Диалоговая плашка снизу экрана на картинке-рамке (ui/dialogue.png).
 * Показывает текст и подсказку "[E] продолжить".
 */
public class DialogueBox {

    private final Assets assets;
    private final GlyphLayout layout = new GlyphLayout();
    private final Color textColor = new Color(1f, 0.97f, 0.9f, 1f);

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

        float boxW = worldWidth * 0.74f;
        float boxH = 176f;
        float boxX = (worldWidth - boxW) / 2f;
        float boxY = 28f;

        // рамка-картинка (тёмное стекло с золотой нитью по краю)
        batch.setColor(1f, 1f, 1f, 0.97f * alpha);
        batch.draw(assets.dialoguePanel, boxX, boxY, boxW, boxH);
        batch.setColor(Color.WHITE);

        // основной текст
        BitmapFont font = assets.fontMedium;
        textColor.a = alpha;
        layout.setText(font, text, textColor, boxW - 130f, Align.left, true);
        font.draw(batch, layout, boxX + 66f, boxY + boxH - 38f);

        // подсказка
        BitmapFont hint = assets.fontSmall;
        hint.setColor(0.82f, 0.74f, 0.55f, alpha);
        hint.draw(batch, "[E] продолжить", boxX + boxW - 200f, boxY + 40f);

        hint.setColor(Color.WHITE);
        font.setColor(Color.WHITE);
    }
}
