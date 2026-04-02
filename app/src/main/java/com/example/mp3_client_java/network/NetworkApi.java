package com.example.mp3_client_java.network;

import android.content.Context;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttp;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class NetworkApi {
    private static final String BASE_URL = "https://10.0.2.2:55555";
    private static final String AUTH_URL = "https://10.0.2.2:55556";
    private static NetworkApi SINGLTON;
    private Retrofit retrofit;



    static  ObjectMapper mapper = new ObjectMapper();

    Context context;
    TokenManager tokenManager;
//    public NetworkApi(Context context_){
//        context = context_;
//        tokenManager = new TokenManager(context);
//    }


    private static OkHttpClient getUnsafeOkHttpClient()
    {
        try {
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {

                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[]{};
                        }
                    }
            };
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            return new OkHttpClient().newBuilder()
                    .sslSocketFactory(sslContext.getSocketFactory(), (X509TrustManager) trustAllCerts[0])
                    .hostnameVerifier((hostname, session) -> true)
                    .build();
        }catch(Exception ex){
            throw  new RuntimeException(ex);
        }
    }

    public static NetworkApi getSINGLTON(){
        if(SINGLTON == null){ SINGLTON = new NetworkApi(); }
        return SINGLTON;
    }

    private NetworkApi(){
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        OkHttpClient okCLinet = getUnsafeOkHttpClient();

        retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(okCLinet)
                .addConverterFactory(JacksonConverterFactory.create(mapper))
                .build();


    }


    public MusicApiService getApi(){
        return retrofit.create(MusicApiService.class);
    }


//    public CompletableFuture<Boolean> refreshTokenAsync(){
//        return CompletableFuture.supplyAsync(() -> {
//            try {
//                String refreshToken = tokenManager.getRefreshToken();
//
//            }
//        })
//    }



}
