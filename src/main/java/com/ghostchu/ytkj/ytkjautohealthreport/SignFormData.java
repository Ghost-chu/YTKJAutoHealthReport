package com.ghostchu.ytkj.ytkjautohealthreport;

import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public class SignFormData {
    private final Map<String, Object> map;

    public SignFormData(Config config, String setId, String userId, String id) {
        this.map = new LinkedHashMap<>();
        map.put("setid", setId); // 隐藏字段
        map.put("userid", userId);// 隐藏字段
        map.put("id", id);// 隐藏字段
        map.put("zx", config.inSchool()); // 是否在校
        map.put("zx_select", config.inSchool()); // 是否在校（选中项）
        // 宿舍位置
        map.put("wxwz", config.getLocation()); // GPS坐标通过高德地图SDK转换的位置文本
        // 随机安全体温 ( 36.0 ~ 36.7 )
        map.put("tw", randomSafeTemp()); // 体温(早上)
        map.put("tw2", randomSafeTemp()); // 体温(中午)
        map.put("tw5", randomSafeTemp()); // 体温(晚上)
        map.put("ks", "否"); // 是否有咳嗽、呕吐、咽痛、嗅味觉减退等症状
        map.put("ks_select", "否"); // 是否有咳嗽、呕吐、咽痛、嗅味觉减退等症状(选中项)
        map.put("jkm", ""); // 健康码
        map.put("xcm", ""); // 行程卡
        map.put("gtjz1", ""); // 共同居住人健康码
        map.put("gtjz5", ""); // 共同居住人行程卡

        Log.info("上报表格数据已生成：" + map.entrySet());
    }

    /**
     * 随机生成安全体温
     *
     * @return 返回一个在 36.0 ~ 36.7 的体温
     */
    @NotNull
    public String randomSafeTemp() {
        return "36." + new Random().nextInt(7);
    }

    public Map<String, Object> generateParams() {
        return map;
    }
}
