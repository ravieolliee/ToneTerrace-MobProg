package com.toneterrace.app.api;

import com.toneterrace.app.models.LyricsResponse;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Path;

public interface LyricsApi {

    // The endpoint uses {artist} and {title} as dynamic placeholders
    @GET("v1/{artist}/{title}")
    Call<LyricsResponse> getLyrics(
            @Path("artist") String artist,
            @Path("title") String title
    );
}