package com.ghostchu.ytkj.ytkjautohealthreport;

import lombok.Data;

@Data
public class NotifyMessage {
    private final String summary;
    private final String content;

    public NotifyMessage(String summary, String content) {
        this.summary = summary;
        this.content = content + "\n\n## 程序日志\n```" + Log.getHistory() + "\n```\n";
    }
}
