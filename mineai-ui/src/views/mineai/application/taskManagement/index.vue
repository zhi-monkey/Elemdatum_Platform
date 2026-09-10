<template>
  <div>
    <BasicTable @register="registerTable">
      <template #toolbar v-if="hasPermission([RoleEnum.TaskManagement_Write])">
        <a-button type="primary" @click="handleCreate">新增应用任务</a-button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:info-circle-outlined',
              tooltip: '详情',
              onClick: () => openDetailModal(record), // 点击详情按钮时调用函数
            },
            {
              icon: 'clarity:note-edit-line',
              tooltip: '编辑',
              onClick: handleEdit.bind(null, record),
              ifShow: record.isReleased != '已发布',
              disabled: !hasPermission([RoleEnum.TaskManagement_Write]),
            },
            {
              icon: 'clarity:paperclip-line',
              tooltip: '绑定场景',
              onClick: () => openSceneDrawer(record),
              ifShow: false,
              disabled: !hasPermission([RoleEnum.TaskManagement_Write]),
            },
            {
              icon: 'clarity:paperclip-line',
              tooltip: '绑定设备-固件',
              onClick: () => openDeviceDrawer(record),
              ifShow: false,
              disabled: !hasPermission([RoleEnum.TaskManagement_Write]),
            },
            {
              icon: 'clarity:paperclip-line',
              tooltip: '绑定算法',
              onClick: () => openAlgoDrawer(record),
              ifShow: false,
              disabled: !hasPermission([RoleEnum.TaskManagement_Write]),
            },
            {
              icon: 'ant-design:file-add-outlined',
              tooltip: '导入算法应用包',
              onClick: handleUploadZip.bind(null, record, dataTypeCodeMap.IMAGE),
              ifShow: record.isReleased != '已发布',
              disabled: !hasPermission([RoleEnum.TaskManagement_Write]),
            },
            {
              icon: 'ant-design:cloud-upload-outlined',
              tooltip: '更新算法包',
              onClick: handleUploadZip.bind(null, record, dataTypeCodeMap.IMAGE),
              ifShow: record.isReleased == '已发布',
              disabled: !hasPermission([RoleEnum.TaskManagement_Write]),
            },
            {
              icon: 'ant-design:check-outlined',
              tooltip: '发布',
              popConfirm: {
                title: '是否确认发布',
                confirm: handleReleased.bind(null, record),
              },
              ifShow:
                record.applicableScene.length > 0 &&
                !!record.appZipPath &&
                !!record.device &&
                !!record.model &&
                record.isReleased != '已发布',
              disabled: !hasPermission([RoleEnum.TaskManagement_Write]),
            },
            {
              icon: 'ant-design:close-outlined',
              tooltip: '取消发布',
              popConfirm: {
                title: '是否取消发布',
                confirm: handleUnReleased.bind(null, record),
              },
              ifShow: record.isReleased == '已发布',
              disabled: !hasPermission([RoleEnum.TaskManagement_Write]),
            },
            {
              icon: 'ant-design:download-outlined',
              tooltip: '下载',
              onClick: handleDownload.bind(null, record),
              ifShow:
                record.applicableScene.length > 0 &&
                !!record.appZipPath &&
                !!record.device &&
                !!record.model,
            },
            {
              icon: 'ant-design:delete-outlined',
              tooltip: '删除',
              color: 'error',
              popConfirm: {
                title: '是否确认删除',
                confirm: handleDelete.bind(null, record),
              },
              disabled:
                !hasPermission([RoleEnum.TaskManagement_Write]) || record.isReleased == '已发布',
            },
          ]"
        />
      </template>
    </BasicTable>
    <!-- DetailModal 用于展示详情 -->
    <DetailModal
      :visible="isDetailModalVisible"
      :record="selectedRecord"
      @update:visible="isDetailModalVisible = $event"
      v-if="isDetailModalVisible"
    />
    <ApplicationModal @register="createAndUpdateModal" @form-submit="handleFormSubmit" />
    <sceneDrawer
      :visible="isSceneDrawerVisible"
      :tree-data="sceneTreeData"
      :current-row-checked-keys="currentRowCheckedKeys"
      @close="closeSceneDrawer"
      @confirm="handleSceneConfirm($event)"
    />
    <deviceDrawer
      :visible="isDeviceDrawerVisible"
      :tree-data="deviceTreeData"
      :current-row-checked-key="currentRowCheckedKey"
      @close="closeDeviceDrawer"
      @confirm="handleDeviceConfirm($event)"
    />
    <algoDrawer
      :visible="isAlgoDrawerVisible"
      :current-row-checked-key="currentRowCheckedKey"
      :tree-data="modelTreeData"
      @confirm="handleAlgoConfirm($event)"
      @close="closeAlgoDrawer"
    />
    <UploadZip
      :close-upload-zip-file="closeUploadZipFile"
      :row="importRow"
      :visible="uploadZipFileVisible"
      @upload-all-done="uploadAllDone"
    />
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
  </div>
</template>
<script lang="ts" setup>
  import { computed, ComputedRef, onActivated, onMounted, Ref, ref } from 'vue';
  import { Button as AButton, Modal as AModal, Spin as ASpin } from 'ant-design-vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { columns, searchFormSchema } from './model.data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useModal } from '/@/components/Modal';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { RoleEnum } from '/@/enums/roleEnum';
  import ApplicationModal from './taskManagementModal.vue';
  import sceneDrawer from './sceneDrawer.vue';
  import deviceDrawer from './deviceDrawer.vue';
  import algoDrawer from './algoDrawer.vue';
  import { dataTypeCodeMap } from '/@/views/mineai/data/dataset-details2/util';
  import UploadZip from './upload-zip.vue';
  import { Recordable } from 'vite-plugin-mock';
  import {
    checkIfApplicationExistsApi,
    checkModelApplicationCanUnbind,
    checkPackStatus,
    deleteModelApplication,
    downloadAndDelete,
    getDeviceById,
    getDeviceTreeData,
    getModelApplicationList,
    getModelById,
    getModelTreeData,
    getSceneListByIds,
    getSceneTreeData,
    getTaskDetail,
    setReleasedState,
    startDownLoad,
    unsetReleasedState,
  } from '/@/views/mineai/application/taskManagement/api/api';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import DetailModal from './DetailModal.vue'; // 引入 DetailModal

  // 添加授权码相关状态
  const isInputAuthCode = ref(false);
  const authCode = ref('');
  // 控制弹窗的显示状态
  const isDetailModalVisible = ref(false);
  // 存储当前点击的记录信息
  const selectedRecord = ref(null);

  // 打开详情模态框的函数
  const openDetailModal = (record) => {
    selectedRecord.value = record; // 记录选中的数据
    isDetailModalVisible.value = true; // 打开弹窗
  };
  const importRow: Ref = ref(null);
  const uploadZipFileVisible: Ref<boolean> = ref(false);
  const isSceneDrawerVisible = ref(false);
  const isDeviceDrawerVisible = ref(false);
  const isAlgoDrawerVisible = ref(false);
  const { hasPermission } = usePermission();
  const [
    createAndUpdateModal,
    { openModal: openCreateAndUpdateModal, closeModal: closeCreateAndUpdateModal },
  ] = useModal();
  const userPermission: ComputedRef<boolean> = computed(() => {
    return hasPermission([RoleEnum.SystemUser]);
  });
  const { createMessage } = useMessage();
  // sceneTreeData 是场景组件中树组件的数据
  const sceneTreeData = ref([]);
  const deviceTreeData = ref([]);
  const modelTreeData = ref([]);
  let timer = ref(null);

  const [registerTable, { reload }] = useTable({
    title: '应用任务列表',
    api: async (params) => {
      return await getModelApplicationList(params);
    },
    beforeFetch: (v) => {
      //发出分页查询请求前，将1-started页码（VBen）转换为0-started页码（Spring Page）
      v.page -= 1;
      return v;
    },
    afterFetch: (itemList) => {
      return itemList;
    },
    columns: columns,
    formConfig: {
      labelWidth: 120,
      showAdvancedButton: false,
      schemas: searchFormSchema,
    },
    useSearchForm: true,
    showTableSetting: false,
    bordered: false,
    showIndexColumn: false,
    expandRowByClick: true,
    handleSearchInfoFn(info) {
      return info;
    },
    actionColumn: {
      width: 100,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
  });

  function handleUploadZip(record: Recordable, labelType) {
    importRow.value = record;
    importRow.value.labelType = labelType;
    uploadZipFileVisible.value = true;
  }

  const closeUploadZipFile = (flag): void => {
    uploadZipFileVisible.value = false;
    // 判断是否是取消还是完成
    if (flag) {
      reload();
    }
  };

  const uploadAllDone = () => {
    reload();
  };

  const currentRowRecord = ref(null);
  let currentRowCheckedKeys = ref([]);

  function openSceneDrawer(record: Recordable) {
    isSceneDrawerVisible.value = true;
    currentRowRecord.value = record;
    currentRowCheckedKeys.value = record.applicableScene?.map((item) => item.id);
  }

  function closeSceneDrawer() {
    isSceneDrawerVisible.value = false;
  }

  // 这个key是绑定只能选中一个的那种
  let currentRowCheckedKey = ref(0);

  function openDeviceDrawer(record: Recordable) {
    isDeviceDrawerVisible.value = true;
    currentRowRecord.value = record;
    currentRowCheckedKey.value = record.device?.id;
  }

  function closeDeviceDrawer() {
    isDeviceDrawerVisible.value = false;
  }

  function openAlgoDrawer(record: Recordable) {
    isAlgoDrawerVisible.value = true;
    currentRowRecord.value = record;
    currentRowCheckedKey.value = record.model.id;
  }

  function closeAlgoDrawer() {
    isAlgoDrawerVisible.value = false;
  }

  function handleReleased(record: Recordable) {
    // 检查是否所有必填字段都已填写
    if (
      !record.allApplicableSceneName ||
      !record.appZipName ||
      !record.device ||
      !record.applicationTaskName ||
      !record.model
    ) {
      createMessage.error('发布失败: 请填写所有必填信息');
      return;
    }

    // 检查算法应用商城中是否存在相同记录
    checkIfApplicationExists(record)
      .then((existingRecord) => {
        if (existingRecord) {
          // 判断现有记录是否由当前用户发布
          if (existingRecord.publisherId === record.publisherId) {
            // 如果是当前用户发布，询问是否要替换
            AModal.confirm({
              title: '替换确认',
              content: '算法应用商城中已有相同的应用任务，是否替换？',
              onOk() {
                proceedWithRelease(record);
                handleUnReleased(existingRecord);
              },
              onCancel() {
                createMessage.info('发布操作已取消');
              },
            });
          } else {
            // 如果已经被发布了
            createMessage.warning(
              '该应用名称下已经发布过相同设备固件的应用了，如非本人发布，请联系管理员处理',
            );
          }
        } else {
          // 如果没有相同记录，直接发布
          proceedWithRelease(record);
        }
      })
      .catch((error) => {
        createMessage.error(`发布检查失败: ${error.message}`);
      });
  }

  function proceedWithRelease(record: Recordable) {
    setReleasedState(record.id)
      .then(() => {
        createMessage.success('发布成功！');
        reload(); // 发布成功后重新加载表格数据
      })
      .catch((error) => {
        createMessage.error(`发布失败: ${error.message}`);
      });
  }

  async function handleUnReleased(record: Recordable) {
    try {
      const checkResult = await checkModelApplicationCanUnbind(record.id);
      if (!checkResult?.canUnbind) {
        const occupyText =
          checkResult?.blockingGenerations?.length > 0
            ? checkResult.blockingGenerations
                .map((item) => `${item.name}(ID:${item.id})`)
                .join('，')
            : '';
        const message = occupyText
          ? `取消发布失败：以下生产任务处于数据准备中：${occupyText}`
          : '取消发布失败：存在处于数据准备中的生产任务。';
        createMessage.warning(message);
        return;
      }
      await unsetReleasedState(record.id);
      createMessage.success('发布已取消！');
      reload();
    } catch (error: any) {
      createMessage.error(`取消发布失败: ${error?.message || error}`);
    }
  }

  async function checkIfApplicationExists(record: Recordable) {
    try {
      const isExist = await checkIfApplicationExistsApi({
        deviceName: record.device.deviceName,
        firmwareVersion: record.device.firmwareVersion,
        applicationName: record.applicationName.applicationName,
      });
      return isExist;
    } catch (error) {
      console.error('检查是否存在相同的应用场景和设备固件的记录时出错:', error);
      throw error;
    }
  }

  function handleCreate() {
    openCreateAndUpdateModal(true, {
      isUpdate: false,
    });
  }

  const isPolling = ref(false);
  let packingStatusModelVisible = ref(false);
  let currentModelApplicationId = ref(0);
  const handleDownload = async (record: Recordable) => {
    if (!isPolling.value && currentModelApplicationId.value === 0) {
      packingStatusModelVisible.value = true;
      currentModelApplicationId.value = record.id;
      authCode.value = ''; //每次打开清空
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
      currentModelApplicationId.value = 0;
      packingStatusModelVisible.value = false;
      authCode.value = ''; // 清空授权码
      if (message) {
        isSuccess ? createMessage.success(message) : createMessage.error(message);
      }
    };

    try {
      const taskId = await startDownLoad(currentModelApplicationId.value, authCode.value);
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
            // 获取任务详情以获取具体错误信息
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
      currentModelApplicationId.value = 0;
      authCode.value = '';
      isPolling.value = false;
      return;
    }

    // 正常取消流程
    packingStatusModelVisible.value = false;
    currentModelApplicationId.value = 0;
    authCode.value = '';
    isInputAuthCode.value = false;
    isPolling.value = false;

    // 清理可能存在的定时器
    if (timer.value) {
      clearInterval(timer.value);
      timer.value = null;
    }
  };

  function handleFormSubmit(data: any, isUpdate: boolean) {
    closeCreateAndUpdateModal();
    reload();
  }

  function handleEdit(record: Recordable) {
    openCreateAndUpdateModal(true, {
      record,
      isUpdate: true,
    });
  }

  function handleDelete(record: Recordable) {
    deleteModelApplication(record.id)
      .then(() => {
        createMessage.success('删除成功！');
        reload(); // 删除成功后重新加载表格数据
      })
      .catch((error) => {
        createMessage.error(`删除失败: ${error.message}`);
      });
  }

  const handleAlgoConfirm = async (selectedAlgoId) => {
    currentRowRecord.value.model = await getModelById(selectedAlgoId);
    const data = {
      ...currentRowRecord.value,
    };
    await maHttp
      .post(
        {
          url: 'modelApplication/bindModel',
          data,
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(() => {
        createMessage.success('更新成功！');
        reload();
        currentRowRecord.value = null;
      });
  };

  const handleDeviceConfirm = async (selectedDeviceId) => {
    currentRowRecord.value.device = await getDeviceById(selectedDeviceId);
    // const res = await getDeviceById(selectedDeviceId);
    const data = {
      ...currentRowRecord.value,
    };
    await maHttp
      .post(
        {
          url: 'modelApplication/bindDevice',
          data,
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(() => {
        createMessage.success('更新成功！');
        reload();
        currentRowRecord.value = null;
      });
  };

  const handleSceneConfirm = async (selectedSceneIdList) => {
    currentRowRecord.value.applicableScene = await getSceneListByIds(selectedSceneIdList);
    const data = {
      ...currentRowRecord.value,
    };
    await maHttp
      .post(
        {
          url: 'modelApplication/bindScene',
          data,
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(() => {
        createMessage.success('更新成功！');
        reload();
        curentntRowRecord.value = null;
      });
  };

  onMounted(async () => {
    sceneTreeData.value = await getSceneTreeData();
    deviceTreeData.value = await getDeviceTreeData();
    modelTreeData.value = await getModelTreeData();
  });
  onActivated(() => {
    reload();
  });
</script>
<style lang="scss">
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
    .ant-input-password {
      background-color: #18181b !important; /* 深色背景 */
      border-color: #37373c !important; /* 边框颜色 */
      color: white !important; /* 文字颜色 */
    }

    .ant-input-password:focus {
      border-color: #4096ff !important;
      box-shadow: 0 0 0 2px rgba(64, 150, 255, 0.2) !important;
    }
  }

  // Tags display styles
  :deep(.tags-container) {
    display: flex;
    align-items: center;
    gap: 4px;
  }

  :deep(.tag-item) {
    margin: 0;
  }

  :deep(.no-tags) {
    color: #999;
    font-style: italic;
  }

  :deep(.tags-ellipsis) {
    color: #999;
    font-size: 14px;
    margin: 0 4px;
    font-weight: 500;
  }

  :deep(.show-all-tags-trigger) {
    color: #1890ff;
    cursor: pointer;
    font-size: 12px;
    text-decoration: none;
    margin-left: 4px;
  }

  :deep(.show-all-tags-trigger:hover) {
    color: #40a9ff;
    text-decoration: underline;
  }
</style>
