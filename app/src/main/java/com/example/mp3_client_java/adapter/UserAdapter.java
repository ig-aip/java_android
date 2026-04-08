package com.example.mp3_client_java.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mp3_client_java.R;
import com.example.mp3_client_java.network.response.UserListResponse;
import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {
    private List<UserListResponse.User> users;
    public UserAdapter(List<UserListResponse.User> users) { this.users = users; }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new UserViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_user, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserListResponse.User user = users.get(position);
        holder.tvUsername.setText(user.username);
        holder.tvInitial.setText(user.username != null && !user.username.isEmpty() ? String.valueOf(user.username.charAt(0)).toUpperCase() : "?");
        holder.tvBio.setText("User description will be implemented here."); // Заглушка, как ты и просил
    }

    @Override public int getItemCount() { return users == null ? 0 : users.size(); }

    static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitial, tvUsername, tvBio, tvTracksCount;
        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitial = itemView.findViewById(R.id.tvUserInitial);
            tvUsername = itemView.findViewById(R.id.tvUsername);
            tvBio = itemView.findViewById(R.id.tvBio);
            tvTracksCount = itemView.findViewById(R.id.tvTracksCount);
        }
    }
}