import albumentations as A
import numpy as np
import json

def random_rain(img, enhance_params):
    try:
        if isinstance(enhance_params, str):
            enhance_params = json.loads(enhance_params)
        drop_length = int(enhance_params.get('drop_length', 10))
        blur_value = int(enhance_params.get('blur_value', 3))

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

        aug = A.RandomRain(
            rain_type='heavy',
            drop_length=drop_length,
            drop_width=1,
            blur_value=blur_value,
            brightness_coefficient=0.9,
            always_apply=True,
        )
        result = aug(image=img)["image"]
        return result.astype(np.float32) / 255.0
    except Exception:
        raise

