package com.example.mp3_client_java;

import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.example.mp3_client_java.network.TokenManager;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

public class ProfileFragment extends Fragment {

    public ProfileFragment(){
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        TextView tvProfileName = view.findViewById(R.id.tvProfileName);
        TokenManager tm = new TokenManager(requireContext());
        String token = tm.getAccessToken();
        if (token != null) {
            try {
                String[] parts = token.split("\\.");
                if (parts.length == 3) {
                    // Декодируем Payload
                    String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE));
                    JSONObject json = new JSONObject(payload);
                    // Достаем username (auth server обычно кладет его в токен)
                    String username = json.optString("username", "Unknown User");
                    if (tvProfileName != null) {
                        tvProfileName.setText(username);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        // Обработчик нажатия на шестеренку
        ImageView btnSettings = view.findViewById(R.id.btnSettings);
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                if (getActivity() != null) {
                    getActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.nav_host_fragment, new SettingsFragment())
                            .addToBackStack(null) // Позволяет вернуться назад системной кнопкой 'назад'
                            .commit();
                }
            });
        }

        TabLayout tableLayout = view.findViewById(R.id.profileTabLayout);
        ViewPager2 viewPager2 = view.findViewById(R.id.profileViewPager);

        String[] tableNames = {"All Music", "Upload Tracks", "Liked songs"};

        viewPager2.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        return new ProfileAllMusicFragment(); // All music
                    case 1:
                        return new UploadedTracksFragment(); // Тот самый новый фрагмент!
                    case 2:
                        return new LikedFragment(); // Понравившиеся
                    default:
                        return new HomeFragment();
                }
            }

            @Override
            public int getItemCount() {
                return tableNames.length;
            }
        });

        new TabLayoutMediator(tableLayout, viewPager2, (tab, position) -> {
            tab.setText(tableNames[position]);
        }).attach();
    }
}