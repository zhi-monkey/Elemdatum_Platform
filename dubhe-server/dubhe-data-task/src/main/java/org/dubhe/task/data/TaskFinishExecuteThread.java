

package org.dubhe.task.data;

import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.dubhe.biz.base.constant.MagicNumConstant;
import org.dubhe.biz.base.utils.SpringContextHolder;
import org.dubhe.biz.log.enums.LogEnum;
import org.dubhe.biz.log.utils.LogUtil;
import org.dubhe.biz.redis.utils.RedisUtils;
import org.dubhe.data.util.TaskUtils;
import org.dubhe.task.constant.DataAlgorithmEnum;
import org.dubhe.task.constant.TaskQueueNameEnum;
import org.dubhe.task.execute.AbstractAlgorithmExecute;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.TimeUnit;

@Component
public class TaskFinishExecuteThread implements Runnable {

    @Autowired
    private TaskUtils taskUtils;

    @Autowired
    private RedisUtils redisUtils;

    private Object object;

    private String detailQueue;

    /**
     * 启动标注任务处理线程
     */
    @PostConstruct
    public void start() {
        Thread thread = new Thread(this, "任务完成任务处理队列");
        thread.start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                finishExecute(true);
                finishExecute(false);
                TimeUnit.MILLISECONDS.sleep(MagicNumConstant.ONE_THOUSAND);
            } catch (Exception e) {
                LogUtil.error(LogEnum.BIZ_DATASET, "finish algorithm task failed:{}", e);
            }
        }
    }

    private void finishExecute(boolean ifFinish) {
        String queueName;
        if(ifFinish){
            queueName = TaskQueueNameEnum.getTemplate(TaskQueueNameEnum.FINISHED_TASK, TaskQueueNameEnum.TaskQueueConfigEnum.ALL);
        } else {
            queueName = TaskQueueNameEnum.getTemplate(TaskQueueNameEnum.FAILED_TASK, TaskQueueNameEnum.TaskQueueConfigEnum.ALL);
        }
        JSONObject detail = getDetail(queueName, ifFinish);
        if(detail != null){
            Integer algorithm = detail.getInteger("algorithm");
            AbstractAlgorithmExecute abstractAlgorithmExecute = (AbstractAlgorithmExecute) SpringContextHolder.getBean(DataAlgorithmEnum.getType(algorithm).getClassName());
            // 将object信息添加到detail中，方便finishExecute使用
            detail.put("queueObject", object);
            if(ifFinish){
                abstractAlgorithmExecute.finishMethod(object,detailQueue,detail);
            } else {
                abstractAlgorithmExecute.failMethod(object,detailQueue,detail);
            }
        }
    }

    private JSONObject getDetail(String queueName,boolean ifFinish){
        if(ifFinish){
            object = taskUtils.getFinishedTask(queueName);
        } else {
            object = taskUtils.getFailedTask(queueName);
        }
        if (ObjectUtil.isNotNull(object)) {
            // 处理字节数组：如果object是byte[]，需要转换为字符串
            String objectString;
            if (object instanceof byte[]) {
                objectString = new String((byte[]) object, java.nio.charset.StandardCharsets.UTF_8);
            } else {
                objectString = object.toString();
            }
            
            // 处理十六进制格式 x'...' (MySQL十六进制字面量)
            if (objectString.startsWith("x'") && objectString.endsWith("'")) {
                String hexString = objectString.substring(2, objectString.length() - 1);
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < hexString.length(); i += 2) {
                    String hexByte = hexString.substring(i, i + 2);
                    sb.append((char) Integer.parseInt(hexByte, 16));
                }
                objectString = sb.toString();
            }
            
            // 去除可能存在的引号（无论是byte[]还是String都需要）
            if (objectString.startsWith("\"") && objectString.endsWith("\"")) {
                objectString = objectString.substring(1, objectString.length() - 1);
            }
            
            // 更新object为转换后的字符串，确保后续使用的是正确的字符串格式
            object = objectString;
            
            // 兼容处理annotation和finished两种队列
            int annotationIndex = objectString.lastIndexOf("annotation");
            int finishedIndex = objectString.lastIndexOf("finished");
            
            String dataKey;  // 实际存储数据的key
            if (annotationIndex >= 0) {
                // 从annotation队列取出，数据就在annotation key中
                dataKey = objectString;
                // 构造detail key用于后续删除等操作
                StringBuffer sb = new StringBuffer(objectString);
                detailQueue = sb.replace(annotationIndex, annotationIndex + "annotation".length(), "detail").toString();
            } else if (finishedIndex >= 0) {
                // 从finished队列取出，需要转换为annotation key来获取数据
                StringBuffer sb = new StringBuffer(objectString);
                dataKey = sb.replace(finishedIndex, finishedIndex + "finished".length(), "annotation").toString();
                // 构造detail key
                detailQueue = objectString.replace("finished", "detail");
            } else {
                LogUtil.error(LogEnum.BIZ_DATASET, "无法识别的队列key格式: {}, annotationIndex: {}, finishedIndex: {}", objectString, annotationIndex, finishedIndex);
                return null;
            }
            
            // 从detail key获取原始任务参数（包含algorithm等字段）
            Object detailObj = redisUtils.get(detailQueue);
            
            JSONObject taskDetail = null;
            if (detailObj instanceof String) {
                // Redis返回的是String（JSON字符串），直接解析
                taskDetail = JSON.parseObject((String) detailObj);
            } else if (detailObj instanceof JSONObject) {
                // Redis返回的已经是JSONObject，直接使用
                taskDetail = (JSONObject) detailObj;
            } else {
                // 其他类型，尝试转换
                taskDetail = JSON.parseObject(JSON.toJSONString(detailObj));
            }
            
            if (taskDetail == null) {
                LogUtil.error(LogEnum.BIZ_DATASET, "无法获取任务参数，detailQueue: {}", detailQueue);
                return null;
            }
            
            // 从annotation key获取处理结果（Python保存的结果数据）
            Object annotationObj = redisUtils.get(dataKey);
            
            JSONObject resultData = null;
            if (annotationObj instanceof String) {
                // Redis返回的是String（JSON字符串），直接解析
                resultData = JSON.parseObject((String) annotationObj);
            } else if (annotationObj instanceof JSONObject) {
                // Redis返回的已经是JSONObject，直接使用
                resultData = (JSONObject) annotationObj;
            } else {
                // 其他类型，尝试转换
                resultData = JSON.parseObject(JSON.toJSONString(annotationObj));
            }
            
            if (resultData != null) {
                // 将结果数据放到object字段（VideoSampleQueueExecute.finishExecute需要）
                taskDetail.put("object", resultData);
            } else {
                // annotation key不存在，尝试从detail key中恢复object字段
                Object objectFromDetail = taskDetail.get("object");
                if (objectFromDetail == null) {
                    LogUtil.warn(LogEnum.BIZ_DATASET, "无法恢复object字段，annotation和detail中都没有数据");
                }
            }
            
            return taskDetail;
        }
        return null;

    }
}
