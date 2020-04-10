package com.example.translator;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.translator.util.AudioRecorder;
import com.example.translator.util.AudioTrackManager;
import com.example.translator.util.WebIATHTTP;
import com.example.translator.util.WebITSHTTP;
import com.example.translator.util.WebTTSWS;

import java.util.HashMap;

public class ThirdActivity extends AppCompatActivity {
    private static final String TAG = "ThirdActivity";
    private static final int LEFT_RECORDER = 1;
    private static final int RIGHT_RECORDER = 2;
    private ImageView imageView;
    private Drawable drawable;
    private Button btnSelf;
    private Button btnOther;
    private Spinner spinnerSelf;
    private Spinner spinnerOther;
    private TextView textViewSelf;
    private TextView textViewOther;
    private boolean recording = false;
    private int curSelfPositon = 0; // 中文
    private int curOtherPositon = 1; // 英文
    private int curRecorder = LEFT_RECORDER;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_third);
        initView();
    }
    private void initView() {
        // spinnerSelf是activity_third显示元素中自己的翻译类型的选择，默认中文
        spinnerSelf = findViewById(R.id.spinner_left);
        spinnerSelf.setSelection(curSelfPositon);
        spinnerSelf.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                curSelfPositon = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        // spinnerSelf是activity_third显示元素中他人的翻译类型的选择，默认英文
        spinnerOther = findViewById(R.id.spinner_right);
        spinnerOther.setSelection(curOtherPositon);
        spinnerOther.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                curOtherPositon = position;
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // imageView是中间说话时的录音动画，drawable用于显示音量大小的动画
        imageView = findViewById(R.id.img);
        drawable = imageView.getDrawable();
        imageView.setVisibility(View.INVISIBLE);

        // 按钮用于长按时录音，抬起时翻译
        btnSelf = findViewById(R.id.btn_left);
        btnSelf.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (ContextCompat.checkSelfPermission(ThirdActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(ThirdActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                            return false;
                        }
                        curRecorder = LEFT_RECORDER;
                        startRecorder();
                        break;
                    case MotionEvent.ACTION_UP:
                        stopRecorder();
                        break;
                }
                return true;
            }
        });
        btnOther = findViewById(R.id.btn_right);
        btnOther.setOnTouchListener(new View.OnTouchListener() {
            public boolean onTouch(View v, MotionEvent event) {
                switch(event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        if (ContextCompat.checkSelfPermission(ThirdActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(ThirdActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                            return false;
                        }
                        curRecorder = RIGHT_RECORDER;
                        startRecorder();
                        break;
                    case MotionEvent.ACTION_UP:
                        stopRecorder();
                        break;
                }
                return true;
            }
        });

        // 显示翻译文本
        textViewSelf = findViewById(R.id.textView);
        textViewOther = findViewById(R.id.textView2);
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 1){
            if (grantResults.length <= 0 || grantResults[0] == PackageManager.PERMISSION_DENIED){
                Toast.makeText(this, "申请权限", Toast.LENGTH_SHORT).show();
                if (!ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO))
                    Toast.makeText(this, "录音权限被拒绝", Toast.LENGTH_SHORT).show();
            }
            else if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "权限已经打开", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void startRecorder(){
        Toast.makeText(ThirdActivity.this, "正在录音", Toast.LENGTH_SHORT).show();
        if (curRecorder == LEFT_RECORDER) {
            btnSelf.setText("停止录音");
        } else if (curRecorder == RIGHT_RECORDER) {
            btnOther.setText("停止录音");
        }
        AudioRecorder.getInstance().start(handler);
        imageView.setVisibility(View.VISIBLE);
        recording = !recording;
    }
    private void stopRecorder(){
        Toast.makeText(ThirdActivity.this, "已停止录音", Toast.LENGTH_SHORT).show();
        if (curRecorder == LEFT_RECORDER) {
            btnSelf.setText("开始录音");
        } else if (curRecorder == RIGHT_RECORDER) {
            btnOther.setText("开始录音");
        }
        AudioRecorder.getInstance().stop();
        imageView.setVisibility(View.INVISIBLE);
        recording = !recording;
    }

    WebTTSWS.IResponseResult responseResult = new WebTTSWS.IResponseResult() {
        @Override
        public void setAudio(byte[] audio) {
            AudioTrackManager.getInstance().startPlay(audio);
        }
    };

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case AudioRecorder.VOICE_VOLUME:
                    drawable.setLevel(msg.arg1);
                    break;
                case AudioRecorder.RECORD_STOP:
                    drawable.setLevel(0);
                    Bundle data = msg.getData();
                    final byte[] audio = data.getByteArray("audio");
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                String text = WebIATHTTP.AudioToText(audio);
                                String src = getResources().getStringArray(R.array.translater_language_id)[curSelfPositon];
                                String dst = getResources().getStringArray(R.array.translater_language_id)[curOtherPositon];
                                String dstText = "";
                                String vcn = "";
                                WebITSHTTP.TransResult transResult = new WebITSHTTP.TransResult();
                                if (curRecorder == LEFT_RECORDER) {
                                    transResult = WebITSHTTP.getITSData(text, src, dst);
                                    vcn = getResources().getStringArray(R.array.voicer_cloud_name)[curOtherPositon];
                                } else if (curRecorder == RIGHT_RECORDER) {
                                    transResult = WebITSHTTP.getITSData(text, dst, src);
                                    vcn = getResources().getStringArray(R.array.voicer_cloud_name)[curSelfPositon];
                                }
                                if (null != transResult && null != vcn) {
                                    WebTTSWS.getTTSData(transResult.getDst(), vcn, responseResult);
                                    if (curRecorder == LEFT_RECORDER) {
                                        textViewSelf.setText(transResult.getSrc());
                                        textViewOther.setText(transResult.getDst());
                                    } else {
                                        textViewOther.setText(transResult.getSrc());
                                        textViewSelf.setText(transResult.getDst());
                                    }
                                }
                            } catch(Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();
                    break;
                default:
                    break;
            }
            return true;
        }
    });
}
