package pl.aprilapps.easyphotopicker.sample;

import com.google.gson.annotations.SerializedName;

public class ImageClass {

    @SerializedName("image")
    private String _image;

    @SerializedName("response")
    private String _response;

    public String getResponse() {
        return _response;
    }
}
