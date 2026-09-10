<template>
  <div>
    <PageWrapper :title="`算法应用 - ${modelName}`" @back="goBack">
      <BasicTable @register="registerTable">
        <template #action="{ record }">
          <TableAction
            :actions="[
              {
                icon: 'ant-design:download-outlined',
                tooltip: '下载',
                onClick: handleDownload.bind(null, record),
                ifShow: !isVisitor,
              },
              {
                icon: 'ant-design:delete-outlined',
                tooltip: '删除',
                color: 'error',
                onClick: handleDelete.bind(null, record),
                disabled:
                  !hasPermission([RoleEnum.ModelApplicationDetail_Write]) || !record.canBeDeleted,
              },
            ]"
          />
        </template>
      </BasicTable>
    </PageWrapper>
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
    <a-modal
      v-model:visible="deleteModelVisible"
      title="删除"
      cancelText="隐藏"
      confirmText="删除"
      @ok="confirmDelete"
      @cancel="cancelDelete"
    >
      <div class="modal-content">
        <div class="center-text">确定要删除此条目吗？</div>
      </div>
    </a-modal>
  </div>
</template>
<script lang="ts" setup>
  import { computed, ComputedRef, onActivated, ref } from 'vue';
  import { useRouter } from 'vue-router';
  import { Modal as AModal, Spin as ASpin } from 'ant-design-vue';
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { sonColumns } from './model.data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { usePermission } from '/@/hooks/web/usePermission';
  import { RoleEnum } from '/@/enums/roleEnum';

  import {
    checkPackStatus,
    checkModelApplicationCanUnbind,
    downloadAndDelete,
    getModelApplicationList,
    getModelApplicationListForReal,
    getModelApplicationTaskList,
    startDownLoad,
    unsetReleasedState,
  } from '/@/views/mineai/application/taskManagement/api/api';
  import { useUserStore } from '/@/store/modules/user';
  import PageWrapper from '/@/components/Page/src/PageWrapper.vue';

  const router = useRouter();
  let modelName = '';
  const { hasPermission } = usePermission();
  const userPermission: ComputedRef<boolean> = computed(() => {
    return hasPermission([RoleEnum.SystemUser]);
  });
  const { createMessage } = useMessage();
  const userStore = useUserStore();
  const userData = userStore.getUserInfo;
  const roles = userData.roles;

  // 添加授权码相关状态
  const isInputAuthCode = ref(false);
  const isVisitor = computed(() => {
    return roles[0].name === '游客';
  });
  const authCode = ref('');
  const modelNum = window.location.href.lastIndexOf('/');
  const name = window.location.href.substring(modelNum + 1);
  modelName = decodeURIComponent(name);
  const [registerTable, { reload }] = useTable({
    title: '算法应用列表',
    api: async (params) => {
      params.modelName = modelName;
      return await getModelApplicationTaskList({
        ...params, // 设置每页返回的条目数
      });
    },
    beforeFetch: (v) => {
      //发出分页查询请求前，将1-started页码（VBen）转换为0-started页码（Spring Page）
      v.page -= 1;
      console.log(v);
      return v;
    },
    afterFetch: (itemList) => {
      // item.value = itemList.filter((item) => item.isReleased === '已发布');
      console.log(itemList);
      return itemList;
    },
    columns: sonColumns,
    formConfig: {
      labelWidth: 120,
      showAdvancedButton: false,
      // schemas: userPermission.value ? searchFormSchema : simpleSearchFormSchema,
    },
    // useSearchForm: true,
    showTableSetting: true,
    bordered: false,
    showIndexColumn: false,
    expandRowByClick: true,
    // handleSearchInfoFn(info) {
    //   return info;
    // },
    actionColumn: {
      width: 100,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
      // ifShow: userPermission.value,
    },
  });

  let deleteModelVisible = ref(false);
  const isPolling = ref(false);
  let recordId = ref('');
  let packingStatusModelVisible = ref(false);
  let currentModelApplicationId = ref(0);
  let timer = ref(null);
  const handleDownload = async (record: Recordable) => {
    if (!isPolling.value && currentModelApplicationId.value === 0) {
      packingStatusModelVisible.value = true;
      currentModelApplicationId.value = record.id;
      authCode.value = ''; // 每次打开清空
      isInputAuthCode.value = false; // 确保初始状态正确
    } else {
      createMessage.warning('还有打包任务正在进行中！');
    }
  };
  // 添加返回函数
  const goBack = () => {
    router.push('/maApplication/appMall');
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
            stopPolling('打包任务失败，请重试！');
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

  const handleDelete = async (record: Recordable) => {
    // 设置删除模型的可见性为true，显示删除弹窗
    recordId = record.id;
    deleteModelVisible.value = true;
  };

  // 定义确认删除的函数
  const confirmDelete = async () => {
    try {
      const checkResult = await checkModelApplicationCanUnbind(recordId);
      if (!checkResult?.canUnbind) {
        const occupyText =
          checkResult?.blockingGenerations?.length > 0
            ? checkResult.blockingGenerations
                .map((item) => `${item.name}(ID:${item.id})`)
                .join('，')
            : '';
        const message = occupyText
          ? `删除失败：以下生产任务处于数据准备中：${occupyText}`
          : '删除失败：存在处于数据准备中的生产任务。';
        createMessage.warning(message);
        return;
      }
      // 执行删除操作
      await unsetReleasedState(recordId);
      createMessage.success('删除成功！');
      const response = await getModelApplicationList({});
      const items = response.content.filter(
        (item) =>
          item.isReleased === '已发布' && item.applicationName.applicationName === modelName,
      );
      // 根据返回的响应判断是否已经删除完所有条目
      const allItemsDeleted = items.length === 0; // 如果长度为 0，表示已删除所有条目
      if (allItemsDeleted) {
        // 返回上一级页面
        // router.go(-1);
        router.push({
          path: `/maAlgorithmMall/algorithmMall`,
        });
      } else {
        // 重新加载当前页面
        reload();
      }
      deleteModelVisible.value = false;
    } catch (error) {
      // 删除失败后，显示错误消息
      createMessage.error(`删除失败: ${error.message}`);
    }
  };

  // 定义取消删除的函数
  const cancelDelete = async () => {
    // 取消删除，只需要隐藏弹窗
    deleteModelVisible.value = false;
  };

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
</style>
