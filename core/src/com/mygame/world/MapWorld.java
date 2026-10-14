package com.mygame.world;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;

/**
 * Мир карты. Проходимые места строятся автоматически из картинки нити:
 * всё, что достаточно яркое (сама нить), расширяется на несколько пикселей,
 * получаются «коридоры» шириной в нить с запасом. Руками размечать ничего не нужно:
 * заменишь thread.png на новую («i love») — коридоры перестроятся сами.
 *
 * Размер мира в игровых единицах задаётся шириной; высота считается по пропорциям картинки.
 */
public class MapWorld {

    public final float width;
    public final float height;

    private final int maskW;
    private final int maskH;
    private final boolean[] walkable;

    // Закрытые узлы (ещё не открытые уровни): вокруг них пройти нельзя
    private float[] lockX = new float[0];
    private float[] lockY = new float[0];
    private float lockRadius = 48f;

    /**
     * @param image       картинка нити (светлая нить на чёрном)
     * @param worldWidth  ширина мира в игровых единицах
     * @param dilateWorld на сколько единиц расширять нить в каждую сторону (ширина коридора)
     * @param threshold   значение синего канала 0..255, начиная с которого пиксель считается нитью
     */
    public MapWorld(FileHandle image, float worldWidth, float dilateWorld, int threshold) {
        Pixmap pm = new Pixmap(image);
        int tw = pm.getWidth();
        int th = pm.getHeight();

        this.width = worldWidth;
        this.height = worldWidth * th / tw;

        // Маска считается не крупнее 1536 px по ширине — этого хватает
        this.maskW = Math.min(tw, 1536);
        this.maskH = Math.max(1, Math.round(maskW * (float) th / tw));

        boolean[] thin = new boolean[maskW * maskH];
        for (int my = 0; my < maskH; my++) {
            int sy = Math.min(th - 1, (int) ((my + 0.5f) * th / maskH));
            for (int mx = 0; mx < maskW; mx++) {
                int sx = Math.min(tw - 1, (int) ((mx + 0.5f) * tw / maskW));
                int p = pm.getPixel(sx, sy);
                // Берём синий канал: нить светлая (в нём яркая), а тёплое
                // золотое свечение вокруг неё почти не содержит синего.
                int b = (p >>> 8) & 0xff;
                thin[my * maskW + mx] = b >= threshold;
            }
        }
        pm.dispose();

        float worldPerPixel = width / maskW;
        int radius = Math.max(1, Math.round(dilateWorld / worldPerPixel));
        this.walkable = dilate(thin, maskW, maskH, radius);
    }

    /** Квадратное расширение маски на r пикселей (через префиксные суммы, быстро). */
    private static boolean[] dilate(boolean[] src, int w, int h, int r) {
        boolean[] tmp = new boolean[w * h];
        boolean[] out = new boolean[w * h];
        int[] pref = new int[Math.max(w, h) + 1];

        for (int y = 0; y < h; y++) {
            pref[0] = 0;
            for (int x = 0; x < w; x++) {
                pref[x + 1] = pref[x] + (src[y * w + x] ? 1 : 0);
            }
            for (int x = 0; x < w; x++) {
                int a = Math.max(0, x - r);
                int b = Math.min(w - 1, x + r);
                tmp[y * w + x] = pref[b + 1] - pref[a] > 0;
            }
        }
        for (int x = 0; x < w; x++) {
            pref[0] = 0;
            for (int y = 0; y < h; y++) {
                pref[y + 1] = pref[y] + (tmp[y * w + x] ? 1 : 0);
            }
            for (int y = 0; y < h; y++) {
                int a = Math.max(0, y - r);
                int b = Math.min(h - 1, y + r);
                out[y * w + x] = pref[b + 1] - pref[a] > 0;
            }
        }
        return out;
    }

    /**
     * Делает круг вокруг точки проходимым в любом случае.
     * Нужно для узлов: так до них точно можно дойти, а точка над «i»
     * (отдельный островок нити) получает достаточно места для клубка.
     */
    public void carveDisc(float x, float y, float radius) {
        float worldPerPixelX = width / maskW;
        float worldPerPixelY = height / maskH;
        int cx = (int) (x / width * maskW);
        int cy = (int) ((height - y) / height * maskH);
        int rx = (int) Math.ceil(radius / worldPerPixelX);
        int ry = (int) Math.ceil(radius / worldPerPixelY);
        for (int dy = -ry; dy <= ry; dy++) {
            for (int dx = -rx; dx <= rx; dx++) {
                float wx = dx * worldPerPixelX;
                float wy = dy * worldPerPixelY;
                if (wx * wx + wy * wy > radius * radius) continue;
                int mx = cx + dx;
                int my = cy + dy;
                if (mx >= 0 && my >= 0 && mx < maskW && my < maskH) {
                    walkable[my * maskW + mx] = true;
                }
            }
        }
    }

    /** Задаёт закрытые узлы: вокруг каждого образуется непроходимый круг. */
    public void setLockedNodes(float[] xs, float[] ys, float radius) {
        this.lockX = xs;
        this.lockY = ys;
        this.lockRadius = radius;
    }

    /** Можно ли стоять в точке мира (x, y). Ось Y направлена вверх. */
    public boolean isWalkable(float x, float y) {
        if (x < 0f || y < 0f || x >= width || y >= height) return false;

        int mx = (int) (x / width * maskW);
        int my = (int) ((height - y) / height * maskH);
        if (mx < 0 || my < 0 || mx >= maskW || my >= maskH) return false;
        if (!walkable[my * maskW + mx]) return false;

        float r2 = lockRadius * lockRadius;
        for (int i = 0; i < lockX.length; i++) {
            float dx = lockX[i] - x;
            float dy = lockY[i] - y;
            if (dx * dx + dy * dy < r2) return false;
        }
        return true;
    }
}
