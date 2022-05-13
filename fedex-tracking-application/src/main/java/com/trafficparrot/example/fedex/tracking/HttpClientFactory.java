package com.trafficparrot.example.fedex.tracking;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import org.springframework.stereotype.Component;

import static okhttp3.logging.HttpLoggingInterceptor.Level.BODY;

@Component
public class HttpClientFactory {

    public OkHttpClient httpClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(new HttpLoggingInterceptor().setLevel(BODY))
                .build();
    }
}
