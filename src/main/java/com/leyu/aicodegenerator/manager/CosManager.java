package com.leyu.aicodegenerator.manager;

import com.leyu.aicodegenerator.config.CosClientConfig;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

/** CosManager implementation. */
@Component
@Slf4j
public class CosManager {

    @Resource
    private CosClientConfig cosClientConfig;

    @Resource
    private COSClient cosClient;


    /** putObject implementation. */
    public PutObjectResult putObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(cosClientConfig.getBucket(), key, file);
        return cosClient.putObject(putObjectRequest);
    }

    public String uploadFile(String key, File file) {
        PutObjectResult result = putObject(key, file);
        if (result != null) {
            String url = String.format("%s%s", cosClientConfig.getHost(), key);
            log.info("Upload file {} success, url: {}", file.getName(), url);
            return url;
        } else {
            log.info("Upload file fail, return null");
            return null;
        }

    }
}
