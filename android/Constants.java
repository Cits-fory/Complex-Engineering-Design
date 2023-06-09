package com.example.myapplication;

/**
 * 一些常量设置
 * <p>
 * 双目偏移可在 [参数设置] -> [识别界面适配] 界面中进行设置
 */
public class Constants {

    /**
     * 方式一： 填好APP_ID等参数，进入激活界面激活
     */
    public static final String APP_ID = "BKfvyX19e5UrUBk4Lw5zLRGghR7axmkbodwM3qLEAi2y";
    public static final String SDK_KEY = "BQVj7Z6ig7hJZDW8ZZ9JiQzMaD73p2hmS4XFCWTw3KDq";
    public static final String ACTIVE_KEY = "85Q1-11J3-D13L-P1P2";

    /**
     * 方式二： 在激活界面读取本地配置文件进行激活
     *
     * 配置文件名称，格式如下：
     * APP_ID:XXXXXXXXXXXXX
     * SDK_KEY:XXXXXXXXXXXXXXX
     * ACTIVE_KEY:XXXX-XXXX-XXXX-XXXX
     */
    public static final String ACTIVE_CONFIG_FILE_NAME = "active_result.dat";


    /**
     * 注册图所在路径
     */
    public static final String DEFAULT_REGISTER_FACES_DIR = "sdcard/arcfacedemo/register";
    public static final String IS_ACTIVE = "IS_ACTIVE";
}
