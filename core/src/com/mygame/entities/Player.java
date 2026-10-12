package com.mygame.entities;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.mygame.assets.Assets;
import com.mygame.world.Room;

/**
 * Игрок: плавное движение с ускорением/трением + раздельные коллизии по осям.
 */
public class Player {

    public static final float WIDTH  = 24f;
    public static final float HEIGHT = 32f;

    private static final float MAX_SPEED = 260f;
    private static final float ACCEL     = 2200f;
    private static final float FRICTION  = 2000f;

    private final Assets assets;
    private final Vector2 position;
    private final Vector2 velocity;
    private boolean facingLeft = false;

    public Player(float x, float y, Assets assets) {
        this.assets = assets;
        this.position = new Vector2(x, y);
        this.velocity = new Vector2();
    }

    public void update(float delta, Room room) {
        Vector2 input = readInput();

        if (input.len2() > 0.01f) {
            input.nor();
            velocity.x += input.x * ACCEL * delta;
            velocity.y += input.y * ACCEL * delta;
            if (input.x < -0.1f) facingLeft = true;
            if (input.x >  0.1f) facingLeft = false;
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

    public void draw(SpriteBatch batch) {
        if (facingLeft) {
            batch.draw(assets.player, position.x + WIDTH, position.y, -WIDTH, HEIGHT);
        } else {
            batch.draw(assets.player, position.x, position.y, WIDTH, HEIGHT);
        }
    }

    public float getCenterX() { return position.x + WIDTH / 2f; }
    public float getCenterY() { return position.y + HEIGHT / 2f; }
    public Rectangle getBounds() { return new Rectangle(position.x, position.y, WIDTH, HEIGHT); }
    public void setPosition(float x, float y) {
        position.set(x, y);
        velocity.setZero();
    }
}