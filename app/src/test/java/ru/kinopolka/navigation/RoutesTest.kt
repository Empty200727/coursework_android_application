package ru.kinopolka.navigation

import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.kinopolka.core.model.MediaFilter
import ru.kinopolka.core.model.MediaKey
import ru.kinopolka.core.model.MediaType

class RoutesTest {

    @Test
    fun `details route keeps type and id`() {
        val route = DetailsRoute(MediaKey(MediaType.TV, 1396))

        assertEquals("tv", route.mediaTypeKey)
        assertEquals(MediaKey(MediaType.TV, 1396), route.mediaKey)
    }

    @Test
    fun `genre route keeps the filter`() {
        assertEquals(MediaFilter.SERIES, GenreRoute("drama", MediaFilter.SERIES).mediaFilter)
        assertEquals(MediaFilter.ALL, GenreRoute("drama").mediaFilter)
        assertEquals(MediaFilter.ALL, GenreRoute("drama", filter = "broken").mediaFilter)
    }

    @Test
    fun `only constructor arguments are serialized`() {
        val json = Json.encodeToString(DetailsRoute.serializer(), DetailsRoute("movie", 550))

        assertEquals("""{"mediaTypeKey":"movie","id":550}""", json)
    }
}
