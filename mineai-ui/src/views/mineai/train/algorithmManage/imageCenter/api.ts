import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
export async function dynamicFindModelVersionPage(
  page = 0,
  pageSize = 15,
  order = 'desc',
  vagueInfo?: string,
  chipType?: string,
  showName?: string,
  description?: string,
  userName?: string,
) {
  return await maHttp.get(
    {
      url: 'modelVersion/dynamicFindModelVersionPage',
      params: {
        page,
        pageSize,
        order,
        vagueInfo,
        chipType,
        showName,
        description,
        userName,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}
