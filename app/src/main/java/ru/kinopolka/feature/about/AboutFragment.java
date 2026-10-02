package ru.kinopolka.feature.about;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import dagger.hilt.android.AndroidEntryPoint;
import ru.kinopolka.BuildConfig;
import ru.kinopolka.R;
import ru.kinopolka.databinding.FragmentAboutBinding;
import ru.kinopolka.navigation.Navigator;

/** «О приложении» (F-14): version and the TMDB attribution. */
@AndroidEntryPoint
public class AboutFragment extends Fragment {

    static final String TMDB_URL = "https://www.themoviedb.org";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        FragmentAboutBinding binding = FragmentAboutBinding.inflate(inflater, container, false);
        binding.toolbar.setNavigationOnClickListener(v -> Navigator.back(this));
        binding.version.setText(getString(R.string.about_version, BuildConfig.VERSION_NAME));
        binding.tmdbLink.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(TMDB_URL)));
            } catch (ActivityNotFoundException ignored) {
                // No browser on the device: the link text is still visible.
            }
        });
        return binding.getRoot();
    }
}
