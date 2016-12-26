package com.yiguy.myweather;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.baidu.location.LocationClient;
import com.yiguy.app.MyApplication;
import com.yiguy.bean.TodayWeather;
import com.yiguy.db.CityDB;
import com.yiguy.service.UpdateService;
import com.yiguy.util.AndroidShare;
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClientOption;


/**
 * Created by Xiaoyi on 2016/9/20.
 */
public class MainActivity extends Activity implements View.OnClickListener {

    private static final int UPDATE_TODAY_WEATHER = 1;
    private static final int UPDATE_WEATHER_FINISH = 2;
    private static final int UPDATE_FUTURE_WEATHER = 3;
    private static final int LOCATE_LOCATION = 4;

    private String log_tag = "";

    private Calendar c = null;

    // 分享按钮
    //更新按钮
    private ImageView mUpdateBtn;
    private ImageView mShareBtn;
    private ImageView mLocationBtn;
    //选择城市按钮
    private ImageView mCitySelect;

    private IntentFilter intentFilter;

    // 通知栏
    private static final int NOTIFICATION_ID = 0x123;
    private NotificationManager nm;


    View future_three;
    View future_six;

    //对应的viewPager
    private ViewPager viewPager;
    //view数组
    private List<View> viewList;

    private ViewPager vp_guide;
    private List<ImageView> mImgList;//导航图集合
    private LinearLayout ll_container;//小圆点容器
    private int mCurrentIndex = 0;//当前小圆点的位置
    private int[] imgArray = {R.drawable.page, R.drawable.page};

    public LocationClient mLocationClient = null;
    public BDLocationListener myListener = null;

    private BroadcastReceiver intentReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            TodayWeather newWeather = (TodayWeather) intent.getSerializableExtra("weather1");
            updateTodayWeather(newWeather);

            List<TodayWeather> weatherList = new ArrayList<TodayWeather>();
            weatherList.add((TodayWeather) intent.getSerializableExtra("weather0"));
            weatherList.add((TodayWeather) intent.getSerializableExtra("weather1"));
            weatherList.add((TodayWeather) intent.getSerializableExtra("weather2"));
            weatherList.add((TodayWeather) intent.getSerializableExtra("weather3"));
            weatherList.add((TodayWeather) intent.getSerializableExtra("weather4"));
            weatherList.add((TodayWeather) intent.getSerializableExtra("weather5"));
            updateFutureWeather(weatherList);

            // Toast.makeText(MainActivity.this, "天气信息更新成功！" , Toast.LENGTH_SHORT).show();
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Date curDate = new Date(System.currentTimeMillis());    //获取当前时间
            String time = formatter.format(curDate);
            Log.i(log_tag, "天气数据更新了，更新时间为:" + time);
        }
    };

    private TextView cityTv, timeTv, humidityTv, weekTv, pmDataTv, pmQualityTv, temperatureTv, climateTv, windTv, city_nameTv, currentTemperatureTv;
    private ImageView weatherImg, pmImg;
    private ProgressBar progressBar;
    private TextView txtFutWeek1, txtFutWeek2, txtFutWeek3, txtFutWeek4, txtFutWeek5, txtFutWeek6;
    private ImageView imgFutWeather1, imgFutWeather2, imgFutWeather3, imgFutWeather4, imgFutWeather5, imgFutWeather6;
    private TextView txtFutTem1, txtFutTem2, txtFutTem3, txtFutTem4, txtFutTem5, txtFutTem6;
    private TextView txtFutWeather1, txtFutWeather2, txtFutWeather3, txtFutWeather4, txtFutWeather5, txtFutWeather6;
    private TextView txtFutFeng1, txtFutFeng2, txtFutFeng3, txtFutFeng4, txtFutFeng5, txtFutFeng6;

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
                case UPDATE_FUTURE_WEATHER:
                    updateFutureWeather((List<TodayWeather>) msg.obj);
                    break;
                case LOCATE_LOCATION:
                    getLocation((Bundle) msg.obj);
                    break;
                default:
                    break;
            }
        }
    };


    private void getLocation(Bundle data) {
        try {
            String province = (String) data.get("province");
            province = province.substring(0, province.length() - 1);
            String city = (String) data.get("city");
            city = city.substring(0, city.length() - 1);
            String district = (String) data.get("district");
            district = district.substring(0, district.length() - 1);

            if (province != null && city != null && district != null) {
                // 查询数据库，找到对应城市的代码
                Log.i("MyWeather", province + "   " + city + "  " + district);
                MyApplication myApp = (MyApplication) getApplication();
                CityDB cityDB = myApp.getmCityDB();
                String number = cityDB.getCityCode(province, city, district);
                if (number != null) {
                    SharedPreferences sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putString("main_city_code", number);
                    editor.commit();

                    String cityCode = sharedPreferences.getString("main_city_code", "101010100");
                    queryWeather(cityCode);
                    Toast.makeText(MainActivity.this, "当前定位城市：" + city + "-" + district + "。天气信息更新成功！", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            SharedPreferences sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
            String cityCode = sharedPreferences.getString("main_city_code", "101010100");
            queryWeather(cityCode);
            Toast.makeText(MainActivity.this, "为获得更好的体验，请您允许APP获取位置！", Toast.LENGTH_LONG).show();
        }
    }

    // 设置定时时间
    public void setTime(int hourParam, int minParam) {
        c.setTimeInMillis(System.currentTimeMillis());
        c.setTimeInMillis(System.currentTimeMillis());
        // 这里时区需要设置一下，不然会有8个小时的时间差
        c.setTimeZone(TimeZone.getTimeZone("GMT+8"));
        //设置小时分钟，秒和毫秒都设置为0
        c.set(Calendar.HOUR_OF_DAY, hourParam);
        c.set(Calendar.MINUTE, minParam);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        Intent intent = new Intent(MainActivity.this, AlarmReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(MainActivity.this, 0, intent, 0);
        //得到AlarmManager实例
        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        //根据当前时间预设一个警报
        // am.set(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pi);

        // 计算延迟时间
        long dayLong = 24 * 60 * 60 * 1000;
        long delay = c.getTimeInMillis() - System.currentTimeMillis();
        if (delay < 0) {
            delay = dayLong + c.getTimeInMillis();
        } else {
            delay = c.getTimeInMillis();
        }
        am.setRepeating(AlarmManager.RTC_WAKEUP, delay, dayLong, pi);
    }

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

        mShareBtn = (ImageView) findViewById(R.id.title_share);
        mShareBtn.setOnClickListener(this);


        ll_container = (LinearLayout) findViewById(R.id.ll_container);
        mImgList = new ArrayList<>();
        for (int i = 0; i < imgArray.length; i++) {
            //获取4个圆点
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(imgArray[i]);
            mImgList.add(imageView);
            ImageView dot = new ImageView(this);
            if (i == mCurrentIndex) {
                dot.setImageResource(R.drawable.page_now); //设置当前页的圆点
            } else {
                dot.setImageResource(R.drawable.page); //其余页的圆点
            }
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout
                    .LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            if (i > 0) {
                params.leftMargin = 10;//设置圆点边距
            }
            dot.setLayoutParams(params);
            ll_container.addView(dot);//将圆点添加到容器中
        }


        // 滑动页面
        viewPager = (ViewPager) findViewById(R.id.vpFutureWeather);
        LayoutInflater inflater = getLayoutInflater();
        future_three = inflater.inflate(R.layout.future_three, null);
        future_six = inflater.inflate(R.layout.future_six, null);

        try {
            // 自动定位
            mLocationClient = new LocationClient(getApplicationContext());     //声明LocationClient类
            myListener = new MyLocationListener(mLocationClient);
            mLocationClient.registerLocationListener(myListener);    //注册监听函数
            initLocation();
            mLocationClient.start();
        } catch (Exception e) {
            Log.e(log_tag, e.toString());
        }

        initView();

        mLocationBtn.setOnClickListener(this);
        // 将要分页显示的View装入数组中
        viewList = new ArrayList<View>();
        viewList.add(future_three);
        viewList.add(future_six);

        PagerAdapter pagerAdapter = new PagerAdapter() {
            @Override
            public int getCount() {
                return viewList.size();
            }

            @Override
            public boolean isViewFromObject(View view, Object object) {
                return view == object;
            }

            @Override
            public Object instantiateItem(ViewGroup container, int position) {
                container.addView(viewList.get(position));
                return viewList.get(position);
            }

            @Override
            public void destroyItem(ViewGroup container, int position, Object object) {
                container.removeView(viewList.get(position));
            }
        };
        viewPager.setAdapter(pagerAdapter);


        //给viewpager添加监听
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int
                    positionOffsetPixels) {

            }

            @Override
            public void onPageSelected(int position) {
                //根据监听的页面改变当前页对应的小圆点
                mCurrentIndex = position;
                for (int i = 0; i < ll_container.getChildCount(); i++) {
                    ImageView imageView = (ImageView) ll_container.getChildAt(i);
                    if (i == position) {
                        imageView.setImageResource(R.drawable.page_now);
                    } else {
                        imageView.setImageResource(R.drawable.page);
                    }
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });

        try {
            // 在应用启动时，启动"定时更新数据"的service服务
            startService(new Intent(getBaseContext(), UpdateService.class));
        } catch (Exception e) {
            Log.e(log_tag, e.toString());
        }

        //得到日历实例，主要是为了下面的获取时间
        c = Calendar.getInstance();

        // 设置定时提醒的时间
        setTime(7, 0);
    }

    // 初始化百度地图
    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy
        );//可选，默认高精度，设置定位模式，高精度，低功耗，仅设备
        option.setCoorType("bd09ll");//可选，默认gcj02，设置返回的定位结果坐标系
        int span = 1000;
        option.setScanSpan(span);//可选，默认0，即仅定位一次，设置发起定位请求的间隔需要大于等于1000ms才是有效的
        option.setIsNeedAddress(true);//可选，设置是否需要地址信息，默认不需要
        option.setOpenGps(true);//可选，默认false,设置是否使用gps
        option.setLocationNotify(true);//可选，默认false，设置是否当GPS有效时按照1S/1次频率输出GPS结果
        option.setIsNeedLocationDescribe(true);//可选，默认false，设置是否需要位置语义化结果，可以在BDLocation.getLocationDescribe里得到，结果类似于“在北京天安门附近”
        option.setIsNeedLocationPoiList(true);//可选，默认false，设置是否需要POI结果，可以在BDLocation.getPoiList里得到
        option.setIgnoreKillProcess(false);//可选，默认true，定位SDK内部是一个SERVICE，并放到了独立进程，设置是否在stop的时候杀死这个进程，默认不杀死
        option.SetIgnoreCacheException(false);//可选，默认false，设置是否收集CRASH信息，默认收集
        option.setEnableSimulateGps(false);//可选，默认false，设置是否需要过滤GPS仿真结果，默认需要
        mLocationClient.setLocOption(option);
    }

    // 当天气数据更新之后，更新按钮停止旋转
    public void updateFinish() {
        mUpdateBtn.setVisibility(View.VISIBLE);
        RelativeLayout.LayoutParams param = (RelativeLayout.LayoutParams) mShareBtn.getLayoutParams();
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
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) mShareBtn.getLayoutParams();
                params.addRule(RelativeLayout.LEFT_OF, R.id.title_update_progress);
                mShareBtn.setLayoutParams(params);
                // 查询天气状况
                queryWeather(cityCode);
                Toast.makeText(MainActivity.this, "天气信息更新成功！", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(log_tag, "网络挂了");
                Toast.makeText(MainActivity.this, "网络挂了！", Toast.LENGTH_LONG).show();
            }
        }
        // 点击定位按钮
        if (view.getId() == R.id.title_location) {
            mLocationClient.start();
        }

        if (view.getId() == R.id.title_share) {
            mShareBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    AndroidShare as = new AndroidShare(MainActivity.this, "来自Yiguy天气的分享",
                            "http://hoop8.com/1612D/YsH6Z8Sr.jpg");
                    as.show();
                }
            });
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

    private class MyLocationListener implements BDLocationListener {

        public LocationClient mLocationClient = null;

        MyLocationListener(LocationClient client) {
            mLocationClient = client;
        }

        @Override
        public void onReceiveLocation(BDLocation location) {
            try {
                String province = location.getProvince();
                String city = location.getCity();
                String district = location.getDistrict();

                Message msg = new Message();
                msg.what = LOCATE_LOCATION;
                Bundle bundle = new Bundle();
                bundle.putString("province", province);
                bundle.putString("city", city);
                bundle.putString("district", district);

                msg.obj = bundle;
                mHandler.sendMessage(msg);
                mLocationClient.stop();
            } catch (Exception e) {

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

                    // 解析今天以及未来几天的天气情况
                    List<TodayWeather> weatherList = parseXML(responseStr);
                    if (weatherList != null) {
                        todayWeather = weatherList.get(1);
                    }
                    if (todayWeather != null) {
                        Message msg = new Message();
                        msg.what = UPDATE_TODAY_WEATHER;
                        msg.obj = todayWeather;
                        mHandler.sendMessage(msg);
                    }

                    if (weatherList != null) {
                        Message msg = new Message();
                        msg.what = UPDATE_FUTURE_WEATHER;
                        msg.obj = weatherList;
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
     * 解析XML，获取当前城市昨天-今天-以及未来的天气信息
     *
     * @param xmldata
     */
    private List<TodayWeather> parseXML(String xmldata) {
        List<TodayWeather> weatherList = null;
        TodayWeather weather = null;
        String city = "";
        String updateTime = "";
        String wendu = "";
        String shidu = "";
        String fengli = "";
        String fengxiang = "";
        String pm25 = "";
        String quality = "";
        int fengxiangCount = 0;
        int fengliCount = 0;
        int type1Count = 0;
        int fx1Count = 0;
        int fl1Count = 0;
        boolean isTodayWeather = true;
        try {
            XmlPullParserFactory fac = XmlPullParserFactory.newInstance();
            XmlPullParser xmlPullParser = fac.newPullParser();
            xmlPullParser.setInput(new StringReader(xmldata));
            int eventType = xmlPullParser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    //判断当前事件是否为文档开始事件
                    case XmlPullParser.START_DOCUMENT:
                        break;
                    //判断当前事件是否为标签元素开始事件
                    case XmlPullParser.START_TAG:
                        if (xmlPullParser.getName().equals("resp")) {
                            weatherList = new ArrayList<TodayWeather>();
                        }
                        if (xmlPullParser.getName().equals("weather") || xmlPullParser.getName().equals("yesterday")) {
                            weather = new TodayWeather();
                        }
                        if (xmlPullParser.getName().equals("city")) {
                            eventType = xmlPullParser.next();
                            city = xmlPullParser.getText();
                        } else if (xmlPullParser.getName().equals("updatetime")) {
                            eventType = xmlPullParser.next();
                            updateTime = xmlPullParser.getText();
                        } else if (xmlPullParser.getName().equals("shidu")) {
                            eventType = xmlPullParser.next();
                            shidu = xmlPullParser.getText();
                        } else if (xmlPullParser.getName().equals("wendu")) {
                            eventType = xmlPullParser.next();
                            wendu = xmlPullParser.getText();
                        } else if (xmlPullParser.getName().equals("pm25")) {
                            eventType = xmlPullParser.next();
                            pm25 = xmlPullParser.getText();
                        } else if (xmlPullParser.getName().equals("quality")) {
                            eventType = xmlPullParser.next();
                            quality = xmlPullParser.getText();
                        } else if (xmlPullParser.getName().equals("fengxiang") && fengxiangCount == 0) {
                            eventType = xmlPullParser.next();
                            fengxiang = xmlPullParser.getText();
                            fengxiangCount = 1;
                        } else if (xmlPullParser.getName().equals("fengli") && fengliCount == 0) {
                            eventType = xmlPullParser.next();
                            fengli = xmlPullParser.getText();
                            fengliCount = 1;
                        }
                        if (weather != null) {
                            if (xmlPullParser.getName().equals("date")) {
                                if (isTodayWeather) {
                                    weather.setCity(city);
                                    weather.setWendu(wendu);
                                    weather.setShidu(shidu);
                                    weather.setPm25(pm25);
                                    weather.setFengxiang(fengxiang);
                                    weather.setFengli(fengli);
                                    weather.setUpdatetime(updateTime);
                                    weather.setQuality(quality);
                                    isTodayWeather = false;
                                }
                                eventType = xmlPullParser.next();
                                weather.setDate(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("high")) {
                                eventType = xmlPullParser.next();
                                weather.setHigh(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("low")) {
                                eventType = xmlPullParser.next();
                                weather.setLow(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("type") && weather.getType() == null) {
                                eventType = xmlPullParser.next();
                                weather.setType(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("date_1")) {
                                eventType = xmlPullParser.next();
                                weather.setDate(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("high_1")) {
                                eventType = xmlPullParser.next();
                                weather.setHigh(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("low_1")) {
                                eventType = xmlPullParser.next();
                                weather.setLow(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("type_1") && type1Count == 0) {
                                eventType = xmlPullParser.next();
                                weather.setType(xmlPullParser.getText());
                                type1Count = 1;
                            } else if (xmlPullParser.getName().equals("fx_1") && fx1Count == 0) {
                                eventType = xmlPullParser.next();
                                weather.setFengxiang(xmlPullParser.getText());
                                fx1Count = 1;
                            } else if (xmlPullParser.getName().equals("fl_1") && fl1Count == 0) {
                                eventType = xmlPullParser.next();
                                weather.setFengli(xmlPullParser.getText());
                                fl1Count = 1;
                            } else if (xmlPullParser.getName().equals("fengxiang") && weather.getFengxiang() == null) {
                                eventType = xmlPullParser.next();
                                weather.setFengxiang(xmlPullParser.getText());
                            } else if (xmlPullParser.getName().equals("fengli") && weather.getFengli() == null) {
                                eventType = xmlPullParser.next();
                                weather.setFengli(xmlPullParser.getText());
                            }
                        }
                        break;
                    //判断当前事件是否为标签元素结束事件
                    case XmlPullParser.END_TAG:
                        if (xmlPullParser.getName().equals("weather") || xmlPullParser.getName().equals("yesterday")) {
                            weatherList.add(weather);
                        }
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
        return weatherList;
    }

    /**
     * 查找各个View
     */
    private void findViews() {
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
        mLocationBtn = (ImageView) findViewById(R.id.title_location);
        progressBar = (ProgressBar) findViewById(R.id.title_update_progress);

        txtFutWeek1 = (TextView) future_three.findViewById(R.id.txtWeek1);
        txtFutWeek2 = (TextView) future_three.findViewById(R.id.txtWeek2);
        txtFutWeek3 = (TextView) future_three.findViewById(R.id.txtWeek3);
        txtFutWeek4 = (TextView) future_six.findViewById(R.id.txtWeek4);
        txtFutWeek5 = (TextView) future_six.findViewById(R.id.txtWeek5);
        txtFutWeek6 = (TextView) future_six.findViewById(R.id.txtWeek6);

        imgFutWeather1 = (ImageView) future_three.findViewById(R.id.imgWeather1);
        imgFutWeather2 = (ImageView) future_three.findViewById(R.id.imgWeather2);
        imgFutWeather3 = (ImageView) future_three.findViewById(R.id.imgWeather3);
        imgFutWeather4 = (ImageView) future_six.findViewById(R.id.imgWeather4);
        imgFutWeather5 = (ImageView) future_six.findViewById(R.id.imgWeather5);
        imgFutWeather6 = (ImageView) future_six.findViewById(R.id.imgWeather6);

        txtFutTem1 = (TextView) future_three.findViewById(R.id.txtTemperature1);
        txtFutTem2 = (TextView) future_three.findViewById(R.id.txtTemperature2);
        txtFutTem3 = (TextView) future_three.findViewById(R.id.txtTemperature3);
        txtFutTem4 = (TextView) future_six.findViewById(R.id.txtTemperature4);
        txtFutTem5 = (TextView) future_six.findViewById(R.id.txtTemperature5);
        txtFutTem6 = (TextView) future_six.findViewById(R.id.txtTemperature6);

        txtFutWeather1 = (TextView) future_three.findViewById(R.id.txtWeather1);
        txtFutWeather2 = (TextView) future_three.findViewById(R.id.txtWeather2);
        txtFutWeather3 = (TextView) future_three.findViewById(R.id.txtWeather3);
        txtFutWeather4 = (TextView) future_six.findViewById(R.id.txtWeather4);
        txtFutWeather5 = (TextView) future_six.findViewById(R.id.txtWeather5);
        txtFutWeather6 = (TextView) future_six.findViewById(R.id.txtWeather6);

        txtFutFeng1 = (TextView) future_three.findViewById(R.id.txtFeng1);
        txtFutFeng2 = (TextView) future_three.findViewById(R.id.txtFeng2);
        txtFutFeng3 = (TextView) future_three.findViewById(R.id.txtFeng3);
        txtFutFeng4 = (TextView) future_six.findViewById(R.id.txtFeng4);
        txtFutFeng5 = (TextView) future_six.findViewById(R.id.txtFeng5);
        txtFutFeng6 = (TextView) future_six.findViewById(R.id.txtFeng6);
    }

    void initView() {
        findViews();
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
        if (todayWeather != null) {
            city_nameTv.setText(todayWeather.getCity() + "天气");
            cityTv.setText(todayWeather.getCity());
            timeTv.setText(todayWeather.getUpdatetime() + " 发布");
            humidityTv.setText("湿度：" + todayWeather.getShidu());
            if (todayWeather.getPm25() == null || todayWeather.getPm25().equals("")) {
                pmDataTv.setText("无");
            } else {
                pmDataTv.setText(todayWeather.getPm25());
            }
            if (todayWeather.getQuality() == null || todayWeather.getQuality().equals("")) {
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
            weatherImg.setImageResource(getRightImg(type));
        }
    }

    /**
     * 利用TodayWeather对象更新UI中的控件--最下方viewpager部分
     *
     * @param weatherList
     */
    void updateFutureWeather(List<TodayWeather> weatherList) {
        if (weatherList != null && weatherList.size() == 6) {
            int count = weatherList.size();
            TodayWeather weather1 = weatherList.get(0);
            String week = weather1.getDate().substring(3);
            txtFutWeek1.setText(week);
            String low = weather1.getLow();
            low = low.substring(3, low.length() - 1);
            String high = weather1.getHigh();
            high = high.substring(3, high.length() - 1);
            txtFutTem1.setText(low + "~" + high + "°C");
            txtFutWeather1.setText(weather1.getType());
            imgFutWeather1.setImageResource(getRightImg(weather1.getType()));
            txtFutFeng1.setText(weather1.getFengli());

            TodayWeather weather2 = weatherList.get(1);
            String week2 = weather2.getDate().substring(3);
            txtFutWeek2.setText(week2);
            String low2 = weather2.getLow();
            low2 = low2.substring(3, low2.length() - 1);
            String high2 = weather2.getHigh();
            high2 = high2.substring(3, high2.length() - 1);
            txtFutTem2.setText(low2 + "~" + high2 + "°C");
            txtFutWeather2.setText(weather2.getType());
            imgFutWeather2.setImageResource(getRightImg(weather2.getType()));
            txtFutFeng2.setText(weather2.getFengli());


            TodayWeather weather3 = weatherList.get(2);
            String week3 = weather3.getDate().substring(3);
            txtFutWeek3.setText(week3);
            String low3 = weather3.getLow();
            low3 = low3.substring(3, low3.length() - 1);
            String high3 = weather3.getHigh();
            high3 = high3.substring(3, high3.length() - 1);
            txtFutTem3.setText(low3 + "~" + high3 + "°C");
            txtFutWeather3.setText(weather3.getType());
            txtFutFeng3.setText(weather3.getFengli());
            imgFutWeather3.setImageResource(getRightImg(weather3.getType()));


            TodayWeather weather4 = weatherList.get(3);
            String week4 = weather4.getDate().substring(3);
            txtFutWeek4.setText(week4);
            String low4 = weather4.getLow();
            low4 = low4.substring(3, low4.length() - 1);
            String high4 = weather4.getHigh();
            high4 = high4.substring(3, high4.length() - 1);
            txtFutTem4.setText(low4 + "~" + high4 + "°C");
            txtFutWeather4.setText(weather4.getType());
            txtFutFeng4.setText(weather4.getFengli());
            imgFutWeather4.setImageResource(getRightImg(weather4.getType()));

            TodayWeather weather5 = weatherList.get(4);
            String week5 = weather5.getDate().substring(3);
            txtFutWeek5.setText(week5);
            String low5 = weather5.getLow();
            low5 = low5.substring(3, low5.length() - 1);
            String high5 = weather5.getHigh();
            high5 = high5.substring(3, high5.length() - 1);
            txtFutTem5.setText(low5 + "~" + high5 + "°C");
            txtFutWeather5.setText(weather5.getType());
            txtFutFeng5.setText(weather5.getFengli());
            imgFutWeather5.setImageResource(getRightImg(weather5.getType()));


            TodayWeather weather6 = weatherList.get(5);
            String week6 = weather6.getDate().substring(3);
            txtFutWeek6.setText(week6);
            String low6 = weather6.getLow();
            low6 = low6.substring(3, low6.length() - 1);
            String high6 = weather6.getHigh();
            high6 = high6.substring(3, high6.length() - 1);
            txtFutTem6.setText(low6 + "~" + high6 + "°C");
            txtFutWeather6.setText(weather6.getType());
            txtFutFeng6.setText(weather6.getFengli());
            imgFutWeather6.setImageResource(getRightImg(weather6.getType()));
        }
    }


    private int getRightImg(String type) {
        if (type.equals("晴")) {
            return R.drawable.biz_plugin_weather_qing;
        } else if (type.equals("暴雪")) {
            return R.drawable.biz_plugin_weather_baoxue;
        } else if (type.equals("暴雨")) {
            return R.drawable.biz_plugin_weather_baoyu;
        } else if (type.equals("雾")) {
            return R.drawable.biz_plugin_weather_wu;
        } else if (type.equals("小雪")) {
            return R.drawable.biz_plugin_weather_xiaoxue;
        } else if (type.equals("小雨")) {
            return R.drawable.biz_plugin_weather_xiaoyu;
        } else if (type.equals("阴")) {
            return R.drawable.biz_plugin_weather_yin;
        } else if (type.equals("雨夹雪")) {
            return R.drawable.biz_plugin_weather_yujiaxue;
        } else if (type.equals("阵雪")) {
            return R.drawable.biz_plugin_weather_zhenxue;
        } else if (type.equals("阵雨")) {
            return R.drawable.biz_plugin_weather_zhenxue;
        } else if (type.equals("中雪")) {
            return R.drawable.biz_plugin_weather_zhongxue;
        } else if (type.equals("中雨")) {
            return R.drawable.biz_plugin_weather_zhongyu;
        } else if (type.equals("多云")) {
            return R.drawable.biz_plugin_weather_duoyun;
        } else {
            return R.drawable.biz_plugin_weather_qing;
        }
    }
}
