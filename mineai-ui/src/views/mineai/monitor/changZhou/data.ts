import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum, MaBackendUrlEnum } from '/@/enums/mineaiEnum';

// 获取最近的GPU使用率
export async function getRecentGpuUsage() {
  return await maHttp.get(
    {
      url: '/prometheus/getRecentGpuUsage',
      params: {},
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

// 获取GPU资源，仅返回当前时间点的信息
export async function getGpuResource() {
  return await maHttp.get(
    {
      url: '/prometheus/getGpuResource',
      params: {},
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}
//获取用户资源使用情况
export async function getUserResourceUsed() {
  return await maHttp.get(
    {
      url: '/users/getUserResourceUsed',
      params: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

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
export async function getOnlineUserStats() {
  return await maHttp.get(
    {
      url: '/users/getOnlineUserStats',
      params: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

// 查询已启用用户数量
export async function getEnabledUserStats() {
  return await maHttp.get(
    {
      url: '/users/getEnableUserStats',
      params: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}
//获取算法应用商城的信息
export async function getModelApplicationListForReal(params) {
  return await maHttp.get(
    {
      url: 'modelApplication/findAllForReal',
      params,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}
//获得算力资源信息
export const getResourceData = async () => {
  return await maHttp.get(
    {
      url: `prometheus/getComputingResource`,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

// 获取未删除作业数量
export async function getModelJobCountWithNoDeleted() {
  return await maHttp.get(
    {
      url: 'modelJob/getJobCountWithNoDeleted',
      params: {},
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
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

//获得集群信息
export const getClusterData = async () => {
  return await maHttp.get(
    {
      url: 'api/k8s/cluster/details',
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

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

// 获取所有作业（训练任务）列表
export const getModelJobList = async (params) => {
  return await maHttp.get(
    {
      url: 'modelJob/getJobWithNoDeleted',
      params,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

// 获取所有作业（训练任务）列表
export const getModelJobWithUserList = async (params) => {
  return await maHttp.get(
    {
      url: 'modelJob/getJobWithUser',
      params,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
};

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
