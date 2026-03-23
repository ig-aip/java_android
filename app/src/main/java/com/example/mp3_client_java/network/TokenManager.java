package com.example.mp3_client_java.network;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private final String PREF_NAME = "auth_prefs";
    private final String KEY_ACCESS = "access_token";
    private final String KEY_REFRESH = "refresh_token";

    private SharedPreferences prefs;

    public TokenManager(Context context){
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveTokens(String access, String refresh){
        prefs.edit().putString(KEY_ACCESS, access).putString(KEY_REFRESH, refresh);
    }

    public String getAccessToken() { return prefs.getString(KEY_ACCESS, null); }
    public String getRefreshToken() { return prefs.getString(KEY_REFRESH, null);}

    public void clear(){
        prefs.edit().clear().apply();
    }
}
