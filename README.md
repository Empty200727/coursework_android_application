# Кинополка

Android-приложение для поиска фильмов и сериалов через [TMDB API](https://developer.themoviedb.org/)
и ведения личной медиатеки, которая работает без сети. Курсовой проект.

- **Главная** — «В тренде за неделю», популярные фильмы и сериалы, 8 полок жанров, фильтр
  «Все / Фильмы / Сериалы».
- **Поиск** — фильмы и сериалы одним запросом, бесконечная прокрутка, жанры в результатах.
- **Экран жанра** — полный список с сортировкой «Популярные / По рейтингу / Новые».
- **Карточка** — описание, рейтинг, актёры, похожие; кнопки «Хочу посмотреть», «Смотрел»,
  «В избранное».
- **Моя полка** — вкладки «Хочу», «Смотрел», «Избранное» со счётчиками, сортировкой и удалением
  с отменой; работает в режиме полёта вместе с постерами.

Документы: требования и архитектура — [docs/PLAN.md](docs/PLAN.md), план итераций —
[docs/ITERATIONS.md](docs/ITERATIONS.md), аудит требований — [docs/AUDIT.md](docs/AUDIT.md).

## Скриншоты

<!-- Положите скриншоты в docs/screenshots и замените строки ниже. -->

| Главная | Поиск | Карточка | Моя полка |
| --- | --- | --- | --- |
| `docs/screenshots/home.png` | `docs/screenshots/search.png` | `docs/screenshots/details.png` | `docs/screenshots/library.png` |

## Сборка

Нужны Android Studio (свежая стабильная версия, встроенный JDK 21) и Android SDK 37 — Studio
предложит установить его при первой синхронизации.

1. Получите токен TMDB: зарегистрируйтесь на themoviedb.org, откройте **Settings → API** и
   скопируйте **API Read Access Token** (длинная строка, начинается с `eyJ`).
2. Добавьте его в `local.properties` в корне проекта. Файл есть в `.gitignore` и не попадает
   в Git (N-09):

   ```properties
   TMDB_TOKEN=eyJhbGciOiJIUzI1NiJ9...
   ```

   Для CI вместо файла можно задать переменную окружения `TMDB_TOKEN`.
3. Откройте проект в Android Studio и запустите конфигурацию `app` на эмуляторе или телефоне.

### Команды

| Команда | Что делает |
| --- | --- |
| `./gradlew assembleDebug` | Debug-сборка (с LeakCanary) |
| `./gradlew assembleRelease` | Release-сборка с R8: `app/build/outputs/apk/release/app-release.apk` |
| `./gradlew testDebugUnitTest` | Все автотесты, включая UI-сценарии и тесты базы на Robolectric — без устройства |
| `./gradlew lint detekt ktlintCheck` | Статический анализ; `./gradlew ktlintFormat` исправляет оформление |
| `./gradlew koverHtmlReportCoverage koverVerifyCoverage` | Покрытие `core/data` и `core/database` (цель — от 60%) |

Release-APK подписан debug-ключом, чтобы его можно было установить на устройство для
демонстрации; для публикации понадобился бы собственный ключ.

## Архитектура

MVVM с однонаправленным потоком данных: экран получает один `StateFlow<UiState>` из ViewModel,
репозитории читают Room и обновляют его из сети (офлайн-first).

```
feature/*        экраны, ViewModel и UiState: home, search, genre, details, library, about
core/ui          тема Material 3 и общие компоненты (постер, полка, скелеты, состояния)
core/data        репозитории, политика кэша, Paging, медиатека, очистка кэша (WorkManager)
core/database    Room: сущности, DAO, схема в app/schemas
core/network     Retrofit API TMDB, DTO, интерцепторы токена, языка и повтора при 429
core/model       доменные модели
```

Стек: Kotlin, Jetpack Compose, Material 3, Navigation Compose, Hilt, Room, Retrofit, OkHttp,
kotlinx.serialization, Coroutines/Flow, Paging 3, Coil, WorkManager. Версии — в
`gradle/libs.versions.toml`.

## Тестирование

Все автотесты запускаются на JVM, без устройства: unit-тесты ViewModel, репозиториев, мапперов и
политики кэша; сеть — на MockWebServer с JSON-фикстурами TMDB; база — на Room in-memory и
`MigrationTestHelper`; три сквозных UI-сценария (поиск → карточка → «Хочу посмотреть»,
«Моя полка» без сети, удаление с отменой) и проверки доступности — на Robolectric с Hilt.
Ручные проверки перед защитой перечислены в [docs/AUDIT.md](docs/AUDIT.md).

---

This product uses the TMDB API but is not endorsed or certified by TMDB.
