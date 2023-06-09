package com.example.myapplication;

import android.graphics.Bitmap;
import android.os.Message;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Calendar;

public class PostToServer {
    public static String JsonObjectPack(int mode, String data){
        JSONObject obj = new JSONObject();
        try {
            obj.put("code",200);
            obj.put("open_mode",mode);
            switch (mode){
                case 1:
                    obj.put("face_data",data);
                case 2:
                    obj.put("rfid_id",data);
                case 3:
                    obj.put("password",data);
            }
            obj.put("time",getSystemTime());
//            obj.put("result",1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String str = AES.encrypt(obj.toString());
        return str;
    }
    public static long getSystemTime(){
        long systemTimes = (long) Calendar.getInstance().getTimeInMillis();
        return systemTimes;
    }
    public static String submitPost(String url, String paramContent) {
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
                System.out.println("paramContent=" + paramContent + "|err=" + e);
            }

        }
        System.out.println(AES.decrypt(responseMessage.toString()));
        return AES.decrypt(responseMessage.toString());
    }


    public static JSONObject stringToJSONObject(String str) throws JSONException {
        JSONObject jsonObject = new JSONObject(str);
        return jsonObject;
    }


}
