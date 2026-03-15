package com.leyu.aicodegenerator.constant;

public final class AppConstant {
    private AppConstant() {}

    public static final Integer GOOD_APP_PRIORITY = 99;

    public static final Integer DEFAULT_APP_PRIORITY = 0;

    public static final String CODE_OUTPUT_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_output";

    public static final String CODE_DEPLOY_ROOT_DIR = System.getProperty("user.dir") + "/tmp/code_deploy";

    public static final String CODE_DEPLOY_HOST = "http://localhost";
}
