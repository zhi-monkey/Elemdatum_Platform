import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum, MaBackendUrlEnum } from '/@/enums/mineaiEnum';

// 数据集详情
export const getDatasetCardInfo = async (datasetId) => {
  return await maHttp.get(
    {
      url: `datasets/guidedDatasetCardInfo/${datasetId}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const getDatasetListApi = async () => {
  return maHttp
    .get(
      {
        url: `datasets/getAllPublishedDatasets`,
        headers: {},
      },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
    )
    .then((res) => {
      const result = res ?? [];
      const options = result.map((item) => ({ label: item.name, value: item.id }));
      return Array.isArray(result) ? options : [];
    });
};
/**
 * 获取可用版本列表
 * @param selectedDatasetId 选择出来的数据集id
 * @param targetDatasetId 引导式训练自己的datasetId
 */
export const getAvailableVersionList = async (selectedDatasetId, targetDatasetId) => {
  return maHttp
    .get(
      {
        url: `datasets/getAvailableVersionList`,
        params: { selectedDatasetId, targetDatasetId },
        headers: {},
      },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
    )
    .then((res) => {
      const result = res ?? [];
      const options = result.map((item) => ({ label: item.versionName, value: item.id }));
      return Array.isArray(result) ? options : [];
    });
};

export const getImportingVersionInfo = async (fromVersionId, targetDatasetId) => {
  return maHttp
    .get(
      {
        url: `datasets/versions/getImportingVersionInfo`,
        params: { fromVersionId, targetDatasetId },
        headers: {},
      },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
    )
    .then((res) => {
      return res;
    });
};

export const searchPublicDatasetGroupByName = async (params: {
  current: number;
  size: number;
  name?: string;
}) => {
  return await maHttp.get(
    {
      url: 'datasets/group/searchPublicDatasetGroupByName',
      params,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const getPublicDatasetGroupByPage = async (params: { current: number; size: number }) => {
  return await maHttp.get(
    {
      url: 'datasets/group/getPublicDatasetGroupByPage',
      params,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const bindDatasetVersions = async (params: {
  datasetVersionIds: number[];
  modelGenerationId: number;
}) => {
  return await maHttp.post(
    {
      url: 'modelGeneration/bindDatasetVersions',
      params,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

export const getPublicDatasetsByDatasetGroupId = async (datasetGroupId: number) => {
  return await maHttp.get(
    {
      url: 'datasets/group/getPublicDatasetsByDatasetGroupId',
      params: {
        datasetGroupId,
      },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 根据数据集组ID和标注类型获取公开数据集列表
 */
export const getPublicDatasetsByDatasetGroupIdAndAnnotateType = async (
  datasetGroupId: number,
  annotateType: number,
) => {
  return await maHttp.get(
    {
      url: 'datasets/group/getPublicDatasetsByDatasetGroupIdAndAnnotateType',
      params: { datasetGroupId, annotateType },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const getPublicDatasetVersionsByDatasetId = async (params: {
  datasetId: number;
  current: number;
  size: number;
  labelList: string[];
  isPublic: boolean;
}) => {
  const serializedParams = {
    ...params,
    // 将 labelList 数组转换为逗号分隔的字符串
    labelList: params.labelList.join(','),
  };
  return await maHttp.get(
    {
      url: 'datasets/versions/publicVersionsDetailList',
      params: serializedParams,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const getPublicDatasetVersionsByDatasetIdAll = async (params: {
  datasetId: number;
  labelList: string[];
  isPublic: boolean;
}) => {
  const serializedParams = {
    ...params,
    // 将 labelList 数组转换为逗号分隔的字符串
    labelList: params.labelList.join(','),
  };
  return await maHttp.get(
    {
      url: 'datasets/versions/publicVersionsDetailListAll',
      params: serializedParams,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const getGuidedGenerationLabels = async (modelGenerationId: number) => {
  return await maHttp.get(
    {
      url: 'modelGeneration/getGuidedGenerationLabels',
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
