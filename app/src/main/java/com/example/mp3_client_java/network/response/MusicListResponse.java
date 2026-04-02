package com.example.mp3_client_java.network.response;

import com.example.mp3_client_java.music.MusicFile;

import java.util.List;

public class MusicListResponse {
    List<MusicFile> music_list;

    public MusicListResponse() {
    }

    public List<MusicFile> getMusic_list() {
        return music_list;
    }

    public void setMusic_list(List<MusicFile> music_list) {
        this.music_list = music_list;
    }
}
