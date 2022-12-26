package com.ghostchu.ytkj.ytkjautohealthreport;

import com.ghostchu.ytkj.ytkjautohealthreport.exception.*;
import kong.unirest.Unirest;

public class AutoSign {
    /**
     * 启动主类
     *
     * @param args 启动参数（未使用）
     */
    public static void main(String[] args) {
        Log.info("数字烟科健康上报小助手 - v1.3");
        Log.info("读取配置文件...");
        Config config = new Config();
        Log.info("临时目录被设置为：" + System.getProperty("java.io.tmpdir"));
        Log.info("正在设置 HTTP 请求配置项...");
        Unirest.config()
                // 设置默认 User-Agent
                .addDefaultHeader("User-Agent", config.getUserAgent()).followRedirects(true).cacheResponses(false).enableCookieManagement(true);
        Log.info("登录到烟台科技学院统一身份验证系统...");
        Auth auth = new Auth(config);
        try {
            auth.auth();
        } catch (CaptchaOCRException e) {
            Log.error("验证码 OCR 失败，是否验证码发生了变动？");
            NotifyUtil.notify(config, e.getNotifyMessage());
            return;
        } catch (LoginException e) {
            Log.error("登陆失败");
            NotifyUtil.notify(config, e.getNotifyMessage());
            return;
        } catch (OCRServiceException e) {
            Log.error("百度云服务无效");
            NotifyUtil.notify(config, e.getNotifyMessage());
            return;
        }
        Log.info("加载签到工具类...");
        Sign sign = new Sign(config);
        Log.info("正在执行健康上报...");
        try {
            Sign.SignReport report = sign.sign();
            if (report.isSuccess()) {
                Log.info("上报成功！");
            } else {
                Log.error("上报失败：" + report.generateReportSummary() + " => " + report.getResponse().getBody() + " - " + report.getResponse().getStatusText());
            }
            NotifyUtil.notify(config, report.generateReport());
        } catch (AuthCodeInvalidException e) {
            Log.error("错误：auth_code 无效");
            NotifyUtil.notify(config, e.getNotifyMessage());
            System.exit(-1);
        } catch (ParseException e) {
            Log.error("错误：DOM节点解析异常");
            NotifyUtil.notify(config, e.getNotifyMessage());
            System.exit(-1);
        } catch (TimeRangeReportedException e) {
            Log.info("该时间段内已被上报过健康数据");
            NotifyUtil.notify(config, e.getNotifyMessage());
        } catch (SignVerifyFailureException e) {
            Log.error("签到结果验证不同步！");
            NotifyUtil.notify(config, e.getNotifyMessage());
            System.exit(-1);
        }
        Log.info("正在停止 HTTP 客户端...");
        Unirest.shutDown();
        Log.info("Bye！");
    }
}
