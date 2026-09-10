import { defHttp, maHttp } from '/@/utils/http/axios';
import { DemoOptionsItem, selectParams } from './model/optionsModel';

enum Api {
  OPTIONS_LIST = '/select/getDemoOptions',
}

/**
 * @description: Get sample options value
 */
export const optionsListApi = (params?: selectParams) =>
  defHttp.get<DemoOptionsItem[]>({ url: Api.OPTIONS_LIST, params });

export const msOptionsListApi = (params) =>
  maHttp.get<DemoOptionsItem[]>(
    {
      url: params.url,
      params: params.params,
    },
    params.options,
  );

export const msPostOptionsListApi = (params) =>
  maHttp.post<DemoOptionsItem[]>(
    {
      url: params.url,
      params: params.params,
    },
    params.options,
  );
