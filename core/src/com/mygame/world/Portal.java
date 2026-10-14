package com.mygame.world;

import com.badlogic.gdx.math.Rectangle;

/**
 * Проход между локациями одного уровня: дверь в кулисы, выход обратно и т.п.
 *
 * Когда героиня входит в bounds, GameScreen переключает её в комнату targetRoom
 * и ставит в точку (spawnX, spawnY).
 */
public class Portal {

    public final Rectangle bounds;
    public final int targetRoom;
    public final float spawnX;
    public final float spawnY;

    public Portal(float x, float y, float w, float h, int targetRoom, float spawnX, float spawnY) {
        this.bounds = new Rectangle(x, y, w, h);
        this.targetRoom = targetRoom;
        this.spawnX = spawnX;
        this.spawnY = spawnY;
    }
}
