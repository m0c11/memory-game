package com.mygame.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.mygame.assets.Assets;
import com.mygame.world.Room;

/**
 * Игрок в комнате: героиня. Плавное движение с ускорением/трением
 * и раздельные коллизии по осям (хитбокс остаётся маленьким, как у клубка).
 *
 * Анимация:
 *  - стоит: стойка анфас с лёгким «дыханием»;
 *  - идёт: 9 кадров ходьбы в профиль (вправо), при движении влево кадры отражаются.
 *    Скорость смены кадров зависит от скорости героини, поэтому «хромая» походка
 *    (setSpeedScale) выглядит медленнее.
 * Кадры лежат в assets/chars/girl (создаются tools/prep_character.py).
 */
public class Player {

    public static final float WIDTH  = 24f;
    public static final float HEIGHT = 32f;

    /** Размер кадра на экране. Холст кадра 96x120, ноги стоят на 6 px выше его низа. */
    private static final float DRAW_W = 80f;
    private static final float DRAW_H = 100f;
    private static final float FEET_IN_FRAME = 6f * DRAW_H / 120f;
    /** Насколько выше нижнего края хитбокса стоят ступни. */
    private static final float FEET_LIFT = 2f;

    private static final float MAX_SPEED = 260f;
    private static final float ACCEL     = 2200f;
    private static final float FRICTION  = 2000f;

    /** Скорость (px/с), выше которой считаем, что героиня идёт. */
    private static final float MOVING_SPEED = 25f;
    /** Скорость смены кадров = speed / FRAME_DIVISOR (при 260 это ~12 кадров в секунду). */
    private static final float FRAME_DIVISOR = 22f;

    private final Assets assets;
    private final TextureRegion idleFront;
    private final TextureRegion[] walk;

    private final Vector2 position;
    private final Vector2 velocity;
    private float speedScale = 1f;

    private boolean facingLeft = false;
    private boolean moving = false;
    private float walkTime = 0f;
    private float idleTime = 0f;

    public Player(float x, float y, Assets assets) {
        this.assets = assets;
        this.idleFront = new TextureRegion(assets.girlIdleFront);
        this.walk = new TextureRegion[assets.girlWalk.length];
        for (int i = 0; i < walk.length; i++) {
            walk[i] = new TextureRegion(assets.girlWalk[i]);
        }
        this.position = new Vector2(x, y);
        this.velocity = new Vector2();
    }

    /** Множитель максимальной скорости (например, 0.7 — «прихрамывающая» походка). */
    public void setSpeedScale(float scale) {
        this.speedScale = scale;
    }

    public void update(float delta, Room room) {
        update(delta, room, readInput());
    }

    /** Движение с внешним вектором ввода (например, наведение мышью). */
    public void update(float delta, Room room, Vector2 input) {
        if (input.len2() > 0.01f) {
            input.nor();
            velocity.x += input.x * ACCEL * delta;
            velocity.y += input.y * ACCEL * delta;
        } else {
            applyFriction(delta);
        }

        float maxSpeed = MAX_SPEED * speedScale;
        if (velocity.len() > maxSpeed) {
            velocity.setLength(maxSpeed);
        }

        moveWithCollision(delta, room);
        updateAnimation(delta);
    }

    private Vector2 readInput() {
        Vector2 input = new Vector2();
        if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT))  input.x -= 1;
        if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) input.x += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP))    input.y += 1;
        if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN))  input.y -= 1;
        return input;
    }

    private void applyFriction(float delta) {
        float speed = velocity.len();
        if (speed <= 0f) return;
        float drop = FRICTION * delta;
        if (drop > speed) velocity.setZero();
        else velocity.scl((speed - drop) / speed);
    }

    private void moveWithCollision(float delta, Room room) {
        if (room == null) {
            // На открытой локации (база отдыха) коллизий с комнатой нет — свободное движение
            position.add(velocity.x * delta, velocity.y * delta);
            return;
        }
        float nx = position.x + velocity.x * delta;
        if (!room.collides(nx, position.y, WIDTH, HEIGHT)) {
            position.x = nx;
        } else {
            velocity.x = 0f;
        }

        float ny = position.y + velocity.y * delta;
        if (!room.collides(position.x, ny, WIDTH, HEIGHT)) {
            position.y = ny;
        } else {
            velocity.y = 0f;
        }
    }

    private void updateAnimation(float delta) {
        float speed = velocity.len();
        moving = speed > MOVING_SPEED;
        if (Math.abs(velocity.x) > 8f) {
            facingLeft = velocity.x < 0f;
        }
        if (moving) {
            walkTime += delta * speed / FRAME_DIVISOR;
            idleTime = 0f;
        } else {
            walkTime = 0f;
            idleTime += delta;
        }
    }

    public void draw(SpriteBatch batch) {
        float cx = position.x + WIDTH / 2f;
        float feetY = position.y + FEET_LIFT;

        // Тень под ногами
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(assets.shadow, cx - 24f, feetY - 8f, 48f, 16f);

        TextureRegion frame;
        float scaleY = 1f;
        if (moving) {
            frame = walk[((int) walkTime) % walk.length];
        } else {
            frame = idleFront;
            scaleY = 1f + 0.012f * (float) Math.sin(idleTime * 2.2f); // лёгкое «дыхание»
        }

        float h = DRAW_H * scaleY;
        float x = cx - DRAW_W / 2f;
        float y = feetY - FEET_IN_FRAME;
        if (facingLeft && moving) {
            batch.draw(frame, x + DRAW_W, y, -DRAW_W, h);   // отражаем по горизонтали
        } else {
            batch.draw(frame, x, y, DRAW_W, h);
        }
        batch.setColor(Color.WHITE);
    }

    public float getCenterX() { return position.x + WIDTH / 2f; }
    public float getCenterY() { return position.y + HEIGHT / 2f; }
    public Rectangle getBounds() { return new Rectangle(position.x, position.y, WIDTH, HEIGHT); }
    public void setPosition(float x, float y) {
        position.set(x, y);
        velocity.setZero();
    }
}
