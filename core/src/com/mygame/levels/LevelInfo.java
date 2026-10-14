package com.mygame.levels;

/**
 * Описание одного узла на карте (одного уровня).
 * refX / refY — позиция узла на картинке нити в пикселях исходного
 * изображения 1024x559 (отсчёт от верхнего левого угла).
 */
public class LevelInfo {

    public final String title;      // название, появляется в рамке сверху
    public final String subtitle;   // короткая фраза под названием
    public final String mechanic;   // заметка о фишке главы (пока для заглушек)
    public final boolean chapter;   // true — большая глава, false — «ключ» между главами
    public final float refX;
    public final float refY;

    public LevelInfo(String title, String subtitle, String mechanic,
                     boolean chapter, float refX, float refY) {
        this.title = title;
        this.subtitle = subtitle;
        this.mechanic = mechanic;
        this.chapter = chapter;
        this.refX = refX;
        this.refY = refY;
    }
}
