package com.ghostchu.ytkj.ytkjautohealthreport;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class JsoupUtil {
    /*
    来自 StackOverFlow，用于 debug 元素节点
     */
    public static String getCssPath(Element el) {
        if (el == null)
            return "";

        if (!el.id().isEmpty())
            return "#" + el.id();

        StringBuilder selector = new StringBuilder(el.tagName());
        String classes = StringUtil.join(el.classNames(), ".");
        if (!classes.isEmpty())
            selector.append('.').append(classes);

        if (el.parent() == null)
            return selector.toString();

        selector.insert(0, " > ");
        if (el.parent().select(selector.toString()).size() > 1)
            selector.append(String.format(
                    ":nth-child(%d)", el.elementSiblingIndex() + 1));

        return getCssPath(el.parent()) + selector;
    }

    @NotNull
    public static String getUrlFromMetaRedirect(String html) {
        Document doc = Jsoup.parse(html);
        Element eMETA = doc.select("META").first();
        String content = eMETA.attr("content");
        return StringUtils.substringAfter(content, "url=");
    }
}
