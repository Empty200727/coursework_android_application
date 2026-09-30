# Кинополка

Android-приложение для поиска фильмов и сериалов через TMDB API с офлайн-медиатекой.
Требования и архитектура — [docs/PLAN.md](docs/PLAN.md), план разработки — [docs/ITERATIONS.md](docs/ITERATIONS.md).

Статус: готовы итерации 1 «Фундамент и данные» и 2 «Каталог и поиск» — главная с подборками и
полками жанров, экран жанра, поиск; карточка и медиатека пока заглушки.

## Сборка

Нужны Android Studio с Android SDK 37 и JDK 17+.

1. Получите в кабинете TMDB (Settings → API) токен доступа на чтение (API Read Access Token).
2. Добавьте его в `local.properties` в корне проекта (файл не попадает в Git):

   ```properties
   TMDB_TOKEN=eyJhbGciOiJIUzI1NiJ9...
   ```

   Для CI вместо файла можно задать переменную окружения `TMDB_TOKEN`.
3. Соберите и проверьте проект:

   ```bash
   ./gradlew assembleDebug testDebugUnitTest lint detekt ktlintCheck
   ```

`./gradlew ktlintFormat` исправляет оформление кода автоматически.

## Стек

Kotlin, Jetpack Compose + Material 3, MVVM, Hilt, Retrofit + OkHttp + kotlinx.serialization,
Coroutines/Flow, Room, Coil, Navigation Compose. Версии — в `gradle/libs.versions.toml`.

---

This product uses the TMDB API but is not endorsed or certified by TMDB.
