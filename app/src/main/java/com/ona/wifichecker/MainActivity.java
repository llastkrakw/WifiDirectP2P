package com.ona.wifichecker;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity  implements ClipboardManager.OnPrimaryClipChangedListener {

    WifiP2pManager manager;
    WifiP2pManager.Channel channel;
    WifiManager wifiManager;
    BroadcastReceiver receiver;
    IntentFilter intentFilter;
    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNames;
    WifiP2pDevice[] devices;
    private static final int MY_ACCESS_REQUEST_CODE = 100;


    private Button statusButton, buttonDiscover, send;
    private ListView item_recycler;
    private EditText message_send;
    public TextView message_receive, connection_status;


    private ClipboardManager cm;
    private ClipData pData;


    static final int MESSAGE_READ = 1;

    ServerClass serverClass;
    ClientClass clientClass;
    SendReceive sendReceive;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        updateUi();
        exqListner();

        cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.addPrimaryClipChangedListener(this);

        manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        channel = manager.initialize(this, getMainLooper(), null);
        receiver = new WiFiDirectBroadcastReceiver(manager, channel, this);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);


    }

    Handler handler =  new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(@NonNull Message message) {

            switch (message.what){

                case MESSAGE_READ :
                    byte[] readbuffer = (byte[]) message.obj;
                    String temMsg = new String(readbuffer, 0, message.arg1);
                    message_receive.setText(temMsg);
                    break;

            }

            return true;
        }
    });


    private void exqListner() {

        statusButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (wifiManager.isWifiEnabled()) {
                    wifiManager.setWifiEnabled(false);
                    String value = "ON WIFI";
                    statusButton.setText(value);
                } else {
                    wifiManager.setWifiEnabled(true);
                    String value = "OFF WIFI";
                    statusButton.setText(value);
                }

            }
        });


        buttonDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, MY_ACCESS_REQUEST_CODE);
                }

                manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {

                        String value = "Discovery Started";
                        connection_status.setText(value);

                    }

                    @Override
                    public void onFailure(int i) {

                        String value = "Discovery Failing Started";
                        connection_status.setText(value);

                    }
                });

            }
        });

    }


    WifiP2pManager.PeerListListener myPeerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {

            if (!peerList.getDeviceList().equals(peers)) {

                peers.clear();
                peers.addAll(peerList.getDeviceList());

                deviceNames = new String[peerList.getDeviceList().size()];
                devices = new WifiP2pDevice[peerList.getDeviceList().size()];

                int index = 0;

                for (WifiP2pDevice device : peerList.getDeviceList()) {

                    deviceNames[index] = device.deviceName;
                    devices[index] = device;
                    index++;

                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(), android.R.layout.simple_list_item_1, deviceNames);
                item_recycler.setAdapter(adapter);

                item_recycler.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                        final WifiP2pDevice device = devices[i];
                        WifiP2pConfig config = new WifiP2pConfig();
                        config.deviceAddress = device.deviceAddress;

                        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            ActivityCompat.requestPermissions(MainActivity.this, new String[] {Manifest.permission.ACCESS_FINE_LOCATION}, MY_ACCESS_REQUEST_CODE);
                        }
                        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
                            @Override
                            public void onSuccess() {

                                Toast.makeText(getApplicationContext(), "Connected to " + device.deviceAddress, Toast.LENGTH_SHORT).show();

                            }

                            @Override
                            public void onFailure(int i) {

                                Toast.makeText(getApplicationContext(), "Connection failure " + device.deviceAddress, Toast.LENGTH_SHORT).show();

                            }
                        });
                    }
                });

                send.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                         new MyTask().execute(message_send.getText().toString());

                    }
                });



            }

            if(peerList.getDeviceList().size() == 0){
                Toast.makeText(MainActivity.this, "Not decices found", Toast.LENGTH_LONG).show();
                return;
            }

        }
    };

    @Override
    public void onPrimaryClipChanged() {

        pData = cm.getPrimaryClip();
        ClipData.Item item = pData.getItemAt(0);
        new MyTask().execute(item.getText().toString());


    }


    public class MyTask extends AsyncTask<String, Void, Void>{


        @Override
        protected Void doInBackground(String... strings) {

            String[] msgs = strings.clone();
            String msg = msgs[0];
            sendReceive.write(msg.getBytes());

            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            Toast.makeText(getApplicationContext(), "good", Toast.LENGTH_LONG).show();
        }
    }


    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {

            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;

            if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner){

                String value = "Host";
                connection_status.setText(value);

                serverClass = new ServerClass();
                serverClass.start();

            }
            else if(wifiP2pInfo.groupFormed){

                String value = "Client";
                connection_status.setText(value);

                clientClass = new ClientClass(groupOwnerAddress);
                clientClass.start();

            }

        }
    };


    private void updateUi(){

        statusButton = (Button) findViewById(R.id.buttonStatut);
        buttonDiscover = (Button) findViewById(R.id.buttonDiscover);
        send = (Button) findViewById(R.id.send);

        message_receive = (TextView) findViewById(R.id.message_receive);
        connection_status = (TextView) findViewById(R.id.connectionStatus);

        item_recycler = (ListView) findViewById(R.id.item_recycler);

        message_send = (EditText) findViewById(R.id.message_send);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

    }



    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(receiver, intentFilter);

    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
    }

    public class ServerClass extends Thread{

        Socket socket;
        ServerSocket serverSocket;

        @Override
        public void run() {
            super.run();

            try {
                serverSocket = new ServerSocket(8888);
                socket = serverSocket.accept();

                sendReceive = new SendReceive(socket);
                sendReceive.start();

            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    public class ClientClass extends Thread{

        Socket socket;
        String hostAdd;

        public ClientClass(InetAddress hostAddress){

            hostAdd = hostAddress.getHostAddress();
            socket = new Socket();

        }

        @Override
        public void run() {
            super.run();

            try {

                socket.connect(new InetSocketAddress(hostAdd, 8888), 1000);
                sendReceive = new SendReceive(socket);
                sendReceive.start();


            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }


    public class SendReceive extends Thread{

        Socket socket;
        InputStream inputStream;
        OutputStream outputStream;

        public SendReceive(Socket skt){

            socket = skt;
            try {
                inputStream = skt.getInputStream();
                outputStream = skt.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void run() {
            super.run();

            byte[] buffer = new byte[1024];
            int bytes;

            while(socket != null){

                try {
                    bytes = inputStream.read(buffer);

                    if(bytes > 0){

                        handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

        }

        public void write(byte[] bytes){

            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_ACCESS_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "permission granted", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
            }
        }
    }

}