package com.yiguy.myweather;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteException;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Toast;

import com.yiguy.app.MyApplication;
import com.yiguy.bean.City;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Xiaoyi on 2016/10/3.
 */
public class SelectCity extends Activity implements View.OnClickListener{
    private static final int FILTER_CITY = 1;
    private ImageView mBackBtn;
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
            default:
                break;
        }
    }
}
