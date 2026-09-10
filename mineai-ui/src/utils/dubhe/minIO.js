import { maHttp } from '/@/utils/http/axios';
import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';

// 新增配置获取方法
async function getMinIOConfig() {
  return await maHttp.post(
    {
      url: 'params/minioConfig',
      params: {
        hostIp: window.location.host,
      },
      headers: {
        // @ts-ignore
        ignoreCancelToken: true,
      },
    },
    { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
  );
}

// 优先读取后端按访问入口下发的配置；接口异常时使用环境变量兜底，不能阻断路由初始化。
let minIOConfigResponse;
try {
  minIOConfigResponse = await getMinIOConfig();
} catch (error) {
  console.error('[MinIO配置] 动态配置接口请求失败，已回退到前端环境变量：', error);
  minIOConfigResponse = {
    endPoint: import.meta.env.VITE_MINIO_ENDPOINT,
    port: import.meta.env.VITE_MINIO_PORT,
    bucketName: import.meta.env.VITE_MINIO_BUCKETNAME,
    prefix: '',
  };
}

// 组装 minIO 配置信息
export const minIOConfig = {
  config: {
    endPoint: minIOConfigResponse.endPoint,
    port: Number(minIOConfigResponse.port),
    useSSL: import.meta.env.VITE_MINIO_USESSL === true || import.meta.env.VITE_MINIO_USESSL === 'true',
  },
  bucketName: minIOConfigResponse.bucketName,
};

// 导出 bucketName
export const { bucketName } = minIOConfig;

// 更新 bucketHost 逻辑；根据配置决定协议，避免 HTTPS 页面产生混合内容请求。
const minioProtocol = minIOConfig.config.useSSL ? 'https:' : 'http:';
export const bucketHost = `${minioProtocol}//${minIOConfig.config.endPoint}:${minIOConfig.config.port}`;

const minioPrefix = minIOConfigResponse.prefix || '';
export const minioBaseUrl = `${bucketHost}${minioPrefix}/${bucketName}`;
