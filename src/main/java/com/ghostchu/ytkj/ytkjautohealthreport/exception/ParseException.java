package com.ghostchu.ytkj.ytkjautohealthreport.exception;

import com.ghostchu.ytkj.ytkjautohealthreport.NotifyMessage;

/**
 * 当 DOM 节点解析出错时抛出
 */
public class ParseException extends AbstractPushException {

    public ParseException(NotifyMessage notifyMessage) {
        super(notifyMessage);
    }
}
