package pl.aprilapps.easyphotopicker.sample;

import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

public interface ApiInterface {

    @FormUrlEncoded
    @POST("processImage")
    Call<ImageClass> processImage(@Field("title") String title, @Field("image") String image);

}
