package com.ghostchu.ytkj.ytkjautohealthreport;

import com.ghostchu.ytkj.ytkjautohealthreport.exception.CaptchaOCRException;
import com.ghostchu.ytkj.ytkjautohealthreport.exception.LoginException;
import com.ghostchu.ytkj.ytkjautohealthreport.exception.OCRServiceException;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import net.steppschuh.markdowngenerator.text.Text;
import net.steppschuh.markdowngenerator.text.code.CodeBlock;
import net.steppschuh.markdowngenerator.text.heading.Heading;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;

public class Auth {
    private final Config config;

    public Auth(Config config) {
        this.config = config;
    }

    /**
     * 登录烟台科技学院统一身份验证系统
     *
     * @throws CaptchaOCRException 百度云 OCR 错误
     * @throws LoginException      登录失败
     */
    public void auth() throws CaptchaOCRException, LoginException, OCRServiceException {
        // 初始化错误块
        StringJoiner exceptionRaiser = new StringJoiner("\n\n");
        exceptionRaiser.add(new Heading("鉴权失败", 1).toString());
        // 初始化错误块结束

        Map<String, Object> mapping = getPostMapping();
        String captcha;
        try {
            captcha = getCaptcha();
        } catch (IOException e) {
            exceptionRaiser.add(new Text("图片编码转换失败。").toString());
            throw new CaptchaOCRException(new NotifyMessage("jfif转jpg与灰度处理失败。", exceptionRaiser.toString()));
        }
        if (captcha == null) {
            exceptionRaiser.add(new Text("连续 10 次验证码 OCR 失败，请手动签到。").toString());
            throw new CaptchaOCRException(new NotifyMessage("连续 10 次验证码 OCR 失败，请手动签到。", exceptionRaiser.toString()));
        }
        mapping.put("code", captcha);
        Unirest.get("https://ujnpl.educationgroup.cn/sso/auth?redirect=%2Fapi%3Fscope%3Dbase%26response_type%3Dcode%26state%3Ddefault%26redirect_uri%3Dhttps%253A%252F%252Fujnpl.educationgroup.cn%252Fportal%252FoauthApi%252FgetAccessToken%253Fredirect%253D%25252Fhome%2526authType%253Dauth%26client_id%3DA0002").asString();
        HttpResponse<String> result = Unirest.post("https://ujnpl.educationgroup.cn/sso/auth/login")
                .fields(mapping)
                .header("referer", "https://ujnpl.educationgroup.cn/sso/auth?redirect=%2Fapi%3Fscope%3Dbase%26response_type%3Dcode%26state%3Ddefault%26redirect_uri%3Dhttps%253A%252F%252Fujnpl.educationgroup.cn%252Fportal%252FoauthApi%252FgetAccessToken%253Fredirect%253D%25252Fhome%2526authType%253Dauth%26client_id%3DA0002")
                .asString();

        if (!result.isSuccess()) {
            // 鉴权错误处理
            Log.error("无法登录：" + result.getBody());
            exceptionRaiser.add(new Text("服务器返回了非预期的鉴权响应，请检查是否为用户名或密码错误").toString());
            exceptionRaiser.add(new Heading("服务器响应", 2).toString());
            exceptionRaiser.add(new Text(result.getStatus() + " " + result.getStatusText()).toString());
            exceptionRaiser.add(new CodeBlock(result.getBody()).toString());
            throw new LoginException(new NotifyMessage("服务器返回了非预期的鉴权响应，请检查是否为用户名或密码错误。", exceptionRaiser.toString()));
        }

        // 解析跳转目标地址
        String url = "https://ujnpl.educationgroup.cn" + JsoupUtil.getUrlFromMetaRedirect(result.getBody());
        HttpResponse<String> resp = Unirest.get(url).asString();
        Log.info(url);
        if (resp.isSuccess()) {
            Log.info("登录成功！");
        } else {
            exceptionRaiser.add(new Text("疫情上报系统鉴权失败，请检查 Access Token 是否有效。").toString());
            exceptionRaiser.add(new Heading("服务器响应", 2).toString());
            exceptionRaiser.add(new Text(result.getStatus() + " " + result.getStatusText()).toString());
            exceptionRaiser.add(new CodeBlock(result.getBody()).toString());
            throw new LoginException(new NotifyMessage("疫情上报系统鉴权失败，请检查 Access Token 是否有效。", exceptionRaiser.toString()));
        }
    }

    /**
     * 获取请求体
     *
     * @return 请求体（无code）
     */
    @NotNull
    public Map<String, Object> getPostMapping() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("redirect", "/api?scope=base&response_type=code&state=default&redirect_uri=https%3A%2F%2Fujnpl.educationgroup.cn%2Fportal%2FoauthApi%2FgetAccessToken%3Fredirect%3D%252Findex%26authType%3Dauth&client_id=A0002");
        map.put("loginType", "account");
        map.put("usertel", "");
        map.put("usertel_code", "");
        map.put("username", Base64.getEncoder().encodeToString(config.getUsername().getBytes(StandardCharsets.UTF_8)));
        map.put("password", Base64.getEncoder().encodeToString(config.getPassword().getBytes(StandardCharsets.UTF_8)));
        return map;
    }

    /**
     * 获取一个可用的验证码
     *
     * @return 验证码
     */
    @Nullable
    public String getCaptcha() throws OCRServiceException, IOException {
        int retry = 0;
        while (retry < 15) {
            File tmpFile = new File(System.getProperty("java.io.tmpdir"), "ytkjcaptcha.jfif");
            HttpResponse<File> resp = Unirest.get("https://ujnpl.educationgroup.cn/sso/auth/genCode?random=" + Math.random())
                    .asFile(tmpFile.getPath(), StandardCopyOption.REPLACE_EXISTING);
            if (resp.isSuccess()) {
                String code = OCR.scanText(config, resp.getBody());
                resp.getBody().deleteOnExit();
                if (code != null)
                    return code;
            }
            Log.error("验证码验证失败，重试...");
            retry++;
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        Log.error("验证码重试失败超过 15 次...");
        return null;
    }
}
