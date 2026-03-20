package com.leyu.aicodegenerator.utils;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;

/** CacheKeyUtils implementation. */
public class CacheKeyUtils {

    public static String generateKey(Object obj) {
        if (obj == null) return DigestUtil.md5Hex("null");

        String jsonStr = JSONUtil.toJsonStr(obj);
        return DigestUtil.md5Hex(jsonStr);
    }
}
