package com.ghostchu.ytkj.ytkjautohealthreport.exception;

import com.ghostchu.ytkj.ytkjautohealthreport.NotifyMessage;

/**
 * 百度云验证码 OCR 失败
 */
public class CaptchaOCRException extends AbstractPushException {

    public CaptchaOCRException(NotifyMessage notifyMessage) {
        super(notifyMessage);
    }
}
