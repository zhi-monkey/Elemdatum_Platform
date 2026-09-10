import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

/**
 * 跨数据集多条件检索图片
 */
export const searchFiles = async (params) => {
  return await maHttp.post(
    { url: 'datasets/files/search', data: params },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 现存全部标签（检索标签筛选下拉用）
 */
export const getAllLabels = async () => {
  return await maHttp.get(
    { url: 'datasets/files/search/labels' },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 图片数据集（检索数据集筛选下拉用，data_type=0）
 */
export const getAllDatasets = async () => {
  return await maHttp.get(
    { url: 'datasets/getAllDatasetsByType?dataType=0' },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 跨数据集多条件检索视频文件
 */
export const searchVideoFiles = async (params) => {
  return await maHttp.post(
    { url: 'video/datasets/files/search', data: params },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 视频数据集（检索数据集筛选下拉用）
 */
export const getVideoDatasets = async () => {
  return await maHttp.get(
    { url: 'video/datasets/list' },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 跨数据集多条件检索点云文件
 */
export const searchPcFiles = async (params) => {
  return await maHttp.post(
    { url: 'pointcloud/datasets/files/search', data: params },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 点云数据集（检索数据集筛选下拉用）
 */
export const getPcDatasets = async () => {
  return await maHttp.get(
    { url: 'pointcloud/datasets/list' },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 提交数据批量导出任务
 */
export const createExportTask = async (params) => {
  return await maHttp.post(
    { url: 'datasets/export/tasks', data: params },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 导出任务列表（分页）
 */
export const getExportTasks = async (params) => {
  return await maHttp.get(
    { url: 'datasets/export/tasks', params },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 查询单个导出任务进度
 */
export const getExportTask = async (taskId: string) => {
  return await maHttp.get(
    { url: `datasets/export/tasks/${taskId}` },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 下载导出文件（返回原生响应，blob）
 */
export const downloadExportTask = async (taskId: string) => {
  return await maHttp.get(
    { url: `datasets/export/tasks/${taskId}/download`, responseType: 'blob' },
    { isReturnNativeResponse: true, isTransformResponse: false, urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 确认导出任务
 */
export const confirmExportTask = async (taskId: string) => {
  return await maHttp.post(
    { url: `datasets/export/tasks/${taskId}/confirm` },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 取消导出任务
 */
export const cancelExportTask = async (taskId: string) => {
  return await maHttp.post(
    { url: `datasets/export/tasks/${taskId}/cancel` },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 批量删除导出任务
 */
export const deleteExportTasks = async (ids: number[]) => {
  return await maHttp.delete(
    { url: 'datasets/export/tasks', data: ids },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 从检索结果新建数据集
 */
export const createDatasetFromSearch = async (params) => {
  return await maHttp.post(
    { url: 'datasets/createFromSearch', data: params },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 全部数据集组（新建数据集弹窗下拉用）
 */
export const getAllDatasetGroup = async () => {
  return await maHttp.get(
    { url: 'datasets/group/getAllDatasetGroup' },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 数据集组下的数据集列表（合并数据集弹窗用）
 */
export const getDatasetsByDatasetGroupId = async (datasetGroupId: number) => {
  return await maHttp.get(
    { url: 'datasets/group/getDatasetsByDatasetGroupId', params: { datasetGroupId } },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 合并文件到已有数据集
 */
export const mergeToDataset = async (params) => {
  return await maHttp.post(
    { url: 'datasets/mergeToDataset', data: params },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};
