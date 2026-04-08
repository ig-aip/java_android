package com.example.mp3_client_java;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mp3_client_java.adapter.TrackAdapter;
import com.example.mp3_client_java.music.MusicFile;
import com.example.mp3_client_java.network.NetworkApi;
import com.example.mp3_client_java.network.TokenManager;
import com.example.mp3_client_java.network.response.MusicListResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileAllMusicFragment extends Fragment {

    private RecyclerView rvTracks;
    private Map<String, MusicFile> uniqueTracks = new LinkedHashMap<>();
    private int pendingRequests = 2;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_uploaded_tracks, container, false);
        rvTracks = view.findViewById(R.id.rv_uploaded_tracks);
        rvTracks.setLayoutManager(new LinearLayoutManager(requireContext()));
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        uniqueTracks.clear();
        pendingRequests = 2;
        loadMixedTracks();
    }

    private void loadMixedTracks() {
        TokenManager tManager = new TokenManager(requireContext());
        Map<String, String> body = new HashMap<>();
        body.put("access_token", tManager.getAccessToken());

        // Получаем загруженные треки
        NetworkApi.getSINGLTON().getApi().getMyMusic(body).enqueue(new Callback<MusicListResponse>() {
            @Override
            public void onResponse(Call<MusicListResponse> call, Response<MusicListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for(MusicFile track : response.body().getMusic_list()) uniqueTracks.put(track.getId(), track);
                }
                checkAndSetAdapter();
            }
            @Override public void onFailure(Call<MusicListResponse> call, Throwable t) { checkAndSetAdapter(); }
        });

        // Получаем лайкнутые треки
        NetworkApi.getSINGLTON().getApi().getLikedMusic(body).enqueue(new Callback<MusicListResponse>() {
            @Override
            public void onResponse(Call<MusicListResponse> call, Response<MusicListResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    for(MusicFile track : response.body().getMusic_list()) uniqueTracks.put(track.getId(), track);
                }
                checkAndSetAdapter();
            }
            @Override public void onFailure(Call<MusicListResponse> call, Throwable t) { checkAndSetAdapter(); }
        });
    }

    private void checkAndSetAdapter() {
        pendingRequests--;
        if (pendingRequests <= 0 && isAdded()) {
            List<MusicFile> finalList = new ArrayList<>(uniqueTracks.values());
            TrackAdapter adapter = new TrackAdapter(finalList, track -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).playTrackList(finalList, finalList.indexOf(track));
                }
            });
            rvTracks.setAdapter(adapter);
        }
    }
}