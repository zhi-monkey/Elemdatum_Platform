<template>
  <!-- 模型下载卡片 -->
  <a-card v-if="!hideModelDownload" title="模型下载" style="margin-bottom: 20px">
    <a-table :dataSource="normalModelData" :pagination="false">
      <a-table-column title="文件名" dataIndex="fileName" key="fileName" />
      <a-table-column title="文件大小" dataIndex="fileSize" key="fileSize" />
      <a-table-column title="操作" key="action">
        <template #default="{ record }">
          <a-button type="link" @click="downloadFile(record.originalName)"> 下载</a-button>
        </template>
      </a-table-column>
    </a-table>
  </a-card>

  <!-- 算法包下载卡片 -->
  <a-card title="算法应用部署包下载" style="margin-bottom: 100px">
    <a-table :dataSource="convertedModelData" :pagination="false">
      <a-table-column title="文件名" dataIndex="fileName" key="fileName" />
      <a-table-column title="文件大小" dataIndex="fileSize" key="fileSize" />
      <a-table-column title="操作" key="action">
        <template #default="{ record }">
          <a-button type="link" @click="packAndDownloadFile(record.originalName)"> 下载</a-button>
        </template>
      </a-table-column>
    </a-table>
  </a-card>

  <a-modal
    v-model:visible="packingStatusModelVisible"
    title="打包"
    :cancelText="isInputAuthCode ? '返回' : '隐藏'"
    :confirmText="isInputAuthCode ? '确认' : '开始打包'"
    @ok="confirmPack"
    @cancel="cancelPack"
  >
    <!-- 如果正在打包，显示加载组件；否则，显示确认开始打包文字 -->
    <div class="modal-content">
      <a-spin v-if="isPolling" size="large" tip="正在打包，请稍候..." />
      <div v-else-if="!isInputAuthCode" class="center-text">确定开始打包任务？</div>
      <div v-else class="auth-code-input">
        <div class="auth-code-tip">
          请确认该算法是否需要授权码，如果需要请联系管理员获取，并在下方输入，若不需要授权码，则直接点击确认执行打包即可。
        </div>
        <div class="auth-code-form">
          <div class="form-label">授权码：</div>
          <a-input-password v-model:value="authCode" placeholder="请输入授权码（可选）" />
        </div>
      </div>
    </div>
  </a-modal>
</template>

<script setup lang="ts">
  import { onMounted, ref } from 'vue';
  import {
    Button as AButton,
    Card as ACard,
    Table as ATable,
    TableColumn as ATableColumn,
    Modal as AModal,
    Spin as ASpin,
    InputPassword as AInputPassword,
  } from 'ant-design-vue';
  import {
    downloadWeightFilesByFileName,
    getModelFileApi,
  } from '/@/views/mineai/train/generationGuideList/generationGuideList.data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import {
    checkPackStatus,
    downloadAndDelete,
    getTaskDetail,
    startAlterAndPack,
  } from '/@/views/mineai/application/taskManagement/api/api';

  const { createMessage } = useMessage();
  const props = defineProps<{
    appZipPath: string;
    jobId: number;
    datasetId: number;
    datasetVersionName: string;
    hideModelDownload?: boolean;
  }>();

  // 添加授权码相关状态
  const isInputAuthCode = ref(false);
  const authCode = ref('');

  // 定义普通模型数据（.pt 和 .onnx）
  const normalModelData = ref<any[]>([]);

  // 定义转换模型数据（需要打包的算法包）
  const convertedModelData = ref<any[]>([]);

  /**
   * 下载文件
   * @param filePath 不带datasetRootPath的文件路径 /weight/job-local-cz-449/best.onnx
   */
  const downloadFile = async (filePath: string) => {
    // 截掉第一个 /weight
    filePath = filePath.substring(filePath.indexOf('/weight') + '/weight'.length); // /job-local-cz-449/best.onnx
    // 在这里添加下载逻辑
    const { success, message } = await downloadWeightFilesByFileName(filePath);
    if (success) {
      createMessage.success('下载文件成功, 请注意查收');
    } else {
      createMessage.error('下载文件失败' + message);
    }
  };

  // const packAndDownloadFile = async (filePath: string) => {
  //   console.log(`packAndDownloading ${filePath}`);
  //   console.log(`appZipPath ${props.appZipPath}`);
  // };
  let timer = ref(null);
  const isPolling = ref(false);
  let packingStatusModelVisible = ref(false);
  let currentFilePath = ref('');
  const packAndDownloadFile = async (filePath: string) => {
    filePath = filePath.substring(filePath.indexOf('/weight') + '/weight'.length); // /job-local-cz-449/best.onnx
    console.log(`packAndDownloading ${filePath}`);
    console.log(`appZipPath ${props.appZipPath}`);
    if (!isPolling.value && props.appZipPath.length > 0) {
      packingStatusModelVisible.value = true;
      currentFilePath.value = filePath;
      authCode.value = ''; // 每次打开清空
      isInputAuthCode.value = false; // 确保初始状态正确
    } else {
      createMessage.warning('还有打包任务正在进行中！');
    }
  };

  const confirmPack = async () => {
    // 如果还没输入授权码，显示输入框
    if (!isInputAuthCode.value) {
      isInputAuthCode.value = true;
      return;
    }

    // 移除授权码必填校验，允许授权码为空
    isPolling.value = true;
    isInputAuthCode.value = false;

    const stopPolling = (message, isSuccess = false) => {
      clearInterval(timer);
      isPolling.value = false;
      currentFilePath.value = '';
      packingStatusModelVisible.value = false;
      authCode.value = ''; // 清空授权码
      if (message) {
        isSuccess ? createMessage.success(message) : createMessage.error(message);
      }
    };

    try {
      const taskId = await startAlterAndPack(
        currentFilePath.value,
        props.appZipPath,
        props.datasetId,
        props.datasetVersionName,
        props.jobId,
        authCode.value, // 添加授权码参数
      );
      if (!taskId) {
        stopPolling('打包任务发起失败，请重试！');
        return;
      }

      createMessage.success('打包任务已经发起，请在此页面等待片刻即可自动下载', 8);

      timer = setInterval(async () => {
        try {
          const status = await checkPackStatus(taskId);

          if (status === 'COMPLETED') {
            stopPolling('打包任务已完成，开始下载...', true);

            const result = await downloadAndDelete(taskId);
            if (result.success) {
              createMessage.success('下载完成，文件已保存到您的下载文件夹中。');
            } else {
              stopPolling('下载失败，请重试！');
            }
          } else if (status === 'FAILED') {
            try {
              const taskDetail = await getTaskDetail(taskId);
              if (taskDetail && taskDetail.errorType) {
                // 根据错误类型显示不同的提示
                if (taskDetail.errorType === 'AUTH_CODE_INVALID') {
                  stopPolling('授权码验证失败，请检查授权码是否正确！');
                } else if (taskDetail.errorMessage) {
                  stopPolling(taskDetail.errorMessage);
                } else {
                  stopPolling('打包任务失败，请重试！');
                }
              } else {
                console.warn('任务详情中没有错误类型信息');
                stopPolling('打包任务失败，请重试！');
              }
            } catch (detailError) {
              console.error('获取任务详情失败：', detailError);
              stopPolling('打包任务失败，请重试！');
            }
          }
        } catch (error) {
          console.error('检查打包状态时发生错误：', error);
          stopPolling('检查打包状态时发生错误，请重试！');
        }
      }, 5000);
    } catch (e) {
      stopPolling('打包任务发起失败，请重试！');
    }
  };

  const cancelPack = () => {
    // 如果是在输入授权码阶段，返回确认界面
    if (isInputAuthCode.value) {
      isInputAuthCode.value = false;
      // 关键：在授权码阶段返回时也要重置其他状态
      packingStatusModelVisible.value = false;
      currentFilePath.value = '';
      authCode.value = '';
      isPolling.value = false;
      return;
    }

    // 正常取消流程
    packingStatusModelVisible.value = false;
    currentFilePath.value = '';
    authCode.value = '';
    isInputAuthCode.value = false;
    isPolling.value = false;

    // 清理可能存在的定时器
    if (timer.value) {
      clearInterval(timer.value);
      timer.value = null;
    }
  };

  const getModelFile = async () => {
    const res = await getModelFileApi(props.jobId);
    let normalKey = 1;
    let convertedKey = 1;

    // 从appZipPath中提取算法包名称
    const getAppZipName = () => {
      if (props.appZipPath) {
        const fullName = props.appZipPath.split('/').pop();
        if (fullName) {
          const namePart = fullName.split('-')[0];
          const extensionPart = fullName.split('.').pop();
          return `${namePart}.${extensionPart}`;
        }
      }
      return '';
    };

    const appZipName = getAppZipName();

    res.forEach((item) => {
      let splitElement = item.fileName.split('/').pop();
      let extension = splitElement.split('.').pop();

      // 定义允许在模型下载中显示的文件类型
      const allowedExtensions = ['pt', 'onnx', 'rknn', 'bmodel'];

      // 判断是否为转换模型（.rknn, .engine 等）
      const isConvertedModel = extension !== 'pt' && extension !== 'onnx';

      // 只有指定类型的文件才放入模型下载区域
      if (allowedExtensions.includes(extension)) {
        normalModelData.value.push({
          key: normalKey++,
          originalName: item.fileName,
          fileName: splitElement,
          fileSize: item.fileSize ? item.fileSize : 'N/A',
        });
      }

      // 只有转换模型才放入算法包下载区域（用于打包）
      if (isConvertedModel) {
        convertedModelData.value.push({
          key: convertedKey++,
          originalName: item.fileName,
          fileName: appZipName || splitElement,
          fileSize: item.fileSize ? item.fileSize : 'N/A',
        });
      }
    });
  };
  onMounted(() => {
    getModelFile();
  });
</script>

<style scoped lang="less">
  .modal-content {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 100px; // 确保内容在垂直方向有一定高度
  }

  .center-text {
    text-align: center;
  }

  .auth-code-input {
    width: 100%;
    padding: 20px; // 上下左右统一边距

    .auth-code-tip {
      margin-bottom: 20px;
      padding: 12px 16px;
      background-color: rgba(64, 150, 255, 0.1); // 深色背景下的蓝色半透明背景
      border-left: 3px solid #4096ff;
      color: rgba(255, 255, 255, 0.85); // 深色主题下的文字颜色
      font-size: 14px;
      line-height: 1.6;
      border-radius: 4px;
    }

    .auth-code-form {
      .form-label {
        margin-bottom: 8px;
        color: rgba(255, 255, 255, 0.85);
        font-size: 14px;
        font-weight: 500;
      }
    }

    // 覆盖 input-password 的默认样式
    :deep(.ant-input-password) {
      background-color: #18181b !important; /* 深色背景 */
      border-color: #37373c !important; /* 边框颜色 */
      color: white !important; /* 文字颜色 */
    }

    :deep(.ant-input-password:focus) {
      border-color: #4096ff !important;
      box-shadow: 0 0 0 2px rgba(64, 150, 255, 0.2) !important;
    }
  }
</style>
