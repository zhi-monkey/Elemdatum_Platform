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

import cv2
import logging
import numpy as np
import os
import shutil
import time


from ACE import ACE_color
from contrast_enhance import contrast_enhance
from dehaze import deHaze, addHaze
from hist_equalize import adaptive_hist_equalize
from sharpen import sharpen
from gauss_noise import gauss_noise
from random_fog import random_fog
from random_rain import random_rain
from random_snow import random_snow
from random_shadow import random_shadow
from random_sunflare import random_sunflare
from to_gray import to_gray
from hue_saturation_value import hue_saturation_value
from random_brightness_contrast import random_brightness_contrast
from gaussian_blur import gaussian_blur
from motion_blur import motion_blur
from horizontal_flip import horizontal_flip
from vertical_flip import vertical_flip
from concurrent.futures import ProcessPoolExecutor, as_completed


def read_annotation(ann_path):
    """读取平台统一格式的标注文件（JSON数组）"""
    try:
        with open(ann_path, 'r', encoding='utf-8') as f:
            return json.load(f)
    except Exception as e:
        logging.warning(f"Failed to read annotation {ann_path}: {e}")
        return []


def write_annotation(ann_path, annotations):
    """保存平台统一格式的标注文件（JSON数组）"""
    try:
        with open(ann_path, 'w', encoding='utf-8') as f:
            json.dump(annotations, f, ensure_ascii=False)
    except Exception as e:
        logging.error(f"Failed to write annotation {ann_path}: {e}")


def transform_bbox_horizontal(bbox, img_width):
    """水平翻转bbox坐标
    bbox格式: [x, y, width, height]
    """
    x, y, w, h = bbox
    new_x = img_width - x - w
    return [new_x, y, w, h]


def transform_bbox_vertical(bbox, img_height):
    """垂直翻转bbox坐标
    bbox格式: [x, y, width, height]
    """
    x, y, w, h = bbox
    new_y = img_height - y - h
    return [x, new_y, w, h]


def transform_category_id(annotations, label_mapping):
    """将标注中的category_id从标签名称转换为标签ID
    
    Args:
        annotations: 标注数组，每个元素可能包含category_id字段
        label_mapping: 标签映射字典，格式为 {标签名称: 标签ID}
    
    Returns:
        转换后的标注数组
    """
    for ann in annotations:
        if 'category_id' in ann:
            category_id = ann['category_id']
            # 如果category_id是字符串（标签名称），转换为ID
            if isinstance(category_id, str):
                if category_id in label_mapping:
                    ann['category_id'] = label_mapping[category_id]
                    logging.debug(f"Converted category_id from '{category_id}' to {label_mapping[category_id]}")
                else:
                    logging.warning(f"Label name '{category_id}' not found in label mapping")
    
    return annotations


def transform_annotations(annotations, img_width, img_height, flip_type):
    """变换标注数组中的所有bbox
    
    Args:
        annotations: 标注数组，每个元素包含bbox字段
        img_width: 图像宽度
        img_height: 图像高度
        flip_type: 'horizontal' 或 'vertical'
    
    Returns:
        变换后的标注数组
    """
    if not annotations:
        return annotations
    
    transformed = []
    for ann in annotations:
        new_ann = ann.copy()
        if 'bbox' in ann and ann['bbox']:
            bbox = ann['bbox']
            if flip_type == 'horizontal':
                new_ann['bbox'] = transform_bbox_horizontal(bbox, img_width)
            elif flip_type == 'vertical':
                new_ann['bbox'] = transform_bbox_vertical(bbox, img_height)
        transformed.append(new_ann)
    
    return transformed


def execute(task):
    return start_enhance_task(task)


def start_enhance_task(taskParameters):
    logging.debug(taskParameters)
    """
    Enhance task method.
    """
    dataset_id = taskParameters['id']
    img_save_path = taskParameters['enhanceFilePath']
    ann_save_path = taskParameters["enhanceAnnotationPath"]
    file_list = taskParameters['fileDtos']
    nums_, img_path_list, ann_path_list = img_ann_list_gen(file_list)

    types = taskParameters.get('types')
    re_task_id = taskParameters['reTaskId']
    tp_raw = taskParameters.get('taskParams', '{}')
    try:
        params_obj = json.loads(tp_raw) if isinstance(tp_raw, str) else (tp_raw or {})
    except Exception:
        params_obj = {}
    if not isinstance(params_obj, dict):
        params_obj = {}
    if 'enhanceMode' not in params_obj:
        params_obj['enhanceMode'] = 'parallel'
    task_params = params_obj
    enhance_mode = params_obj['enhanceMode']

    img_process_config = [
        dataset_id,
        img_save_path,
        ann_save_path,
        img_path_list,
        ann_path_list,
        types,
        re_task_id,
        task_params,
        enhance_mode
    ]

    logging.debug(img_process_config)
    logging.debug(str(nums_) + ' images for augment')
    return image_enhance_process(img_process_config)



def img_ann_list_gen(file_list):
    """Analyze the json request and convert to list"""
    nums_ = len(file_list)
    img_list = []
    ann_list = []
    for i in range(nums_):
        img_list.append(file_list[i]['filePath'])
        ann_list.append(file_list[i]['annotationPath'])
    return nums_, img_list, ann_list


def image_enhance_process(img_task):
    global finish_key, re_task_id
    logging.info('img_process server start'.center(66, '-'))
    
    try:
        dataset_id = img_task[0]
        img_save_path = img_task[1]
        ann_save_path = img_task[2]
        img_list = img_task[3]
        ann_list = img_task[4]
        types = img_task[5]  # list of ints
        re_task_id = img_task[6]
        task_params = img_task[7]
        enhance_mode = img_task[8]
        suffix = '_en_' + re_task_id
        finish_key = {"processKey": re_task_id}
        finish_data = {"id": re_task_id, "suffix": suffix}

        
        try:
            cpu_cnt = int((os.cpu_count() or 2))
        except Exception:
            cpu_cnt = 2
        budget = max(1, cpu_cnt // 2)
        base_workers = max(1, budget)
        workers = max(1, min(base_workers, len(ann_list)))
        logging.info(json.dumps({"mode": enhance_mode, "workers": workers, "images": len(ann_list)}))
        if enhance_mode == 'serial':
            with ProcessPoolExecutor(max_workers=workers) as executor:
                futures = []
                for j in range(len(ann_list)):
                    futures.append(executor.submit(serial_img_process,
                        img_list[j], ann_list[j], img_save_path, ann_save_path, types, suffix, task_params))
                for f in as_completed(futures):
                    try:
                        _ = f.result()
                    except Exception:
                        pass
        else:
            if not types:
                logging.error("Parallel mode requires non-empty 'types'; task aborted")
                raise ValueError("Empty types in parallel mode")
            parts = partition_images_for_types(img_list, ann_list, types)
            with ProcessPoolExecutor(max_workers=workers) as executor:
                futures = []
                for t, pairs in parts.items():
                    for (ip, ap) in pairs:
                        futures.append(executor.submit(img_process, suffix, ip, ap, img_save_path, ann_save_path, t, task_params))
                for f in as_completed(futures):
                    try:
                        _ = f.result()
                    except Exception:
                        pass

        logging.info("End img_process of dataset:" + str(dataset_id))
        total_images = len(ann_list)
        logging.info(json.dumps({
            "datasetId": dataset_id,
            "reTaskId": re_task_id,
            "images": total_images
        }))
        time.sleep(0.01)
        return {**finish_data, "images": total_images}

    except Exception as e:
        logging.error("Error imgProcess")
        logging.error(e)
        return {"id": re_task_id, "suffix": "_error"}

def serial_img_process(img_path, ann_path, img_save_path, ann_save_path, type_list, suffix, task_params):
    """Apply multiple enhancements in sequence on one image"""
    img_raw = cv2.imdecode(np.fromfile(img_path.encode('utf-8'), dtype=np.uint8), 1)
    img_height, img_width = img_raw.shape[:2]
    img = img_raw.copy()
    
    # 获取标签映射表
    label_mapping = task_params.get('labelMapping', {}) if isinstance(task_params, dict) else {}
    
    # 判断是否有翻转操作
    has_flip = any(t in [18, 19] for t in type_list)
    annotations = None
    
    # 如果有翻转操作，读取标注
    if has_flip:
        annotations = read_annotation(ann_path)
    
    # 应用增强操作
    for type_ind in type_list:
        img = apply_enhance_once(img, type_ind, task_params)
        
        # 如果是翻转操作且有标注，同时变换标注坐标
        if annotations is not None:
            if type_ind == 18:  # 水平翻转
                annotations = transform_annotations(annotations, img_width, img_height, 'horizontal')
            elif type_ind == 19:  # 垂直翻转
                annotations = transform_annotations(annotations, img_width, img_height, 'vertical')

    # 保存图像
    img_suffix = os.path.splitext(img_path)[-1]
    ann_name = os.path.basename(ann_path)
    base_img_name = os.path.splitext(os.path.basename(img_path))[0]
    if img.max() <= 1.0:
        img = (img * 255).astype(np.uint8)
    else:
        img = img.astype(np.uint8)
    cv2.imwrite(os.path.join(img_save_path, ann_name + suffix + img_suffix), img)
    
    # 保存标注
    new_ann_path = os.path.join(ann_save_path, base_img_name + suffix)
    if has_flip and annotations is not None:
        # 翻转操作：转换category_id并保存变换后的标注
        annotations = transform_category_id(annotations, label_mapping)
        write_annotation(new_ann_path, annotations)
    else:
        # 其他操作：读取原标注，转换category_id，然后写入
        annotations = read_annotation(ann_path)
        annotations = transform_category_id(annotations, label_mapping)
        write_annotation(new_ann_path, annotations)
    
    return None



inds2method = {
        1: deHaze,
        2: addHaze,
        3: ACE_color,
        4: adaptive_hist_equalize,
        5: sharpen,
        6: contrast_enhance,
        7: gauss_noise,
        8: random_fog,
        9: random_rain,
        10: random_snow,
        11: random_shadow,
        12: random_sunflare,
        13: to_gray,
        14: hue_saturation_value,
        15: random_brightness_contrast,
        16: gaussian_blur,
        17: motion_blur,
        18: horizontal_flip,
        19: vertical_flip,
    }

def apply_enhance_once(img, method_ind, task_params):

    method = inds2method.get(method_ind)
    if method is None:
        raise ValueError(f"Unknown method_ind: {method_ind}")

    if method_ind in [5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19]:
        result = method(img, task_params)
    elif method_ind in [1, 2, 3]:
        img_normalized = (img / 255.0).astype(np.float32)
        result = method(img_normalized) * 255
    elif method_ind == 4:
        # Ensure uint8 for cv2.cvtColor operations
        if img.dtype != np.uint8:
            if img.max() <= 1.0:
                img = (img * 255).astype(np.uint8)
            else:
                img = img.astype(np.uint8)
        result = method(img)
    else:
        result = img
    
    # Ensure consistent output format (uint8 for further processing)
    if result.dtype not in [np.uint8]:
        if result.max() <= 1.0:
            result = (result * 255).astype(np.uint8)
        else:
            result = np.clip(result, 0, 255).astype(np.uint8)
    
    return result



def img_process(suffix, img_path, ann_path, img_save_path, ann_save_path, method_ind, task_params):
    """Process images and save in specified path (parallel mode)"""
    img_raw = cv2.imdecode(np.fromfile(img_path.encode('utf-8'), dtype=np.uint8), 1)
    img_height, img_width = img_raw.shape[:2]
    img_suffix = os.path.splitext(img_path)[-1]
    ann_name = os.path.basename(ann_path)
    base_img_name = os.path.splitext(os.path.basename(img_path))[0]
    
    # 获取标签映射表
    label_mapping = task_params.get('labelMapping', {}) if isinstance(task_params, dict) else {}
    
    # 判断是否为翻转操作
    is_flip = method_ind in [18, 19]
    annotations = None
    
    # 如果是翻转操作，读取标注
    if is_flip:
        annotations = read_annotation(ann_path)
    
    # 应用增强操作
    processed_img = apply_enhance_once(img_raw, method_ind, task_params)
    
    # 如果是翻转操作且有标注，变换标注坐标
    if is_flip and annotations is not None:
        if method_ind == 18:  # 水平翻转
            annotations = transform_annotations(annotations, img_width, img_height, 'horizontal')
        elif method_ind == 19:  # 垂直翻转
            annotations = transform_annotations(annotations, img_width, img_height, 'vertical')
    
    # 保存图像
    if processed_img.max() <= 1.0:
        save_img = (processed_img * 255).astype(np.uint8)
    else:
        save_img = np.clip(processed_img, 0, 255).astype(np.uint8)
    cv2.imwrite(img_save_path + "/" + ann_name + suffix + img_suffix, save_img)
    
    # 保存标注
    new_ann_path = ann_save_path + "/" + base_img_name + suffix
    if is_flip and annotations is not None:
        # 翻转操作：转换category_id并保存变换后的标注
        annotations = transform_category_id(annotations, label_mapping)
        write_annotation(new_ann_path, annotations)
    else:
        # 其他操作：读取原标注，转换category_id，然后写入
        annotations = read_annotation(ann_path)
        annotations = transform_category_id(annotations, label_mapping)
        write_annotation(new_ann_path, annotations)
    
    return None

def partition_images_for_types(img_list, ann_list, types):
    parts = {t: [] for t in types}
    m = len(types)
    if m <= 0:
        return {}
    for i in range(len(img_list)):
        t = types[i % m]
        parts[t].append((img_list[i], ann_list[i]))
    return parts
