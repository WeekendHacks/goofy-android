package com.khb.goofy;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Socket;
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

public class PlayActivity extends Activity implements
        PlayerNotificationCallback, ConnectionStateCallback {

    private static final String CLIENT_ID = "a083380c45ed40eb9a13b8856f431fba";
    private static final String REDIRECT_URI = "http://localhost:8888/callback";
    private static final int REQUEST_CODE = 1337;

    private Socket mSocket;
    Button mPlayBtn;
    Button mStopBtn;
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
        mSocket.on("track", onTrack);
        mSocket.on("ping",onPing);
        mSocket.on("ponged_reply",onPongedReply);
        mSocket.on("stop_track",onStopTrack);
        mSocket.connect();

        mPlayBtn = (Button)findViewById(R.id.play);
        mStopBtn = (Button) findViewById(R.id.stop);
        mPlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject sendData = new JSONObject();
                try {
                    sendData.put("mood","Happy");
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
                    mMediaPlayer.release();
                    mlatency = 0;
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

    @Override
    public void onDestroy() {
        super.onDestroy();

        mSocket.disconnect();

        mSocket.off(Socket.EVENT_CONNECT, onConnect);
        mSocket.off(Socket.EVENT_DISCONNECT, onDisconnect);
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
