package com.ghostchu.ytkj.ytkjautohealthreport.exception;

import com.ghostchu.ytkj.ytkjautohealthreport.NotifyMessage;

/**
 * 当健康上报已上报过时抛出
 */
public class TimeRangeReportedException extends AbstractPushException {

    public TimeRangeReportedException(NotifyMessage notifyMessage) {
        super(notifyMessage);
    }
}
