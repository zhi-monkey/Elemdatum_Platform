import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

// 定义部门接口
interface Department {
  id: number;
  name: string;
  description: string;
  createTime: string;
}

// 定义查询参数接口
interface QueryParams {
  page: number;
  pageSize: number;
  name?: string;
  order?: string;
  sort?: string;
}

export async function isDepartmentNameExists(name: string) {
  const state = await maHttp.get(
    {
      url: `departments/isnameexist/${name}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
  return state;
}
// 查询部门列表
export async function queryDepartments(params: QueryParams) {
  const response = await maHttp.get<{
    result: Department[];
    page: { total: number };
  }>(
    {
      url: 'departments',
      params,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );

  return {
    items: response.result,
    total: response.page.total,
  };
}

// 删除部门
export async function deleteDepartment(id: number) {
  return await maHttp.delete(
    {
      url: `departments/${id}`,
      headers: {},
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
  );
}

// 创建或更新部门
export async function saveOrUpdateDepartment(department: Omit<Department, 'createTime'>) {
  const urlPrefix = { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN };

  if (department.id) {
    // 如果存在 id，则执行更新操作
    return await maHttp.post(
      {
        url: 'departments',
        data: department,
        headers: {},
      },
      urlPrefix,
    );
  } else {
    // 如果不存在 id，则执行创建操作
    return await maHttp.post(
      {
        url: 'departments',
        data: department,
        headers: {},
      },
      urlPrefix,
    );
  }
}
