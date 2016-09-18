package com.khb.goofy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import org.json.JSONObject;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class CameraActivity extends Activity {
public static final String EMOTION_ANGER = "com.khb.goofy.CameraActivity";
    private Button mCameraButton;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        mCameraButton = (Button)findViewById(R.id.camera_button);
        mCameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("Sending Data outside", "Data Sent");
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            sendImage(imageBitmap);
        }

    }

    private void sendImage(Bitmap image) {

            GetImageStatistics stat = new GetImageStatistics(getApplicationContext());
            stat.execute(image);
        }
    }
     class GetImageStatistics extends AsyncTask<Bitmap, Void, String> {
         protected Context mContext;
         String mEmotion = null;
         GetImageStatistics(Context context) {
    this.mContext = context;
}
        @Override
        protected String doInBackground(Bitmap... Params) {

            try {
                URL url = new URL("https://api.projectoxford.ai/emotion/v1.0/recognize");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setUseCaches (false);
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("content-type", "application/octet-stream");
                connection.setRequestProperty("Ocp-Apim-Subscription-Key", "b943675cbe41441a90e2b42d2abecf38");
                connection.connect();
                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                Params[0].compress(Bitmap.CompressFormat.PNG, 100, stream);
                byte[] byteArray = stream.toByteArray();

                OutputStream os = connection.getOutputStream();
                os.write(byteArray);
                os.flush();
                os.close();

                Log.d("Response code is", Integer.toString(connection.getResponseCode()));
                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));

                StringBuffer response = new StringBuffer();
                setEmotionObject(rd, response);

                Log.d("Response", response.toString());
            } catch (Exception e) {
                Log.d("Error", e.toString());
            }
            return mEmotion;
    }
         @Override
         protected void onPostExecute(String emotion) {
             Intent intent = new Intent(mContext, PlayActivity.class);
             intent.putExtra(CameraActivity.EMOTION_ANGER, mEmotion );
             mContext.startActivity(intent);
         }
         private  void setEmotionObject(BufferedReader rd, StringBuffer response) {
             String line;


             try {
                 while ((line = rd.readLine()) != null) {
                         response.append(line);
                         response.append('\n');
                     }
                 ArrayList<Object> one= new Gson().fromJson(response.toString(), ArrayList.class);
                 Map<String, Object> jsonJavaRootObject = (Map<String, Object>) one.get(0);
                 Map<String, Double> emotionMap = (Map)jsonJavaRootObject.get("scores");
                 Map.Entry<String, Double> maxEntry = null;
                 Map<String, Double> myMap = new HashMap<String, Double>();
                 myMap.put("anger", emotionMap.get("anger"));
                 myMap.put("sadness", emotionMap.get("sadness"));
                 myMap.put("happiness", emotionMap.get("happiness"));
                 for (Map.Entry<String, Double> entry : myMap.entrySet())
                 {
                     if (maxEntry == null || entry.getValue().compareTo(maxEntry.getValue()) > 0)
                         maxEntry = entry;
                 }
                 Log.d("Anger", maxEntry.toString());
             } catch (Exception e) {
                 Log.d("Error", e.toString());
                 return;
             }
             return;
         }
     }

