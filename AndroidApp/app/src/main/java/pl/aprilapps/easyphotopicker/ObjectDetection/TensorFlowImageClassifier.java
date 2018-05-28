package pl.aprilapps.easyphotopicker.ObjectDetection;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * A classifier specialized to label images using TensorFlow.
 */
public class TensorFlowImageClassifier implements Classifier {

    private static final String TAG = "ImageClassifier";

    private static final int MAX_RESULTS = 1;

    // Config values.
    private String inputName;
    private String outputName;
    private int inputSize;
    private int imageMean;
    private float imageStd;

    // Pre-allocated buffers.
    private List<String> labels;
    private int[] intValues;
    private float[] floatValues;
    private float[] outputs;
    private String[] outputNames;

    private TensorFlowInferenceInterface inferenceInterface;

    private boolean runStats = false;

    private TensorFlowImageClassifier() {}

    /**
     * Initializes a native TensorFlow session for classifying images.
     *
     * @param assetManager  The asset manager to be used to load assets.
     * @param modelFilename The filepath of the model GraphDef protocol buffer.
     * @param labelFilename The filepath of label file for classes.
     * @param inputSize     The input size. A square image of inputSize x inputSize is assumed.
     * @param imageMean     The assumed mean of the image values.
     * @param imageStd      The assumed std of the image values.
     * @param inputName     The label of the image input node.
     * @param outputName    The label of the output node.
     * @throws IOException
     */
    public static Classifier create(
            AssetManager assetManager,
            String modelFilename,
            String labelFilename,
            int inputSize,
            int imageMean,
            float imageStd,
            String inputName,
            String outputName)
            throws IOException {
        TensorFlowImageClassifier classifier = new TensorFlowImageClassifier();
        classifier.inputName = inputName;
        classifier.outputName = outputName;

        String actualLabelFileName = labelFilename.split("file:///android_asset/")[1];

        classifier.labels = classifier.loadLabelList(assetManager, actualLabelFileName);

        classifier.inferenceInterface = new TensorFlowInferenceInterface(assetManager, modelFilename);
        // The shape of the output is [N, NUM_CLASSES], where N is the batch size.
        int numClasses =
                (int) classifier.inferenceInterface.graph().operation(outputName).output(0).shape().size(1);
        Log.i(TAG, "Read " + classifier.labels.size() + " labels, output layer size is " + numClasses);

        // Ideally, inputSize could have been retrieved from the shape of the input operation.  Alas,
        // the placeholder node for input in the graphdef typically used does not specify a shape, so it
        // must be passed in as a parameter.
        classifier.inputSize = inputSize;
        classifier.imageMean = imageMean;
        classifier.imageStd = imageStd;

        // Pre-allocate buffers.
        classifier.outputNames = new String[]{outputName};
        classifier.intValues = new int[inputSize * inputSize];
        classifier.floatValues = new float[inputSize * inputSize * 3];
        classifier.outputs = new float[numClasses];

        return classifier;
    }


    private List<String> loadLabelList(AssetManager assetManager, String labelPath) throws IOException {
        List<String> labelList = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(assetManager.open(labelPath)));
        String line;
        while ((line = reader.readLine()) != null) {
            labelList.add(line);
        }
        reader.close();
        return labelList;
    }


    @Override
    public List<Recognition> recognizeImage(final Bitmap bitmap) {
        // Preprocess the image data from 0-255 int to normalized float based
        // on the provided parameters.
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        for (int i = 0; i < intValues.length; ++i) {
            final int val = intValues[i];
            floatValues[i * 3 + 0] = (((val >> 16) & 0xFF) - imageMean) / imageStd;
            floatValues[i * 3 + 1] = (((val >> 8) & 0xFF) - imageMean) / imageStd;
            floatValues[i * 3 + 2] = ((val & 0xFF) - imageMean) / imageStd;
        }

        inferenceInterface.feed(inputName, floatValues, new long[]{1, inputSize, inputSize, 3});
        inferenceInterface.run(outputNames, runStats);
        inferenceInterface.fetch(outputName, outputs);

        List<Recognition> sortedRecognitions = getResults();

        return sortedRecognitions;
    }


    private List<Recognition> getResults(){

        // Find the best classifications.
        PriorityQueue<Recognition> pq =
                new PriorityQueue<Recognition>(
                        3,
                        new Comparator<Recognition>() {
                            @Override
                            public int compare(Recognition lhs, Recognition rhs) {
                                // Intentionally reversed to put high confidence at the head of the queue.
                                return Float.compare(rhs.getConfidence(), lhs.getConfidence());
                            }
                        });
        for (int i = 0; i < outputs.length; ++i) {
            pq.add(new Recognition("" + i, labels.size() > i ? labels.get(i) : "unknown", outputs[i]));
        }
        final ArrayList<Recognition> recognitions = new ArrayList<Recognition>();
        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }
        return recognitions;
    }


    @Override
    public void close() {
        inferenceInterface.close();
    }
}