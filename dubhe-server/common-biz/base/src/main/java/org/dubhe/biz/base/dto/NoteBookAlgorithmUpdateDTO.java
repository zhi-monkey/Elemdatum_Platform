

package org.dubhe.biz.base.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

/**
 * @description 算法更新notebook对象
 * @date 2020-12-14
 */
@Data
public class NoteBookAlgorithmUpdateDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     *   NoteBookID
     */
    @NotEmpty(message = "NoteBookID不能为空")
    private List<Long> notebookIdList;

    /**
     *   算法ID
     */
    @NotNull(message = "算法ID不能为空")
    private Long algorithmId;


}
