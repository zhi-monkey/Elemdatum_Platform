import albumentations as A
import numpy as np
import json
import cv2

def to_gray(img, enhance_params):
    """
    转为灰度图
    可选择输出单通道或三通道灰度图
    """
    try:
        if isinstance(enhance_params, str):
            enhance_params = json.loads(enhance_params)

        keep_channels = bool(enhance_params.get('keep_channels', True))

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

        gray = cv2.cvtColor(img, cv2.COLOR_BGR2GRAY)
        
        if keep_channels:
            result = cv2.cvtColor(gray, cv2.COLOR_GRAY2BGR)
            return result.astype(np.float32) / 255.0
        else:
            return gray.astype(np.float32) / 255.0
    except Exception:
        raise
