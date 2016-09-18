package com.khb.goofy;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
import com.google.gson.Gson;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import com.spotify.sdk.android.player.Config;
import com.spotify.sdk.android.player.ConnectionStateCallback;
import com.spotify.sdk.android.player.Player;
import com.spotify.sdk.android.player.PlayerNotificationCallback;
import com.spotify.sdk.android.player.PlayerState;
import com.spotify.sdk.android.player.Spotify;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class PlayActivity extends Activity implements
        PlayerNotificationCallback, ConnectionStateCallback {

    private static final String CLIENT_ID = "a083380c45ed40eb9a13b8856f431fba";
    private static final String REDIRECT_URI = "http://localhost:8888/callback";
    private static final int REQUEST_CODE = 1337;
    private Button mCameraButton;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static StringBuffer emotion = new StringBuffer("happiness");
    private Socket mSocket;
    Button mPlayBtn;
    Button mStopBtn;
    Button mPlayNxt;
    private Boolean isConnected = true;
    private double mlatency = 0;
    private String Log_TAG = "PlayActivity";
    private MediaPlayer mMediaPlayer;
    private Config playerConfig;

    private Player mPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MainSocket socket = new MainSocket();
        mSocket = socket.getSocket();
        mSocket.on(Socket.EVENT_CONNECT, onConnect);
        mSocket.on(Socket.EVENT_DISCONNECT,onDisconnect);
        mSocket.on(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.on("track", onTrack);
        mSocket.on("ping",onPing);
        mSocket.on("ponged_reply",onPongedReply);
        mSocket.on("play_next",onPlayNext);
        mSocket.on("stop_track",onStopTrack);
        mSocket.connect();

        mPlayBtn = (Button)findViewById(R.id.play);
        mStopBtn = (Button) findViewById(R.id.stop);
        mPlayNxt = (Button) findViewById(R.id.next);
        mPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject sendData = new JSONObject();
                try {
                    sendData.put("mood",emotion.toString());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                mSocket.emit("mood",sendData);
            }
        });
        mStopBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSocket.emit("stop", "stop");
            }
        });
        mPlayNxt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mSocket.emit("next_song", "next");
            }
        });
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

        AuthenticationRequest.Builder builder = new AuthenticationRequest.Builder(CLIENT_ID,
                AuthenticationResponse.Type.TOKEN,
                REDIRECT_URI);
        builder.setScopes(new String[]{"user-read-private", "streaming"});
        AuthenticationRequest request = builder.build();

        AuthenticationClient.openLoginActivity(this, REQUEST_CODE, request);
    }

    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!isConnected) {
                        isConnected = true;
                        mMediaPlayer = new MediaPlayer();
                    }
                }
            });
        }
    };

    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    isConnected = false;
                    if(mMediaPlayer != null) {
                        mMediaPlayer.release();
                    }
                    mlatency = 0;
                    if (mSocket != null){
                        mSocket.disconnect();
                        mSocket.off(Socket.EVENT_CONNECT, onConnect);
                        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
                        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
                        mSocket.off("track", onTrack);
                        mSocket.off("ping",onPing);
                        mSocket.off("ponged_reply",onPongedReply);
                        mSocket.off("stop_track", onStopTrack);
                        mSocket.off("play_next", onPlayNext);
                    }
                }
            });
        }
    };

    private Emitter.Listener onTrack = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];

                    try {
                        double serverTime = data.getDouble("latency");
                        double totalLatency = serverTime + mlatency;
                        final String trackName = data.getString("track");
                        Log.d("trackname", trackName);
                        try{
                            Thread.sleep((long) (5000 - totalLatency));

//                            if(mMediaPlayer == null){
//                                mMediaPlayer = new MediaPlayer();
//                            }
//                            mMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.all);
//                            mMediaPlayer.start();
                            Spotify.getPlayer(playerConfig, this, new Player.InitializationObserver() {
                                @Override
                                public void onInitialized(Player player) {
                                    mPlayer = player;
                                    mPlayer.addConnectionStateCallback(PlayActivity.this);
                                    mPlayer.addPlayerNotificationCallback(PlayActivity.this);
                                    mPlayer.play(trackName);
                                }

                                @Override
                                public void onError(Throwable throwable) {
                                    Log.e("MainActivity", "Could not initialize player: " + throwable.getMessage());
                                }
                            });
                        }catch(InterruptedException e){
                            e.printStackTrace();
                        }
                    } catch (JSONException e) {
                        return;
                    }

                }
            });
        }
    };

    private Emitter.Listener onPlayNext = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];

                    try {
                        double serverTime = data.getDouble("latency");
                        double totalLatency = serverTime + mlatency;

                        try{
                            Thread.sleep((long) (5000 - totalLatency));

//                            if(mMediaPlayer == null){
//                                mMediaPlayer = new MediaPlayer();
//                            }
//                            mMediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.all);
//                            mMediaPlayer.start();
                            if(mPlayer != null){
                                mPlayer.skipToNext();
                            }
                        }catch(InterruptedException e){
                            e.printStackTrace();
                        }
                    } catch (JSONException e) {
                        return;
                    }

                }
            });
        }
    };

    private Emitter.Listener onStopTrack = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mMediaPlayer != null) {
                        if (mMediaPlayer.isPlaying()) {
                            mMediaPlayer.stop();
                            mMediaPlayer.release();
                            mMediaPlayer = null;
                        }
                    } else if(mPlayer != null) {
                        mPlayer.pause();
                        mPlayer.seekToPosition(0);
                    }
                }
            });
        }
    };

    private Emitter.Listener onPing = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];
                    JSONObject sendData = new JSONObject();

                    try {
                        double serverTime = data.getDouble("time");
                        double millis = System.currentTimeMillis();
                        sendData.put("time", serverTime);
                        sendData.put("ctime",millis);
                        mSocket.emit("ponged_new",sendData);

                    } catch (JSONException e) {
                        return;
                    }

                }
            });
        }
    };

    private Emitter.Listener onPongedReply = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    JSONObject data = (JSONObject) args[0];

                    try {
                        double sentTime = 0.0;
                        sentTime = data.getDouble("ctime");
                        if(sentTime != 0.0) {
                            double singleLatency = (System.currentTimeMillis() - sentTime)/2;
                            if(mlatency != 0){
                                mlatency = (mlatency + singleLatency) /2;
                            }
                            else {
                                mlatency = singleLatency;
                            }
                            Log.d(Log_TAG,String.valueOf(mlatency));
                        }
                    } catch (JSONException e) {
                        return;
                    }

                }
            });
        }
    };

    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(Object... args) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (mSocket != null){
                        mSocket.disconnect();
                        mSocket.off(Socket.EVENT_CONNECT, onConnect);
                        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
                        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
                        mSocket.off("track", onTrack);
                        mSocket.off("ping",onPing);
                        mSocket.off("ponged_reply",onPongedReply);
                        mSocket.off("stop_track", onStopTrack);
                        mSocket.off("play_next",onPlayNext);
                    }
                }
            });
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();

        mSocket.disconnect();

        mSocket.off(Socket.EVENT_CONNECT, onConnect);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
        mSocket.off(Socket.EVENT_CONNECT_ERROR, onConnectError);
        mSocket.off("track", onTrack);
        mSocket.off("ping",onPing);
        mSocket.off("ponged_reply",onPongedReply);
        mSocket.off("stop_track",onStopTrack);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, data);
            if (response.getType() == AuthenticationResponse.Type.TOKEN) {
                playerConfig = new Config(this, response.getAccessToken(), CLIENT_ID);

            }
        }
        else if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            ImageView imgView = (ImageView)findViewById(R.id.imageView);
            imgView.setImageBitmap(imageBitmap);
            sendImage(imageBitmap);
        }
    }
    private void sendImage(Bitmap image) {

        GetImageStatistics stat = new GetImageStatistics(getApplicationContext(), mSocket,emotion);
        stat.execute(image);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onLoggedIn() {

    }

    @Override
    public void onLoggedOut() {

    }

    @Override
    public void onLoginFailed(Throwable throwable) {

    }

    @Override
    public void onTemporaryError() {

    }

    @Override
    public void onConnectionMessage(String s) {

    }

    @Override
    public void onPlaybackEvent(EventType eventType, PlayerState playerState) {

    }

    @Override
    public void onPlaybackError(ErrorType errorType, String s) {

    }
}


class GetImageStatistics extends AsyncTask<Bitmap, Void , String> {
    protected Context mContext;
    protected Socket mSocket;
    String mEmotion = null;
    StringBuffer mBuff = null;
    GetImageStatistics(Context context, Socket socket , StringBuffer buff) {
        this.mContext = context;
        this.mSocket = socket;
        this.mBuff = buff;
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
        //emotion
        JSONObject sendData = new JSONObject();
        try {
            sendData.put("mood",mEmotion);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        mSocket.emit("mood",sendData);
        mBuff.delete(0, mBuff.length());
        mBuff.append(mEmotion);
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
            Log.d("Anger", maxEntry.toString().split("=")[0]);
            mEmotion = maxEntry.toString().split("=")[0];

        } catch (Exception e) {
            Log.d("Error", e.toString());
            return;
        }
        return;
    }
}