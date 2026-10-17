# memory-game

2D-игра на libGDX (Java 17) для Ubuntu. Все объяснения пользователю — на русском.
Сборка — Gradle, модули `core` и `desktop`.

## Команды

- Запуск: `./gradlew desktop:run` (окно 1280×720, 60 FPS; стартовый экран — главное меню)
- Jar: `./gradlew desktop:jar` → `dist/desktop-0.1.0.jar`; запуск: `java -jar dist/desktop-0.1.0.jar`
- Unit-тесты (чистая логика, без графики): `./gradlew :core:runTests` — запускает `com.mygame.test.AllTests` (`LevelsTest`, `RhythmChartTest`, `DistractionMeterTest`); JUnit не используется
- После правок проверяй: `./gradlew desktop:jar` + `./gradlew desktop:run`. Линтера/форматтера/CI нет
- Требования: JDK 17 + Gradle 8.x (wrapper уже в репозитории — 8.10.2). **Gradle 8.x не работает с Java 24/25** — нужен Java 17: `JAVA_HOME=/usr/lib/jvm/java-17.0.20-openjdk-amd64 ./gradlew ...` или `sdk use java 17.0.20-tem`; `run_game.sh` проверяет версию Java

## Структура

- `core/` — модуль `java-library`; логика в пакетах `com.mygame.{assets, levels, mechanics, save, screens, entities, world, ui, effects, debug}`; источники — `core/src/` (не `src/main/java`; в `build.gradle` переопределён `sourceSets.main.java.srcDirs = ['src/']`)
- `desktop/` — модуль `application`; entry `com.mygame.desktop.DesktopLauncher` (`new Lwjgl3Application(new MyGame())`); `assets/` корня — `sourceSets.main.resources.srcDirs`
- `assets/` — картинки, тайлы (`tiles/grey_*`, `tiles/color_*`), JSON комнат (`assets/rooms/*.json`), музыка, звуки; `.ttf` в репозитории нет (системные шрифты)

## Архитектура

- `MyGame` — корень `com.badlogic.gdx.Game`: `SpriteBatch batch`, `Assets assets`, `SaveManager save`; `MyGame.create()` инициализирует `assets` первым делом; экраны — через `setScreen()`
- `Assets` — загрузка ресурсов; текстуры комнат генерируются кодом (`createTexture`), часть грузится из `assets/`; шрифты — `FreeTypeFontGenerator` из системных `.ttf`
- `RoomFactory.createRoom(index, assets)` — комната по уровню; свои комнаты для 0, 1, 3, 5, 11, остальные — `roomPlaceholder`; уровень 1 — три связанные локации (`createRooms()`) с порталами
- `Levels.COUNT = 12`; оттенок комнаты = `index / (COUNT - 1)`
- `RhythmChart` — `BPM = 114`, `START = 12.6`, `LENGTH = 72`, 4 lane'а, seed `20241014`; при замене музыки (`assets/music/boos_1_level.mp3`) — править эти константы
- `SaveManager` — libGDX `Preferences`; Linux: `~/.prefs/memory-game-save`

## Уровень 5 «Школа Актива» (хоррор)

- `HorrorBaseScreen` — база отдыха «Веневитино», открытый мир (2400×2400), камера следует за игроком, можно уходить за границы экрана
- `HorrorHouseScreen` — домик; вход с базы, выход по ESC или двери внизу комнаты (ESC не выходит с уровня)
- `PacmanBossScreen` — босс-пакман, запускается когда все бутылки найдены
- На базе кусты (`Bush`): `R` — спрятаться, `E` — поиск бутылок (`BushSearch`)
- Домики — 4 здания; каждая своя комната (26×15 тайлов): стены `houseWallsA/B/C/D()`, предметы/бутылки `houseItemsA/B/C/D()` в `RoomFactory.java`
- **Общий счетчик бутылок**: база (кусты) и дома — в один общий счётчик; прогресс сохраняется при выходе из домика
- Организаторы патрулируют базу; при касании — диалог, герой вытаскивается к входу в случайный домик (не конец игры)

## Отладка

- `DebugLogger` (`com.mygame.debug`): в консоль + файл `debug.log` (текущая рабочая директория). `log("ТЕГ", "сообщение %s", arg)`, `err("ТЕГ", exception, "сообщение %s", arg)` (до 8 строк стектрейса). Использовать при переключении экранов, поиске бутылок, касании организатора.
- `DebugOverlay` (`com.mygame.debug`): FPS, текущий экран, уровень, позиция игрока, счётчик бутылок. Выключен по умолчанию; рисовать вручную в конце `render()` экрана: `if (DebugOverlay.isEnabled()) overlay.draw(batch, game, delta)`. Клавиша F1 сейчас не обрабатывается автоматически — `DebugOverlay.toggle()` нужно вызывать самому (в input handling).

## Типичные ошибки

| Симптом | Где искать |
|---------|------------|
| `ArrayIndexOutOfBoundsException` | Room-индексы вне `0..COLS-1` (26) / `0..ROWS-1` (15) — `RoomFactory.java`, `Room.setWall()` |
| Текстура не грузится / чёрный экран | `Assets.java` — `loadImage()` / путь через `Gdx.files.internal(path)` |
| Кириллица квадраты | Нет системного `.ttf` — `/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf`, затем Liberation; `sudo apt install fonts-dejavu` |
| Не стартует на Java 24/25 | Gradle 8.x не поддерживает — нужен Java 17 |
| Сохранение не работает | `~/.prefs/memory-game-save` (Linux); не удалять без разрешения |
| Бутылки не считаются | `HorrorBaseScreen.createBushes()` / `createHouses()`, `HorrorHouseScreen.interact()` |
| NPE на `game.assets` | Экран создан до инициализации `Assets` в `MyGame.create()` |
| Экран не переключается | `MyGame.setScreen()` не вызван или экран `null` |
| Портал ведёт не туда | `RoomFactory.java` — `Portal(..., targetRoom, spawnX, spawnY)` |

## Важно

- При замене музыки босса — поправь `BPM`, `START`, `LENGTH` в `RhythmChart.java`
- Новая глава — добавь `case` в `RoomFactory.createRoom()` и проверь `Levels.COUNT` (12)
- `tools/` — Python-скрипты (`pip install pillow numpy scipy`) для нарезки тайлов и сборки JSON оформления сцены; не входят в Gradle-сборку
- Не удаляй `~/.prefs/memory-game-save` — единственная копия прогресса, она не в репозитории

## Управление (для проверки в игре)

- `WASD`/стрелки — движение, `Z` — взаимодействие/сохранение, `ESC` — меню; полный список — в `README.md`
- На карте (`MapScreen`) `F4` — разблокировать все уровни для тестирования
- На уровне 5: `E` — взять предмет / поиск в кусте, `R` — спрятаться в куст / выйти из куста, `ESC` — выйти из домика / пауза (не выходит с уровня)
