package com.example.mp3_client_java.network.response;
import java.util.List;

public class UserListResponse {
    public List<User> users;
    public static class User {
        public String uuid;
        public String username;
    }
}