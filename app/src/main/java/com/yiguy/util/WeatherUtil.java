package com.yiguy.util;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.yiguy.bean.TodayWeather;

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
import java.util.ArrayList;
import java.util.List;

/**
 * Created by hadoop on 2016/12/23.
 */
public class WeatherUtil {

    private static final int QUERY_WEATHER = 10010;

    public static void queryWeather(String cityCode, final Handler handler) {
        final String address = "http://wthrcdn.etouch.cn/WeatherApi?citykey=" + cityCode;
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection con = null;
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
                    // 查询完毕，发送消息
                    Message msg = new Message();
                    msg.what = QUERY_WEATHER;
                    msg.obj = responseStr;
                    handler.sendMessage(msg);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                }
            }
        }).start();
    }

    // 同步查询天气状况
    public static List<TodayWeather> syncQueryWeather(String cityCode) {
        String address = "http://wthrcdn.etouch.cn/WeatherApi?citykey=" + cityCode;
        HttpURLConnection con = null;
        List<TodayWeather> weatherList = null;
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
            weatherList = parseXML(responseStr);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return weatherList;
    }

    /**
     * 解析XML，获取当前城市昨天-今天-以及未来的天气信息
     * @param xmldata
     */
    public static List<TodayWeather> parseXML(String xmldata) {
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

}
