package ru.kinopolka.core.data.mapper;

import androidx.annotation.Nullable;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import ru.kinopolka.core.model.CastMember;
import ru.kinopolka.core.model.Media;
import ru.kinopolka.core.model.MediaDetails;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaType;
import ru.kinopolka.core.model.TmdbGenre;
import ru.kinopolka.core.network.model.AggregateCastDto;
import ru.kinopolka.core.network.model.CastDto;
import ru.kinopolka.core.network.model.GenreDto;
import ru.kinopolka.core.network.model.GenreListResponseDto;
import ru.kinopolka.core.network.model.MediaListItemDto;
import ru.kinopolka.core.network.model.MovieDetailsDto;
import ru.kinopolka.core.network.model.PagedResponseDto;
import ru.kinopolka.core.network.model.RoleDto;
import ru.kinopolka.core.network.model.TvDetailsDto;

/** TMDB DTO → domain models. */
public final class NetworkMappers {

    /** F-09: the title card shows the first 10 actors. */
    public static final int MAX_CAST_MEMBERS = 10;

    private NetworkMappers() {
    }

    /** TMDB dates are {@code yyyy-MM-dd}; an empty or malformed date means «unknown». */
    @Nullable
    public static LocalDate toLocalDateOrNull(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }

    @Nullable
    private static String nullIfBlank(@Nullable String value) {
        return value == null || value.isBlank() ? null : value;
    }

    @Nullable
    private static String firstNonNull(@Nullable String first, @Nullable String second) {
        return first != null ? first : second;
    }

    private static <T> T orDefault(@Nullable T value, T fallback) {
        return value != null ? value : fallback;
    }

    @Nullable
    public static Media toMediaOrNull(MediaListItemDto dto) {
        return toMediaOrNull(dto, null);
    }

    /**
     * Maps a list item to {@link Media}. The type comes from {@code media_type} when TMDB sends it
     * (search/multi, trending) and from {@code defaultType} otherwise (popular, discover).
     * Returns {@code null} for people (F-01), unknown types and items without id or title.
     */
    @Nullable
    public static Media toMediaOrNull(MediaListItemDto dto, @Nullable MediaType defaultType) {
        MediaType type = dto.mediaType() != null ? MediaType.fromKey(dto.mediaType()) : defaultType;
        Integer id = dto.id();
        if (type == null || id == null) {
            return null;
        }
        // Movies use title and release_date, series name and first_air_date.
        boolean movie = type == MediaType.MOVIE;
        String title = nullIfBlank(movie
                ? firstNonNull(dto.title(), dto.name())
                : firstNonNull(dto.name(), dto.title()));
        String originalTitle = nullIfBlank(movie
                ? firstNonNull(dto.originalTitle(), dto.originalName())
                : firstNonNull(dto.originalName(), dto.originalTitle()));
        String date = movie ? firstNonNull(dto.releaseDate(), dto.firstAirDate())
                : firstNonNull(dto.firstAirDate(), dto.releaseDate());
        String resolvedTitle = title != null ? title : originalTitle;
        if (resolvedTitle == null) {
            return null;
        }
        return new Media(
                new MediaKey(type, id),
                resolvedTitle,
                originalTitle,
                nullIfBlank(dto.overview()),
                nullIfBlank(dto.posterPath()),
                nullIfBlank(dto.backdropPath()),
                toLocalDateOrNull(date),
                orDefault(dto.voteAverage(), 0.0),
                orDefault(dto.voteCount(), 0),
                orDefault(dto.popularity(), 0.0),
                orDefault(dto.genreIds(), List.of()));
    }

    public static List<Media> toMediaList(@Nullable PagedResponseDto<MediaListItemDto> response) {
        return toMediaList(response, null);
    }

    /** Maps a page of results, dropping people and duplicates. */
    public static List<Media> toMediaList(@Nullable PagedResponseDto<MediaListItemDto> response,
            @Nullable MediaType defaultType) {
        List<Media> result = new ArrayList<>();
        if (response == null || response.results() == null) {
            return result;
        }
        Set<MediaKey> seen = new HashSet<>();
        for (MediaListItemDto dto : response.results()) {
            Media media = dto == null ? null : toMediaOrNull(dto, defaultType);
            if (media != null && seen.add(media.key())) {
                result.add(media);
            }
        }
        return result;
    }

    @Nullable
    public static TmdbGenre toTmdbGenreOrNull(GenreDto dto, MediaType type) {
        String name = nullIfBlank(dto.name());
        if (dto.id() == null || name == null) {
            return null;
        }
        return new TmdbGenre(type, dto.id(), name.trim());
    }

    public static List<TmdbGenre> toTmdbGenres(GenreListResponseDto response, MediaType type) {
        List<TmdbGenre> result = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (TmdbGenre genre : toTmdbGenreList(response.genres(), type)) {
            if (seen.add(genre.id())) {
                result.add(genre);
            }
        }
        return result;
    }

    private static List<TmdbGenre> toTmdbGenreList(@Nullable List<GenreDto> genres, MediaType type) {
        List<TmdbGenre> result = new ArrayList<>();
        if (genres != null) {
            for (GenreDto dto : genres) {
                TmdbGenre genre = dto == null ? null : toTmdbGenreOrNull(dto, type);
                if (genre != null) {
                    result.add(genre);
                }
            }
        }
        return result;
    }

    private static List<Integer> ids(List<TmdbGenre> genres) {
        List<Integer> ids = new ArrayList<>(genres.size());
        for (TmdbGenre genre : genres) {
            ids.add(genre.id());
        }
        return ids;
    }

    @Nullable
    private static Integer positiveOrNull(@Nullable Integer value) {
        return value != null && value > 0 ? value : null;
    }

    @Nullable
    public static MediaDetails toMediaDetails(MovieDetailsDto dto) {
        if (dto.id() == null) {
            return null;
        }
        List<TmdbGenre> genres = toTmdbGenreList(dto.genres(), MediaType.MOVIE);
        Media media = toMediaOrNull(new MediaListItemDto(dto.id(), MediaType.MOVIE.key(), dto.title(), null,
                dto.originalTitle(), null, dto.overview(), dto.posterPath(), dto.backdropPath(), dto.releaseDate(),
                null, dto.voteAverage(), dto.voteCount(), dto.popularity(), ids(genres), null));
        if (media == null) {
            return null;
        }
        List<CastDto> cast = dto.credits() == null || dto.credits().cast() == null ? List.of() : dto.credits().cast();
        return new MediaDetails(
                media,
                genres,
                positiveOrNull(dto.runtime()),
                null,
                null,
                null,
                false,
                toCastMembers(cast),
                toMediaList(dto.recommendations(), MediaType.MOVIE),
                toMediaList(dto.similar(), MediaType.MOVIE),
                true);
    }

    @Nullable
    public static MediaDetails toMediaDetails(TvDetailsDto dto) {
        if (dto.id() == null) {
            return null;
        }
        List<TmdbGenre> genres = toTmdbGenreList(dto.genres(), MediaType.TV);
        Media media = toMediaOrNull(new MediaListItemDto(dto.id(), MediaType.TV.key(), null, dto.name(), null,
                dto.originalName(), dto.overview(), dto.posterPath(), dto.backdropPath(), null, dto.firstAirDate(),
                dto.voteAverage(), dto.voteCount(), dto.popularity(), ids(genres), null));
        if (media == null) {
            return null;
        }
        List<AggregateCastDto> cast = dto.aggregateCredits() == null || dto.aggregateCredits().cast() == null
                ? List.of() : dto.aggregateCredits().cast();
        return new MediaDetails(
                media,
                genres,
                null,
                positiveOrNull(dto.numberOfSeasons()),
                toLocalDateOrNull(dto.lastAirDate()),
                dto.inProduction(),
                false,
                toAggregateCastMembers(cast),
                // Recommendations of a series may contain movies, so the item type is kept when present.
                toMediaList(dto.recommendations(), MediaType.TV),
                toMediaList(dto.similar(), MediaType.TV),
                true);
    }

    /** First {@link #MAX_CAST_MEMBERS} actors in billing order; an actor with two roles is listed once. */
    public static List<CastMember> toCastMembers(List<CastDto> cast) {
        List<CastMember> result = new ArrayList<>();
        for (int index = 0; index < cast.size(); index++) {
            CastDto dto = cast.get(index);
            String name = dto == null ? null : nullIfBlank(dto.name());
            if (name == null || dto.id() == null) {
                continue;
            }
            result.add(new CastMember(dto.id(), name, nullIfBlank(dto.character()), nullIfBlank(dto.profilePath()),
                    orDefault(dto.order(), index)));
        }
        return normalizeCast(result);
    }

    /** Series cast: the role with the most episodes is shown. */
    public static List<CastMember> toAggregateCastMembers(List<AggregateCastDto> cast) {
        List<CastMember> result = new ArrayList<>();
        for (int index = 0; index < cast.size(); index++) {
            AggregateCastDto dto = cast.get(index);
            String name = dto == null ? null : nullIfBlank(dto.name());
            if (name == null || dto.id() == null) {
                continue;
            }
            RoleDto mainRole = null;
            if (dto.roles() != null) {
                for (RoleDto role : dto.roles()) {
                    if (role == null || role.character() == null || role.character().isBlank()) {
                        continue;
                    }
                    if (mainRole == null || orDefault(role.episodeCount(), 0) > orDefault(mainRole.episodeCount(), 0)) {
                        mainRole = role;
                    }
                }
            }
            result.add(new CastMember(dto.id(), name, mainRole == null ? null : mainRole.character().trim(),
                    nullIfBlank(dto.profilePath()), orDefault(dto.order(), index)));
        }
        return normalizeCast(result);
    }

    private static List<CastMember> normalizeCast(List<CastMember> cast) {
        List<CastMember> sorted = new ArrayList<>(cast);
        sorted.sort(Comparator.comparingInt(CastMember::order));
        List<CastMember> result = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (CastMember member : sorted) {
            if (result.size() == MAX_CAST_MEMBERS) {
                break;
            }
            if (seen.add(member.personId())) {
                result.add(member);
            }
        }
        return result;
    }
}
