package org.dubhe.data.domain.dto;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import java.math.BigDecimal;

/**
 * @author mingming
 * @date 2025/07/07
 */
@Data
public class UserProportionDTO {
    private Long userId;

    @Min(0)
    @Max(100)
    private BigDecimal proportion;
}
