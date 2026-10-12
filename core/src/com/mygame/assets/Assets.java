package com.mygame.assets;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.Disposable;

/**
 * Все ресурсы игры. Пока всё генерируется кодом — ассеты не нужны.
 * Когда дойдёшь до этапа визуала, просто замени createTexture(...) на new Texture("path.png").
 */
public class Assets implements Disposable {

    // Текстуры
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

    // Шрифты
    public BitmapFont fontSmall;
    public BitmapFont fontMedium;
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

        fontSmall  = createFont(16);
        fontMedium = createFont(22);
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
        fontSmall.dispose();
        fontMedium.dispose();
        fontLarge.dispose();
    }
}