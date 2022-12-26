package com.ghostchu.ytkj.ytkjautohealthreport;

import com.baidu.aip.ocr.AipOcr;
import com.ghostchu.ytkj.ytkjautohealthreport.exception.OCRServiceException;
import net.steppschuh.markdowngenerator.text.Text;
import net.steppschuh.markdowngenerator.text.code.CodeBlock;
import net.steppschuh.markdowngenerator.text.heading.Heading;
import org.jetbrains.annotations.Nullable;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringJoiner;

public class OCR {
    @Nullable
    public static String scanText(Config config, File file) throws OCRServiceException, IOException {
        Log.info("转换验证码格式与灰度处理...");
        BufferedImage image = ImageIO.read(file);
        image.getGraphics().drawImage(image, 0, 0, null);
        for (int y = 0; y < image.getHeight(); y++)
            for (int x = 0; x < image.getWidth(); x++) {
                Color pixel = new Color(image.getRGB(x, y));
                image.setRGB(x, y, new Color(getGray(pixel), getGray(pixel), getGray(pixel)).getRGB());
            }
        File outputfile = new File(file.getParentFile(), "ytkjcaptcha.jpeg");
        ImageIO.write(image, "jpeg", outputfile);
        Log.info("上传验证码 OCR...");
        StringJoiner exceptionRaiser = new StringJoiner("\n\n");
        exceptionRaiser.add(new Heading("百度云 OCR 服务不可用", 1).toString());
        List<Character> list = new ArrayList<>();
        // 验证码字符集
        for (char c : "123456789QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm".toCharArray()) {
            list.add(c);
        }
        AipOcr client = new AipOcr(config.getAppId(), config.getApiKey(), config.getApiSecret());
        String path = outputfile.getPath();
        String code;
        JSONObject res = client.basicGeneral(path, new HashMap<>());
        try {
            code = res.getJSONArray("words_result").getJSONObject(0).getString("words");
        } catch (Exception e) {
            exceptionRaiser.add(new Text("服务器响应如下：").toString());
            exceptionRaiser.add(new CodeBlock(res.toString()).toString());
            throw new OCRServiceException(new NotifyMessage("百度云 OCR 服务不可用，请进行手动签到", exceptionRaiser.toString()));
        }
        StringBuilder builder = new StringBuilder();
        for (char c : code.toCharArray()) {
            if (list.contains(c))
                builder.append(c);
        }
        Log.info("百度云验证码 OCR 结果：" + builder);
        if (builder.toString().length() != 4) {
            Log.info("验证码位数不匹配！");
            return null;
        }
        return builder.toString();
    }

    public static int getGray(Color pixel) {
        return (pixel.getRed() * 30 + pixel.getGreen() * 60 + pixel.getBlue() * 10) / 100;
    }
}
