//api
// 查询数据增强字典
import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum, MaBackendUrlEnum } from '/@/enums/mineaiEnum';
import { fileCodeMap } from '/@/views/mineai/data/dataset-details2/util';

//查询所有标签组
export const findAllLabelGroupList = async () => {
  return await maHttp.get(
    {
      url: 'labelGroup/getAll',
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};
//根据标签组id查询信息
export const getLabelGroupDetail = async (labelGroupId: number) => {
  return await maHttp.get(
    {
      url: `labelGroup/${labelGroupId}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const queryDataEnhanceList = async () => {
  return await maHttp.get(
    {
      url: 'user/dict/dataset_enhance',
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
};

// 数据集详情
export const detail = async (id) => {
  return await maHttp.get(
    {
      url: `datasets/${id}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 查询数据集该搜索条件下的文件数量
export const count = async (datasetId, params: any = {}) => {
  const newParams = {
    // annotationResult必填，但只传该参数时，不影响数量统计，是筛选时结合下面两个参数使用
    annotationResult: params?.status || fileCodeMap.HAVE_ANNOTATION,
    annotationStatus: params?.annotateStatus || [],
    annotationMethod: params?.annotateType || [],
    labelIds: params?.labelId || [],
  };
  return await maHttp.get(
    {
      url: `datasets/${datasetId}/count`,
      params: newParams,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 查询目标数据集的图片数量信息
export const getTeamLabelInfo = async (subtaskId: number) => {
  return await maHttp.get(
    {
      url: `datasets/team/${subtaskId}/subtask/annotation/status`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 目标检测文件列表
export const detectFileList = async (datasetId, params) => {
  return await maHttp.get(
    {
      url: `datasets/${datasetId}/files/detection`,
      params: params,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 查询文件偏移值
export const queryFileOffset = async (datasetId, fileId, query = {}) => {
  return await maHttp.get(
    {
      url: `datasets/${datasetId}/files/${fileId}/offset`,
      params: query,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const getAbsoluteOffsetForAnnotationStatus = async (datasetId, params) => {
  return await maHttp.get(
    {
      url: `datasets/${datasetId}/annotation-offset`,
      params: {
        annotationStatus: params.annotationStatus, // 标注状态，如101
        startOffset: params.startOffset, // 起始偏移量
        total: params.total, // 分配的文件总数
      },
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 获取当前文件之后最近的未标注图片的ID
export const getNearestUnannotatedFileId = async (datasetId, params) => {
  return await maHttp.get(
    {
      url: `datasets/${datasetId}/nearest-unannotated-after-current`,
      params: {
        currentFileId: params.currentFileId, // 当前文件ID
        labelId: params.labelId, // 标签筛选（可选）
        versionName: params.versionName, // 版本名称（可选）
      },
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 指定原始文件，获取增强文件列表
export const getEnhanceFileList = async (datasetId, fileId) => {
  return await maHttp.get(
    {
      url: `datasets/${datasetId}/${fileId}/enhanceFileList`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 查询数据集标签
export const queryLabels = async (datasetId, params) => {
  return maHttp.get(
    {
      url: `datasets/${datasetId}/labels`,
      params,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 创建数据集标签
export const createLabel = async (datasetId, data) => {
  return maHttp.post(
    {
      url: `datasets/${datasetId}/labels`,
      data,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// labels
export const getLabels = async (id) => {
  return await maHttp.get(
    {
      url: `datasets/${id}/labels`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const getAutoLabels = async (labelGroupType) => {
  return await maHttp.get(
    {
      url: `datasets/labels/auto/${labelGroupType}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const editLabel = async (labelId, label, datasetId = undefined) => {
  label.labelId = labelId;
  return await maHttp.put(
    {
      url: `datasets/labels`,
      data: { ...label, datasetId },
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

//files
// 请求指定图片信息
export const queryFile = async (datasetId, id, versionName = undefined) => {
  const params = versionName ? { versionName } : {};
  return (
    (await maHttp.get(
      {
        url: `datasets/files/${datasetId}/${id}/info`,
        params,
        headers: {},
      },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
    )) || {}
  );
};

// 当不存在 fileId 时, 获取数据集下面的第一个图片
export const queryFirstImg = async (datasetId: number, versionName?: string) => {
  const params = versionName ? { versionName } : {};
  return (
    (await maHttp.get(
      {
        url: `datasets/${datasetId}/files/first`,
        params,
        headers: {},
      },
      { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
    )) || {}
  );
};

// cruds
export const list = async (params) => {
  const { datasetId } = params;
  delete params.datasetId;
  return await maHttp.get(
    {
      url: `datasets/${datasetId}/files`,
      params: params,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const del = async (params) => {
  const { datasetId } = params;
  delete params.datasetId;
  params.datasetIds = [datasetId];
  return await maHttp.delete(
    {
      url: `datasets/files`,
      data: params,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};
export const delVideo = async (params) => {
  const { datasetId } = params;
  delete params.datasetId;
  params.datasetIds = [datasetId];
  return await maHttp.delete(
    {
      url: `datasets/files/videos`,
      data: params,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 上传文件接口（异步，返回taskId）
export const submit = async (id, files): Promise<{ taskId: string }> => {
  return await maHttp.post(
    {
      url: `datasets/${id}/files`,
      data: { files },
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 数据集下图片分页查询
export const imageFilesByPage = async (datasetId, params) => {
  return await maHttp.get(
    {
      url: `datasets/${datasetId}/files/images`,
      params,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 查询数据集下一发布版本
export const queryNextVersion = async (datasetId) => {
  return await maHttp.get(
    {
      url: `datasets/versions/${datasetId}/nextVersionName`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 查询数据集对应的标注信息
export const queryAnnotationInfo = async (datasetId) => {
  return await maHttp.get(
    {
      url: `datasets/${datasetId}/annotationName`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 数据集版本保存
 * @param data
 */
export const saveVersion = async (data = {}) => {
  return await maHttp.post(
    {
      url: `datasets/versions`,
      data,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 数据集发布, 与上面保存版本不同
 * @param data
 */
export const datasetRelease = async (data = {}) => {
  return await maHttp.post(
    {
      url: `datasets/versions/release`,
      data,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 数据集发布（支持选择是否存档）
 * @param data
 */
export const datasetReleaseWithOption = async (data = {}) => {
  return await maHttp.post(
    {
      url: `datasets/versions/releaseWithOption`,
      data,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};
// 删除数据集版本
export const deleteDatasetVersion = async (datasetId, versionName) => {
  return await maHttp.delete(
    {
      url: `datasets/versions`,
      data: {
        datasetId: datasetId,
        versionName: versionName,
      },
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};
// 旧版图片数据集中的视频分页查询（data_file 表）
export const videoFilesByPage = async (datasetId, params) => {
  return await maHttp.get(
    {
      url: `datasets/${datasetId}/files/videos`,
      params,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 独立视频数据集文件分页查询（video_dataset_file 表）
export const getVideoDatasetFiles = async (datasetId: number, params) => {
  return await maHttp.get(
    {
      url: `video/datasets/${datasetId}/files`,
      params,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 独立视频数据集详情（video 命名空间，数字 id 与图片数据集隔离，避免跨类型冲突）
export const getVideoDatasetDetail = async (datasetId: number) => {
  return await maHttp.get(
    {
      url: `video/datasets/${datasetId}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

//获取数据集版本列表
export const datasetVersionsByPage = async (params) => {
  return await maHttp.get(
    {
      url: `datasets/versions`,
      params,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 切换数据集版本
export const switchVersion = async (datasetId, versionName) => {
  return await maHttp.put(
    {
      url: `datasets/versions/${datasetId}?versionName=${versionName}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 获取数据集详情信息
export const datasetDetails = async (datasetId) => {
  return await maHttp.get(
    {
      url: `datasets/${datasetId}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const PicAndVideoDetails = async (datasetId) => {
  return await maHttp.get(
    {
      url: `datasets/${datasetId}/files/infos`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const getMinIOAuth = async () => {
  return await maHttp.get(
    {
      url: 'minio/info',
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
};

//视频抽帧接口
export const videoSample = async (params) => {
  return await maHttp.get(
    {
      url: `datasets/sample`,
      params,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 查询标注进度
export const queryDatasetsProgress = async (params) => {
  return await maHttp.get(
    {
      url: 'datasets/progress',
      params,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 查询数据集状态（发布情况，抽帧，增强情况）
export const queryDatasetStatus = async (params) => {
  return await maHttp.get(
    {
      url: 'datasets/status',
      params,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 根据ID删除progressTimeMap中的记录
export const deleteMapRecordById = async (id) => {
  return await maHttp.delete(
    {
      url: `datasets/map/delete?id=${id}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const getDatasetLabelInfo = async (params) => {
  return await maHttp.get(
    {
      url: `datasets/getDatasetLabelInfo`,
      params,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// ============ 点云数据集（PCD）相关接口 ============

// 查询点云数据集详情（基础信息 + 文件统计）
export const getPcDatasetDetail = async (datasetId: number) => {
  return await maHttp.get(
    {
      url: `pointcloud/datasets/${datasetId}/detail`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 分页查询点云数据集 PCD 文件列表
export const getPcDatasetFiles = async (datasetId: number, params) => {
  return await maHttp.get(
    {
      url: `pointcloud/datasets/${datasetId}/files`,
      params,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 删除单个 PCD 文件
export const deletePcDatasetFile = async (datasetId: number, fileId: number) => {
  return await maHttp.delete(
    {
      url: `pointcloud/datasets/${datasetId}/files/${fileId}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 查询点云数据集标签列表
export const getPcDatasetLabels = async (datasetId: number) => {
  return await maHttp.get(
    {
      url: `pointcloud/datasets/${datasetId}/labels`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 保存点云 PCD 文件的 3D Box 标注
export const savePcDatasetAnnotations = async (
  datasetId: number,
  fileId: number,
  payload: { datasetId: number; fileId: number; fileName: string; boxes: unknown[] },
) => {
  return await maHttp.put(
    {
      url: `pointcloud/datasets/${datasetId}/files/${fileId}/annotations`,
      data: payload,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 查询点云 PCD 文件已保存的 3D Box 标注
export const getPcDatasetAnnotations = async (datasetId: number, fileId: number) => {
  return await maHttp.get(
    {
      url: `pointcloud/datasets/${datasetId}/files/${fileId}/annotations`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 数据集批量详情
export const batchGet = async (datasetIds: number[]) => {
  return await maHttp.get(
    {
      url: `datasets/batchGet/${datasetIds.join(',')}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 批量抽帧接口
export const batchVideoSample = async (params) => {
  return await maHttp.get(
    {
      url: `datasets/sampleList`,
      params,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 获取视频信息（分辨率、帧率等）
export const getVideoInfo = async (datasetId: number, fileId: number) => {
  return await maHttp.get(
    {
      url: `datasets/${datasetId}/video/${fileId}/info`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 批量查询数据集图片信息
export const batchQueryFile = async (data) => {
  return maHttp.post(
    {
      url: `datasets/files/infos`,
      data,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 上传文件接口
export const submitImage = async (id, files) => {
  return await maHttp.post(
    {
      url: `datasets/${id}/files`,
      headers: {},
      data: {
        files,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 上传视频接口
export const submitVideo = async (id, data) => {
  return await maHttp.post(
    {
      url: `datasets/${id}/video`,
      headers: {},
      data,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 查询上传任务状态
export const getUploadTaskStatus = async (
  datasetId: number,
  taskId: string,
): Promise<{
  taskId: string;
  status: 'processing' | 'success' | 'failed';
  errorMessage?: string;
  fileCount: number;
  createdTime: string;
  updatedTime: string;
}> => {
  return await maHttp.get(
    {
      url: `datasets/${datasetId}/upload-tasks/${taskId}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 获取数据集指定图片id的标注信息,可指定类型
export const getImageAnnotationInfo = async (datasetId, fileId, labelType) => {
  return await maHttp.get(
    {
      url: `datasets/files/${datasetId}/${fileId}/label?labelType=${labelType}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const batchGetImageAnnotationInfo = async (datasetId, fileIds, labelType) => {
  return await maHttp.get(
    {
      url: `datasets/files/${datasetId}/batchLabel`,
      params: { fileIds, labelType },
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const getYoloLabelsFile = async (datasetId) => {
  return await maHttp.get(
    {
      url: `datasets/files/${datasetId}/yoloLabelsFile`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 获取数据仓库文件列表
export const getDataRepoFiles = async (params?: any) => {
  return await maHttp.get(
    {
      url: `datarepofiles`,
      headers: {},
      params: params,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 获取数据仓库文件列表
export const getDataRepo = async (params?: any) => {
  return await maHttp.get(
    {
      url: `datarepos`,
      headers: {},
      params: params,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 获取数据仓库的预览信息
export const getDataRepoInfo = async (datasetId) => {
  return await maHttp.get(
    {
      url: `datarepos/${datasetId}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 获取数据仓库中的文件信息: 图片文件、视频文件、其他文件
export const PicAndVideoOthersDetails = async (datasetId) => {
  return await maHttp.get(
    {
      url: `datarepofiles/countFileTypes/${datasetId}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};
export const videoRepoFilesByPage = async (datasetId) => {
  const params: {} = {
    datasetId: datasetId,
    fileType: 1,
  };
  return await maHttp.get(
    {
      url: `datarepofiles`,
      headers: {},
      params: params,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const delDataRepoFile = async (id: number) => {
  return await maHttp.delete(
    {
      url: `datarepofiles/${id}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const delDataRepoFileBatch = async (params) => {
  return await maHttp.delete(
    {
      url: `datarepofiles/batch`,
      data: { ids: params.fileIds },
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const delDataRepo = async (id: number) => {
  return await maHttp.delete(
    {
      url: `datarepos/${id}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 多人标注 —— 新增任务
 *
 * @param data
 */
export const addNewTask = async (data) => {
  return await maHttp.post(
    {
      url: `datasets/team/task`,
      data: { ...data },
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 验证图片数量是否大于团队人员数量
export const validateDatasetPicNum = async (datesetId, teamId) => {
  return await maHttp.get(
    {
      url: `datasets/team/validateDatasetPicNum/${datesetId}/${teamId}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 获取数据集第一张图片ID
export const getFirstFileId = async (datasetId, versionName = null, type = null) => {
  return await maHttp.get(
    {
      url: `datasets/${datasetId}/files/first`,
      params: { versionName, type },
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 获取最近的未标注图片信息
export const getNearestUnannotatedFileInfo = async (
  datasetId,
  currentFileId,
  versionName = null,
  labelId = null,
) => {
  return await maHttp.get(
    {
      url: `datasets/${datasetId}/nearest-unannotated-after-current`,
      params: { currentFileId, versionName, labelId },
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 多人标注 —— 新增团队
 *
 * @param data
 */
export const addNewTeam = async (data) => {
  return await maHttp.post(
    {
      url: 'datasets/team',
      data: { ...data },
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 多人标注 —— 删除标注团队
 *
 * @param data
 */
export const deleteTeam = async (params) => {
  return await maHttp.delete(
    {
      url: 'datasets/team',
      data: { ids: [params.id] },
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const getTeamInfo = async (teamId) => {
  return await maHttp.get(
    {
      url: `datasets/team/${teamId}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const getDatasetImageCount = async (datasetId) => {
  return await maHttp.get(
    {
      url: `datasets/getDatasetImageCount/${datasetId}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const isDatasetIsGuided = async (datasetId) => {
  return await maHttp.get(
    {
      url: `datasets/isGuided/${datasetId}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 多人标注 —— 删除发布任务
 *
 * @param data
 */
export const deleteTask = async (params) => {
  return await maHttp.delete(
    {
      url: 'datasets/team/task',
      data: { ids: [params.id] },
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 多人标注 —— 提交子任务
 *
 * @param taskId
 * @param subtaskId
 */
export const submitSubtask = async (taskId, subtaskId) => {
  return await maHttp.post(
    {
      url: `datasets/team/${taskId}/${subtaskId}/subtask/submit`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 多人标注 —— 检查子任务是否所有图片都已标注
 *
 * @param subtaskId
 */
export const checkAllAnnotated = async (subtaskId) => {
  return await maHttp.get(
    {
      url: `datasets/team/${subtaskId}/check-all-annotated`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 多人标注 —— 一键确认（批量提交）
 *
 * @param taskId
 * @param subtaskId
 */
export const batchConfirmSubtask = async (taskId, subtaskId) => {
  return await maHttp.post(
    {
      url: `datasets/team/${taskId}/${subtaskId}/batch-confirm`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 多人标注 —— 终止任务
 *
 * @param taskId
 */
export const terminatetask = async (taskId) => {
  return await maHttp.post(
    {
      url: `datasets/team/${taskId}/task/terminate`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 多人标注 —— 更新团队信息
 *
 * @param data
 */
export const updateTeam = async (data) => {
  return await maHttp.put(
    {
      url: 'datasets/team',
      data: { ...data },
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 多人标注 —— 更新团队信息
 *
 * @param data
 */
export const importDatasetFromDataRepo = async (dataRepoId, datasetId, dataType) => {
  return await maHttp.post(
    {
      url: `datasets/${dataRepoId}/${datasetId}/${dataType}/import`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 多人标注 —— 更新团队信息
 *
 * @param datasetId
 * @param versionName
 */
export const getDatasetVersion = async (datasetId, versionName) => {
  return await maHttp.get(
    {
      url: `datasets/versions/getDatasetVersionByDatasetId/${datasetId}/${versionName}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

/**
 * 数据集版本公开
 * @param datasetId
 * @param versionName
 */
export const publishDatasetVersion = async (datasetId, versionName) => {
  return await maHttp.put(
    {
      url: `datasets/versions/publish/${datasetId}/${versionName}`,
      headers: {},
    },
    {
      urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET,
    },
  );
};

/**
 * 数据集版本取消公开
 * @param datasetId
 * @param versionName
 */
export const cancelPublicDatasetVersion = async (datasetId, versionName) => {
  return await maHttp.put(
    {
      url: `datasets/versions/cancelPublish/${datasetId}/${versionName}`,
      headers: {},
    },
    {
      urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET,
    },
  );
};

export const getTemplateLabels = async () => {
  return await maHttp.get(
    {
      url: `labelTemplate`,
      headers: {},
    },
    {
      urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET,
    },
  );
};

export const exportDatasetVersion = async (datasetId, versionName) => {
  return await maHttp.post(
    {
      url: `datasets/versions/datasetExport/${datasetId}/${versionName}`,
      headers: {},
    },
    {
      urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET,
    },
  );
};

export const checkExportStatus = async (datasetVersionId: number) => {
  return await maHttp.get(
    {
      url: `datasets/versions/checkTaskStatus`,
      params: {
        datasetVersionId: datasetVersionId,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const recordDatasetDownload = async () => {
  return await maHttp.post(
    {
      url: `datasets/versions/recordDatasetDownload`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const exportDatasetFromOrigin = async (datasetId: number) => {
  return await maHttp.post(
    {
      url: `datasets/versions/datasetExportFromOrigin/${datasetId}`,
      headers: {},
    },
    {
      urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET,
    },
  );
};

export const checkOriginExportStatus = async (datasetId: number) => {
  return await maHttp.get(
    {
      url: `datasets/versions/checkOriginExportStatus`,
      params: {
        datasetId: datasetId,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const getOriginExportUrl = async (datasetId: number) => {
  return await maHttp.get(
    {
      url: `datasets/versions/getOriginExportUrl`,
      params: {
        datasetId: datasetId,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 从origin导出未标注的数据
export const exportUnannotatedDatasetFromOrigin = async (datasetId: number) => {
  return await maHttp.get(
    {
      url: `datasets/versions/exportUnannotatedFromOrigin`,
      params: {
        datasetId: datasetId,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 检查从origin导出未标注数据的任务状态
export const checkOriginUnannotatedExportStatus = async (datasetId: number) => {
  return await maHttp.get(
    {
      url: `datasets/versions/checkOriginUnannotatedExportStatus`,
      params: {
        datasetId: datasetId,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 获取从origin导出未标注数据的下载 URL
export const getOriginUnannotatedExportUrl = async (datasetId: number) => {
  return await maHttp.get(
    {
      url: `datasets/versions/getOriginUnannotatedExportUrl`,
      params: {
        datasetId: datasetId,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export function generateRandomHexColor() {
  // 生成随机的RGB值 (0-255)
  const r = Math.floor(Math.random() * 256);
  const g = Math.floor(Math.random() * 256);
  const b = Math.floor(Math.random() * 256);

  // 将RGB值转换为16进制，并确保每个值都是两位数（补0）
  const hexR = r.toString(16).padStart(2, '0');
  const hexG = g.toString(16).padStart(2, '0');
  const hexB = b.toString(16).padStart(2, '0');

  // 返回带#号的16进制颜色
  return `#${hexR}${hexG}${hexB}`;
}

export const getDatasetById = async (datasetId) => {
  return await maHttp.get(
    {
      url: `datasets/${datasetId}`,
      headers: {},
    },
    {
      urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET,
    },
  );
};

export const getDatasetVersionLabels = async (datasetId, versionName) => {
  return await maHttp.post(
    {
      url: `datasets/versions/getDatasetVersionLabels`,
      data: {
        datasetId: datasetId,
        versionName: versionName,
      },
      headers: {},
    },
    {
      urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET,
    },
  );
};

export const getLabelsByModelGenerationIdApi = async (modelGenerationId) => {
  return await maHttp.get(
    {
      url: `modelGeneration/getVersionLabelsByModelGenerationId/${modelGenerationId}`,
      headers: {},
    },
    {
      urlPrefix: MaBackendUrlEnum.MODEL_MANAGER,
    },
  );
};

export const getLabelsByJobNameApi = async (jobName) => {
  return await maHttp.get(
    {
      url: `modelGeneration/getVersionLabelsByJobName`,
      params: {
        jobName: jobName,
      },
      headers: {},
    },
    {
      urlPrefix: MaBackendUrlEnum.MODEL_MANAGER,
    },
  );
};

export const getLabelsByImageUrlApi = async (imageUrl) => {
  return await maHttp
    .get(
      {
        url: `modelVersionDefaultLabels/getLabelsByImageUrl`,
        params: {
          imageUrl: imageUrl,
        },
        headers: {},
      },
      {
        urlPrefix: MaBackendUrlEnum.MODEL_MANAGER,
      },
    )
    .then((res) => {
      return res.map((item) => {
        return {
          name: item,
        };
      });
    });
};

export const getLabelsIByImageUrlApiAndDatasetId = async (imageUrl, datasetId) => {
  return await maHttp
    .get(
      {
        url: `modelVersionDefaultLabels/getLabelsByImageUrlAndDatasetId`,
        params: {
          imageUrl: imageUrl,
          datasetId: datasetId,
        },
        headers: {},
      },
      {
        urlPrefix: MaBackendUrlEnum.MODEL_MANAGER,
      },
    )
    .then((res) => {
      return res.map((item) => {
        return {
          name: item,
        };
      });
    });
};

export const getDatasetEventPage = async (page, datasetId) => {
  return await maHttp.get(
    {
      url: `datasets/dataset-operations`,
      params: {
        current: page.current, // 当前页码
        size: page.size, // 每页条数
        datasetId: datasetId, // 传递数据集ID作为查询条件
      },
      headers: {},
    },
    {
      urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET, // API请求的前缀
    },
  );
};

export const checkDatasetBindRelationWithGenerationApi = async (datasetIds) => {
  return await maHttp.post(
    {
      url: `modelGeneration/checkDatasetBindRelationWithGeneration`,
      data: {
        ids: datasetIds,
      },
      headers: {},
    },
    {
      urlPrefix: MaBackendUrlEnum.MODEL_MANAGER,
    },
  );
};

// 标准化用, 根据jobName查相关信息
export const getJobAccLossAndImgCountsByJobName = async (jobName) => {
  return await maHttp.get(
    {
      url: `modelGeneration/getJobAccLossAndImgCountsByJobName`,
      params: {
        jobName: jobName,
      },
      headers: {},
    },
    {
      urlPrefix: MaBackendUrlEnum.MODEL_MANAGER,
    },
  );
};
// 引导式用, 引导式一个generation -> 唯一job
export const getJobAccLossAndImgCountsByGuidedGenerationId = async (modelGenerationId) => {
  return await maHttp.get(
    {
      url: `modelGeneration/getJobAccLossAndImgCountsByGuidedGenerationId`,
      params: {
        modelGenerationId: modelGenerationId,
      },
      headers: {},
    },
    {
      urlPrefix: MaBackendUrlEnum.MODEL_MANAGER,
    },
  );
};

export const batchRecoverDataset = async (datasetIds) => {
  return await maHttp.post(
    {
      url: `datasets/batchRecoverDataset`,
      data: {
        ids: datasetIds,
      },
      headers: {},
    },
    {
      urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET,
    },
  );
};

export const deleteDatasetGroup = async (datasetGroupId) => {
  return await maHttp.delete(
    {
      url: `datasets/group/deleteDatasetGroup/${datasetGroupId}`,
      headers: {},
    },
    {
      urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET,
    },
  );
};

export const checkDatasetVersionBindRelationWithGenerationApi = async (versionID: number) => {
  return await maHttp.get(
    {
      url: `modelGeneration/checkDatasetVersionBindRelationWithGeneration`,
      headers: {},
      params: {
        versionID: versionID,
      },
    },
    {
      urlPrefix: MaBackendUrlEnum.MODEL_MANAGER,
    },
  );
};

export const getAllLabelTemplate = async (type?: number): Promise<{ id: number; name: string; displayName?: string; annotationName?: string; shape?: string }[]> => {
  return await maHttp.get(
    {
      url: `labelTemplate`,
      params: type !== undefined ? { type } : {},
      headers: {},
    },
    {
      urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET,
    },
  );
};

/**
 * 获取点云标签库（独立表 label_template_pointcloud）的全部标签
 * 展示中文名称 displayName，标注值用 annotationName
 */
export const getAllLabelTemplatePointcloud = async (): Promise<{ id: number; name: string; displayName?: string; annotationName?: string; shape?: string; color?: string }[]> => {
  return await maHttp.get(
    {
      url: `labelTemplatePointcloud`,
      headers: {},
    },
    {
      urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET,
    },
  );
};

/**
 * 获取视频标签库（独立表 label_template_video）的全部标签
 * 视频标注工作台专用标签来源，字段与点云标签一致（展示中文 displayName，标注值用 annotationName）
 */
export const getAllLabelTemplateVideo = async (): Promise<{ id: number; name: string; displayName?: string; annotationName?: string; shape?: string; color?: string }[]> => {
  return await maHttp.get(
    {
      url: `labelTemplateVideo`,
      headers: {},
    },
    {
      urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET,
    },
  );
};

// ===== 视频标注 =====
export const openVideoAnnotationTask = async (datasetId: number, fileId: number) => {
  return await maHttp.post(
    { url: `video/datasets/${datasetId}/files/${fileId}/annotation/tasks`, headers: {} },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const loadVideoAnnotation = async (datasetId: number, fileId: number) => {
  return await maHttp.get(
    { url: `video/datasets/${datasetId}/files/${fileId}/annotation`, headers: {} },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const createVideoTrack = async (data) => {
  return await maHttp.post(
    { url: `video/tracks`, data, headers: {} },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const createVideoKeyframe = async (trackId: number, data) => {
  return await maHttp.post(
    { url: `video/tracks/${trackId}/keyframes`, data, headers: {} },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const updateVideoKeyframe = async (keyframeId: number, data) => {
  return await maHttp.put(
    { url: `video/keyframes/${keyframeId}`, data, headers: {} },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const deleteVideoKeyframe = async (keyframeId: number) => {
  return await maHttp.delete(
    { url: `video/keyframes/${keyframeId}`, headers: {} },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const finishVideoTrack = async (trackId: number) => {
  return await maHttp.post(
    { url: `video/tracks/${trackId}/finish`, headers: {} },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const deleteVideoTrack = async (trackId: number) => {
  return await maHttp.delete(
    { url: `video/tracks/${trackId}`, headers: {} },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const submitVideoAnnotation = async (fileId: number) => {
  return await maHttp.post(
    { url: `video/files/${fileId}/annotation/submit`, headers: {} },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const reviewVideoTask = async (taskId: number) => {
  return await maHttp.post(
    { url: `video/tasks/${taskId}/review`, headers: {} },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const rejectVideoTask = async (taskId: number) => {
  return await maHttp.post(
    { url: `video/tasks/${taskId}/reject`, headers: {} },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const listVideoTasks = async (datasetId: number) => {
  return await maHttp.get(
    { url: `video/datasets/${datasetId}/tasks`, headers: {} },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

export const exportVideoAnnotation = async (fileId: number, format: string) => {
  return await maHttp.get(
    { url: `video/files/${fileId}/annotation/export`, params: { format }, headers: {} },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};
