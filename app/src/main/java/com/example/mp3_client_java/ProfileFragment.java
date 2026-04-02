package com.example.mp3_client_java;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import org.jetbrains.annotations.NotNull;

public class ProfileFragment extends Fragment {

    public ProfileFragment(){
        super(R.layout.fragment_profile);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState){
        super.onViewCreated(view, savedInstanceState);

        TabLayout tableLayout = view.findViewById(R.id.profileTabLayout);
        ViewPager2 viewPager2 = view.findViewById(R.id.profileViewPager);

        String[] tableNames = {"All Music", "Upload Tracks", "Liked songs"};

        viewPager2.setAdapter(new FragmentStateAdapter(this) {
            @NonNull
            @Override
            public Fragment createFragment(int position) {
                switch (position) {
                    case 0:
                        return new HomeFragment(); // All music
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
