package org.dubhe.data.service.pcd;

import com.alibaba.fastjson.JSONObject;
import org.dubhe.biz.base.exception.BusinessException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Reads only the ASCII PCD header. Binary and compressed PCD files are rejected. */
public final class PcdAsciiParser {
    private static final int MAX_HEADER_BYTES = 1024 * 1024;

    private PcdAsciiParser() {
    }

    public static ParsedPcd parse(InputStream input) throws IOException {
        Map<String, Object> metadata = new LinkedHashMap<>();
        long consumed = 0;
        String data = null;
        Long points = null;
        Long width = null;
        Long height = null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(input, StandardCharsets.US_ASCII))) {
            String line;
            while ((line = reader.readLine()) != null) {
                consumed += line.length() + 1L;
                if (consumed > MAX_HEADER_BYTES) {
                    throw new BusinessException("PCD header is too large");
                }
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                int split = line.indexOf(' ');
                if (split < 0) {
                    split = line.indexOf('\t');
                }
                String key = split < 0 ? line : line.substring(0, split);
                String value = split < 0 ? "" : line.substring(split + 1).trim();
                metadata.put(key.toLowerCase(), value);
                if ("points".equalsIgnoreCase(key)) {
                    points = parseLong(value, "POINTS");
                } else if ("width".equalsIgnoreCase(key)) {
                    width = parseLong(value, "WIDTH");
                } else if ("height".equalsIgnoreCase(key)) {
                    height = parseLong(value, "HEIGHT");
                } else if ("data".equalsIgnoreCase(key)) {
                    data = value.toLowerCase();
                    break;
                }
            }
        }
        if (data == null) {
            throw new BusinessException("Invalid PCD file: DATA header is missing");
        }
        if (!"ascii".equals(data)) {
            throw new BusinessException("Only ASCII PCD files are supported");
        }
        if (!metadata.containsKey("version") || !metadata.containsKey("fields") || !metadata.containsKey("size")
                || !metadata.containsKey("type") || width == null || height == null) {
            throw new BusinessException("Invalid PCD file: mandatory header fields are missing");
        }
        String fields = String.valueOf(metadata.get("fields")).toLowerCase();
        if (!fields.matches(".*(^|\\s)x(\\s|$).*") || !fields.matches(".*(^|\\s)y(\\s|$).*")
                || !fields.matches(".*(^|\\s)z(\\s|$).*")) {
            throw new BusinessException("Invalid PCD file: FIELDS must contain x, y and z");
        }
        if (points == null && width != null && height != null) {
            try {
                points = Math.multiplyExact(width, height);
            } catch (ArithmeticException ex) {
                throw new BusinessException("Invalid PCD dimensions");
            }
        }
        if (points == null || points < 0) {
            throw new BusinessException("Invalid PCD file: POINTS header is missing");
        }
        long expectedPoints;
        try {
            expectedPoints = Math.multiplyExact(width, height);
        } catch (ArithmeticException ex) {
            throw new BusinessException("Invalid PCD dimensions");
        }
        if (width < 0 || height < 0 || points != expectedPoints) {
            throw new BusinessException("Invalid PCD dimensions");
        }
        metadata.put("data_encoding", "ascii");
        return new ParsedPcd(points, JSONObject.toJSONString(metadata));
    }

    private static long parseLong(String value, String key) {
        try {
            return Long.parseLong(value.split("\\s+")[0]);
        } catch (RuntimeException ex) {
            throw new BusinessException("Invalid PCD " + key + " value");
        }
    }

    public static final class ParsedPcd {
        private final Long pointCount;
        private final String metadata;

        public ParsedPcd(Long pointCount, String metadata) {
            this.pointCount = pointCount;
            this.metadata = metadata;
        }

        public Long getPointCount() {
            return pointCount;
        }

        public String getMetadata() {
            return metadata;
        }
    }
}
