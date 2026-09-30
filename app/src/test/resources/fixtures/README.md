# JSON-фикстуры TMDB

Ответы TMDB API v3 с `language=ru-RU` в реальном формате, сокращённые до нескольких элементов
(лишние поля оставлены намеренно: DTO должны их игнорировать). Чтобы заменить фикстуру свежим
ответом, выполните запрос с токеном и сохраните тело ответа в файл с тем же именем, например:

    curl -H "Authorization: Bearer $TMDB_TOKEN" \
      "https://api.themoviedb.org/3/movie/550?language=ru-RU&append_to_response=credits,recommendations,similar"

Тесты опираются на id, названия и число элементов в фикстурах — после замены обновите ожидания.
