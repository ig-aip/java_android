package com.example.mp3_client_java;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.mp3_client_java.adapter.TrackAdapter;
import com.example.mp3_client_java.adapter.UserAdapter;
import com.example.mp3_client_java.network.NetworkApi;
import com.example.mp3_client_java.network.TokenManager;
import com.example.mp3_client_java.network.response.MusicListResponse;
import com.example.mp3_client_java.network.response.UserListResponse;
import com.google.android.material.tabs.TabLayout;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SearchFragment extends Fragment {
    private EditText etSearch;
    private TabLayout tabLayoutSearch;
    private RecyclerView rvResults;
    private TextView tvPlaceholder;
    private String currentQuery = "";
    private boolean isSearchingUsers = false;
    private Handler searchHandler = new Handler(Looper.getMainLooper());

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_search, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        etSearch = view.findViewById(R.id.et_search);
        tabLayoutSearch = view.findViewById(R.id.tabLayoutSearch);
        rvResults = view.findViewById(R.id.rv_search_results);
        tvPlaceholder = view.findViewById(R.id.tv_placeholder);

        rvResults.setLayoutManager(new LinearLayoutManager(requireContext()));

        tabLayoutSearch.addTab(tabLayoutSearch.newTab().setText("Tracks"));
        tabLayoutSearch.addTab(tabLayoutSearch.newTab().setText("Users"));

        tabLayoutSearch.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                isSearchingUsers = (tab.getPosition() == 1);
                performSearch(currentQuery);
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().trim();
                searchHandler.removeCallbacksAndMessages(null);
                searchHandler.postDelayed(() -> performSearch(currentQuery), 500); // 500ms delay for auto-search
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void performSearch(String query) {
        if (query.isEmpty()) {
            rvResults.setAdapter(null);
            tvPlaceholder.setText("Search for users or tracks to get started");
            tvPlaceholder.setVisibility(View.VISIBLE);
            return;
        }

        TokenManager tm = new TokenManager(requireContext());
        Map<String, String> body = new HashMap<>();
        body.put("query", query);
        body.put("access_token", tm.getAccessToken());

        if (isSearchingUsers) {
            NetworkApi.getSINGLTON().getApi().searchUsers(body).enqueue(new Callback<UserListResponse>() {
                @Override
                public void onResponse(Call<UserListResponse> call, Response<UserListResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().users != null) {
                        if (response.body().users.isEmpty()) {
                            tvPlaceholder.setText("not found");
                            tvPlaceholder.setVisibility(View.VISIBLE);
                            rvResults.setAdapter(null);
                        } else {
                            tvPlaceholder.setVisibility(View.GONE);
                            rvResults.setAdapter(new UserAdapter(response.body().users));
                        }
                    }
                }
                @Override public void onFailure(Call<UserListResponse> call, Throwable t) {}
            });
        } else {
            NetworkApi.getSINGLTON().getApi().searchMusic(body).enqueue(new Callback<MusicListResponse>() {
                @Override
                public void onResponse(Call<MusicListResponse> call, Response<MusicListResponse> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getMusic_list() != null) {
                        if (response.body().getMusic_list().isEmpty()) {
                            tvPlaceholder.setText("not found");
                            tvPlaceholder.setVisibility(View.VISIBLE);
                            rvResults.setAdapter(null);
                        } else {
                            tvPlaceholder.setVisibility(View.GONE);
                            rvResults.setAdapter(new TrackAdapter(response.body().getMusic_list(), track -> {
                                if (getActivity() instanceof MainActivity) {
                                    ((MainActivity) getActivity()).playTrackList(response.body().getMusic_list(), response.body().getMusic_list().indexOf(track));
                                }
                            }));
                        }
                    }
                }
                @Override public void onFailure(Call<MusicListResponse> call, Throwable t) {}
            });
        }
    }
}