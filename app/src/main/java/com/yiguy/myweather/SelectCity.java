package com.yiguy.myweather;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.yiguy.app.MyApplication;
import com.yiguy.bean.City;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaoyi on 2016/10/3.
 */
public class SelectCity extends Activity implements View.OnClickListener{
    private static final int FILTER_CITY = 1;
    private ImageView mBackBtn;
    private ImageView imvRecord;
    private EditText search_edit;

    // 城市列表
    private List<City> citys = new ArrayList<City>() ;
    // 选中的城市编号
    private String cityCode = "";
    // 日志标识
    private String log_tag = "";

    // ListView适配器
    private ArrayAdapter<String> adapter;
    private List<City> cityTemp;

    // ListView
    private ListView lvCity;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case FILTER_CITY:
                    filterCitys((String) msg.obj);
                    break;
                default:
                    break;
            }
        }
    };

    private TextWatcher mTextWatcher = new TextWatcher() {
        private CharSequence temp;
        private int editStart;
        private int editEnd;
        
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            temp = s;
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {

        }

        @Override
        public void afterTextChanged(Editable s) {
            editStart = search_edit.getSelectionStart();
            editEnd = search_edit.getSelectionEnd();
            if(temp.length() > 10){
                Toast.makeText(SelectCity.this, "您输入的字数已经超过了限制！", Toast.LENGTH_SHORT).show();
                s.delete(editStart-1, editEnd);
                int tempSelection = editStart;
                search_edit.setText(s);
                search_edit.setSelection(tempSelection);
            }

            // 发送消息给UI线程
            Message msg = new Message();
            msg.what = FILTER_CITY;
            msg.obj = s.toString();
            mHandler.sendMessage(msg);
        }
    };


    protected void filterCitys(String filterStr){
        cityTemp.clear();
        // 过滤符合条件的城市列表
        for(City c: citys){
            // 简单过滤原则：名字匹配
            if( (c.getCity().contains(filterStr))){
                cityTemp.add(c);
            }
        }
        String[] names = new String[cityTemp.size()];
        for(int i=0; i<cityTemp.size(); i++){
            names[i] = cityTemp.get(i).getProvince()+ "-" + cityTemp.get(i).getCity();
        }
        ArrayAdapter adapterTemp = new ArrayAdapter<String>(
                SelectCity.this, android.R.layout.simple_list_item_1, names);
        lvCity.setAdapter(adapterTemp);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_city);

        search_edit = (EditText) findViewById(R.id.search_edit);
        search_edit.addTextChangedListener(mTextWatcher);
        mBackBtn = (ImageView)findViewById(R.id.title_back);
        mBackBtn.setOnClickListener(this);
        imvRecord= (ImageView) findViewById(R.id.imvRecord);
        imvRecord.setOnClickListener(this);

        SpeechUtility.createUtility(this, SpeechConstant.APPID + "=585c9bf4");

        lvCity = (ListView)findViewById(R.id.lvCity);

        // 设置日志标识
        MyApplication myApp = (MyApplication) getApplication();
        log_tag = myApp.getLogTag();

        // 设置默认的城市编码
        SharedPreferences sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
        cityCode = sharedPreferences.getString("main_city_code", "101010100");
        // 获取所有城市的信息
        citys = myApp.getCityList();
        cityTemp = new ArrayList<City>(citys);

        String[] cityName = new String[citys.size()];
        for(int i=0; i<citys.size(); i++){
            cityName[i] = citys.get(i).getProvince()+ "-" + citys.get(i).getCity();
        }
        adapter=new ArrayAdapter<String>(
                SelectCity.this, android.R.layout.simple_list_item_1, cityName);
        lvCity.setAdapter(adapter);

        lvCity.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                cityCode = cityTemp.get(i).getNumber();
                String cityName = cityTemp.get(i).getProvince() +"-" + cityTemp.get(i).getCity();
                Toast.makeText(SelectCity.this, "您已选中:" + cityName,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.title_back:
                Intent i = new Intent();
                i.putExtra("cityCode",cityCode);
                setResult(RESULT_OK, i);
                finish();
                break;
            case R.id.imvRecord:
                btnVoice();
                break;
            default:
                break;
        }
    }

    private void btnVoice() {
        RecognizerDialog dialog = new RecognizerDialog(this,null);
        dialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
        dialog.setParameter(SpeechConstant.ACCENT, "mandarin");

        dialog.setListener(new RecognizerDialogListener() {
            @Override
            public void onResult(RecognizerResult recognizerResult, boolean b) {
                printResult(recognizerResult);
            }
            @Override
            public void onError(SpeechError speechError) {
            }
        });
        dialog.show();
        Toast.makeText(this, "请开始说话", Toast.LENGTH_SHORT).show();
    }

    //回调结果：
    private void printResult(RecognizerResult results) {
        String text = parseIatResult(results.getResultString());
        if(!text.equals("。") && !text.equals(".") && !text.equals("！")){
            // 自动填写城市
            search_edit.setText(text);
        }
        //Toast.makeText(SelectCity.this, text, Toast.LENGTH_SHORT).show();
    }

    public static String parseIatResult(String json) {
        StringBuffer ret = new StringBuffer();
        try {
            JSONTokener tokener = new JSONTokener(json);
            JSONObject joResult = new JSONObject(tokener);

            JSONArray words = joResult.getJSONArray("ws");
            for (int i = 0; i < words.length(); i++) {
                // 转写结果词，默认使用第一个结果
                JSONArray items = words.getJSONObject(i).getJSONArray("cw");
                JSONObject obj = items.getJSONObject(0);
                ret.append(obj.getString("w"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret.toString();
    }
}
