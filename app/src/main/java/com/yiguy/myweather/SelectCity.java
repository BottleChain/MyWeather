package com.yiguy.myweather;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
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

    private ImageView mBackBtn;
    // 城市列表
    private List<City> citys = new ArrayList<City>() ;
    // 选中的城市编号
    private String cityCode = "";
    // 日志标识
    private String log_tag = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.select_city);

        mBackBtn = (ImageView)findViewById(R.id.title_back);
        mBackBtn.setOnClickListener(this);

        final ListView lvCity = (ListView)findViewById(R.id.lvCity);

        // 设置日志标识
        MyApplication myApp = (MyApplication) getApplication();
        log_tag = myApp.getLogTag();

        // 设置默认的城市编码
        SharedPreferences sharedPreferences = getSharedPreferences("config", MODE_PRIVATE);
        cityCode = sharedPreferences.getString("main_city_code", "101010100");
        // 获取所有城市的信息
        citys = myApp.getCityList();

        String[] cityName = new String[citys.size()];
        for(int i=0; i<citys.size(); i++){
            cityName[i] = citys.get(i).getProvince()+ "-" + citys.get(i).getCity();
        }
        final ArrayAdapter<String> adapter=new ArrayAdapter<String>(
                SelectCity.this, android.R.layout.simple_list_item_1, cityName);
        lvCity.setAdapter(adapter);

        lvCity.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                cityCode = citys.get(i).getNumber();
                String cityName = citys.get(i).getProvince() +"-" + citys.get(i).getCity();
                Toast.makeText(SelectCity.this, "您已选中:" + cityName,
                        Toast.LENGTH_SHORT).show();
                // 被选中的行移动到最顶端
                //lvCity.setSelection(pos);
                //lvCity.setSelected(true);

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
