package pl.aprilapps.easyphotopicker.sample;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ApiClient {

    private static final String BASE_URL = "http://recog-env-1.ztppawaibd.ap-southeast-2.elasticbeanstalk.com/rest/";
    private static Retrofit _retrofit;

    public static Retrofit getApiClient(){

        if(_retrofit == null){
            _retrofit = new Retrofit.Builder().baseUrl(BASE_URL).addConverterFactory(ScalarsConverterFactory.create()).addConverterFactory(GsonConverterFactory.create()).build();
        }
        return _retrofit;
    }

}
