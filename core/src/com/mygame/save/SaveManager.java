package com.mygame.save;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.mygame.levels.Levels;

/**
 * Сохранение прогресса. Использует Preferences из libGDX:
 * на Linux файл лежит в ~/.prefs/memory-game-save
 * (на Windows — в папке пользователя, на Mac — в ~/.prefs).
 *
 * Хранится:
 *  - started   — была ли начата игра (по нему в меню появляется «Продолжить»)
 *  - completed — сколько уровней пройдено подряд с начала
 *  - ballX/ballY — где стоит клубок на карте
 *  - trail     — пройденная нить (список точек)
 */
public class SaveManager {

    private static final String PREFS_NAME = "memory-game-save";
    private static final String KEY_STARTED = "started";
    private static final String KEY_COMPLETED = "completed";
    private static final String KEY_HAS_BALL = "hasBall";
    private static final String KEY_BALL_X = "ballX";
    private static final String KEY_BALL_Y = "ballY";
    private static final String KEY_TRAIL = "trail";
    private static final String KEY_CHECKPOINT = "checkpointLevel";

    private final Preferences prefs;

    public SaveManager() {
        prefs = Gdx.app.getPreferences(PREFS_NAME);
    }

    public boolean hasSave() {
        return prefs.getBoolean(KEY_STARTED, false);
    }

    public int getCompleted() {
        int value = prefs.getInteger(KEY_COMPLETED, 0);
        return Math.max(0, Math.min(Levels.COUNT, value));
    }

    /** Стирает старый прогресс (включая карту) и создаёт новое сохранение. */
    public void newGame() {
        prefs.putBoolean(KEY_STARTED, true);
        prefs.putInteger(KEY_COMPLETED, 0);
        prefs.remove(KEY_HAS_BALL);
        prefs.remove(KEY_BALL_X);
        prefs.remove(KEY_BALL_Y);
        prefs.remove(KEY_TRAIL);
        prefs.remove(KEY_CHECKPOINT);
        prefs.flush();
    }

    /**
     * Контрольное сохранение у звезды (в духе Undertale).
     * Прогресс (число пройденных уровней) не меняется — просто фиксируем
     * состояние и запоминаем, где сохранились.
     */
    public void saveCheckpoint(int levelIndex) {
        prefs.putBoolean(KEY_STARTED, true);
        prefs.putInteger(KEY_CHECKPOINT, levelIndex);
        prefs.flush();
    }

    public int getCheckpoint() {
        return prefs.getInteger(KEY_CHECKPOINT, 0);
    }

    /** Отмечает уровень пройденным. Повторное прохождение прогресс не уменьшает. */
    public void completeLevel(int index) {
        if (index >= getCompleted()) {
            prefs.putBoolean(KEY_STARTED, true);
            prefs.putInteger(KEY_COMPLETED, index + 1);
            prefs.flush();
        }
    }

    // ---------- состояние карты ----------

    public boolean hasBallPosition() {
        return prefs.getBoolean(KEY_HAS_BALL, false);
    }

    public float getBallX() {
        return prefs.getFloat(KEY_BALL_X, 0f);
    }

    public float getBallY() {
        return prefs.getFloat(KEY_BALL_Y, 0f);
    }

    public String getTrail() {
        return prefs.getString(KEY_TRAIL, "");
    }

    public void saveMapState(float ballX, float ballY, String trail) {
        prefs.putBoolean(KEY_HAS_BALL, true);
        prefs.putFloat(KEY_BALL_X, ballX);
        prefs.putFloat(KEY_BALL_Y, ballY);
        prefs.putString(KEY_TRAIL, trail);
        prefs.flush();
    }
}
