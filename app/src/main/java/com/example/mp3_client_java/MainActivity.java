package com.example.mp3_client_java;

import android.content.Intent;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.mp3_client_java.music.MusicFile;
import com.example.mp3_client_java.network.JwtPair;
import com.example.mp3_client_java.network.NetworkApi;
import com.example.mp3_client_java.network.TokenManager;
import com.example.mp3_client_java.network.response.DownloadResponse;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {


    private List<MusicFile> currentPlaylist; // Текущий список треков
    private int currentIndex = -1;
    private MusicFile currentTrack;
    private MediaPlayer mediaPlayer;
    private SeekBar playerSeekBar;
    private TextView tvTrackTitle, tvTrackArtist;
    private ImageButton btnPlayPause, btnLike, btnPrev, btnNext;
    private TextView tvCurrentTime, tvTotalTime;

    private boolean isLiked = false; // Локальный стейт для лайка
    private final Handler handler = new Handler(Looper.getMainLooper());


    private boolean isListenRecorded = false;
    private int listenTimeMs = 0;
    private long lastPlayTime = 0;

    private final Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                int currentPos = mediaPlayer.getCurrentPosition();
                playerSeekBar.setProgress(currentPos);
                tvCurrentTime.setText(formatTime(currentPos));


                if (!isListenRecorded) {
                    long now = System.currentTimeMillis();
                    if (lastPlayTime > 0) {
                        listenTimeMs += (now - lastPlayTime);
                    }
                    lastPlayTime = now;

                    if (listenTimeMs >= 30000) {
                        recordListen(currentTrack.getId());
                    }
                }
                // ------------------------------------

                handler.postDelayed(this, 500);
            } else {
                lastPlayTime = 0;
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
        tvCurrentTime = findViewById(R.id.tv_current_time);
        tvTotalTime = findViewById(R.id.tv_total_time);

        btnPlayPause = findViewById(R.id.btn_play_pause);
        btnLike = findViewById(R.id.btn_like);

        // НОВЫЕ КНОПКИ:
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);

        // ЛОГИКА КНОПКИ НАЗАД
        btnPrev.setOnClickListener(v -> {
            if (currentPlaylist != null && currentIndex > 0) {
                currentIndex--;
                playCurrentIndex();
            }
        });

        // ЛОГИКА КНОПКИ ВПЕРЕД
        btnNext.setOnClickListener(v -> {
            if (currentPlaylist != null && currentPlaylist.size() > 0) {
                currentIndex++;
                playCurrentIndex();
            }
        });

        btnPlayPause.setOnClickListener(v -> {
            if (mediaPlayer != null) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                    handler.removeCallbacks(updateSeekBar);
                    lastPlayTime = 0; // Пауза: останавливаем таймер учета прослушивания
                } else {
                    mediaPlayer.start();
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                    lastPlayTime = System.currentTimeMillis(); // Старт: возобновляем
                    handler.post(updateSeekBar);
                }
            }
        });

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
                    if (response.isSuccessful()) {
                        currentTrack.setIs_liked(isLiked);
                    } else {
                        isLiked = !isLiked;
                        btnLike.setImageResource(isLiked ? R.drawable.ic_liked_filled : R.drawable.ic_liked);
                    }
                }
                @Override
                public void onFailure(Call<Map<String, Boolean>> call, Throwable t) {
                    isLiked = !isLiked;
                    btnLike.setImageResource(isLiked ? R.drawable.ic_liked_filled : R.drawable.ic_liked);
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

        tvTrackTitle.setSelected(true);
    }

    public void playTrackList(List<MusicFile> playlist, int startIndex) {
        this.currentPlaylist = playlist;
        this.currentIndex = startIndex;
        playCurrentIndex();
    }
    private void playCurrentIndex() {

        if (currentIndex == 0) {
            btnPrev.setEnabled(false);
            btnPrev.setAlpha(0.3f); // Тусклая кнопка
        } else {
            btnPrev.setEnabled(true);
            btnPrev.setAlpha(1.0f); // Нормальная кнопка
        }

        if (currentPlaylist == null || currentPlaylist.isEmpty()) return;

        // Зацикливание списка (когда трек последний - начинаем с нулевого)
        if (currentIndex >= currentPlaylist.size()) currentIndex = 0;
        if (currentIndex < 0) currentIndex = currentPlaylist.size() - 1;

        MusicFile track = currentPlaylist.get(currentIndex);
        this.currentTrack = track;

        // Моментальное обновление UI для текущего трека
        String cleanTitle = track.getTitle();
        if (cleanTitle != null) {
            cleanTitle = cleanTitle.replaceAll("(?i)\\.mp3$", "");
        }
        tvTrackTitle.setText(cleanTitle);
        tvTrackArtist.setText(track.getUsername() != null ? track.getUsername() : "Unknown Artist");
        isLiked = track.isIs_liked();
        btnLike.setImageResource(isLiked ? R.drawable.ic_liked_filled : R.drawable.ic_liked);

        btnPlayPause.setImageResource(android.R.drawable.ic_popup_sync);
        btnPlayPause.setEnabled(false);
        playerSeekBar.setProgress(0);
        tvTotalTime.setText("0:00");
        tvCurrentTime.setText("0:00");

        // Сброс счетчиков
        isListenRecorded = false;
        listenTimeMs = 0;
        lastPlayTime = 0;

        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
            handler.removeCallbacks(updateSeekBar);
        }

        // Берем ссылку с сервера
        TokenManager tm = new TokenManager(this);
        Map<String, String> body = new HashMap<>();
        body.put("access_token", tm.getAccessToken());
        body.put("s3_path", track.getS3_path());


        NetworkApi.getSINGLTON().getApi().getDownloadLink(body).enqueue(new Callback<DownloadResponse>() {
            @Override
            public void onResponse(Call<DownloadResponse> call, Response<DownloadResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String url = response.body().getDownload_link().replace("127.0.0.1", "10.0.2.2");
                    startActualPlayback(url);
                } else if (response.code() == 401 || response.code() == 403) {
                    refreshTokenForPlayback();
                } else {
                    btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                }
            }

            @Override
            public void onFailure(Call<DownloadResponse> call, Throwable t) {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
            }
        });
    }

    private void startActualPlayback(String url) {
        try {
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .build()
            );

            mediaPlayer.setDataSource(url);
            mediaPlayer.prepareAsync();

            mediaPlayer.setOnPreparedListener(mp -> {
                btnPlayPause.setEnabled(true);
                btnPlayPause.setImageResource(android.R.drawable.ic_media_pause);
                mediaPlayer.start();

                int duration = mediaPlayer.getDuration();
                playerSeekBar.setMax(duration);
                tvTotalTime.setText(formatTime(duration));

                lastPlayTime = System.currentTimeMillis();
                handler.post(updateSeekBar);
            });

            // КОГДА ТРЕК ЗАКАНЧИВАЕТСЯ:
            mediaPlayer.setOnCompletionListener(mp -> {
                btnPlayPause.setImageResource(android.R.drawable.ic_media_play);
                playerSeekBar.setProgress(0);
                tvCurrentTime.setText("0:00");
                handler.removeCallbacks(updateSeekBar);
                lastPlayTime = 0;

                // Зачет прослушивания для коротких треков
                if (!isListenRecorded && currentTrack != null) {
                    recordListen(currentTrack.getId());
                }

                // ПЕРЕКЛЮЧЕНИЕ НА СЛЕДУЮЩИЙ ТРЕК
                currentIndex++;
                playCurrentIndex();
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Ошибка воспроизведения", Toast.LENGTH_SHORT).show();
        }
    }


    private void refreshTokenForPlayback() {
        TokenManager tm = new TokenManager(this);
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        Map<String, String> refreshBody = new HashMap<>();
        refreshBody.put("refresh_token", tm.getRefreshToken());
        refreshBody.put("device_id", deviceId);

        NetworkApi.getSINGLTON().getApi().refreshToken(refreshBody).enqueue(new Callback<JwtPair>() {
            @Override
            public void onResponse(Call<JwtPair> call, Response<JwtPair> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getAccess_token() != null) {
                    tm.saveTokens(response.body().getAccess_token(), response.body().getRefresh_token());
                    playCurrentIndex();
                } else {
                    goLogin();
                }
            }

            @Override
            public void onFailure(Call<JwtPair> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Ошибка сети", Toast.LENGTH_SHORT).show();
            }
        });
    }


    public void playGlobalTrack(String url, MusicFile music) {
        try {
            this.currentTrack = music;

            isListenRecorded = false;
            listenTimeMs = 0;
            lastPlayTime = 0;

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
            tvTrackArtist.setText(music.getUsername() != null ? music.getUsername() : "Unknown Artist");
            isLiked =  music.isIs_liked();
            btnLike.setImageResource(isLiked ? R.drawable.ic_liked_filled : R.drawable.ic_liked);

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
                tvCurrentTime.setText("0:00");
                handler.removeCallbacks(updateSeekBar);
                lastPlayTime = 0;


                // Засчитываем прослушивание, если трек закончился, а 30 сек не набралось
                if (!isListenRecorded && currentTrack != null) {
                    recordListen(currentTrack.getId());
                }
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
            getSupportFragmentManager().beginTransaction().replace(R.id.nav_host_fragment, new HomeFragment()).commit();
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
                getSupportFragmentManager().beginTransaction().replace(R.id.nav_host_fragment, selectedFragment).commit();
            }
            return true;
        });
    }



    private void recordListen(String musicId) {
        isListenRecorded = true;
        TokenManager tm = new TokenManager(this);
        Map<String, String> body = new HashMap<>();
        body.put("access_token", tm.getAccessToken());
        body.put("music_id", musicId);

        NetworkApi.getSINGLTON().getApi().recordListen(body).enqueue(new Callback<Map<String, Boolean>>() {
            @Override public void onResponse(Call<Map<String, Boolean>> call, Response<Map<String, Boolean>> response) {}
            @Override public void onFailure(Call<Map<String, Boolean>> call, Throwable t) {}
        });
    }
}