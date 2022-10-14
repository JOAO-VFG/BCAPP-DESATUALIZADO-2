package com.nibblelinx.BCAPP;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class Arduino extends AppCompatActivity {

    // Button ButtonMS9Back;

    Button buttonConectarArduino, buttonLED1, buttonLED2, buttonLED3;
    private static final int SOLICITA_ATIVACAO = 1;
    private static final int SOLICITA_CONEXAO = 2;

    private static final int MESSAGE_READ = 3;

    MyBluetoothService.ConnectedThread connectedThread;

    static Handler handler;

    StringBuilder dadosBluetooth = new StringBuilder();

    BluetoothAdapter meuBluetoothAdapter = null;
    BluetoothDevice meuDevice = null;
    BluetoothSocket meuSocket = null;

    boolean conexao = false;
    UUID meu_UUID =  UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arduino);

        buttonConectarArduino = findViewById(R.id.buttonConectarArduino);
        buttonLED1 = findViewById(R.id.buttonLED1);
        buttonLED2 = findViewById(R.id.buttonLED2);
        buttonLED3 = findViewById(R.id.buttonLED3);

        meuBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Register for broadcasts when a device is discovered.
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver, filter);

        if(meuBluetoothAdapter == null) {
            Toast.makeText(getApplicationContext(), "Seu dispositivo não possui bluetooth", Toast.LENGTH_LONG).show();
        }else if(!meuBluetoothAdapter.isEnabled()) {
            Intent ativaBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(ativaBluetooth, SOLICITA_ATIVACAO);
        }

        buttonConectarArduino.setOnClickListener(v -> {
            if(conexao){
                //desconectar
                try{
                    meuSocket.close();
                    conexao = false;
                    buttonConectarArduino.setText("Conectar");
                    Toast.makeText(getApplicationContext(), "O bluetooth foi desconectado", Toast.LENGTH_LONG).show();

                } catch (IOException erro){
                    Toast.makeText(getApplicationContext(), "ocorreu um erro: " + erro, Toast.LENGTH_LONG).show();

                }
            }else{
                Intent abreLista = new Intent(Arduino.this, ListaDispositivos.class);//conectar
                startActivityForResult(abreLista, SOLICITA_CONEXAO );
            }
        });

        buttonLED1.setOnClickListener(view -> {

            if(conexao) {
                connectedThread.enviar("led1");
            }else{
                Toast.makeText(getApplicationContext(), "Bluetooth não está conectado", Toast.LENGTH_LONG).show();
            }
        });



        buttonLED2.setOnClickListener(view -> {

            if(conexao){
                connectedThread.enviar("led2");
            }else{
                Toast.makeText(getApplicationContext(), "Bluetooth não está conectado", Toast.LENGTH_LONG).show();

            }
        });

        buttonLED3.setOnClickListener(view -> {

            if(conexao){
                connectedThread.enviar("led3");
            }else{
                Toast.makeText(getApplicationContext(), "Bluetooth não está conectado", Toast.LENGTH_LONG).show();

            }
        });

        handler = new Handler(Looper.myLooper()) {

            @Override
            public void handleMessage(Message msg) {
                if(msg.what == MESSAGE_READ){
                    String recebidos = (String) msg.obj;

                    dadosBluetooth.append(recebidos);

                    int fimInformacao = dadosBluetooth.indexOf("}");

                    if(fimInformacao > 0) {

                        String dadosCompletos = dadosBluetooth.substring(0, fimInformacao);

                        int tamInformacao = dadosCompletos.length();

                        if(dadosBluetooth.charAt(0) ==  '{'){

                            String dadosFinais = dadosBluetooth.substring(1, tamInformacao);

                            Log.d("Recebidos", dadosFinais);

                            if(dadosFinais.contains("l1on")){
                                buttonLED1.setText("LED 1 LIGADO");
                                Log.d("LED1", "ligado");
                            }else if(dadosFinais.contains("l1off")){
                                buttonLED1.setText("LED 1 DESLIGADO");
                                Log.d("LED1", "desligado");

                            }

                            if(dadosFinais.contains("l2on")){
                                buttonLED2.setText("LED 2 LIGADO");
                                Log.d("LED2", "ligado");
                            }else if(dadosFinais.contains("l2off")){
                                buttonLED2.setText("LED 2 DESLIGADO");
                                Log.d("LED2", "desligado");

                            }


                            if(dadosFinais.contains("l3on")){
                                buttonLED3.setText("LED 3 LIGADO");
                                Log.d("LED3", "ligado");
                            }else if(dadosFinais.contains("l3off")){
                                buttonLED3.setText("LED 3 DESLIGADO");
                                Log.d("LED3", "desligado");

                            }

                        }
                        dadosBluetooth.delete(0,dadosBluetooth.length());

                    }
                }
            }
        };



    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        // super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {

            case SOLICITA_ATIVACAO:
                if(resultCode == Activity.RESULT_OK){
                    Toast.makeText(getApplicationContext(), "O bluetooth foi ativado", Toast.LENGTH_LONG).show();
                }else{
                    Toast.makeText(getApplicationContext(), "O bluetooth não foi ativado, o aplicativo será encerrado", Toast.LENGTH_LONG).show();
                    finish();
                }
                break;
            case SOLICITA_CONEXAO:
                if(resultCode == Activity.RESULT_OK){
                    assert data != null;
                    String MAC = data.getExtras().getString(ListaDispositivos.ENDERECO_MAC);
                    Toast.makeText(getApplicationContext(), "MAC Final: " + MAC, Toast.LENGTH_LONG).show();
                    meuDevice = meuBluetoothAdapter.getRemoteDevice(MAC);

                    try {
                        meuSocket = meuDevice.createRfcommSocketToServiceRecord(meu_UUID);
                        meuSocket.connect();
                        conexao = true;

                        connectedThread = new MyBluetoothService.ConnectedThread(meuSocket);
                        connectedThread.start();

                        buttonConectarArduino.setText("Desconectar");
                        Toast.makeText(getApplicationContext(), "Você foi conectado com: " + MAC, Toast.LENGTH_LONG).show();
                    } catch (IOException erro) {
                        conexao = false;
                        Toast.makeText(getApplicationContext(), "Ocorreu um erro: " + MAC, Toast.LENGTH_LONG).show();
                    }
                }else{
                    Toast.makeText(getApplicationContext(), "falha ao obter o MAC", Toast.LENGTH_LONG).show();

                }
        }
    }

    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Discovery has found a device. Get the BluetoothDevice
                // object and its info from the Intent.
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                String deviceName = device.getName();
                String deviceHardwareAddress = device.getAddress(); // MAC address
            }
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Don't forget to unregister the ACTION_FOUND receiver.
        unregisterReceiver(receiver);
    }

    public static class MyBluetoothService {
        private static final String TAG = "MY_APP_DEBUG_TAG";

        // Defines several constants used when transmitting messages between the
        // service and the UI.


        private static class ConnectedThread extends Thread {
            private final InputStream mmInStream;
            private final OutputStream mmOutStream;

            public ConnectedThread(BluetoothSocket socket) {
                InputStream tmpIn = null;
                OutputStream tmpOut = null;

                // Get the input and output streams; using temp objects because
                // member streams are final.
                try {
                    tmpIn = socket.getInputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when creating input stream", e);
                }
                try {
                    tmpOut = socket.getOutputStream();
                } catch (IOException e) {
                    Log.e(TAG, "Error occurred when creating output stream", e);
                }

                mmInStream = tmpIn;
                mmOutStream = tmpOut;
            }

            public void run() {
                // mmBuffer store for the stream
                byte[] mmBuffer = new byte[1024];
                int numBytes; // bytes returned from read()

                // Keep listening to the InputStream until an exception occurs.
                while (true) {
                    try {
                        // Read from the InputStream.
                        numBytes = mmInStream.read(mmBuffer);

                        String dadosBt = new String(mmBuffer,0, numBytes);

                        // Send the obtained bytes to the UI activity.
                        handler.obtainMessage(MESSAGE_READ, numBytes, -1, dadosBt).sendToTarget();

                    } catch (IOException e) {
                        Log.d(TAG, "Input stream was disconnected", e);
                        break;
                    }
                }
            }


            public void enviar(String dadosEnviar) {
                byte[] msgBuffer = dadosEnviar.getBytes();
                try {
                    mmOutStream.write(msgBuffer);

                } catch (IOException ignored) {

                }
            }

        }
    }
}