import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

export const createJob = async (generationId: number) => {
  return await maHttp.get(
    {
      url: 'modelJob/createJob',
      params: {
        modelGenerationId: generationId,
        modelWorkingMode: 1,
      },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

export const findModelGenerationById = async (generationId: number) => {
  return await maHttp.get(
    {
      url: 'modelGeneration/findModelGenerationById',
      params: { id: generationId },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

export const createTestJob = async (generationId: number) => {
  return await maHttp.get(
    {
      url: 'modelJob/createTestJob',
      params: { modelGenerationId: generationId, modelWorkingMode: 5 },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

export const findModelGenerationByIdWithTest = async (generationId: number) => {
  return await maHttp.get(
    {
      url: 'modelGeneration/findModelGenerationById',
      params: { id: generationId },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

export const getTestJobProgress = async (testJobId: number, anotherTestJobId: number) => {
  return await maHttp.get(
    {
      url: 'modelJob/getTestJobProgressAndAcc',
      params: { testJobId, anotherTestJobId },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

export const createTrainJob = async (generationId: number) => {
  return await maHttp.get(
    {
      url: 'modelJob/createJob',
      params: {
        modelGenerationId: generationId,
        modelWorkingMode: 1,
      },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

export const getModel = async () => {
  return await maHttp.get(
    {
      url: 'model/getModel',
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

export const getMockModel = async () => {
  // 返回假数据
  return [
    {
      id: 73,
      modelName: '安全帽',
      modelEnglishName: 'model-1718772777385',
      mainConfig: null,
      monitorType: 0,
      description: '识别算法',
      areaLabelingTip: null,
      isDelete: 0,
      modelConfigList: [],
      subsystem: 'CENTRAL_PLATFORM',
      source: 2,
      generationId: 154,
      releaseTime: '2024-06-19 12:52:57',
      trainModelVersion: {
        id: 64,
        name: 'asda_v.10',
        level: 'v.10',
        versionLevel: 0,
        showName: 'asda',
        description: 'desc',
        size: 2048,
        modelConfigList: [
          {
            id: 376,
            field: 'HP_BATCH_SIZE',
            label: null,
            defaultNum: null,
            required: true,
            min: null,
            max: null,
            msg: 'numeric:[1,)',
          },
          {
            id: 377,
            field: 'HP_CONFIDENCE',
            label: null,
            defaultNum: null,
            required: true,
            min: null,
            max: null,
            msg: 'numeric:(0,1)',
          },
          {
            id: 378,
            field: 'HP_WEIGHT_DECAY',
            label: null,
            defaultNum: null,
            required: true,
            min: null,
            max: null,
            msg: 'numeric:(,)',
          },
          {
            id: 379,
            field: 'HP_EPOCHES',
            label: null,
            defaultNum: null,
            required: true,
            min: null,
            max: null,
            msg: 'numeric:[1,)',
          },
          {
            id: 380,
            field: 'HP_MOMENTUN',
            label: null,
            defaultNum: null,
            required: true,
            min: null,
            max: null,
            msg: 'numeric:(,)',
          },
          {
            id: 381,
            field: 'HP_LEARNING_RATE',
            label: null,
            defaultNum: null,
            required: true,
            min: null,
            max: null,
            msg: 'numeric:(,)',
          },
        ],
        architecture: 'arm64',
        weightPath: null,
        reuseId: 0,
        url: 'asda_image1718771427271',
        createTime: '2024-06-19 12:30:28',
        isDelete: 0,
        trainNum: null,
        inspectNum: null,
        areaLabelingTip: null,
        status: 1,
        conversionPlatform: null,
        roleId: 1,
        useGpu: true,
        inferable: false,
        inspectable: false,
        trainable: true,
        reuse: false,
        autoLabel: false,
      },
      deployModelVersion: null,
      convertModelVersion: null,
      bestWeightPath: 'job-cloud-merge-600',
      autoLabelModelVersion: null,
      trainModelJob: null,
      modelClassification: [
        {
          id: 28,
          classificationName: '识别',
          createTime: '2024-06-13 14:58:07',
          updateTime: '2024-06-13 14:58:07',
        },
      ],
    },
  ];
};

// 获取所有数据集
export const getAllDatasets = async () => {
  return await maHttp.get(
    {
      url: 'modelDataset/findAllDatasets',
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

// 获取所有硬件参数
export const getAllHardwareParams = async () => {
  return await maHttp.get(
    {
      url: 'modelGeneration/findAllHardwareParams',
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

// 按照 id 获取硬件信息
export const getHardwareParamsById = async (hardwareParamsId: number) => {
  return await maHttp.get(
    {
      url: `hardwareParams/${hardwareParamsId}`,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

// 获取训练作业进度
export const getTrainJobProgress = async (jobId) => {
  return await maHttp.get(
    {
      url: 'modelJob/getTrainJobProgress',
      params: { jobId },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

// 获取生产任务信息
export const fetchGenerationData = async (jobId) => {
  await maHttp.get(
    {
      url: 'modelGeneration/findGeneraByJob',
      params: { modelJobId: jobId },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

// 获取生产任务信息
export const fetchGenerationDataMock = async () => {
  return null;
};

// 获取作业信息
export const fetchJobData = async (jobId) => {
  return await maHttp.get(
    {
      url: 'modelJob/getJobByJobId',
      params: { id: jobId },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

// 获取作业信息
export const fetchJobDataMock = async () => {
  return {
    id: 624,
    name: 'job-cloud-merge-624',
    modelVersion: null,
    jobType: 1,
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
  };
};

// 获取作业参数
export const fetchJobParams = async (jobId) => {
  return await maHttp.get(
    {
      url: 'modelJob/getParamByJobId',
      params: { id: jobId },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

export const fetchJobParamsMock = async () => {
  return {
    HP_BATCH_SIZE: '10',
    HP_CONFIDENCE: '0.85',
    HP_EPOCHES: '10',
    HP_WEIGHT_DECAY: '0.0005',
    HP_MOMENTUN: '0.937',
    HP_LEARNING_RATE: '0.01',
    MODEL_WORKING_MODE: '1',
  };
};

// 获取控制台输出
export const fetchJobLog = async (jobId) => {
  const job = await maHttp.get(
    {
      url: 'modelJob/getJobByJobId',
      params: { id: jobId },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );

  const jobName = job.name; // 假设 jobName 是一个局部变量
  return await maHttp.get(
    {
      url: 'log/getJobLog',
      params: {
        jobName,
        direction: 'BACKWARD',
        namespace: 'ai-platform',
        start: 1,
      },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

// 获取控制台输出
export const fetchJobLogMock = async () => {
  return [
    {
      '1719044558135382404': '=======\n',
    },
    {
      '1719044558189896705':
        '\r  0%|          | 0/1 [00:00<?, ?it/s]\r       6/10     0.851G      1.249      2.657      1.333         13        640:   0%|          | 0/1 [00:00<?, ?it/s]\r       6/10     0.851G      1.249      2.657      1.333         13        640: 100%|██████████| 1/1 [00:00<00:00, 18.39it/s]\n',
    },
    {
      '1719044558227097212':
        '\r                 Class     Images  Instances      Box(P          R      mAP50  mAP50-95):   0%|          | 0/1 [00:00<?, ?it/s]\r                 Class     Images  Instances      Box(P          R      mAP50  mAP50-95): 100%|██████████| 1/1 [00:00<00:00, 32.16it/s]\n',
    },
    {
      '1719044558231530867':
        '                   all          4         13      0.725      0.902      0.962      0.836\n',
    },
    {
      '1719044558416550049': '\n',
    },
    {
      '1719044558416561725':
        '      Epoch    GPU_mem   box_loss   cls_loss   dfl_loss  Instances       Size\n',
    },
    {
      '1719044558416563620': '===on_fit_epoch_end===\n',
    },
    {
      '1719044558416569085': 'Epoch: 6 | train loss: 20.958484649658203 | test accuracy: 0.72484\n',
    },
    {
      '1719044558416570733': '=======\n',
    },
    {
      '1719044558467710606':
        '\r  0%|          | 0/1 [00:00<?, ?it/s]\r       7/10     0.851G      1.228       3.01      1.502         13        640:   0%|          | 0/1 [00:00<?, ?it/s]\r       7/10     0.851G      1.228       3.01      1.502         13        640: 100%|██████████| 1/1 [00:00<00:00, 19.61it/s]\n',
    },
    {
      '1719044558514089919':
        '\r                 Class     Images  Instances      Box(P          R      mAP50  mAP50-95):   0%|          | 0/1 [00:00<?, ?it/s]\r                 Class     Images  Instances      Box(P          R      mAP50  mAP50-95): 100%|██████████| 1/1 [00:00<00:00, 24.75it/s]\n',
    },
    {
      '1719044558518417380':
        '                   all          4         13       0.72      0.906      0.963      0.835\n',
    },
    {
      '1719044558700736815': '\n',
    },
    {
      '1719044558700747982':
        '      Epoch    GPU_mem   box_loss   cls_loss   dfl_loss  Instances       Size\n',
    },
    {
      '1719044558700815135': '===on_fit_epoch_end===\n',
    },
    {
      '1719044558700819078': 'Epoch: 7 | train loss: 22.956295013427734 | test accuracy: 0.72014\n',
    },
    {
      '1719044558700820465': '=======\n',
    },
    {
      '1719044558755692375':
        '\r  0%|          | 0/1 [00:00<?, ?it/s]\r       8/10     0.851G     0.9787      1.958      1.328         13        640:   0%|          | 0/1 [00:00<?, ?it/s]\r       8/10     0.851G     0.9787      1.958      1.328         13        640: 100%|██████████| 1/1 [00:00<00:00, 18.26it/s]\n',
    },
    {
      '1719044558802861063':
        '\r                 Class     Images  Instances      Box(P          R      mAP50  mAP50-95):   0%|          | 0/1 [00:00<?, ?it/s]\r                 Class     Images  Instances      Box(P          R      mAP50  mAP50-95): 100%|██████████| 1/1 [00:00<00:00, 24.23it/s]\n',
    },
    {
      '1719044558807274401':
        '                   all          4         13      0.721      0.908      0.963      0.842\n',
    },
    {
      '1719044558991450075': '\n',
    },
    {
      '1719044558991461772':
        '      Epoch    GPU_mem   box_loss   cls_loss   dfl_loss  Instances       Size\n',
    },
    {
      '1719044558991543781': '===on_fit_epoch_end===\n',
    },
    {
      '1719044558991548623': 'Epoch: 8 | train loss: 17.05790138244629 | test accuracy: 0.72121\n',
    },
    {
      '1719044558991550329': '=======\n',
    },
    {
      '1719044559046330298':
        '\r  0%|          | 0/1 [00:00<?, ?it/s]\r       9/10     0.851G      1.093      1.983      1.203         13        640:   0%|          | 0/1 [00:00<?, ?it/s]\r       9/10     0.851G      1.093      1.983      1.203         13        640: 100%|██████████| 1/1 [00:00<00:00, 18.29it/s]\n',
    },
    {
      '1719044559093308402':
        '\r                 Class     Images  Instances      Box(P          R      mAP50  mAP50-95):   0%|          | 0/1 [00:00<?, ?it/s]\r                 Class     Images  Instances      Box(P          R      mAP50  mAP50-95): 100%|██████████| 1/1 [00:00<00:00, 24.38it/s]\n',
    },
    {
      '1719044559097727626':
        '                   all          4         13       0.72       0.91      0.962      0.841\n',
    },
    {
      '1719044559280089807': '\n',
    },
    {
      '1719044559280102045':
        '      Epoch    GPU_mem   box_loss   cls_loss   dfl_loss  Instances       Size\n',
    },
    {
      '1719044559280140892': '===on_fit_epoch_end===\n',
    },
    {
      '1719044559280145981': 'Epoch: 9 | train loss: 17.11558723449707 | test accuracy: 0.71954\n',
    },
    {
      '1719044559280147630': '=======\n',
    },
    {
      '1719044559335640647':
        '\r  0%|          | 0/1 [00:00<?, ?it/s]\r      10/10     0.854G      1.251      3.652      1.485         13        640:   0%|          | 0/1 [00:00<?, ?it/s]\r      10/10     0.854G      1.251      3.652      1.485         13        640: 100%|██████████| 1/1 [00:00<00:00, 18.06it/s]\n',
    },
    {
      '1719044559386319986':
        '\r                 Class     Images  Instances      Box(P          R      mAP50  mAP50-95):   0%|          | 0/1 [00:00<?, ?it/s]\r                 Class     Images  Instances      Box(P          R      mAP50  mAP50-95): 100%|██████████| 1/1 [00:00<00:00, 22.42it/s]\n',
    },
    {
      '1719044560448173923':
        '                   all          4         13      0.719      0.912      0.963      0.842\n',
    },
    {
      '1719044562765524606': '\n',
    },
    {
      '1719044562765538781': '10 epochs completed in 0.002 hours.\n',
    },
    {
      '1719044562838623223': 'Optimizer stripped from /tmp/run/train/weights/last.pt, 6.5MB\n',
    },
    {
      '1719044562890330608': 'Optimizer stripped from /tmp/run/train/weights/best.pt, 6.5MB\n',
    },
    {
      '1719044562890793072': '\n',
    },
    {
      '1719044562890796290': 'Validating /tmp/run/train/weights/best.pt...\n',
    },
    {
      '1719044562890992514':
        'Ultralytics YOLOv8.0.151 🚀 Python-3.10.14 torch-2.2.2 CUDA:0 (NVIDIA GeForce RTX 2080 Ti, 11009MiB)\n',
    },
    {
      '1719044563031943055':
        'Model summary (fused): 168 layers, 3151904 parameters, 0 gradients, 8.7 GFLOPs\n',
    },
    {
      '1719044563141944395': '===on_fit_epoch_end===\n',
    },
    {
      '1719044563141957297':
        'Epoch: 10 | train loss: 25.555435180664062 | test accuracy: 0.71928\n',
    },
    {
      '1719044563141959117': '=======\n',
    },
    {
      '1719044563167959729':
        '\r                 Class     Images  Instances      Box(P          R      mAP50  mAP50-95):   0%|          | 0/1 [00:00<?, ?it/s]\r                 Class     Images  Instances      Box(P          R      mAP50  mAP50-95): 100%|██████████| 1/1 [00:00<00:00, 38.79it/s]\n',
    },
    {
      '1719044564132801442':
        '                   all          4         13      0.789      0.894      0.965      0.843\n',
    },
    {
      '1719044564132817411':
        '                 zebra          4          1          1          1      0.995      0.995\n',
    },
    {
      '1719044564132852417':
        '               giraffe          4          2          1      0.904      0.995      0.656\n',
    },
    {
      '1719044564132883099':
        '                  bowl          4          3      0.588          1      0.913       0.79\n',
    },
    {
      '1719044564132951175':
        '                orange          4          4          1      0.356       0.87      0.572\n',
    },
    {
      '1719044564132956339':
        '              broccoli          4          1       0.45          1      0.995      0.995\n',
    },
    {
      '1719044564133013885':
        '          potted plant          4          1      0.885          1      0.995      0.995\n',
    },
    {
      '1719044564133017232':
        '                  vase          4          1      0.597          1      0.995      0.895\n',
    },
    {
      '1719044566285283009':
        'Speed: 0.2ms preprocess, 1.3ms inference, 0.0ms loss, 1.0ms postprocess per image\n',
    },
    {
      '1719044566285294682': 'Results saved to \u001b[1m/tmp/run/train\u001b[0m\n',
    },
    {
      '1719044566988450493': 'Ultralytics YOLOv8.0.151 🚀 Python-3.10.14 torch-2.2.2 CPU ()\n',
    },
    {
      '1719044567102998789':
        'Model summary (fused): 168 layers, 3151904 parameters, 0 gradients, 8.7 GFLOPs\n',
    },
    {
      '1719044567362717507': '\n',
    },
    {
      '1719044567362764175':
        "\u001b[34m\u001b[1mPyTorch:\u001b[0m starting from '/tmp/run/train/weights/best.pt' with input shape (1, 3, 640, 640) BCHW and output shape(s) ((1, 64, 80, 80), (1, 80, 80, 80), (1, 1, 80, 80), (1, 64, 40, 40), (1, 80, 40, 40), (1, 1, 40, 40), (1, 64, 20, 20), (1, 80, 20, 20), (1, 1, 20, 20)) (6.2 MB)\n",
    },
    {
      '1719044567363082623': '\n',
    },
    {
      '1719044567363103919':
        '\u001b[34m\u001b[1mRKNN:\u001b[0m starting export with torch 2.2.2...\n',
    },
    {
      '1719044568515547731': '\n',
    },
    {
      '1719044568515564550':
        '\u001b[34m\u001b[1mRKNN:\u001b[0m feed /tmp/run/train/weights/best.onnx to RKNN-Toolkit or RKNN-Toolkit2 to generate RKNN model.\n',
    },
    {
      '1719044568515566635':
        'Refer https://github.com/airockchip/rknn_model_zoo/tree/main/models/CV/object_detection/yolo\n',
    },
    {
      '1719044568515583327':
        "\u001b[34m\u001b[1mRKNN:\u001b[0m export success ✅ 1.2s, saved as '/tmp/run/train/weights/best.onnx' (12.1 MB)\n",
    },
    {
      '1719044568515672820': '\n',
    },
    {
      '1719044568515678038': 'Export complete (1.5s)\n',
    },
    {
      '1719044568515679793': 'Results saved to \u001b[1m/tmp/run/train/weights\u001b[0m\n',
    },
    {
      '1719044568515681259':
        'Predict:         yolo predict task=detect model=/tmp/run/train/weights/best.onnx imgsz=640 \n',
    },
    {
      '1719044568515682548':
        'Validate:        yolo val task=detect model=/tmp/run/train/weights/best.onnx imgsz=640 data=/tmp/dataset/data.yaml \n',
    },
    {
      '1719044568515683832': 'Visualize:       https://netron.app\n',
    },
    {
      '1719044568949698779':
        'Ultralytics YOLOv8.0.151 🚀 Python-3.10.14 torch-2.2.2 CUDA:0 (NVIDIA GeForce RTX 2080 Ti, 11009MiB)\n',
    },
    {
      '1719044569080659994':
        'Model summary (fused): 168 layers, 3151904 parameters, 0 gradients, 8.7 GFLOPs\n',
    },
    {
      '1719044569088499725': '===on_fit_epoch_end===\n',
    },
    {
      '1719044569088511268':
        'Epoch: 10 | train loss: 25.555435180664062 | test accuracy: 0.7886282627254538\n',
    },
    {
      '1719044569088513120': '=======\n',
    },
    {
      '1719044569088514478': 'Start validating model.\n',
    },
    {
      '1719044569088515710': 'Total sample number: 5\n',
    },
    {
      '1719044569090336153':
        '\r\u001b[34m\u001b[1mval: \u001b[0mScanning /tmp/dataset/labels/test...:   0%|          | 0/4 [00:00<?, ?it/s]\r\u001b[34m\u001b[1mval: \u001b[0mScanning /tmp/dataset/labels/test... 4 images, 0 backgrounds, 0 corrupt: 100%|██████████| 4/4 [00:00<00:00, 2391.28it/s]\n',
    },
    {
      '1719044569091036827':
        '\u001b[34m\u001b[1mval: \u001b[0mNew cache created: /tmp/dataset/labels/test.cache\n',
    },
    {
      '1719044570473493587':
        '\r                 Class     Images  Instances      Box(P          R      mAP50  mAP50-95):   0%|          | 0/1 [00:00<?, ?it/s]\r                 Class     Images  Instances      Box(P          R      mAP50  mAP50-95): 100%|██████████| 1/1 [00:01<00:00,  1.16s/it]\r                 Class     Images  Instances      Box(P          R      mAP50  mAP50-95): 100%|██████████| 1/1 [00:01<00:00,  1.16s/it]\n',
    },
    {
      '1719044571428638235':
        '                   all          4         13      0.788      0.894      0.968      0.843\n',
    },
    {
      '1719044571428652063':
        '                 zebra          4          1          1          1      0.995      0.995\n',
    },
    {
      '1719044571428680424':
        '               giraffe          4          2          1      0.904      0.995      0.655\n',
    },
    {
      '1719044571428718580':
        '                  bowl          4          3      0.587          1      0.913       0.79\n',
    },
    {
      '1719044571428746313':
        '                orange          4          4          1      0.356      0.888      0.575\n',
    },
    {
      '1719044571428774292':
        '              broccoli          4          1      0.448          1      0.995      0.995\n',
    },
    {
      '1719044571428807178':
        '          potted plant          4          1      0.885          1      0.995      0.995\n',
    },
    {
      '1719044571428840544':
        '                  vase          4          1      0.597          1      0.995      0.895\n',
    },
    {
      '1719044574488647388':
        'Speed: 0.3ms preprocess, 14.1ms inference, 0.0ms loss, 0.6ms postprocess per image\n',
    },
    {
      '1719044574488655481': 'Results saved to \u001b[1m/tmp/run/train2\u001b[0m\n',
    },
    {
      '1719044574489008984': '===on_val_batch_end===\n',
    },
    {
      '1719044574489016321':
        'tested: 4, correct: 4, accuracy: 0.788222, recall: 0.894281, map50: 0.967908, map50_95: 0.843029\n',
    },
    {
      '1719044574489018106': '======\n',
    },
    {
      '1719044574489019427': '===on_val_end===\n',
    },
    {
      '1719044574489020657':
        'tested: 4, correct: 4, accuracy: 0.788222, recall: 0.894281, map50: 0.967908, map50_95: 0.843029\n',
    },
    {
      '1719044574489021957': '======\n',
    },
  ];
};
// 获取饼图数据
export const fetchPieData = async (jobId) => {
  return await maHttp.get(
    {
      url: 'modelJob/getTrainAccAndRecall',
      params: { trainJobId: jobId },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

// 获取 Epoch 详细信息
export const fetchEpochDetail = async (jobName) => {
  return await maHttp.get(
    {
      url: 'log/getEpochDetail',
      params: {
        jobName,
        limit: 500,
        start: 1,
      },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

// 获取 Epoch 详细信息
export const fetchEpochDetailMock = () => {
  return {
    'train loss': [
      '15.821107864379883',
      '17.811477661132812',
      '15.515989303588867',
      '18.949058532714844',
      '13.870832443237305',
      '20.958484649658203',
      '22.956295013427734',
      '17.05790138244629',
      '17.11558723449707',
      '25.555435180664062',
      '25.555435180664062',
    ],
    'test accuracy': [
      '0.71474',
      '0.73163',
      '0.78851',
      '0.72722',
      '0.72695',
      '0.72484',
      '0.72014',
      '0.72121',
      '0.71954',
      '0.71928',
      '0.7886282627254538',
    ],
    epoch: ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10', '10'],
  };
};

// 获取训练作业进度
export const fetchTrainJobProgress = async (jobId) => {
  return await maHttp.get(
    {
      url: 'modelJob/getTrainJobProgress',
      params: { jobId },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

// 获取训练作业进度
export const fetchTrainJobProgressMock = (progress) => {
  return progress;
};
