package com.mygame.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.mygame.assets.Assets;
import com.mygame.entities.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Комната: тайловая сетка пола/стен, список объектов, дверь.
 * Тайл = 48 px, размеры задаются в RoomFactory.
 */
public class Room {

    public static final float TILE = 48f;

    private final Assets assets;
    private final int cols;
    private final int rows;
    private final boolean[][] walls;

    private final List<Interactable> interactables = new ArrayList<>();
    private Rectangle doorBounds;
    private String name = "";
    private Color tint = Color.WHITE;

    public Room(Assets assets, int cols, int rows) {
        this.assets = assets;
        this.cols = cols;
        this.rows = rows;
        this.walls = new boolean[cols][rows];
    }

    public void setName(String name) { this.name = name; }
    public String getName() { return name; }

    public void setTint(Color c) { this.tint = c; }
    public Color getTint() { return tint; }

    public void setWall(int c, int r, boolean isWall) {
        if (c >= 0 && c < cols && r >= 0 && r < rows) {
            walls[c][r] = isWall;
        }
    }

    public void buildBorders() {
        for (int c = 0; c < cols; c++) {
            walls[c][0] = true;
            walls[c][rows - 1] = true;
        }
        for (int r = 0; r < rows; r++) {
            walls[0][r] = true;
            walls[cols - 1][r] = true;
        }
    }

    public void openDoorTiles(int col, int rowFrom, int rowTo) {
        for (int r = rowFrom; r <= rowTo; r++) {
            if (r >= 0 && r < rows) walls[col][r] = false;
        }
    }

    public void setDoor(float x, float y, float w, float h) {
        doorBounds = new Rectangle(x, y, w, h);
    }

    public void addInteractable(Interactable i) { interactables.add(i); }

    public float getPixelWidth()  { return cols * TILE; }
    public float getPixelHeight() { return rows * TILE; }

    public void draw(SpriteBatch batch) {
        // Пол
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                float x = c * TILE;
                float y = r * TILE;
                if (walls[c][r]) continue;
                // Лёгкая шахматка для читаемости пола
                boolean alt = ((c + r) & 1) == 0;
                batch.draw(alt ? assets.floor : assets.floorAlt, x, y, TILE, TILE);
            }
        }

        // Дверь (если открыта — золотая, если закрыта — тёмная)
        if (doorBounds != null) {
            if (isComplete()) {
                batch.draw(assets.doorOpen, doorBounds.x, doorBounds.y,
                        doorBounds.width, doorBounds.height);
            } else {
                batch.draw(assets.doorLocked, doorBounds.x, doorBounds.y,
                        doorBounds.width, doorBounds.height);
            }
        }

        // Стены поверх пола + тонкая подсветка сверху
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                if (!walls[c][r]) continue;
                float x = c * TILE;
                float y = r * TILE;
                batch.draw(assets.wall, x, y, TILE, TILE);
                if (r + 1 < rows && !walls[c][r + 1]) {
                    batch.draw(assets.wallTop, x, y, TILE, 6f);
                }
            }
        }

        // Объекты
        for (Interactable i : interactables) {
            i.draw(batch);
        }
    }

    /** Проверка коллизии прямоугольника с сеткой стен. */
    public boolean collides(float x, float y, float w, float h) {
        int c1 = (int) Math.floor(x / TILE);
        int c2 = (int) Math.floor((x + w - 0.01f) / TILE);
        int r1 = (int) Math.floor(y / TILE);
        int r2 = (int) Math.floor((y + h - 0.01f) / TILE);

        for (int c = c1; c <= c2; c++) {
            for (int r = r1; r <= r2; r++) {
                if (c < 0 || c >= cols || r < 0 || r >= rows) return true;
                if (walls[c][r]) return true;
            }
        }
        return false;
    }

    public boolean isPlayerAtDoor(Player player) {
        return doorBounds != null && doorBounds.overlaps(player.getBounds());
    }

    public Interactable findClosestInteractable(float cx, float cy) {
        Interactable best = null;
        float bestSq = Interactable.INTERACT_RADIUS * Interactable.INTERACT_RADIUS;
        for (Interactable i : interactables) {
            float dx = i.getCenterX() - cx;
            float dy = i.getCenterY() - cy;
            float d2 = dx * dx + dy * dy;
            if (d2 < bestSq) {
                bestSq = d2;
                best = i;
            }
        }
        return best;
    }

    public boolean isComplete() {
        for (Interactable i : interactables) {
            if (!i.isUsed()) return false;
        }
        return true;
    }
}