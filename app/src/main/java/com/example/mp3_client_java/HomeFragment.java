package com.example.mp3_client_java;

// ...импорты...
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mp3_client_java.adapter.TrackAdapter;
import com.example.mp3_client_java.music.MusicFile;
import com.example.mp3_client_java.network.JwtPair;
import com.example.mp3_client_java.network.NetworkApi;
import com.example.mp3_client_java.network.TokenManager;
import com.example.mp3_client_java.network.response.MusicListResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private RecyclerView rvPopularTracks;

    public HomeFragment() { super(R.layout.fragment_home); }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvPopularTracks = view.findViewById(R.id.rv_popular_tracks);
        rvPopularTracks.setLayoutManager(new LinearLayoutManager(requireContext()));
        loadPopularTracks();
    }

    private void loadPopularTracks() {
        TokenManager tManager = new TokenManager(requireContext());
        Map<String, String> body = new HashMap<>();
        body.put("access_token", tManager.getAccessToken());

        NetworkApi.getSINGLTON().getApi().getPopularMusic(body).enqueue(new Callback<MusicListResponse>() {
            @Override
            public void onResponse(Call<MusicListResponse> call, Response<MusicListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MusicFile> tracks = response.body().getMusic_list();
                    // ПЕРЕДАЕМ ВЕСЬ ПЛЕЙЛИСТ И ИНДЕКС В MAIN ACTIVITY
                    TrackAdapter adapter = new TrackAdapter(tracks, track -> {
                        if (getActivity() instanceof MainActivity) {
                            int index = tracks.indexOf(track);
                            ((MainActivity) getActivity()).playTrackList(tracks, index);
                        }
                    });
                    rvPopularTracks.setAdapter(adapter);
                } else if (response.code() == 401 || response.code() == 403) {
                    refreshTokensAndRetry();
                }
            }

            @Override
            public void onFailure(Call<MusicListResponse> call, Throwable t) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Ошибка сети loudPopularTracks", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void refreshTokensAndRetry() {
        if (getContext() == null) return;
        TokenManager tManager = new TokenManager(getContext());
        String deviceId = Settings.Secure.getString(getContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        Map<String, String> refreshBody = new HashMap<>();
        refreshBody.put("refresh_token", tManager.getRefreshToken());
        refreshBody.put("device_id", deviceId);

        NetworkApi.getSINGLTON().getApi().refreshToken(refreshBody).enqueue(new Callback<JwtPair>() {
            @Override
            public void onResponse(Call<JwtPair> call, Response<JwtPair> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getAccess_token() != null) {
                    if (getContext() != null) {
                        tManager.saveTokens(response.body().getAccess_token(), response.body().getRefresh_token());
                        loadPopularTracks();
                    }
                } else {
                    if (getContext() != null) {
                        tManager.clear();
                        startActivity(new Intent(getContext(), LoginActivity.class));
                        if (getActivity() != null) getActivity().finish();
                    }
                }
            }

            @Override
            public void onFailure(Call<JwtPair> call, Throwable t) {}
        });
    }
}