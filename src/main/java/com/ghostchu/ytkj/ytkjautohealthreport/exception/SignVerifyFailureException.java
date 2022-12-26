package com.ghostchu.ytkj.ytkjautohealthreport.exception;

import com.ghostchu.ytkj.ytkjautohealthreport.NotifyMessage;

/**
 * 登录验证失败
 */
public class SignVerifyFailureException extends AbstractPushException {

    public SignVerifyFailureException(NotifyMessage notifyMessage) {
        super(notifyMessage);
    }
}
