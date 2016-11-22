package com.yiguy.myweather;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.yiguy.app.MyApplication;
import com.yiguy.bean.TodayWeather;
import com.yiguy.service.UpdateService;
import com.yiguy.util.NetUtil;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Xiaoyi on 2016/9/20.
 */
public class MainActivity extends Activity implements View.OnClickListener {

    private static final int UPDATE_TODAY_WEATHER = 1;
    private static final int UPDATE_WEATHER_FINISH = 2;

    private String log_tag = "";
    //更新按钮
    private ImageView mUpdateBtn;
    // 分享按钮
    private ImageView mShareBtn;
    //选择城市按钮
    private ImageView mCitySelect;

    private IntentFilter intentFilter;

    private BroadcastReceiver intentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TodayWeather newWeather = (TodayWeather) intent.getSerializableExtra("newWeather");
            updateTodayWeather(newWeather);
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date curDate = new Date(System.currentTimeMillis());    //获取当前时间
            String time = formatter.format(curDate);
            Log.i(log_tag, "天气数据更新了，更新时间为:" + time);
        }
    };

    private TextView cityTv, timeTv, humidityTv, weekTv, pmDataTv, pmQualityTv, temperatureTv, climateTv, windTv, city_nameTv, currentTemperatureTv;
    private ImageView weatherImg, pmImg;
    private  ProgressBar progressBar;


    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_TODAY_WEATHER:
                    updateTodayWeather((TodayWeather) msg.obj);
                    break;
                case UPDATE_WEATHER_FINISH:
                    updateFinish();
                    break;
                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_info);

        mUpdateBtn = (ImageView) findViewById(R.id.title_update_btn);
        mUpdateBtn.setOnClickListener(this);
        // 设置日志标识
        MyApplication myApp = (MyApplication) getApplication();
        log_tag = myApp.getLogTag();
        if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
            Log.d(log_tag, "网络OK");
        } else {
            Log.d(log_tag, "网络挂了");
            Toast.makeText(MainActivity.this, "网络挂了！", Toast.LENGTH_LONG).show();
        }
        mCitySelect = (ImageView) findViewById(R.id.title_city_manager);
        mCitySelect.setOnClickListener(this);

        // 在应用启动时，启动"定时更新数据"的service服务
        startService(new Intent(getBaseContext(), UpdateService.class));
        initView();
    }


    // 当天气数据更新之后，更新按钮停止旋转
    public void updateFinish(){
        mUpdateBtn.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams)mShareBtn.getLayoutParams();
        param.addRule(RelativeLayout.LEFT_OF, R.id.title_update_btn);
        mShareBtn.setLayoutParams(param);
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.title_city_manager) {
            Intent i = new Intent(this, SelectCity.class);
            startActivityForResult(i, 1);
        }
        // 点击“更新按钮"的响应事件
        if (view.getId() == R.id.title_update_btn) {
            SharedPreferences sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
            String cityCode = sharedPreferences.getString("main_city_code", "101010100");

            if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
                Log.d(log_tag, "网络OK");
                // 更新按钮开始旋转
                mUpdateBtn.setVisibility(View.GONE);
                progressBar.setVisibility(View.VISIBLE);
                // 开始查询之前，将更新按钮的参考位置设置为 progressBar
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams)mShareBtn.getLayoutParams();
                params.addRule(RelativeLayout.LEFT_OF, R.id.title_update_progress);
                mShareBtn.setLayoutParams(params);
                // 查询天气状况
                queryWeather(cityCode);

            } else {
                Log.d(log_tag, "网络挂了");
                Toast.makeText(MainActivity.this, "网络挂了！", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        intentFilter = new IntentFilter();
        intentFilter.addAction("Data_Update_Action");
        registerReceiver(intentReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(intentReceiver);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1 && resultCode == RESULT_OK) {
            String newCityCode = data.getStringExtra("cityCode");
            Log.d(log_tag, "选择的城市代码为" + newCityCode);

            if (NetUtil.getNetworkState(this) != NetUtil.NETWORN_NONE) {
                Log.d(log_tag, "网络OK");
                queryWeather(newCityCode);
            } else {
                Log.d(log_tag, "网络挂了");
                Toast.makeText(MainActivity.this, "网络挂了！", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * 查询某城市的天气信息
     *
     * @param cityCode 当前所在城市编码
     */
    private void queryWeather(String cityCode) {
        final String address = "http://wthrcdn.etouch.cn/WeatherApi?citykey=" + cityCode;
        Log.d(log_tag, address);
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection con = null;
                TodayWeather todayWeather = null;
                try {
                    URL url = new URL(address);
                    con = (HttpURLConnection) url.openConnection();
                    con.setRequestMethod("GET");
                    con.setConnectTimeout(8000);
                    con.setReadTimeout(8000);
                    InputStream in = con.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                    StringBuilder response = new StringBuilder();
                    String str;
                    while ((str = reader.readLine()) != null) {
                        response.append(str);
                    }
                    String responseStr = response.toString();
                    Log.d(log_tag, responseStr);
                    todayWeather = parseXML(responseStr);
                    if (todayWeather != null) {
                        Message msg = new Message();
                        msg.what = UPDATE_TODAY_WEATHER;
                        msg.obj = todayWeather;
                        mHandler.sendMessage(msg);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                    Message msg = new Message();
                    msg.what = UPDATE_WEATHER_FINISH;
                    mHandler.sendMessage(msg);
                }
            }
        }).start();
    }


    /**
     * 解析XML，获取当前城市的天气信息
     *
     * @param xmldata
     */
    private TodayWeather parseXML(String xmldata) {
        TodayWeather todayWeather = null;

        int fengxiangCount = 0;
        int fengliCount = 0;
        int dateCount = 0;
        int highCount = 0;
        int lowCount = 0;
        int typeCount = 0;
        try {
            XmlPullParserFactory fac = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = fac.newPullParser();
            xmlPullParser.setInput(new StringReader(xmldata));
            int eventType = xmlPullParser.getEventType();
            Log.d(log_tag, "pareseXML");
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    //判断当前事件是否为文档开始事件
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    //判断当前事件是否为标签元素开始事件
                    case XmlPullParser.START_TAG:
                        if (xmlPullParser.getName().equals("resp")) {
                            todayWeather = new TodayWeather();
                        }
                        if (todayWeather != null) {
                            if (xmlPullParser.getName().equals("city")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setCity(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("updatetime")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setUpdatetime(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("shidu")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setShidu(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("wendu")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setWendu(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("pm25")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setPm25(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("quality")) {
                                eventType = xmlPullParser.next();
                                todayWeather.setQuality(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("fengxiang") && fengxiangCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengxiang(xmlPullParser.getText());
                                fengxiangCount++;
                            } else if (xmlPullParser.getName().equals("fengli") && fengliCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setFengli(xmlPullParser.getText());
                                fengliCount++;
                            } else if (xmlPullParser.getName().equals("date") && dateCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setDate(xmlPullParser.getText());
                                dateCount++;
                            } else if (xmlPullParser.getName().equals("high") && highCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setHigh(xmlPullParser.getText());
                                highCount++;
                            } else if (xmlPullParser.getName().equals("low") && lowCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setLow(xmlPullParser.getText());
                                lowCount++;
                            } else if (xmlPullParser.getName().equals("type") && typeCount == 0) {
                                eventType = xmlPullParser.next();
                                todayWeather.setType(xmlPullParser.getText());
                                typeCount++;
                            }
                        }
                        break;
                    //判断当前事件是否为标签元素结束事件
                    case XmlPullParser.END_TAG:
                        break;
                }
                //进入下一个元素并触发相应事件
                eventType = xmlPullParser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return todayWeather;
    }

    void initView() {
        city_nameTv = (TextView) findViewById(R.id.title_city_name);
        cityTv = (TextView) findViewById(R.id.city);
        timeTv = (TextView) findViewById(R.id.time);
        humidityTv = (TextView) findViewById(R.id.humidity);
        weekTv = (TextView) findViewById(R.id.week_today);
        pmDataTv = (TextView) findViewById(R.id.pm_data);
        pmQualityTv = (TextView) findViewById(R.id.pm2_5_quality);
        pmImg = (ImageView) findViewById(R.id.pm2_5_img);
        temperatureTv = (TextView) findViewById(R.id.temperature);
        climateTv = (TextView) findViewById(R.id.climate);
        windTv = (TextView) findViewById(R.id.wind);
        currentTemperatureTv = (TextView) findViewById(R.id.currentTemperature);
        weatherImg = (ImageView) findViewById(R.id.weather_img);
        mShareBtn = (ImageView) findViewById(R.id.title_share);
        progressBar = (ProgressBar)findViewById(R.id.title_update_progress);

        city_nameTv.setText("N/A");
        cityTv.setText("N/A");
        timeTv.setText("N/A");
        humidityTv.setText("N/A");
        pmDataTv.setText("N/A");
        pmQualityTv.setText("N/A");
        weekTv.setText("N/A");
        temperatureTv.setText("N/A");
        climateTv.setText("N/A");
        windTv.setText("N/A");
        currentTemperatureTv.setText("N/A");

        String initCityCode = "101010100";
        queryWeather(initCityCode);
    }

    // 关闭应用时，关闭"定时更新数据"的服务
    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(new Intent(getBaseContext(), UpdateService.class));
    }

    /**
     * 利用TodayWeather对象更新UI中的控件
     *
     * @param todayWeather
     */
    void updateTodayWeather(TodayWeather todayWeather) {
        city_nameTv.setText(todayWeather.getCity() + "天气");
        cityTv.setText(todayWeather.getCity());
        timeTv.setText(todayWeather.getUpdatetime() + " 发布");
        humidityTv.setText("湿度：" + todayWeather.getShidu());
        if (todayWeather.getPm25() == null) {
            pmDataTv.setText("无");
        } else {
            pmDataTv.setText(todayWeather.getPm25());
        }
        if (todayWeather.getQuality() == null) {
            pmQualityTv.setText("暂无数据");
        } else {
            pmQualityTv.setText(todayWeather.getQuality());
        }
        weekTv.setText(todayWeather.getDate());
        currentTemperatureTv.setText("当前温度:" + todayWeather.getWendu() + "°C");
        String low = todayWeather.getLow();
        low = low.substring(3, low.length() - 1);
        String high = todayWeather.getHigh();
        high = high.substring(3, high.length() - 1);
        temperatureTv.setText(low + "°C~" + high + "°C");
        climateTv.setText(todayWeather.getType());
        windTv.setText("风力：" + todayWeather.getFengli());
        try {
            //更新PM2.5图片
            String pm25 = todayWeather.getPm25();
            double pmData = Double.parseDouble(pm25);
            if (pmData >= 0 && pmData <= 50) {
                pmImg.setImageResource(R.drawable.biz_plugin_weather_0_50);
            } else if (pmData > 50 && pmData <= 100) {
                pmImg.setImageResource(R.drawable.biz_plugin_weather_51_100);
            } else if (pmData > 100 && pmData <= 150) {
                pmImg.setImageResource(R.drawable.biz_plugin_weather_101_150);
            } else if (pmData > 150 && pmData <= 200) {
                pmImg.setImageResource(R.drawable.biz_plugin_weather_151_200);
            } else if (pmData > 200 && pmData <= 300) {
                pmImg.setImageResource(R.drawable.biz_plugin_weather_201_300);
            } else if (pmData > 300) {
                pmImg.setImageResource(R.drawable.biz_plugin_weather_greater_300);
            }
        } catch (Exception e) {
            pmImg.setImageResource(R.drawable.biz_plugin_weather_0_50);
        }

        //更新天气情况图片
        String type = todayWeather.getType();
        if (type.equals("晴")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_qing);
        } else if (type.equals("暴雪")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_baoxue);
        } else if (type.equals("暴雨")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_baoyu);
        } else if (type.equals("雾")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_wu);
        } else if (type.equals("小雪")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_xiaoxue);
        } else if (type.equals("小雨")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_xiaoyu);
        } else if (type.equals("阴")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_yin);
        } else if (type.equals("雨夹雪")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_yujiaxue);
        } else if (type.equals("阵雪")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_zhenxue);
        } else if (type.equals("阵雨")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_zhenxue);
        } else if (type.equals("中雪")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_zhongxue);
        } else if (type.equals("中雨")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_zhongyu);
        } else if (type.equals("多云")) {
            weatherImg.setImageResource(R.drawable.biz_plugin_weather_duoyun);
        }
        Toast.makeText(MainActivity.this, "更新成功！", Toast.LENGTH_SHORT).show();
    }
}
