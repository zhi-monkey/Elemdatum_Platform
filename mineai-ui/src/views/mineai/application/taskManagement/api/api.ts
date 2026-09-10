import { maHttp } from '/@/utils/http/axios';
import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';

export async function startAlterAndPack(
  filePath: string,
  appZipPath: string,
  datasetId: number,
  datasetName: string,
  jobId: number,
  authCode: string,
) {
  return await maHttp.get(
    {
      url: 'modelApplication/startAlterAndPack',
      params: {
        filePath: filePath,
        appZipPath: appZipPath,
        datasetId: datasetId,
        versionName: datasetName,
        jobId,
        authCode: authCode,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export async function getModelApplicationList(params) {
  return await maHttp
    .get(
      {
        url: 'modelApplication/findAll',
        params,
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then(async (v) => {
      // console.log(v);
      v.items = v.content;
      v.total = v.totalElements;
      // console.log(v);
      // Fetch labels for each item
      const itemsWithLabels = await Promise.all(
        v.items.map(async (item) => {
          if (item.device) {
            item.device.deviceAndFirmwareName =
              item.device.deviceName + '-' + item.device.firmwareVersion;
          }
          if (item.applicableScene.length > 0) {
            item.allApplicableSceneName = item.applicableScene.map((e) => e.name).join('; ');
          }
          if (item.appZipPath) {
            const fullName = item.appZipPath.split('/').pop();
            const namePart = fullName.split('-')[0]; // 获取 "a"
            const extensionPart = fullName.split('.').pop(); // 获取 "zip"
            item.appZipName = `${namePart}.${extensionPart}`;
          }
          // 用于编辑model中applicationName的复显
          item.applicationNameId = item.applicationName.id;

          // 获取算法包大小并展示
          item.appZipSizeFormatted = `${(item.appZipSize / 1024 / 1024).toFixed(2)} MB`; // 格式化为 MB 显示
          // Fetch labels for this item
          try {
            item.labels = await getLabelByModelApplicationId(item.id);
          } catch (error) {
            console.warn(`Failed to fetch labels for application ${item.id}:`, error);
            item.labels = [];
          }
          return item;
        }),
      );
      v.items = itemsWithLabels;
      // console.log(v.items);
      return v;
    });
}

export async function getModelApplicationListForReal(params) {
  return await maHttp
    .get(
      {
        url: 'modelApplication/findAllForReal',
        params,
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((v) => {
      // console.log(v);
      v.items = v.content;
      v.total = v.totalElements;
      // console.log(params.modelName);
      v.items.forEach((item) => {
        if (item.device) {
          item.device.deviceAndFirmwareName =
            item.device.deviceName + ' - ' + item.device.firmwareVersion;
        }
        if (item.applicableScene.length > 0) {
          item.allApplicableSceneName = item.applicableScene.map((e) => e.name).join('; ');
        }
        if (item.appZipPath) {
          const fullName = item.appZipPath.split('/').pop();
          const namePart = fullName.split('-')[0]; // 获取 "a"
          const extensionPart = fullName.split('.').pop(); // 获取 "zip"
          item.appZipName = `${namePart}.${extensionPart}`;
        }
        // 用于编辑model中applicationName的复显
        item.applicationNameId = item.applicationName.id;

        // 获取算法包大小并展示
        item.appZipSizeFormatted = `${(item.appZipSize / 1024 / 1024).toFixed(2)} MB`; // 格式化为 MB 显示
        // if (item.appZipSize) {
        //   item.appZipSizeFormatted = `${(item.appZipSize / 1024 / 1024).toFixed(2)} MB`;
        // } else {
        //   // 如果未获取到文件大小，显示一个默认信息
        //   item.appZipSizeFormatted = '0.0MB';
        // }
      });
      // console.log(v.items);
      return v;
    });
}

export async function getModelApplicationTaskList(params) {
  return await maHttp
    .get(
      {
        url: 'modelApplication/findAllSubtask',
        params,
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    )
    .then((v) => {
      // console.log(v);
      // v.items = v.content.filter(
      //   (item) =>
      //     item.isReleased === '已发布' && item.applicationName.applicationName === params.modelName,
      // );
      // const pageSize = params.pageSize; // 与api中设置的pageSize保持一致
      // const page = params.page; // 当前页码
      // const startIndex = page * pageSize;
      // const endIndex = (page + 1) * pageSize - 1;
      //
      // // 使用slice获取指定范围的记录
      // v.items = v.items.slice(startIndex, endIndex);
      // v.total = v.items.legnth;
      // console.log(startIndex, endIndex);
      console.log(v);
      v.items = v.content;
      v.total = v.totalElements;
      v.items.forEach((item) => {
        if (item.device) {
          item.device.deviceAndFirmwareName =
            item.device.deviceName + ' - ' + item.device.firmwareVersion;
        }
        if (item.applicableScene.length > 0) {
          item.allApplicableSceneName = item.applicableScene.map((e) => e.name).join('; ');
        }
        if (item.appZipPath) {
          const fullName = item.appZipPath.split('/').pop();
          const namePart = fullName.split('-')[0]; // 获取 "a"
          const extensionPart = fullName.split('.').pop(); // 获取 "zip"
          item.appZipName = `${namePart}.${extensionPart}`;
        }
        // 用于编辑model中applicationName的复显
        item.applicationNameId = item.applicationName.id;

        // 获取算法包大小并展示
        item.appZipSizeFormatted = `${(item.appZipSize / 1024 / 1024).toFixed(2)} MB`; // 格式化为 MB 显示
        // if (item.appZipSize) {
        //   item.appZipSizeFormatted = `${(item.appZipSize / 1024 / 1024).toFixed(2)} MB`;
        // } else {
        //   // 如果未获取到文件大小，显示一个默认信息
        //   item.appZipSizeFormatted = '0.0MB';
        // }
      });
      // console.log(v.items);
      return v;
    });
}

// 添加和编辑应用
export async function addAndUpdateModelApplication(data: any) {
  return await maHttp.post(
    {
      url: `modelApplication/save`,
      data,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
  // console.log(data);
}

// 删除应用，根据id删除
export async function deleteModelApplication(id: number) {
  return await maHttp.delete(
    {
      url: `modelApplication/delete/${id}`,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export async function setReleasedState(id: number) {
  return await maHttp.post(
    {
      url: `modelApplication/bindReleased/${id}`,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export async function unsetReleasedState(id: number) {
  return await maHttp.post(
    {
      url: `modelApplication/unbindReleased/${id}`,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export async function checkModelApplicationCanUnbind(id: number) {
  return await maHttp.get(
    {
      url: `modelApplication/checkUnbindReleased/${id}`,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export async function getSceneTreeData() {
  return await maHttp.get(
    {
      url: 'scene/findAll',
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export async function getSceneListByIds(ids: number[]) {
  return await maHttp.get(
    {
      url: 'scene/findSceneListByIds',
      params: {
        ids: ids,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export async function getDeviceTreeData() {
  return await maHttp.get(
    {
      url: 'device/findAll',
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export async function getDeviceById(id: number) {
  return await maHttp.get(
    {
      url: `device/findDeviceById`,
      params: {
        id: id,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export async function getModelTreeData() {
  return await maHttp.get(
    {
      url: 'model/getAllModelInfo',
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export async function getModelById(id: number) {
  return await maHttp.get(
    {
      url: `model/findModelById`,
      params: {
        id: id,
      },
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}

export async function checkPackStatus(taskId: string) {
  return await maHttp.get(
    {
      url: `package/checkTaskStatus/`,
      params: {
        taskId: taskId,
      },
    },
    { urlPrefix: MaBackendUrlEnum.PACKAGE_MANAGER },
  );
}

export async function getTaskDetail(taskId: string) {
  return await maHttp.get(
    {
      url: `package/getTaskDetail`,
      params: {
        taskId: taskId,
      },
    },
    { urlPrefix: MaBackendUrlEnum.PACKAGE_MANAGER },
  );
}

/**
 * @param taskId 任务的id
 */
export async function downloadAndDelete(taskId: string): Promise<{
  success: boolean;
  message?: string;
}> {
  try {
    const response = await maHttp.get(
      {
        url: 'package/getPackedFile',
        responseType: 'blob',
        params: { taskId },
      },
      {
        isReturnNativeResponse: true,
        isTransformResponse: false,
        urlPrefix: MaBackendUrlEnum.PACKAGE_MANAGER,
      },
    );

    if (!response.data) {
      return { success: false, message: '下载失败, 获取的文件流为空' };
    }

    // 从响应头获取文件名
    const contentDisposition = response.headers['content-disposition'];
    let fileName = 'default_filename';

    if (contentDisposition) {
      // 优先处理 RFC 5987 编码格式 (filename*=)
      const rfc5987Match = contentDisposition.match(/filename\*=([^;]+)/i);
      if (rfc5987Match && rfc5987Match[1]) {
        // 格式: UTF-8''model.bmodel
        const parts = rfc5987Match[1].split("'");
        if (parts.length >= 3) {
          // 提取最后一部分作为文件名
          fileName = decodeURIComponent(parts[parts.length - 1]);
        }
      } else {
        // 回退处理标准格式 (filename=)
        const fileNameMatch = contentDisposition.match(/filename="?([^";]+)"?/i);
        if (fileNameMatch && fileNameMatch[1]) {
          fileName = decodeURIComponent(fileNameMatch[1]);
        }
      }
    }

    // 创建下载链接
    const blob = new Blob([response.data]);
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);

    return { success: true };
  } catch (error) {
    console.error('Error fetching file:', error);
    return {
      success: false,
      message: error instanceof Error ? error.message : 'Unknown error',
    };
  }
}

export async function startDownLoad(modelApplicationId: number, authCode: string) {
  return await maHttp.get(
    {
      url: `modelApplication/startPacking/${modelApplicationId}`,
      params: {
        authCode: authCode,
      },
    },
    {
      urlPrefix: MaBackendUrlEnum.MODEL_MANAGER,
    },
  );
}

/**
 * 调用浏览器的download
 * @param {*} data Bolb格式文件
 * @param {*} filename 文件名
 * @param {*} mime 暂时不知道用处
 * @param {*} bom 暂时不知道用处
 */
export function downloadByData(data: BlobPart, filename: string, mime?: string, bom?: BlobPart) {
  const blobData = typeof bom !== 'undefined' ? [bom, data] : [data];
  const blob = new Blob(blobData, { type: mime || 'application/octet-stream' });
  const blobURL = window.URL.createObjectURL(blob);
  const tempLink = document.createElement('a');
  tempLink.style.display = 'none';
  tempLink.href = blobURL;
  tempLink.setAttribute('download', filename);
  if (typeof tempLink.download === 'undefined') {
    tempLink.setAttribute('target', '_blank');
  }
  document.body.appendChild(tempLink);
  tempLink.click();
  document.body.removeChild(tempLink);
  window.URL.revokeObjectURL(blobURL);
}

export function getLabelByModelApplicationId(modelApplicationId: number) {
  return maHttp.get(
    {
      url: `modelApplicationZipLabel`,
      params: {
        modelApplicationId: modelApplicationId,
      },
    },
    {
      urlPrefix: MaBackendUrlEnum.MODEL_MANAGER,
      joinTime: false,
    },
  );
}

export async function checkIfApplicationExistsApi(data: any) {
  return await maHttp.post(
    {
      url: `modelApplication/checkIfApplicationExists`,
      data,
    },
    { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
  );
}
