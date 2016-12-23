package com.yiguy.myweather;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class TestActivity extends AppCompatActivity {
    private ListView listView;
    String string[] = { "a", "b", "c", "d", "e", "f", "g", "h" };
    private LayoutInflater inflater;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_activiy);

        listView = (ListView) findViewById(R.id.listViewId);
        ListViewAdapter adapter = new ListViewAdapter();
        listView.setAdapter(adapter);
        listView.setItemsCanFocus(true);// 让ListView的item获得焦点
        listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);// 单选模式
        // 默认第一个item被选中
        listView.setItemChecked(0, true);
    }

    private class ListViewAdapter extends BaseAdapter {

        public ListViewAdapter() {
            super();
            inflater = LayoutInflater.from(TestActivity.this);
        }

        @Override
        public int getCount() {
            return string.length;
        }

        @Override
        public Object getItem(int position) {
            return string[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (view == null) {
                view = inflater.inflate(R.layout.item, parent, false);
                ViewHolder viewHolder = new ViewHolder();
                viewHolder.content = (TextView) view
                        .findViewById(R.id.textViewId);
                view.setTag(viewHolder);
            }
            ViewHolder viewHolder = (ViewHolder) view.getTag();
            viewHolder.content.setText(string[position]);

            return view;
        }

    }

    private class ViewHolder {
        private TextView content;
    }
}