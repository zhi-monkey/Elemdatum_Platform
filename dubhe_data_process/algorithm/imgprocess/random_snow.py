import albumentations as A
import numpy as np
import json
import cv2

def random_snow(img, enhance_params):
    """
    添加雪花效果（而非积雪覆盖效果）
    通过在图像上叠加白色噪点来模拟飘落的雪花
    优化版：使用向量化操作提升性能
    """
    try:
        if isinstance(enhance_params, str):
            enhance_params = json.loads(enhance_params)

        snow_density = float(enhance_params.get('snow_density', 0.01))
        snow_size_min = int(enhance_params.get('snow_size_min', 1))
        snow_size_max = int(enhance_params.get('snow_size_max', 3))
        snow_alpha = float(enhance_params.get('snow_alpha', 0.8))

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

        result = img.copy().astype(np.float32)
        h, w = result.shape[:2]

        num_snowflakes = int(h * w * snow_density)
        
        if num_snowflakes > 0:
            x_coords = np.random.randint(0, w, num_snowflakes)
            y_coords = np.random.randint(0, h, num_snowflakes)
            sizes = np.random.randint(snow_size_min, snow_size_max + 1, num_snowflakes)

            snow_layer = np.zeros((h, w), dtype=np.float32)

            for x, y, size in zip(x_coords, y_coords, sizes):
                cv2.circle(snow_layer, (x, y), size, 1.0, -1)

            snow_layer = np.stack([snow_layer] * 3, axis=-1)

            result = result * (1 - snow_alpha * snow_layer) + 255 * snow_alpha * snow_layer

        
        return (result / 255.0).astype(np.float32)
    except Exception:
        raise

