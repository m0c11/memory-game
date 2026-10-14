package com.mygame.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Disposable;

/**
 * Все ресурсы игры.
 * Комнаты пока рисуются сгенерированными текстурами,
 * а карта и меню используют картинки из папки assets/map.
 */
public class Assets implements Disposable {

    // Сгенерированные текстуры (комнаты)
    public Texture player;
    public Texture floor;
    public Texture floorAlt;
    public Texture wall;
    public Texture wallTop;
    public Texture interactable;
    public Texture interactableUsed;
    public Texture doorLocked;
    public Texture doorOpen;
    public Texture pixel;

    // Картинки карты (assets/map/*.png, 1024x559)
    public Texture mapBackground;
    public Texture mapThread;
    public Texture mapOrb;
    /** Квадратная вырезка светящегося шара с мягким ореолом. */
    public TextureRegion orb;

    /** Круглая маска света: прозрачный центр, чёрный край (рисуется поверх карты). */
    public Texture light;
    /** Клубок ниток — герой карты. */
    public Texture yarnBall;

    // Шрифты
    public BitmapFont fontSmall;
    public BitmapFont fontMedium;
    public BitmapFont fontMenu;
    public BitmapFont fontLarge;

    public Assets() {
        player           = createTexture(24, 32, Color.valueOf("FFD34E"));
        floor            = createTexture(48, 48, Color.valueOf("32323B"));
        floorAlt         = createTexture(48, 48, Color.valueOf("2A2A32"));
        wall             = createTexture(48, 48, Color.valueOf("17171C"));
        wallTop          = createTexture(48, 6,  Color.valueOf("3A3A46"));
        interactable     = createTexture(22, 22, Color.valueOf("7FD7F6"));
        interactableUsed = createTexture(22, 22, Color.valueOf("45454F"));
        doorLocked       = createTexture(40, 96, Color.valueOf("33283F"));
        doorOpen         = createTexture(40, 96, Color.valueOf("FFD700"));
        pixel            = createTexture(1, 1, Color.WHITE);

        mapBackground = loadImage("map/background.png");
        mapThread     = loadImage("map/thread.png");
        mapOrb        = loadImage("map/orb.png");
        orb           = createOrbRegion(mapOrb);
        light         = createLightMask(256);
        yarnBall      = createYarnBall(64);

        fontSmall  = createFont(16);
        fontMedium = createFont(22);
        fontMenu   = createFont(30);
        fontLarge  = createFont(40);
    }

    private Texture createTexture(int w, int h, Color color) {
        Pixmap pixmap = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        pixmap.setColor(color);
        pixmap.fill();
        Texture tex = new Texture(pixmap);
        tex.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        pixmap.dispose();
        return tex;
    }

    private Texture loadImage(String path) {
        Texture tex = new Texture(Gdx.files.internal(path));
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return tex;
    }

    /**
     * Шар в orb.png стоит строго по центру картинки, а ореол занимает
     * примерно ±100 px. Вырезаем квадрат 300x300 вокруг центра
     * (размеры пересчитываются от ширины картинки 1024).
     */
    private TextureRegion createOrbRegion(Texture tex) {
        int half = Math.round(tex.getWidth() * 150f / 1024f);
        int cx = tex.getWidth() / 2;
        int cy = tex.getHeight() / 2;
        return new TextureRegion(tex, cx - half, cy - half, half * 2, half * 2);
    }

    /**
     * Маска света: центр полностью прозрачный, к краю плавно уходит в чёрный.
     * Рисуется поверх карты вокруг героя, всё остальное закрывается чёрными прямоугольниками.
     */
    private Texture createLightMask(int size) {
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setBlending(Pixmap.Blending.None);
        float c = (size - 1) / 2f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - c;
                float dy = y - c;
                float d = (float) Math.sqrt(dx * dx + dy * dy) / (size / 2f);
                float t = Math.max(0f, Math.min(1f, (d - 0.12f) / 0.88f));
                float a = t * t * (3f - 2f * t); // smoothstep
                pm.setColor(0f, 0f, 0f, a);
                pm.drawPixel(x, y);
            }
        }
        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return tex;
    }

    /** Простой клубок: золотой круг с «намотанными» нитками и бликом. */
    private Texture createYarnBall(int size) {
        Pixmap pm = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        pm.setColor(0f, 0f, 0f, 0f);
        pm.fill();

        int c = size / 2;
        int r = size / 2 - 3;

        pm.setColor(Color.valueOf("D9A441"));
        pm.fillCircle(c, c, r);

        pm.setColor(Color.valueOf("A8742A"));
        float[][] chords = {{20, 160}, {45, 195}, {75, 225}, {110, 265}, {140, 300}};
        for (float[] ch : chords) {
            float a1 = (float) Math.toRadians(ch[0]);
            float a2 = (float) Math.toRadians(ch[1]);
            int x1 = Math.round(c + (float) Math.cos(a1) * (r - 1));
            int y1 = Math.round(c + (float) Math.sin(a1) * (r - 1));
            int x2 = Math.round(c + (float) Math.cos(a2) * (r - 1));
            int y2 = Math.round(c + (float) Math.sin(a2) * (r - 1));
            pm.drawLine(x1, y1, x2, y2);
        }

        pm.setColor(Color.valueOf("F6D98A"));
        pm.fillCircle(c - size / 7, c - size / 7, size / 11);

        Texture tex = new Texture(pm);
        tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return tex;
    }

    private BitmapFont createFont(int size) {
        // Ищем системный шрифт с кириллицей. Пробуем стандартные пути для Linux/Mac/Windows.
        String[] candidates = {
                "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf",
                "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf",
                "/usr/share/fonts/TTF/DejaVuSans.ttf",
                "/usr/share/fonts/dejavu/DejaVuSans.ttf",
                "/System/Library/Fonts/Supplemental/Arial.ttf",
                "/Library/Fonts/Arial.ttf",
                "C:/Windows/Fonts/segoeui.ttf",
                "C:/Windows/Fonts/arial.ttf"
        };

        for (String path : candidates) {
            FileHandle handle = Gdx.files.absolute(path);
            if (!handle.exists()) continue;

            FreeTypeFontGenerator gen = new FreeTypeFontGenerator(handle);
            FreeTypeFontGenerator.FreeTypeFontParameter param =
                    new FreeTypeFontGenerator.FreeTypeFontParameter();
            param.size = size;
            param.characters = FreeTypeFontGenerator.DEFAULT_CHARS
                    + "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
                    + "абвгдеёжзийклмнопрстуфхцчшщъыьэюя"
                    + "←→↑↓";
            param.minFilter = Texture.TextureFilter.Linear;
            param.magFilter = Texture.TextureFilter.Linear;
            param.color = Color.WHITE;

            BitmapFont font = gen.generateFont(param);
            gen.dispose();
            return font;
        }

        Gdx.app.error("Assets",
                "Системный шрифт не найден — кириллица может не отрисоваться. " +
                        "Установи пакет fonts-dejavu или добавь .ttf вручную.");
        BitmapFont fallback = new BitmapFont();
        fallback.getData().setScale(size / 15f);
        return fallback;
    }

    @Override
    public void dispose() {
        player.dispose();
        floor.dispose();
        floorAlt.dispose();
        wall.dispose();
        wallTop.dispose();
        interactable.dispose();
        interactableUsed.dispose();
        doorLocked.dispose();
        doorOpen.dispose();
        pixel.dispose();
        mapBackground.dispose();
        mapThread.dispose();
        mapOrb.dispose();
        light.dispose();
        yarnBall.dispose();
        fontSmall.dispose();
        fontMedium.dispose();
        fontMenu.dispose();
        fontLarge.dispose();
    }
}
