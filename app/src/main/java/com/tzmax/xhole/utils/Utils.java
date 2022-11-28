package com.tzmax.xhole.utils;

import com.tzmax.xhole.BaseApplication;
import com.tzmax.xhole.MainActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
import java.util.regex.Pattern;

public class Utils {
    public static String LOCAL_SAVE_DATA_TAG = Objects.requireNonNull(BaseApplication.class.getPackage()).getName();
    public static String MatchTestURLCache = "";

    public static String convertStreamToString(InputStream is) {


        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    // MD5 编码
    public static String md5(String content) {
        byte[] hash;
        try {
            hash = MessageDigest.getInstance("MD5").digest(content.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("NoSuchAlgorithmException", e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("UnsupportedEncodingException", e);
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10) {
                hex.append("0");
            }
            hex.append(Integer.toHexString(b & 0xFF));
        }
        return hex.toString();
    }

    // URL 路由匹配模式
    // 参考链接：https://developer.chrome.com/docs/extensions/mv3/match_patterns/
    public static boolean urlRoutingMatch(String ruleStr, String domainStr) {
        boolean is = false;

        // 替换规则中 * 符号为正则 .* 匹配符号
        ruleStr = ruleStr.replaceAll("(?<=[^\\\\])\\*", "\\.*");

        // 通过正则判断是否匹配
        try {
            if (Pattern.compile(ruleStr).matcher(domainStr).matches()) {
                is = true;
            }
        } catch (Exception e) {
        }

        return is;
    }


}
