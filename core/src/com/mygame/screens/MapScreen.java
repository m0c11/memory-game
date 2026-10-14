package com.mygame.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygame.MyGame;
import com.mygame.assets.Assets;
import com.mygame.entities.YarnBall;
import com.mygame.levels.LevelInfo;
import com.mygame.levels.Levels;
import com.mygame.world.MapWorld;
import com.mygame.world.Trail;

import java.util.List;

/**
 * Карта «Нить воспоминаний» в духе обзорной карты Cuphead.
 *
 *  - Большой мир, камера следует за клубком.
 *  - Темнота, видно только круг света вокруг клубка.
 *  - Клубок ходит только по линиям нити и оставляет за собой золотой след.
 *  - Узлы на нити — уровни. Подойди к открытому узлу и нажми E.
 *  - Закрытые узлы блокируют путь, пока не пройден предыдущий уровень.
 *
 * Управление: WASD / стрелки — идти, E — войти в уровень, Esc — меню,
 * F3 — отладка (без темноты, номера узлов, координаты мыши).
 */
public class MapScreen extends ScreenAdapter {

    // Размер окна камеры (в игровых единицах) — такой же, как у остальных экранов
    private static final float VIEW_W = 1248f;
    private static final float VIEW_H = 720f;

    // Мир
    private static final float WORLD_W = 4200f;       // ширина мира; высота — по пропорциям картинки
    private static final float CORRIDOR_PAD = 22f;     // на сколько расширяем нить (ширина коридора)
    private static final int   THREAD_THRESHOLD = 100; // значение синего канала, с которого пиксель считается нитью
    private static final float LOCK_RADIUS = 52f;      // непроходимый круг вокруг закрытого узла
    private static final float NODE_CARVE_RADIUS = 46f; // вокруг каждого узла всегда можно стоять

    // Размер картинки, в которой заданы refX/refY узлов (см. Levels)
    private static final float REF_W = 1024f;
    private static final float REF_H = 547f;

    // Свет и взаимодействие
    private static final float LIGHT_RADIUS = 340f;
    private static final float ENTER_RADIUS = 80f;
    private static final float FOCUS_RADIUS = 150f;
    private static final float AWAKEN_TIME = 3.5f;

    private final MyGame game;
    private final int justCompleted;
    private final boolean awaken;

    private OrthographicCamera camera;
    private OrthographicCamera hudCamera;
    private FitViewport viewport;
    private final GlyphLayout layout = new GlyphLayout();
    private final Vector2 mouse = new Vector2();

    private MapWorld world;
    private Trail trail;
    private YarnBall ball;
    private TextureRegion pixelRegion;
    private TextureRegion yarnRegion;

    private int completed;
    private int unlockedMax;
    private final float[] lit = new float[Levels.COUNT];

    private int focus = -1;
    private float frameAlpha = 0f;
    private float time = 0f;
    private float fade = 0f;
    private float awakenTimer = 0f;
    private boolean debug = false;

    public MapScreen(MyGame game) {
        this(game, -1, false);
    }

    /** После прохождения уровня: justCompleted — номер пройденного уровня. */
    public MapScreen(MyGame game, int justCompleted) {
        this(game, justCompleted, false);
    }

    /** Карта сразу после вступительной сценки (эффект «пробуждения»). */
    public static MapScreen afterIntro(MyGame game) {
        return new MapScreen(game, -1, true);
    }

    private MapScreen(MyGame game, int justCompleted, boolean awaken) {
        this.game = game;
        this.justCompleted = justCompleted;
        this.awaken = awaken;
    }

    // ------------------------------------------------------------------

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(VIEW_W, VIEW_H, camera);
        hudCamera = new OrthographicCamera(VIEW_W, VIEW_H);
        hudCamera.position.set(VIEW_W / 2f, VIEW_H / 2f, 0f);
        hudCamera.update();

        pixelRegion = new TextureRegion(game.assets.pixel);
        yarnRegion = new TextureRegion(game.assets.yarnBall);

        completed = game.save.getCompleted();
        unlockedMax = Math.min(completed, Levels.COUNT - 1);

        world = new MapWorld(Gdx.files.internal("map/thread.png"),
                             WORLD_W, CORRIDOR_PAD, THREAD_THRESHOLD);
        for (int i = 0; i < Levels.COUNT; i++) {
            world.carveDisc(nodeX(i), nodeY(i), NODE_CARVE_RADIUS);
        }
        lockClosedNodes();

        trail = new Trail(world.width, world.height);
        trail.load(game.save.getTrail());

        ball = new YarnBall();
        if (justCompleted == 0) {
            // «Мост»: точка над «i» — отдельный островок. После первого уровня нить
            // сама переносит клубок на букву, ходить через разрыв не нужно.
            ball.setPosition(nodeX(1), nodeY(1));
        } else if (game.save.hasBallPosition()
            && world.isWalkable(game.save.getBallX(), game.save.getBallY())) {
            ball.setPosition(game.save.getBallX(), game.save.getBallY());
        } else {
            ball.setPosition(nodeX(unlockedMax), nodeY(unlockedMax));
        }

        for (int i = 0; i < Levels.COUNT; i++) {
            lit[i] = litTarget(i);
        }
        // Только что пройденный узел «загорается» плавно
        if (justCompleted >= 0 && justCompleted < Levels.COUNT) {
            lit[justCompleted] = 0.6f;
        }
        if (justCompleted >= 0 && completed < Levels.COUNT) {
            lit[completed] = 0.12f;
        }

        camera.position.set(clampCamX(ball.getX()), clampCamY(ball.getY()), 0f);
        camera.update();
    }

    private void lockClosedNodes() {
        int count = Levels.COUNT - 1 - unlockedMax;
        float[] xs = new float[count];
        float[] ys = new float[count];
        for (int i = 0; i < count; i++) {
            int node = unlockedMax + 1 + i;
            xs[i] = nodeX(node);
            ys[i] = nodeY(node);
        }
        world.setLockedNodes(xs, ys, LOCK_RADIUS);
    }

    private float litTarget(int i) {
        if (i < completed) return 1f;
        if (i == completed) return 0.6f;
        return 0.12f;
    }

    private float nodeX(int i) {
        return Levels.ALL[i].refX / REF_W * world.width;
    }

    private float nodeY(int i) {
        return world.height - Levels.ALL[i].refY / REF_H * world.height;
    }

    private float clampCamX(float x) {
        return MathUtils.clamp(x, VIEW_W / 2f, world.width - VIEW_W / 2f);
    }

    private float clampCamY(float y) {
        return MathUtils.clamp(y, VIEW_H / 2f, world.height - VIEW_H / 2f);
    }

    // ------------------------------------------------------------------

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1f / 30f);
        time += delta;
        fade = Math.min(1f, fade + delta * 1.6f);
        frameAlpha = Math.min(1f, frameAlpha + delta * 2f);
        awakenTimer += delta;

        if (handleInput()) return;
        update(delta);
        draw();
    }

    private boolean controlsEnabled() {
        return !awaken || awakenTimer >= AWAKEN_TIME;
    }

    /** @return true, если экран был сменён. */
    private boolean handleInput() {
        mouse.set(Gdx.input.getX(), Gdx.input.getY());
        viewport.unproject(mouse);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MainMenuScreen(game));
            return true;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
            debug = !debug;
        }
        if (debug && Gdx.input.justTouched()) {
            System.out.printf("Позиция на картинке 1024x559: refX=%.0f, refY=%.0f%n",
                mouse.x / world.width * REF_W,
                (world.height - mouse.y) / world.height * REF_H);
        }

        if (controlsEnabled() && Gdx.input.isKeyJustPressed(Input.Keys.E)
            && focus >= 0 && distanceToNode(focus) <= ENTER_RADIUS) {
            game.setScreen(new GameScreen(game, focus));
            return true;
        }
        return false;
    }

    private float distanceToNode(int i) {
        float dx = nodeX(i) - ball.getX();
        float dy = nodeY(i) - ball.getY();
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    private void update(float delta) {
        ball.update(delta, world, trail, controlsEnabled());

        // Камера плавно следует за клубком
        float k = Math.min(1f, delta * 4f);
        camera.position.x += (clampCamX(ball.getX()) - camera.position.x) * k;
        camera.position.y += (clampCamY(ball.getY()) - camera.position.y) * k;
        camera.update();

        float lk = Math.min(1f, delta * 2.2f);
        for (int i = 0; i < Levels.COUNT; i++) {
            lit[i] += (litTarget(i) - lit[i]) * lk;
        }

        // Ближайший открытый узел рядом с клубком
        int best = -1;
        float bestDist = FOCUS_RADIUS;
        for (int i = 0; i <= unlockedMax; i++) {
            float d = distanceToNode(i);
            if (d < bestDist) {
                bestDist = d;
                best = i;
            }
        }
        if (best != focus) {
            focus = best;
            frameAlpha = 0f;
        }
    }

    // ------------------------------------------------------------------

    private void draw() {
        Assets a = game.assets;

        Gdx.gl.glClearColor(0.01f, 0.01f, 0.015f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        // Тёмная «пещера» (фон пока минимальный)
        game.batch.setColor(0.32f, 0.32f, 0.38f, 1f);
        game.batch.draw(a.mapBackground, 0, 0, world.width, world.height);

        // Нить, след, узлы и свечение — аддитивно
        game.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE);
        game.batch.setColor(0.75f, 0.75f, 0.82f, 0.7f);
        game.batch.draw(a.mapThread, 0, 0, world.width, world.height);

        drawTrail();
        drawNodes();
        ball.drawGlow(game.batch, a, time);

        game.batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        game.batch.setColor(Color.WHITE);

        ball.drawBody(game.batch, yarnRegion);

        if (debug) {
            drawDebugWorld();
        } else {
            drawDarkness();
        }

        // Интерфейс — в экранных координатах
        game.batch.setProjectionMatrix(hudCamera.combined);
        drawHud();
        drawOverlays();

        game.batch.end();
    }

    // ---------- нить за клубком ----------

    private void drawTrail() {
        List<Vector2> pts = trail.getPoints();
        float cx = camera.position.x;
        float cy = camera.position.y;
        float x0 = cx - VIEW_W / 2f - 40f;
        float x1 = cx + VIEW_W / 2f + 40f;
        float y0 = cy - VIEW_H / 2f - 40f;
        float y1 = cy + VIEW_H / 2f + 40f;
        float link2 = Trail.MAX_LINK * Trail.MAX_LINK;

        for (int pass = 0; pass < 2; pass++) {
            float width = pass == 0 ? 14f : 4.5f;
            if (pass == 0) game.batch.setColor(1f, 0.75f, 0.3f, 0.2f);
            else           game.batch.setColor(1f, 0.92f, 0.62f, 0.95f);

            for (int i = 1; i < pts.size(); i++) {
                Vector2 a = pts.get(i - 1);
                Vector2 b = pts.get(i);
                if ((a.x < x0 && b.x < x0) || (a.x > x1 && b.x > x1)
                    || (a.y < y0 && b.y < y0) || (a.y > y1 && b.y > y1)) continue;
                if (a.dst2(b) > link2) continue;
                segment(a.x, a.y, b.x, b.y, width);
            }

            // Нить тянется от последней точки к самому клубку
            if (!pts.isEmpty()) {
                Vector2 last = pts.get(pts.size() - 1);
                if (last.dst2(ball.getX(), ball.getY()) <= link2) {
                    segment(last.x, last.y, ball.getX(), ball.getY(), width);
                }
            }
        }
    }

    private void segment(float x1, float y1, float x2, float y2, float width) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.01f) return;
        float angle = MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees;
        game.batch.draw(pixelRegion,
            x1, y1 - width / 2f,
            0f, width / 2f,
            len, width,
            1f, 1f, angle);
    }

    // ---------- узлы ----------

    private void drawNodes() {
        Assets a = game.assets;
        for (int i = 0; i < Levels.COUNT; i++) {
            LevelInfo info = Levels.ALL[i];
            float x = nodeX(i);
            float y = nodeY(i);
            float l = lit[i];

            Texture[] set = info.chapter ? a.nodeChapter : a.nodeKey;
            float display = info.chapter ? 150f : 124f;
            float flicker = 0.93f + 0.07f * (float) Math.sin(time * 3f + i * 1.7f);

            float scale = 1f;
            if (i == focus) {
                scale = 1.12f + 0.05f * (float) Math.sin(time * 3f);
            }
            float size = display * scale;

            // Мягкое золотое свечение под открытыми узлами
            if (l > 0.3f) {
                float glow = size * 1.7f;
                game.batch.setColor(1f, 1f, 1f, 0.55f * l * flicker);
                game.batch.draw(a.orb, x - glow / 2f, y - glow / 2f, glow, glow);
            }

            // Три состояния плавно перетекают друг в друга: закрыт -> доступен -> пройден
            float t1 = MathUtils.clamp((l - 0.12f) / 0.3f, 0f, 1f);
            float t2 = MathUtils.clamp((l - 0.6f) / 0.4f, 0f, 1f);
            float aLocked = 1f - t1;
            float aAvail = t1 * (1f - t2);
            float aDone = t2;

            drawNodeSprite(set[0], x, y, size, aLocked);
            drawNodeSprite(set[1], x, y, size, aAvail * flicker);
            drawNodeSprite(set[2], x, y, size, aDone * flicker);
        }
    }

    private void drawNodeSprite(Texture tex, float x, float y, float size, float alpha) {
        if (alpha <= 0.01f) return;
        game.batch.setColor(1f, 1f, 1f, Math.min(1f, alpha));
        game.batch.draw(tex, x - size / 2f, y - size / 2f, size, size);
    }

    // ---------- темнота ----------

    /**
     * Поверх мира рисуется круг света (прозрачный центр, чёрный край),
     * а всё остальное закрывается чёрными прямоугольниками.
     */
    private void drawDarkness() {
        float scale;
        if (awaken) {
            float t = MathUtils.clamp(awakenTimer / AWAKEN_TIME, 0f, 1f);
            scale = t * t * (3f - 2f * t);
        } else {
            scale = 1f;
        }
        float flicker = 1f + 0.02f * (float) Math.sin(time * 5f) + 0.015f * (float) Math.sin(time * 9.3f);
        float r = Math.max(6f, LIGHT_RADIUS * scale * flicker);

        float bx = ball.getX();
        float by = ball.getY();

        float vx0 = camera.position.x - VIEW_W / 2f - 8f;
        float vx1 = camera.position.x + VIEW_W / 2f + 8f;
        float vy0 = camera.position.y - VIEW_H / 2f - 8f;
        float vy1 = camera.position.y + VIEW_H / 2f + 8f;

        game.batch.setColor(Color.WHITE);
        game.batch.draw(game.assets.light, bx - r, by - r, r * 2f, r * 2f);

        game.batch.setColor(0f, 0f, 0f, 1f);
        fillBlack(vx0, vy0, vx1, by - r);          // снизу
        fillBlack(vx0, by + r, vx1, vy1);          // сверху
        fillBlack(vx0, by - r, bx - r, by + r);    // слева
        fillBlack(bx + r, by - r, vx1, by + r);    // справа
        game.batch.setColor(Color.WHITE);
    }

    private void fillBlack(float x0, float y0, float x1, float y1) {
        if (x1 <= x0 || y1 <= y0) return;
        game.batch.draw(game.assets.pixel, x0, y0, x1 - x0, y1 - y0);
    }

    // ---------- интерфейс ----------

    private void drawHud() {
        Assets a = game.assets;

        if (controlsEnabled()) {
            if (focus >= 0) {
                LevelInfo info = Levels.ALL[focus];

                BitmapFont title = a.fontMenu;
                title.setColor(1f, 0.93f, 0.78f, frameAlpha);
                layout.setText(title, info.title);
                title.draw(game.batch, layout, (VIEW_W - layout.width) / 2f, VIEW_H - 36f);

                BitmapFont sub = a.fontSmall;
                sub.setColor(0.78f, 0.78f, 0.84f, frameAlpha * 0.9f);
                layout.setText(sub, info.subtitle);
                sub.draw(game.batch, layout, (VIEW_W - layout.width) / 2f, VIEW_H - 78f);

                if (distanceToNode(focus) <= ENTER_RADIUS) {
                    float blink = 0.55f + 0.45f * (float) Math.abs(Math.sin(time * 2.5f));
                    BitmapFont f = a.fontMedium;
                    f.setColor(1f, 0.9f, 0.55f, blink);
                    layout.setText(f, "[ E ]  войти");
                    f.draw(game.batch, layout, (VIEW_W - layout.width) / 2f, 120f);
                }
            }

            BitmapFont s = a.fontSmall;
            s.setColor(0.7f, 0.7f, 0.76f, 0.9f);
            s.draw(game.batch, "ВОСПОМИНАНИЯ  " + Math.min(completed, Levels.COUNT)
                + " / " + Levels.COUNT, 36f, VIEW_H - 30f);

            s.setColor(0.45f, 0.45f, 0.5f, 0.9f);
            layout.setText(s, "WASD — идти      E — войти      Esc — меню");
            s.draw(game.batch, layout, (VIEW_W - layout.width) / 2f, 30f);
        }

        if (debug) {
            BitmapFont s = a.fontSmall;
            s.setColor(0.4f, 1f, 0.6f, 1f);
            s.draw(game.batch, String.format("refX=%.0f  refY=%.0f  (клик — вывод в консоль)",
                mouse.x / world.width * REF_W,
                (world.height - mouse.y) / world.height * REF_H), 36f, VIEW_H - 60f);
        }

        a.fontMenu.setColor(Color.WHITE);
        a.fontMedium.setColor(Color.WHITE);
        a.fontSmall.setColor(Color.WHITE);
    }

    /** Вспышка при «пробуждении» и плавное появление экрана. */
    private void drawOverlays() {
        Assets a = game.assets;

        if (awaken && awakenTimer < 1.4f) {
            game.batch.setColor(1f, 1f, 1f, 1f - awakenTimer / 1.4f);
            game.batch.draw(a.pixel, 0, 0, VIEW_W, VIEW_H);
        } else if (!awaken && fade < 1f) {
            game.batch.setColor(0f, 0f, 0f, 1f - fade);
            game.batch.draw(a.pixel, 0, 0, VIEW_W, VIEW_H);
        }
        game.batch.setColor(Color.WHITE);
    }

    /** В режиме отладки темноты нет, поверх рисуются номера узлов. */
    private void drawDebugWorld() {
        BitmapFont s = game.assets.fontSmall;
        s.setColor(0.4f, 1f, 0.6f, 1f);
        for (int i = 0; i < Levels.COUNT; i++) {
            s.draw(game.batch, String.valueOf(i + 1), nodeX(i) + 20f, nodeY(i) + 40f);
        }
        s.setColor(Color.WHITE);
    }

    // ------------------------------------------------------------------

    private void saveState() {
        if (ball == null || trail == null) return;
        game.save.saveMapState(ball.getX(), ball.getY(), trail.serialize());
    }

    @Override
    public void hide() {
        saveState();
    }

    @Override
    public void dispose() {
        saveState();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }
}
