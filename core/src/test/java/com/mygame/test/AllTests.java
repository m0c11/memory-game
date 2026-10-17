package com.mygame.test;

/**
 * Запускает все unit-тесты игры подряд.
 * Тесты не требуют графического контекста libGDX — только чистая логика.
 *
 * Запуск:
 *   ./gradlew :core:runTests
 * или вручную:
 *   java -cp core/build/classes/java/*:core/build/resources/main:$(gradle find gdx jar) com.mygame.test.AllTests
 */
public class AllTests {

    public static void main(String[] args) {
        int failed = 0;

        System.out.println("=== LevelsTest ===");
        failed += run(com.mygame.levels.LevelsTest.class);

        System.out.println("=== RhythmChartTest ===");
        failed += run(com.mygame.mechanics.RhythmChartTest.class);

        System.out.println("=== DistractionMeterTest ===");
        failed += run(com.mygame.mechanics.DistractionMeterTest.class);

        System.out.println();
        if (failed == 0) {
            System.out.println("ВСЕ ТЕСТЫ ПРОЙДЕНЫ");
            System.exit(0);
        } else {
            System.out.println("ТЕСТОВ С ОШИБКАМИ: " + failed);
            System.exit(1);
        }
    }

    private static int run(Class<?> testClass) {
        try {
            testClass.getMethod("main", String[].class).invoke(null, (Object) new String[0]);
            return 0;
        } catch (Throwable t) {
            System.out.println("ОШИБКА ЗАПУСКА " + testClass.getSimpleName() + ": " + t.getCause());
            return 1;
        }
    }
}
