package ru.kinopolka.testing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LifecycleRegistry;
import androidx.lifecycle.LiveData;
import androidx.paging.AsyncPagingDataDiffer;
import androidx.paging.PagingData;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListUpdateCallback;
import java.util.List;

/** Loads the first page of paged results, like a list on screen would. */
public final class PagingTesting {

    private PagingTesting() {
    }

    public static <T> List<T> firstPage(LiveData<PagingData<T>> data, DiffUtil.ItemCallback<T> diff) {
        AsyncPagingDataDiffer<T> differ = new AsyncPagingDataDiffer<>(diff, new NoUpdates());
        ResumedOwner owner = new ResumedOwner();
        data.observe(owner, page -> differ.submitData(owner.getLifecycle(), page));
        long deadline = System.currentTimeMillis() + 5_000;
        while (differ.getItemCount() == 0 && System.currentTimeMillis() < deadline) {
            LiveDataTesting.idleMain();
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        List<T> items = differ.snapshot().getItems();
        owner.destroy();
        return items;
    }

    private static final class ResumedOwner implements LifecycleOwner {
        private final LifecycleRegistry registry = new LifecycleRegistry(this);

        ResumedOwner() {
            registry.setCurrentState(Lifecycle.State.RESUMED);
        }

        void destroy() {
            registry.setCurrentState(Lifecycle.State.DESTROYED);
        }

        @NonNull
        @Override
        public Lifecycle getLifecycle() {
            return registry;
        }
    }

    private static final class NoUpdates implements ListUpdateCallback {
        @Override
        public void onInserted(int position, int count) {
            // Only the snapshot is read.
        }

        @Override
        public void onRemoved(int position, int count) {
            // Only the snapshot is read.
        }

        @Override
        public void onMoved(int fromPosition, int toPosition) {
            // Only the snapshot is read.
        }

        @Override
        public void onChanged(int position, int count, @Nullable Object payload) {
            // Only the snapshot is read.
        }
    }
}
