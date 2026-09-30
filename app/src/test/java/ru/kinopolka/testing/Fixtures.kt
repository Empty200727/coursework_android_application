package ru.kinopolka.testing

import ru.kinopolka.core.network.TmdbJson

/** JSON responses of TMDB stored in src/test/resources/fixtures. */
object Fixtures {
    fun read(name: String): String {
        val resource = requireNotNull(javaClass.classLoader?.getResource("fixtures/$name")) {
            "Fixture $name not found"
        }
        return resource.readText()
    }

    inline fun <reified T> parse(name: String): T = TmdbJson.decodeFromString(read(name))
}
