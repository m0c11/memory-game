package com.mygame.world;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.MathUtils;
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
    private final List<Bush> bushes = new ArrayList<>();
    private final List<Accent> accentsUnder = new ArrayList<>();
    private final List<Accent> accentsOver = new ArrayList<>();
    private final List<Decor> decorUnder = new ArrayList<>();
    private final List<Decor> decorOver = new ArrayList<>();
    /** Комната со своим оформлением из RoomLayout: стандартные пол, стены и проходы не рисуются. */
    private boolean themed = false;
    private SavePoint savePoint;
    private Rectangle doorBounds;
    private Rectangle finishBounds;
    /** Прогресс открытия двери: 0 = закрыта, 1 = полностью открыта (анимация). */
    private float doorOpenProgress = 0f;
    /** Цель прогресса двери (куда анимируем). */
    private float doorOpenTarget = 0f;

    /** Световые эффекты сцены (концертный свет). */
    private final List<LightCue> lightCues = new ArrayList<>();

    /** Один цветной прожектор с анимацией цвета и позиции. */
    /** Один стробоскопный круг света — мигает, скачет по сцене, меняет цвет. */
    public static class LightCue {
        public float x, y;           // текущая позиция центра круга
        public float w, h;           // размер круга
        public Color color;          // текущий цвет
        public float targetR, targetG, targetB;
        public float targetX, targetY; // цель перемещения
        public float speed;          // скорость перемещения и смены цвета
        public boolean additive;     // аддитивное смешивание (свет)

        // Стробоскоп: мигание
        public float flashTimer = 0f;
        public float flashInterval = 0.3f; // как часто мигает
        public float flashDuration = 0.08f; // сколько времени горит

        // Перемещения: скачок в новое место
        public float moveTimer = 0f;
        public float moveInterval = 0.6f; // как часто меняет место

        public LightCue(float x, float y, float w, float h, Color color) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.color = color.cpy();
            this.targetR = color.r;
            this.targetG = color.g;
            this.targetB = color.b;
            this.targetX = x;
            this.targetY = y;
            this.speed = 4f;
            this.additive = true;
            this.flashInterval = MathUtils.random(0.15f, 0.5f);
            this.flashDuration = MathUtils.random(0.05f, 0.12f);
            this.moveInterval = MathUtils.random(0.4f, 1.0f);
        }
    }

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

    /**
     * Элемент оформления: картинка (пара серая/цветная) или цветной прямоугольник.
     * Серый «двойник» прямоугольника считается из цвета, как у картинок.
     */
    private static class Decor {
        final Texture[] pair;   // для картинки
        final Color color;      // для прямоугольника (цветной вариант)
        final Color grey;       // для прямоугольника (серый вариант)
        final float x, y, w, h;

        Decor(Texture[] pair, float x, float y, float w, float h) {
            this.pair = pair;
            this.color = null;
            this.grey = null;
            this.x = x; this.y = y; this.w = w; this.h = h;
        }

        Decor(Color color, float x, float y, float w, float h) {
            this.pair = null;
            this.color = color.cpy();
            float lum = 0.299f * color.r + 0.587f * color.g + 0.114f * color.b;
            this.grey = new Color(Math.min(1f, lum * 0.89f), Math.min(1f, lum * 1.03f),
                                  Math.min(1f, lum * 1.11f), color.a);
            this.x = x; this.y = y; this.w = w; this.h = h;
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

    public Rectangle getDoorBounds() { return doorBounds; }

    public Rectangle getFinishBounds() { return finishBounds; }

    /** Установить цель открытия двери (0 = закрыта, 1 = открыта). Вызывается при завершении уровня. */
    public void setDoorOpenTarget(float target) {
        this.doorOpenTarget = Math.max(0f, Math.min(1f, target));
    }

    public float getDoorOpenProgress() { return doorOpenProgress; }

    /** Обновить анимацию двери. Возвращает true, если ещё анимируется. */
    public boolean updateDoor(float delta) {
        float diff = doorOpenTarget - doorOpenProgress;
        if (Math.abs(diff) < 0.001f) {
            doorOpenProgress = doorOpenTarget;
            return false;
        }
        doorOpenProgress += diff * Math.min(1f, delta * 4f); // скорость анимации
        return true;
    }

    public void addInteractable(Interactable i) { interactables.add(i); }

    /** Куст — объект для поиска бутылок. */
    public void addBush(Bush bush) { bushes.add(bush); }
    public List<Bush> getBushes() { return bushes; }

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

    /** Добавить цветной прожектор сцены. */
    public void addLightCue(LightCue cue) {
        lightCues.add(cue);
    }

    public List<LightCue> getLightCues() { return lightCues; }

    /** Обновить все световые эффекты. */
    /** Обновить все стробоскопы: мигание, перемещение, смена цвета. */
    public void updateLights(float delta) {
        for (LightCue c : lightCues) {
            // Стробоскоп: мигание
            c.flashTimer += delta;
            if (c.flashTimer > c.flashInterval) {
                c.flashTimer = 0f;
                c.flashInterval = MathUtils.random(0.15f, 0.5f);
                c.flashDuration = MathUtils.random(0.04f, 0.12f);
                // Случайный цвет при вспышке
                c.targetR = MathUtils.random(0.3f, 1f);
                c.targetG = MathUtils.random(0.1f, 0.7f);
                c.targetB = MathUtils.random(0.2f, 1f);
            }
            // Цвет плавно меняется к цели
            c.color.r += (c.targetR - c.color.r) * Math.min(1f, delta * c.speed);
            c.color.g += (c.targetG - c.color.g) * Math.min(1f, delta * c.speed);
            c.color.b += (c.targetB - c.color.b) * Math.min(1f, delta * c.speed);

            // Перемещение: скачок в новое место над сценой
            c.moveTimer += delta;
            if (c.moveTimer > c.moveInterval) {
                c.moveTimer = 0f;
                c.moveInterval = MathUtils.random(0.3f, 0.9f);
                c.targetX = MathUtils.random(4f * Room.TILE, 22f * Room.TILE);
                c.targetY = MathUtils.random(11f * Room.TILE, 14f * Room.TILE);
                c.speed = MathUtils.random(4f, 8f);
            }
            // Плавное движение к цели
            c.x += (c.targetX - c.x) * Math.min(1f, delta * c.speed);
            c.y += (c.targetY - c.y) * Math.min(1f, delta * c.speed);
        }
    }

    /** Точка сохранения. Не считается предметом и не влияет на прохождение комнаты. */
    public void setSavePoint(float x, float y) {
        this.savePoint = new SavePoint(assets, x, y);
    }

    public SavePoint getSavePoint() { return savePoint; }

    /** Включает «своё оформление»: стандартные тайлы пола/стен и двери-проходы не рисуются. */
    public void setThemed(boolean themed) { this.themed = themed; }

    /** Картинка оформления. over=false — под стенами и кульями (пол, ковёр), true — поверх (стены, кресла, реквизит). */
    public void addDecorSprite(Texture[] pair, float x, float y, float w, float h, boolean over) {
        (over ? decorOver : decorUnder).add(new Decor(pair, x, y, w, h));
    }

    /** Цветной прямоугольник оформления (с серым двойником, цвет возвращается по ходу уровня). */
    public void addDecorRect(float x, float y, float w, float h, Color color, boolean over) {
        (over ? decorOver : decorUnder).add(new Decor(color, x, y, w, h));
    }

    /** Сколько предметов в комнате уже использовано. */
    public int getUsedCount() {
        int n = 0;
        for (Interactable i : interactables) if (i.isUsed()) n++;
        return n;
    }

    /** Сколько всего предметов в комнате. */
    public int getTotalCount() { return interactables.size(); }

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
        if (!themed) {
            for (int c = 0; c < cols; c++) {
                for (int r = 0; r < rows; r++) {
                    if (walls[c][r]) continue;
                    drawPair(batch, assets.tileFloor, c * TILE, r * TILE, TILE, TILE);
                }
            }
        }

        // Оформление под стенами (пол, настил сцены, ковёр) и оверлеи поверх пола
        drawDecor(batch, decorUnder);
        drawAccents(batch, accentsUnder);

        // Дверь: закрыта (тёмно-фиолетовая) или открыта (тёплый свет), анимация открытия
        if (doorBounds != null) {
            Texture[] pair = isComplete() ? assets.tileDoorOpen : assets.tileDoorClosed;
            // Если двери анимируются — рисуем их смещёнными по прогрессу
            if (doorOpenProgress > 0.001f) {
                float shift = doorBounds.height * doorOpenProgress;
                batch.setColor(1f, 1f, 1f, 1f);
                batch.draw(pair[0], doorBounds.x, doorBounds.y - shift,
                         doorBounds.width, doorBounds.height);
                if (colorAmount > 0.01f) {
                    batch.setColor(1f, 1f, 1f, colorAmount);
                    batch.draw(pair[1], doorBounds.x, doorBounds.y - shift,
                             doorBounds.width, doorBounds.height);
                    batch.setColor(1f, 1f, 1f, 1f);
                }
            } else {
                drawPair(batch, pair, doorBounds.x, doorBounds.y,
                         doorBounds.width, doorBounds.height);
            }
        }

        // Стены
        if (!themed) {
            for (int c = 0; c < cols; c++) {
                for (int r = 0; r < rows; r++) {
                    if (!walls[c][r]) continue;
                    drawPair(batch, assets.tileWall, c * TILE, r * TILE, TILE, TILE);
                }
            }
        }

        // Оверлеи поверх стен и оформление поверх (стены, занавес, кресла, реквизит)
        drawAccents(batch, accentsOver);
        drawDecor(batch, decorOver);

        // Проходы между локациями — как открытые двери (в комнатах со своим оформлением рисует оно)
        if (!themed) {
            for (Portal p : portals) {
                drawPair(batch, assets.tileDoorOpen,
                         p.bounds.x, p.bounds.y, p.bounds.width, p.bounds.height);
            }
        }

        // Точка сохранения
        if (savePoint != null) {
            savePoint.draw(batch);
        }

        // Объекты
        for (Interactable i : interactables) {
            i.draw(batch);
        }

        // Кусты
        for (Bush bush : bushes) {
            bush.draw(batch);
        }
    }

    private void drawDecor(SpriteBatch batch, List<Decor> list) {
        for (Decor d : list) {
            if (d.pair != null) {
                drawPair(batch, d.pair, d.x, d.y, d.w, d.h);
            } else {
                batch.setColor(d.grey);
                batch.draw(assets.pixel, d.x, d.y, d.w, d.h);
                if (colorAmount > 0.01f) {
                    batch.setColor(d.color.r, d.color.g, d.color.b, d.color.a * colorAmount);
                    batch.draw(assets.pixel, d.x, d.y, d.w, d.h);
                }
            }
        }
        batch.setColor(Color.WHITE);
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
