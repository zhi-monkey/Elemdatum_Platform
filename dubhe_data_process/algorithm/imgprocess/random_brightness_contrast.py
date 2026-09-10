import albumentations as A
import numpy as np
import json
import cv2

def random_brightness_contrast(img, enhance_params):
    """
    随机调整图像的亮度和对比度
    """
    try:
        if isinstance(enhance_params, str):
            enhance_params = json.loads(enhance_params)

        brightness_limit = float(enhance_params.get('brightness_limit', 0.2))
        contrast_limit = float(enhance_params.get('contrast_limit', 0.2))

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

        img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        
        aug = A.RandomBrightnessContrast(
            brightness_limit=brightness_limit,
            contrast_limit=contrast_limit,
            always_apply=True,
        )
        result_rgb = aug(image=img_rgb)["image"]
        
        # 转回BGR格式
        result = cv2.cvtColor(result_rgb, cv2.COLOR_RGB2BGR)
        return result.astype(np.float32) / 255.0
    except Exception:
        raise
