package org.dubhe.admin.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @Author：Yan Zhaoyang
 * @Package：org.dubhe.admin.domain.dto
 * @Project：mineai
 * @name：UserStatisticsDTO
 * @Date：2023/12/18 15:24
 * @Filename：UserStatisticsDTO
 */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class UserStatisticsDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Integer userOnNum;

    private Integer userOffNum;

    private Integer userNum;

}
