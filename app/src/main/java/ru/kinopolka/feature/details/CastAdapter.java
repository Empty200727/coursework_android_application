package ru.kinopolka.feature.details;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import ru.kinopolka.core.model.CastMember;
import ru.kinopolka.core.network.ImageSize;
import ru.kinopolka.core.ui.Images;
import ru.kinopolka.databinding.ItemCastBinding;

/** F-09: first 10 actors with photo, name and role. */
final class CastAdapter extends ListAdapter<CastMember, CastAdapter.ViewHolder> {

    private static final DiffUtil.ItemCallback<CastMember> DIFF = new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull CastMember oldItem, @NonNull CastMember newItem) {
            return oldItem.personId() == newItem.personId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull CastMember oldItem, @NonNull CastMember newItem) {
            return oldItem.equals(newItem);
        }
    };

    CastAdapter() {
        super(DIFF);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemCastBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CastMember member = getItem(position);
        holder.binding.name.setText(member.name());
        holder.binding.character.setText(member.character());
        holder.binding.character.setVisibility(member.character() == null ? View.GONE : View.VISIBLE);
        Images.load(holder.binding.photo, member.profilePath(), ImageSize.PROFILE);
    }

    static final class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemCastBinding binding;

        ViewHolder(ItemCastBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
            binding.photo.setClipToOutline(true);
        }
    }
}
