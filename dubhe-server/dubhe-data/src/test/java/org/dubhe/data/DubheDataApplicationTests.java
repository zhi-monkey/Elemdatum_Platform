

package org.dubhe.data;

import org.dubhe.biz.log.enums.LogEnum;
import org.dubhe.biz.log.utils.LogUtil;
import org.dubhe.data.service.DataTeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @description 定时任务管理
 * @date 2020-12-18
 */
@SpringBootTest
class DubheDataApplicationTests {

    @Autowired
    DataTeamService dataTeamService;

    @Test
    void contextLoads() {
    }

    @Test
    public void testGetSubtaskStatusListByTaskId(){
        System.out.println(dataTeamService.getSubtaskStatusListByTaskId(15L));
    }

}
