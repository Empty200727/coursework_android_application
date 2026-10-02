# Аудит требований

Состояние после перевода приложения на Java (итерация 5). «Выполнено» — реализовано и покрыто
автотестом; «Частично» — реализовано, но окончательная проверка требует устройства. Пути указаны
относительно `app/src/main/java/ru/kinopolka`, тесты лежат в `app/src/test`.

## Функциональные требования

| ID | Требование | Статус | Код | Тесты |
| --- | --- | --- | --- | --- |
| F-01 | Поиск фильмов и сериалов одним запросом, без персон, 400 мс, от 2 символов, отмена предыдущего | Выполнено | [SearchViewModel](../app/src/main/java/ru/kinopolka/feature/search/SearchViewModel.java), [SearchPagingSource](../app/src/main/java/ru/kinopolka/core/data/paging/SearchPagingSource.java) | SearchViewModelTest, PagingSourcesTest, UserScenariosTest |
| F-02 | Фильтр «Все / Фильмы / Сериалы» на главной и в поиске, переживает поворот | Выполнено | [FilterChips](../app/src/main/java/ru/kinopolka/core/ui/FilterChips.java), SavedStateHandle в HomeViewModel и SearchViewModel | HomeViewModelTest, SearchViewModelTest, ScreensSmokeTest |
| F-03 | Бесконечная прокрутка (Paging 3) | Выполнено | [TmdbPagingSource](../app/src/main/java/ru/kinopolka/core/data/paging/TmdbPagingSource.java), [LoadStateFooterAdapter](../app/src/main/java/ru/kinopolka/core/ui/LoadStateFooterAdapter.java) | PagingSourcesTest, OfflineFirstMediaRepositoryTest |
| F-04 | Объединённый справочник жанров | Выполнено | [GenreCatalog](../app/src/main/java/ru/kinopolka/core/data/GenreCatalog.java), [OfflineFirstGenreRepository](../app/src/main/java/ru/kinopolka/core/data/repository/OfflineFirstGenreRepository.java) | GenreCatalogTest, OfflineFirstGenreRepositoryTest |
| F-05 | 6–8 полок жанров по 20, кнопка «Все» | Выполнено | [HomeShelves](../app/src/main/java/ru/kinopolka/feature/home/HomeShelves.java), [ShelfBinder](../app/src/main/java/ru/kinopolka/core/ui/ShelfBinder.java) | HomeShelvesTest, ScreensSmokeTest |
| F-06 | Экран жанра: сетка, пагинация, сортировка | Выполнено | [GenreFragment](../app/src/main/java/ru/kinopolka/feature/genre/GenreFragment.java), [GenrePagingSource](../app/src/main/java/ru/kinopolka/core/data/paging/GenrePagingSource.java) | GenreViewModelTest, PagingSourcesTest, ScreensSmokeTest |
| F-07 | Подборки «В тренде», «Популярные фильмы / сериалы» | Выполнено | [OfflineFirstMediaRepository](../app/src/main/java/ru/kinopolka/core/data/repository/OfflineFirstMediaRepository.java) | OfflineFirstMediaRepositoryTest |
| F-08 | Карточка: названия, год, тип, рейтинг и голоса, жанры, описание, длительность или сезоны | Выполнено | [DetailsFragment](../app/src/main/java/ru/kinopolka/feature/details/DetailsFragment.java), [OfflineFirstDetailsRepository](../app/src/main/java/ru/kinopolka/core/data/repository/OfflineFirstDetailsRepository.java) | OfflineFirstDetailsRepositoryTest, DetailsViewModelTest, ScreensSmokeTest |
| F-09 | 10 актёров: фото, имя, роль | Выполнено | [NetworkMappers](../app/src/main/java/ru/kinopolka/core/data/mapper/NetworkMappers.java) (`MAX_CAST_MEMBERS`), [CastAdapter](../app/src/main/java/ru/kinopolka/feature/details/CastAdapter.java) | NetworkMappersTest, OfflineFirstDetailsRepositoryTest |
| F-10 | Похожие: recommendations, при пустом ответе similar | Выполнено | [MediaDetails.related](../app/src/main/java/ru/kinopolka/core/model/MediaDetails.java) | OfflineFirstDetailsRepositoryTest |
| F-11 | «Хочу посмотреть», «Смотрел», «В избранное» из карточки | Выполнено | [DetailsViewModel](../app/src/main/java/ru/kinopolka/feature/details/DetailsViewModel.java), [LibraryRules](../app/src/main/java/ru/kinopolka/core/data/library/LibraryRules.java) | LibraryRulesTest, DetailsViewModelTest, UserScenariosTest |
| F-12 | «Моя полка»: вкладки со счётчиками, сортировка, работает без сети | Выполнено | [LibraryFragment](../app/src/main/java/ru/kinopolka/feature/library/LibraryFragment.java), [OfflineFirstLibraryRepository](../app/src/main/java/ru/kinopolka/core/data/repository/OfflineFirstLibraryRepository.java) | LibraryViewModelTest, LibraryRulesTest, UserScenariosTest |
| F-13 | Удаление свайпом или кнопкой, отмена через Snackbar | Выполнено | [SwipeToRemove](../app/src/main/java/ru/kinopolka/feature/library/SwipeToRemove.java), [LibraryFragment](../app/src/main/java/ru/kinopolka/feature/library/LibraryFragment.java) | LibraryViewModelTest, UserScenariosTest |
| F-14 | «О приложении»: логотип и атрибуция TMDB, версия | Выполнено | [AboutFragment](../app/src/main/java/ru/kinopolka/feature/about/AboutFragment.java), `res/drawable/tmdb_logo.xml` | ScreensSmokeTest |
| F-15…F-18 | Дополнительные функции | Не делались | — | — |

## Нефункциональные требования

| ID | Требование | Статус | Как обеспечено | Проверка |
| --- | --- | --- | --- | --- |
| N-01 | «Моя полка» и сохранённые карточки без сети, с постерами | Выполнено | Room — единый источник; постеры в `filesDir/posters` ([FilePosterStorage](../app/src/main/java/ru/kinopolka/core/data/library/FilePosterStorage.java)); очистка не трогает медиатеку | UserScenariosTest, CacheCleanerTest, FilePosterStorageTest; вручную — режим полёта |
| N-02 | Кэш и баннер без сети, автообновление при появлении сети | Выполнено | [ConnectivityManagerNetworkMonitor](../app/src/main/java/ru/kinopolka/core/data/util/ConnectivityManagerNetworkMonitor.java), `res/layout/view_offline_banner.xml` | HomeViewModelTest, DetailsViewModelTest, ScreensSmokeTest |
| N-03 | Жанры 7 дней, полки 6 ч, детали 24 ч, кэш изображений 250 МБ | Выполнено | [CachePolicy](../app/src/main/java/ru/kinopolka/core/data/CachePolicy.java), дисковый кэш Glide по умолчанию (250 МБ) | CachePolicyTest и тесты репозиториев с подменой времени |
| N-04 | Холодный старт до 2 с, плавная прокрутка, `w342` / `w500` | Частично | Размеры изображений по требованию, DiffUtil и Paging в списках, R8 в release | Время старта и плавность — замер в Android Studio Profiler на устройстве |
| N-05 | Состояние переживает поворот и смерть процесса | Выполнено | Запрос, фильтры, сортировка, вкладка — SavedStateHandle; прокрутка — состояние RecyclerView | SearchViewModelTest, HomeViewModelTest, LibraryViewModelTest; вручную — «Don't keep activities» |
| N-06 | Загрузка, пусто, ошибка с «Повторить» на каждом экране | Выполнено | [StateViews](../app/src/main/java/ru/kinopolka/core/ui/StateViews.java), скелеты в `res/layout/*skeleton*.xml` | ScreensSmokeTest |
| N-07 | `ru-RU`, английское описание с пометкой, строки в `strings.xml` | Выполнено | [TmdbRequestInterceptor](../app/src/main/java/ru/kinopolka/core/network/TmdbRequestInterceptor.java), `withOverview` в OfflineFirstDetailsRepository | TmdbApiTest, OfflineFirstDetailsRepositoryTest |
| N-08 | Описания изображений, зоны нажатия от 48 dp, крупный шрифт, TalkBack | Частично | Подписи у всех кнопок-иконок, постеры декоративны при наличии названия, один узел TalkBack на элемент | AccessibilityTest (48 dp и подписи, в том числе при шрифте 150%); контраст и TalkBack — Accessibility Scanner на устройстве |
| N-09 | Ключ в `local.properties` → `BuildConfig`, не в Git | Выполнено | `app/build.gradle.kts`, `.gitignore` | История Git проверена: токена нет |
| N-10 | Данные только на устройстве, без аналитики и сторонних SDK | Выполнено | Резервное копирование отключено (`data_extraction_rules.xml`, `backup_rules.xml`); в release нет аналитики; LeakCanary только в debug | Обзор зависимостей `releaseRuntimeClasspath` |

## Перевод на Java (итерация 5)

- Весь код и тесты переписаны на Java 17; интерфейс — фрагменты с XML-разметкой вместо Compose.
- Схема Room версии 1 не изменилась: `app/schemas/.../1.json` совпадает побайтно, MigrationTest проходит.
- Тесты нашли и помогли исправить две ошибки нового кода: двойное обновление данных при старте экрана
  и сохранение страниц старых поисковых запросов (кэш Paging применялся к каждому запросу отдельно).
- Проверки: 198 автотестов, Android Lint без замечаний, Checkstyle без замечаний, покрытие строк
  `core/data` и `core/database` — 94%, release-сборка с R8.

## Проверки, которые остаются ручными

Claude Code не видит экран устройства, поэтому перед защитой нужно вручную:

1. Режим полёта после сохранения 5 произведений: «Моя полка» и их карточки с постерами (N-01).
2. Переключение сети на эмуляторе: баннер и автообновление главной (N-02).
3. «Don't keep activities» в настройках разработчика: запрос, фильтр и прокрутка сохраняются (N-05).
4. Accessibility Scanner и TalkBack на главной, в поиске, карточке и «Моей полке» (N-08).
5. Холодный старт и прокрутка в Android Studio Profiler (N-04).
6. Установка `app-release.apk` на реальное устройство.
