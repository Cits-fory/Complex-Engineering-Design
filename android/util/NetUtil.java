package com.example.myapplication.util;

import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class NetUtil {

    public static final String URL_WEATHER_WITH_FUTURE = "https://www.yiketianqi.com/free/day?appid=97387519&appsecret=Lk3eK4sR&unescape=1";
    public static final String URL_warning_url = "http://1.117.101.235:8080/getnotice.php";



    public static String doGet(String urlStr) {
        String result = "";
        HttpURLConnection connection = null;
        InputStreamReader inputStreamReader = null;
        BufferedReader bufferedReader = null;
        // 连接网络
        try {
            URL urL = new URL(urlStr);
            connection = (HttpURLConnection) urL.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);

            // 从连接中读取数据(二进制)
            InputStream inputStream = connection.getInputStream();
            inputStreamReader = new InputStreamReader(inputStream);
            // 二进制流送入缓冲区
            bufferedReader = new BufferedReader(inputStreamReader);

            // 从缓存区中一行行读取字符串
            StringBuilder stringBuilder = new StringBuilder();
            String line = "";
            while ((line = bufferedReader.readLine()) != null) {
                stringBuilder.append(line);
            }
            result = stringBuilder.toString();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }

            if (inputStreamReader != null) {
                try {
                    inputStreamReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (bufferedReader != null) {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }

        return result;
    }


    public static String getWeatherOfCity(String city) {        //从网络中得到了天气数据的字符串
        // 拼接出获取天气数据的URL
        // https://www.yiketianqi.com/free/week?unescape=1&appid=97387519&appsecret=Lk3eK4sR
        String weatherUrl = URL_WEATHER_WITH_FUTURE + "&city=" + city;
        Log.d("fan", "----weatherUrl----" + weatherUrl);
        String weatherResult = doGet(weatherUrl);
        Log.d("fan", "----weatherResult----" + weatherResult);
        return weatherResult;
    }

    public static String GetWarningText() {        //从网络中得到了天气数据的字符串
        // 根据通告对应的url获取响应警告内容
        //http://1.117.101.235:8080/getnotice.php
        String warningUrl = URL_warning_url;
        Log.d("fan", "----weatherUrl----" + warningUrl);
        String warningResult = doGet(warningUrl);
        Log.d("fan", "----weatherResult----" + warningResult);
        return warningResult;
    }

    public static boolean doPost(String urlStr,Map<String, String> paramMap) {
        HttpURLConnection conn = null;
        OutputStream outputStream = null;
        boolean result = false;

        try {
            URL url = new URL(urlStr);
            // 1. 打开连接
            conn = (HttpURLConnection) url.openConnection();
            // 2. 准备请求参数
            String paramData = paramMapToString(paramMap);
            // 3. 设置连接信息
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(10 * 1000);
            conn.setRequestProperty("Content-Length", String.valueOf(paramData.length()));
            // 设置conn可以向服务端输出内容
            conn.setDoOutput(true);
            // 4. 获取输出流，并进行输出（也就是向服务端发送数据）
            outputStream = conn.getOutputStream();
            outputStream.write(paramData.getBytes());
            // 5. 获取服务端的响应结果
            int code = conn.getResponseCode();
            if (code == 200) {
                result = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
        return result;
    }

    public static String paramMapToString(Map<String, String> paramMap) {
        StringBuilder sb = new StringBuilder();
        try {
            Set<Map.Entry<String, String>> entries = paramMap.entrySet();
            for (Map.Entry<String, String> entry :
                    entries) {
                sb.append(entry.getKey())
                        .append("=")
                        .append(URLEncoder.encode(entry.getValue(), "UTF-8"))
                        .append("&");
            }
            // 去掉最后一个 &
            sb.deleteCharAt(sb.length() - 1);

        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return sb.toString();
    }

}
