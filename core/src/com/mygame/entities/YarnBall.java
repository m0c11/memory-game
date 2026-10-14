package com.mygame.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.mygame.assets.Assets;
import com.mygame.world.MapWorld;
import com.mygame.world.Trail;

/**
 * Герой карты — клубок ниток. Ходит только по коридорам нити (MapWorld),
 * оставляя за собой след (Trail).
 *
 * Движение с «подруливанием»: если прямой шаг упирается в стену, клубок пробует
 * чуть довернуть в стороны. Благодаря этому по изогнутой нити можно идти,
 * просто удерживая одну клавишу, не выцеливая каждый поворот.
 */
public class YarnBall {

    public static final float RADIUS = 22f;
    private static final float SPEED = 330f;
    private static final float[] STEER_ANGLES = {0f, 20f, -20f, 40f, -40f, 60f, -60f, 80f, -80f};

    private final Vector2 position = new Vector2();
    private final Vector2 velocity = new Vector2();
    private float rotation = 0f;

    public void setPosition(float x, float y) {
        position.set(x, y);
        velocity.setZero();
    }

    public float getX() { return position.x; }
    public float getY() { return position.y; }

    public void update(float delta, MapWorld world, Trail trail, boolean controlsEnabled) {
        float ix = 0f;
        float iy = 0f;
        if (controlsEnabled) {
            if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))  ix -= 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) ix += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP))    iy += 1f;
            if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN))  iy -= 1f;
        }
        float len = (float) Math.sqrt(ix * ix + iy * iy);
        if (len > 0f) {
            ix /= len;
            iy /= len;
        }

        // Плавный разгон и остановка
        float k = Math.min(1f, delta * 10f);
        velocity.x += (ix * SPEED - velocity.x) * k;
        velocity.y += (iy * SPEED - velocity.y) * k;

        float prevX = position.x;
        float prevY = position.y;

        float dx = velocity.x * delta;
        float dy = velocity.y * delta;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > 0.001f) {
            int steps = Math.max(1, (int) Math.ceil(dist / 6f));
            float sx = dx / steps;
            float sy = dy / steps;
            for (int i = 0; i < steps; i++) {
                if (!tryMove(world, sx, sy)) {
                    velocity.setZero();
                    break;
                }
            }
        }

        rotation -= (position.x - prevX) / RADIUS * MathUtils.radiansToDegrees;
        trail.update(position.x, position.y, prevX, prevY);
    }

    private boolean tryMove(MapWorld world, float dx, float dy) {
        for (float deg : STEER_ANGLES) {
            float rad = deg * MathUtils.degreesToRadians;
            float cos = MathUtils.cos(rad);
            float sin = MathUtils.sin(rad);
            float scale = Math.max(0.35f, cos);
            float rx = (dx * cos - dy * sin) * scale;
            float ry = (dx * sin + dy * cos) * scale;
            if (world.isWalkable(position.x + rx, position.y + ry)) {
                position.add(rx, ry);
                return true;
            }
        }
        return false;
    }

    /** Мягкое свечение вокруг клубка. Вызывать при аддитивном смешивании. */
    public void drawGlow(SpriteBatch batch, Assets assets, float time) {
        float size = 230f + 14f * (float) Math.sin(time * 2.6f);
        batch.setColor(1f, 1f, 1f, 0.85f);
        batch.draw(assets.orb, position.x - size / 2f, position.y - size / 2f, size, size);
    }

    /** Сам клубок. Вызывать при обычном смешивании. */
    public void drawBody(SpriteBatch batch, TextureRegion region) {
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(region,
            position.x - RADIUS, position.y - RADIUS,
            RADIUS, RADIUS,
            RADIUS * 2f, RADIUS * 2f,
            1f, 1f, rotation);
    }
}
