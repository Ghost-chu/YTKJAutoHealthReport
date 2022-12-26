package com.ghostchu.ytkj.ytkjautohealthreport.exception;

import com.ghostchu.ytkj.ytkjautohealthreport.NotifyMessage;

public abstract class AbstractPushException extends Exception {
    private final NotifyMessage notifyMessage;

    public AbstractPushException(NotifyMessage notifyMessage) {
        this.notifyMessage = notifyMessage;
    }

    public NotifyMessage getNotifyMessage() {
        return notifyMessage;
    }
}
