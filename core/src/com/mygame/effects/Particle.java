package com.mygame.effects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

/**
 * Одна частица: пылинка, искра, конфетти.
 * Живёт ограниченное время, движется с затуханием, исчезает.
 */
public class Particle {

    public float x, y;
    public float vx, vy;
    public float life;
    public float maxLife;
    public float size;
    public Color color;
    public float alpha;
    public float rotation;
    public float rotationSpeed;
    public boolean gravity;

    public Particle(float x, float y, float vx, float vy, float life, float size,
                   Color color, boolean gravity) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = life;
        this.maxLife = life;
        this.size = size;
        this.color = color;
        this.alpha = 1f;
        this.rotation = MathUtils.random(0f, 360f);
        this.rotationSpeed = MathUtils.random(-90f, 90f);
        this.gravity = gravity;
    }

    public void update(float delta) {
        life -= delta;
        if (life <= 0f) return;

        x += vx * delta;
        y += vy * delta;

        if (gravity) {
            vy -= 60f * delta; // гравитация вниз
        }

        // Затухание скорости
        vx *= (1f - delta * 1.5f);
        vy *= (1f - delta * 1.5f);

        rotation += rotationSpeed * delta;
        alpha = Math.max(0f, life / maxLife);
    }

    public boolean isDead() {
        return life <= 0f;
    }

    public void draw(SpriteBatch batch, TextureRegion texture) {
        if (life <= 0f) return;

        float s = size * (0.5f + 0.5f * alpha);
        batch.setColor(color.r, color.g, color.b, alpha * color.a);
        batch.draw(texture, x - s / 2f, y - s / 2f, s / 2f, s / 2f, s, s, 1f, 1f, rotation);
        batch.setColor(Color.WHITE);
    }
}
