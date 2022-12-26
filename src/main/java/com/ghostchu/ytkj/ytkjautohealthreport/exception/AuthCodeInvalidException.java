package com.ghostchu.ytkj.ytkjautohealthreport.exception;

import com.ghostchu.ytkj.ytkjautohealthreport.NotifyMessage;

/**
 * 当用户提供的 Cookies 失效时，该错误将会抛出
 */
public class AuthCodeInvalidException extends AbstractPushException {

    public AuthCodeInvalidException(NotifyMessage notifyMessage) {
        super(notifyMessage);
    }
}
