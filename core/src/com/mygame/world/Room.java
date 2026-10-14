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
    private final List<Portal> portals = new ArrayList<>();
    private final List<Accent> accentsUnder = new ArrayList<>();
    private final List<Accent> accentsOver = new ArrayList<>();
    private SavePoint savePoint;
    private Rectangle doorBounds;
    private Rectangle finishBounds;
    private String name = "";
    private Color tint = Color.WHITE;
    private float colorAmount = 0f;

    /** Прямоугольная заливка цветом поверх пола или стен (для сцены, занавеса, кресел). */
    public static class Accent {
        public final Rectangle rect;
        public final Color color;
        public Accent(float x, float y, float w, float h, Color color) {
            this.rect = new Rectangle(x, y, w, h);
            this.color = color.cpy();
        }
    }

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

    /** Зона выхода из уровня (например, центр сцены). Если не задана — используется дверь. */
    public void setFinish(float x, float y, float w, float h) {
        finishBounds = new Rectangle(x, y, w, h);
    }

    public Rectangle getFinishBounds() { return finishBounds; }

    public void addInteractable(Interactable i) { interactables.add(i); }

    /** Проход в другую локацию уровня. */
    public void addPortal(Portal p) { portals.add(p); }
    public List<Portal> getPortals() { return portals; }

    public Portal portalAt(Rectangle bounds) {
        for (Portal p : portals) {
            if (p.bounds.overlaps(bounds)) return p;
        }
        return null;
    }

    public void addAccentUnder(float x, float y, float w, float h, Color color) {
        accentsUnder.add(new Accent(x, y, w, h, color));
    }

    public void addAccentOver(float x, float y, float w, float h, Color color) {
        accentsOver.add(new Accent(x, y, w, h, color));
    }

    /** Точка сохранения. Не считается предметом и не влияет на прохождение комнаты. */
    public void setSavePoint(float x, float y) {
        this.savePoint = new SavePoint(assets, x, y);
    }

    public SavePoint getSavePoint() { return savePoint; }

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

        // Оверлеи поверх пола (настил сцены, струи света и т.п.)
        drawAccents(batch, accentsUnder);

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

        // Оверлеи поверх стен (занавес, ряды кресел)
        drawAccents(batch, accentsOver);

        // Проходы между локациями — как открытые двери
        for (Portal p : portals) {
            drawPair(batch, assets.tileDoorOpen,
                     p.bounds.x, p.bounds.y, p.bounds.width, p.bounds.height);
        }

        // Точка сохранения
        if (savePoint != null) {
            savePoint.draw(batch);
        }

        // Объекты
        for (Interactable i : interactables) {
            i.draw(batch);
        }
    }

    private void drawAccents(SpriteBatch batch, List<Accent> accents) {
        for (Accent a : accents) {
            batch.setColor(a.color);
            batch.draw(assets.pixel, a.rect.x, a.rect.y, a.rect.width, a.rect.height);
        }
        batch.setColor(Color.WHITE);
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
