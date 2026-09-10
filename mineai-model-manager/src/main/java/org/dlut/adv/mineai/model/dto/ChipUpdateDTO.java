package org.dlut.adv.mineai.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChipUpdateDTO {
    private Long id; // ID

    private String chipType; // 芯片类型
}
