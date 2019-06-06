package com.mani.bluetooth_signal;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    Button send,conn,listen;
    EditText mess;
    TextView rec,stat;
    ListView listView;
    SendRecieve sendRecieve;
    BluetoothAdapter bluetoothAdapter;
    BluetoothDevice bluetoothDevice[];
    static final int STATE_LISTNING=1;
    static final int STATE_CONNECTING=2;
    static final int STATE_CONNECTED=3;
    static final int STATE_CONNECTION_FAILED=4;
    static final int STATE_MESSEGE_RECIVED=5;
    int REQUEST_ENABLE_BLUETOOTH=1;
    public static final String APP_NAME="bluetooth_signal";
    public static final  UUID MY_UUID= UUID.fromString("8ce255c0-223a-11e0-ac64-0803450c9a66");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.e(TAG, "onCreate: ");
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate: ");
        setContentView(R.layout.activity_main);
        Log.i(TAG, "onCreate: layout set ");
        init();
        Log.i(TAG, "onCreate: init done");
        bluetoothAdapter=BluetoothAdapter.getDefaultAdapter();
        Log.i(TAG, "onCreate: bluetooth set");
        if(!bluetoothAdapter.isEnabled())
        {

            Intent intent=new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,REQUEST_ENABLE_BLUETOOTH);
        }
        Log.i(TAG, "onCreate: bluetooth done");
        implementlistner();
        Log.i(TAG, "onCreate: listner done");

    }
    String TAG="ak47";
    private void implementlistner() {
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message=String.valueOf(mess.getText());
                sendRecieve.write(message.getBytes());
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i(TAG, "onItemClick: "+position+bluetoothDevice[position].getName());
                ClientClass clientClass=new ClientClass(bluetoothDevice[position]);
                clientClass.start();
            }
        });
        listen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                serverclass serverClass= new serverclass();
                serverClass.start();
                stat.setText("Connecting");
            }
        });
        conn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: " );
                Set<BluetoothDevice> bluetoothDevices=bluetoothAdapter.getBondedDevices();
                Log.e(TAG, "onClick: bounded devices" );
                String btd[]=new String[bluetoothDevices.size()];
                Log.e(TAG, "onClick: string done" );
                int index=0;
                Log.e(TAG, "onClick: log done" );
                if(bluetoothDevices.size()>0)
                {
                    Log.e(TAG, "onClick: size>0" );
                    bluetoothDevice=new BluetoothDevice[bluetoothDevices.size()];
                    Log.e(TAG, "onClick: size set" );
                    for(BluetoothDevice bt: bluetoothDevices)
                    {
                        Log.e(TAG, "onClick: loop" );
                        bluetoothDevice[index]=bt;
                        Log.e(TAG, "onClick: name of device" );
                        btd[index++]=bt.getName();
                        Log.e(TAG, "onClick: all process done" );
                    }
                }
                Log.e(TAG, "onClick: loop done" );
                ArrayAdapter<String> arrayAdapter=new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,btd);
                Log.e(TAG, "onClick: arry adpter set" );
                listView.setAdapter(arrayAdapter);
                Log.e(TAG, "onClick: all done" );
            }
        });
    }
    Handler handler=new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what)
            {
                case STATE_LISTNING:
                    stat.setText("listning");
                    break;
                case STATE_CONNECTING:
                    stat.setText("connecting");
                    break;
                case STATE_CONNECTED:
                    stat.setText("Connected");
                    break;
                case STATE_CONNECTION_FAILED:
                    stat.setText("failed");
                    break;
                case STATE_MESSEGE_RECIVED:
                    byte[] readBuffer=(byte[])msg.obj;
                    String tempMes=new String(readBuffer,0,msg.arg1);
                    rec.setText(tempMes);
                    //later
                    break;

            }
            return false;
        }
    });

    private void init() {
        send=findViewById(R.id.button);
        listen=findViewById(R.id.button3);
        mess=findViewById(R.id.editText);
        rec=findViewById(R.id.textView);
        stat=findViewById(R.id.textView2);
        conn=findViewById(R.id.button2);
        listView=findViewById(R.id.list);
        conn.setText("connect");
        send.setText("Send");
        mess.setHint("messege");
        rec.setText("");
        stat.setText("not connected");
        listen.setText("Listen");
    }
    private class serverclass extends Thread{
        private BluetoothServerSocket bluetoothServerSocket;
        public serverclass()
        {
            try {
                bluetoothServerSocket=bluetoothAdapter.listenUsingRfcommWithServiceRecord(APP_NAME,MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            super.run();
            BluetoothSocket socket=null;
            while (socket==null)
            {
                try {
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTING;
                    handler.sendMessage(message);
                    socket=bluetoothServerSocket.accept();
                } catch (IOException e) {
                    e.printStackTrace();
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTION_FAILED;
                    handler.sendMessage(message);
                }
                if(socket!=null)
                {
                    Message message=Message.obtain();
                    message.what=STATE_CONNECTED;
                    handler.sendMessage(message);
                    sendRecieve=new SendRecieve(socket);
                    sendRecieve.start();
                    break;
                }
            }

        }
    }
    private class ClientClass extends Thread{
        private BluetoothDevice bluetoothDevice;
        private BluetoothSocket bluetoothSocket;
        public ClientClass(BluetoothDevice device1)
        {
            bluetoothDevice=device1;
            Log.i(TAG, "ClientClass: "+device1.getName());
            try {
                bluetoothSocket=bluetoothDevice.createRfcommSocketToServiceRecord(MY_UUID);
                Log.i(TAG, "ClientClass: "+"bluetoothSocket set");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        public void run()
        {
            try {
                bluetoothSocket.connect();
                Message message=Message.obtain();
                message.what=STATE_CONNECTED;
                handler.sendMessage(message);
                sendRecieve=new SendRecieve(bluetoothSocket);
                sendRecieve.start();

            } catch (IOException e) {
                e.printStackTrace();
                Message message=Message.obtain();
                message.what=STATE_CONNECTION_FAILED;
                handler.sendMessage(message);

            }
        }
    }
    private class SendRecieve extends Thread{
        private final BluetoothSocket bluetoothSocket;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        public SendRecieve(BluetoothSocket socket)
        {
            bluetoothSocket=socket;
            InputStream tempIn=null;
            OutputStream tempOut=null;
            try {
                tempIn=bluetoothSocket.getInputStream();
                tempOut=bluetoothSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            inputStream=tempIn;
            outputStream=tempOut;

        }

        @Override
        public void run() {
            super.run();
            byte[] buffer =new byte[1024];
            int bytes;
            while (true)
            {
                try {
                    bytes=inputStream.read(buffer);
                    handler.obtainMessage(STATE_MESSEGE_RECIVED,bytes,-1,buffer).sendToTarget();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        public void write(byte[] bytes)
        {
            try {
                outputStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
