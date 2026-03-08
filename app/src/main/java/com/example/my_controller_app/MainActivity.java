package com.example.my_controller_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // UI 변수
    private TextView tvStatus;
    private Button btnConnect, btnForward, btnBackward, btnLeft, btnRight, btnHeightUp, btnHeightDown;

    // 블루투스 통신 변수
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private boolean isConnected = false;

    // HC-06(또는 HC-05) 기본 통신 UUID
    private static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 1. UI 연결
        tvStatus = findViewById(R.id.tvStatus);
        btnConnect = findViewById(R.id.btnConnect);
        btnForward = findViewById(R.id.btnForward);
        btnBackward = findViewById(R.id.btnBackward);
        btnLeft = findViewById(R.id.btnLeft);
        btnRight = findViewById(R.id.btnRight);
        btnHeightUp = findViewById(R.id.btnHeightUp);
        btnHeightDown = findViewById(R.id.btnHeightDown);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // 2. 블루투스 연결 버튼 클릭 이벤트
        btnConnect.setOnClickListener(v -> checkBluetoothPermissionAndConnect());

        // 3. 조종 버튼 터치 이벤트 설정 (누를 때 가고, 뗄 때 멈춤)
        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (!isConnected) return false;

                int action = event.getAction();
                if (action == MotionEvent.ACTION_DOWN) {
                    // 손가락이 버튼에 닿았을 때 (각각 고유 알파벳 전송)
                    if (v.getId() == R.id.btnForward) sendData("F");
                    else if (v.getId() == R.id.btnBackward) sendData("B");
                    else if (v.getId() == R.id.btnLeft) sendData("L");
                    else if (v.getId() == R.id.btnRight) sendData("R");
                    else if (v.getId() == R.id.btnHeightUp) sendData("U");
                    else if (v.getId() == R.id.btnHeightDown) sendData("W");
                } else if (action == MotionEvent.ACTION_UP) {
                    // 손가락을 뗐을 때 (무조건 정지 신호 'S' 전송)
                    sendData("S");
                }
                return false;
            }
        };

        // 버튼들에 터치 리스너 달아주기
        btnForward.setOnTouchListener(touchListener);
        btnBackward.setOnTouchListener(touchListener);
        btnLeft.setOnTouchListener(touchListener);
        btnRight.setOnTouchListener(touchListener);
        btnHeightUp.setOnTouchListener(touchListener);
        btnHeightDown.setOnTouchListener(touchListener);
    }

    // --- 블루투스 연결 관련 로직 ---
    private void checkBluetoothPermissionAndConnect() {
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "블루투스를 지원하지 않는 기기입니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this, "블루투스를 먼저 켜주세요!", Toast.LENGTH_SHORT).show();
            return;
        }

        // 안드로이드 12 이상 권한 체크 (간략화)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, 100);
                return;
            }
        }
        selectDevice();
    }

    @SuppressLint("MissingPermission")
    private void selectDevice() {
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() == 0) {
            Toast.makeText(this, "페어링된 기기가 없습니다. 스마트폰 설정에서 HC-06을 먼저 페어링하세요.", Toast.LENGTH_LONG).show();
            return;
        }

        List<String> listItems = new ArrayList<>();
        final List<BluetoothDevice> devices = new ArrayList<>();

        for (BluetoothDevice device : pairedDevices) {
            listItems.add(device.getName());
            devices.add(device);
        }

        CharSequence[] items = listItems.toArray(new CharSequence[0]);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("블루투스 기기 선택");
        builder.setItems(items, (dialog, item) -> connectToDevice(devices.get(item)));
        builder.show();
    }

    @SuppressLint("MissingPermission")
    private void connectToDevice(BluetoothDevice device) {
        tvStatus.setText("연결 중: " + device.getName() + "...");
        new Thread(() -> {
            try {
                bluetoothSocket = device.createRfcommSocketToServiceRecord(BT_UUID);
                bluetoothSocket.connect();
                outputStream = bluetoothSocket.getOutputStream();
                isConnected = true;

                runOnUiThread(() -> {
                    tvStatus.setText("연결 성공: " + device.getName());
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50")); // 초록색
                    Toast.makeText(MainActivity.this, "연결 완료! 조종을 시작하세요.", Toast.LENGTH_SHORT).show();
                });
            } catch (IOException e) {
                isConnected = false;
                runOnUiThread(() -> {
                    tvStatus.setText("연결 실패! 모듈 전원을 확인하세요.");
                    tvStatus.setTextColor(android.graphics.Color.parseColor("#F44336")); // 빨간색
                    try {
                        if (bluetoothSocket != null) bluetoothSocket.close();
                    } catch (IOException ex) { ex.printStackTrace(); }
                });
            }
        }).start();
    }

    // --- 아두이노로 데이터 쏘는 함수 ---
    private void sendData(String data) {
        if (isConnected && outputStream != null) {
            try {
                outputStream.write(data.getBytes());
            } catch (IOException e) {
                Toast.makeText(this, "데이터 전송 실패. 연결이 끊겼습니다.", Toast.LENGTH_SHORT).show();
                isConnected = false;
                tvStatus.setText("연결 끊김");
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            if (bluetoothSocket != null) bluetoothSocket.close();
        } catch (IOException e) { e.printStackTrace(); }
    }
}