package com.ghostchu.ytkj.ytkjautohealthreport;

import com.zjiecode.wxpusher.client.WxPusher;
import com.zjiecode.wxpusher.client.bean.Message;

/**
 * PushDeer 客户端消息推送工具类
 */
public class NotifyUtil {
    public static void notify(Config config, NotifyMessage msg) {
        Message message = new Message();
        message.setAppToken(config.getPushApiKey());
        message.setSummary(msg.getSummary());
        message.setContentType(Message.CONTENT_TYPE_MD);
        message.setContent(msg.getContent());
        message.setUid(config.getPushUid());
        Log.info(WxPusher.send(message).getMsg());
    }
}
