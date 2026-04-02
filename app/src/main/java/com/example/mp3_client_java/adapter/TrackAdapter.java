package com.example.mp3_client_java.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mp3_client_java.R;
import com.example.mp3_client_java.music.MusicFile;

import java.util.List;

public class TrackAdapter extends RecyclerView.Adapter<TrackAdapter.TrackViewHolder> {

    private List<MusicFile> musicList;
    private OnTrackClickListener clickListener;


    public interface OnTrackClickListener {
        void onTrackClick(MusicFile track);
    }
    public TrackAdapter(List<MusicFile> musicList, OnTrackClickListener listener){
        this.musicList = musicList;
        this.clickListener = listener;
    }

    @NonNull
    @Override
    public TrackViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_track, parent, false);
        return new TrackViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrackViewHolder holder, int position) {
        MusicFile track = musicList.get(position);

        holder.tvNumber.setText(String.valueOf(position + 1));
        holder.tvTitle.setText(track.getTitle());
        holder.tvArtist.setText(track.getArtist() != null ? track.getArtist() : "Unknown Artist");

        holder.itemView.setOnClickListener(v -> clickListener.onTrackClick(track));
    }

    @Override
    public int getItemCount() {
        if(musicList == null){
            return 0;
        }else{
            return musicList.size();
        }
    }

    public class TrackViewHolder extends RecyclerView.ViewHolder {
        TextView tvNumber, tvTitle, tvArtist, tvDuration;
        ImageView likeButton;

        public TrackViewHolder(@NonNull View itemView){
            super(itemView);

            tvNumber = itemView.findViewById(R.id.tv_track_number);
            tvTitle = itemView.findViewById(R.id.tv_track_title);
            tvArtist = itemView.findViewById(R.id.tv_track_artist);
            tvDuration = itemView.findViewById(R.id.tv_duration);

            likeButton = itemView.findViewById(R.id.btn_like);
        }

    }
}
