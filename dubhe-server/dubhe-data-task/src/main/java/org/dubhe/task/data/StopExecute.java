

package org.dubhe.task.data;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.dubhe.biz.base.constant.MagicNumConstant;
import org.dubhe.data.domain.entity.Task;
import org.dubhe.data.service.TaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class StopExecute {

    @Autowired
    private TaskService taskService;

    public boolean isStop(Long taskId){
        QueryWrapper<Task> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(Task::getId,taskId);
        Task task = taskService.selectOne(wrapper);
        return task.getStatus() == MagicNumConstant.THREE;
    }
}
