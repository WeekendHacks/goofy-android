package com.khb.goofy;

import android.app.Application;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import java.net.URISyntaxException;
/**
 * Created by melvin on 9/17/16.
 */
public class MainSocket extends Application {
    private Socket mSocket;
    {
        try {
            mSocket = IO.socket("http://goofy-web.cloudapp.net");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public Socket getSocket() {
        return mSocket;
    }
}
