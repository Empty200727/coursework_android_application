package ru.kinopolka.core.data.mapper;

import androidx.annotation.Nullable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import ru.kinopolka.core.database.entity.CastMemberEntity;
import ru.kinopolka.core.database.entity.FeedItemEntity;
import ru.kinopolka.core.database.entity.GenreEntity;
import ru.kinopolka.core.database.entity.LibraryEntryEntity;
import ru.kinopolka.core.database.entity.MediaEntity;
import ru.kinopolka.core.database.entity.MediaGenreEntity;
import ru.kinopolka.core.database.entity.RelatedMediaEntity;
import ru.kinopolka.core.model.CastMember;
import ru.kinopolka.core.model.LibraryEntry;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaDetails;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.RelatedKind;
import ru.kinopolka.core.model.TmdbGenre;

/** Domain models ↔ Room entities. */
public final class EntityMappers {

    private EntityMappers() {
    }

    @Nullable
    private static String dateToString(@Nullable LocalDate date) {
        return date == null ? null : date.toString();
    }

    /** A list item: detail columns stay empty until the title card is opened. */
    public static MediaEntity toEntity(Media media, long cachedAt) {
        return new MediaEntity(media.mediaType(), media.tmdbId(), media.title(), media.originalTitle(),
                media.overview(), false, media.posterPath(), media.backdropPath(),
                dateToString(media.releaseDate()), null, media.voteAverage(), media.voteCount(), media.popularity(),
                null, null, null, cachedAt, null);
    }

    public static MediaEntity toEntity(MediaDetails details, long cachedAt) {
        Media media = details.media();
        return new MediaEntity(media.mediaType(), media.tmdbId(), media.title(), media.originalTitle(),
                media.overview(), details.overviewFallback(), media.posterPath(), media.backdropPath(),
                dateToString(media.releaseDate()), dateToString(details.lastAirDate()), media.voteAverage(),
                media.voteCount(), media.popularity(), details.runtimeMinutes(), details.numberOfSeasons(),
                details.inProduction(), cachedAt, cachedAt);
    }

    public static Media toMedia(MediaEntity entity) {
        return toMedia(entity, List.of());
    }

    public static Media toMedia(MediaEntity entity, List<Integer> genreIds) {
        return new Media(
                new MediaKey(entity.mediaType, entity.tmdbId),
                entity.title,
                entity.originalTitle,
                entity.overview,
                entity.posterPath,
                entity.backdropPath,
                NetworkMappers.toLocalDateOrNull(entity.releaseDate),
                entity.voteAverage,
                entity.voteCount,
                entity.popularity,
                genreIds);
    }

    public static List<Media> toMediaList(List<MediaEntity> entities) {
        List<Media> result = new ArrayList<>(entities.size());
        for (MediaEntity entity : entities) {
            result.add(toMedia(entity));
        }
        return result;
    }

    public static List<MediaGenreEntity> toGenreEntities(Media media) {
        List<MediaGenreEntity> result = new ArrayList<>();
        for (Integer genreId : new LinkedHashSet<>(media.genreIds())) {
            result.add(new MediaGenreEntity(media.mediaType(), media.tmdbId(), genreId));
        }
        return result;
    }

    public static GenreEntity toEntity(TmdbGenre genre, long cachedAt) {
        return new GenreEntity(genre.mediaType(), genre.id(), genre.name(), cachedAt);
    }

    public static TmdbGenre toTmdbGenre(GenreEntity entity) {
        return new TmdbGenre(entity.mediaType, entity.genreId, entity.name);
    }

    public static CastMemberEntity toEntity(CastMember member, MediaKey key) {
        return new CastMemberEntity(key.mediaType(), key.tmdbId(), member.personId(), member.name(),
                member.character(), member.profilePath(), member.order());
    }

    public static CastMember toCastMember(CastMemberEntity entity) {
        return new CastMember(entity.personId, entity.name, entity.character, entity.profilePath, entity.castOrder);
    }

    public static List<RelatedMediaEntity> relatedEntities(MediaKey source, RelatedKind kind, List<Media> targets) {
        List<RelatedMediaEntity> result = new ArrayList<>();
        Set<MediaKey> seen = new HashSet<>();
        for (Media target : targets) {
            if (seen.add(target.key())) {
                result.add(new RelatedMediaEntity(source.mediaType(), source.tmdbId(), kind, target.mediaType(),
                        target.tmdbId(), result.size()));
            }
        }
        return result;
    }

    public static List<FeedItemEntity> feedItemEntities(String feedKey, List<Media> items, long fetchedAt) {
        List<FeedItemEntity> result = new ArrayList<>();
        Set<MediaKey> seen = new HashSet<>();
        for (Media media : items) {
            if (seen.add(media.key())) {
                result.add(new FeedItemEntity(feedKey, result.size(), media.mediaType(), media.tmdbId(), fetchedAt));
            }
        }
        return result;
    }

    public static LibraryEntryEntity toEntity(LibraryEntry entry) {
        return new LibraryEntryEntity(
                entry.key().mediaType(),
                entry.key().tmdbId(),
                entry.watchStatus(),
                entry.favorite(),
                entry.addedAt().toEpochMilli(),
                entry.watchedAt() == null ? null : entry.watchedAt().toEpochMilli(),
                entry.userRating(),
                entry.localPosterPath());
    }

    public static LibraryEntry toLibraryEntry(LibraryEntryEntity entity) {
        return new LibraryEntry(
                new MediaKey(entity.mediaType, entity.tmdbId),
                entity.watchStatus,
                entity.isFavorite,
                Instant.ofEpochMilli(entity.addedAt),
                entity.watchedAt == null ? null : Instant.ofEpochMilli(entity.watchedAt),
                entity.userRating,
                entity.localPosterPath);
    }

    /** Title card from the cache; works offline with whatever was stored (N-01). */
    public static MediaDetails toMediaDetails(
            MediaEntity entity,
            List<Integer> genreIds,
            Map<Integer, String> genreNames,
            List<CastMemberEntity> cast,
            List<MediaEntity> recommendations,
            List<MediaEntity> similar) {
        List<TmdbGenre> genres = new ArrayList<>();
        for (Integer id : genreIds) {
            String name = genreNames.get(id);
            if (name != null) {
                genres.add(new TmdbGenre(entity.mediaType, id, name));
            }
        }
        List<CastMember> members = new ArrayList<>(cast.size());
        for (CastMemberEntity member : cast) {
            members.add(toCastMember(member));
        }
        boolean movie = entity.mediaType == MediaType.MOVIE;
        return new MediaDetails(
                toMedia(entity, genreIds),
                genres,
                movie ? entity.runtime : null,
                movie ? null : entity.numberOfSeasons,
                NetworkMappers.toLocalDateOrNull(entity.lastAirDate),
                entity.inProduction,
                entity.isOverviewFallback,
                members,
                toMediaList(recommendations),
                toMediaList(similar),
                entity.detailsCachedAt != null);
    }
}
