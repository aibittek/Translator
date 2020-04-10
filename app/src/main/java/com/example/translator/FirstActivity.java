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
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.translator.util.AudioRecorder;

public class FirstActivity extends AppCompatActivity {
    private static final String TAG = "FirstActivity";
    private ImageView imageView;
    private Drawable drawable;
    private Button button;
    private boolean recording = false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first);
        imageView = findViewById(R.id.img);
        button = findViewById(R.id.btn_left);
        drawable = imageView.getDrawable();

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    if (ContextCompat.checkSelfPermission(FirstActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(FirstActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                        return;
                    }
                    if (!recording)
                        startRecorder();
                    else
                        stopRecorder();
                }
            });
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
                startRecorder();
            }
        }
    }

    private void startRecorder(){
        Toast.makeText(FirstActivity.this, "正在录音", Toast.LENGTH_SHORT).show();
        button.setText("停止录音");
        AudioRecorder.getInstance().start(handler);
        recording = !recording;
    }
    private void stopRecorder(){
        Toast.makeText(FirstActivity.this, "已停止录音", Toast.LENGTH_SHORT).show();
        button.setText("开始录音");
        AudioRecorder.getInstance().stop();
        recording = !recording;
    }

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what){
                case AudioRecorder.VOICE_VOLUME:
                    drawable.setLevel(msg.arg1);
                    break;
                case AudioRecorder.RECORD_STOP:
                    drawable.setLevel(0);
                    break;
                default:
                    break;
            }
            return true;
        }
    });
}
