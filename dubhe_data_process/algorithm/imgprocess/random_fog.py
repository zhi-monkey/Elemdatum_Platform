import albumentations as A
import numpy as np
import json

def random_fog(img, enhance_params):
    try:
        if isinstance(enhance_params, str):
            enhance_params = json.loads(enhance_params)
        fog_coef = enhance_params.get('fog_coef', 0.1)
        alpha_coef = enhance_params.get('alpha_coef', 0.08)

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

        aug = A.RandomFog(
            fog_coef_lower=fog_coef,
            fog_coef_upper=fog_coef,
            alpha_coef=alpha_coef,
            always_apply=True,
        )
        result = aug(image=img)["image"]
        return result.astype(np.float32) / 255.0
    except Exception as e:
        raise

