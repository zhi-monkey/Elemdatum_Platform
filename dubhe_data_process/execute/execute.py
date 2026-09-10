#!/usr/bin/env python
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

import importlib
import os
import sys
import json
import time
import traceback
import cv2
import common.util.public.logger_util as logger_util
logger = logger_util.get_logger("algorithm")
current_dir = os.path.dirname(os.path.abspath(__file__))

redis_module_name="common.util.public.RedisUtil"
gpu_model_name="common.util.public.select_gpu"
lua_script_module_name="execute.lua_script"
infernce_model_name="algorithm.%s.inference"
algorithm_parent_dir = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), ".."))
def start(algorithm, gpu, redis_config):
    """
        启动程序，启动步骤
        1.判断是否有gpu，如果有，则需要选择GPU卡槽(为了解决多卡槽时，一个卡槽用完导致无法使用其他卡槽的问题)
        2.调用模型初始化方法
        3.初始化redis客户端
        4.获取任务
        5.调用推理接口
        6.保存结果
    """
    sys.path.insert(0, os.path.abspath(os.path.join(current_dir, ".." + os.sep + "algorithm" + os.sep + algorithm)))
    os.environ.setdefault("OMP_NUM_THREADS", "1")
    os.environ.setdefault("OPENBLAS_NUM_THREADS", "1")
    os.environ.setdefault("MKL_NUM_THREADS", "1")
    try:
        cv2.setNumThreads(1)
    except Exception:
        pass
    logger.debug("service start123")
    if gpu:
        logger.debug("switch GPU")
        select_gpu()
    init_model(algorithm)
    #logger.debug("start main loop")
    # start_up()
    loop_count = 1
    sticky_namespace = None
    sticky_empty_count = 0
    _retask_agg = {}
    while True:
        try:
            redis_client = None
            task = None
            redis_client = get_redis_client(redis_config)
            tasks_batch = []
            max_batch = 16
            
            for _ in range(max_batch):
                ns = sticky_namespace if sticky_namespace else "dataset:" + algorithm
                t = get_one_task(redis_client, ns)
                if t is None:
                    if sticky_namespace:
                        sticky_empty_count += 1
                        if sticky_empty_count >= 2:
                            logger.info(f"[任务调度] 命名空间 {sticky_namespace} 连续2次为空，切换到默认命名空间")
                            sticky_namespace = None
                            sticky_empty_count = 0
                    break
                task_str = t.decode(encoding="utf-8").replace("\"", "")
                tasks_batch.append(task_str)
            if len(tasks_batch) > 0:
                logger.info(f"[任务调度] 本批次获取到 {len(tasks_batch)} 个任务")
                
                try:
                    parts = tasks_batch[0].split(":")
                    sticky_namespace = f"{parts[0]}:{parts[1]}:{parts[2]}"
                    sticky_empty_count = 0
                except Exception as e:
                    logger.warning(f"[任务调度] 解析命名空间失败: {str(e)}")
                    sticky_namespace = None
                    sticky_empty_count = 0
                    
                import os as _os
                results = {}
                _retids_updated = set()
                saved_success = 0
                saved_error = 0
                
                for idx, tk in enumerate(tasks_batch):
                    try:
                        raw = get_task_detail(redis_client, tk)
                        payload_str = raw.decode(encoding="utf-8") if raw is not None else None
                        
                        if payload_str is None:
                            logger.error(f"[任务处理] 任务详情为空: {tk}")
                            raise Exception("detail_absent")
                        
                        res = compute_inference(algorithm, payload_str)
                    except Exception as e:
                        logger.error(f"[任务处理] 任务处理失败: {tk}, 错误: {str(e)}")
                        logger.error(f"[任务处理] 异常堆栈: {traceback.format_exc()}")
                        res = {"id": tk.split(":")[-1], "suffix": "_error", "error_stage": "detail_or_inference", "error": str(e)}
                    results[tk] = res
                    
                    save_result(redis_client, tk, json.dumps(res))
                    
                    if res.get("suffix") == "_error":
                        saved_error += 1
                    else:
                        saved_success += 1
                    _rid = res.get("id")
                    if _rid is not None:
                        agg = _retask_agg.get(_rid) or {"images": 0}
                        agg["images"] += int(res.get("images", 0) or 0)
                        _retask_agg[_rid] = agg
                        _retids_updated.add(_rid)
                logger.info(f"[任务调度] 批次处理完成 - 成功: {saved_success}, 失败: {saved_error}, 总计: {len(tasks_batch)}")
                
                for _rid in _retids_updated:
                    agg = _retask_agg.get(_rid) or {}
                    logger.info(f"[任务统计] 算法: {algorithm}, 任务ID: {_rid}, 累计图片数: {agg.get('images', 0)}")
            else:
                time.sleep(3)
            loop_count = loop_count + 1
        except Exception as e:
            logger.error(f"[任务调度] 主循环异常: {str(e)}")
            logger.error(f"[任务调度] 异常堆栈: {traceback.format_exc()}")
            traceback.print_exc()       
            try:
                if redis_client is not None and task is not None:
                    logger.error(f"[任务调度] 尝试保存失败任务结果: {task}")
                    parts = task.split(":")
                    fail_id = parts[-1] if len(parts) > 0 else ""
                    fail_result = json.dumps({"id": fail_id, "suffix": "_error"})
                    save_result(redis_client, task, fail_result)
                    logger.info(f"[任务调度] 失败任务结果已保存: {task}")
            except Exception as save_error:
                logger.error(f"[任务调度] 保存失败任务结果时出错: {str(save_error)}")
                pass       

def select_gpu():
    """
        选择GPU处理
    """
    logger.debug("Service switch GPU card slot start")
    module = importlib.import_module(gpu_model_name)
    module.select_gpu()
    logger.debug("Service switch GPU card slot complete")


def init_model(algorithm):
    """
        模型初始化操作
    """
    logger.debug("Model initialization operation start")
    module = importlib.import_module(infernce_model_name % algorithm)
    module.load()
    logger.debug("Model initialization operation complete")


def get_redis_client(redis_config):
    """
        获取redis客户端
    """
    #logger.debug("get redis client")
    redis_config = redis_config.split(",")
    module = importlib.import_module(redis_module_name)
    return module.getRedisConnection(redis_config[0], redis_config[1], redis_config[2], redis_config[3])


def get_one_task(redis_client, namespace):
    """
        获取一个待处理任务
    """
    try:
        module = importlib.import_module(lua_script_module_name)
        script = redis_client.register_script(module.getTaskLua)
        result = script(keys=[namespace])
        if result:
            return result
    except Exception as e:
        logger.error(f"[Redis] 获取任务失败，命名空间: {namespace}, 错误: {str(e)}")
        raise


def infernce(task, algorithm):
    """
        调用推理接口
    """
    logger.debug("Call the inference interface for inference")
    module = importlib.import_module(infernce_model_name % algorithm)
    logger.debug(task)
    return module.inference(task)


def save_result(redis_client, task, result):
    """
        保存推理结果
    """
    try:
        module = importlib.import_module(lua_script_module_name)
        script = redis_client.register_script(module.saveTaskLua)
        ret = script(keys=[task, result])
        return ret
    except Exception as e:
        logger.error(f"[Redis] 保存结果失败: {task}, 错误: {str(e)}")
        raise


def compute_inference(algorithm, task_detail_str, outer_workers=None):
    import importlib as _imp
    module = _imp.import_module(infernce_model_name % algorithm)
    payload = json.loads(task_detail_str)
    return module.inference(payload)


def get_task_detail(redis_client, task):
    """
        获取任务详情
    """
    try:
        detail = redis_client.get(task)
        if not detail:
            logger.warning(f"[Redis] 任务详情为空: {task}")
        return detail
    except Exception as e:
        logger.error(f"[Redis] 获取任务详情失败: {task}, 错误: {str(e)}")
        raise


def start_up():
    """
        启动成功(生成启动成功文件)
    """
    start_up_file = "/tmp/.startup"
    if not os.path.exists(start_up_file):
        os.mknod(start_up_file)
    