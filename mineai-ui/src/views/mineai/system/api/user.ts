import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

export async function list(params) {
  return await maHttp.get(
    {
      url: `users`,
      params,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

export async function add(data) {
  return await maHttp.post(
    {
      url: `users`,
      data,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

export async function updatePassByAdmin(userId, data) {
  return await maHttp.post(
    {
      url: `user/${userId}/updatePassByAdmin`,
      data,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

export async function del(ids) {
  return await maHttp.delete(
    {
      url: `users`,
      data: { ids },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

export async function edit(data) {
  return await maHttp.put(
    {
      url: `users`,
      data,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

// 根据用户名模糊搜索用户列表
export async function findByNickName() {
  return await maHttp.get(
    {
      url: `users/findByNickName`,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

// 获取用户配置信息
export async function getUserConfig(userId) {
  return await maHttp.get(
    {
      url: `users/getUserConfig`,
      params: { userId },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

// 更改用户配置信息
export async function submitUserConfig(data) {
  return await maHttp.put(
    {
      url: `users/setUserConfig`,
      data,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

export async function getVisUserInfo() {
  return await maHttp.get(
    {
      url: `users/decryptVisUser`,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

// 更新用户密码
export async function updatePass(data) {
  return await maHttp.post(
    {
      url: `user/updatePass`,
      data,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

export async function updateUserInfo(data) {
  return await maHttp.put(
    {
      url: `user/info`,
      data,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

export async function getUserInfo() {
  return await maHttp.get(
    {
      url: `user/info`,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}
