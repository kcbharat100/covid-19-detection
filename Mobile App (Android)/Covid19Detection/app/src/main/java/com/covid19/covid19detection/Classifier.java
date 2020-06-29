package com.covid19.covid19detection;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class Classifier {
    private Interpreter interpreter;
    public List lableList;
    private final int INPUT_SIZE;
    private final int PIXEL_SIZE = 3;
    private final int IMAGE_MEAN = 0;
    private final float IMAGE_STD = 255.f;
    private final int MAX_RESULTS = 3;
    private final float THRESHOLD = 0.4f;
    private AssetManager assetManager;


    public Classifier(AssetManager assetManager, String modelPath, String labelPath, int inputSize) throws IOException {

        this.INPUT_SIZE = inputSize;
        this.assetManager = assetManager;
        Interpreter.Options options = new Interpreter.Options();
        options.setNumThreads(5);
        options.setUseNNAPI(true);
        this.interpreter = new Interpreter((ByteBuffer) this.loadModelFile(assetManager, modelPath), options);
        this.lableList = loadLabelList(assetManager, labelPath);
    }

    private MappedByteBuffer loadModelFile(AssetManager assetManager, String modelPath) throws IOException {
        AssetFileDescriptor fileDescriptor = fileDescriptor = assetManager.openFd(modelPath);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    /** Reads label list from Assets. */
    private List<String> loadLabelList(AssetManager assetManager, String labelPath) throws IOException {
        List<String> labels = new ArrayList<String>();
        BufferedReader reader =
                new BufferedReader(new InputStreamReader(assetManager.open(labelPath)));
        String line;
        while ((line = reader.readLine()) != null) {
            labels.add(line);
        }
        reader.close();
        return labels;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(4 * this.INPUT_SIZE * this.INPUT_SIZE * this.PIXEL_SIZE);
        byteBuffer.order(ByteOrder.nativeOrder());
        int[] intValues = new int[this.INPUT_SIZE * this.INPUT_SIZE];
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        int pixel = 0;

        for (int i = 0; i < INPUT_SIZE; i++) {
            for (int j = 0; j < this.INPUT_SIZE; j++) {
                int input = intValues[pixel++];
                byteBuffer.putFloat((float) ((input >> 16 & 255) - this.IMAGE_MEAN) / this.IMAGE_STD);
                byteBuffer.putFloat((float) ((input >> 8 & 255) - this.IMAGE_MEAN) / this.IMAGE_STD);
                byteBuffer.putFloat((float) ((input & 255) - this.IMAGE_MEAN) / this.IMAGE_STD);
            }
        }
        return byteBuffer;
    }

    public List recognizeImage(Bitmap bitmap) {
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, this.INPUT_SIZE, this.INPUT_SIZE, false);
        ByteBuffer byteBuffer = this.convertBitmapToByteBuffer(scaledBitmap);
        int labelSize = lableList.size();
        float[][] result = new float[1][labelSize];

        interpreter.run(byteBuffer, result);

//        Log.d("T:result covid", String.valueOf(result[0][0]));
//        Log.d("T:result normal", String.valueOf(result[0][1]));
        return this.getSortedResult(result);
    }

    public float[][] recognizeImageAllResult(Bitmap bitmap) {
        Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, this.INPUT_SIZE, this.INPUT_SIZE, false);
        ByteBuffer byteBuffer = this.convertBitmapToByteBuffer(scaledBitmap);
        int labelSize = lableList.size();
        float[][] result = new float[1][labelSize];

        interpreter.run(byteBuffer, result);

//        Log.d("T:result covid", String.valueOf(result[0][0]));
//        Log.d("T:result normal", String.valueOf(result[0][1]));
        return result;
    }

    private final List getSortedResult(float[][] labelProbArray) {

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
        for (int i = 0; i < lableList.size(); ++i) {
            float confidence = labelProbArray[0][i];
            Log.d("Confidence", String.valueOf(confidence));
            if (confidence >= THRESHOLD)
                pq.add(new Recognition("" + i, lableList.size() > i ? lableList.get(i).toString() : "unknown", confidence));
        }
        final ArrayList<Recognition> recognitions = new ArrayList<Recognition>();
        int recognitionsSize = Math.min(pq.size(), MAX_RESULTS);
        for (int i = 0; i < recognitionsSize; ++i) {
            recognitions.add(pq.poll());
        }

        return recognitions;
    }

    public static class Recognition {
        /**
         * A unique identifier for what has been recognized. Specific to the class, not the instance of
         * the object.
         */
        private String id;
        /** Display name for the recognition. */
        private String title;
        /**
         * A sortable score for how good the recognition is relative to others. Higher should be better.
         */
        private float confidence;

        public Recognition(String id, String title, float confidence) {
            this.id = id;
            this.title = title;
            this.confidence = confidence;
        }

        public String toString() {
            return "Title = " + this.title + ", Confidence = " + String.format("(%.1f%%) ", confidence * 100.0f)+ ')';
        }

        public final String getId() {
            return this.id;
        }

        public final void setId(String var1) {
            this.id = var1;
        }

        public final String getTitle() {
            return this.title;
        }

        public final void setTitle(String var1) {
            this.title = var1;
        }

        public final float getConfidence() {
            return this.confidence;
        }

        public final void setConfidence(float var1) {
            this.confidence = var1;
        }


    }

}


