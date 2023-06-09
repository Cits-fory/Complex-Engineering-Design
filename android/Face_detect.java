package com.example.myapplication;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Base64;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.arcsoft.face.AgeInfo;
import com.arcsoft.face.ErrorInfo;
import com.arcsoft.face.FaceEngine;
import com.arcsoft.face.FaceInfo;
import com.arcsoft.face.GenderInfo;
import com.arcsoft.face.LivenessInfo;
import com.arcsoft.face.enums.DetectFaceOrientPriority;
import com.arcsoft.face.enums.DetectMode;
import com.example.myapplication.widget.FaceSuccess;
import com.example.myapplication.widget.LoadingDialog;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class Face_detect extends AppCompatActivity implements Camera.PreviewCallback{

    int FACE_MODE = 1;
    int RFID_MODE = 2;
    int PWD_MODE = 3;
    private int byteOffset;
    private String SendData;
    private Camera mCamera;
    private SurfaceView mSurfaceView;
    private SurfaceHolder mSurfaceHolder;
    private ConcurrentHashMap<Integer, Integer> rgbLivenessMap;
    FaceEngine faceEngine;
    boolean startFaceCheck=false;
    SurfaceView surfaceView;
    List<FaceInfo> faceInfoList;
    private Camera.Size previewSize;
    TextView face_num;
    TextView liveness;
    Button button_switch;
    private ExecutorService livenessExecutor;
    private final ReentrantLock livenessDetectLock = new ReentrantLock();
    private Rect recognizeArea = new Rect(Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE);
    public Handler mhandler;

    private FaceRectTransformer rgbFaceRectTransformer;
    private List<Integer> currentTrackIdList = new ArrayList<>();
    /**
     * 本次打开引擎后的最大faceId
     */
    private int currentMaxFaceId = 0;
    /**
     * 上次应用退出时，记录的该App检测过的人脸数了
     */
    private int trackedFaceCount = 0;
    FaceRectView dualCameraFaceRectView;
    int frontCameraId;
    Button button_init;
    ImageView imageView;
    ImageView imageView2;
    private Handler handler;
    TextView state;


    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bluetoothDevice;
    private BluetoothSocket bluetoothSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private boolean isConnected=false;
    final String DEVICE_UUID = "00001101-0000-1000-8000-00805F9B34FB";
    // 这是一个随机生成的数字，用于确定哪些数据是来自这个应用程序的。
    final int REQUEST_ENABLE_BT = 1;
    // 此处 HC-05 蓝牙模块的 MAC 地址需根据自己的实际情况进行修改
    final String DEVICE_ADDRESS = "98:D3:71:FD:39:EA";
    private static final int MY_PERMISSIONS_REQUEST_CAMERA = 123;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_face);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.CAMERA },
                    MY_PERMISSIONS_REQUEST_CAMERA);
        }

        new Thread(() -> {
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter == null) {
                Looper.prepare();
                Toast.makeText(Face_detect.this, "设备不支持蓝牙功能", Toast.LENGTH_SHORT).show();
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


        surfaceView = findViewById(R.id.surfaceview);
        face_num = (TextView) findViewById(R.id.face_num);
        rgbLivenessMap = new ConcurrentHashMap<>();
        state = (TextView)findViewById(R.id.state);
        imageView2 = (ImageView) findViewById(R.id.image2);


        init();
        initVideoEngine(Face_detect.this, 2);

        // 获取前置摄像头 ID
        frontCameraId = -1;
        int cameraCount = Camera.getNumberOfCameras();
        for (int i = 0; i < cameraCount; i++) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                frontCameraId = i;
                break;
            }
        }
        // 获取相机实例
        mCamera = Camera.open(frontCameraId);

        // 获取 SurfaceView 和 SurfaceHolder
        mSurfaceView = findViewById(R.id.surfaceview);
        mSurfaceHolder = mSurfaceView.getHolder();

        // 设置相机参数
        Camera.Parameters parameters = mCamera.getParameters();
        parameters.setPreviewSize(640, 480);
        mCamera.setParameters(parameters);

        // 设置相机预览回调
        mCamera.setPreviewCallback(this);

        // 开始预览
        mCamera.startPreview();
        mSurfaceHolder.addCallback(new SurfaceHolder.Callback() {

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                // 将 SurfaceHolder 与相机绑定
                try {
                    mCamera.setPreviewDisplay(holder);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                // 开始预览
                mCamera.startPreview();
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                // 释放相机资源
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;
            }
        });

    }


    /**
     * 初始化
     * */
    private void init() {
        /*
         * 激活
         * */
        String APP_ID = "BKfvyX19e5UrUBk4Lw5zLRGghR7axmkbodwM3qLEAi2y";
        String SDK_KEY = "BQVj7Z6ig7hJZDW8ZZ9JiQzMaD73p2hmS4XFCWTw3KDq";
        String ACTIVE_KEY = "85Q1-11J3-D12Z-GF86";

        int code = FaceEngine.activeOnline(Face_detect.this,ACTIVE_KEY, APP_ID, SDK_KEY);
        if (code == ErrorInfo.MOK) {
            System.out.println("activeOnline success");
            Toast.makeText(Face_detect.this, "激活成功", Toast.LENGTH_SHORT).show();
        } else if (code == ErrorInfo.MERR_ASF_ALREADY_ACTIVATED) {
            System.out.println("already activated");
//            Toast.makeText(Face_detect.this, "已经激活", Toast.LENGTH_SHORT).show();
        } else {
            System.out.println("activeOnline failed, code is : " + code);
        }

        /*
         * 初始化
         * */
        faceEngine = new FaceEngine();
        int code_init = faceEngine.init(getApplicationContext(), DetectMode.ASF_DETECT_MODE_VIDEO, DetectFaceOrientPriority.ASF_OP_ALL_OUT,2, 0);
        if (code_init != ErrorInfo.MOK) {
            Toast.makeText(this, "init failed, code is : " + code,
                    Toast.LENGTH_SHORT).show();
        } else {
            System.out.println("init success");
//            Toast.makeText(Face_detect.this, "初始化成功", Toast.LENGTH_SHORT).show();
        }

        rgbFaceRectTransformer = new FaceRectTransformer(
                640, 480,
                2000, 1200,
                0, frontCameraId, true,
                ConfigUtil.isDrawRgbRectHorizontalMirror(this),
                ConfigUtil.isDrawRgbRectVerticalMirror(this)
        );
        livenessExecutor = new ThreadPoolExecutor(1, 1,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                r -> {
                    Thread t = new Thread(r);
                    t.setName("flThread-" + t.getId());
                    return t;
                });

        handler = new Handler(Looper.myLooper()) {
            @SuppressLint("SetTextI18n")
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what) {
                    case 0:
                        String[] stringdata = (String[]) msg.obj;
                        System.out.println(stringdata[0]);
                        String face_data = stringdata[0];
                        String name = stringdata[1];
                        String id = stringdata[2];
                        Bitmap bitmap = base64ToBitmap(face_data);
                        FaceSuccess md = new FaceSuccess(Face_detect.this,R.style.success,bitmap,id,name); //传入值
                        closeConnection();
                        md.show();
                        if(md.isShowing()) {
                            new Handler().postDelayed(() -> {
                                md.dismiss();
                                ( Face_detect.this).finish();
                            }, 4000);
                        }
                        break;
                    case 1:
                        LoadingDialog.close();
                        // 获取当前文本
                        String currentText = state.getText().toString();

                        // 追加新的文本
                        String appendedText = currentText + " \n已完成人脸识别处理！";
                        state.setText(appendedText);

                        break;
                    case 2:
//                        Toast.makeText(Face_detect.this,"人脸检测失败，请重试",Toast.LENGTH_SHORT);
//                        mCamera.startPreview();
//                        state.setText("人脸识别");

                        Faliure fl = new Faliure(Face_detect.this,R.style.success,1); //人脸失败
                        fl.show();
                        if(fl.isShowing()) {
                            new Handler().postDelayed(() -> {
                                fl.dismiss();
                                mCamera.startPreview();
                                state.setText("人脸识别");
                            }, 3000);
                        }

                    case 3:
                        String nums = (String) msg.obj;
                        System.out.println("已开门"+nums);

                }
            }

        };
    }
    //回调函数
    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // 获取 NV21 数据
        byte[] nv21Data = data;
        /*
         * 显示人脸个数
         * */
        List<FaceInfo> faceInfoList = new ArrayList<>();
        long start = System.currentTimeMillis();
        int code = faceEngine.detectFaces(nv21Data, 640, 480,
                FaceEngine.CP_PAF_NV21, faceInfoList);
        if (code == ErrorInfo.MOK && faceInfoList.size() > 0) {
            //  Toast.makeText(Face_detect.this,"检测成功",Toast.LENGTH_SHORT).show();

        } else {
            //   Toast.makeText(Face_detect.this,"检测失败",Toast.LENGTH_SHORT).show();
        }
        face_num.setText("人脸数：" + faceInfoList.size());
        refreshTrackId(faceInfoList);
        if(faceInfoList.size()>0) {
            List<FacePreviewInfo> facePreviewInfoList = onPreviewTransfer(nv21Data, faceInfoList);
            processLiveness(nv21Data, facePreviewInfoList, this);
            if (facePreviewInfoList != null ) {
                //这里检测完活体
                if (rgbFaceRectTransformer != null) {
                    List<FaceRectView.DrawInfo> rgbDrawInfoList = getDrawInfo(facePreviewInfoList, LivenessType.RGB);
                    if(rgbDrawInfoList.get(0).getLiveness() == LivenessInfo.ALIVE){
                        mCamera.stopPreview();
                        Rect rect = facePreviewInfoList.get(0).getForeRect();
                        NV21ToBitmap nv21ToBitmap = new NV21ToBitmap(this);
                        //转换，裁剪
                        Bitmap bitmap= nv21ToBitmap.nv21ToBitmap(nv21Data,640, 480);

                        LoadingDialog.show(Face_detect.this,"已成功识别人脸，正在上传数据，请稍候...");

                        // 获取当前文本
                        String currentText = state.getText().toString();
                        // 追加新的文本
                        String appendedText = currentText + " \n已成功识别人脸，正在上传数据，请稍候...";
                        state.setText(appendedText);

                        Thread thread = new Thread(new Runnable() {
                            @Override
                            public void run() {
                                System.out.println("666");
                                String face_data = Bitmap2StrByBase641(bitmap);
                                Bitmap test = base64ToBitmap(face_data);

                                String Jsondata = JsonObjectPack(FACE_MODE,face_data);
                                String jsondata = submitPost("http://服务器ip/check.php", Jsondata);


                                Message msg = new Message();
                                msg.what=1;
                                handler.sendMessage(msg);
                                parseJSONWithJSONObject(jsondata);
                            }
                        });
                        thread.start(); // 启动子线程
                        System.out.println(rect.width()+"==="+rect.height()+"++"+rect.left+"++"+rect.top);
                    }
//                            dualCameraFaceRectView.drawRealtimeFaceInfo(rgbDrawInfoList);//绘制人脸框
                }
            }
        }
        // 将 NV21 数据渲染到 SurfaceView 上
        Canvas canvas = null;
        try {
            if (canvas != null) {
                synchronized (mSurfaceHolder) {
                    // 将 NV21 数据转换为 Bitmap
                    YuvImage yuvImage = new YuvImage(nv21Data, ImageFormat.NV21, 640, 480, null);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    yuvImage.compressToJpeg(new Rect(0, 0, 640, 480), 100, outputStream);
                    byte[] jpegData = outputStream.toByteArray();
                    Bitmap bitmap = BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length);

                    // 将 Bitmap 绘制到 Canvas 上
                    canvas.drawBitmap(bitmap, 0, 0, null);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (canvas != null) {
                // 解锁 Canvas
                mSurfaceHolder.unlockCanvasAndPost(canvas);
            }
        }

    }

    public long getSystemTime(){
        long systemTimes = (long) Calendar.getInstance().getTimeInMillis();
        return systemTimes;
    }

    public String JsonObjectPack(int mode,String data){
        JSONObject obj = new JSONObject();
        try {
            obj.put("code",200);
            obj.put("open_mode",mode);
            switch (mode){
                case 1:
                    obj.put("face_data",data);
                    break;
                case 2:
                    obj.put("rfid_id",data);
                    break;
                case 3:
                    obj.put("password",data);
                    break;
            }
            obj.put("time",getSystemTime());
//            obj.put("result",1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String str = AES.encrypt(obj.toString());
        return str;
    }

    public static String Bitmap2StrByBase641(Bitmap bit) {
        BitmapFactory.Options options;
        options = new BitmapFactory.Options();

        options.inSampleSize = 6;
        ByteArrayOutputStream baos1 = new ByteArrayOutputStream();
        if (bit != null) {
            // 改图片质量
            bit.compress(Bitmap.CompressFormat.JPEG, 30, baos1);
            byte[] b1 = baos1.toByteArray();
            return (Base64.encodeToString(b1,
                    Base64.NO_WRAP));
        }
        return "";

    }

    private void parseJSONWithJSONObject(String jsonData){
        try {
            JSONObject jsonObject = stringToJSONObject(jsonData);
            int code = jsonObject.getInt("code");
            System.out.println("-====="+code);

            getSystemTime();
            if(code == 200){
                //解析json数据并转码图片
                String name = jsonObject.getString("name");
                String num = jsonObject.getString("num");
                String face_data = jsonObject.getString("face_data");
                int result =jsonObject.getInt("result");
                Bitmap bitmap = base64ToBitmap(face_data);

                Message msg = new Message();
                msg.what=0;
                msg.obj= new String[]{face_data,name,num};
                handler.sendMessage(msg);


                if(result == 1){
                    System.out.println(name);
                    System.out.println(num);
                    //添加蓝牙传输
                    sendData();
                }
            }
            else{
                String error = jsonObject.getString("msg");
                String id = jsonObject.getString("num");
                System.out.println(id);
                System.out.println(error);
                Message msg = new Message();
                msg.what=2;
                handler.sendMessage(msg);
            }
        } catch (JSONException e) {
            System.out.println(e);
        }

    }

    public static Bitmap base64ToBitmap(String base64Data) {
        byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    /**
     * 根据预览信息生成绘制信息
     *
     * @param facePreviewInfoList 预览信息
     * @return 绘制信息
     */
    public List<FaceRectView.DrawInfo> getDrawInfo(List<FacePreviewInfo> facePreviewInfoList, LivenessType livenessType) {
        List<FaceRectView.DrawInfo> drawInfoList = new ArrayList<>();
        for (int i = 0; i < facePreviewInfoList.size(); i++) {
            int liveness = livenessType == LivenessType.RGB ? facePreviewInfoList.get(i).getRgbLiveness() : facePreviewInfoList.get(i).getIrLiveness();
            Rect rect = livenessType == LivenessType.RGB ?
                    facePreviewInfoList.get(i).getRgbTransformedRect() :
                    facePreviewInfoList.get(i).getIrTransformedRect();

            // 根据识别结果和活体结果设置颜色
            int color;
            String name;

            switch (liveness) {
                case LivenessInfo.ALIVE:
                    color = RecognizeColor.COLOR_SUCCESS;
                    name = "ALIVE";
                    break;
                case LivenessInfo.NOT_ALIVE:
                    color = RecognizeColor.COLOR_FAILED;
                    name = "NOT_ALIVE";
                    break;
                default:
                    color = RecognizeColor.COLOR_UNKNOWN;
                    name = "UNKNOWN";
                    break;
            }

            drawInfoList.add(new FaceRectView.DrawInfo(rect, GenderInfo.UNKNOWN,
                    AgeInfo.UNKNOWN_AGE, liveness, color, name));
        }
        return drawInfoList;
    }

    /**
     * 处理帧数据
     *
     * @param rgbNv21     可见光相机预览回传的NV21数据
     * @param faceInfoList 人脸
     * @return 实时人脸处理结果，封装添加了一个trackId，trackId的获取依赖于faceId，用于记录人脸序号并保存
     */
    public List<FacePreviewInfo> onPreviewTransfer(@NonNull byte[] rgbNv21, @NonNull List<FaceInfo> faceInfoList) {
        List<FacePreviewInfo> facePreviewInfoList = new ArrayList<>();
        if (faceEngine != null && faceInfoList.size()>0) {

            for (int i = 0; i < faceInfoList.size(); i++) {
                FacePreviewInfo facePreviewInfo = new FacePreviewInfo(faceInfoList.get(i), currentTrackIdList.get(i));
                if (rgbFaceRectTransformer != null && recognizeArea != null) {
                    Rect rect = rgbFaceRectTransformer.adjustRect(faceInfoList.get(i).getRect());
                    Rect foreRect = rgbFaceRectTransformer.adjustRect(faceInfoList.get(i).getForeheadRect());
                    facePreviewInfo.setRgbTransformedRect(rect);
                    facePreviewInfo.setForeRect(foreRect);
                }
                facePreviewInfoList.add(facePreviewInfo);
            }
            clearLeftFace(facePreviewInfoList);
        } else {
            facePreviewInfoList.clear();
        }
        return facePreviewInfoList;
    }

    public String submitPost(String url, String paramContent) {
        StringBuffer responseMessage = null;
        java.net.URLConnection connection = null;
        java.net.URL reqUrl = null;
        OutputStreamWriter reqOut = null;
        InputStream in = null;
        BufferedReader br = null;
        String param = paramContent;
        try {
            System.out.println("url=" + url + "?" + paramContent + "\n");
            System.out.println("===========post method start=========");
            responseMessage = new StringBuffer();
            reqUrl = new java.net.URL(url);
            connection = reqUrl.openConnection();
            connection.setDoOutput(true);
            reqOut = new OutputStreamWriter(connection.getOutputStream());
            reqOut.write(param);
            reqOut.flush();
            int charCount = -1;
            in = connection.getInputStream();

            br = new BufferedReader(new InputStreamReader(in, "utf-8"));
            while ((charCount = br.read()) != -1) {
                responseMessage.append((char) charCount);
            }

        } catch (Exception ex) {
            System.out.println("url=" + url + "?" + paramContent + "\n e=" + ex);
        } finally {
            try {
                in.close();
                reqOut.close();
            } catch (Exception e) {
//                System.out.println("paramContent=" + paramContent + "|err=" + e);
                System.out.println("|err=" + e);
            }

        }
        System.out.println(responseMessage.toString());
        return responseMessage.toString();
    }

    public static JSONObject stringToJSONObject(String str) throws JSONException {
        String decryptstr=AES.decrypt(str);
        JSONObject jsonObject = new JSONObject(decryptstr);
        return jsonObject;
    }

    public void onFail(Exception e) {
        Log.e(TAG, "onFail:" + e.getMessage());
    }

    /**
     * 刷新trackId
     *
     * @param ftFaceList 传入的人脸列表
     */
    private void refreshTrackId(List<FaceInfo> ftFaceList) {
        currentTrackIdList.clear();
        for (FaceInfo faceInfo : ftFaceList) {
            currentTrackIdList.add(faceInfo.getFaceId() + trackedFaceCount);
        }
        if (!ftFaceList.isEmpty()) {
            currentMaxFaceId = ftFaceList.get(ftFaceList.size() - 1).getFaceId();
        }
    }
    /**
     * 清除人脸
     * */
    private void clearLeftFace(List<FacePreviewInfo> facePreviewInfoList) {
        Enumeration<Integer> keys = rgbLivenessMap.keys();
        while (keys.hasMoreElements()) {
            int key = keys.nextElement();
            boolean contained = false;
            for (FacePreviewInfo facePreviewInfo : facePreviewInfoList) {
                if (facePreviewInfo.getTrackId() == key) {
                    contained = true;
                    break;
                }
            }
            if (!contained) {
                rgbLivenessMap.remove(key);
            }
        }
    }

    private List<FacePreviewInfo> processLiveness(byte[] nv21, List<FacePreviewInfo> previewInfoList, Context context) {
        if (previewInfoList == null || previewInfoList.size() == 0) {
            return null;
        }
        if (!livenessDetectLock.isLocked() && livenessExecutor != null) {
            //对每一帧进行检测
            livenessExecutor.execute(() -> {
                List<FacePreviewInfo> facePreviewInfoList = new LinkedList<>(previewInfoList);
                livenessDetectLock.lock();
                try {
                    int processRgbLivenessCode;
                    if (facePreviewInfoList.isEmpty()) {
                        Log.e(TAG, "facePreviewInfoList isEmpty");
                    } else {
                        synchronized (faceEngine) {
                            processRgbLivenessCode = faceEngine.process(nv21, 640, 480, FaceEngine.CP_PAF_NV21,
                                    new ArrayList<>(Collections.singletonList(facePreviewInfoList.get(0).getFaceInfoRgb())), FaceEngine.ASF_LIVENESS);
                        }
                        if (processRgbLivenessCode != ErrorInfo.MOK) {
                            Log.e(TAG, "process RGB Liveness error: " + processRgbLivenessCode);
                        } else {
                            List<LivenessInfo> rgbLivenessInfoList = new ArrayList<>();
                            int getRgbLivenessCode = faceEngine.getLiveness(rgbLivenessInfoList);
                            if (getRgbLivenessCode != ErrorInfo.MOK) {
                                Log.e(TAG, "get RGB LivenessResult error: " + getRgbLivenessCode);
                            } else {
                                rgbLivenessMap.put(facePreviewInfoList.get(0).getTrackId(), rgbLivenessInfoList.get(0).getLiveness());
                            }
                        }
                    }
                } finally {
                    livenessDetectLock.unlock();
                }
            });
        }
        for (FacePreviewInfo facePreviewInfo : previewInfoList) {
            Integer rgbLiveness = rgbLivenessMap.get(facePreviewInfo.getTrackId());
            if (rgbLiveness != null) {
                facePreviewInfo.setRgbLiveness(rgbLiveness);
                if(rgbLiveness == LivenessInfo.ALIVE){
                    System.out.println("这里呢");
                }
            }
        }
        return previewInfoList;
    }

    /**
     * 初始化图片识别引擎（视频、拍照）
     * 调用FaceEngine的init方法初始化SDK，初始化成功后才能进一步使用SDK的功能。
     *
     * @param context
     * @param detectFaceMaxNum 最大检测人数
     */
    public boolean initVideoEngine(Context context, int detectFaceMaxNum) {
        faceEngine = new FaceEngine();
        int faceEngineCode = faceEngine.init(context,
                DetectMode.ASF_DETECT_MODE_VIDEO,
                ConfigUtil.getFtOrient(context),
                detectFaceMaxNum,
                FaceEngine.ASF_FACE_RECOGNITION | FaceEngine.ASF_FACE_DETECT | FaceEngine.ASF_AGE | FaceEngine.ASF_GENDER | FaceEngine.ASF_LIVENESS);
        if (faceEngineCode == ErrorInfo.MOK) {
            Log.i(TAG, "人脸引擎初始化成功");
            startFaceCheck=true;
        } else {
            Log.i(TAG, "人脸引擎初始化失败 " + faceEngineCode);
        }
        return faceEngineCode == ErrorInfo.MOK;
    }




    public void connectDevice() {
        bluetoothDevice = bluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS);
        if (bluetoothDevice == null) {
            Looper.prepare();
            Toast.makeText(Face_detect.this, "没有找到指定的设备", Toast.LENGTH_SHORT).show();
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
                Toast.makeText(Face_detect.this, "Socket创建失败", Toast.LENGTH_SHORT).show();
                Looper.loop();
                return;
            }
        }

        // 开启蓝牙通信线程
        new Thread(() -> {
            boolean sign1 = true;
            while (sign1) {
                try {
                    try {
                        bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(UUID.fromString(DEVICE_UUID));
                    } catch (IOException e) {
                        System.out.println("Socket创建失败" + e);
                        Looper.prepare();
                        Toast.makeText(Face_detect.this, "Socket创建失败", Toast.LENGTH_SHORT).show();
                        Looper.loop();
                        return;
                    }
                    // 建立连接
                    bluetoothSocket.connect();
                    outputStream = bluetoothSocket.getOutputStream();
                    inputStream = bluetoothSocket.getInputStream();
                    isConnected = true;
                    Looper.prepare();
                    Toast.makeText(Face_detect.this, "蓝牙连接成功", Toast.LENGTH_SHORT).show();
                    Looper.loop();
                    sign1=false;
                    System.out.println("sadufhsaoidfj");

                } catch (IOException e) {
                    System.out.println("Socket连接失败" + e);
//                    Looper.prepare();
//                    Toast.makeText(Face_detect.this, "Socket连接失败，请重试", Toast.LENGTH_SHORT).show();
//                    Looper.loop();
//                    isConnected = false;
//                    closeConnection();
                }
            }
        }).start();
        //接收数据线程
        new Thread(() -> {
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
                                if (command.equals("010")) { // 如果命令码是001
                                    // 上传密码有关的数据到服务器
                                    System.out.println(command);
                                    Looper.prepare();
                                    Toast.makeText(this, "已开门", Toast.LENGTH_SHORT).show();
                                    Looper.loop();
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
        }).start();
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

    private void sendData() {
        String data = "0101111";
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




    /**
     * 绘制RGB、IR画面的实时人脸信息
     *
     * @param facePreviewInfoList RGB画面的实时人脸信息
     */
    private void drawPreviewInfo(List<FacePreviewInfo> facePreviewInfoList) {
        if (rgbFaceRectTransformer != null) {
            List<FaceRectView.DrawInfo> rgbDrawInfoList = getDrawInfo(facePreviewInfoList, LivenessType.RGB);
            if(rgbDrawInfoList.get(0).getLiveness() == LivenessInfo.ALIVE){
                Rect rect = facePreviewInfoList.get(0).getForeRect();

                System.out.println(rect.width()+"==="+rect.height());
            }

            dualCameraFaceRectView.drawRealtimeFaceInfo(rgbDrawInfoList);
        }
    }

    /**
     * 绘制人脸框
     *
     * @param bitmap
     */
    public Bitmap drawFaceRect(Bitmap bitmap) {
        //绘制bitmap
        bitmap = bitmap.copy(Bitmap.Config.RGB_565, true);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        paint.setStrokeWidth(10);
        paint.setColor(Color.YELLOW);

        for (int i = 0; i < faceInfoList.size(); i++) {
            //绘制人脸框
            paint.setStyle(Paint.Style.STROKE);
            canvas.drawRect(faceInfoList.get(i).getRect(), paint);
            //绘制人脸序号
            paint.setStyle(Paint.Style.FILL_AND_STROKE);
            paint.setTextSize(faceInfoList.get(i).getRect().width() / 2);
            canvas.drawText("" + i, faceInfoList.get(i).getRect().left, faceInfoList.get(i).getRect().top, paint);
        }
        return bitmap;
    }

    /**
     * 图像裁剪
     * @param bitmap 需裁剪的图片
     * @param rect 裁剪框
     * @return 返回的裁剪后图片
     *
     * */
    public Bitmap ImageCropWithRect(Bitmap bitmap,Rect rect)
    {
        if (bitmap == null)
        {
            return null;
        }

        int nw, nh, retX, retY;
        retX=rect.left;
        retY=rect.top;
        nw=rect.width();
        nh=rect.width();

        Matrix m = new Matrix();
//        m.postScale(1, -1);   //镜像垂直翻转
        m.postScale(-1, 1);   //镜像水平翻转
//        m.postRotate(-90);  //旋转-90度

        if(retX>0&&retY>0&&nw>0&&nh>0 ) {
            // 下面这句是关键
            Bitmap bmp = Bitmap.createBitmap(bitmap, retX, retY, nw, nh, m,
                    true);

            return bmp;
        }
        return null;
    }

    /**
     * 检查权限
     * @param neededPermissions 权限列表
     */
    protected boolean checkPermissions(String[] neededPermissions) {
        if (neededPermissions == null || neededPermissions.length == 0) {
            return true;
        }
        boolean allGranted = true;
        for (String neededPermission : neededPermissions) {
            allGranted &= ContextCompat.checkSelfPermission(this, neededPermission) == PackageManager.PERMISSION_GRANTED;
        }
        return allGranted;
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeConnection();
        System.out.println("onPause()");
        if (mCamera!=null){

            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }


    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("破坏");
        closeConnection();
        if (mCamera!=null){

            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }

    }
}
