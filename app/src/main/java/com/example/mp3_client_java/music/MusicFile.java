package com.example.mp3_client_java.music;

public class MusicFile {
    String id;
    String user_uuid;
    String s3_path;
    String title;

    String username;
    boolean is_public;
    boolean is_liked;

    public MusicFile() {
    }

    public MusicFile(String id, String user_uuid, String s3_path, String title, boolean is_public) {
        this.id = id;
        this.user_uuid = user_uuid;
        this.s3_path = s3_path;
        this.title = title;
        this.is_public = is_public;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUser_uuid() {
        return user_uuid;
    }

    public void setUser_uuid(String user_uuid) {
        this.user_uuid = user_uuid;
    }

    public String getS3_path() {
        return s3_path;
    }

    public void setS3_path(String s3_path) {
        this.s3_path = s3_path;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public boolean isIs_public() {
        return is_public;
    }

    public void setIs_public(boolean is_public) {
        this.is_public = is_public;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isIs_liked() {
        return is_liked;
    }

    public void setIs_liked(boolean is_liked) {
        this.is_liked = is_liked;
    }
}
