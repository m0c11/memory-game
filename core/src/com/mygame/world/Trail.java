package com.mygame.world;

import com.badlogic.gdx.math.Vector2;

import java.util.ArrayList;
import java.util.List;

/**
 * Нить, которую клубок оставляет за собой. Остаётся на карте навсегда:
 * в конце игры именно она «выписывает» всю фразу.
 *
 * Чтобы список не раздувался, когда герой ходит по уже пройденным местам,
 * новые точки добавляются только на «свежей» земле (сетка посещённых клеток).
 * Соседние точки соединяются линией, только если они близко друг к другу.
 */
public class Trail {

    public static final float SPACING = 14f;
    public static final float MAX_LINK = SPACING * 3.5f;
    private static final float CELL = 12f;
    private static final int MAX_POINTS = 20000;

    private final List<Vector2> points = new ArrayList<>();
    private final boolean[] visited;
    private final int gridW;
    private final int gridH;
    private boolean lastFresh = false;

    public Trail(float worldWidth, float worldHeight) {
        gridW = (int) Math.ceil(worldWidth / CELL) + 2;
        gridH = (int) Math.ceil(worldHeight / CELL) + 2;
        visited = new boolean[gridW * gridH];
    }

    public List<Vector2> getPoints() {
        return points;
    }

    /** Вызывается каждый кадр с текущей и предыдущей позицией героя. */
    public void update(float x, float y, float prevX, float prevY) {
        boolean fresh = !isVisited(x, y);
        if (fresh && points.size() < MAX_POINTS) {
            if (points.isEmpty() || !lastFresh) {
                add(prevX, prevY); // «якорь» там, где нить начала новый участок
            }
            Vector2 last = points.get(points.size() - 1);
            if (last.dst2(x, y) >= SPACING * SPACING) {
                add(x, y);
            }
        }
        lastFresh = fresh;
    }

    private void add(float x, float y) {
        points.add(new Vector2(x, y));
        mark(x, y);
    }

    private boolean isVisited(float x, float y) {
        int cx = (int) (x / CELL);
        int cy = (int) (y / CELL);
        if (cx < 0 || cy < 0 || cx >= gridW || cy >= gridH) return true;
        return visited[cy * gridW + cx];
    }

    private void mark(float x, float y) {
        int cx = (int) (x / CELL);
        int cy = (int) (y / CELL);
        for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
                int nx = cx + dx;
                int ny = cy + dy;
                if (nx >= 0 && ny >= 0 && nx < gridW && ny < gridH) {
                    visited[ny * gridW + nx] = true;
                }
            }
        }
    }

    // ---------- сохранение / загрузка ----------

    public String serialize() {
        StringBuilder sb = new StringBuilder(points.size() * 10);
        for (Vector2 p : points) {
            sb.append(Math.round(p.x)).append(',').append(Math.round(p.y)).append(';');
        }
        return sb.toString();
    }

    public void load(String data) {
        points.clear();
        java.util.Arrays.fill(visited, false);
        lastFresh = false;
        if (data == null || data.isEmpty()) return;

        for (String token : data.split(";")) {
            if (token.isEmpty()) continue;
            String[] xy = token.split(",");
            if (xy.length != 2) continue;
            try {
                add(Float.parseFloat(xy[0]), Float.parseFloat(xy[1]));
            } catch (NumberFormatException ignored) {
                // битую точку просто пропускаем
            }
        }
    }
}
