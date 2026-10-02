package ru.kinopolka.core.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static ru.kinopolka.testing.LiveDataTesting.awaitValue;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.kinopolka.core.database.dao.CastDao;
import ru.kinopolka.core.database.dao.FeedDao;
import ru.kinopolka.core.database.dao.GenreDao;
import ru.kinopolka.core.database.dao.RelatedMediaDao;
import ru.kinopolka.core.database.entity.CastMemberEntity;
import ru.kinopolka.core.database.entity.FeedItemEntity;
import ru.kinopolka.core.database.entity.GenreEntity;
import ru.kinopolka.core.database.entity.RelatedMediaEntity;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.RelatedKind;

/** Genre, feed, cast and related-title tables. */
@RunWith(AndroidJUnit4.class)
public class CatalogDaoTest extends DatabaseTest {

    @Test
    public void genresAreReplacedPerMediaType() {
        GenreDao dao = database.genreDao();
        dao.upsert(List.of(
                new GenreEntity(MediaType.MOVIE, 28, "боевик", 100L),
                new GenreEntity(MediaType.MOVIE, 99, "устаревший", 50L),
                new GenreEntity(MediaType.TV, 10759, "Боевик и Приключения", 200L)));
        assertEquals(Long.valueOf(50L), dao.oldestCachedAt(MediaType.MOVIE));

        dao.replaceForType(MediaType.MOVIE, List.of(new GenreEntity(MediaType.MOVIE, 28, "боевик", 300L)));

        assertEquals(List.of("movie:28", "tv:10759"), awaitValue(dao.observeAll()).stream()
                .map(it -> it.mediaType.key() + ":" + it.genreId).toList());
        assertEquals(Long.valueOf(300L), dao.oldestCachedAt(MediaType.MOVIE));
        assertEquals(Long.valueOf(200L), dao.oldestCachedAt(MediaType.TV));
    }

    @Test
    public void noGenresMeansNoCacheTime() {
        assertNull(database.genreDao().oldestCachedAt(MediaType.TV));
    }

    @Test
    public void feedKeepsTheOrderOfPositions() {
        database.mediaDao().upsert(List.of(media(1), media(2), media(3, MediaType.TV)));
        FeedDao feedDao = database.feedDao();

        feedDao.replaceFeed("trending", List.of(
                new FeedItemEntity("trending", 0, MediaType.TV, 3, 500L),
                new FeedItemEntity("trending", 1, MediaType.MOVIE, 1, 500L)));
        feedDao.replaceFeed("popular_movies", List.of(new FeedItemEntity("popular_movies", 0, MediaType.MOVIE, 2,
                700L)));

        assertEquals(List.of(3, 1), awaitValue(feedDao.observeFeed("trending")).stream().map(it -> it.tmdbId).toList());
        assertEquals(Long.valueOf(500L), feedDao.fetchedAt("trending"));
        assertNull(feedDao.fetchedAt("unknown"));

        feedDao.replaceFeed("trending", List.of(new FeedItemEntity("trending", 0, MediaType.MOVIE, 2, 900L)));
        assertEquals(List.of(2), feedDao.getFeed("trending").stream().map(it -> it.tmdbId).toList());
        assertEquals(List.of(2), feedDao.getFeed("popular_movies").stream().map(it -> it.tmdbId).toList());
    }

    @Test
    public void castIsOrderedByBilling() {
        database.mediaDao().upsert(List.of(media(550)));
        CastDao castDao = database.castDao();

        castDao.replaceCast(MediaType.MOVIE, 550, List.of(
                new CastMemberEntity(MediaType.MOVIE, 550, 287, "Брэд Питт", "Тайлер Дёрден", null, 1),
                new CastMemberEntity(MediaType.MOVIE, 550, 819, "Эдвард Нортон", "Рассказчик", null, 0)));

        assertEquals(List.of(819, 287), awaitValue(castDao.observeCast(MediaType.MOVIE, 550)).stream()
                .map(it -> it.personId).toList());
    }

    @Test
    public void recommendationsAndSimilarTitlesAreStoredSeparately() {
        database.mediaDao().upsert(List.of(media(550), media(807), media(680), media(1954)));
        RelatedMediaDao dao = database.relatedMediaDao();

        dao.replaceRelated(MediaType.MOVIE, 550, RelatedKind.RECOMMENDATION, List.of(
                new RelatedMediaEntity(MediaType.MOVIE, 550, RelatedKind.RECOMMENDATION, MediaType.MOVIE, 680, 1),
                new RelatedMediaEntity(MediaType.MOVIE, 550, RelatedKind.RECOMMENDATION, MediaType.MOVIE, 807, 0)));
        dao.replaceRelated(MediaType.MOVIE, 550, RelatedKind.SIMILAR, List.of(
                new RelatedMediaEntity(MediaType.MOVIE, 550, RelatedKind.SIMILAR, MediaType.MOVIE, 1954, 0)));

        assertEquals(List.of(807, 680), awaitValue(dao.observeRelated(MediaType.MOVIE, 550,
                RelatedKind.RECOMMENDATION)).stream().map(it -> it.tmdbId).toList());
        assertEquals(List.of(1954), dao.getRelated(MediaType.MOVIE, 550, RelatedKind.SIMILAR).stream()
                .map(it -> it.tmdbId).toList());
    }
}
