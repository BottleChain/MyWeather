package com.yiguy.service;

import android.app.Application;
import android.app.IntentService;
import android.content.Intent;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.yiguy.app.MyApplication;
import com.yiguy.bean.TodayWeather;
import com.yiguy.util.CommonUtil;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

public class UpdateService extends IntentService {

    private Timer timer = new Timer();
    private static final int UPDATE_INTERVAL = 1000;
    private String log_tag = "";

    public UpdateService() {
        super("UpdateService");
        MyApplication myApp = (MyApplication) getApplication();
        log_tag = myApp.getLogTag();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                Log.i(log_tag, "service is running...");
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
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(timer != null){
            timer.cancel();
        }
    }
}
