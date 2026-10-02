# Кинополка

Android-приложение: каталог фильмов и сериалов на TMDB API с офлайн-медиатекой.
Требования и архитектура: docs/PLAN.md (ID требований F-xx, N-xx). Итерации: docs/ITERATIONS.md.

## Стек
Java 17, фрагменты + XML-разметка + ViewBinding, Material Components (Material 3), MVVM, Hilt,
Retrofit + OkHttp + Gson, LiveData, ExecutorService/ListenableFuture, Room, Paging 3 (ListenableFuture),
Glide, Navigation Component (XML-граф), WorkManager. Kotlin в проекте не используется.

## Правила
- Room — единый источник данных для UI; сеть только обновляет базу.
- Ключ произведения везде — пара (mediaType, tmdbId).
- Один UiState (record) на экран, LiveData из ViewModel; запрос и фильтры — в SavedStateHandle.
- Репозитории блокирующие: ViewModel вызывает их через `AppExecutors.io()`, читает LiveData из Room.
- Пакеты по функциям: feature/*, core/network, core/database, core/data, core/model, core/ui.
- Версии зависимостей только в gradle/libs.versions.toml, стабильные.
- Токен TMDB: local.properties -> BuildConfig. Никогда не коммитить.
- Строки интерфейса — в strings.xml, на русском.
- Не менять схему Room без миграции и теста.
- Новый код покрывать тестами. Перед завершением задачи:
  ./gradlew assembleDebug testDebugUnitTest lint checkstyle coverageVerify

## Заметки по сборке
- AGP 9: встроенная поддержка Kotlin выключена (`android.builtInKotlin=false` в gradle.properties).
- Checkstyle — задача `checkstyle`, конфигурация config/checkstyle/checkstyle.xml; отступы — .editorconfig.
  Правило можно отключить для одного элемента: `@SuppressWarnings("checkstyle:ИмяПравила")`.
- Схемы Room экспортируются в app/schemas — их нужно коммитить. Идентичность схемы проверяет MigrationTest.
- Никогда не использовать OnConflictStrategy.REPLACE для таблицы media: REPLACE удаляет строку
  и каскадно стирает актёров, жанры и ленты. Только @Upsert или частичный @Update.
- Все тесты в `src/test` на Robolectric: Room, ViewModel (`ViewModelTest`: InstantTaskExecutorRule и время
  главного потока), экраны и UI-сценарии (Espresso, Hilt, `FakeDataModule`, `UiTest`). Устройство не нужно.
  Локаль экранов в тестах — `ru` (`@Config(qualifiers)`).
- Фейковые списки Paging — `FakeMediaRepository.pages(...)`: статические данные должны сообщать
  завершённую загрузку, иначе адаптер не вызывает слушателя состояний.
- ATF под Robolectric ничего не находит — доступность проверяет `AccessibilityTest` (48 dp и подписи
  кликабельных View, в том числе при шрифте 150%).
- Покрытие JaCoCo: `coverageReport` (HTML), `coverageVerify` (≥ 60% строк в core/data и core/database).
- Release: R8 и сжатие ресурсов, правила для Gson-записей в app/proguard-rules.pro, подпись debug-ключом.
