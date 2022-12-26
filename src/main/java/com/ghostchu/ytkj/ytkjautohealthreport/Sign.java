package com.ghostchu.ytkj.ytkjautohealthreport;

import com.ghostchu.ytkj.ytkjautohealthreport.exception.AuthCodeInvalidException;
import com.ghostchu.ytkj.ytkjautohealthreport.exception.ParseException;
import com.ghostchu.ytkj.ytkjautohealthreport.exception.SignVerifyFailureException;
import com.ghostchu.ytkj.ytkjautohealthreport.exception.TimeRangeReportedException;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import net.steppschuh.markdowngenerator.image.Image;
import net.steppschuh.markdowngenerator.text.Text;
import net.steppschuh.markdowngenerator.text.code.CodeBlock;
import net.steppschuh.markdowngenerator.text.heading.Heading;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.StringJoiner;

public class Sign {
    private final Config config;

    public Sign(Config config) {
        this.config = config;
    }

    /**
     * 开始健康上报
     *
     * @return 上报结果
     * @throws AuthCodeInvalidException   AuthCode 过期
     * @throws TimeRangeReportedException 该时间段已上报过
     * @throws ParseException             DOM解析异常
     */
    @NotNull
    public SignReport sign() throws AuthCodeInvalidException, TimeRangeReportedException, ParseException, SignVerifyFailureException {
        StringJoiner exceptionRaiser = new StringJoiner("\n\n");
        exceptionRaiser.add(new Heading("上报失败", 1).toString());
        Log.info("检查 auth_code 有效性...");
        if (!isAuthCodeValid()) {
            exceptionRaiser.add(new Text("验证登录态失败，登录态无效").toString());
            throw new AuthCodeInvalidException(new NotifyMessage("上报失败，登录态无效，请手动上报", exceptionRaiser.toString()));
        }
        Log.info("解析健康上报文件地址...");
        String reportUrl = getReportPageUrl();
        if (reportUrl == null) {
            Log.error("上报问卷地址为空，处理失败，DOM可能已发生变动！");
            exceptionRaiser.add(new Text("解析 DOM 失败，问卷地址解析失败，DOM可能已发生变动。").toString());
            throw new ParseException(new NotifyMessage("上报问卷地址为空，处理失败，DOM可能已发生变动！", exceptionRaiser.toString()));
        }
        Log.info("检查重复上报...");
        if (!config.skipReportedCheck())
            if (isReported(reportUrl))
                throw new TimeRangeReportedException(new NotifyMessage("今天已经上报过啦", "# 跳过上报\n\n今天已经上报过啦\n\n"));
        Log.info("正在上报...");
        SignReport firstReportResult = submit(reportUrl);
        if (!firstReportResult.isSuccess()) // 不成功原样返回
            return firstReportResult;
        // 验证上报结果，避免上报失败没有通知
        if (!isReported(reportUrl)) {
            exceptionRaiser.add(new Text("验证上报结果失败，获取上报结果成功但查询上报结果为未上报。").toString());
            throw new SignVerifyFailureException(new NotifyMessage("验证上报结果失败，获取上报结果成功但查询上报结果为未上报。", exceptionRaiser.toString()));
        }
        return firstReportResult;
    }

    /**
     * 检查 AuthCode 有效性
     * 该方法会向 tbIndex 发送一个 GET 请求，如果登录有效则列出可用的健康上报问卷
     * 若 AuthCode 无效则会被重定向到统一认证门户
     *
     * @return Cookies是否有效
     */
    private boolean isAuthCodeValid() {
        HttpResponse<String> resp = Unirest.get("https://ujnpl.educationgroup.cn/jksb/tb/tbIndex")
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/104.0.5112.102 Safari/537.36 Edg/104.0.1293.63").asString();
        if (!resp.isSuccess()) return false;
        return resp.getBody().contains("健康上报");
    }

    /**
     * DOM 解析健康问卷上报链接地址
     *
     * @return 上报链接地址
     */
    @Nullable
    private String getReportPageUrl() {
        HttpResponse<String> resp = Unirest.get("https://ujnpl.educationgroup.cn/jksb/tb/tbIndex").header("User-Agent", config.getUserAgent()).asString();
        // 如果请求出错，则返回空地址
        if (!resp.isSuccess()) return null;
        Document document = Jsoup.parse(resp.getBody());
        for (Element element : document.getElementsByClass("weui-cells")) {
            for (Element child : element.children()) {
                for (Element aElement : child.getElementsByTag("a")) {
                    String linkPath = aElement.attr("href");
                    if (linkPath.startsWith("/jksb/tb/index?id=")) for (Element linkChild : aElement.children()) {
                        for (Element p : linkChild.getElementsByTag("p")) {
                            if (p.text().contains("健康上报")) {
                                Log.info("已选中 DOM：" + JsoupUtil.getCssPath(p) + " => " + p);
                                Log.info("解析 href 地址：" + linkPath);
                                return "https://ujnpl.educationgroup.cn" + linkPath;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * 提交问卷
     *
     * @param reportPageUrl 健康上报问卷 URL
     * @return 上报结果
     */
    @NotNull
    private SignReport submit(String reportPageUrl) {
        SignFormData formData = readMeta(reportPageUrl);
        HttpResponse<String> resp = Unirest.post("https://ujnpl.educationgroup.cn/jksb/tb/save").header("User-Agent", config.getUserAgent()).contentType("application/x-www-form-urlencoded").fields(formData.generateParams()).asString();
        return new SignReport(this, resp.getBody().contains("提交成功"), formData, resp);
    }

    /**
     * 检查目的问卷在目标时间段内是否已进行过上报了
     *
     * @param reportPageUrl 健康上报问卷 URL
     * @return 是否已进行过上报
     */
    private boolean isReported(String reportPageUrl) {
        HttpResponse<String> resp = Unirest.get(reportPageUrl).asString();
        if (!resp.isSuccess()) return false;
        return resp.getBody().contains("今日已填");
    }

    /**
     * 解析健康上报问卷表格的动态变化的ID数据
     *
     * @param reportPageUrl 健康上报问卷 URL
     * @return 表格动态数据
     */
    @NotNull
    private SignFormData readMeta(String reportPageUrl) {
        HttpResponse<String> resp = Unirest.get(reportPageUrl).asString();
        if (!resp.isSuccess()) return null;
        Document document = Jsoup.parse(resp.getBody());
        return new SignFormData(config, document.getElementsByAttributeValue("name", "setid").get(0).val(), document.getElementsByAttributeValue("name", "userid").get(0).val(), document.getElementsByAttributeValue("name", "id").get(0).val());
        // 此处还有个未使用的 ticket 变量，目前好像没什么用，先不加了
    }

    public static class SignReport {
        private final Sign parent;
        private final boolean success;
        private final SignFormData data;
        private final HttpResponse<String> response;

        public SignReport(Sign parent, boolean success, @NotNull SignFormData data, @NotNull HttpResponse<String> response) {
            this.parent = parent;
            this.success = success;
            this.data = data;
            this.response = response;
        }

        /**
         * 生成上报结果摘要
         *
         * @return 上报结果摘要
         */
        @NotNull
        public NotifyMessage generateReport() {
            StringJoiner reportMaker = new StringJoiner("\n\n");
            if (success) {
                reportMaker.add(new Heading("上报成功", 1).toString());
                reportMaker.add(new Text(generateReportSummary()).toString());
                reportMaker.add(new Heading("上报截图", 2).toString());
                String url = new Screenshot(parent.config, parent.getReportPageUrl()).capture();
                if (url != null) {
                    reportMaker.add(new Text("长按以保存图片...").toString());
                    reportMaker.add(new Image(url).toString());
                } else {
                    reportMaker.add(new Text("文件上传失败!").toString());
                }
                reportMaker.add(new Heading("体温数据", 2).toString());
                String temperature = data.generateParams().get("tw") + "℃/" + data.generateParams().get("tw2") + "℃/" + data.generateParams().get("tw5") + "℃";
                reportMaker.add(new Text("早上：" + data.generateParams().get("tw") + "℃").toString());
                reportMaker.add(new Text("中午：" + data.generateParams().get("tw2") + "℃").toString());
                reportMaker.add(new Text("晚上：" + data.generateParams().get("tw5") + "℃").toString());
                reportMaker.add(new Heading("地理位置", 2).toString());
                reportMaker.add(new Text(data.generateParams().get("wxwz")).toString());
                reportMaker.add(new Heading("在校状态", 2).toString());
                reportMaker.add(new Text(data.generateParams().get("zx").equals("是") ? "在校" : "离校").toString());
                return new NotifyMessage("数字烟科上报成功 " + temperature + " " + data.generateParams().get("wxwz").toString() + " " + (data.generateParams().get("zx").equals("是") ? "在校" : "离校"), reportMaker.toString());
            } else {
                reportMaker.add(new Heading("上报失败", 1).toString());
                reportMaker.add(new Text("服务器返回了上报错误响应，请检查表单是否发生变动").toString());
                reportMaker.add(new Heading("服务器响应", 2).toString());
                reportMaker.add(new Text(response.getStatus() + " " + response.getStatusText()).toString());
                reportMaker.add(new CodeBlock(response.getBody()).toString());
                return new NotifyMessage("上报失败 点击工单消息查看详情", reportMaker.toString());
            }
        }

        /**
         * 生成上报结果摘要
         *
         * @return 上报结果摘要
         */
        @NotNull
        public String generateReportMarkdown() {
            StringJoiner joiner = new StringJoiner("\n");
            if (success) {
                joiner.add("# 上报成功");
                joiner.add("## 体温");
                joiner.add(data.generateParams().get("tw") + "℃/" + data.generateParams().get("tw2") + "℃/" + data.generateParams().get("tw5") + "℃");
                joiner.add("## 地理位置");
                joiner.add((String) data.generateParams().get("wxwz"));
                joiner.add("## 在校状态");
                joiner.add(data.generateParams().get("zx").equals("是") ? "在校" : "离校");
            } else {
                joiner.add("# 上报失败");
                joiner.add("## 服务器响应");
                joiner.add("```\n" + response.getStatusText());
                Document document = Jsoup.parse(response.getBody());
                Element element = document.getElementsByTag("h2").first();
                if (element.text() != null) {
                    joiner.add(element.text());
                } else {
                    joiner.add("Unknown");
                }
                joiner.add("```");
            }
            return joiner.toString();
        }

        /**
         * 生成上报结果摘要
         *
         * @return 上报结果摘要
         */
        @NotNull
        public String generateReportSummary() {
            StringJoiner joiner = new StringJoiner(" ");
            if (success) {
                joiner.add("数字烟科上报成功！");
                joiner.add(data.generateParams().get("tw") + "℃/" + data.generateParams().get("tw2") + "℃/" + data.generateParams().get("tw5") + "℃");
                joiner.add((String) data.generateParams().get("wxwz"));
                joiner.add(data.generateParams().get("zx").equals("是") ? "在校" : "离校");
            } else {
                joiner.add("填报失败！！");
                joiner.add("服务器响应 =>");
                if (!response.isSuccess()) {
                    joiner.add("失败 = " + response.getStatusText());
                } else {
                    Document document = Jsoup.parse(response.getBody());
                    Element element = document.getElementsByTag("h2").first();
                    if (element.text() != null) {
                        joiner.add(element.text());
                    } else {
                        joiner.add("Unknown");
                    }
                }
            }
            return joiner.toString();
        }

        @NotNull
        public SignFormData getData() {
            return data;
        }

        @NotNull
        public HttpResponse<String> getResponse() {
            return response;
        }

        public boolean isSuccess() {
            return success;
        }
    }
}
