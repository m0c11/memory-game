package com.mygame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mygame.assets.Assets;
import com.mygame.save.SaveManager;
import com.mygame.screens.MainMenuScreen;

/**
 * Точка входа всей игры.
 * Держит общие ресурсы (SpriteBatch, Assets, SaveManager) и переключает экраны.
 */
public class MyGame extends Game {

    public SpriteBatch batch;
    public Assets assets;
    public SaveManager save;

    @Override
    public void create() {
        batch = new SpriteBatch();
        assets = new Assets();
        save = new SaveManager();
        setScreen(new MainMenuScreen(this));
    }

    @Override
    public void dispose() {
        if (getScreen() != null) {
            getScreen().dispose();
        }
        batch.dispose();
        assets.dispose();
    }
}
