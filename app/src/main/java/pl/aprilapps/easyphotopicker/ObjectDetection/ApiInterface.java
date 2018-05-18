package pl.aprilapps.easyphotopicker.ObjectDetection;

import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiInterface {

    @Multipart
    @POST("upload")
    Call<String> processImage(@Part MultipartBody.Part image);

}
