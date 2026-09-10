import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum, MaBackendUrlEnum } from '/@/enums/mineaiEnum';

export const getGenerationDetailById = async (generationId: Number) => {
  return maHttp.get(
    {
      url: 'modelGeneration/findModelGenerationById',
      params: {
        id: generationId,
      },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

export const getIsPublishing = async (datasetId: Number) => {
  return maHttp.get(
    {
      url: `datasets/isPublishing/${datasetId}`,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const getModelGenerationBoundVersions = async (modelGenerationId: Number) => {
  return maHttp.get(
    {
      url: 'modelGeneration/getModelGenerationBoundVersions',
      params: {
        modelGenerationId,
      },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

export const PipelinePhase = {
  ANNOTATING: 0, // 标注
  PUBLISHING: 1, // 发布中
  SPLITTING: 2, // 切分
  TRAINING: 3, // 训练
  CONVERTING: 4, // 转换
  FINISHED: 5, // 完成
};
export const StepPhase = {
  DATA: 0,
  TRAINING: 1,
  CONVERTING: 2,
  FINISHED: 3,
};
