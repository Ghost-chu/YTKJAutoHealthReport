package com.ghostchu.ytkj.ytkjautohealthreport.exception;

import com.ghostchu.ytkj.ytkjautohealthreport.NotifyMessage;

/**
 * 由于某种原因登陆失败了
 */
public class LoginException extends AbstractPushException {

    public LoginException(NotifyMessage notifyMessage) {
        super(notifyMessage);
    }
}
