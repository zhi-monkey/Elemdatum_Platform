import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

export function getMenusTree() {
  return maHttp.get(
    {
      url: `menus/tree`,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

export function list(params) {
  return maHttp.get(
    {
      url: `menus`,
      params,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

export function add(data) {
  return maHttp.post(
    {
      url: `menus`,
      data,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

export function del(ids) {
  return maHttp.delete(
    {
      url: `menus`,
      data: { ids },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

export function edit(data) {
  return maHttp.put(
    {
      url: `menus`,
      data,
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}
