package com.ghostchu.ytkj.ytkjautohealthreport;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringJoiner;

public class Log {

    private static final List<String> history = new ArrayList<>();

    public static void error(String log) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        System.err.println("[" + format.format(new Date()) + "] " + log);
        history.add("[" + format.format(new Date()) + "] " + log);
    }

    public static void info(String log) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        System.out.println("[" + format.format(new Date()) + "] " + log);
        history.add("[" + format.format(new Date()) + "] " + log);
    }

    public static String getHistory() {
        StringJoiner joiner = new StringJoiner("\n");
        history.forEach(joiner::add);
        return joiner.toString();
    }
}
