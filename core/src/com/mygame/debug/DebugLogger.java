package com.mygame.debug;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Логгер отладочных событий игры.
 * Пишет в консоль и в файл `debug.log` в рабочей директории.
 * Используйте для быстрой диагностики: где упал, какой был экран, какая бутылка найдена.
 *
 * Примеры использования:
 *   DebugLogger.log("ЭКРАН", "Переход на MapScreen, уровень=%d", levelIndex);
 *   DebugLogger.log("БУТЫЛКА", "Найдена в домике %d, всего=%d/%d", houseIdx, found, total);
 *   DebugLogger.err("ОШИБКА", e, "Не удалось загрузить ресурс %s", path);
 */
public final class DebugLogger {

    private static final boolean ENABLED = true;
    private static final SimpleDateFormat DF = new SimpleDateFormat("HH:mm:ss.SSS");
    private static PrintWriter fileWriter;

    static {
        if (ENABLED) {
            try {
                fileWriter = new PrintWriter(new FileWriter("debug.log", true), true);
            } catch (IOException ignored) {
                // Если не получилось создать файл — пишем только в консоль
            }
        }
    }

    private DebugLogger() {}

    private static String now() {
        return DF.format(new Date());
    }

    private static void write(String tag, String msg) {
        if (!ENABLED) return;
        String line = String.format("[%s] [%s] %s", now(), tag, msg);
        System.out.println(line);
        if (fileWriter != null) {
            fileWriter.println(line);
            fileWriter.flush();
        }
    }

    /** Обычное сообщение. */
    public static void log(String tag, String format, Object... args) {
        write(tag, String.format(format, args));
    }

    /** Ошибка с исключением. */
    public static void err(String tag, Throwable t, String format, Object... args) {
        if (!ENABLED) return;
        String msg = String.format(format, args) + " -> " + t.getClass().getSimpleName() + ": " + t.getMessage();
        write(tag, msg);
        StackTraceElement[] st = t.getStackTrace();
        for (int i = 0; i < Math.min(st.length, 8); i++) {
            write(tag, "    at " + st[i]);
        }
        if (fileWriter != null) fileWriter.flush();
    }

    /** Закрыть файл лога (вызывать при завершении игры). */
    public static void close() {
        if (fileWriter != null) {
            fileWriter.close();
            fileWriter = null;
        }
    }
}
