package org.dlut.adv.mineai.packagem.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author mingming
 * @date 2025/06/25
 */
@AllArgsConstructor
@Data
public class PackedFileResult {
    private String fileName;
    private byte[] data;
}
