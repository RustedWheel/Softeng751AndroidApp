package pl.aprilapps.easyphotopicker.sample;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import pl.aprilapps.easyphotopicker.DefaultCallback;
import pl.aprilapps.easyphotopicker.EasyImage;
import pl.tajchert.nammu.Nammu;
import pl.tajchert.nammu.PermissionCallback;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final String CLOUD_VISION_API_KEY = BuildConfig.API_KEY;
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    private static final int MAX_LABEL_RESULTS = 10;
    private static final int MAX_DIMENSION = 1200;
    private static final int INPUT_SIZE = 224;
    private static final int IMAGE_MEAN = 117;
    private static final float IMAGE_STD = 1;
    private static final String INPUT_NAME = "input";
    private static final String OUTPUT_NAME = "output";

    private static final String MODEL_FILE = "file:///android_asset/tensorflow_inception_graph.pb";
    private static final String LABEL_FILE = "file:///android_asset/imagenet_comp_graph_label_strings.txt";

    private Classifier classifier;
    private Executor executor = Executors.newSingleThreadExecutor();

    protected View galleryButton;

    private ImageView mainImage;

    private TextView imageDetails;

    private ProgressBar progressBar;

    private Spinner spinnerAPI;

    private String[] APIs = new String[]{"Local Tensorflow", "AWS Tensorflow", "Azure Tensorflow", "Google Cloud Tensorflow", "Google Cloud Vision"};

    private String api = APIs[0];

    private File imageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Nammu.init(this);

        initiateTensorFlowClassifier();

        mainImage = findViewById(R.id.main_image);
        imageDetails = findViewById(R.id.image_details);
        progressBar = findViewById(R.id.image_Progress);
        galleryButton = findViewById(R.id.gallery_button);
        spinnerAPI = findViewById(R.id.spinnerAPI);

        spinnerAPI.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                api = (String) adapterView.getItemAtPosition(i);

                if(imageFile != null){
                    callAPI(imageFile);
                }

            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });


        ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, APIs);
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAPI.setAdapter(dataAdapter);


        EasyImage.configuration(this)
                .setImagesFolderName("Softeng751")
                .setCopyTakenPhotosToPublicGalleryAppFolder(false)
                .setCopyPickedImagesToPublicGalleryAppFolder(false)
                .setAllowMultiplePickInGallery(false);

        checkGalleryAppAvailability();

        findViewById(R.id.camera_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EasyImage.openCamera(MainActivity.this, 0);
            }
        });


        findViewById(R.id.gallery_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /** Some devices such as Samsungs which have their own gallery app require write permission. Testing is advised! */

                int permissionCheck = ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                    EasyImage.openDocuments(MainActivity.this, 0);
                } else {
                    Nammu.askForPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE, new PermissionCallback() {
                        @Override
                        public void permissionGranted() {
                            EasyImage.openDocuments(MainActivity.this, 0);
                        }

                        @Override
                        public void permissionRefused() {

                        }
                    });
                }
            }
        });

    }


    private void checkGalleryAppAvailability() {
        if (!EasyImage.canDeviceHandleGallery(this)) {
            //Device has no app that handles gallery intent
            galleryButton.setVisibility(View.GONE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Nammu.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        EasyImage.handleActivityResult(requestCode, resultCode, data, this, new DefaultCallback() {
            @Override
            public void onImagePickerError(Exception e, EasyImage.ImageSource source, int type) {
                //Some error handling
                e.printStackTrace();
            }

            @Override
            public void onImagesPicked(List<File> imageFiles, EasyImage.ImageSource source, int type) {

                File image = imageFiles.get(0);
                imageFile = image;
                callAPI(image);

            }

            @Override
            public void onCanceled(EasyImage.ImageSource source, int type) {
                //Cancel handling, you might wanna remove taken photo if it was canceled
                if (source == EasyImage.ImageSource.CAMERA) {
                    File photoFile = EasyImage.lastlyTakenButCanceledPhoto(MainActivity.this);
                    if (photoFile != null) photoFile.delete();
                }
            }
        });
    }


    private void callAPI(File image){

        ApiInterface apiInterface;

        switch (api) {
            case "Local Tensorflow":
                setLoadingUI(image);
                LocalOjectDetector detectionTask = new LocalOjectDetector(this, image);
                detectionTask.execute();
                break;
            case "AWS Tensorflow":
                setLoadingUI(image);
                apiInterface = ApiClient.getApiClientAWS().create(ApiInterface.class);
                processImageOnCloud(image, apiInterface);
                break;
            case "Azure Tensorflow":
                setLoadingUI(image);
                apiInterface = ApiClient.getApiClientAzure().create(ApiInterface.class);
                processImageOnCloud(image, apiInterface);
                break;
            case "Google Cloud Tensorflow":
                setLoadingUI(image);
                apiInterface = ApiClient.getApiClientGoogleCloud().create(ApiInterface.class);
                processImageOnCloud(image, apiInterface);
                break;
            case "Google Cloud Vision":
                Uri uri = Uri.fromFile(image);
                uploadImage(uri);
                break;
        }

    }

    private class LocalOjectDetector extends AsyncTask<Void, Void, Classifier.Recognition> {

        private final WeakReference<MainActivity> mActivityWeakReference;
        private File imageFile;
        private long startTime;

        LocalOjectDetector(MainActivity activity, File image) {
            mActivityWeakReference = new WeakReference<>(activity);
            imageFile = image;
        }

        @Override
        protected Classifier.Recognition doInBackground(Void... params) {

            startTime = System.currentTimeMillis();
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);
            final List<Classifier.Recognition> results = classifier.recognizeImage(bitmap);

            Classifier.Recognition result = results.get(0);

            return result;
        }

        @Override
        protected void onPostExecute(Classifier.Recognition result) {

            MainActivity activity = mActivityWeakReference.get();
            if (activity != null && !activity.isFinishing()) {

                if(result != null){
                    TextView imageDetail = activity.findViewById(R.id.image_details);
                    String bestMatch = String.format("BEST MATCH: %s (%.2f%% likely)",
                            result.getTitle(),
                            result.getConfidence() * 100f);

                    long elapsedTime = System.currentTimeMillis() - startTime;

                    imageDetail.setText(bestMatch + "\n\n" + "Total elapsed request/response time in milliseconds: " + elapsedTime);
                    ProgressBar progress = activity.findViewById(R.id.image_Progress);
                    progress.setVisibility(View.INVISIBLE);
                }

            }

        }
    }


    private void processImageOnCloud(File file, ApiInterface apiInterface){

        MultipartBody.Part filePart = MultipartBody.Part.createFormData("file", file.getName(), RequestBody.create(MediaType.parse("image/*"), file));

        Call<String> call = apiInterface.processImage(filePart);

        long startTime = System.currentTimeMillis();

        call.enqueue(new Callback<String>() {
            @Override
            public void onResponse(Call<String> call, Response<String> response) {

                progressBar.setVisibility(View.INVISIBLE);

                String responseMessage;

                if(response.body() != null){
                    responseMessage = response.body().toString();
                    Log.d(TAG, "Response received! Value: " + responseMessage);

                    long elapsedTime = System.currentTimeMillis() - startTime;

                    imageDetails.setText(responseMessage + "\n\n" + "Total elapsed request/response time in milliseconds: " + elapsedTime);
                } else {

                    imageDetails.setText("Request failed!");
                }

            }

            @Override
            public void onFailure(Call<String> call, Throwable throwable) {

                Log.d(TAG, "Request failed, exception: " + throwable.toString());

                progressBar.setVisibility(View.INVISIBLE);
                imageDetails.setText("Request failed!");
            }
        });
    }


    public void uploadImage(Uri uri) {
        if (uri != null) {
            try {
                // scale the image to save on bandwidth
                Bitmap bitmap =
                        scaleBitmapDown(
                                MediaStore.Images.Media.getBitmap(getContentResolver(), uri),
                                MAX_DIMENSION,MAX_DIMENSION);

                mainImage.setImageBitmap(bitmap);

                callCloudVision(bitmap);

            } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
        }
    }


    private Vision.Images.Annotate prepareAnnotationRequest(Bitmap bitmap) throws IOException {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        VisionRequestInitializer requestInitializer =
                new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                    /**
                     * We override this so we can inject important identifying fields into the HTTP
                     * headers. This enables use of a restricted cloud platform API key.
                     */
                    @Override
                    protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                            throws IOException {
                        super.initializeVisionRequest(visionRequest);

                        String packageName = getPackageName();
                        visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                        String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                        visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                    }
                };

        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(requestInitializer);

        Vision vision = builder.build();

        BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                new BatchAnnotateImagesRequest();
        batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
            AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

            // Add the image
            Image base64EncodedImage = new Image();
            // Convert the bitmap to a JPEG
            // Just in case it's a format that Android understands but Cloud Vision
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
            byte[] imageBytes = byteArrayOutputStream.toByteArray();

            // Base64 encode the JPEG
            base64EncodedImage.encodeContent(imageBytes);
            annotateImageRequest.setImage(base64EncodedImage);

            // add the features we want
            annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                Feature labelDetection = new Feature();
                labelDetection.setType("LABEL_DETECTION");
                labelDetection.setMaxResults(MAX_LABEL_RESULTS);
                add(labelDetection);
            }});

            // Add the list of one thing to the request
            add(annotateImageRequest);
        }});

        Vision.Images.Annotate annotateRequest =
                vision.images().annotate(batchAnnotateImagesRequest);
        // Due to a bug: requests to Vision API containing large images fail when GZipped.
        annotateRequest.setDisableGZipContent(true);
        Log.d(TAG, "created Cloud Vision request object, sending request");

        return annotateRequest;
    }


    private void callCloudVision(final Bitmap bitmap) {
        // Switch text to loading
        imageDetails.setText(R.string.loading_message);
        progressBar.setVisibility(View.VISIBLE);

        // Do the real work in an async task, because we need to use the network anyway
        try {
            AsyncTask<Object, Void, String> labelDetectionTask = new GoogleCloudObjectDetectionTask(this, prepareAnnotationRequest(bitmap));
            labelDetectionTask.execute();
        } catch (IOException e) {
            Log.d(TAG, "failed to make API request because of other IOException " +
                    e.getMessage());
        }
    }


    private Bitmap scaleBitmapDown(Bitmap bitmap, int maxWidthDimension, int maxHeightDimension) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = maxWidthDimension;
        int resizedHeight = maxHeightDimension;

        if (originalHeight > originalWidth) {
            resizedHeight = maxHeightDimension;
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedWidth = maxWidthDimension;
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        } else if (originalHeight == originalWidth) {
            resizedHeight = maxHeightDimension;
            resizedWidth = maxWidthDimension;
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }


    private static String convertResponseToString(BatchAnnotateImagesResponse response) {
        StringBuilder message = new StringBuilder();

        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
        if (labels != null) {
            for (EntityAnnotation label : labels) {
                message.append(String.format(Locale.US, "%.3f%% %s", label.getScore()*100, label.getDescription()));
                message.append("\n");
            }
        } else {
            message.append("nothing");
        }

        return message.toString();
    }


    // Unused
    private static class GoogleCloudObjectDetectionTask extends AsyncTask<Object, Void, String> {
        private final WeakReference<MainActivity> mActivityWeakReference;
        private Vision.Images.Annotate mRequest;
        private long startTime;
        private long elapsedTime;

        GoogleCloudObjectDetectionTask(MainActivity activity, Vision.Images.Annotate annotate) {
            mActivityWeakReference = new WeakReference<>(activity);
            mRequest = annotate;
        }

        @Override
        protected String doInBackground(Object... params) {
            try {
                Log.d(TAG, "created Cloud Vision request object, sending request");
                startTime = System.currentTimeMillis();
                BatchAnnotateImagesResponse response = mRequest.execute();
                elapsedTime = System.currentTimeMillis() - startTime;
                return convertResponseToString(response);

            } catch (GoogleJsonResponseException e) {
                Log.d(TAG, "failed to make API request because " + e.getContent());
            } catch (IOException e) {
                Log.d(TAG, "failed to make API request because of other IOException " +
                        e.getMessage());
            }
            return "Cloud Vision API request failed. Check logs for details.";
        }

        protected void onPostExecute(String result) {
            MainActivity activity = mActivityWeakReference.get();
            if (activity != null && !activity.isFinishing()) {
                TextView imageDetail = activity.findViewById(R.id.image_details);
                imageDetail.setText(result + "\n\n" + "Total elapsed request/response time in milliseconds: " + elapsedTime);
                ProgressBar progress = activity.findViewById(R.id.image_Progress);
                progress.setVisibility(View.INVISIBLE);

            }
        }
    }


    private void setLoadingUI(File image){
        Bitmap bitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
        mainImage.setImageBitmap(bitmap);
        imageDetails.setText(R.string.loading_message);
        progressBar.setVisibility(View.VISIBLE);
    }


    private void initiateTensorFlowClassifier() {
        executor.execute(new Runnable() {
            @Override
            public void run() {
                try {

                    classifier = TensorFlowImageClassifier.create(
                            getAssets(),
                            MODEL_FILE,
                            LABEL_FILE,
                            INPUT_SIZE,
                            IMAGE_MEAN,
                            IMAGE_STD,
                            INPUT_NAME,
                            OUTPUT_NAME);

                } catch (final Exception e) {
                    throw new RuntimeException("Error initializing TensorFlow!", e);
                }
            }
        });
    }


    private void disableUIComponents(){

    }


    private void enableUIComponents(){

    }



    @Override
    protected void onDestroy() {
        // Clear any configuration that was done!
        EasyImage.clearConfiguration(this);
        super.onDestroy();
        executor.execute(new Runnable() {
            @Override
            public void run() {
                classifier.close();
            }
        });
    }

}