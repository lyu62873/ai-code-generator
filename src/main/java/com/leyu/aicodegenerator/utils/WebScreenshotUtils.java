package com.leyu.aicodegenerator.utils;

import cn.hutool.core.img.ImgUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.leyu.aicodegenerator.exception.BusinessException;
import com.leyu.aicodegenerator.exception.ErrorCode;
import io.github.bonigarcia.wdm.WebDriverManager;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.time.Duration;
import java.util.UUID;

/** WebScreenshotUtils implementation. */
@Slf4j
public class WebScreenshotUtils {

    private static final WebDriver webDriver;

    static {
        final int DEFAULT_WIDTH = 1600;
        final int DEFAULT_HEIGHT = 900;
        webDriver = initChromeDriver(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /** destroy implementation. */
    @PreDestroy
    public void destroy() {
        webDriver.quit();
    }

    /**
     *  Initialize Chrome Driver
     */
    private static WebDriver initChromeDriver(int width, int height) {
        try {
            // auto manage ChromeDriver
            WebDriverManager.chromedriver().setup();
            // set up Chrome
            ChromeOptions options = new ChromeOptions();
            // headless
            options.addArguments("--headless");

            options.addArguments("--disable-gpu");
            // Docker environment needed
            options.addArguments("--no-sandbox");

            options.addArguments("--disable-dev-shm-usage");
            // set window
            options.addArguments(String.format("--window-size=%d,%d", width, height));

            options.addArguments("--disable-extensions");
            // set user-agent
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            // create driver
            WebDriver driver = new ChromeDriver(options);
            // set timeout
            driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(30));
            // set waiting
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(10));
            return driver;
        } catch (Exception e) {
            log.error("Initializing Chrome failed", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Initializing Chrome failed");
        }
    }

    /** saveImage implementation. */
    private static void saveImage(byte[]  imageBytes, String imagePath) {
        try {
            FileUtil.writeBytes(imageBytes, imagePath);
        } catch (Exception e) {
            log.error("Saving image failed: {}", imagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Saving image failed");
        }
    }

    /** compressImage implementation. */
    private static void compressImage(String originalImagePath, String compressedImagePath) {
        final float COMPRESSION_QUALITY = 0.3f;
        try {
            ImgUtil.compress(
                    FileUtil.file(originalImagePath),
                    FileUtil.file(compressedImagePath),
                    COMPRESSION_QUALITY
            );
        } catch (Exception e) {
            log.error("Compressing image failed: {} -> {}", originalImagePath, compressedImagePath, e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "Compressing image failed");
        }
    }

    /** waitForPageLoad implementation. */
    private static void waitForPageLoad(WebDriver driver) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
            wait.until(webDriver -> ((JavascriptExecutor) webDriver).executeScript("return document.readyState").equals("complete"));
            Thread.sleep(2000);
            log.info("Page load complete");
        } catch (Exception e) {
            log.error("Exception occurred while waiting for page load, continue making screenshot", e);
        }
    }

    /** saveWebPageScreenshot implementation. */
    public static String saveWebPageScreenshot(String webUrl) {
        if (StrUtil.isBlank(webUrl)) {
            log.error("webUrl is empty");
            return null;
        }
        try {
            String rootPath = System.getProperty("user.dir") + File.separator + "tmp" + File.separator + "screenshots"
                    + File.separator + UUID.randomUUID().toString().substring(0, 8);
            FileUtil.mkdir(rootPath);

            final String IMAGE_SUFFIX = ".png";

            String imageSavePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + IMAGE_SUFFIX;

            webDriver.get(webUrl);

            waitForPageLoad(webDriver);

            byte[] screenshotsBytes = ((TakesScreenshot) webDriver).getScreenshotAs(OutputType.BYTES);

            saveImage(screenshotsBytes, imageSavePath);
            log.info("Original screenshot saved to {}", imageSavePath);

            final String COMPRESSION_SUFFIX = "_compressed.jpg";
            String compressedImagePath = rootPath + File.separator + RandomUtil.randomNumbers(5) + COMPRESSION_SUFFIX;
            compressImage(imageSavePath, compressedImagePath);
            log.info("Compressed screenshot saved to {}", compressedImagePath);
            FileUtil.del(imageSavePath);
            return compressedImagePath;
        } catch (Exception e) {
            log.error("Screenshot save failed: {}", webUrl, e);
            return null;
        }
    }
}
