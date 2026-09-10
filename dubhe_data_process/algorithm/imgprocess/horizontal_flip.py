import albumentations as A
import numpy as np
import json

def horizontal_flip(img, enhance_params, bboxes=None):
    """
    水平翻转图像，同时变换标注框
    
    Args:
        img: 输入图像
        enhance_params: 参数（本算子无参数）
        bboxes: 标注框列表，格式为 [[xmin, ymin, xmax, ymax, class_id], ...]
    
    Returns:
        如果有bboxes，返回 (transformed_img, transformed_bboxes)
        否则返回 transformed_img
    """
    try:
        if isinstance(enhance_params, str):
            enhance_params = json.loads(enhance_params)

        if img is None or not isinstance(img, np.ndarray):
            raise TypeError("img must be numpy.ndarray")
        if img.size == 0:
            raise ValueError("img is empty")
        if img.ndim != 3 or img.shape[2] != 3:
            raise ValueError(f"img must be HWC 3-channel, got {img.shape}")

        if img.max() <= 1.0:
            img = (img * 255).astype(np.uint8)
        elif img.dtype != np.uint8:
            img = img.astype(np.uint8)

        if bboxes is not None and len(bboxes) > 0:
            transform = A.Compose([
                A.HorizontalFlip(p=1.0)
            ], bbox_params=A.BboxParams(
                format='pascal_voc',
                label_fields=['class_labels']
            ))

            class_labels = [bbox[-1] for bbox in bboxes]
            bbox_coords = [bbox[:-1] for bbox in bboxes]

            transformed = transform(image=img, bboxes=bbox_coords, class_labels=class_labels)
            result_img = transformed['image']

            result_bboxes = [[*bbox, label] for bbox, label in zip(transformed['bboxes'], transformed['class_labels'])]
            
            return result_img.astype(np.float32) / 255.0, result_bboxes
        else:
            transform = A.HorizontalFlip(p=1.0)
            result = transform(image=img)['image']
            return result.astype(np.float32) / 255.0
            
    except Exception:
        raise
