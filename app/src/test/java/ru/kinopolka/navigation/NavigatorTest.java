package ru.kinopolka.navigation;

import static org.junit.Assert.assertEquals;

import android.os.Bundle;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import ru.kinopolka.core.model.MediaFilter;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.model.MediaType;

/** Only primitive arguments are passed between screens (docs/PLAN.md, section 7). */
@RunWith(AndroidJUnit4.class)
public class NavigatorTest {

    @Test
    public void detailsArgumentsKeepTypeAndId() {
        Bundle args = Navigator.detailsArgs(new MediaKey(MediaType.TV, 1396));

        assertEquals("tv", args.getString(Navigator.ARG_MEDIA_TYPE));
        assertEquals(1396, args.getInt(Navigator.ARG_ID));
        assertEquals(new MediaKey(MediaType.TV, 1396), Navigator.mediaKey("tv", 1396));
        assertEquals("unknown type falls back to a movie", new MediaKey(MediaType.MOVIE, 1),
                Navigator.mediaKey("person", 1));
    }

    @Test
    public void genreArgumentsKeepTheFilter() {
        Bundle args = Navigator.genreArgs("drama", MediaFilter.SERIES);

        assertEquals("drama", args.getString(Navigator.ARG_GENRE_KEY));
        assertEquals("SERIES", args.getString(Navigator.ARG_FILTER));
        assertEquals(2, args.size());
    }
}
