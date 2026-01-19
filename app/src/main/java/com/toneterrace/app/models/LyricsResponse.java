package com.toneterrace.app.models;

import com.google.gson.annotations.SerializedName;

public class LyricsResponse {

    // @SerializedName maps the JSON key "lyrics" to our Java variable
    @SerializedName("lyrics")
    private String lyrics;

    public String getLyrics() {
        return lyrics;
    }
}