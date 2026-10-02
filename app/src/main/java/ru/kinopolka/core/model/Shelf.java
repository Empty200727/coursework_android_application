package ru.kinopolka.core.model;

/** A horizontal collection of the home screen (F-05, F-07). */
public sealed interface Shelf permits Shelf.Trending, Shelf.Popular, Shelf.ByGenre {

    /** Stable key of the shelf in lists. */
    String key();

    /** «В тренде за неделю» for the selected filter. */
    record Trending(MediaFilter filter) implements Shelf {
        @Override
        public String key() {
            return "trending";
        }
    }

    /** «Популярные фильмы» or «Популярные сериалы». */
    record Popular(MediaType mediaType) implements Shelf {
        @Override
        public String key() {
            return "popular-" + mediaType.key();
        }
    }

    /** First 20 titles of a genre; with «Все» movies and series are merged by popularity. */
    record ByGenre(Genre genre, MediaFilter filter) implements Shelf {
        @Override
        public String key() {
            return "genre-" + genre.key();
        }
    }
}
