import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

export async function list(params) {
  return await maHttp.get(
    { url: `authCode`, params },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

export async function add(data) {
  return await maHttp.post(
    {
      url: `authCode`,
      data,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

export async function del(ids) {
  return await maHttp.delete(
    {
      url: `authCode`,
      data: { ids },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

export async function edit(data) {
  return await maHttp.put(
    {
      url: `authCode`,
      data,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

// 不分页的列表接口，用于角色权限管理
export async function getAuthCodeList() {
  return await maHttp.get(
    {
      url: `authCode/list`,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

export async function getPermissionTree() {
  return await maHttp.get(
    {
      url: `permission/tree`,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}
