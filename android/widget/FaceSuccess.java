package com.example.myapplication.widget;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.SoundPool;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.example.myapplication.R;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

/*
 * 1.设计自定义对话框样式
 * 2.设计style 去除自带样式
 * 3.将第一步的布局应用到自定义对话框
 * 4.实例化对话框（参数1：环境上下文 参数2：创建style R.style.mydialog） 并展示show,这一步是在调用控件的Activity编写
 *  */
public class FaceSuccess extends Dialog {

    private SoundPool soundPool;
    private HashMap<Integer,Integer> soundmap = new HashMap<Integer, Integer>();

    public FaceSuccess(@NonNull Context context, int themeResId,@NonNull Bitmap bitmap , String num, String name) {
        super(context,themeResId);
        //添加声音
        soundPool = new SoundPool(1, AudioManager.STREAM_SYSTEM,0);
        soundmap.put(1,soundPool.load(getContext(), R.raw.di,1));
        //为对话框添加布局
        setContentView(R.layout.face_success);
        ImageView imageView = findViewById(R.id.imageview);
        imageView.setImageBitmap(bitmap);
        EditText num_t = findViewById(R.id.num);
        EditText name_t = findViewById(R.id.name);
        num_t.setText(num);
        name_t.setText(name);
        setCancelable(false);




        soundPool.setOnLoadCompleteListener(new   SoundPool.OnLoadCompleteListener() {
            @Override
            public void onLoadComplete(SoundPool soundPool, int i, int i2) {
                //开始播放
                soundPool.play(soundmap.get(1),1,1,0,0,1);
            }
        });

    }

}

