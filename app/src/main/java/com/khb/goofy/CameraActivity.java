package com.khb.goofy;

import android.app.Activity;
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
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class CameraActivity extends Activity {

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

            GetImageStatistics stat = new GetImageStatistics();
            stat.execute(image);
        }
    }
     class GetImageStatistics extends AsyncTask<Bitmap, Void, Integer> {
        @Override
        protected Integer doInBackground(Bitmap... Params) {
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
                Log.d("Message is", connection.getResponseMessage());
                InputStream is = connection.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String line;
                StringBuffer response = new StringBuffer();
                while((line = rd.readLine()) != null) {
                    response.append(line);
                    response.append('\n');
                }
                Log.d("Response", response.toString());
            } catch (Exception e) {
                Log.d("Error", e.toString());
            }
            return 0;
    }
}

