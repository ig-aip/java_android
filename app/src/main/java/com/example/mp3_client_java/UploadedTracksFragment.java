package com.example.mp3_client_java;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.provider.Settings;
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
import com.example.mp3_client_java.network.response.DownloadResponse;
import com.example.mp3_client_java.network.response.MusicListResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadedTracksFragment extends Fragment {

    private RecyclerView rvUploadedTracks;
    private MediaPlayer mediaPlayer;

    public UploadedTracksFragment() {
        super(R.layout.fragment_uploaded_tracks);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        rvUploadedTracks = view.findViewById(R.id.rv_uploaded_tracks);
        rvUploadedTracks.setLayoutManager(new LinearLayoutManager(requireContext()));
    }

    // Загружаем данные каждый раз, когда вкладка становится видимой
    @Override
    public void onResume() {
        super.onResume();
        loadMyTracks();
    }

    private void loadMyTracks() {
        TokenManager tManager = new TokenManager(requireContext());
        Map<String, String> body = new HashMap<>();
        body.put("access_token", tManager.getAccessToken());

        NetworkApi.getSINGLTON().getApi().getMyMusic(body).enqueue(new Callback<MusicListResponse>() {
            @Override
            public void onResponse(Call<MusicListResponse> call, Response<MusicListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<MusicFile> tracks = response.body().getMusic_list();
                    TrackAdapter adapter = new TrackAdapter(tracks, track -> playTrack(track));
                    rvUploadedTracks.setAdapter(adapter);
                }

                else if (response.code() == 401 || response.code() == 403) {
                    refreshTokensAndRetry(null);
                }
            }
            @Override
            public void onFailure(Call<MusicListResponse> call, Throwable t) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(requireContext(), "Ошибка: loadMyTracks", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    // Проигрывание (такое же как в HomeFragment)
    private void playTrack(MusicFile track) {
        TokenManager tManager = new TokenManager(requireContext());
        Map<String, String> body = new HashMap<>();
        body.put("access_token", tManager.getAccessToken());
        body.put("s3_path", track.getS3_path());

        NetworkApi.getSINGLTON().getApi().getDownloadLink(body).enqueue(new Callback<DownloadResponse>() {
            @Override
            public void onResponse(Call<DownloadResponse> call, Response<DownloadResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String url = response.body().getDownload_link().replace("127.0.0.1", "10.0.2.2");
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).playGlobalTrack(url, track);
                    }
                }else if (response.code() == 401 || response.code() == 403) {
                    refreshTokensAndRetry(track);
                }
            }
            @Override
            public void onFailure(Call<DownloadResponse> call, Throwable t) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(requireContext(), "Ошибка: playTrack", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    private void startAudioPlayer(String url) {
        try {
            if (mediaPlayer != null) mediaPlayer.release();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(MediaPlayer::start);
        } catch (Exception e) { e.printStackTrace(); }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mediaPlayer != null) mediaPlayer.release();
    }

    private void refreshTokensAndRetry(MusicFile track) {
        TokenManager tManager = new TokenManager(requireContext());
        String deviceId = Settings.Secure.getString(requireContext().getContentResolver(), Settings.Secure.ANDROID_ID);

        Map<String, String> refreshBody = new HashMap<>();
        refreshBody.put("refresh_token", tManager.getRefreshToken());
        refreshBody.put("device_id", deviceId);

        NetworkApi.getSINGLTON().getApi().refreshToken(refreshBody).enqueue(new Callback<JwtPair>() {
            @Override
            public void onResponse(Call<JwtPair> call, Response<JwtPair> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getAccess_token() != null) {
                    tManager.saveTokens(response.body().getAccess_token(), response.body().getRefresh_token());

                    if(track == null){
                        loadMyTracks();
                    }else {
                        playTrack(track);
                    }
                } else {
                    tManager.clear();
                    startActivity(new Intent(requireContext(), LoginActivity.class));
                    requireActivity().finish();
                }
            }

            @Override
            public void onFailure(Call<JwtPair> call, Throwable t) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(requireContext(), "Нет интернета refreshTokensAndRetry", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }
}