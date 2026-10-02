package ru.kinopolka.core.data.library;

import androidx.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.HashSet;
import java.util.Set;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okio.BufferedSink;
import okio.Okio;
import ru.kinopolka.core.model.MediaKey;
import ru.kinopolka.core.network.ImageSize;
import ru.kinopolka.core.network.TmdbNetwork;

public final class FilePosterStorage implements PosterStorage {

    private final File directory;
    private final OkHttpClient client;
    private final String imageBaseUrl;

    public FilePosterStorage(File directory, OkHttpClient client, String imageBaseUrl) {
        this.directory = directory;
        this.client = client;
        this.imageBaseUrl = imageBaseUrl;
    }

    @Nullable
    @Override
    public String save(MediaKey key, String posterPath) {
        String url = TmdbNetwork.imageUrl(posterPath, ImageSize.POSTER_LIST, imageBaseUrl);
        if (url == null) {
            return null;
        }
        File target = new File(directory, key.mediaType().key() + "_" + key.tmdbId() + ".jpg");
        File partial = new File(directory, target.getName() + ".part");
        try {
            if (!directory.isDirectory() && !directory.mkdirs()) {
                return null;
            }
            try (Response response = client.newCall(new Request.Builder().url(url).build()).execute()) {
                if (!response.isSuccessful()) {
                    return null;
                }
                try (BufferedSink sink = Okio.buffer(Okio.sink(partial))) {
                    sink.writeAll(response.body().source());
                }
            }
            Files.move(partial.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return target.getAbsolutePath();
        } catch (IOException ignored) {
            // No poster file: the library shows the network image and saving is retried later.
            deleteQuietly(partial);
            return null;
        }
    }

    @Override
    public boolean exists(String path) {
        return new File(path).isFile();
    }

    @Override
    public void delete(String path) {
        File file = new File(path);
        // Only files of this storage may be removed.
        if (isInDirectory(file)) {
            deleteQuietly(file);
        }
    }

    @Override
    public int deleteAllExcept(Set<String> keep) {
        Set<String> kept = new HashSet<>();
        for (String path : keep) {
            kept.add(canonicalPath(new File(path)));
        }
        File[] files = directory.listFiles();
        if (files == null) {
            return 0;
        }
        int deleted = 0;
        for (File file : files) {
            if (file.isFile() && !kept.contains(canonicalPath(file)) && file.delete()) {
                deleted++;
            }
        }
        return deleted;
    }

    private boolean isInDirectory(File file) {
        File parent = file.getParentFile();
        return parent != null && canonicalPath(parent).equals(canonicalPath(directory));
    }

    private static String canonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException ignored) {
            return file.getAbsolutePath();
        }
    }

    private static void deleteQuietly(File file) {
        try {
            Files.deleteIfExists(file.toPath());
        } catch (IOException ignored) {
            // A leftover file is removed by the next cache cleanup.
        }
    }
}
