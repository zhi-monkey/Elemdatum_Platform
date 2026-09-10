# !/usr/bin/env python
# -*- coding:utf-8 -*-

"""
Copyright 2020 Tianshu AI Platform. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
=============================================================
"""
import json
import os
import time
import logging
import traceback

import cv2

# 配置日志
logger = logging.getLogger(__name__)

datasetIdKey = ""


def execute(task):
    try:
        result = sampleProcess(task)
        return result
    except Exception as e:
        logger.error(f"[视频抽帧] 任务执行失败: {task.get('id', 'unknown')}, 错误: {str(e)}")
        logger.error(f"[视频抽帧] 异常堆栈: {traceback.format_exc()}")
        raise


def sampleProcess(taskParameters):
    """Video sampling method.
        Args:
            taskParameters: taskParameters.
    """
    global datasetIdKey, height, width
    
    path = taskParameters['path']
    frameList = taskParameters['frames']
    datasetId = taskParameters['datasetId']
    task_id = taskParameters['id']
    
    # 获取分辨率参数，默认为原分辨率（类型1）
    resolution_type = taskParameters.get('resolutionType', 1)
    custom_width = taskParameters.get('customWidth', None)
    custom_height = taskParameters.get('customHeight', None)
    
    logger.info(f"[视频抽帧] 开始任务 - ID: {task_id}, 视频: {path.split('/')[-1]}, 帧数: {len(frameList)}")
    
    datasetIdJson = {'datasetIdKey': datasetId}
    datasetIdKey = json.dumps(datasetIdJson, separators=(',', ':'))
    try:
        videoName = path.split('/')[-1]
        save_path = path.split('video')[0] + 'origin/'
        
        is_exists = os.path.exists(save_path)
        if not is_exists:
            os.makedirs(save_path)
        
        # 检查视频文件是否存在
        if not os.path.exists(path):
            logger.error(f"[视频抽帧] 视频文件不存在: {path}")
            raise FileNotFoundError(f"视频文件不存在: {path}")
        
        cap = cv2.VideoCapture(path)
        
        if not cap.isOpened():
            logger.error(f"[视频抽帧] 无法打开视频文件: {path}")
            raise Exception(f"无法打开视频文件: {path}")
        
        # 获取视频基本信息
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT))
        fps = cap.get(cv2.CAP_PROP_FPS)
        video_width = int(cap.get(cv2.CAP_PROP_FRAME_WIDTH))
        video_height = int(cap.get(cv2.CAP_PROP_FRAME_HEIGHT))
        
        pic_name_list = []
        finish_json = {}
        current_time = int(time.time())
        success_count = 0
        fail_count = 0
        
        for idx, i in enumerate(frameList):
            try:
                cap.set(cv2.CAP_PROP_POS_FRAMES, i)
                success, video_capture = cap.read()
                
                # 保存图片
                if success is True and video_capture is not None:
                    # 根据分辨率类型处理图像
                    if resolution_type == 2 and custom_width and custom_height:
                        # 自定义分辨率：使用cv2.resize调整图像大小
                        video_capture = cv2.resize(video_capture, (custom_width, custom_height), interpolation=cv2.INTER_LINEAR)
                    # 如果是类型1（原分辨率），则不做任何处理
                    
                    save_name = save_path + videoName.split('.')[0] + '_' + str(i) + '_' + str(current_time) + '.jpg'
                    cv2.imwrite(save_name, video_capture)
                    pic_name_list.append(save_name)
                    success_count += 1
                    
                    # 获取图像尺寸
                    [height, width, pixels] = video_capture.shape
                else:
                    fail_count += 1
            except Exception as e:
                fail_count += 1
                logger.error(f"[视频抽帧] 处理帧时出错，帧号: {i}, 错误: {str(e)}")
        # 释放视频资源
        cap.release()
        
        pic_name_list.reverse()
        finish_json['height'] = height
        finish_json['width'] = width
        finish_json['pictureNames'] = pic_name_list
        finish_json['datasetIdAndSub'] = datasetId
        finish_json['id'] = task_id
        
        logger.info(f"[视频抽帧] 任务完成 - ID: {task_id}, 成功: {success_count}/{len(frameList)}, 图片尺寸: {width}x{height}")
        
        return finish_json
    except Exception as e:
        logger.error(f"[视频抽帧] 任务处理失败 - 任务ID: {task_id}, 数据集ID: {datasetId}")
        logger.error(f"[视频抽帧] 错误信息: {str(e)}")
        logger.error(f"[视频抽帧] 异常堆栈: {traceback.format_exc()}")
        
        # 确保释放视频资源
        try:
            if 'cap' in locals():
                cap.release()
        except:
            pass
        
        failed_json = {'datasetIdAndSub': datasetId, 'id': task_id, 'error': str(e)}
        return failed_json
