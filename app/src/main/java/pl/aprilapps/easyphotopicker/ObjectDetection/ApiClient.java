package pl.aprilapps.easyphotopicker.ObjectDetection;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

public class ApiClient {

    private static final String AWS_BASE_URL = "http://recog-env-1.ztppawaibd.ap-southeast-2.elasticbeanstalk.com/rest/";
    private static final String AZURE_BASE_URL = "https://objectrecognition.azurewebsites.net/rest/";
    private static final String GOOGLE_CLOUD_BASE_URL = "https://softeng751-203910.appspot.com/rest/";

    private static Retrofit _retrofitAWS;
    private static Retrofit _retrofitAzure;
    private static Retrofit _retrofitGoogleCloud;

    public static Retrofit getApiClientAWS(){

        if(_retrofitAWS == null){
            _retrofitAWS = new Retrofit.Builder().baseUrl(AWS_BASE_URL).addConverterFactory(ScalarsConverterFactory.create()).addConverterFactory(GsonConverterFactory.create()).build();
        }

        return _retrofitAWS;
    }

    public static Retrofit getApiClientAzure(){

        if(_retrofitAzure == null){
            _retrofitAzure = new Retrofit.Builder().baseUrl(AZURE_BASE_URL).addConverterFactory(ScalarsConverterFactory.create()).addConverterFactory(GsonConverterFactory.create()).build();
        }

        return _retrofitAzure;
    }

    public static Retrofit getApiClientGoogleCloud(){

        if(_retrofitGoogleCloud == null){
            _retrofitGoogleCloud = new Retrofit.Builder().baseUrl(GOOGLE_CLOUD_BASE_URL).addConverterFactory(ScalarsConverterFactory.create()).addConverterFactory(GsonConverterFactory.create()).build();
        }

        return _retrofitGoogleCloud;
    }
}
