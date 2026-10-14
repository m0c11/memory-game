package com.mygame.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Align;
import com.mygame.assets.Assets;

/**
 * Диалоговая плашка в духе Undertale: чёрный прямоугольник с белой обводкой,
 * текст печатается по буквам и начинается со «* », слева может быть портрет
 * говорящего, а справа внизу мигает квадратик-подсказка «дальше».
 *
 * Первое нажатие во время печати дописывает реплику целиком, второе — закрывает.
 * Подтверждение: Z / E / Enter / Пробел.
 */
public class DialogueBox {

    /** Скорость печатной машинки, символов в секунду. */
    private static final float CHARS_PER_SECOND = 48f;

    private final Assets assets;
    private final GlyphLayout layout = new GlyphLayout();
    private final Color textColor = new Color(1f, 0.98f, 0.93f, 1f);

    private String fullText = null;
    private int shownChars = 0;
    private float charTimer = 0f;
    private boolean fullyShown = true;

    private TextureRegion portrait = null;

    private float alpha = 0f;
    private float targetAlpha = 0f;
    private float blink = 0f;

    public DialogueBox(Assets assets) {
        this.assets = assets;
    }

    /** Реплика без портрета (повествование, предметы). */
    public void show(String text) {
        show(null, text);
    }

    /** Реплика с портретом говорящего (например, клубок-героиня). */
    public void show(Texture portraitTexture, String text) {
        this.portrait = portraitTexture == null ? null : new TextureRegion(portraitTexture);
        this.fullText = "* " + text;
        this.shownChars = 0;
        this.charTimer = 0f;
        this.fullyShown = false;
        this.targetAlpha = 1f;
    }

    public void hide() {
        targetAlpha = 0f;
    }

    /** Нажатие кнопки подтверждения: дописать или закрыть. */
    public void advance() {
        if (alpha < 0.01f) return;
        if (!fullyShown) {
            shownChars = fullText.length();
            fullyShown = true;
        } else {
            hide();
        }
    }

    public boolean isVisible() {
        return alpha > 0.01f;
    }

    /** Идёт печать текста. */
    public boolean isTyping() {
        return alpha > 0.01f && !fullyShown;
    }

    public void update(float delta) {
        alpha += (targetAlpha - alpha) * Math.min(1f, delta * 10f);
        blink += delta;

        if (targetAlpha == 1f && !fullyShown && fullText != null) {
            charTimer += delta;
            int n = (int) (charTimer * CHARS_PER_SECOND);
            if (n >= fullText.length()) {
                shownChars = fullText.length();
                fullyShown = true;
            } else {
                shownChars = Math.max(0, n);
            }
        }

        if (targetAlpha == 0f && alpha < 0.01f) {
            alpha = 0f;
            fullText = null;
            portrait = null;
        }
    }

    public void draw(SpriteBatch batch, float worldWidth, float worldHeight) {
        if (fullText == null || alpha < 0.01f) return;

        float boxW = worldWidth * 0.76f;
        float boxH = 196f;
        float boxX = (worldWidth - boxW) / 2f;
        float boxY = 26f;

        // Белая обводка и чёрное поле — как в Undertale.
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(assets.pixel, boxX - 4f, boxY - 4f, boxW + 8f, boxH + 8f);
        batch.setColor(0.02f, 0.02f, 0.03f, alpha);
        batch.draw(assets.pixel, boxX, boxY, boxW, boxH);
        batch.setColor(Color.WHITE);

        // Портрет слева
        float textX = boxX + 40f;
        float wrapW = boxW - 80f;
        if (portrait != null) {
            float ps = 104f;
            float px = boxX + 26f;
            float py = boxY + (boxH - ps) / 2f;
            batch.setColor(0f, 0f, 0f, alpha);
            batch.draw(assets.pixel, px - 3f, py - 3f, ps + 6f, ps + 6f);
            batch.setColor(1f, 1f, 1f, alpha);
            batch.draw(portrait, px, py, ps, ps);
            batch.setColor(Color.WHITE);
            textX = px + ps + 28f;
            wrapW = boxX + boxW - 40f - textX;
        }

        // Печатающийся текст
        String shown = fullText.substring(0, Math.min(shownChars, fullText.length()));
        BitmapFont font = assets.fontMedium;
        textColor.a = alpha;
        layout.setText(font, shown, textColor, wrapW, Align.left, true);
        font.draw(batch, layout, textX, boxY + boxH - 36f);

        // Мигающий квадратик «дальше»
        if (fullyShown && alpha > 0.6f) {
            float a = 0.35f + 0.65f * (float) Math.abs(Math.sin(blink * 4f));
            batch.setColor(1f, 1f, 1f, a * alpha);
            batch.draw(assets.pixel, boxX + boxW - 34f, boxY + 22f, 13f, 13f);
            batch.setColor(Color.WHITE);
        }

        font.setColor(Color.WHITE);
    }
}
