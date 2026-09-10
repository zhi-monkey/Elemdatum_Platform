

package org.dubhe.task.execute;

import com.alibaba.fastjson.JSONObject;
import org.dubhe.biz.base.utils.SpringContextHolder;
import org.dubhe.biz.log.enums.LogEnum;
import org.dubhe.biz.log.utils.LogUtil;
import org.dubhe.biz.redis.utils.RedisUtils;

public abstract class AbstractAlgorithmExecute {

    private RedisUtils redisUtils;

    public AbstractAlgorithmExecute(){
        this.redisUtils = SpringContextHolder.getBean(RedisUtils.class);
    }

    public final void finishMethod(Object object, String queueName,JSONObject taskDetail){
        try{
            if(!checkStop(object, queueName,taskDetail)){
                finishExecute(taskDetail);
            }
            deleteRedisKey(object,queueName);
        } catch (Exception e){
            LogUtil.error(LogEnum.BIZ_DATASET, "execute finish task failed:{}", e);
        }
    }

    public final void failMethod(Object object, String queueName,JSONObject failDetail){
        try {
            if(!checkStop(object, queueName, failDetail)){
                failExecute(failDetail);
            }
            deleteRedisKey(object,queueName);
        } catch (Exception e){
            LogUtil.error(LogEnum.BIZ_DATASET, "execute failed task failed:{}", e);
        }
    }

    public abstract void finishExecute(JSONObject taskDetail);

    public void failExecute(JSONObject failDetail){
    }

    public boolean checkStop(Object object, String queueName,JSONObject taskDetail){
        return false;
    }

    public void deleteRedisKey(Object object,String detailQueue) throws Exception{
        // 只删除annotation key（处理结果数据），保留detail key（任务参数）
        // detail key会在整个任务完成后由VideoSampleExecuteThread统一清理
        redisUtils.del(object.toString());
        // 不删除detailQueue，因为重新入队的segment还需要用到
        // redisUtils.del(detailQueue);
    }
}
