package com.mygame.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.mygame.assets.Assets;
import com.mygame.world.Room;

/**
 * Игрок в комнате: клубок. Плавное движение с ускорением/трением
 * и раздельные коллизии по осям. Клубок катится при движении.
 */
public class Player {

    public static final float WIDTH  = 24f;
    public static final float HEIGHT = 32f;

    /** Размер картинки клубка на экране (больше хитбокса, чтобы выглядел приятнее). */
    private static final float SPRITE = 40f;

    private static final float MAX_SPEED = 260f;
    private static final float ACCEL     = 2200f;
    private static final float FRICTION  = 2000f;

    private final TextureRegion region;
    private final Vector2 position;
    private final Vector2 velocity;
    private float rotation = 0f;

    public Player(float x, float y, Assets assets) {
        this.region = new TextureRegion(assets.yarnBall);
        this.position = new Vector2(x, y);
        this.velocity = new Vector2();
    }

    public void update(float delta, Room room) {
        Vector2 input = readInput();

        if (input.len2() > 0.01f) {
            input.nor();
            velocity.x += input.x * ACCEL * delta;
            velocity.y += input.y * ACCEL * delta;
        } else {
            applyFriction(delta);
        }

        if (velocity.len() > MAX_SPEED) {
            velocity.setLength(MAX_SPEED);
        }

        moveWithCollision(delta, room);
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
        float startX = position.x;

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

        // Клубок катится по горизонтали
        rotation -= (position.x - startX) / (SPRITE / 2f) * 57.2958f;
    }

    public void draw(SpriteBatch batch) {
        float cx = position.x + WIDTH / 2f;
        float cy = position.y + HEIGHT / 2f;
        batch.draw(region,
            cx - SPRITE / 2f, cy - SPRITE / 2f,
            SPRITE / 2f, SPRITE / 2f,
            SPRITE, SPRITE,
            1f, 1f, rotation);
    }

    public float getCenterX() { return position.x + WIDTH / 2f; }
    public float getCenterY() { return position.y + HEIGHT / 2f; }
    public Rectangle getBounds() { return new Rectangle(position.x, position.y, WIDTH, HEIGHT); }
    public void setPosition(float x, float y) {
        position.set(x, y);
        velocity.setZero();
    }
}
