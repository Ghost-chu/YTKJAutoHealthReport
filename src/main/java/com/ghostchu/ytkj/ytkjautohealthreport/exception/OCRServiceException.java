package com.ghostchu.ytkj.ytkjautohealthreport.exception;

import com.ghostchu.ytkj.ytkjautohealthreport.NotifyMessage;

/**
 * 百度云OCR服务不可用
 */
public class OCRServiceException extends AbstractPushException {

    public OCRServiceException(NotifyMessage notifyMessage) {
        super(notifyMessage);
    }
}
