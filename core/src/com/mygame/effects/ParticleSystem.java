package com.mygame.effects;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Система частиц: пыль в воздухе, искры при сборе предметов, конфетти.
 * Управляет множеством частиц, обновляет и рисует их.
 */
public class ParticleSystem {

    private final List<Particle> particles = new ArrayList<>();
    private final TextureRegion texture;

    public ParticleSystem(TextureRegion texture) {
        this.texture = texture;
    }

    /** Добавить одну частицу. */
    public void emit(float x, float y, float vx, float vy, float life, float size,
                    Color color, boolean gravity) {
        particles.add(new Particle(x, y, vx, vy, life, size, color, gravity));
    }

    /** Вспышка искр в точке (при сборе предмета). */
    public void burst(float x, float y, Color color, int count) {
        for (int i = 0; i < count; i++) {
            float angle = MathUtils.random(0f, 360f);
            float speed = MathUtils.random(40f, 120f);
            float vx = (float) Math.cos(Math.toRadians(angle)) * speed;
            float vy = (float) Math.sin(Math.toRadians(angle)) * speed;
            emit(x, y, vx, vy, MathUtils.random(0.4f, 0.9f),
                 MathUtils.random(3f, 7f), color, true);
        }
    }

    /** Медленно плавающая пылинка. */
    public void dust(float x, float y) {
        emit(x, y,
             MathUtils.random(-8f, 8f), MathUtils.random(4f, 14f),
             MathUtils.random(2f, 5f),
             MathUtils.random(1.5f, 3.5f),
             new Color(1f, 0.95f, 0.8f, 0.35f),
             false);
    }

    /** Конфетти при завершении уровня. */
    public void confetti(float x, float y) {
        Color[] colors = {
            new Color(1f, 0.4f, 0.4f, 1f),
            new Color(0.4f, 1f, 0.4f, 1f),
            new Color(0.4f, 0.4f, 1f, 1f),
            new Color(1f, 1f, 0.4f, 1f),
            new Color(1f, 0.4f, 1f, 1f),
        };
        for (int i = 0; i < 30; i++) {
            float angle = MathUtils.random(-60f, 60f);
            float speed = MathUtils.random(80f, 200f);
            float vx = (float) Math.cos(Math.toRadians(angle)) * speed;
            float vy = (float) Math.sin(Math.toRadians(angle)) * speed + 60f;
            Color c = colors[MathUtils.random(0, colors.length - 1)];
            emit(x, y, vx, vy, MathUtils.random(0.8f, 1.6f),
                 MathUtils.random(4f, 8f), c, true);
        }
    }

    public void update(float delta) {
        Iterator<Particle> it = particles.iterator();
        while (it.hasNext()) {
            Particle p = it.next();
            p.update(delta);
            if (p.isDead()) it.remove();
        }
    }

    public void draw(SpriteBatch batch) {
        for (Particle p : particles) {
            p.draw(batch, texture);
        }
    }

    public void clear() {
        particles.clear();
    }

    public int size() {
        return particles.size();
    }
}
