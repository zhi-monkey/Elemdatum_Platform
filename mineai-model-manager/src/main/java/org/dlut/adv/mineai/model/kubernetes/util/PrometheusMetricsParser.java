package org.dlut.adv.mineai.model.kubernetes.util;

import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class PrometheusMetricsParser {

    private static final Pattern METRIC_PATTERN = Pattern.compile(
            "^([a-zA-Z_:][a-zA-Z0-9_:]*)" +  // metric name
                    "(?:\\{([^}]+)\\})?" +            // labels (optional)
                    "\\s+" +                          // whitespace
                    "([\\d.eE+-]+)" +                 // value
                    "(?:\\s+(\\d+))?$"                // timestamp (optional)
    );

    private static final Pattern LABEL_PATTERN = Pattern.compile(
            "([a-zA-Z_][a-zA-Z0-9_]*)=\"([^\"]*)\""
    );

    /**
     * 解析 Prometheus metrics 文本
     */
    public static Map<String, List<MetricData>> parse(String metricsText) {
        Map<String, List<MetricData>> metricsMap = new HashMap<>();

        if (metricsText == null || metricsText.isEmpty()) {
            return metricsMap;
        }

        String[] lines = metricsText.split("\n");

        for (String line : lines) {
            line = line.trim();

            // 跳过注释和空行
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            Matcher matcher = METRIC_PATTERN.matcher(line);
            if (matcher.find()) {
                String metricName = matcher.group(1);
                String labelsStr = matcher.group(2);
                String value = matcher.group(3);

                Map<String, String> labels = parseLabels(labelsStr);

                MetricData metricData = new MetricData(metricName, labels, Double.parseDouble(value));

                metricsMap.computeIfAbsent(metricName, k -> new ArrayList<>()).add(metricData);
            }
        }

        return metricsMap;
    }

    /**
     * 解析标签
     */
    private static Map<String, String> parseLabels(String labelsStr) {
        Map<String, String> labels = new HashMap<>();

        if (labelsStr == null || labelsStr.isEmpty()) {
            return labels;
        }

        Matcher matcher = LABEL_PATTERN.matcher(labelsStr);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            labels.put(key, value);
        }

        return labels;
    }

    /**
     * Metric 数据类
     */
    public static class MetricData {
        private final String name;
        private final Map<String, String> labels;
        private final double value;

        public MetricData(String name, Map<String, String> labels, double value) {
            this.name = name;
            this.labels = labels;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public Map<String, String> getLabels() {
            return labels;
        }

        public double getValue() {
            return value;
        }

        public String getLabel(String key) {
            return labels.get(key);
        }
    }
}
