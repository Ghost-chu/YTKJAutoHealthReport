package com.ghostchu.ytkj.ytkjautohealthreport;

import org.bspfsystems.yamlconfiguration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class Config {
    private final YamlConfiguration config;

    public Config() {
        File file = new File("config.yml");
        if (!file.exists()) {
            try {
                Files.copy(this.getClass().getResourceAsStream("config.yml"), file.toPath());
            } catch (IOException | NullPointerException e) {
                throw new RuntimeException("请正确放置 config.yml 配置文件到工作目录下！", e);
            }
        }
        config = YamlConfiguration.loadConfiguration(file);
    }

    public String getApiKey() {
        return readValue("ocr.apikey");
    }

    public String getApiSecret() {
        return readValue("ocr.apisecret");
    }

    public String getAppId() {
        return readValue("ocr.appid");
    }

    public String getLocation() {
        if (config.getString("location") == null) {
            throw new RuntimeException("请在 config.yml 中配置 location 字段！");
        }
        return config.getString("location", "山东省烟台市蓬莱区蓬莱阁街道海滨西路");
    }

    public String getPassword() {
        return readValue("password");
    }

    private String readValue(String key) {
        return System.getProperty(key, config.getString(key));
    }

    public String getPushApiKey() {
        return readValue("push-api-key");
    }

    public String getPushUid() {
        return readValue("push-uid");
    }

    public String getUserAgent() {
        return readValue("user-agent");
    }

    public String getUsername() {
        return readValue("username");
    }

    public String inSchool() {
        return System.getProperty("in-school", config.getBoolean("in-school") ? "是" : "否");
    }

    public String getFtpHost() {
        return readValue("ftp.host");
    }

    public String getFtpUser() {
        return readValue("ftp.username");
    }

    public String getFtpPassword() {
        return readValue("ftp.password");
    }

    public int getFtpPort() {
        return Integer.parseInt(readValue("ftp.port"));
    }

    public String getImagePrefix() {
        return readValue("image-prefix");
    }

    public boolean skipReportedCheck() {
        return Boolean.parseBoolean(readValue("skip-reported-check"));
    }
}
