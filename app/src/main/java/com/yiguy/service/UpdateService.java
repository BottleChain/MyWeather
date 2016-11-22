package com.yiguy.service;

import android.app.Application;
import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.yiguy.app.MyApplication;
import com.yiguy.bean.TodayWeather;
import com.yiguy.util.CommonUtil;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class UpdateService extends Service {

    private Timer timer = new Timer();
    // 每隔两个小时更新一次天气状况
    private static final int UPDATE_INTERVAL = 2 * 60 * 60 * 1000;
    private String log_tag = "";

    public UpdateService() {
        MyApplication myApp = (MyApplication) getApplication();
        log_tag = myApp.getLogTag();
    }

        @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                SharedPreferences sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
                String cityCode = sharedPreferences.getString("main_city_code", "101010100");
                TodayWeather todayWeather = CommonUtil.queryWeather(cityCode);
                // 发送广播
                Intent broadcastIntent = new Intent();
                Bundle bundle = new Bundle();
                bundle.putSerializable("newWeather", todayWeather);
                broadcastIntent.putExtras(bundle);
                broadcastIntent.setAction("Data_Update_Action");
                getBaseContext().sendBroadcast(broadcastIntent);
            }
        }, 0, UPDATE_INTERVAL);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(timer != null){
            timer.cancel();
        }
    }
}
