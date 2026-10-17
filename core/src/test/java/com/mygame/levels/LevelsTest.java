package com.mygame.levels;

/**
 * Проверка конфигурации уровней (Levels.java).
 * Не требует графического контекста — чистая логика.
 * Запуск: java com.mygame.levels.LevelsTest
 */
public class LevelsTest {

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

    public static void main(String[] args) {
        countMatchesAllArray();
        hasBossOnlyOnLevelOne();
        allLevelsHaveTitlesAndValidCoordinates();
        chapterFlagsAreConsistent();

        System.out.println("LevelsTest: passed=" + passed + " failed=" + failed);
        if (failed > 0) System.exit(1);
    }

    static void countMatchesAllArray() {
        check(Levels.COUNT == 12, "COUNT должен быть 12");
        check(Levels.ALL.length == 12, "ALL.length должен быть 12");
    }

    static void hasBossOnlyOnLevelOne() {
        check(Levels.hasBoss(1), "уровень 1 должен иметь босса");
        check(!Levels.hasBoss(0), "уровень 0 не должен иметь босса");
        check(!Levels.hasBoss(2), "уровень 2 не должен иметь босса");
        check(!Levels.hasBoss(5), "уровень 5 не должен иметь босса");
        check(!Levels.hasBoss(11), "уровень 11 не должен иметь босса");
    }

    static void allLevelsHaveTitlesAndValidCoordinates() {
        for (int i = 0; i < Levels.COUNT; i++) {
            LevelInfo info = Levels.ALL[i];
            check(info.title != null && !info.title.isBlank(),
                "уровень " + i + " без названия");
            // refX/refY берутся из картинки thread.png 1024x547
            check(info.refX >= 0 && info.refX <= 1024,
                "уровень " + i + " refX=" + info.refX + " вне картинки");
            check(info.refY >= 0 && info.refY <= 547,
                "уровень " + i + " refY=" + info.refY + " вне картинки");
        }
    }

    static void chapterFlagsAreConsistent() {
        check(Levels.ALL[1].chapter, "уровень 1 (Студвесна) должен быть главой");
        check(Levels.ALL[3].chapter, "уровень 3 (Лагерь) должен быть главой");
        check(Levels.ALL[5].chapter, "уровень 5 (Школа Актива) должен быть главой");
        check(Levels.ALL[7].chapter, "уровень 7 (Корпорат) должен быть главой");
        check(Levels.ALL[9].chapter, "уровень 9 (День рождения) должен быть главой");
        check(Levels.ALL[11].chapter, "уровень 11 (Финал) должен быть главой");
        check(!Levels.ALL[2].chapter, "уровень 2 (Ключ I) не должен быть главой");
        check(!Levels.ALL[4].chapter, "уровень 4 (Ключ II) не должен быть главой");
        check(!Levels.ALL[6].chapter, "уровень 6 (Ключ III) не должен быть главой");
        check(!Levels.ALL[8].chapter, "уровень 8 (Ключ IV) не должен быть главой");
        check(!Levels.ALL[10].chapter, "уровень 10 (Ключ V) не должен быть главой");
    }
}
