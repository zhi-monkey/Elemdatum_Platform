/**
 * ============================================================
 *  pcAnnotationFormats.ts —— 点云标注：对象存储模型 + 多格式导出
 * ============================================================
 *  设计说明（调研结论）：
 *    平台内部标注以「自己设计的 JSON」为存储标签，面向自动驾驶主流的
 *    KITTI 与 nuScenes 在导出时做格式转换。
 *
 *    统一对象模型（存储单元，3D 检测框）：
 *      {
 *        "id": "car_01",
 *        "category": "car",
 *        "center": [12.3, -4.5, 0.9],      // 世界坐标（米）
 *        "size": [4.5, 1.9, 1.6],          // [长, 宽, 高]（米）
 *        "yaw": 1.57,                      // 绕 Z 轴旋转角（弧度）
 *        "num_points": 356                 // 框内点云数
 *      }
 *
 *  平台 JSON 同时保留「点级分割对象」（segments：点索引集合 + 类别 + 颜色），
 *  KITTI / nuScenes 为 3D 框格式，不消费 segments。
 *
 *  坐标系约定（与自动驾驶主流一致）：
 *    - 平台内部（等同 nuScenes 全局系）：X 前 / Y 左 / Z 上，yaw 绕 Z 轴正方向（俯视逆时针）
 *    - KITTI 相机系：X 右 / Y 下 / Z 前，rotation_y 绕相机 Y 轴
 *    - 导出 KITTI 时使用标准 Velodyne→Camera 变换：
 *        x_cam = -y_lidar, y_cam = -z_lidar, z_cam = x_lidar
 * ============================================================
 */

/** ==================== 统一存储模型 ==================== */

/** 标注对象（存储单元）—— 平台内部 JSON 与各格式导出的中间模型 */
export interface PcAnnotationObject {
  /** 对象唯一 ID */
  id: string;
  /** 类别（导出名，如 car / pedestrian，与前端标签名映射） */
  category: string;
  /** 中心点（世界坐标，米）：[x, y, z] */
  center: [number, number, number];
  /** 尺寸（米）：[长(length), 宽(width), 高(height)] */
  size: [number, number, number];
  /** 绕 Z 轴旋转角（弧度），俯视逆时针为正 */
  yaw: number;
  /** 框内点云数 */
  num_points: number;
}

/** 平台内部存储 JSON（一帧的标注结果） */
export interface PlatformAnnotationJson {
  /** 平台格式版本号 */
  format: 'elemdatum-pc-annotation';
  /** 格式版本 */
  version: number;
  /** 帧标识（PCD 文件名或帧序号） */
  frame_id: string | number;
  /** 数据文件信息（可选元信息） */
  meta?: {
    datasetId?: number | string;
    fileName?: string;
    pointCount?: number;
    exportedAt?: string;
  };
  /** 该帧全部标注对象 */
  objects: PcAnnotationObject[];
  /** 点级分割对象（点云语义分割结果，仅平台内部存储） */
  segments?: PcSegmentationObject[];
}

/** 点级分割对象（仅平台内部 JSON 存储，导出 KITTI/nuScenes 时忽略） */
export interface PcSegmentationObject {
  /** 分割对象唯一 ID */
  id: string;
  /** 类别 */
  category: string;
  /** 显示颜色（#rrggbb） */
  color?: string;
  /** 点云点数 */
  num_points: number;
  /** 原始点索引集合（上限保护，超大数据降采样） */
  indices: number[];
}

/** ==================== 导出源（组件收集的原始标注） ==================== */

/** 供导出函数使用的源对象（由组件从 BoxAnnotation 收集，几何单位为米、角度为度） */
export interface ExportAnnotationSource {
  /** 帧标识 */
  frameId: string | number;
  /** 数据文件信息 */
  meta?: {
    datasetId?: number | string;
    fileName?: string;
    pointCount?: number;
    exportedAt?: string;
  };
  /** 原始对象列表 */
  objects: {
    id: string;
    /** 类别名（前端标签名，如「车辆」；导出时经 categoryMap 映射为英文） */
    label: string;
    center: [number, number, number];
    /** [长, 宽, 高]（米） */
    size: [number, number, number];
    /** 绕 Z 轴旋转角（度） */
    yawDeg: number;
    /** 框内点云数 */
    numPoints: number;
  }[];
  /** 点级分割对象（仅平台 JSON 导出包含；KITTI/nuScenes 忽略） */
  segments?: {
    id: string;
    label: string;
    color?: string;
    numPoints: number;
    indices: number[];
  }[];
  /** 类别导出名映射（前端中文标签 → KITTI/nuScenes 英文类别，如 车辆 → Car）。
   *  未提供时使用内置默认映射，命中不到则原样保留标签。 */
  categoryMap?: Record<string, string>;
}

/** ==================== 类别导出名映射 ==================== */

/** 内置默认映射：平台常见中文标签 → KITTI/nuScenes 类别名 */
export const DEFAULT_CATEGORY_MAP: Record<string, string> = {
  车辆: 'Car',
  汽车: 'Car',
  轿车: 'Car',
  卡车: 'Truck',
  货车: 'Truck',
  厢式货车: 'Van',
  面包车: 'Van',
  客车: 'Bus',
  行人: 'Pedestrian',
  人: 'Pedestrian',
  骑行者: 'Cyclist',
  自行车: 'Cyclist',
  摩托车: 'Motorcyclist',
  三轮车: 'Tricycle',
  障碍物: 'Obstacle',
  其他: 'Other',
  其它: 'Other',
};

/** 解析导出类别名：优先 categoryMap，其次内置默认映射，最后原样返回 */
export function resolveExportCategory(label: string, categoryMap?: Record<string, string>): string {
  if (categoryMap && categoryMap[label] !== undefined) return categoryMap[label];
  if (DEFAULT_CATEGORY_MAP[label] !== undefined) return DEFAULT_CATEGORY_MAP[label];
  return label;
}

/** ==================== 工具函数 ==================== */

/** 角度 → 弧度 */
export function degToRad(deg: number): number {
  return (deg * Math.PI) / 180;
}

/** 弧度 → 角度 */
export function radToDeg(rad: number): number {
  return (rad * 180) / Math.PI;
}

/**
 * 生成对象 ID（组件内为前端唯一 id；导出时如无自定义 id 可用此生成：
 * 形如 car_01 / pedestrian_02，按类别累加）
 */
export function buildObjectId(category: string, index: number): string {
  const cat = category.toLowerCase().replace(/[^a-z0-9]/gi, '');
  return `${cat || 'obj'}_${String(index + 1).padStart(2, '0')}`;
}

/** 数字格式化：保留指定位小数，去掉多余尾零 */
function fmt(value: number, digits = 6): string {
  if (!Number.isFinite(value)) return '0';
  const rounded = Number(value.toFixed(digits));
  return String(rounded);
}

/** 统一构建存储对象列表（度 → 弧度，供平台 JSON 使用） */
function toObjects(source: ExportAnnotationSource): PcAnnotationObject[] {
  return source.objects.map((obj, index) => ({
    id: obj.id || buildObjectId(resolveExportCategory(obj.label, source.categoryMap), index),
    category: resolveExportCategory(obj.label, source.categoryMap),
    center: [obj.center[0], obj.center[1], obj.center[2]] as [number, number, number],
    size: [obj.size[0], obj.size[1], obj.size[2]] as [number, number, number],
    yaw: degToRad(obj.yawDeg),
    num_points: obj.numPoints || 0,
  }));
}

/** ==================== 1. 平台内部 JSON ==================== */

/** 平台内部存储 JSON（与用户约定样例同构） */
export function toPlatformJson(source: ExportAnnotationSource): PlatformAnnotationJson {
  const json: PlatformAnnotationJson = {
    format: 'elemdatum-pc-annotation',
    version: 2,
    frame_id: source.frameId,
    ...(source.meta ? { meta: source.meta } : {}),
    objects: toObjects(source),
  };
  // 点级分割对象（如有）随平台 JSON 存储
  if (source.segments && source.segments.length > 0) {
    json.segments = source.segments.map((seg) => {
      const indices = seg.indices.slice(0, 100000); // 点索引上限保护（超大数据降采样导出提示）
      return {
        id: seg.id,
        category: resolveExportCategory(seg.label, source.categoryMap),
        ...(seg.color ? { color: seg.color } : {}),
        num_points: indices.length, // 与实际导出的 indices 保持一致（回读重建不错位）
        indices,
      };
    });
  }
  return json;
}

/** 序列化平台 JSON 为字符串 */
export function stringifyPlatformJson(source: ExportAnnotationSource): string {
  return JSON.stringify(toPlatformJson(source), null, 2);
}

/** ==================== 2. KITTI 格式 ==================== */

/**
 * 导出 KITTI label 文本（每行一个对象）：
 *   type truncated occluded alpha bbox_left bbox_top bbox_right bbox_bottom height width length pos_x pos_y pos_z rotation_y
 * 字段说明：
 *   - truncated / occluded / alpha / 2D bbox：平台为点云标注，无相机 2D 投影，统一填 -1（表示缺失/不可用）
 *   - height/width/length：物体尺寸（相机系，单位米）
 *   - pos_x/pos_y/pos_z：3D 框中心（相机系）
 *   - rotation_y：绕相机 Y 轴旋转（弧度）
 * 坐标系：使用标准 Velodyne→Camera 变换（x_cam=-y_lidar, y_cam=-z_lidar, z_cam=x_lidar），
 *   因此 pos_x=-obj.y, pos_y=-obj.z, pos_z=obj.x；rotation_y ≈ -yaw。
 */
export function toKittiText(source: ExportAnnotationSource): string {
  const lines: string[] = [];
  source.objects.forEach((obj) => {
    // 平台尺寸 [长, 宽, 高] → KITTI 相机系尺寸 [高, 宽, 长]
    const length = obj.size[0];
    const width = obj.size[1];
    const height = obj.size[2];
    const [x, y, z] = obj.center;
    // 相机系中心
    const posX = -y;
    const posY = -z;
    const posZ = x;
    const rotationY = -degToRad(obj.yawDeg);
    const type = resolveExportCategory(obj.label, source.categoryMap) || 'Unknown';
    lines.push(
      [
        type,
        '0.00', // truncated（无相机截断信息）
        '0', // occluded（无遮挡信息）
        '0.00', // alpha（无观察角）
        '-1', '-1', '-1', '-1', // 2D bbox（无相机投影，-1 表示缺失）
        fmt(height),
        fmt(width),
        fmt(length),
        fmt(posX),
        fmt(posY),
        fmt(posZ),
        fmt(rotationY),
      ].join(' '),
    );
  });
  return lines.join('\n');
}

/** ==================== 3. nuScenes 格式 ==================== */

/** nuScenes 单条标注（对齐 sample_annotation.json 的核心字段） */
export interface NuscenesAnnotation {
  token: string;
  sample_token: string;
  instance_token: string;
  category: string;
  /** 3D 框中心（全局系，米）：[x, y, z] */
  translation: [number, number, number];
  /** 尺寸（nuScenes 顺序：[宽, 高, 长]）：[width, height, length] */
  size: [number, number, number];
  /** 四元数（w, x, y, z），仅绕 Z 轴旋转 */
  rotation: [number, number, number, number];
  /** LiDAR 点云框内点数 */
  num_lidar_pts: number;
  num_radar_pts: number;
}

/** nuScenes 标注文件（按 sample_token 聚合） */
export interface NuscenesExportJson {
  format: 'nuscenes-sample-annotation';
  version: number;
  /** 每帧的标注列表 */
  samples: {
    sample_token: string;
    annotations: NuscenesAnnotation[];
  }[];
}

/** 绕 Z 轴 yaw（弧度）→ 四元数 [w, x, y, z] */
function yawToQuaternion(yaw: number): [number, number, number, number] {
  const half = yaw / 2;
  return [Math.cos(half), 0, 0, Math.sin(half)];
}

/** 导出 nuScenes 标注 JSON */
export function toNuscenesJson(source: ExportAnnotationSource): NuscenesExportJson {
  const annotations: NuscenesAnnotation[] = source.objects.map((obj, index) => {
    // 平台尺寸 [长, 宽, 高] → nuScenes [宽, 高, 长]
    const [length, width, height] = obj.size;
    const category = resolveExportCategory(obj.label, source.categoryMap);
    const token = obj.id || buildObjectId(category, index);
    return {
      token,
      sample_token: String(source.frameId),
      instance_token: token,
      category,
      translation: [obj.center[0], obj.center[1], obj.center[2]] as [number, number, number],
      size: [width, height, length] as [number, number, number],
      rotation: yawToQuaternion(degToRad(obj.yawDeg)),
      num_lidar_pts: obj.numPoints || 0,
      num_radar_pts: 0,
    };
  });
  return {
    format: 'nuscenes-sample-annotation',
    version: 1,
    samples: [
      {
        sample_token: String(source.frameId),
        annotations,
      },
    ],
  };
}

/** 序列化 nuScenes JSON 为字符串 */
export function stringifyNuscenesJson(source: ExportAnnotationSource): string {
  return JSON.stringify(toNuscenesJson(source), null, 2);
}

/** ==================== 便捷分发 ==================== */

export type ExportFormat = 'platform' | 'kitti' | 'nuscenes';

/** 按格式生成导出内容、MIME 类型与文件名 */
export function buildExportContent(
  format: ExportFormat,
  source: ExportAnnotationSource,
  defaultBaseName: string,
): { content: string; mime: string; ext: string; fileName: string } {
  const base = defaultBaseName.replace(/\.pcd$/i, '').replace(/[\\/:*?"<>|]/g, '_');
  switch (format) {
    case 'kitti':
      // KITTI label 文件惯例：与点云同名（不带 _annotations 后缀），便于训练框架直接按帧匹配
      return { content: toKittiText(source), mime: 'text/plain', ext: 'txt', fileName: `${base}.txt` };
    case 'nuscenes':
      return { content: stringifyNuscenesJson(source), mime: 'application/json', ext: 'json', fileName: `${base}_annotations.json` };
    case 'platform':
    default:
      return { content: stringifyPlatformJson(source), mime: 'application/json', ext: 'json', fileName: `${base}_annotations.json` };
  }
}
