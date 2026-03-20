package com.leyu.aicodegenerator.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/** ProjectDownloadService contract. */
public interface ProjectDownloadService {

    void downloadProjectAsZip(String projectPath, String downloadFileName, HttpServletResponse response);
}
