import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum, MaBackendUrlEnum } from '/@/enums/mineaiEnum';

// 查询角色人数统计和总人数
export async function getRoleUserCountsAndTotalCount() {
  return await maHttp.get(
    {
      url: '/users/roleCountsAndTotal',
      params: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

// 查询在线用户数量
export async function getOnlineUsersCount() {
  return await maHttp.get(
    {
      url: '/users/getOnlineUserStats',
      params: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

//获得所有公开的数据集列表
export const getAllPublicDatasets = async (params) => {
  return await maHttp.get(
    {
      url: 'datasets/getAllPublicDatasets',
      params,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
};

// 获取数据集总数（公开+私有）
export async function getAllDatasetCount() {
  return await maHttp.get(
    {
      url: 'datasets/getAllDatasetCount',
      params: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
}

//获取应用任务列表
export async function getModelApplicationList(params) {
  return await maHttp.get(
    {
      url: 'modelApplication/findAll',
      params,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

// 获取算法应用数量（只返回数量）
export async function getModelApplicationCount() {
  return await maHttp.get(
    {
      url: 'modelApplication/countAll',
      params: {},
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}
export async function getChipCount() {
  return await maHttp.get(
    {
      url: 'chips/chipCount',
      params: {},
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

// 获取未删除作业数量
export async function countGenerationWithNoDeleted() {
  return await maHttp.get(
    {
      url: 'modelGeneration/countGenerationWithNoDeleted',
      params: {},
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

// 获取基础算法数量
export async function getBaseModelCount(params) {
  return await maHttp.get(
    {
      url: 'model/getBaseModelCount',
      params,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

//获取所有标签组的信息
export async function getLabelGroupList(params: any) {
  return await maHttp.get(
    {
      url: 'labelGroup/getAll',
      params,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
}

// 获取所有作业（训练任务）列表（带用户信息）
export const getModelJobWithUserList = async (params) => {
  return await maHttp.get(
    {
      url: 'modelJob/getJobWithUser',
      params,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

export async function getOverviewTrainingStats() {
  return await maHttp.get(
    {
      url: `/modelJob/overviewTrainingStats`,
      headers: {},
    },
    {
      urlPrefix: MaBackendUrlEnum.MODEL_MANAGER,
    },
  );
}

// 获取集群内存汇总信息（总量、已用、使用率）
export async function getClusterMemorySummary() {
  return await maHttp.get(
    {
      url: 'api/k8s/cluster/memory/summary',
    },
    {
      urlPrefix: MaBackendUrlEnum.MODEL_MANAGER,
    },
  );
}

// 获取集群磁盘汇总信息（总量、已用、使用率）
export async function getClusterDiskSummary() {
  return await maHttp.get(
    {
      url: 'api/k8s/cluster/disk/summary',
    },
    {
      urlPrefix: MaBackendUrlEnum.MODEL_MANAGER,
    },
  );
}

// 获取所有 GPU 卡的最近使用率时间序列
export async function getRecentGpuMetrics(params) {
  return await maHttp.get(
    {
      url: 'api/k8s/cluster/gpu/metrics/recent',
      params,
    },
    {
      urlPrefix: MaBackendUrlEnum.MODEL_MANAGER,
    },
  );
}
