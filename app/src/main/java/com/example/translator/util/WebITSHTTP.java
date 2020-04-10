package com.example.translator.util;

import android.util.Base64;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

import com.google.gson.JsonObject;

/**
 * 机器翻译 WebAPI 接口调用示例
 * 运行前：请先填写Appid、APIKey、APISecret
 * 运行方法：直接运行 main() 即可
 * 结果： 控制台输出结果信息
 *
 * 1.接口文档（必看）：https://www.xfyun.cn/doc/nlp/xftrans/API.html
 * 2.错误码链接：https://www.xfyun.cn/document/error-code （错误码code为5位数字）
 * @author iflytek
 */

public class WebITSHTTP {
    // OTS webapi 接口地址
    private static final String WebITS_URL = "https://itrans.xfyun.cn/v2/its";
    // 应用ID（到控制台获取）
    private static final String APPID = "5e02d61f";
    // 接口APIKey（到控制台机器翻译服务页面获取）
    private static final String API_KEY = "8f4aed44e398cae9923fa76543011d51";
    // 接口APISercet（到控制台机器翻译服务页面获取）
    private static final String API_SECRET = "aeaa04a94d9dccce66de3e13becce8b2";
    // 语种列表参数值请参照接口文档：https://doc.xfyun.cn/rest_api/机器翻译.html

    /**
     * OTS WebAPI 调用示例程序
     *
     * @param text
     * @throws Exception
     */
    public static TransResult getITSData(String text, String src, String dst) throws Exception {
        TransResult transResult = new TransResult();
        if (APPID.equals("") || API_KEY.equals("") || API_SECRET.equals("")) {
            System.out.println("Appid 或APIKey 或APISecret 为空！请打开demo代码，填写相关信息。");
            return null;
        }
        String body = buildHttpBody(text, src, dst);
        //System.out.println("【ITSWebAPI body】\n" + body);
        Map<String, String> header = buildHttpHeader(body);
        Map<String, Object> resultMap = HttpUtil.doPost2(WebITS_URL, header, body);
        if (resultMap != null) {
            String resultStr = resultMap.get("body").toString();
            System.out.println("【ITS WebAPI 接口调用结果】\n" + resultStr);
            //以下仅用于调试
            Gson json = new Gson();
            ResponseData resultData = json.fromJson(resultStr, ResponseData.class);
            int code = resultData.getCode();
            if (resultData.getCode() != 0) {
                System.out.println("请前往https://www.xfyun.cn/document/error-code?code=" + code + "查询解决办法");
            }
            if (null != resultData.getData() &&
                null != resultData.getData().getResult() &&
                null != resultData.getData().getResult().getTransResult() &&
                null != resultData.getData().getResult().getTransResult().getDst()) {
                transResult = resultData.getData().getResult().getTransResult();
            }
        } else {
            System.out.println("调用失败！请根据错误信息检查代码，接口文档：https://www.xfyun.cn/doc/nlp/xftrans/API.html");
        }
        return transResult;
    }

    /**
     * 组装http请求头
     */
    public static Map<String, String> buildHttpHeader(String body) throws Exception {
        Map<String, String> header = new HashMap<String, String>();
        URL url = new URL(WebITS_URL);

        //时间戳
        SimpleDateFormat format = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date dateD = new Date();
        String date = format.format(dateD);
        //System.out.println("【ITS WebAPI date】\n" + date);

        //对body进行sha256签名,生成digest头部，POST请求必须对body验证
        String digestBase64 = "SHA-256=" + signBody(body);
        //System.out.println("【ITS WebAPI digestBase64】\n" + digestBase64);

        //hmacsha256加密原始字符串
        StringBuilder builder = new StringBuilder("host: ").append(url.getHost()).append("\n").//
                append("date: ").append(date).append("\n").//
                append("POST ").append(url.getPath()).append(" HTTP/1.1").append("\n").//
                append("digest: ").append(digestBase64);
        //System.out.println("【ITS WebAPI builder】\n" + builder);
        String sha = hmacsign(builder.toString(), API_SECRET);
        //System.out.println("【ITS WebAPI sha】\n" + sha);

        //组装authorization
        String authorization = String.format("api_key=\"%s\", algorithm=\"%s\", headers=\"%s\", signature=\"%s\"", API_KEY, "hmac-sha256", "host date request-line digest", sha);
        System.out.println("【ITS WebAPI authorization】\n" + authorization);

        header.put("Authorization", authorization);
        header.put("Content-Type", "application/json");
        header.put("Accept", "application/json,version=1.0");
        header.put("Host", url.getHost());
        header.put("Date", date);
        header.put("Digest", digestBase64);
        System.out.println("【ITS WebAPI header】\n" + header);
        return header;
    }


    /**
     * 组装http请求体
     */
    public static String buildHttpBody(String text, String src, String dst) throws Exception {
        JsonObject body = new JsonObject();
        JsonObject business = new JsonObject();
        JsonObject common = new JsonObject();
        JsonObject data = new JsonObject();
        //填充common
        common.addProperty("app_id", APPID);
        //填充business
        business.addProperty("from", src);
        business.addProperty("to", dst);
        //填充data
        //System.out.println("【OTS WebAPI TEXT字个数：】\n" + TEXT.length());
        byte[] textByte = text.getBytes("UTF-8");
        String textBase64 = Base64.encodeToString(textByte, Base64.NO_WRAP);
        //System.out.println("【OTS WebAPI textBase64编码后长度：】\n" + textBase64.length());
        data.addProperty("text", textBase64);
        //填充body
        body.add("common", common);
        body.add("business", business);
        body.add("data", data);
        return body.toString();
    }

    /**
     * 对body进行SHA-256加密
     */
    private static String signBody(String body) throws Exception {
        MessageDigest messageDigest;
        String encodestr = "";
        try {
            messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(body.getBytes("UTF-8"));
            encodestr = Base64.encodeToString(messageDigest.digest(), Base64.NO_WRAP);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return encodestr;
    }

    /**
     * hmacsha256加密
     */
    private static String hmacsign(String signature, String apiSecret) throws Exception {
        Charset charset = Charset.forName("UTF-8");
        Mac mac = Mac.getInstance("hmacsha256");
        SecretKeySpec spec = new SecretKeySpec(apiSecret.getBytes(charset), "hmacsha256");
        mac.init(spec);
        byte[] hexDigits = mac.doFinal(signature.getBytes(charset));
        return Base64.encodeToString(hexDigits, Base64.NO_WRAP);
    }

    public static class ResponseData {
        private int code;
        private String message;
        private String sid;
        private Data data;
        public int getCode() {
            return code;
        }
        public String getMessage() {
            return this.message;
        }
        public String getSid() {
            return sid;
        }
        public Data getData() {
            return data;
        }
    }
    public static class Data {
        private Result result;
        public Result getResult() {
            return result;
        }
    }
    public static class Result {
        private String from;
        private String to;
        private TransResult trans_result;
        public String getfrom() {
            return from;
        }
        public String getto() {
            return to;
        }
        public TransResult getTransResult() {
            return trans_result;
        }
    }
    public static class TransResult {
        private String dst;
        private String src;
        public String getDst() {
            return dst;
        }
        public String getSrc() {
            return src;
        }
    }
}

