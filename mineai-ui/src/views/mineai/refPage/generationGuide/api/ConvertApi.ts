// 获取作业信息
export const fetchConvertDataMock = async () => {
  return {
    id: 624,
    name: 'job-cloud-merge-624',
    modelVersion: null,
    jobType: 3,
    controller: null,
    status: 1,
    modelGenerationId: 156,
    description: null,
    params: {
      HP_BATCH_SIZE: '10',
      HP_CONFIDENCE: '0.85',
      HP_EPOCHES: '10',
      HP_WEIGHT_DECAY: '0.0005',
      HP_MOMENTUN: '0.937',
      HP_LEARNING_RATE: '0.01',
      MODEL_WORKING_MODE: '1',
    },
    weightPath: 'job-cloud-merge-624',
    memory: '10Gi',
    cpus: '4',
    gpus: '2',
    logFilePath: null,
    createTime: '2024-06-22 16:21:36',
    lastJobTime: '2024-06-22 16:23:00',
    accuracy: null,
    deviceFirmWire: 'KBA12C-v1.0',
    SoftPlatformInfo: 'rknn',
    HardPlatformInfo: 'rk3588',
  };
};
