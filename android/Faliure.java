package com.example.myapplication;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

public class Faliure extends Dialog {

    private SoundPool soundPool;
    private HashMap<Integer,Integer> soundmap = new HashMap<Integer, Integer>();
    public Faliure(@NonNull Context context, int themeResId, int faliureMode) {
        super(context, themeResId);
        //添加声音
        soundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM,0);
        soundmap.put(1,soundPool.load(getContext(),R.raw.error,1));
        //为对话框添加布局
        setContentView(R.layout.faliure);
        TextView faliueState = (TextView) findViewById(R.id.faliuestate);
        switch (faliureMode){
            case 1:
                faliueState.setText("人脸识别失败,请重试");
                break;
            case 200:
                faliueState.setText("RFID认证失败,请重试");
                break;
            case 300:
                faliueState.setText("密码输入错误,请重试");
                break;
        }
        setCancelable(false);

        soundPool.setOnLoadCompleteListener(new   SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int i, int i2) {
                //开始播放
                soundPool.play(soundmap.get(1),1,1,0,0,1);
            }
        });

//        Button fl_btn = (Button) findViewById(R.id.tuichu);
//        fl_btn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                dismiss();
//            }
//        });

    }
}