package com.example.localsocketdemo;

import java.io.IOException;
import java.io.InputStream;

import android.app.Activity;
import android.net.LocalServerSocket;
import android.net.LocalSocket;
import android.net.LocalSocketAddress;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
*
* @author Denis Migol
*
*/
public class MainActivity extends Activity {
    
    public static String SOCKET_ADDRESS = "your.local.socket.address";
    
    // background threads use this Handler to post messages to
    // the main application thread
    private final Handler handler = new Handler();
    
    public class NotificationRunnable implements Runnable {
        private String message = null;
        
        public void run() {
            if (message != null && message.length() > 0) {
                showNotification(message);
            }
        }
        
        /**
        * @param message the message to set
        */
        public void setMessage(String message) {
            this.message = message;
        }
    }
    
    // post this to the Handler when the background thread notifies
    private final NotificationRunnable notificationRunnable = new NotificationRunnable();
    
    public void showNotification(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    
    class SocketListener extends Thread {
        private Handler handler = null;
        private NotificationRunnable runnable = null;
        
        public SocketListener(Handler handler, NotificationRunnable runnable) {
            this.handler = handler;
            this.runnable = runnable;
            this.handler.post(this.runnable);
        }
        
        /**
        * Show UI notification.
        * @param message
        */
        private void showMessage(String message) {
            this.runnable.setMessage(message);
            this.handler.post(this.runnable);
        }
        
        @Override
        public void run() {
            //showMessage("DEMO: SocketListener started!");
            try {
                LocalServerSocket server = new LocalServerSocket(SOCKET_ADDRESS);
                setName("LocalServerSocket-server");
                while (true) {
                    LocalSocket receiver = server.accept();
                    if (receiver != null) {
                        InputStream input = receiver.getInputStream();
                        
                        // simply for java.util.ArrayList
                        int readed = input.read();
                        int size = 0;
                        int capacity = 0;
                        byte[] bytes = new byte[capacity];
                        
                        // reading
                        while (readed != -1) {
                            // java.util.ArrayList.Add(E e);
                            capacity = (capacity * 3)/2 + 1;
                            //bytes = Arrays.copyOf(bytes, capacity);
                            byte[] copy = new byte[capacity];
                            System.arraycopy(bytes, 0, copy, 0, bytes.length);
                            bytes = copy;
                            bytes[size++] = (byte)readed;
                            
                            // read next byte
                            readed = input.read();
                        }
                        
                        showMessage(new String(bytes, 0, size));
                    }
                }
            } catch (IOException e) {
                Log.e(getClass().getName(), e.getMessage());
            }
        }
    }
    
    public static void writeSocket(String message) throws IOException {
        LocalSocket sender = new LocalSocket();
        sender.connect(new LocalSocketAddress(SOCKET_ADDRESS));
        sender.getOutputStream().write(message.getBytes());
        sender.getOutputStream().close();
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        new SocketListener(this.handler, this.notificationRunnable).start();
        
        Button send1 = (Button)findViewById(R.id.send_1_button);
        send1.setOnClickListener(new OnClickListener() {
            
            @Override
            public void onClick(View v) {
            	EditText msg = (EditText)findViewById(R.id.msg);
                try {
                    writeSocket(msg.getText().toString());
                } catch (IOException e) {
                    Log.e(getClass().getName(), e.getMessage());
                }
            }
            
        });
    }
}