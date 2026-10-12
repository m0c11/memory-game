package com.mygame;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.mygame.assets.Assets;
import com.mygame.screens.MainMenuScreen;

/**
 * Точка входа всей игры.
 * Держит общие ресурсы (SpriteBatch, Assets) и умеет переключать экраны.
 */
public class MyGame extends Game {

    public SpriteBatch batch;
    public Assets assets;

    @Override
    public void create() {
        batch = new SpriteBatch();
        assets = new Assets();
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