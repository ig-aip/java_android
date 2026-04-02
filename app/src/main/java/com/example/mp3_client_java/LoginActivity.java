package com.example.mp3_client_java;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.mp3_client_java.network.JwtPair;
import com.example.mp3_client_java.network.NetworkApi;
import com.example.mp3_client_java.network.TokenManager;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        EditText etEmail = findViewById(R.id.etEmail);
        EditText etPassword = findViewById(R.id.etPassword);
        Button btnLogin = findViewById(R.id.btnLogin);
        TextView tvSignUpView = findViewById(R.id.tvSignUpLink);


        btnLogin.setOnClickListener(v -> {
            String email = etEmail.getText().toString();
            String pass = etPassword.getText().toString();

            if (email.isEmpty() || pass.isEmpty()) return;

            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            String deviceName = android.os.Build.MODEL;

            Map<String, String> body = new HashMap<>();
            body.put("email", email);
            body.put("password", pass);
            body.put("device_id", deviceId);
            body.put("device_name", deviceName);

            NetworkApi.getSINGLTON().getApi().logIn(body).enqueue(new Callback<JwtPair>() {
                @Override
                public void onResponse(Call<JwtPair> call, Response<JwtPair> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        TokenManager tm = new TokenManager(LoginActivity.this);
                        tm.saveTokens(response.body().getAccess_token(), response.body().getRefresh_token());
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Ошибка логина", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<JwtPair> call, Throwable t) {
                    Toast.makeText(LoginActivity.this, "Ошибка сети login", Toast.LENGTH_SHORT).show();
                }
            });
        });

        tvSignUpView.setOnClickListener( v -> {
            startActivity(new Intent(LoginActivity.this, RegistrationActivity.class));
            finish();
        });

    }
}
