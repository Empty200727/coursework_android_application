package ru.kinopolka.core.ui;

import android.view.View;
import androidx.annotation.Nullable;
import ru.kinopolka.R;
import ru.kinopolka.core.data.DataError;
import ru.kinopolka.databinding.ViewStateBinding;

/** «Пусто» and «Ошибка» states (N-06). */
public final class StateViews {

    private StateViews() {
    }

    public static void showMessage(ViewStateBinding binding, CharSequence message) {
        show(binding, message, null, null);
    }

    public static void show(ViewStateBinding binding, CharSequence message, @Nullable CharSequence actionLabel,
            @Nullable Runnable action) {
        binding.getRoot().setVisibility(View.VISIBLE);
        binding.stateMessage.setText(message);
        if (actionLabel != null && action != null) {
            binding.stateAction.setVisibility(View.VISIBLE);
            binding.stateAction.setText(actionLabel);
            binding.stateAction.setOnClickListener(v -> action.run());
        } else {
            binding.stateAction.setVisibility(View.GONE);
            binding.stateAction.setOnClickListener(null);
        }
    }

    /** «Ошибка» with «Повторить». */
    public static void showError(ViewStateBinding binding, DataError error, Runnable onRetry) {
        android.content.Context context = binding.getRoot().getContext();
        show(binding, context.getString(Formats.errorMessage(error)), context.getString(R.string.action_retry),
                onRetry);
    }

    public static void hide(ViewStateBinding binding) {
        binding.getRoot().setVisibility(View.GONE);
    }

    public static void setVisible(View view, boolean visible) {
        view.setVisibility(visible ? View.VISIBLE : View.GONE);
    }
}
