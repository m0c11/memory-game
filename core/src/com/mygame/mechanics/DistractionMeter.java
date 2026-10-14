package com.mygame.mechanics;

import com.badlogic.gdx.math.MathUtils;

/**
 * Фишка главы «Подготовка к Студвесне» — тревожная шкала.
 *
 * Пока героиня ходит по комнате и собирает предметы, шкала медленно
 * заполняется — это отвлекает и требует внимания. Как только шкала
 * заполнилась, нужно успеть нажать кнопку (ПРОБЕЛ), чтобы «выдохнуть»
 * и сбросить её. Если дать шкале переполниться или нажать слишком рано —
 * героиня спотыкается: экран трясёт, всплывает «Ой!».
 *
 * Три спотыкания подряд (без удачного нажатия между ними) — «срыв»:
 * срабатывает событие brokeDown, и дальше шкала ведёт себя мягче
 * (заполняется медленнее, а окно на нажатие шире). Метафора: рядом
 * вовремя оказалась поддержка — тот, кто взял за руку.
 *
 * Класс ничего не рисует и не знает про экраны — только считает состояние.
 */
public class DistractionMeter {

    /** Сколько секунд шкала заполняется (обычный режим). */
    public static final float FILL_TIME = 3.3f;
    /** Заполнение после «поддержки» — легче. */
    public static final float SUPPORT_FILL_TIME = 4.6f;
    /** Сколько можно держать полную шкалу до провала. */
    public static final float GRACE = 1.0f;
    public static final float SUPPORT_GRACE = 1.7f;
    /** Нажатие раньше этого порога — преждевременное спотыкание. */
    public static final float GOOD = 0.55f;
    /** Ступор после обычного спотыкания. */
    public static final float STUMBLE_FREEZE = 0.6f;
    /** Ступор после «срыва». */
    public static final float BREAK_FREEZE = 1.4f;
    /** Сколько спотыканий подряд до срыва. */
    public static final int MISTAKES_TO_BREAK = 3;

    private float charge = 0f;       // 0..1
    private float graceTimer = 0f;   // обратный отсчёт на полной шкале
    private float freezeTimer = 0f;
    private int mistakes = 0;
    private boolean support = false;

    // --- события кадра (живут до clearEvents) ---
    public boolean success = false;
    public boolean stumble = false;
    public boolean brokeDown = false;

    public void update(float delta) {
        if (freezeTimer > 0f) {
            freezeTimer -= delta;
            return;
        }

        float fill = 1f / (support ? SUPPORT_FILL_TIME : FILL_TIME);
        if (charge < 1f) {
            charge = Math.min(1f, charge + fill * delta);
            if (charge >= 1f) {
                graceTimer = support ? SUPPORT_GRACE : GRACE;
            }
        } else {
            graceTimer -= delta;
            if (graceTimer <= 0f) fail();
        }
    }

    /** Нажатие кнопки. Рано — спотыкание, в зоне успеха — сброс шкалы. */
    public void press() {
        if (freezeTimer > 0f) return;
        if (charge >= GOOD) {
            success = true;
            mistakes = 0;
            reset();
        } else {
            fail();
        }
    }

    private void fail() {
        stumble = true;
        mistakes++;
        reset();
        if (mistakes >= MISTAKES_TO_BREAK) {
            brokeDown = true;
            mistakes = 0;
            support = true;
            freezeTimer = BREAK_FREEZE;
        } else {
            freezeTimer = STUMBLE_FREEZE;
        }
    }

    private void reset() {
        charge = 0f;
        graceTimer = 0f;
    }

    /** Заполнение шкалы 0..1 — для отрисовки. */
    public float getCharge() {
        return MathUtils.clamp(charge, 0f, 1f);
    }

    /** Шкала заполнена и ждёт нажатия. */
    public boolean isFull() {
        return charge >= 1f;
    }

    public boolean isFrozen() {
        return freezeTimer > 0f;
    }

    public int getMistakes() {
        return mistakes;
    }

    public boolean hasSupport() {
        return support;
    }

    /** Сбрасывает события кадра. Вызывается экраном в конце update(). */
    public void clearEvents() {
        success = false;
        stumble = false;
        brokeDown = false;
    }
}
