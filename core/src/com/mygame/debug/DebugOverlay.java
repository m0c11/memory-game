package com.mygame.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mygame.MyGame;
import com.mygame.assets.Assets;

/**
 * Отладочный overlay на экране. Показывает:
 *  - FPS, имя текущего экрана, номер уровня
 *  - координаты игрока (X, Y)
 *  - счетчик бутылок (уровень 5)
 *  - состояние дверей/порталов комнаты
 * Включается клавишей F1 (переключатель).
 *
 * Использование в экране (в методе render):
 *   if (DebugOverlay.isEnabled()) overlay.draw(batch, game, delta);
 */
public class DebugOverlay {

    private static boolean enabled = false;
    private final GlyphLayout layout = new GlyphLayout();
    private int frames = 0;
    private float fpsTimer = 0f;
    private int fps = 0;

    /** Включить/выключить overlay (например, по клавише F1). */
    public static void setEnabled(boolean v) {
        enabled = v;
    }

    public static boolean isEnabled() {
        return enabled;
    }

    /** Переключить состояние. */
    public static void toggle() {
        enabled = !enabled;
    }

    /**
     * Нарисовать отладочную информацию в левом верхнем углу.
     * Вызывать после основного рисования, в экранных координатах.
     */
    public void draw(SpriteBatch batch, MyGame game, float delta) {
        if (!enabled) return;

        frames++;
        fpsTimer += delta;
        if (fpsTimer >= 1f) {
            fps = frames;
            frames = 0;
            fpsTimer = 0f;
        }

        Assets a = game.assets;
        BitmapFont f = a.fontSmall;
        float x = 12f;
        float y = Gdx.graphics.getHeight() - 20f;
        float lineH = 16f;

        f.setColor(0f, 1f, 0f, 0.9f);

        layout.setText(f, "FPS: " + fps);
        f.draw(batch, layout, x, y); y -= lineH;

        String screen = game.getScreen() != null ? game.getScreen().getClass().getSimpleName() : "null";
        layout.setText(f, "Screen: " + screen);
        f.draw(batch, layout, x, y); y -= lineH;

        // Уровень (если MyGame хранит — через save.getCheckpoint или уровень экрана)
        layout.setText(f, "CheckpointLevel: " + game.save.getCheckpoint());
        f.draw(batch, layout, x, y); y -= lineH;

        // Координаты игрока — пробуем достать из известных экранов
        String pos = playerPos(game);
        layout.setText(f, "Player: " + pos);
        f.draw(batch, layout, x, y); y -= lineH;

        // Бутылки (уровень 5)
        layout.setText(f, "Bottles: " + bottleCount(game));
        f.draw(batch, layout, x, y); y -= lineH;

        f.setColor(Color.WHITE);
    }

    private String playerPos(MyGame game) {
        Object screen = game.getScreen();
        if (screen == null) return "—";
        String name = screen.getClass().getSimpleName();
        try {
            switch (name) {
                case "HorrorBaseScreen":
                    return String.format("X=%.0f Y=%.0f",
                            getFloat(screen, "player.getCenterX()"),
                            getFloat(screen, "player.getCenterY()"));
                case "HorrorHouseScreen":
                    return String.format("X=%.0f Y=%.0f",
                            getFloat(screen, "player.getCenterX()"),
                            getFloat(screen, "player.getCenterY()"));
                case "GameScreen":
                    return String.format("X=%.0f Y=%.0f",
                            getFloat(screen, "player.getCenterX()"),
                            getFloat(screen, "player.getCenterY()"));
                case "MapScreen":
                    return String.format("X=%.0f Y=%.0f",
                            getFloat(screen, "ball.getX()"),
                            getFloat(screen, "ball.getY()"));
                default:
                    return "—";
            }
        } catch (Exception e) {
            return "? (" + e.getClass().getSimpleName() + ")";
        }
    }

    private String bottleCount(MyGame game) {
        Object screen = game.getScreen();
        if (screen == null) return "—";
        String name = screen.getClass().getSimpleName();
        try {
            if (name.equals("HorrorBaseScreen")) {
                int found = getInt(screen, "bottlesFound");
                int total = getInt(screen, "bottlesTotal");
                return found + " / " + total;
            }
            if (name.equals("HorrorHouseScreen")) {
                int base = getInt(screen, "baseBottlesFound");
                int inHouse = getInt(screen, "bottlesFoundInHouse");
                int total = getInt(screen, "bottlesTotal");
                return (base + inHouse) + " / " + total;
            }
        } catch (Exception ignored) {}
        return "—";
    }

    // Рефлексия для доступа к приватным полям экранов (только для отладки)
    private float getFloat(Object obj, String expr) throws Exception {
        // expr вида "player.getCenterX()" — вытаскиваем имя поля до точки
        String field = expr.split("\\.")[0];
        java.lang.reflect.Field f = obj.getClass().getDeclaredField(field);
        f.setAccessible(true);
        Object val = f.get(obj);
        if (val == null) return 0f;
        // Если это Player — вызываем getCenterX()
        if (val.getClass().getSimpleName().equals("Player")) {
            return (Float) val.getClass().getMethod("getCenterX").invoke(val);
        }
        if (val.getClass().getSimpleName().equals("YarnBall")) {
            return (Float) val.getClass().getMethod("getX").invoke(val);
        }
        return ((Number) val).floatValue();
    }

    private int getInt(Object obj, String name) throws Exception {
        java.lang.reflect.Field f = obj.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.getInt(obj);
    }
}
