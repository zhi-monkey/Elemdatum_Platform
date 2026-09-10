import json

import cv2
import numpy as np

def sharpen(img, enhance_params):
    """
    简单锐化（Unsharp Mask），支持可调strength和kernel_size。

    参数:
        img: numpy数组，支持0~255或0~1，灰度或RGB
        enhance_params: dict，包含
            - 'sharpen_strength': float, 默认1.0
            - 'sharpen_kernel_size': int, 默认3，必须为奇数
    返回:
        sharpened_img: numpy数组，0~1浮点型
    """
    try:
        # 如果传进来是 str，自动转成 dict
        if isinstance(enhance_params, str):
            enhance_params = json.loads(enhance_params)
        strength = enhance_params.get('sharpen_strength', 1.0)
        kernel_size = enhance_params.get('sharpen_kernel_size', 3)
        print(f"[INFO] sharpen_strength={strength}, sharpen_kernel_size={kernel_size}")

        # 检查 kernel_size
        if not isinstance(kernel_size, int) or kernel_size < 3 or kernel_size % 2 == 0:
            raise ValueError(f"[sharpen ERROR] kernel_size must be odd and >=3, got {kernel_size}")

        # 检查 img
        if img is None or not isinstance(img, np.ndarray):
            raise TypeError("[sharpen ERROR] img must be a valid numpy.ndarray")
        if img.size == 0:
            raise ValueError("[sharpen ERROR] img is empty.")
        if img.ndim not in [2, 3]:
            raise ValueError(f"[sharpen ERROR] img shape not valid: {img.shape}")

        # 转换到0~255 uint8
        if img.max() <= 1.0:
            img_uint8 = (img * 255).astype(np.uint8)
        else:
            img_uint8 = img.astype(np.uint8)

        blurred = cv2.GaussianBlur(img_uint8, (kernel_size, kernel_size), 0)
        sharpened = cv2.addWeighted(img_uint8, 1 + strength, blurred, -strength, 0)

        result = sharpened.astype(np.float32) / 255.0
        return result
    except Exception as e:
        print(f"[sharpen ERROR] {str(e)}")
        # 可选：raise 让外层继续处理
        raise
