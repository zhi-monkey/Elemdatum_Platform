<template>
  <div>
    <BasicTable @register="registerTable" @selection-change="selectionChange">
      <template #toolbar>
        <a-button type="primary" @click="handleCreate"> 新增用户</a-button>
        <a-button v-show="isAdmin" @click="handleOpenImportGuide">
          <UploadOutlined />
          批量导入
        </a-button>
        <a-button type="primary" @click="handleGroupDelete" :disabled="canDelete">
          批量删除
        </a-button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'clarity:note-edit-line',
              tooltip: '编辑用户',
              onClick: handleEdit.bind(null, record),
            },
            {
              icon: 'ant-design:key-outlined',
              tooltip: '修改密码',
              disabled: record.id === ADMIN_USER_ID && userStore.getUserInfo.id != ADMIN_USER_ID,
              onClick: handleChangePassword.bind(null, record),
            },
            {
              icon: 'ant-design:delete-outlined',
              tooltip: '删除用户',
              color: 'error',
              disabled: record.id === ADMIN_USER_ID,
              popConfirm: {
                title: '是否确认删除',
                confirm: handleDelete.bind(null, record),
              },
            },
          ]"
        />
      </template>
    </BasicTable>
    <UserModal @register="registerModal" @success="handleSuccess" />
    <PasswordModal @register="registerPasswordModal" @success="handleSuccess" />
    <BatchImportModal
      ref="batchImportModalRef"
      @register="registerBatchImportModal"
      @success="handleImportSuccess"
      @cancel="reload"
    />

    <!-- 导入引导模态框 -->
    <a-modal
      v-model:visible="importGuideVisible"
      title="批量导入用户"
      :footer="null"
      width="640px"
      :bodyStyle="{ padding: '32px' }"
    >
      <div class="import-guide-content">
        <!-- 说明区域 -->
        <a-alert
          message="导入说明"
          description="请先下载Excel模板文件，按照模板格式填写用户信息后上传。支持批量导入多个用户。"
          type="info"
          show-icon
          :style="{
            marginBottom: '28px',
            borderRadius: '8px',
          }"
        />

        <!-- 步骤说明 -->
        <div class="steps-section">
          <h4 class="section-title">操作步骤</h4>
          <ol class="steps-list">
            <li class="step-item">
              <span class="step-number">1</span>
              <span class="step-text">点击下方按钮下载Excel模板文件</span>
            </li>
            <li class="step-item">
              <span class="step-number">2</span>
              <span class="step-text">使用Excel编辑模板文件，填写用户信息</span>
            </li>
            <li class="step-item">
              <span class="step-number">3</span>
              <span class="step-text">保存文件后，点击下方上传按钮选择文件导入</span>
            </li>
          </ol>
        </div>

        <!-- 下载模板按钮 -->
        <div class="download-section">
          <a-button
            type="dashed"
            size="large"
            block
            @click="handleDownloadTemplate"
            class="download-btn"
          >
            <DownloadOutlined />
            下载Excel模板文件
          </a-button>
        </div>

        <a-divider :style="{ margin: '28px 0' }" />

        <!-- 上传区域 -->
        <div class="upload-section">
          <a-upload
            :before-upload="handleBeforeUpload"
            :show-upload-list="false"
            accept=".xlsx"
            :customRequest="() => {}"
          >
            <a-button type="primary" size="large" block class="upload-btn">
              <UploadOutlined />
              选择Excel文件上传
            </a-button>
          </a-upload>
          <p class="upload-tip">支持的文件格式：.xlsx</p>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import UserModal from './UserModal.vue';
  import { useModal } from '/@/components/Modal';
  import { columns, searchFormSchema } from './user.data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import {
    Alert as AAlert,
    Button as AButton,
    Divider as ADivider,
    Modal,
    Modal as AModal,
    Upload as AUpload,
  } from 'ant-design-vue';
  import { del, list } from '/@/views/mineai/system/api/user';
  import PasswordModal from '/@/views/mineai/system/user/PasswordModal.vue';
  import { useUserStore } from '/@/store/modules/user';
  import { computed, createVNode, onUnmounted, ref, Ref } from 'vue';
  import {
    DownloadOutlined,
    ExclamationCircleOutlined,
    UploadOutlined,
  } from '@ant-design/icons-vue';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useExcel } from '/@/utils/useCsv';
  import BatchImportModal from '/@/views/mineai/system/user/BatchImportModal.vue';
  import * as XLSX from 'xlsx';

  const userStore = useUserStore();
  let roles = userStore.getUserInfo.roles;
  const { parseExcel } = useExcel();
  const ADMIN_USER_ID = 1; // 管理员角色id
  const [registerModal, { openModal }] = useModal();
  const [registerPasswordModal, { openModal: openPasswordModal }] = useModal();
  const [registerBatchImportModal, { openModal: openBatchImportModal }] = useModal();
  const batchImportModalRef = ref(null);

  // 导入引导模态框可见性
  const importGuideVisible = ref(false);

  const [registerTable, { reload, clearSelectedRowKeys }] = useTable({
    title: '用户列表',
    api: async (params) => {
      const v = await list(params);
      //完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
      v.items = v.result;
      v.total = v.page.total;
      return v;
    },
    beforeFetch: (v) => {
      // 发出分页查询请求前，将1-started页码（VBen）转换为0-started页码（Spring Page）
      v = Object.assign(v, {
        current: v.page,
        size: v.pageSize,
      });
      // 主动使用排序功能，则进行参数转换
      if (Object.hasOwn(v, 'field')) {
        Object.assign(v, {
          order: v.order === 'ascend' ? 'asc' : 'desc',
          sort: v.field,
        });
      }
      // 否则进行默认排序,根据id降序排序
      else {
        Object.assign(v, {
          order: 'desc',
          sort: 'id',
        });
      }
      return v;
    },
    afterFetch: (itemList) => {
      //GET请求被解析为 Vben表格Table的行后,做一些操作
      //console.log('afterFetch', itemList);
      return itemList;
    },
    columns,
    formConfig: {
      labelWidth: 120,
      schemas: searchFormSchema,
      showAdvancedButton: false,
      showResetButton: true,
    },
    rowSelection: {
      type: 'checkbox',
    },
    useSearchForm: true,
    showTableSetting: false,
    bordered: true,
    showIndexColumn: false,
    clearSelectOnPageChange: true,
    actionColumn: {
      width: 120,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
    handleSearchInfoFn(info) {
      return info;
    },
  });

  const canDelete: Ref<boolean> = ref(true);
  const selectedKeys: Ref<Array<any>> = ref([]);
  const selectedRows: Ref<Array<any>> = ref([]);
  const selectionChange = ({ keys, rows }) => {
    selectedKeys.value = keys;
    selectedRows.value = rows;
    canDelete.value = keys.length <= 0;
  };

  const isAdmin = computed(() => {
    // id=1 是管理员角色, id=89是管理人员角色
    return roles[0].name === '管理员' || roles[0].name === '管理人员';
  });

  // 打开导入引导模态框
  const handleOpenImportGuide = () => {
    importGuideVisible.value = true;
  };

  // 下载Excel模板
  const handleDownloadTemplate = () => {
    // 创建Excel模板数据
    const data = [
      ['username', 'nickName', 'sex', 'email', 'phone', 'password', 'remark', 'enabled'],
      [
        'zhangsan',
        '张三',
        '男',
        'zhangsan@example.com',
        '13800138000',
        '123456',
        '测试用户',
        'true',
      ],
      ['lisi', '李四', '女', 'lisi@example.com', '13900139000', '123456', '另一个测试用户', 'true'],
    ];

    // 创建工作簿
    const ws = XLSX.utils.aoa_to_sheet(data);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, '用户导入模板');

    // 导出文件
    XLSX.writeFile(wb, '用户导入模板.xlsx');

    createMessage.success('模板下载成功！');
  };

  const handleBeforeUpload = async (file: File) => {
    try {
      const results = await parseExcel(file);

      const importData = results.data.map((row: any) => {
        // 清理手机号中的格式标记
        let phone = String(row.phone || row.手机号 || '');

        // 移除可能的格式标记
        phone = phone
          .replace(/^="(.*)"$/, '$1') // 移除 ="xxx" 格式
          .replace(/^'/, '') // 移除开头的单引号
          .replace(/^\t/, '') // 移除开头的制表符
          .trim();

        return {
          username: row.username || row.用户名,
          nickName: row.nickName || row.昵称,
          sex: row.sex || row.性别,
          email: row.email || row.邮箱,
          phone: phone,
          password: row.password || row.密码,
          remark: row.remark || row.备注,
          enabled: row.enabled?.toString().toLowerCase() === 'true' || row.enabled === true,
        };
      });

      importGuideVisible.value = false;
      // 在打开新的导入模态框前重置状态
      if (batchImportModalRef.value && typeof batchImportModalRef.value.resetState === 'function') {
        batchImportModalRef.value.resetState();
      }
      openBatchImportModal(true, { importData });
    } catch (error) {
      console.error('文件解析失败:', error);
      createMessage.error('文件解析失败,请检查文件格式');
    }

    return false;
  };

  const handleGroupDelete = () => {
    if (selectedKeys.value.length <= 0) {
      return;
    }
    const modal = Modal.confirm({
      title: () => '确认删除选中的' + selectedKeys.value.length + '条数据?',
      maskClosable: true,
      icon: () => createVNode(ExclamationCircleOutlined),
      okText: () => '确定',
      okType: 'danger',
      cancelText: () => '取消',
      onOk() {
        // 手动关闭弹窗
        modal.destroy();

        // 异步删除操作
        const ids = selectedRows.value.map((row) => row.id);
        maHttp
          .delete(
            {
              url: 'users',
              data: { ids },
              headers: {},
            },
            { urlPrefix: DubheBackendUrlEnum.DUBHE_ADMIN },
          )
          .then(() => {
            createMessage.success('删除成功！');
            // 清空所有选中的keys
            clearSelectedRowKeys();
            // 刷新页面
            reload();
          })
          .catch((e) => {
            console.error(e);
            createMessage.error('删除失败！');
            reload();
            clearSelectedRowKeys();
          });
      },
    });
  };

  function handleCreate() {
    openModal(true, {
      isUpdate: false,
    });
  }

  function handleEdit(record: Recordable) {
    openModal(true, {
      record,
      isUpdate: true,
    });
  }

  function handleChangePassword(record: Recordable) {
    openPasswordModal(true, {
      record,
    });
  }

  const { createMessage } = useMessage();

  function handleDelete(record: Recordable) {
    del([record.id])
      .then(() => {
        createMessage.success('删除成功！');
        reload();
      })
      .catch((e) => {
        console.error(e);
      });
  }

  function handleSuccess() {
    reload();
  }

  const handleImportSuccess = () => {
    console.log('Import success event received, reloading table');
    // 清除可能的缓存并强制刷新
    setTimeout(() => {
      reload(true);
    }, 1000);
  };

  onUnmounted(() => {
    clearSelectedRowKeys();
  });
</script>
<style scoped>
  .import-guide-content {
    min-height: 300px;
  }

  /* 步骤区域 */
  .steps-section {
    margin-bottom: 28px;
    padding: 20px;
    background: linear-gradient(135deg, #1a2332 0%, #1e2836 100%);
    border-radius: 8px;
    border: 1px solid #2d3f5f;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.3);
  }

  .section-title {
    margin: 0 0 16px 0;
    font-size: 15px;
    font-weight: 600;
    color: #e8eaed;
    letter-spacing: 0.3px;
  }

  .steps-list {
    margin: 0;
    padding: 0;
    list-style: none;
  }

  .step-item {
    display: flex;
    align-items: flex-start;
    margin-bottom: 16px;
    line-height: 1.6;
  }

  .step-item:last-child {
    margin-bottom: 0;
  }

  .step-number {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    min-width: 24px;
    height: 24px;
    margin-right: 12px;
    background: linear-gradient(135deg, #4a90e2 0%, #357abd 100%);
    color: #ffffff;
    border-radius: 50%;
    font-size: 13px;
    font-weight: 600;
    flex-shrink: 0;
    box-shadow: 0 2px 6px rgba(74, 144, 226, 0.4);
  }

  .step-text {
    flex: 1;
    padding-top: 2px;
    color: #b8bfc6;
    font-size: 14px;
  }

  /* 下载区域 */
  .download-section {
    margin-bottom: 0;
  }

  .download-btn {
    height: 44px;
    font-size: 14px;
    font-weight: 500;
    border-radius: 6px;
    background: #1a1a1a;
    border-color: #3d5a80;
    color: #6fa8dc;
    transition: all 0.3s ease;
  }

  .download-btn:hover {
    border-color: #4a90e2;
    color: #84b7e8;
    background: #232838;
    transform: translateY(-1px);
    box-shadow: 0 4px 12px rgba(74, 144, 226, 0.25);
  }

  /* 上传区域 */
  .upload-section {
    text-align: center;
  }

  .upload-btn {
    height: 48px;
    font-size: 15px;
    font-weight: 500;
    border-radius: 6px;
    background: linear-gradient(135deg, #4a90e2 0%, #2c5f9e 100%);
    border-color: #4a90e2;
    box-shadow: 0 4px 12px rgba(74, 144, 226, 0.3);
    transition: all 0.3s ease;
  }

  .upload-btn:hover {
    background: linear-gradient(135deg, #5ba0f2 0%, #3d70ae 100%);
    border-color: #5ba0f2;
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(74, 144, 226, 0.4);
  }

  .upload-tip {
    margin-top: 12px;
    margin-bottom: 0;
    color: #6b7785;
    font-size: 13px;
  }

  /* Alert 暗色主题样式 */
  :deep(.ant-alert-info) {
    background: linear-gradient(135deg, #1e3a5f 0%, #1a2f4a 100%);
    border: 1px solid #2d4a6f;
  }

  :deep(.ant-alert-message) {
    font-weight: 600;
    color: #84b7e8;
  }

  :deep(.ant-alert-description) {
    color: #9eb8d1;
  }

  :deep(.ant-alert-icon) {
    color: #6fa8dc;
  }

  /* 分割线暗色样式 */
  :deep(.ant-divider) {
    border-color: #2d3748;
  }
</style>
