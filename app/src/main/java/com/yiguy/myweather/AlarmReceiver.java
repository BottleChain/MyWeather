package com.yiguy.myweather;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.yiguy.bean.TodayWeather;
import com.yiguy.util.CommonUtil;
import com.yiguy.util.WeatherUtil;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

/**
 * Created by hadoop on 2016/12/23.
 */
public class AlarmReceiver extends BroadcastReceiver {

    // 通知栏
    private static final int NOTIFICATION_ID = 0x123;
    private NotificationManager nm;

    // 查询天气状况的消息
    private static final int QUERY_WEATHER = 10010;

    private Context mContext = null;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case QUERY_WEATHER:
                    myNotify(mContext, (String) msg.obj);
                    break;
                default:
                    break;
            }
        };
    };

    @Override
    public void onReceive(Context context, Intent arg1) {
        mContext = context;
        nm =(NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        query(context);
    }


    // 查询今日天气
    public void query(Context context){
        // 获取天气情况
        // 获取城市编码
        SharedPreferences sharedPreferences = context.getSharedPreferences("config", context.MODE_PRIVATE);
        String cityCode = sharedPreferences.getString("main_city_code", "101010100");
        // 查询天气状况
        WeatherUtil.queryWeather(cityCode,handler);
    }

    public void myNotify(Context context, String response){
        TodayWeather weather = null;
        String weatherInfo = "";
        if(response != null) {
            List<TodayWeather> weatherList = WeatherUtil.parseXML(response);
            if (weatherList != null) {
                weather = weatherList.get(1);
            }
        }
        if(weather != null){
            // 将今日天气信息转为String
            weatherInfo = weather.getType() +" 气温：" +  weather.getLow() + "~" + weather.getHigh() +
                    "  空气质量:" + weather.getQuality();
        }
        Intent intent = new Intent(context, MainActivity.class);
        // 单击Notification 通知时将会启动Intent 对应的程序，实现页面的跳转
        PendingIntent pi = PendingIntent.getActivity(context, 0, intent, 0);
        Notification notify = new Notification.Builder(context)
                // 设置打开该通知，该通知自动消失
                .setAutoCancel(true)
                // 设置显示在状态栏的通知提示信息
                .setTicker("Yiguy天气提醒")
                // 设置通知的图标
                .setSmallIcon(R.mipmap.icon_weather)
                // 设置通知内容的标题
                .setContentTitle("Yiguy天气提醒")
                // 设置通知内容
                .setContentText(weatherInfo)
                .setWhen(System.currentTimeMillis())
                // 设改通知将要启动程序的Intent
                .setContentIntent(pi)
                .getNotification();
        // 发送通知
        nm.notify(NOTIFICATION_ID, notify);
    }
}
