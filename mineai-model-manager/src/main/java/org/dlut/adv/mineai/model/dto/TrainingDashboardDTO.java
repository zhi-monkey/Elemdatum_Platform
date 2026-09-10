package org.dlut.adv.mineai.model.dto;


import com.alibaba.fastjson.JSONArray;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author mingming
 * @date 2025/08/25
 */
@Data
public class TrainingDashboardDTO {
    private Double progress;
    private Map<String, List<String>> epochDetail;
    private Map<String, Double> finalMetrics;
    private JSONArray rawLogs;
}
