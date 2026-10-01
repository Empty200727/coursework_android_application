# Аудит требований

Состояние на итерацию 4. «Выполнено» — реализовано и покрыто автотестом; «Частично» — реализовано,
но окончательная проверка требует устройства. Пути указаны относительно `app/src`.

## Функциональные требования

| ID | Требование | Статус | Код | Тесты |
| --- | --- | --- | --- | --- |
| F-01 | Поиск фильмов и сериалов одним запросом, без персон, 400 мс, от 2 символов, отмена предыдущего | Выполнено | [SearchViewModel](../app/src/main/java/ru/kinopolka/feature/search/SearchViewModel.kt), [SearchPagingSource](../app/src/main/java/ru/kinopolka/core/data/paging/SearchPagingSource.kt) | SearchViewModelTest, PagingSourcesTest, UserScenariosTest |
| F-02 | Фильтр «Все / Фильмы / Сериалы» на главной и в поиске, переживает поворот | Выполнено | [MediaFilterChips](../app/src/main/java/ru/kinopolka/core/ui/component/MediaFilterChips.kt), SavedStateHandle в HomeViewModel и SearchViewModel | HomeViewModelTest, SearchViewModelTest |
| F-03 | Бесконечная прокрутка (Paging 3) | Выполнено | [TmdbPagingSource](../app/src/main/java/ru/kinopolka/core/data/paging/TmdbPagingSource.kt) | PagingSourcesTest |
| F-04 | Объединённый справочник жанров | Выполнено | [GenreCatalog](../app/src/main/java/ru/kinopolka/core/data/GenreCatalog.kt), [OfflineFirstGenreRepository](../app/src/main/java/ru/kinopolka/core/data/repository/OfflineFirstGenreRepository.kt) | GenreCatalogTest, OfflineFirstGenreRepositoryTest |
| F-05 | 6–8 полок жанров по 20, кнопка «Все» | Выполнено | [HomeShelves](../app/src/main/java/ru/kinopolka/feature/home/HomeShelves.kt), [MediaShelf](../app/src/main/java/ru/kinopolka/core/ui/component/MediaShelf.kt) | HomeShelvesTest, ScreensSmokeTest |
| F-06 | Экран жанра: сетка, пагинация, сортировка | Выполнено | [GenreScreen](../app/src/main/java/ru/kinopolka/feature/genre/GenreScreen.kt), [GenrePagingSource](../app/src/main/java/ru/kinopolka/core/data/paging/GenrePagingSource.kt) | PagingSourcesTest, ScreensSmokeTest |
| F-07 | Подборки «В тренде», «Популярные фильмы / сериалы» | Выполнено | [OfflineFirstMediaRepository](../app/src/main/java/ru/kinopolka/core/data/repository/OfflineFirstMediaRepository.kt) | OfflineFirstMediaRepositoryTest |
| F-08 | Карточка: названия, год, тип, рейтинг и голоса, жанры, описание, длительность или сезоны | Выполнено | [DetailsScreen](../app/src/main/java/ru/kinopolka/feature/details/DetailsScreen.kt), [OfflineFirstDetailsRepository](../app/src/main/java/ru/kinopolka/core/data/repository/OfflineFirstDetailsRepository.kt) | OfflineFirstDetailsRepositoryTest, ScreensSmokeTest |
| F-09 | 10 актёров: фото, имя, роль | Выполнено | [NetworkMappers](../app/src/main/java/ru/kinopolka/core/data/mapper/NetworkMappers.kt) (`MAX_CAST_MEMBERS`) | NetworkMappersTest, OfflineFirstDetailsRepositoryTest |
| F-10 | Похожие: recommendations, при пустом ответе similar | Выполнено | [MediaDetails.related](../app/src/main/java/ru/kinopolka/core/model/MediaDetails.kt) | OfflineFirstDetailsRepositoryTest |
| F-11 | «Хочу посмотреть», «Смотрел», «В избранное» из карточки | Выполнено | [DetailsViewModel](../app/src/main/java/ru/kinopolka/feature/details/DetailsViewModel.kt), [LibraryRules](../app/src/main/java/ru/kinopolka/core/data/library/LibraryRules.kt) | LibraryRulesTest, DetailsViewModelTest, UserScenariosTest |
| F-12 | «Моя полка»: вкладки со счётчиками, сортировка, работает без сети | Выполнено | [LibraryScreen](../app/src/main/java/ru/kinopolka/feature/library/LibraryScreen.kt), [OfflineFirstLibraryRepository](../app/src/main/java/ru/kinopolka/core/data/repository/OfflineFirstLibraryRepository.kt) | LibraryViewModelTest, LibraryRulesTest, UserScenariosTest |
| F-13 | Удаление свайпом или кнопкой, отмена через Snackbar | Выполнено | [LibraryScreen](../app/src/main/java/ru/kinopolka/feature/library/LibraryScreen.kt) | LibraryViewModelTest, UserScenariosTest |
| F-14 | «О приложении»: логотип и атрибуция TMDB, версия | Выполнено | [AboutScreen](../app/src/main/java/ru/kinopolka/feature/about/AboutScreen.kt), `res/drawable/tmdb_logo.xml` | ScreensSmokeTest |
| F-15…F-18 | Дополнительные функции | Не делались | — | — |

## Нефункциональные требования

| ID | Требование | Статус | Как обеспечено | Проверка |
| --- | --- | --- | --- | --- |
| N-01 | «Моя полка» и сохранённые карточки без сети, с постерами | Выполнено | Room — единый источник; постеры в `filesDir/posters` ([PosterStorage](../app/src/main/java/ru/kinopolka/core/data/library/PosterStorage.kt)); очистка не трогает медиатеку | UserScenariosTest, CacheCleanerTest, FilePosterStorageTest; вручную — режим полёта |
| N-02 | Кэш и баннер без сети, автообновление при появлении сети | Выполнено | [NetworkMonitor](../app/src/main/java/ru/kinopolka/core/data/util/NetworkMonitor.kt), [OfflineBanner](../app/src/main/java/ru/kinopolka/core/ui/component/States.kt) | HomeViewModelTest, DetailsViewModelTest |
| N-03 | Жанры 7 дней, полки 6 ч, детали 24 ч, кэш изображений 250 МБ | Выполнено | [CachePolicy](../app/src/main/java/ru/kinopolka/core/data/CachePolicy.kt), [ImageLoaderModule](../app/src/main/java/ru/kinopolka/core/network/di/ImageLoaderModule.kt) | CachePolicyTest и тесты репозиториев с подменой времени |
| N-04 | Холодный старт до 2 с, плавная прокрутка, `w342` / `w500` | Частично | Размеры изображений по требованию, ключи и Paging в списках, R8 в release | Время старта и плавность — замер в Android Studio Profiler на устройстве |
| N-05 | Состояние переживает поворот и смерть процесса | Выполнено | Запрос, фильтры, сортировка, вкладка — SavedStateHandle; прокрутка — сохраняемые состояния списков | SearchViewModelTest, HomeViewModelTest, LibraryViewModelTest; вручную — «Don't keep activities» |
| N-06 | Загрузка, пусто, ошибка с «Повторить» на каждом экране | Выполнено | [States](../app/src/main/java/ru/kinopolka/core/ui/component/States.kt), [Skeletons](../app/src/main/java/ru/kinopolka/core/ui/component/Skeletons.kt) | ScreensSmokeTest |
| N-07 | `ru-RU`, английское описание с пометкой, строки в `strings.xml` | Выполнено | [TmdbRequestInterceptor](../app/src/main/java/ru/kinopolka/core/network/TmdbRequestInterceptor.kt), `withOverview` в OfflineFirstDetailsRepository | TmdbApiTest, OfflineFirstDetailsRepositoryTest |
| N-08 | Описания изображений, зоны нажатия от 48 dp, крупный шрифт, TalkBack | Частично | Подписи у всех кнопок-иконок, постеры декоративны при наличии названия, один узел TalkBack на элемент | AccessibilityTest (48 dp и подписи, в том числе при шрифте 150%); контраст и TalkBack — Accessibility Scanner на устройстве |
| N-09 | Ключ в `local.properties` → `BuildConfig`, не в Git | Выполнено | `app/build.gradle.kts`, `.gitignore` | История Git проверена: токена нет |
| N-10 | Данные только на устройстве, без аналитики и сторонних SDK | Выполнено | Резервное копирование отключено (`data_extraction_rules.xml`, `backup_rules.xml`); в release нет аналитики; LeakCanary только в debug | Обзор зависимостей `releaseRuntimeClasspath` |

## Найдено и исправлено в итерации 4

- Карточка поиска на главной давала TalkBack два узла: кнопку без названия и некликабельный текст.
  Теперь это одна кнопка с текстом.
- Release-сборка: включены R8 и сжатие ресурсов, добавлены правила, подпись debug-ключом для
  установки на устройство.

## Проверки, которые остаются ручными

Claude Code не видит экран устройства, поэтому перед защитой нужно вручную:

1. Режим полёта после сохранения 5 произведений: «Моя полка» и их карточки с постерами (N-01).
2. Переключение сети на эмуляторе: баннер и автообновление главной (N-02).
3. «Don't keep activities» в настройках разработчика: запрос, фильтр и прокрутка сохраняются (N-05).
4. Accessibility Scanner и TalkBack на главной, в поиске, карточке и «Моей полке» (N-08).
5. Холодный старт и прокрутка в Android Studio Profiler (N-04).
6. Установка `app-release.apk` на реальное устройство.
