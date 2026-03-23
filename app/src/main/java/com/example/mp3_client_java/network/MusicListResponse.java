package com.example.mp3_client_java.network;

import com.example.mp3_client_java.music.MusicFile;

import java.util.List;

public class MusicListResponse {
    List<MusicFile> list;

    public MusicListResponse() {
    }

    public List<MusicFile> getList() {
        return list;
    }

    public void setList(List<MusicFile> list) {
        this.list = list;
    }
}
