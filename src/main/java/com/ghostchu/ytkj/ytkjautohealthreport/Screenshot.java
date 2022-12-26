package com.ghostchu.ytkj.ytkjautohealthreport;

import com.google.common.collect.Lists;
import kong.unirest.Cookie;
import kong.unirest.Unirest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.SneakyThrows;
import lombok.ToString;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class Screenshot {
    private final String url;
    private final Config config;

    public Screenshot(Config config, String url) {
        this.config = config;
        this.url = url;
    }

    @SneakyThrows
    public String capture() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments(Lists.newArrayList("--headless", "--no-sandbox"));
        WebDriver driver = new ChromeDriver(options);
        driver.get(url);
        driver.manage().getCookies().forEach(cookie -> Log.info(cookie.toString()));
        for (Cookie coo : Unirest.get(url).asString().getCookies()) {
            driver.manage().addCookie(new org.openqa.selenium.Cookie(coo.getName(), coo.getValue(), coo.getDomain(), coo.getPath(), null));
        }
        driver.manage().window().setSize(new Dimension(390, 844));
        driver.get(url);
        JavascriptExecutor driver_js = ((JavascriptExecutor) driver);
        driver_js.executeScript("document.body.parentNode.style.overflowY= \"hidden\";");
        // 等待 5 秒钟以便提示悬浮窗在页面加载完毕后弹出
        Thread.sleep(5000);
        byte[] sourcefile = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        String fileName = "tmp_" + System.currentTimeMillis() + ".png";
        driver.close();
        driver.quit();
        FTPClient client = new FTPClient();
        client.connect(config.getFtpHost(), config.getFtpPort());
        if (!client.login(config.getFtpUser(), config.getFtpPassword())) {
            Log.info("登陆失败");
            return null;
        }
        if (client.changeWorkingDirectory("/")) {
            Log.info("切换到根目录");
        }
        client.setFileType(FTPClient.BINARY_FILE_TYPE);
        int reply = client.getReplyCode();
        if (!FTPReply.isPositiveCompletion(reply)) {
            Log.info("返回错误信息");
            client.disconnect();
            return null;
        }
        client.setControlEncoding("UTF-8");
        client.enterLocalPassiveMode();
        try (InputStream is = new ByteArrayInputStream(sourcefile)) {
            boolean ok = client.storeFile(fileName, is);
            if (!ok) {
                Log.info("文件传输失败");
                return null;
            }
            return config.getImagePrefix() + fileName;
        }
    }

    @AllArgsConstructor
    @Data
    @ToString
    static class CookiePair {
        private String name;
        private String value;
        private String domain;
        private String path;
    }
}
