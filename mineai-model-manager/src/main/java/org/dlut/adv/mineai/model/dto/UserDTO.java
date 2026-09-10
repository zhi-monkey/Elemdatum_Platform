package org.dlut.adv.mineai.model.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author oyjp
 * @create 2024/4/7 18:18
 */
@Data
public class UserDTO {

    private Long id;

    private String username;
    /**
     * 用户昵称
     */
    private String nickName;

}
