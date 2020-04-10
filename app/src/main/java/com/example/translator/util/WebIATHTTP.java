package com.example.translator.util;

import android.net.ParseException;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WebIATHTTP {

    private static final String AIUI_URL = "https://openapi.xfyun.cn/v2/aiui";
    private static final String APPID = "5e747d4f";
    private static final String API_KEY = "dce3dba416ee1230cda7e4c1cf164e40";
    private static final String DATA_TYPE = "audio";
    private static final String SCENE = "main_box";
    private static final String SAMPLE_RATE = "16000";
    private static final String AUTH_ID = "88e5b5e42a2a986c9f0ce1c554a537fe";
    private static final String AUE = "raw";

    // 个性化参数，需转义
    private static final String PERS_PARAM = "{\"auth_id\":\"88e5b5e42a2a986c9f0ce1c554a537fe\"}";

    public static String AudioToText(byte[] audio) throws IOException, ParseException, InterruptedException{
        Map<String, String> header = buildHeader();
        String result = httpPost(AIUI_URL, header, audio);
        System.out.println(result);
        return parseIATJson(result);
    }

    private static Map<String, String> buildHeader() throws UnsupportedEncodingException, ParseException {
        String curTime = System.currentTimeMillis() / 1000L + "";
        String param = "{\"aue\":\""+AUE+"\",\"sample_rate\":\""+SAMPLE_RATE+"\",\"auth_id\":\""+AUTH_ID+"\",\"data_type\":\""+DATA_TYPE+"\",\"scene\":\""+SCENE+"\"}";
        //使用个性化参数时参数格式如下：
        //String param = "{\"aue\":\""+AUE+"\",\"sample_rate\":\""+SAMPLE_RATE+"\",\"auth_id\":\""+AUTH_ID+"\",\"data_type\":\""+DATA_TYPE+"\",\"scene\":\""+SCENE+"\",\"pers_param\":\""+PERS_PARAM+"\"}";
        String paramBase64 = new String(Base64.encodeToString(param.getBytes("UTF-8"), Base64.NO_WRAP));
        String checkSum = HexUtil.md5Hex(API_KEY + curTime + paramBase64);
        Map<String, String> header = new HashMap<String, String>();
        header.put("X-Param", paramBase64);
        header.put("X-CurTime", curTime);
        header.put("X-CheckSum", checkSum);
        header.put("X-Appid", APPID);
        return header;
    }

    private static String httpPost(String url, Map<String, String> header, byte[] body) {
        String result = "";
        BufferedReader in = null;
        OutputStream out = null;
        try {
            URL realUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection)realUrl.openConnection();
            for (String key : header.keySet()) {
                connection.setRequestProperty(key, header.get(key));
            }
            connection.setDoOutput(true);
            connection.setDoInput(true);

            //connection.setConnectTimeout(20000);
            //connection.setReadTimeout(20000);
            try {
                out = connection.getOutputStream();
                out.write(body);
                out.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }

            try {
                in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while ((line = in.readLine()) != null) {
                    result += line;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

//    json格式如下
//    {
//        "data": [
//        {
//            "sub": "iat",
//                "auth_id": "91b2438de0cd520a21855f223fb4285c",
//                "text": "测试文本",
//                "result_id": 2,
//                "json_args": {
//            "language": "zh-cn",
//                    "accent": "mandarin"
//        }
//        }
//    ],
//        "sid": "ara033cccfc@dx000111e4e736094000",
//            "code": "0",
//            "desc": "success"
//    }
    static private String parseIATJson(String json) {
        String text = "";
        Gson gson = new Gson();
        ResponseData responseData = gson.fromJson(json, ResponseData.class);
        if (null != responseData.data) {
            for (Data data : responseData.data) {
                text += data.text;
            }
        }
        return text;
    }
    private class ResponseData {
        String sid;
        String code;
        String desc;
        ArrayList<Data> data;
    }
    private class Data {
        String text;
    }
}
