package com.example.mp3_client_java.network;


import com.example.mp3_client_java.network.response.DownloadResponse;
import com.example.mp3_client_java.network.response.MusicListResponse;
import com.example.mp3_client_java.network.response.UploadLinkResponse;
import com.example.mp3_client_java.network.response.UserListResponse;

import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface MusicApiService {

    @POST("/music/my")
    Call<MusicListResponse> getMyMusic(@Body Map<String, String> body);
    @POST("/music/popular")
    Call<MusicListResponse> getPopularMusic(@Body Map<String, String> body);
    @POST("/music/my/download")
    Call<DownloadResponse> getDownloadLink(@Body Map<String, String> body);
    @POST("/music/my/upload")
    Call<UploadLinkResponse> getUploadLink(@Body Map<String, Object> body);
    @POST("/music/my/upload/confirm")
    Call<Map<String, Object>> confirmUpload(@Body Map<String, Object> body);


    @POST("/music/my/liked")
    Call<MusicListResponse> getLikedMusic(@Body Map<String, String> body);
    @POST("/music/my/like")
    Call<Map<String, Boolean>> likeMusic(@Body Map<String, String> body);
    @POST("/music/my/unlike")
    Call<Map<String, Boolean>> unlikeMusic(@Body Map<String, String> body);


    @POST("https://" + Net_settings.auth_ip + ":" + Net_settings.auth_port + "/api/login")
    Call<JwtPair> logIn(@Body Map<String, String> body);
    @POST("https://" + Net_settings.auth_ip + ":" + Net_settings.auth_port + "/api/register")
    Call<JwtPair> register(@Body Map<String, String> body);
    @POST("https://" + Net_settings.auth_ip + ":" + Net_settings.auth_port + "/api/refresh")
    Call<JwtPair> refreshToken(@Body Map<String, String> body);


    @POST("/music/my/listen")
    Call<Map<String, Boolean>> recordListen(@Body Map<String, String> body);

    @POST("/music/search")
    Call<MusicListResponse> searchMusic(@Body Map<String, String> body);

    @POST("https://" + Net_settings.auth_ip + ":" + Net_settings.auth_port + "/api/search/users")
    Call<UserListResponse> searchUsers(@Body Map<String, String> body);
}
