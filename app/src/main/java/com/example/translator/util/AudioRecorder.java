package com.example.translator.util;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class AudioRecorder {
    public final static int VOICE_VOLUME = 1;
    public final static int RECORD_STOP = 2;

    private static final String TAG = "AudioRecorder";

    private static int sampleRateInHz = 16000;
    private static int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;

    private static AudioRecorder recorder;

    private int bufferSize;
    private AudioRecord record;

    private boolean on;

    private AudioRecorder() {
        on = false;
        record = null;
        bufferSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);
    }

    /**
     * singleton
     * @return an AudioRecorder object
     */
    public static AudioRecorder getInstance() {
        if (recorder == null)
            recorder = new AudioRecorder();
        return recorder;
    }

    /**
     * start recorder
     * @param handler a handler to send back message for ui changing
     */
    public void start(final Handler handler) {
        if (record != null || on)
            return;
        record = new AudioRecord(MediaRecorder.AudioSource.MIC,
                sampleRateInHz,
                channelConfig,
                audioFormat,
                bufferSize);

        if (record.getState() == AudioRecord.STATE_UNINITIALIZED) {
            Log.e(TAG, "start: record state uninitialized");
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                ArrayList<byte[]> arrayList = new ArrayList<byte[]>();
                record.startRecording();
                on = true;
                while (on) {
                    byte[] buffer = new byte[bufferSize];
                    record.read(buffer, 0, bufferSize);
                    // 计算分贝值
                    Message message = new Message();
                    message.what = VOICE_VOLUME;
                    // 预设分贝最大为160，放大到10000，步进=10000/160=62.5
                    message.arg1 = (int) (doublecalculateVolume(buffer) * 62.5);
                    if (message.arg1 > 10000) message.arg1 = 10000;
                    handler.sendMessage(message);
                    arrayList.add(buffer);
                }

                record.stop();
                record.release();
                record = null;

                int length = 0;
                for (int i =0; i<arrayList.size(); i++) {
                    length += arrayList.get(i).length;
                }
                byte[] pcm = new byte[length];
                int curLength = 0;
                for (int i =0; i<arrayList.size(); i++) {
                    System.arraycopy(arrayList.get(i), 0, pcm, curLength, arrayList.get(i).length);
                    curLength += arrayList.get(i).length;
                }

                Message message = new Message();
                message.what = RECORD_STOP;
                Bundle data = new Bundle();
                data.putByteArray("audio", pcm);
                message.setData(data);
                handler.sendMessage(message);
            }
        }).start();
    }

    /**
     * stop recording
     */
    public void stop() {
        on = false;
    }

    //录音的编码主要有两种：8位pcm和16位pcm。8位pcm用一个字节表示语音的一个点，16位pcm用两个字节，也就是一个short来表示语音的一个点。需要特别注意的是，如果你用的16位pcm编码，而取录音数据用的是byte的话，需要自己将两个bye转换成一个short。将两个byte转换成一个short，有小端和大端两种，一般默认情况都是小端，但是有的开源库，比如lamemp3需要的就是大端，这个要根据不同的情况进行不同的处理。
    private double calculateVolume(short[] buffer){
        double sumVolume = 0.0;
        double avgVolume = 0.0;
        double volume = 0.0;
        for(short b : buffer){
            sumVolume += Math.abs(b)*Math.abs(b);
        }
        // 平均音量大小
        avgVolume = sumVolume / buffer.length;
        // 音量转分贝的公式
        volume = Math.log10(1 + avgVolume) * 10;
        return volume;
    }

    //如果录音的编码是16为pcm，而录音数据数据是byte，需要将两个byte转为一个short进行处理，建议用小端的方式。
    private double doublecalculateVolume(byte[] buffer)
    {
        double sumVolume = 0.0;
        double avgVolume = 0.0;
        double volume = 0.0;
        for(int i = 0; i < buffer.length; i+=2){
            int v1 = buffer[i] & 0xFF;
            int v2 = buffer[i + 1] & 0xFF;
            int temp = v1 + (v2 << 8);// 小端
            if(temp >= 0x8000) {
                temp = 0xffff - temp;
            }
            sumVolume += temp*temp;
        }
        // 平均音量大小
        avgVolume = sumVolume / (buffer.length/2);
        // 音量转分贝的公式
        volume = Math.log10(1 + avgVolume) * 10;
        return volume;
    }
}
