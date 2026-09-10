package org.dubhe.data.domain.entity;

import lombok.Getter;

/**
 * 图片尺寸信息
 */
@Getter
public class ImageDimension {
    private final int width;
    private final int height;

    public ImageDimension(int width, int height) {
        this.width = width;
        this.height = height;
    }

}
