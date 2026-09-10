import albumentations as A
import numpy as np
import json

def motion_blur(img, enhance_params):
    """
    运动模糊
    """
    try:
        if isinstance(enhance_params, str):
            enhance_params = json.loads(enhance_params)

        blur_limit = enhance_params.get('blur_limit', (3, 7))
        
        if isinstance(blur_limit, int):
            if blur_limit % 2 == 0:
                blur_limit += 1
            blur_limit = (blur_limit, blur_limit)

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

        aug = A.MotionBlur(
            blur_limit=blur_limit,
            always_apply=True,
        )
        result = aug(image=img)["image"]
        return result.astype(np.float32) / 255.0
    except Exception:
        raise
