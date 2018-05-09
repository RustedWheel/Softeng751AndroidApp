package pl.aprilapps.easyphotopicker.sample;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "https://softeng751.azurewebsites.net/rest/";
    private static Retrofit _retrofit;

    public static Retrofit getApiClient(){

        if(_retrofit == null){
            _retrofit = new Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(GsonConverterFactory.create()).build();
        }
        return _retrofit;
    }

}
