package com.better.player;

import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.recyclerview.widget.RecyclerView;

import com.better.player.databinding.RecylerSpeedBinding;

import java.util.List;
import java.util.Optional;

@UnstableApi
public class SpeedRecyclerAdapter extends RecyclerView.Adapter<SpeedRecyclerAdapter.ViewHolder> {

    private final ExoPlayer player;
    private final List<SpeedItems> speeds;
    private SpeedProgress progress;

    public SpeedRecyclerAdapter(List<SpeedItems> speeds, ExoPlayer player) {
        this.speeds = speeds;
        this.player = player;
    }

    public void setChosen(int index, int Chosen) {
        speeds.get(index).setChoose(Chosen);
        notifyItemChanged(index);
    }

    public float getChoseSpeed() {
        int index = getIndexOfChosenSpeed();
        return (index != -1) ? speeds.get(index).getSpeed() : -1;
    }

    public int getIndexOfChosenSpeed() {
        return getSpeedItem(speeds, 1);
    }

    private int getSpeedItem(List<SpeedItems> items, Object value) {
        int position = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Optional<SpeedItems> foundItem = speeds.stream().filter(item -> item.getChoose() == (int) value).findFirst();
            if (foundItem.isPresent()) position = items.indexOf(foundItem.get());
        } else {
            position = speeds.indexOf(new SpeedItems((int) value));
        }
        return position;
    }

    public List<SpeedItems> getSpeedsData() {
        return speeds;
    }

    public void setSpeedProgress(SpeedProgress progress) {
        this.progress = progress;
    }

    @NonNull
    @Override
    public SpeedRecyclerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        RecylerSpeedBinding binding = RecylerSpeedBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull SpeedRecyclerAdapter.ViewHolder holder, int position) {

        SpeedItems speedItems = speeds.get(position);
        String name = speedItems.getName();
        int checks = speedItems.getChoose();
        float speed = speedItems.getSpeed();

        holder.binding.SpeedText.setText(name);
        if (checks == 0) {
            holder.binding.checked.setVisibility(View.GONE);
        } else if (checks == 1) {
            holder.binding.checked.setAlpha(1f);
            holder.binding.checked.setVisibility(View.VISIBLE);
        }

        holder.binding.baseLinear.setOnClickListener(v -> {
            if (checks == 0) {
                holder.binding.checked.setVisibility(View.VISIBLE);
                int index = getIndexOfChosenSpeed();
                if (index != -1) setChosen(getIndexOfChosenSpeed(), 0);
                setChosen(position, 1);
                if (progress != null) progress.animateSpeedProgress(speed);

                @SuppressLint("Recycle") ValueAnimator valueAnimator = ValueAnimator.ofFloat(0f, 1f);
                valueAnimator.addUpdateListener(animation -> holder.binding.checked.setAlpha((float) animation.getAnimatedValue()));
                player.setPlaybackSpeed(speed);
            }
        });
    }

    @Override
    public int getItemCount() {
        return speeds.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final RecylerSpeedBinding binding;

        public ViewHolder(@NonNull RecylerSpeedBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}
