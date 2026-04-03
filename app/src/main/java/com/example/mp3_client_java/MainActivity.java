package com.example.mp3_client_java;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.mp3_client_java.music.MusicFile;
import com.example.mp3_client_java.network.NetworkApi;
import com.example.mp3_client_java.network.TokenManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {


    private MusicFile currentTrack;
    private MediaPlayer mediaPlayer;
    private SeekBar playerSeekBar;
    private TextView tvTrackTitle, tvTrackArtist;
    private ImageButton btnPlayPause, btnLike;

    private TextView tvCurrentTime, tvTotalTime;

    private boolean isLiked = false; // Локальный стейт для лайка
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                int currentPos = mediaPlayer.getCurrentPosition();
                playerSeekBar.setProgress(currentPos);
                tvCurrentTime.setText(formatTime(currentPos)); // ОБНОВЛЯЕМ ТАЙМЕР
                handler.postDelayed(this, 500);
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TokenManager tokenManager = new TokenManager(this);


        if (tokenManager.getAccessToken() == null || tokenManager.getRefreshToken() == null) {
            goLogin();
            return;
        }


        setContentView(R.layout.activity_main);

        initPlayerUI();
        initNavigation(savedInstanceState);
    }


    private String formatTime(int milliseconds) {
        int seconds = (milliseconds / 1000) % 60;
        int minutes = (milliseconds / (1000 * 60)) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void initPlayerUI() {
        playerSeekBar = findViewById(R.id.player_seekbar);
        tvTrackTitle = findViewById(R.id.tv_mini_track_title);
        tvTrackArtist = findViewById(R.id.tv_mini_track_artist);
        tvCurrentTime = findViewById(R.id.tv_current_time); // НОВОЕ
        tvTotalTime = findViewById(R.id.tv_total_time);

        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnLike = findViewById(R.id.btn_like);

        // Включение/пауза
        btnPlayPause.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                    handler.removeCallbacks(updateSeekBar);
                } else {
                    mediaPlayer.start();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                    handler.post(updateSeekBar);
                }
            }
        });

        //  заглушка
        btnLike.setOnClickListener(v -> {
            if (currentTrack == null) return;
            isLiked = !isLiked;
            btnLike.setImageResource(isLiked ? R.drawable.ic_liked_filled : R.drawable.ic_liked);

            TokenManager tm = new TokenManager(this);
            Map<String, String> body = new HashMap<>();
            body.put("access_token", tm.getAccessToken());
            body.put("music_id", currentTrack.getId());

            Call<Map<String, Boolean>> call = isLiked ?
                    NetworkApi.getSINGLTON().getApi().likeMusic(body) :
                    NetworkApi.getSINGLTON().getApi().unlikeMusic(body);

            call.enqueue(new Callback<Map<String, Boolean>>() {
                @Override
                public void onResponse(Call<Map<String, Boolean>> call, Response<Map<String, Boolean>> response) {
                    if (!response.isSuccessful()) {
                        isLiked = !isLiked; // откатываем визуально в случае ошибки на бекенде
                        btnLike.setImageResource(isLiked ? R.drawable.ic_liked_filled : R.drawable.ic_liked);
                        Toast.makeText(MainActivity.this, "Ошибка сервера при оценке", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<Map<String, Boolean>> call, Throwable t) {
                    isLiked = !isLiked; // откатываем
                    btnLike.setImageResource(isLiked ? R.drawable.ic_liked_filled : R.drawable.ic_liked);
                    Toast.makeText(MainActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
                }
            });
        });


        playerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                    tvCurrentTime.setText(formatTime(progress));
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Включаем бегущую строку для длинных названий
        tvTrackTitle.setSelected(true);
    }


    public void playGlobalTrack(String url, MusicFile music) {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                handler.removeCallbacks(updateSeekBar);
            }

            mediaPlayer = new MediaPlayer();

            String cleanTitle = music.getTitle();
            if (cleanTitle != null) {
                cleanTitle = cleanTitle.replaceAll("(?i)\\.mp3$", "");
            }

            mediaPlayer.setOnPreparedListener(mp -> {
                btnPlayPause.setEnabled(true);
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                mediaPlayer.start();

                int duration = mediaPlayer.getDuration();
                playerSeekBar.setMax(duration);
                tvTotalTime.setText(formatTime(duration)); // СТАВИМ ПОЛНОЕ ВРЕМЯ

                handler.post(updateSeekBar);
            });

            tvTrackTitle.setText(cleanTitle);
            tvTrackArtist.setText(music.getArtist() != null ? music.getArtist() : "Unknown Artist");
            isLiked = false; // Сбрасываем визуал лайка при новом треке
            btnLike.setImageResource(R.drawable.ic_liked);

            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();

            // Иконка загрузки (кружок), пока трек буферизуется
            btnPlayPause.setImageResource(android.R.drawable.ic_popup_sync);
            btnPlayPause.setEnabled(false);
            playerSeekBar.setProgress(0);
            tvTotalTime.setText("0:00");

            mediaPlayer.setOnPreparedListener(mp -> {
                btnPlayPause.setEnabled(true);
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                mediaPlayer.start();

                int duration = mediaPlayer.getDuration();
                playerSeekBar.setMax(duration); // Устанавливаем длину трека
                tvTotalTime.setText(formatTime(duration)); // СТАВИМ ПОЛНОЕ ВРЕМЯ

                handler.post(updateSeekBar); // Запускаем движение ползунка
            });

            mediaPlayer.setOnCompletionListener(mp -> {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                playerSeekBar.setProgress(0);
                tvCurrentTime.setText("0:00"); // ОБНУЛЯЕМ ТАЙМЕР
                handler.removeCallbacks(updateSeekBar);
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка воспроизведения", Toast.LENGTH_SHORT).show();
        }
    }


    void goLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    void initNavigation(Bundle savedInstanceState){
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);

        if(savedInstanceState == null){
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.nav_host_fragment, new HomeFragment())
                    .commit();
        }

        bottomNav.setOnItemSelectedListener(menuItem -> {
            Fragment selectedFragment = null;
            int id = menuItem.getItemId();

            if (id == R.id.nav_home) selectedFragment = new HomeFragment();
            else if (id == R.id.nav_search) selectedFragment = new SearchFragment();
            else if (id == R.id.nav_liked) selectedFragment = new LikedFragment();
            else if (id == R.id.nav_upload) selectedFragment = new UploadFragment();
            else if (id == R.id.nav_profile) selectedFragment = new ProfileFragment();

            if(selectedFragment != null){
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.nav_host_fragment, selectedFragment)
                        .commit();
            }
            return true;
        });
    }
}