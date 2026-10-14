package com.mygame.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.mygame.MyGame;
import com.mygame.assets.Assets;
import com.mygame.mechanics.RhythmChart;

import java.util.ArrayList;
import java.util.List;

/**
 * БОСС «Танец» — ритм-игра в духе Piano Tiles.
 *
 * По четырём дорожкам сверху вниз съезжают стрелки. Когда стрелка доходит
 * до линии внизу, нужно нажать такую же клавишу в такт музыке. Промахи
 * отнимают силы (полоска слева). Когда музыка заканчивается — босс пройден.
 *
 * Управление: ← ↓ ↑ → (или W A S D). Esc — пропустить.
 */
public class BossScreen extends ScreenAdapter {

    private static final float W = 1248f;
    private static final float H = 720f;

    // --- геометрия дорожек ---
    private static final int LANES = RhythmChart.LANES;
    private static final float LANE_W = 116f;
    private static final float GAP = 12f;
    private static final float TOTAL_W = LANES * LANE_W + (LANES - 1) * GAP;
    private static final float START_X = (W - TOTAL_W) / 2f;

    private static final float HIT_Y = 128f;
    private static final float SPAWN_Y = H + 30f;
    private static final float LEAD = 1.35f;
    private static final float FALL_SPEED = (SPAWN_Y - HIT_Y) / LEAD;

    // --- окна попадания (сек) ---
    private static final float PERFECT = 0.075f;
    private static final float GOOD = 0.14f;
    private static final float MISS_WINDOW = 0.24f;

    private static final float LEAD_IN = 2.2f;
    private static final float VOLUME = 0.85f;

    private static final String[] GLYPH = {"←", "↓", "↑", "→"};
    private static final String[] KEY_HINT = {"← / A", "↓ / S", "↑ / W", "→ / D"};
    private static final Color[] LANE_COLOR = {
        Color.valueOf("59D9FF"),
        Color.valueOf("6BFF8C"),
        Color.valueOf("FF8CD9"),
        Color.valueOf("FFB84D"),
    };

    private enum State { INTRO, PLAY, RESULT }

    private final MyGame game;
    private final int levelIndex;

    private OrthographicCamera camera;
    private FitViewport viewport;
    private final GlyphLayout layout = new GlyphLayout();

    private State state = State.INTRO;
    private RhythmChart chart;
    private float songTime = 0f;
    private float leadIn = LEAD_IN;

    // счёт и здоровье
    private int score = 0;
    private int combo = 0;
    private int maxCombo = 0;
    private int perfects = 0;
    private int goods = 0;
    private int oks = 0;
    private int misses = 0;
    private float health = 100f;
    private static final float MAX_HEALTH = 100f;
    private boolean failed = false;

    private float time = 0f;
    private float resultTimer = 0f;
    private final float[] laneFlash = new float[LANES];

    private final List<Popup> popups = new ArrayList<>();

    private static class Popup {
        String text;
        float life;
        Color color;
        float x;
        Popup(String t, Color c, float x) { text = t; color = c; this.x = x; life = 0.7f; }
    }

    public BossScreen(MyGame game, int levelIndex) {
        this.game = game;
        this.levelIndex = levelIndex;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(W, H, camera);
        camera.position.set(W / 2f, H / 2f, 0f);
        camera.update();
        chart = RhythmChart.boss1();
    }

    // ------------------------------------------------------------------

    @Override
    public void render(float delta) {
        delta = Math.min(delta, 1f / 30f);
        time += delta;

        handleInput();
        update(delta);
        draw();
    }

    private void handleInput() {
        if (state == State.INTRO) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
                || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
                begin();
            }
            return;
        }

        if (state == State.PLAY) {
            if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                completeAndExit();
                return;
            }
            for (int lane = 0; lane < LANES; lane++) {
                if (isLanePressed(lane)) tryHit(lane);
            }
            return;
        }

        // RESULT
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            completeAndExit();
        } else if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)
            || Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (failed) restart();
            else completeAndExit();
        }
    }

    private boolean isLanePressed(int lane) {
        switch (lane) {
            case 0: return Gdx.input.isKeyJustPressed(Input.Keys.LEFT)
                        || Gdx.input.isKeyJustPressed(Input.Keys.A);
            case 1: return Gdx.input.isKeyJustPressed(Input.Keys.DOWN)
                        || Gdx.input.isKeyJustPressed(Input.Keys.S);
            case 2: return Gdx.input.isKeyJustPressed(Input.Keys.UP)
                        || Gdx.input.isKeyJustPressed(Input.Keys.W);
            case 3: return Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)
                        || Gdx.input.isKeyJustPressed(Input.Keys.D);
            default: return false;
        }
    }

    private void begin() {
        state = State.PLAY;
        leadIn = LEAD_IN;
        songTime = (float) Math.max(0.0, RhythmChart.START - LEAD_IN);
        Music music = game.assets.bossMusic;
        if (music != null) {
            music.setVolume(VOLUME);
            music.setPosition(songTime);
            music.play();
        }
    }

    private void restart() {
        state = State.PLAY;
        failed = false;
        score = 0;
        combo = 0;
        maxCombo = 0;
        perfects = goods = oks = misses = 0;
        health = MAX_HEALTH;
        popups.clear();
        chart = RhythmChart.boss1();
        leadIn = LEAD_IN;
        songTime = (float) Math.max(0.0, RhythmChart.START - LEAD_IN);
        Music music = game.assets.bossMusic;
        if (music != null) {
            music.setPosition(songTime);
            music.play();
        }
    }

    // ------------------------------------------------------------------

    private void update(float delta) {
        for (int i = 0; i < LANES; i++) {
            if (laneFlash[i] > 0f) laneFlash[i] -= delta;
        }
        for (int i = popups.size() - 1; i >= 0; i--) {
            popups.get(i).life -= delta;
            if (popups.get(i).life <= 0f) popups.remove(i);
        }

        if (state != State.PLAY) {
            if (state == State.RESULT) resultTimer += delta;
            return;
        }

        // Время партии: если музыка играет — берём её позицию (точная синхронизация),
        // иначе считаем сами (на случай отсутствия файла).
        Music music = game.assets.bossMusic;
        if (music != null && music.isPlaying()) {
            float mp = music.getPosition();
            if (mp > 0.05f) songTime = mp;
            else songTime += delta;
        } else {
            songTime += delta;
        }

        if (leadIn > 0f) leadIn -= delta;

        // Промахи: ноты, уехавшие за линию без нажатия.
        for (RhythmChart.Note n : chart.notes) {
            if (!n.judged && songTime - n.time > MISS_WINDOW) {
                n.judged = true;
                onMiss();
            }
        }

        if (health <= 0f && !failed) {
            endResult(true);
            return;
        }
        if (songTime >= chart.endTime + 1.6f) {
            endResult(false);
        }
    }

    private void tryHit(int lane) {
        RhythmChart.Note best = null;
        float bestDt = Float.MAX_VALUE;
        for (RhythmChart.Note n : chart.notes) {
            if (n.judged || n.lane != lane) continue;
            float dt = Math.abs(songTime - n.time);
            if (dt <= MISS_WINDOW && dt < bestDt) {
                bestDt = dt;
                best = n;
            }
        }
        if (best == null) {
            // промах по пустой дорожке не наказываем — игра добрая
            laneFlash[lane] = 0.12f;
            return;
        }

        best.judged = true;
        best.hit = true;
        best.hitTime = songTime;
        laneFlash[lane] = 0.22f;

        float cx = laneCenter(lane);
        if (bestDt <= PERFECT) {
            score += 300;
            perfects++;
            health = Math.min(MAX_HEALTH, health + 1.5f);
            spawn("Идеально!", Color.valueOf("8CF7FF"), cx);
        } else if (bestDt <= GOOD) {
            score += 150;
            goods++;
            health = Math.min(MAX_HEALTH, health + 1.0f);
            spawn("Хорошо", Color.valueOf("A6FF7A"), cx);
        } else {
            score += 50;
            oks++;
            health = Math.min(MAX_HEALTH, health + 0.3f);
            spawn("Почти", Color.valueOf("FFE07A"), cx);
        }
        combo++;
        if (combo > maxCombo) maxCombo = combo;
        play(game.assets.sfxChime, 0.45f);
    }

    private void onMiss() {
        misses++;
        combo = 0;
        health = Math.max(0f, health - 6f);
        spawn("Мимо", Color.valueOf("FF6B6B"), W / 2f);
        play(game.assets.sfxThud, 0.6f);
    }

    private void spawn(String text, Color color, float x) {
        popups.add(new Popup(text, color, x));
    }

    private void endResult(boolean failed) {
        this.failed = failed;
        state = State.RESULT;
        resultTimer = 0f;
        Music music = game.assets.bossMusic;
        if (music != null) music.stop();
    }

    private void completeAndExit() {
        Music music = game.assets.bossMusic;
        if (music != null) music.stop();
        game.save.completeLevel(levelIndex);
        if (levelIndex >= com.mygame.levels.Levels.COUNT - 1) {
            game.setScreen(new EndScreen(game));
        } else {
            game.setScreen(new MapScreen(game, levelIndex));
        }
    }

    private void play(com.badlogic.gdx.audio.Sound sound, float volume) {
        if (sound != null) sound.play(volume);
    }

    // ------------------------------------------------------------------

    private float laneCenter(int lane) {
        return START_X + lane * (LANE_W + GAP) + LANE_W / 2f;
    }

    private float laneX(int lane) {
        return START_X + lane * (LANE_W + GAP);
    }

    private float noteY(float noteTime) {
        return HIT_Y + (noteTime - songTime) * FALL_SPEED;
    }

    private void draw() {
        Gdx.gl.glClearColor(0.03f, 0.02f, 0.05f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        viewport.apply();
        camera.update();
        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();

        drawBackground();
        if (state != State.INTRO) drawLanesAndNotes();
        drawHud();

        if (state == State.INTRO) {
            drawIntro();
        } else if (state == State.RESULT) {
            drawResult();
        }

        game.batch.end();
    }

    private void drawBackground() {
        Assets a = game.assets;
        // мягкое затемнение сверху/снизу
        game.batch.setColor(0.10f, 0.07f, 0.16f, 1f);
        game.batch.draw(a.pixel, 0, 0, W, H);

        // пульс в такт
        if (state == State.PLAY) {
            float pulse = beatPulse() * 0.14f;
            game.batch.setColor(0.35f, 0.20f, 0.55f, pulse);
            game.batch.draw(a.pixel, 0, 0, W, H);
        }
        game.batch.setColor(Color.WHITE);
    }

    private float beatPulse() {
        double beatLen = 60.0 / RhythmChart.BPM;
        double beats = (songTime - RhythmChart.START) / beatLen;
        double frac = beats - Math.floor(beats);
        return (float) Math.pow(1.0 - frac, 3.0);
    }

    private void drawLanesAndNotes() {
        Assets a = game.assets;

        // дорожки
        for (int i = 0; i < LANES; i++) {
            float x = laneX(i);
            game.batch.setColor(1f, 1f, 1f, 0.05f);
            game.batch.draw(a.pixel, x, 0, LANE_W, H);
            game.batch.setColor(1f, 1f, 1f, 0.10f);
            game.batch.draw(a.pixel, x, 0, 2f, H);
            game.batch.draw(a.pixel, x + LANE_W - 2f, 0, 2f, H);
        }
        game.batch.setColor(Color.WHITE);

        // подсветка нажатой дорожки
        for (int i = 0; i < LANES; i++) {
            if (laneFlash[i] > 0f) {
                Color c = LANE_COLOR[i];
                game.batch.setColor(c.r, c.g, c.b, Math.min(0.35f, laneFlash[i] * 1.5f));
                game.batch.draw(a.pixel, laneX(i), 0, LANE_W, H);
            }
        }
        game.batch.setColor(Color.WHITE);

        // линия попадания (пульсирует)
        float glow = 0.6f + 0.4f * (state == State.PLAY ? beatPulse() : 0f);
        game.batch.setColor(1f, 0.95f, 0.7f, glow);
        game.batch.draw(a.pixel, START_X - 10f, HIT_Y - 3f, TOTAL_W + 20f, 6f);
        game.batch.setColor(Color.WHITE);

        // подписи клавиш под линией
        BitmapFont f = a.fontSmall;
        f.setColor(0.75f, 0.78f, 0.9f, 0.85f);
        for (int i = 0; i < LANES; i++) {
            layout.setText(f, KEY_HINT[i]);
            f.draw(game.batch, layout, laneCenter(i) - layout.width / 2f, HIT_Y - 22f);
        }

        // стрелки-ноты
        for (RhythmChart.Note n : chart.notes) {
            if (n.judged && !n.hit) continue;
            float y = noteY(n.time);
            if (n.hit) {
                float age = songTime - n.hitTime;
                if (age > 0.25f) continue;
                y = HIT_Y;
            } else if (y > H + 40f || y < HIT_Y - 60f) {
                continue;
            }
            drawNote(n, y);
        }

        drawPopups();
    }

    private void drawNote(RhythmChart.Note n, float y) {
        Assets a = game.assets;
        Color c = LANE_COLOR[n.lane];
        float x = laneX(n.lane) + 8f;
        float w = LANE_W - 16f;
        float h = 54f;
        float drawY = y - h / 2f;

        float alpha = 0.95f;
        if (n.hit) {
            float age = MathUtils.clamp((songTime - n.hitTime) / 0.25f, 0f, 1f);
            alpha = 1f - age;
        }

        // мягкое свечение
        game.batch.setColor(c.r, c.g, c.b, 0.25f * alpha);
        game.batch.draw(a.pixel, x - 6f, drawY - 6f, w + 12f, h + 12f);
        // тело стрелки
        game.batch.setColor(c.r, c.g, c.b, alpha);
        game.batch.draw(a.pixel, x, drawY, w, h);
        // тёмная кромка
        game.batch.setColor(0f, 0f, 0f, 0.45f * alpha);
        game.batch.draw(a.pixel, x, drawY, w, 4f);
        game.batch.draw(a.pixel, x, drawY + h - 4f, w, 4f);

        // символ стрелки
        BitmapFont font = a.fontLarge;
        font.setColor(1f, 1f, 1f, alpha);
        layout.setText(font, GLYPH[n.lane]);
        font.draw(game.batch, layout,
            laneCenter(n.lane) - layout.width / 2f,
            y + layout.height / 2f);
        font.setColor(Color.WHITE);
        game.batch.setColor(Color.WHITE);
    }

    private void drawPopups() {
        Assets a = game.assets;
        for (Popup p : popups) {
            float t = p.life / 0.7f;
            float rise = (0.7f - p.life) * 90f;
            a.fontMedium.setColor(p.color.r, p.color.g, p.color.b, t);
            layout.setText(a.fontMedium, p.text);
            a.fontMedium.draw(game.batch, layout,
                p.x - layout.width / 2f, HIT_Y + 60f + rise);
        }
        a.fontMedium.setColor(Color.WHITE);
    }

    // ---------- HUD ----------

    private void drawHud() {
        Assets a = game.assets;

        // здоровье
        float hx = 40f;
        float hy = H - 54f;
        float hw = 300f;
        game.batch.setColor(0f, 0f, 0f, 0.55f);
        game.batch.draw(a.pixel, hx - 3f, hy - 3f, hw + 6f, 22f);
        float hr = MathUtils.clamp(health / MAX_HEALTH, 0f, 1f);
        game.batch.setColor(1f, 0.3f + 0.5f * hr, 0.35f, 0.95f);
        game.batch.draw(a.pixel, hx, hy, hw * hr, 16f);
        game.batch.setColor(Color.WHITE);
        a.fontSmall.setColor(0.9f, 0.9f, 0.95f, 0.9f);
        a.fontSmall.draw(game.batch, "СИЛЫ", hx, hy + 40f);
        a.fontSmall.setColor(Color.WHITE);

        // счёт
        a.fontMenu.setColor(1f, 0.93f, 0.7f, 1f);
        layout.setText(a.fontMenu, String.valueOf(score));
        a.fontMenu.draw(game.batch, layout, W - 40f - layout.width, H - 34f);
        a.fontMenu.setColor(Color.WHITE);

        // комбо
        if (combo >= 3) {
            String txt = combo + " подряд";
            a.fontMedium.setColor(1f, 0.85f, 0.4f, 0.95f);
            layout.setText(a.fontMedium, txt);
            a.fontMedium.draw(game.batch, layout, (W - layout.width) / 2f, H - 60f);
            a.fontMedium.setColor(Color.WHITE);
        }

        // прогресс песни
        if (state != State.INTRO) {
            float prog = MathUtils.clamp(
                (songTime - (float) RhythmChart.START) / (float) RhythmChart.LENGTH, 0f, 1f);
            game.batch.setColor(1f, 1f, 1f, 0.15f);
            game.batch.draw(a.pixel, START_X, H - 18f, TOTAL_W, 5f);
            game.batch.setColor(1f, 0.85f, 0.4f, 0.9f);
            game.batch.draw(a.pixel, START_X, H - 18f, TOTAL_W * prog, 5f);
            game.batch.setColor(Color.WHITE);
        }

        // «приготовься»
        if (state == State.PLAY && leadIn > 0f) {
            a.fontLarge.setColor(1f, 0.9f, 0.6f, 0.9f);
            layout.setText(a.fontLarge, "Приготовься…");
            a.fontLarge.draw(game.batch, layout, (W - layout.width) / 2f, H * 0.62f);
            a.fontLarge.setColor(Color.WHITE);
        }
    }

    // ---------- Intro / Result ----------

    private void drawIntro() {
        Assets a = game.assets;
        game.batch.setColor(0f, 0f, 0f, 0.55f);
        game.batch.draw(a.pixel, 0, 0, W, H);
        game.batch.setColor(Color.WHITE);

        a.fontLarge.setColor(1f, 0.84f, 0.35f, 1f);
        layout.setText(a.fontLarge, "БОСС — Танец");
        a.fontLarge.draw(game.batch, layout, (W - layout.width) / 2f, H * 0.80f);
        a.fontLarge.setColor(Color.WHITE);

        a.fontMedium.setColor(0.92f, 0.92f, 0.96f, 1f);
        String[] lines = {
            "Светящиеся стрелки съезжают по четырём дорожкам вниз.",
            "Когда стрелка коснётся золотой линии — нажми такую же клавишу:",
            "←   ↓   ↑   →   (можно и на W A S D)",
            "Попадай в такт музыке. Промахи отнимают силы — держи их.",
            "Главное — не скорость, а ритм. Просто танцуй.",
        };
        float y = H * 0.64f;
        for (String line : lines) {
            a.fontMedium.setColor(0.9f, 0.9f, 0.95f, 0.95f);
            layout.setText(a.fontMedium, line);
            a.fontMedium.draw(game.batch, layout, (W - layout.width) / 2f, y);
            y -= 42f;
        }
        a.fontMedium.setColor(Color.WHITE);

        float blink = 0.5f + 0.5f * (float) Math.abs(Math.sin(time * 3f));
        a.fontMedium.setColor(1f, 0.85f, 0.4f, blink);
        layout.setText(a.fontMedium, "[ ПРОБЕЛ ] — начать танец     Esc — выйти");
        a.fontMedium.draw(game.batch, layout, (W - layout.width) / 2f, 90f);
        a.fontMedium.setColor(Color.WHITE);
    }

    private void drawResult() {
        Assets a = game.assets;
        float t = Math.min(1f, resultTimer * 1.5f);
        game.batch.setColor(0f, 0f, 0f, 0.72f * t);
        game.batch.draw(a.pixel, 0, 0, W, H);
        game.batch.setColor(Color.WHITE);

        String title = failed ? "Устала… но ты старалась" : "Танец получился!";
        a.fontLarge.setColor(failed ? 0.95f : 1f, failed ? 0.6f : 0.85f, 0.4f, t);
        layout.setText(a.fontLarge, title);
        a.fontLarge.draw(game.batch, layout, (W - layout.width) / 2f, H * 0.78f);
        a.fontLarge.setColor(Color.WHITE);

        a.fontMedium.setColor(0.92f, 0.92f, 0.96f, t);
        String[] lines = {
            "Очки: " + score,
            "Идеально: " + perfects + "    Хорошо: " + goods + "    Почти: " + oks,
            "Промахи: " + misses + "    Лучшее комбо: " + maxCombo,
        };
        float y = H * 0.60f;
        for (String line : lines) {
            layout.setText(a.fontMedium, line);
            a.fontMedium.draw(game.batch, layout, (W - layout.width) / 2f, y);
            y -= 42f;
        }
        a.fontMedium.setColor(Color.WHITE);

        float blink = 0.5f + 0.5f * (float) Math.abs(Math.sin(time * 3f));
        String prompt = failed
            ? "[ ПРОБЕЛ ] — ещё раз      Esc — продолжить"
            : "[ ПРОБЕЛ ] — дальше";
        a.fontMedium.setColor(1f, 0.85f, 0.4f, blink * t);
        layout.setText(a.fontMedium, prompt);
        a.fontMedium.draw(game.batch, layout, (W - layout.width) / 2f, 110f);
        a.fontMedium.setColor(Color.WHITE);
    }

    // ------------------------------------------------------------------

    @Override
    public void hide() {
        Music music = game.assets.bossMusic;
        if (music != null && music.isPlaying()) music.stop();
    }

    @Override
    public void dispose() {
        hide();
    }

    @Override
    public void resize(int width, int height) {
        viewport.update(width, height);
    }
}
