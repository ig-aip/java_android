package com.example.mp3_client_java.network;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface MusicApiService {

    @POST("/music/my")
    Call<MusicListResponse> getMusic(@Body Map<String, String> body);

    @POST("/api/login")
    Call<JwtPair> logIn(@Body Map<String, String> body);

    @POST("/music/my/download")
    Call<DownloadResponse> getDownloadLink(@Body Map<String, String> body);
}
