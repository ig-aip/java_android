package com.example.mp3_client_java;

import static androidx.core.content.ContentProviderCompat.requireContext;
import static java.security.AccessController.getContext;

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

public class RegistrationActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        EditText etUsername = findViewById(R.id.etRegUsername);
        EditText etEmail = findViewById(R.id.etRegEmail);
        EditText etPassword = findViewById(R.id.etRegPassword);
        Button btnSignUp = findViewById(R.id.btnRegSignUp);
        TextView tvLoginLink = findViewById(R.id.tvRegLoginLink);

        // Переход обратно на логин
        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(RegistrationActivity.this, LoginActivity.class));
            finish();
        });

        // Отправка данных на регистрацию
        btnSignUp.setOnClickListener(v -> {
            btnSignUp.setEnabled(false);

            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            if (username.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
                return;
            }

            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            String deviceName = android.os.Build.MODEL;

            Map<String, String> body = new HashMap<>();
            body.put("username", username);
            body.put("email", email);
            body.put("password", pass);
            body.put("device_id", deviceId);
            body.put("device_name", deviceName);

            NetworkApi.getSINGLTON().getApi().register(body).enqueue(new Callback<JwtPair>() {
                @Override
                public void onResponse(Call<JwtPair> call, Response<JwtPair> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getAccess_token() != null) {
                        TokenManager tm = new TokenManager(RegistrationActivity.this);
                        tm.saveTokens(response.body().getAccess_token(), response.body().getRefresh_token());

                        startActivity(new Intent(RegistrationActivity.this, MainActivity.class));
                        Intent intent = new Intent(RegistrationActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();

                    } else {
                        btnSignUp.setEnabled(true);
                        Toast.makeText(RegistrationActivity.this, "Ошибка регистрации", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<JwtPair> call, Throwable t) {
                    btnSignUp.setEnabled(true);
                    Toast.makeText(RegistrationActivity.this, "Ошибка сети regestartion", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}