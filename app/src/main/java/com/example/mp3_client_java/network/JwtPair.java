package com.example.mp3_client_java.network;

public class JwtPair {
    String access_token;
    String refresh_token;
    String status;


    public JwtPair() {
    }

    public JwtPair(String access_token, String refresh_token, String status) {
        this.access_token = access_token;
        this.refresh_token = refresh_token;
        this.status = status;
    }


    public String getAccess_token() {
        return access_token;
    }

    public void setAccess_token(String access_token) {
        this.access_token = access_token;
    }

    public String getRefresh_token() {
        return refresh_token;
    }

    public void setRefresh_token(String refresh_token) {
        this.refresh_token = refresh_token;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
