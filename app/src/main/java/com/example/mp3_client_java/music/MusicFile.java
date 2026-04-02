package com.example.mp3_client_java.music;

public class MusicFile {
    String id;
    String user_uuid;
    String s3_path;
    String title;

    String artist;
    boolean is_public;

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

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }
}
