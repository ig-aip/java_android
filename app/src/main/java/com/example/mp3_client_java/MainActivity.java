package com.example.mp3_client_java;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.mp3_client_java.network.TokenManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TokenManager tokenManager = new TokenManager(this);


        if (tokenManager.getAccessToken() == null || tokenManager.getRefreshToken() == null) {
            goLogin();
            return;
        }


        setContentView(R.layout.activity_main);


        initNavigation(savedInstanceState);
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