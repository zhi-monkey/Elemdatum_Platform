
package org.dubhe.biz.base.dto;

import lombok.Data;
import org.dubhe.biz.base.constant.MagicNumConstant;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotBlank;
import java.io.Serializable;


@Data
public class TrainAlgorithmSelectByNameDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    @Length(max = MagicNumConstant.THIRTY_TWO, message = "算法名称有误")
    @NotBlank
    private String algorithmName;
}