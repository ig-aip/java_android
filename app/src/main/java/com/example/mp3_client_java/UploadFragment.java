package com.example.mp3_client_java;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.mp3_client_java.network.NetworkApi;
import com.example.mp3_client_java.network.TokenManager;
import com.example.mp3_client_java.network.response.UploadLinkResponse;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import retrofit2.Call;
import retrofit2.Callback;

public class UploadFragment extends Fragment {

    private Uri selectedAudioUri;
    private TextView tvUploadDropZoneText;
    private EditText etTrackTitle;
    private Button btnUpload;
    private TokenManager tokenManager;

    // Лаунчер для вызова окна выбора файла
    private final ActivityResultLauncher<Intent> filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    selectedAudioUri = result.getData().getData();
                    String fileName = getFileName(selectedAudioUri);
                    tvUploadDropZoneText.setText(fileName);

                    // Если пользователь еще не ввел название, используем имя файла
                    if (etTrackTitle.getText().toString().isEmpty()) {
                        etTrackTitle.setText(fileName);
                    }
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_upload, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tokenManager = new TokenManager(requireContext());

        LinearLayout dropZone = view.findViewById(R.id.uploadDropZone);

        // ИСПРАВЛЕНИЕ: Безопасный поиск элемента по ID
        tvUploadDropZoneText = view.findViewById(R.id.tvDropZoneLabel);

        etTrackTitle = view.findViewById(R.id.etTrackTitle);
        btnUpload = view.findViewById(R.id.btnUpload);

        dropZone.setOnClickListener(v -> openFilePicker());
        btnUpload.setOnClickListener(v -> startUploadProcess());
    }

    private void openFilePicker() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/*");
            filePickerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(requireContext(), "No file picker found", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileName(Uri uri) {
        if (uri == null) return "Unknown file";
        String result = null;
        if (uri.getScheme() != null && uri.getScheme().equals("content")) {
            try (Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if(index != -1) {
                        result = cursor.getString(index);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            if (result != null) {
                int cut = result.lastIndexOf('/');
                if (cut != -1) {
                    result = result.substring(cut + 1);
                }
            }
        }
        return result != null ? result : "Unknown file";
    }

    private void startUploadProcess() {
        if (selectedAudioUri == null) {
            Toast.makeText(requireContext(), "Сначала выберите аудиофайл", Toast.LENGTH_SHORT).show();
            return;
        }

        String title = etTrackTitle.getText().toString().trim();
        if (title.isEmpty()) {
            Toast.makeText(requireContext(), "Введите название трека", Toast.LENGTH_SHORT).show();
            return;
        }

        btnUpload.setEnabled(false);
        Toast.makeText(requireContext(), "Запрашиваем ссылку для загрузки...", Toast.LENGTH_SHORT).show();

        Map<String, Object> body = new HashMap<>();
        body.put("access_token", tokenManager.getAccessToken());
        body.put("title", title);
        body.put("is_public", true);

        NetworkApi.getSINGLTON().getApi().getUploadLink(body).enqueue(new Callback<UploadLinkResponse>() {
            @Override
            public void onResponse(Call<UploadLinkResponse> call, retrofit2.Response<UploadLinkResponse> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getUrl() != null) {
                    String uploadUrl = response.body().getUrl();
                    uploadUrl = uploadUrl.replace("127.0.0.1", "10.0.2.2");
                    uploadFileToS3(uploadUrl, title);
                } else {
                    Toast.makeText(requireContext(), "Ошибка получения ссылки: " + response.code(), Toast.LENGTH_SHORT).show();
                    btnUpload.setEnabled(true);
                }
            }

            @Override
            public void onFailure(Call<UploadLinkResponse> call, Throwable t) {
                if(isAdded() && getContext() != null){
                    Toast.makeText(getContext(), "Ошибка сети startUploadProcess", Toast.LENGTH_SHORT).show();
                }
                btnUpload.setEnabled(true);
            }
        });
    }

    private void uploadFileToS3(String uploadUrl, String title) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            try {
                // ПРОВЕРКА КОНТЕКСТА: предотвращает краши, если фрагмент закрыли
                if (!isAdded() || getContext() == null) return;

                InputStream inputStream = getContext().getContentResolver().openInputStream(selectedAudioUri);
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int nRead;
                byte[] data = new byte[16384];
                while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, nRead);
                }
                buffer.flush();

                byte[] fileBytes = buffer.toByteArray();
                inputStream.close();
                buffer.close();

                OkHttpClient client = new OkHttpClient();
                // ИСПРАВЛЕНИЕ MINIO: передаем null вместо MediaType, чтобы избежать добавления не подписанного заголовка Content-Type
                RequestBody requestBody = RequestBody.create(null, fileBytes);
                Request request = new Request.Builder()
                        .url(uploadUrl)
                        .header("Connection", "close")
                        .put(requestBody)
                        .build();

                Response response = client.newCall(request).execute();

                if (response.isSuccessful()) {
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> confirmUpload(title));
                    }
                } else {
                    String errorBody = response.body() != null ? response.body().string() : "No body";
                    Log.e("S3_UPLOAD", "Error " + response.code() + ": " + errorBody);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (isAdded() && getContext() != null) {
                                Toast.makeText(getContext(), "Сбой выгрузки в S3: " + response.code(), Toast.LENGTH_SHORT).show();
                                btnUpload.setEnabled(true);
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Log.e("S3_UPLOAD", "Exception: " + e.getMessage(), e);
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (isAdded() && getContext() != null) {
                            Toast.makeText(getContext(), "Ошибка загрузки: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            btnUpload.setEnabled(true);
                        }
                    });
                }
            }
        });
    }

    private void confirmUpload(String title) {
        if (getContext() == null) return;

        Map<String, Object> body = new HashMap<>();
        body.put("access_token", tokenManager.getAccessToken());
        body.put("title", title);
        body.put("status_code", 1);
        body.put("is_public", true);

        NetworkApi.getSINGLTON().getApi().confirmUpload(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, retrofit2.Response<Map<String, Object>> response) {
                if (isAdded() && getContext() != null) {
                    btnUpload.setEnabled(true);
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "Трек успешно загружен на сервер!", Toast.LENGTH_SHORT).show();
                        selectedAudioUri = null;
                        tvUploadDropZoneText.setText("Choose audio file");
                        etTrackTitle.setText("");
                    } else {
                        Toast.makeText(getContext(), "Ошибка подтверждения на сервере", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (isAdded() && getContext() != null) {
                    btnUpload.setEnabled(true);
                    Toast.makeText(getContext(), "Ошибка сети при подтверждении", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}