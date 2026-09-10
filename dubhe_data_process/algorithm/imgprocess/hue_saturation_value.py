import albumentations as A
import numpy as np
import json
import cv2

def hue_saturation_value(img, enhance_params):
    """
    调整图像的色相、饱和度、亮度
    """
    try:
        if isinstance(enhance_params, str):
            enhance_params = json.loads(enhance_params)

        hue_shift_limit = int(enhance_params.get('hue_shift_limit', 20))
        sat_shift_limit = int(enhance_params.get('sat_shift_limit', 30))
        val_shift_limit = int(enhance_params.get('val_shift_limit', 20))

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

        # Albumentations假定RGB格式，需要从BGR转换
        img_rgb = cv2.cvtColor(img, cv2.COLOR_BGR2RGB)
        
        aug = A.HueSaturationValue(
            hue_shift_limit=hue_shift_limit,
            sat_shift_limit=sat_shift_limit,
            val_shift_limit=val_shift_limit,
            always_apply=True,
        )
        result_rgb = aug(image=img_rgb)["image"]
        result = cv2.cvtColor(result_rgb, cv2.COLOR_RGB2BGR)
        return result.astype(np.float32) / 255.0
    except Exception:
        raise
