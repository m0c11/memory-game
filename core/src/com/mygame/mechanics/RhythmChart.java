package com.mygame.mechanics;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Партитура босса-танца: список нот (стрелок), которые нужно нажимать в ритм.
 *
 * Ритм привязан к музыке assets/music/boos_1_level.mp3: темп определён как
 * BPM 114, а первая сильная доля основной части — на 12.6 секунде. Ноты
 * расставлены по восьмым долям с псевдослучайным рисунком (seed фиксирован,
 * поэтому партия одинаковая и её можно отрепетировать).
 *
 * Если заменишь музыку — поправь BPM, START и END ниже.
 */
public final class RhythmChart {

    /** Название дорожек-стрелок: 0 ←, 1 ↓, 2 ↑, 3 →. */
    public static final int LANES = 4;

    public static final double BPM = 114.0;
    /** Начало партии в секундах от старта музыки (первая сильная доля). */
    public static final double START = 12.6;
    /** Длительность партии, секунды. */
    public static final double LENGTH = 72.0;

    /** Одна стрелка. */
    public static class Note {
        public final float time;   // когда стрелка доходит до линии (сек)
        public final int lane;     // 0..3
        public boolean hit = false;
        public boolean judged = false;
        public float hitTime = 0f;

        Note(float time, int lane) {
            this.time = time;
            this.lane = lane;
        }
    }

    public final List<Note> notes = new ArrayList<>();
    public final double endTime;

    private RhythmChart(double endTime) {
        this.endTime = endTime;
    }

    /** Стандартная партия первого босса. */
    public static RhythmChart boss1() {
        RhythmChart chart = new RhythmChart(START + LENGTH);
        double beat = 60.0 / BPM;
        double eighth = beat * 0.5;
        int steps = (int) Math.floor(LENGTH / eighth);
        Random r = new Random(20241014L);
        int lastLane = -1;
        // Минимальный зазор между стрелками: без одновременных «аккордов»,
        // которые на одной клавиатуре нажать нельзя.
        double minGap = eighth * 0.9;
        double lastTime = -100.0;

        for (int i = 0; i < steps; i++) {
            double t = START + i * eighth;
            boolean onBeat = (i % 2) == 0;

            // На сильных долях стрелок больше, на слабых — реже (ритмичный рисунок).
            double chance = onBeat ? 0.82 : 0.30;
            if (r.nextDouble() > chance) continue;
            if (t - lastTime < minGap) continue;

            int lane = r.nextInt(LANES);
            // Не повторяем одну дорожку подряд слишком часто.
            if (lane == lastLane && r.nextDouble() < 0.65) {
                lane = (lane + 1 + r.nextInt(LANES - 1)) % LANES;
            }
            chart.notes.add(new Note((float) t, lane));
            lastLane = lane;
            lastTime = t;

            // Изредка — короткая «двойка», чтобы был бег.
            double t2 = t + eighth;
            if (onBeat && t2 < START + LENGTH && r.nextDouble() < 0.10) {
                int lane2 = (lane + 1 + r.nextInt(LANES - 1)) % LANES;
                chart.notes.add(new Note((float) t2, lane2));
                lastLane = lane2;
                lastTime = t2;
            }
        }
        return chart;
    }
}
