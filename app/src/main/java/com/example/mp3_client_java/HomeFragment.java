package com.example.mp3_client_java;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
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
import com.example.mp3_client_java.network.response.DownloadResponse;
import com.example.mp3_client_java.network.response.MusicListResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private RecyclerView rvPopularTracks;
    private MediaPlayer mediaPlayer; // Наш плеер

    public HomeFragment() {
        super(R.layout.fragment_home);
    }

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
                    updateUI(response.body().getMusic_list());
                }
                else if (response.code() == 401 || response.code() == 403) {

                    refreshTokensAndRetry();
                }
                else {
                    Log.e("Home", "Ошибка: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<MusicListResponse> call, Throwable t) {
                if(isAdded() && getContext() != null){
                    Toast.makeText(getContext(), "Ошибка сети loudPopularTracks", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void updateUI(List<MusicFile> tracks) {
        // ИСПОЛЬЗУЕМ TRACK ADAPTER, а не FragmentStateAdapter!
        TrackAdapter adapter = new TrackAdapter(tracks, track -> {
            // Что делать при клике на трек:
            playTrack(track);
        });
        rvPopularTracks.setAdapter(adapter);
    }

    // --- ЛОГИКА ВОСПРОИЗВЕДЕНИЯ ---
    private void playTrack(MusicFile track) {
        if (isAdded() && getContext() != null) {
            Toast.makeText(requireContext(), "Загрузка: " + track.getTitle(), Toast.LENGTH_SHORT).show();
        }

        TokenManager tManager = new TokenManager(requireContext());
        Map<String, String> body = new HashMap<>();
        body.put("access_token", tManager.getAccessToken());
        body.put("s3_path", track.getS3_path());

        // Запрашиваем ссылку на скачивание/стриминг
        NetworkApi.getSINGLTON().getApi().getDownloadLink(body).enqueue(new Callback<DownloadResponse>() {
            @Override
            public void onResponse(Call<DownloadResponse> call, Response<DownloadResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String minioUrl = response.body().getDownload_link();
                    // Меняем 127.0.0.1 на 10.0.2.2 для эмулятора Android
                    minioUrl = minioUrl.replace("127.0.0.1", "10.0.2.2");
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).playGlobalTrack(minioUrl, track);
                    }
                }else if (response.code() == 401 || response.code() == 403) {
                    refreshTokensAndRetry();
                }
            }

            @Override
            public void onFailure(Call<DownloadResponse> call, Throwable t) {
                if(isAdded() && getContext() != null){
                    Toast.makeText(getContext(), "Ошибка ссылки playTrack home Fragment", Toast.LENGTH_SHORT).show();
                }

            }
        });
    }

    private void startAudioPlayer(String url) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );

            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync(); // Загружаем музыку в фоне

            mediaPlayer.setOnPreparedListener(mp -> {
                mediaPlayer.start();
                Toast.makeText(requireContext(), "Играет!", Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {
            e.printStackTrace();

            if (isAdded() && getContext() != null) {
                Toast.makeText(getContext(), "Не удалось воспроизвести", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Освобождаем плеер при закрытии фрагмента
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
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
            public void onFailure(Call<JwtPair> call, Throwable t) {
                if (isAdded() && getContext() != null) {
                    Toast.makeText(getContext(), "Нет интернета refreshTokensAndRetry Home", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}