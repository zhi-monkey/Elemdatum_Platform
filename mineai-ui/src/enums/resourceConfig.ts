// 资源配置常量
export const RESOURCE_CONFIGS = {
  basic: {
    memoryLimit: 30,
    cpuLimit: 12,
    gpuMemoryLimit: 40,
    label: '小型团队',
  },
  high: {
    memoryLimit: 120,
    cpuLimit: 48,
    gpuMemoryLimit: 80,
    label: '大型团队',
  },
} as const;

// 训练规模描述配置
export const TRAINING_SCALE_DESCRIPTIONS = {
  basic:
    '在该资源条件下，通常可同时开启约 2 个常规配置单卡训练任务，或约 1 个较大规模配置单卡训练任务，亦可支持 1 个左右的大规模配置单卡训练任务。',
  high: '在该资源条件下，通常可同时开启约 5 个常规配置单卡训练任务，或约 3 个较大规模配置单卡训练任务，亦可支持 2 个左右的大规模配置单卡训练任务。',
} as const;

// 资源配置选项（用于下拉框）
export const RESOURCE_LEVEL_OPTIONS = [
  { label: RESOURCE_CONFIGS.basic.label, value: 'basic' },
  { label: RESOURCE_CONFIGS.high.label, value: 'high' },
  { label: '自定义', value: 'custom' },
];

// 资源配置类型定义
export type ResourceLevel = keyof typeof RESOURCE_CONFIGS;
export type ResourceConfigType = typeof RESOURCE_CONFIGS[ResourceLevel];

// 工具函数
export const getResourceConfig = (level: string): ResourceConfigType => {
  if (level in RESOURCE_CONFIGS) {
    return RESOURCE_CONFIGS[level as ResourceLevel];
  }
  return { memoryLimit: 0, cpuLimit: 0, gpuMemoryLimit: 0, label: '' };
};

export const getTrainingDescription = (level: string): string => {
  if (level in TRAINING_SCALE_DESCRIPTIONS) {
    return TRAINING_SCALE_DESCRIPTIONS[level as ResourceLevel];
  }
  return '';
};

// 判断是否为预设配置
export const isPresetConfig = (
  memoryLimit: number,
  cpuLimit: number,
  gpuMemoryLimit: number,
): ResourceLevel | 'custom' => {
  for (const [key, config] of Object.entries(RESOURCE_CONFIGS)) {
    if (
      config.memoryLimit === memoryLimit &&
      config.cpuLimit === cpuLimit &&
      config.gpuMemoryLimit === gpuMemoryLimit
    ) {
      return key as ResourceLevel;
    }
  }
  return 'custom';
};
