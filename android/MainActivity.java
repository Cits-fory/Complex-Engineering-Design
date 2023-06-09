package com.example.myapplication;

import static android.app.PendingIntent.getActivity;
import static com.example.myapplication.PostToServer.getSystemTime;
import static com.example.myapplication.PostToServer.stringToJSONObject;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatSpinner;
import androidx.collection.ArraySet;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.util.NetUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;


public class MainActivity extends AppCompatActivity implements View.OnClickListener, GestureDetector.OnGestureListener{
    public static final String URL_password_url = "http://服务器ip/check.php";
    private AppCompatSpinner mSpinner;
    private int byteOffset;
    private int sendMode = 3;
    private int sendRFIDMode = 2;
    private String SendData;
    private ArrayAdapter<String> mSpAdapter;
    private String[] mCities;
    private TextView tvWeather, tvTem, tvTemLowHigh, tvWin, tvAir, tvWarn;
    private ImageView ivWeather;
    private Button btn0, btn1, btn2, btn3, btn4, btn5, btn6, btn7, btn8, btn9;
    private EditText resultText;
    private Button DEL;
    private Button confirm;
    //蓝牙模块所使用的在下面
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private boolean isConnected;
    private List<String> mPermissionList;
    private Thread ReceiveThread =null;
    private  Thread SendThread =null;
    private SoundPool soundPool;
    private HashMap<Integer,Integer> soundmap = new HashMap<Integer, Integer>();
    private HashMap<Integer,Integer> soundmap1 = new HashMap<Integer, Integer>();
    private HashMap<Integer,Integer> soundmap2 = new HashMap<Integer, Integer>();

    // 这个 UUID 是用于 HC-05 模块通信的，不同的设备可能不一样，需要根据自己的设备进行更改。
    final String DEVICE_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    // 这是一个随机生成的数字，用于确定哪些数据是来自这个应用程序的。
    final int REQUEST_ENABLE_BT = 1;
    // 此处 HC-05 蓝牙模块的 MAC 地址需根据自己的实际情况进行修改
    final String DEVICE_ADDRESS = "98:D3:71:FD:39:EA";
    private Handler mHandler = new Handler(Looper.myLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                String weather = (String) msg.obj;
                updateUiOfWeather(weather);

            }
            if (msg.what == 1) {
                String warningText = (String) msg.obj;
                tvWarn.setText(warningText);
            }
            if (msg.what == 2) {
                Toast.makeText(MainActivity.this, "密码验证成功", Toast.LENGTH_SHORT).show();
                soundPool.play(soundmap1.get(1),1,1,0,0,1);
                //System.out.println("传递成功");
                sendData();

            }
            if (msg.what == 3) {
                Toast.makeText(MainActivity.this, "密码验证失败", Toast.LENGTH_SHORT).show();
                soundPool.play(soundmap2.get(1),1,1,0,0,1);
            }
            if (msg.what == 4) {
                Toast.makeText(MainActivity.this, "获取数据失败", Toast.LENGTH_SHORT).show();
            }
            if (msg.what == 5) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String Jsondata = PostToServer.JsonObjectPack(sendRFIDMode,SendData);
                        System.out.println(Jsondata);
                        PostToServer.submitPost("http://服务器ip/check.php", Jsondata);
//                        parseJSONWithJSONObject(receiveData);
                    }
                }).start();
            }
        }
    };
    private GestureDetector gestureDetector;

    private void updateUiOfWeather(String jsonStr) {
        try {
            JSONObject jsonObject = new JSONObject(jsonStr);
            String wea = jsonObject.optString("wea");
            String week = jsonObject.optString("week");
            String date = jsonObject.optString("date");
            String win = jsonObject.optString("win");
            String tem = jsonObject.optString("tem");
            String tem_night = jsonObject.optString("tem_night");
            String tem_day = jsonObject.optString("tem_day");
            String tv_air = jsonObject.optString("air");
            String wea_img = jsonObject.optString("wea_img");
            String win_speed = jsonObject.optString("win_speed");
            tvWin.setText("风向：" + win + "" + win_speed);
            tvTem.setText(tem + "°C");
            tvTemLowHigh.setText(tem_night + "°C" + "~" + tem_day + "°C");
            tvWeather.setText(wea + "(" + date + "," + week + ")");
            tvAir.setText("空气质量指数：" + tv_air);
            ivWeather.setImageResource(getImgResOfWeather(wea_img));


        } catch (JSONException e) {
            throw new RuntimeException(e);
        }


    }

    private int getImgResOfWeather(String weaStr) {
        // xue、lei、shachen、wu、bingbao、yun、yu、yin、qing
        int result = 0;
        switch (weaStr) {
            case "qing":
                result = R.drawable.biz_plugin_weather_qing;
                break;
            case "yin":
                result = R.drawable.biz_plugin_weather_yin;
                break;
            case "yu":
                result = R.drawable.biz_plugin_weather_dayu;
                break;
            case "yun":
                result = R.drawable.biz_plugin_weather_duoyun;
                break;
            case "bingbao":
                result = R.drawable.biz_plugin_weather_leizhenyubingbao;
                break;
            case "wu":
                result = R.drawable.biz_plugin_weather_wu;
                break;
            case "shachen":
                result = R.drawable.biz_plugin_weather_shachenbao;
                break;
            case "lei":
                result = R.drawable.biz_plugin_weather_leizhenyu;
                break;
            case "xue":
                result = R.drawable.biz_plugin_weather_daxue;
                break;
            default:
                result = R.drawable.biz_plugin_weather_qing;
                break;
        }

        return result;

    }


    @Override
    protected void onStart() {
        super.onStart();
        new Thread(() -> {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                Looper.prepare();
                Toast.makeText(MainActivity.this, "设备不支持蓝牙功能", Toast.LENGTH_SHORT).show();
                Looper.loop();
                return;
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH)
                    != PackageManager.PERMISSION_GRANTED) {
                // 如果没有蓝牙权限，则请求该权限
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.BLUETOOTH},
                        REQUEST_ENABLE_BT
                );
            }

            // 启用蓝牙设备
            if (!bluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }

            if (isConnected) {
                closeConnection();
            } else {
                connectDevice();
                System.out.println("蓝牙连接成功");
            }
        }).start();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //添加声音
        soundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM,0);
        soundmap.put(1,soundPool.load(MainActivity.this, R.raw.beep,1));
        soundmap1.put(1,soundPool.load(MainActivity.this, R.raw.di ,1));
        soundmap2.put(1,soundPool.load(MainActivity.this, R.raw.error,1));
        gestureDetector = new GestureDetector(this, this);
        btn0 = findViewById(R.id.btn0);
        btn1 = findViewById(R.id.btn1);
        btn2 = findViewById(R.id.btn2);
        btn3 = findViewById(R.id.btn3);
        btn4 = findViewById(R.id.btn4);
        btn5 = findViewById(R.id.btn5);
        btn6 = findViewById(R.id.btn6);
        btn7 = findViewById(R.id.btn7);
        btn8 = findViewById(R.id.btn8);
        btn9 = findViewById(R.id.btn9);
        tvWarn = findViewById(R.id.tv_warn);
        DEL = findViewById(R.id.delete);
        confirm = findViewById(R.id.btn_confirm);
        resultText = findViewById(R.id.result);
        btn0.setOnClickListener(this);
        btn1.setOnClickListener(this);
        btn2.setOnClickListener(this);
        btn3.setOnClickListener(this);
        btn4.setOnClickListener(this);
        btn5.setOnClickListener(this);
        btn6.setOnClickListener(this);
        btn7.setOnClickListener(this);
        btn8.setOnClickListener(this);
        btn9.setOnClickListener(this);
        DEL.setOnClickListener(this);
        confirm.setOnClickListener(this);
        resultText.setOnClickListener(this);
        initView();
        initPermission();


        GetWarningText();


    }

    @Override
    public void onClick(View view) {
        String input = resultText.getText().toString();

        switch (view.getId()) {//选择按钮id
            case R.id.btn0:
            case R.id.btn1:
            case R.id.btn2:
            case R.id.btn3:
            case R.id.btn4:
            case R.id.btn5:
            case R.id.btn6:
            case R.id.btn7:
            case R.id.btn8:
            case R.id.btn9:
                resultText.setText(input + ((Button) view).getText());
                break;
            case R.id.delete://从后往前删除字符
                if (!input.isEmpty())
                    resultText.setText(input.substring(0, input.length() - 1));
                break;
            case R.id.btn_confirm:
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        String Jsondata = PostToServer.JsonObjectPack(sendMode, input);
                        String receiveData = PostToServer.submitPost("http://服务器ip/check.php", Jsondata);
                        parseJSONWithJSONObject(receiveData);
                    }
                }).start();
                resultText.setText("");
                break;
        }
    }

    private void initView() {

        mSpinner = findViewById(R.id.sp_city);
        mCities = getResources().getStringArray(R.array.cities);
        mSpAdapter = new ArrayAdapter<>(this, R.layout.sp_item_layout, mCities);
        mSpinner.setAdapter(mSpAdapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedCity = mCities[position];

                getWeatherOfCity(selectedCity);

            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }

        });

        tvWeather = findViewById(R.id.tv_weather);
        tvAir = findViewById(R.id.tv_air);
        tvTem = findViewById(R.id.tv_tem);
        tvTemLowHigh = findViewById(R.id.tv_tem_low_high);
        tvWin = findViewById(R.id.tv_win);
        ivWeather = findViewById(R.id.iv_weather);


    }

    private void getWeatherOfCity(String selectedCity) {
// 开启子线程，请求网络
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 请求网络
                String weatherOfCity = NetUtil.getWeatherOfCity(selectedCity);
                // 使用handler将数据传递给主线程
                Message message = Message.obtain();
                message.what = 0;
                message.obj = weatherOfCity;
                mHandler.sendMessage(message);

            }
        }).start();

    }

    private void GetWarningText() {
// 开启子线程，请求网络
        new Thread(new Runnable() {
            @Override
            public void run() {
                while (true){
                        String warningText = NetUtil.GetWarningText();
                        // 使用handler将数据传递给主线程
                        parseJSONNotice(warningText);
                        System.out.println("获取公告");
                    try {
                        Thread.sleep(15000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }).start();

    }

    private void parseJSONWithJSONObject(String jsonData) {
        try {
            JSONObject jsonObject = stringToJSONObject(jsonData);
            int code = jsonObject.getInt("code");
            getSystemTime();
            if (code == 200) {
                //解析json数据
                String information = jsonObject.getString("msg");
                int result = jsonObject.getInt("result");

                if (result == 1) {
                    Message msg = new Message();
                    System.out.println("传递成功");
                    msg.what = 2;
                    msg.obj = information;
                    mHandler.sendMessage(msg);
                }
            } else {
                String error = jsonObject.getString("msg");
                System.out.println(error);
                Message msg = new Message();
                msg.what = 3;
                mHandler.sendMessage(msg);
            }
        } catch (JSONException e) {
            System.out.println(e);
        }

    }

    private void parseJSONNotice(String jsonData) {
        try {
            JSONObject jsonObject = stringToJSONObject(jsonData);
            int code = jsonObject.getInt("code");

            getSystemTime();
            if (code == 200) {
                //解析json数据
                JSONArray jsonArray = jsonObject.getJSONArray("context");
                String array = jsonArray.getString(0);
                JSONObject jsonObject1 = stringToJSONObject(array);
                String information = jsonObject1.getString("data");
                Message message = Message.obtain();
                boolean state = jsonObject.getBoolean("warning");
                if(state){
                    soundPool.play(soundmap.get(1),1,1,0,0,1);
                }


//                soundPool.setOnLoadCompleteListener(new   SoundPool.OnLoadCompleteListener() {
//                    @Override
//                    public void onLoadComplete(SoundPool soundPool, int i, int i2) {
//                        //开始播放
//                        soundPool.play(soundmap.get(1),1,1,0,0,1);
//                    }
//                });
                message.what = 1;
                message.obj = information;
                mHandler.sendMessage(message);
            } else {
                String error = jsonObject.getString("msg");
                System.out.println(error);
                Message msg = new Message();
                msg.what = 4;
                mHandler.sendMessage(msg);
            }
        } catch (JSONException e) {
            System.out.println(e);
        }

    }


    private void connectDevice() {
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS);
        if (bluetoothDevice == null) {
            Looper.prepare();
            Toast.makeText(MainActivity.this, "没有找到指定的设备", Toast.LENGTH_SHORT).show();
            Looper.loop();

            return;
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // 请求网络权限（需要用于扫描和连接蓝牙设备）
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_ENABLE_BT
            );
        }

        boolean sign = true;
        while (sign) {
            // 创建蓝牙 socket 连接
            try {
                bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(DEVICE_UUID));
                sign = false;
            } catch (IOException e) {
                System.out.println("Socket创建失败" + e);
                Looper.prepare();
                Toast.makeText(MainActivity.this, "Socket创建失败", Toast.LENGTH_SHORT).show();
                Looper.loop();
                return;
            }
        }

        // 开启蓝牙通信线程
        SendThread = new Thread(() -> {
            boolean sign1 = true;
            while (sign1) {
                try {
                    try {
                        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(DEVICE_UUID));
                    } catch (IOException e) {
                        System.out.println("Socket创建失败" + e);
                        Looper.prepare();
                        Toast.makeText(MainActivity.this, "Socket创建失败", Toast.LENGTH_SHORT).show();
                        Looper.loop();
                        return;
                    }
                    // 建立连接
                    bluetoothSocket.connect();
                    outputStream = bluetoothSocket.getOutputStream();
                    inputStream = bluetoothSocket.getInputStream();
                    isConnected = true;
                    Looper.prepare();
                    Toast.makeText(MainActivity.this, "蓝牙连接成功", Toast.LENGTH_SHORT).show();
                    Looper.loop();
                    sign1=false;
                    System.out.println("sadufhsaoidfj");

                } catch (IOException e) {
                    System.out.println("Socket连接失败" + e);
                }
            }
        });
        //接收数据线程
        ReceiveThread = new Thread(() -> {
            final int PACKET_LENGTH = 240;
            final byte[] buffer = new byte[PACKET_LENGTH];
            boolean isStarted = false;
            int bytes;
            while (true) {
                if (buffer != null && bluetoothSocket != null && isConnected) {
                    try {
                        bytes = inputStream.read(buffer);
                        if (bytes > 0) {
                            String strBuffer = new String(buffer, 0, bytes);
                            int start = strBuffer.indexOf("A");
                            if (start != -1) {
                                byteOffset = start +1;
                                isStarted = true;
                            }
                        }
                        if(isStarted) {
                            String message = new String(buffer, byteOffset, 15);
                            SendData = message;
                            if (message.length() >= 3) { // 确保信息长度至少为3位
                                String command = message.substring(0, 3); // 获取前三位命令码
                                if (command.equals("001")) { // 如果命令码是001
                                    // 上传密码有关的数据到服务器
                                    System.out.println(command);
                                    Message msg = Message.obtain();
                                    msg.what = 5;
                                    msg.obj = message;
                                    if (message != null) {
                                        mHandler.sendMessage(msg);
                                        Arrays.fill(buffer, (byte) 0);//每次向主线程发送一次数据后便将buffer中内容清空一次
                                    }
                                }else{
                                    Arrays.fill(buffer, (byte) 0);
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        break;
                    }
                }
            }
        });
        SendThread.start();
        ReceiveThread.start();
    }


    private void closeConnection() {
        if (outputStream != null) {
            try {
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (inputStream != null) {
            try {
                inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (bluetoothSocket != null) {
            try {
                bluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        isConnected = false;
    }

    public void sendData() {
        String data = "1001111";
        byte[] bytes = data.getBytes();
        if (bluetoothSocket != null && isConnected) {
            try {
                OutputStream outputStream = bluetoothSocket.getOutputStream();
                outputStream.write(bytes);
                outputStream.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void initPermission() {
        mPermissionList = new ArrayList<String>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 版本大于等于 Android12 时
            // 只包括蓝牙这部分的权限，其余的需要什么权限自己添加
            mPermissionList.add(Manifest.permission.BLUETOOTH_SCAN);
            mPermissionList.add(Manifest.permission.BLUETOOTH);
            mPermissionList.add(Manifest.permission.BLUETOOTH_CONNECT);
            mPermissionList.add(Manifest.permission.BLUETOOTH_ADMIN);
        } else {
            // Android 版本小于 Android12 及以下版本
            mPermissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            mPermissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }

        if (mPermissionList.size() > 0) {
            ActivityCompat.requestPermissions(MainActivity.this, mPermissionList.toArray(new String[mPermissionList.size()]), 1);
        }
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return gestureDetector.onTouchEvent(event);
    }
    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (e2.getY() < e1.getY()) {
            // 上滑操作，执行切换活动的代码
            System.out.println("开始");
            Intent intent = new Intent();
            intent.setClass(MainActivity.this,Face_detect.class);
            closeConnection();
            startActivity(intent);
            return true;
        }
        return false;
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeConnection();
        if(SendThread !=null && SendThread.isAlive()){
//            SendThread.stop();
//            closeConnection();
        }
        if(ReceiveThread !=null && ReceiveThread.isAlive()) {
//            ReceiveThread.stop();
        }

    }
}