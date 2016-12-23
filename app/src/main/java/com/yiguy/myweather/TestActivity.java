//package com.yiguy.myweather;
//
//import android.app.Activity;
//import android.support.v7.app.AppCompatActivity;
//import android.os.Bundle;
//import android.view.View;
//import android.widget.Button;
//import android.widget.EditText;
//import android.widget.Toast;
//
//
//
//import org.json.JSONArray;
//import org.json.JSONObject;
//import org.json.JSONTokener;
//
//public class TestActivity extends Activity implements View.OnClickListener {
//
//    private Button btn_click;
//
//    private EditText mResultText;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_test);
//        btn_click = (Button) findViewById(R.id.btn_click);
//        mResultText = ((EditText) findViewById(R.id.result));
//
//        SpeechUtility.createUtility(this, SpeechConstant.APPID + "= 此处需要填写你所申请的appId");
//
//        btn_click.setOnClickListener(this);
//    }
//
//    @Override
//    public void onClick(View v) {
//        btnVoice();
//    }
//
//    //TODO 开始说话：
//    private void btnVoice() {
//        RecognizerDialog dialog = new RecognizerDialog(this,null);
//        dialog.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
//        dialog.setParameter(SpeechConstant.ACCENT, "mandarin");
//
//        dialog.setListener(new RecognizerDialogListener() {
//            @Override
//            public void onResult(RecognizerResult recognizerResult, boolean b) {
//                printResult(recognizerResult);
//            }
//            @Override
//            public void onError(SpeechError speechError) {
//            }
//        });
//        dialog.show();
//        Toast.makeText(this, "请开始说话", Toast.LENGTH_SHORT).show();
//    }
//
//    //回调结果：
//    private void printResult(RecognizerResult results) {
//        String text = parseIatResult(results.getResultString());
//        // 自动填写地址
//        mResultText.append(text);
//    }
//
//    public static String parseIatResult(String json) {
//        StringBuffer ret = new StringBuffer();
//        try {
//            JSONTokener tokener = new JSONTokener(json);
//            JSONObject joResult = new JSONObject(tokener);
//
//            JSONArray words = joResult.getJSONArray("ws");
//            for (int i = 0; i < words.length(); i++) {
//                // 转写结果词，默认使用第一个结果
//                JSONArray items = words.getJSONObject(i).getJSONArray("cw");
//                JSONObject obj = items.getJSONObject(0);
//                ret.append(obj.getString("w"));
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        return ret.toString();
//    }
//}
