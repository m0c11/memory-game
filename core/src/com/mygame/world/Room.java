package com.mygame.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Rectangle;
import com.mygame.assets.Assets;
import com.mygame.entities.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Комната: тайловая сетка пола/стен, список объектов, дверь.
 * Тайл = 48 px, размеры задаются в RoomFactory.
 *
 * Картинки тайлов существуют в двух вариантах: серый и цветной.
 * colorAmount (0..1) решает, насколько цвет уже «вернулся»:
 * сначала рисуется серый тайл, поверх него цветной с прозрачностью colorAmount.
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
    private float colorAmount = 0f;

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

    /** 0 — комната полностью серая, 1 — полностью цветная. */
    public void setColorAmount(float amount) {
        this.colorAmount = Math.max(0f, Math.min(1f, amount));
    }
    public float getColorAmount() { return colorAmount; }

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

    /** Рисует серую текстуру, а поверх неё цветную с прозрачностью colorAmount. */
    private void drawPair(SpriteBatch batch, Texture[] pair, float x, float y, float w, float h) {
        batch.setColor(1f, 1f, 1f, 1f);
        batch.draw(pair[0], x, y, w, h);
        if (colorAmount > 0.01f) {
            batch.setColor(1f, 1f, 1f, colorAmount);
            batch.draw(pair[1], x, y, w, h);
            batch.setColor(1f, 1f, 1f, 1f);
        }
    }

    public void draw(SpriteBatch batch) {
        // Пол
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                if (walls[c][r]) continue;
                drawPair(batch, assets.tileFloor, c * TILE, r * TILE, TILE, TILE);
            }
        }

        // Дверь: закрыта (тёмно-фиолетовая) или открыта (тёплый свет)
        if (doorBounds != null) {
            Texture[] pair = isComplete() ? assets.tileDoorOpen : assets.tileDoorClosed;
            drawPair(batch, pair, doorBounds.x, doorBounds.y,
                     doorBounds.width, doorBounds.height);
        }

        // Стены
        for (int c = 0; c < cols; c++) {
            for (int r = 0; r < rows; r++) {
                if (!walls[c][r]) continue;
                drawPair(batch, assets.tileWall, c * TILE, r * TILE, TILE, TILE);
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
