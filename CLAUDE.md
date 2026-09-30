# Кинополка

Android-приложение: каталог фильмов и сериалов на TMDB API с офлайн-медиатекой.
Требования и архитектура: docs/PLAN.md (ID требований F-xx, N-xx). Итерации: docs/ITERATIONS.md.

## Стек
Kotlin, Jetpack Compose + Material 3, MVVM, Hilt, Retrofit + OkHttp + kotlinx.serialization,
Coroutines/Flow, Room (KSP), Paging 3, Coil, Navigation Compose (типобезопасные маршруты), WorkManager.

## Правила
- Room — единый источник данных для UI; сеть только обновляет базу.
- Ключ произведения везде — пара (mediaType, tmdbId).
- Один UiState на экран, StateFlow из ViewModel; запрос и фильтры — в SavedStateHandle.
- Пакеты по функциям: feature/*, core/network, core/database, core/data, core/model, core/ui.
- Версии зависимостей только в gradle/libs.versions.toml, стабильные.
- Токен TMDB: local.properties -> BuildConfig. Никогда не коммитить.
- Строки интерфейса — в strings.xml, на русском.
- Не менять схему Room без миграции и теста.
- Новый код покрывать тестами. Перед завершением задачи: ./gradlew assembleDebug testDebugUnitTest lint detekt ktlintCheck

## Заметки по сборке
- AGP 9 со встроенной поддержкой Kotlin: плагин org.jetbrains.kotlin.android не подключается.
- detekt и ktlint запускаются как CLI через задачи корневого build.gradle.kts
  (`detekt`, `ktlintCheck`, `ktlintFormat`); конфигурация — config/detekt/detekt.yml и .editorconfig.
- Схемы Room экспортируются в app/schemas — их нужно коммитить.
- Никогда не использовать OnConflictStrategy.REPLACE для таблицы media: REPLACE удаляет строку
  и каскадно стирает актёров, жанры и ленты. Только @Upsert или частичный @Update.
