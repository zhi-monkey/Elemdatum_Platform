import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

/**
 * 分页查询数据集组
 */
export const getPrivateAndPublicDatasetGroupByPage = async (params: {
  current: number;
  size: number;
}) => {
  return await maHttp.get(
    {
      url: 'datasets/group/getPrivateAndPublicDatasetGroupByPage',
      params,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 按名称搜索数据集组（分页）
 */
export const searchPrivateAndPublicDatasetGroupByName = async (params: {
  current: number;
  size: number;
  name?: string;
}) => {
  return await maHttp.get(
    {
      url: 'datasets/group/searchPrivateAndPublicDatasetGroupByName',
      params,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 分页查询公开数据集组
 */
export const getPublicDatasetGroupByPage = async (params: {
  current: number;
  size: number;
}) => {
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

/**
 * 按名称搜索公开数据集组（分页）
 */
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

/**
 * 分页查询私有数据集组
 */
export const getPrivateDatasetGroupByPage = async (params: {
  current: number;
  size: number;
}) => {
  return await maHttp.get(
    {
      url: 'datasets/group/getPrivateDatasetGroupByPage',
      params,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 按名称搜索私有数据集组（分页）
 */
export const searchPrivateDatasetGroupByName = async (params: {
  current: number;
  size: number;
  name?: string;
}) => {
  return await maHttp.get(
    {
      url: 'datasets/group/searchPrivateDatasetGroupByName',
      params,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 根据数据集组ID获取数据集列表
 */
export const getDatasetsByDatasetGroupId = async (datasetGroupId: number) => {
  return await maHttp.get(
    {
      url: 'datasets/group/getDatasetsByDatasetGroupId',
      params: { datasetGroupId },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 根据数据集组ID和标注类型获取数据集列表
 */
export const getDatasetsByDatasetGroupIdAndAnnotateType = async (
  datasetGroupId: number,
  annotateType: number,
) => {
  return await maHttp.get(
    {
      url: 'datasets/group/getDatasetsByDatasetGroupIdAndAnnotateType',
      params: { datasetGroupId, annotateType },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 获取数据集版本列表
 */
export const getDatasetVersions = async (params: {
  datasetId: number;
  page?: number;
  pageSize?: number;
  current?: number;
  size?: number;
  format?: string;
}) => {
  return await maHttp.get(
    {
      url: 'datasets/versions/versionsDetailList',
      params,
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};
