package com.mygame.mechanics;

/**
 * Проверка партитуры босса-ритма (RhythmChart.java).
 * Не требует графического контекста — чистая логика.
 * Запуск: java com.mygame.mechanics.RhythmChartTest
 */
public class RhythmChartTest {

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

    private static void check(double expected, double actual, double tol, String msg) {
        check(Math.abs(expected - actual) <= tol, msg + " (ожидалось " + expected + ", получено " + actual + ")");
    }

    public static void main(String[] args) {
        constantsMatchMusic();
        boss1GeneratesNotesWithinMusicWindow();
        boss1IsDeterministic();
        noSimultaneousNotesOnSameLane();

        System.out.println("RhythmChartTest: passed=" + passed + " failed=" + failed);
        if (failed > 0) System.exit(1);
    }

    static void constantsMatchMusic() {
        check(114.0, RhythmChart.BPM, 0.001, "BPM");
        check(12.6, RhythmChart.START, 0.001, "START");
        check(72.0, RhythmChart.LENGTH, 0.001, "LENGTH");
        check(RhythmChart.LANES == 4, "LANES должен быть 4");
    }

    static void boss1GeneratesNotesWithinMusicWindow() {
        RhythmChart chart = RhythmChart.boss1();
        check(chart != null, "boss1() не должен возвращать null");
        check(!chart.notes.isEmpty(), "партия не должна быть пустой");

        double end = RhythmChart.START + RhythmChart.LENGTH;
        check(Math.abs(end - chart.endTime) <= 0.001,
            "endTime должен быть START + LENGTH (" + end + ", получено " + chart.endTime + ")");

        for (RhythmChart.Note note : chart.notes) {
            check(note.time >= RhythmChart.START - 0.001,
                "нота до начала партии: t=" + note.time);
            check(note.time <= end + 0.001,
                "нота после конца партии: t=" + note.time + " end=" + end);
            check(note.lane >= 0 && note.lane < RhythmChart.LANES,
                "неверная дорожка: " + note.lane);
        }
    }

    static void boss1IsDeterministic() {
        RhythmChart a = RhythmChart.boss1();
        RhythmChart b = RhythmChart.boss1();
        check(a.notes.size() == b.notes.size(),
            "детерминированность: разное число нот");
        for (int i = 0; i < a.notes.size(); i++) {
            check(Math.abs(a.notes.get(i).time - b.notes.get(i).time) <= 0.001,
                "детерминированность: разное время ноты " + i);
            check(a.notes.get(i).lane == b.notes.get(i).lane,
                "детерминированность: разная дорожка ноты " + i);
        }
    }

    static void noSimultaneousNotesOnSameLane() {
        RhythmChart chart = RhythmChart.boss1();
        double beat = 60.0 / RhythmChart.BPM;
        double eighth = beat * 0.5;
        double minGap = eighth * 0.9;

        for (int i = 0; i < chart.notes.size(); i++) {
            for (int j = i + 1; j < chart.notes.size(); j++) {
                RhythmChart.Note n1 = chart.notes.get(i);
                RhythmChart.Note n2 = chart.notes.get(j);
                if (n1.lane == n2.lane) {
                    check(Math.abs(n1.time - n2.time) >= minGap - 0.001,
                        "одновременные ноты на одной дорожке " + n1.lane
                            + ": t1=" + n1.time + " t2=" + n2.time + " minGap=" + minGap);
                }
            }
        }
    }
}
