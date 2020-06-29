package com.covid19.covid19detection;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.github.mikephil.charting.charts.HorizontalBarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.LegendEntry;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.vansuita.pickimage.bean.PickResult;
import com.vansuita.pickimage.bundle.PickSetup;
import com.vansuita.pickimage.dialog.PickImageDialog;
import com.vansuita.pickimage.listeners.IPickClick;
import com.vansuita.pickimage.listeners.IPickResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BinaryClassifierActivity extends AppCompatActivity implements View.OnClickListener, IPickResult {

    private final int mInputSize = 224;
    private final String mModelPath = "converted_covid_model_binary_vgg19.tflite";
    private final String mLabelPath = "label.txt";
    private Classifier classifier;
    private ImageView imageView;
    private Button btnClassify;
    private AssetManager assets;
    private HorizontalBarChart barChart;
    private FloatingActionButton fabAdd;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classifier);

        assets = this.getAssets();

        try {
            initClassifier();
        } catch (IOException e) {
            e.printStackTrace();
        }
        initViews();
        initChart();

    }

    private void initViews() {
        imageView = (ImageView) findViewById(R.id.imageView);
        btnClassify = (Button) findViewById(R.id.btnClassify);
        fabAdd = (FloatingActionButton)findViewById(R.id.fabAdd);
        btnClassify.setOnClickListener(this);
        fabAdd.setOnClickListener(this);
        btnClassify.setVisibility(View.GONE);

    }

    private void initClassifier() throws IOException {
        classifier = new Classifier(assets, mModelPath, mLabelPath, mInputSize);
    }

    @Override
    public void onClick(View v) {

        switch(v.getId()){

            case R.id.fabAdd:
                barChart.clear();
                imageView.setImageBitmap(null);
                PickSetup setup = new PickSetup();
                PickImageDialog.build(setup).show(this);
                break;

            case R.id.btnClassify:
                if(imageView.getDrawable() != null){
                    Drawable drawable = ((ImageView) imageView).getDrawable();
                    Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
                    if(bitmap != null)
                        classifyXrays(bitmap);
                }else {
                    Toast.makeText(getApplicationContext(), " Select X-ray First!", Toast.LENGTH_SHORT).show();
                }
              break;
        }

    }

    private void classifyXrays(Bitmap bitmap) {

        AsyncTaskInference asyncTask=new AsyncTaskInference();
        asyncTask.execute(bitmap);
    }

    private class AsyncTaskInference extends AsyncTask<Bitmap, Void, float[][]> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(BinaryClassifierActivity.this);
            progressDialog.setMessage("Please wait...");
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(false);
            progressDialog.show();

        }
        @Override
        protected float[][] doInBackground(Bitmap... bitmaps) {

            return classifier.recognizeImageAllResult(bitmaps[0]);
        }
        @Override
        protected void onPostExecute(float[][] result) {
            super.onPostExecute(result);

            if(result!=null) {
                progressDialog.hide();

//                Log.d("RESULT", String.valueOf(result[0][0]));
//                Log.d("RESULT", String.valueOf(result[0][1]));

                displayChart(result);

            }else {
                progressDialog.show();
            }


        }
    }

    private void initChart(){
        barChart = findViewById(R.id.barChart);
        barChart.setDrawBarShadow(false);
        barChart.setDrawValueAboveBar(true);
        barChart.getDescription().setEnabled(false);
        barChart.setMaxVisibleValueCount(11);

        XAxis xl = barChart.getXAxis();
        xl.setPosition(XAxis.XAxisPosition.BOTTOM);
        xl.setDrawGridLines(false);
        xl.setCenterAxisLabels(false);
        xl.setTextSize(12);
        xl.setLabelCount(2);
        xl.setDrawLabels(true);

        YAxis yl = barChart.getAxisLeft();
        yl.setDrawAxisLine(true);
        yl.setDrawGridLines(true);
        yl.setAxisMinimum(0f);

        YAxis yr = barChart.getAxisRight();
        yr.setDrawAxisLine(true);
        yr.setDrawGridLines(true);
        yr.setAxisMinimum(0f);

        barChart.setFitBars(true);

        Legend l = barChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.LEFT);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setFormSize(8f);
        l.setXEntrySpace(4f);
        l.setFormToTextSpace(2f);
        l.setTextSize(10f);
//        l.setExtra(new int[]{getResources().getColor(R.color.colorCovid), getResources().getColor(R.color.colorNormal)},
//                new String[]{getResources().getString(R.string.label_covid), getResources().getString(R.string.label_normal)});
    }

    @Override
    public void onPickResult(PickResult r) {

        if (r.getError() == null) {
            imageView.setImageURI(null);
            imageView.setImageURI(r.getUri());
            btnClassify.setVisibility(View.VISIBLE);
        } else {
            Toast.makeText(this, r.getError().getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void displayChart(float[][] result) {

        List values = new ArrayList();

        for (int i = 0; i < result[0].length; i++) {
            values.add(new BarEntry(i, result[0][i]*100.f));
        }

        BarDataSet set1;
        set1 = new BarDataSet(values, "Results in percentages");
        set1.setColors(new int[] {Color.RED,Color.GREEN});
//        set1.setColors(ColorTemplate.COLORFUL_COLORS);
        set1.setLabel("Red: Covid  \n Green: Normal");

        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(set1);

        BarData data = new BarData(dataSets);
        data.setValueTextSize(12f);

        barChart.setData(data);
        barChart.animateY(2000);


    }
}

