import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

export function list(params) {
  return maHttp.get(
    {
      url: `roles`,
      params,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

// 获取所有的Role
export function getAll() {
  return maHttp.get(
    {
      url: `roles/all`,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

export function add(data) {
  return maHttp.post(
    {
      url: `roles`,
      data,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

export function get(id) {
  return maHttp.get(
    {
      url: `roles/${id}`,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

export function del(ids) {
  return maHttp.delete(
    {
      url: `roles`,
      data: { ids },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

export function edit(data) {
  return maHttp.put(
    {
      url: `roles`,
      data,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

export function editMenu(data) {
  return maHttp.put(
    {
      url: `roles/menu`,
      data,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

export function editOperations(data) {
  return maHttp.put(
    {
      url: `roles/auth`,
      data,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

