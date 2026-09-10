import albumentations as A
import numpy as np
import json
import cv2

def random_sunflare(img, enhance_params):
    """
    添加镜头眩光效果
    """
    try:
        if isinstance(enhance_params, str):
            enhance_params = json.loads(enhance_params)

        src_radius = int(enhance_params.get('src_radius', 100))
        flare_intensity = float(enhance_params.get('flare_intensity', 0.6))
        num_flare_circles = int(enhance_params.get('num_flare_circles', 8))

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

        h, w = img.shape[:2]

        src_x = np.random.randint(w // 4, 3 * w // 4)
        src_y = np.random.randint(0, h // 3)

        overlay = np.zeros((h, w, 3), dtype=np.float32)

        y_grid, x_grid = np.ogrid[:h, :w]
        dist = np.sqrt((x_grid - src_x)**2 + (y_grid - src_y)**2)

        glow = np.exp(-(dist**2) / (2 * (src_radius * 1.2)**2))
        glow = glow * flare_intensity * 200

        overlay[:, :, 0] += glow * 0.8
        overlay[:, :, 1] += glow * 0.9
        overlay[:, :, 2] += glow * 1.0

        num_rays = 6
        for i in range(num_rays):
            angle = 2 * np.pi * i / num_rays
            ray_length = min(h, w) * 0.6
            
            end_x = int(src_x + ray_length * np.cos(angle))
            end_y = int(src_y + ray_length * np.sin(angle))

            ray_mask = np.zeros((h, w), dtype=np.float32)
            cv2.line(ray_mask, (src_x, src_y), (end_x, end_y), 1.0, thickness=2)
            ray_mask = cv2.GaussianBlur(ray_mask, (15, 15), 0)
            ray_mask = ray_mask * flare_intensity * 60
            
            overlay[:, :, 0] += ray_mask
            overlay[:, :, 1] += ray_mask
            overlay[:, :, 2] += ray_mask

        target_x = w - src_x
        target_y = h - src_y
        
        for i in range(num_flare_circles):
            t = (i + 1) / (num_flare_circles + 1)
            cx = int(src_x + (target_x - src_x) * t)
            cy = int(src_y + (target_y - src_y) * t)
            radius = max(3, int(src_radius * 0.2 * (1 - t * 0.5)))

            hue = int((i * 30) % 180)
            color_hsv = np.uint8([[[hue, 180, 255]]])
            color_rgb = cv2.cvtColor(color_hsv, cv2.COLOR_HSV2RGB)[0, 0].astype(np.float32)
            
            circle_mask = np.zeros((h, w), dtype=np.float32)
            cv2.circle(circle_mask, (cx, cy), radius, 1.0, -1)
            circle_mask = cv2.GaussianBlur(circle_mask, (11, 11), 0)
            circle_mask = circle_mask * flare_intensity * 80
            
            overlay[:, :, 0] += circle_mask * (color_rgb[0] / 255.0)
            overlay[:, :, 1] += circle_mask * (color_rgb[1] / 255.0)
            overlay[:, :, 2] += circle_mask * (color_rgb[2] / 255.0)

        result = img.astype(np.float32)
        overlay = np.clip(overlay, 0, 255)
        result = np.clip(result + overlay, 0, 255)
        
        return result.astype(np.float32) / 255.0
    except Exception:
        raise

