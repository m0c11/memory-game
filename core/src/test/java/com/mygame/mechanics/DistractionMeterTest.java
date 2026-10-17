package com.mygame.mechanics;

/**
 * Проверка тревожной шкалы главы «Подготовка к Студвесне» (DistractionMeter.java).
 * Не требует графического контекста — чистая логика.
 * Запуск: java com.mygame.mechanics.DistractionMeterTest
 */
public class DistractionMeterTest {

    private static int passed = 0;
    private static int failed = 0;

    private static void check(boolean cond, String msg) {
        if (cond) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL: " + msg);
        }
    }

    private static void check(float expected, float actual, float tol, String msg) {
        check(Math.abs(expected - actual) <= tol, msg + " (ожидалось " + expected + ", получено " + actual + ")");
    }

    private static void check(int expected, int actual, String msg) {
        check(expected == actual, msg + " (ожидалось " + expected + ", получено " + actual + ")");
    }

    public static void main(String[] args) {
        startsEmpty();
        fillsUpOverTime();
        successfulPressResetsCharge();
        earlyPressIsStumble();
        threeMistakesCauseBreakdown();
        successResetsMistakes();
        clearEventsResetsFlags();

        System.out.println("DistractionMeterTest: passed=" + passed + " failed=" + failed);
        if (failed > 0) System.exit(1);
    }

    static void startsEmpty() {
        DistractionMeter m = new DistractionMeter();
        check(0f, m.getCharge(), 0.001f, "начальный заряд");
        check(!m.isFull(), "изначально шкала не полна");
        check(!m.isFrozen(), "изначально нет ступора");
        check(!m.hasSupport(), "изначально нет поддержки");
    }

    static void fillsUpOverTime() {
        DistractionMeter m = new DistractionMeter();
        m.update(DistractionMeter.FILL_TIME);
        check(m.isFull(), "шкала должна заполниться за FILL_TIME=" + DistractionMeter.FILL_TIME);
    }

    static void successfulPressResetsCharge() {
        DistractionMeter m = new DistractionMeter();
        m.update(DistractionMeter.FILL_TIME);
        check(m.isFull(), "шкала заполнена перед нажатием");

        m.press();
        check(m.success, "успешное нажатие должно установить success");
        check(0f, m.getCharge(), 0.001f, "заряд сброшен после успеха");
        check(0, m.getMistakes(), "ошибки сброшены после успеха");
        check(!m.isFrozen(), "нет ступора после успеха");
    }

    /** Нажать кнопку, дожидаясь конца любого ступора, чтобы нажатие не игнорировалось. */
    static void pressAndWait(DistractionMeter m) {
        while (m.isFrozen()) {
            m.update(0.7f); // STUMBLE_FREEZE = 0.6f, BREAK_FREEZE = 1.4f
        }
        m.press();
    }

    static void earlyPressIsStumble() {
        DistractionMeter m = new DistractionMeter();
        m.press(); // сразу, пока charge < GOOD = 0.55
        check(m.stumble, "рано нажатие должно быть спотыканием");
        check(1, m.getMistakes(), "одна ошибка");
        check(m.isFrozen(), "после спотыкания есть ступор");
    }

    static void threeMistakesCauseBreakdown() {
        DistractionMeter m = new DistractionMeter();
        for (int i = 0; i < DistractionMeter.MISTAKES_TO_BREAK; i++) {
            pressAndWait(m);
            if (i < DistractionMeter.MISTAKES_TO_BREAK - 1) {
                m.clearEvents(); // clearEvents сбрасывает флаги кадра, включая brokeDown
            }
        }
        check(m.brokeDown, "после 3 ошибок должен быть срыв");
        check(m.hasSupport(), "после срыва включается поддержка");
        check(m.isFrozen(), "после срыва длинный ступор");
        check(0, m.getMistakes(), "ошибки сброшены после срыва");
    }

    static void successResetsMistakes() {
        DistractionMeter m = new DistractionMeter();
        pressAndWait(m); // ошибка #1
        m.clearEvents();
        pressAndWait(m); // ошибка #2
        m.clearEvents();

        m.update(DistractionMeter.FILL_TIME);
        m.press();
        check(0, m.getMistakes(), "успешное нажатие сбрасывает ошибки");
    }

    static void clearEventsResetsFlags() {
        DistractionMeter m = new DistractionMeter();
        m.press();
        check(m.stumble, "спотыкание зафиксировано");
        m.clearEvents();
        check(!m.stumble, "clearEvents сбрасывает stumble");
        check(!m.success, "clearEvents сбрасывает success");
        check(!m.brokeDown, "clearEvents сбрасывает brokeDown");
    }
}
