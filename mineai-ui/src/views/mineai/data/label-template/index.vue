<template>
  <div>
    <BasicTable
      @register="registerTable"
      @selection-change="selectionChange"
      @change="onPageChange"
    >
      <!-- 搜索表单行自定义插槽：左侧分类切换（图片标签/点云标签），右侧为名称搜索 + 操作按钮 -->
      <template #form-templateType>
        <div class="template-type-switch">
          <div class="template-tab" :class="{ active: activeType === 0 }" @click="switchType(0)">
            图片标签
          </div>
          <div class="template-tab" :class="{ active: activeType === 1 }" @click="switchType(1)">
            点云标签
          </div>
          <div class="template-tab" :class="{ active: activeType === 2 }" @click="switchType(2)">
            视频标签
          </div>
        </div>
      </template>
      <template #toolbar>
        <Button type="primary" @click="handleCreate">新增标签</Button>
        <Button type="primary" @click="handleDelete" :disabled="canDelete">删除标签</Button>
      </template>
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'clarity:note-edit-line',
              tooltip: '编辑',
              onClick: handleEdit.bind(null, record),
            },
          ]"
        />
      </template>
    </BasicTable>
    <LabelTemplateModal @register="registerModal" @success="handleSuccess" />
  </div>
</template>
<script lang="ts" setup>
  import { BasicTable, useTable, TableAction } from '/@/components/Table';
  import { useModal } from '/@/components/Modal';
  import LabelTemplateModal from './LabelTemplateModal.vue';
  import { getColumns, searchFormSchema } from './labelTemplate.data';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { createVNode, Ref, ref } from 'vue';
  import { Modal, Button } from 'ant-design-vue';
  import { ExclamationCircleOutlined } from '@ant-design/icons-vue';

  const { createMessage } = useMessage();
  const [registerModal, { openModal }] = useModal();

  /** 当前选项卡类型：0=图片标签，1=点云标签，2=视频标签 */
  const activeType = ref<number>(0);

  /** 按类型返回接口前缀：0=图片 labelTemplate，1=点云 labelTemplatePointcloud，2=视频 labelTemplateVideo */
  function getUrlPrefix(type: number): string {
    if (type === 1) return 'labelTemplatePointcloud';
    if (type === 2) return 'labelTemplateVideo';
    return 'labelTemplate';
  }

  const [registerTable, { reload, clearSelectedRowKeys, setProps }] = useTable({
    title: '标签库列表',
    api: (params) => {
      // 点云/视频标签走独立表接口，图片标签走 labelTemplate
      const url = getUrlPrefix(activeType.value) + '/query';
      return maHttp
        .get(
          {
            url,
            params,
            headers: {},
          },
          { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
        )
        .then((v) => {
          //完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
          v.items = v.result;
          v.total = v.page.total;
          return v;
        });
    },
    beforeFetch: (v) => {
      //发出分页查询请求前，将1-started页码（VBen）转换为0-started页码（Spring Page）
      v = Object.assign(v, {
        current: v.page,
        size: v.pageSize,
      });
      // 图片标签传 type=0 过滤；点云标签独立表无 type 字段，不传
      if (activeType.value === 0) {
        Object.assign(v, { type: 0 });
      }
      // 主动使用排序功能，则进行参数转换
      if (Object.hasOwn(v, 'field')) {
        Object.assign(v, {
          order: v.order === 'ascend' ? 'asc' : 'desc',
          sort: v.field,
        });
      }
      // 否则进行默认排序，根据id降序排序
      else {
        Object.assign(v, {
          order: 'desc',
          sort: 'id',
        });
      }
      return v;
    },
    columns: getColumns(activeType.value),
    rowSelection: {
      type: 'checkbox',
    },
    formConfig: {
      labelWidth: 120,
      showAdvancedButton: false,
      schemas: searchFormSchema,
      // 按钮组列定宽贴合按钮内容，消除列内左侧空白
      actionColOptions: {
        flex: 'none',
        style: { textAlign: 'right', maxWidth: 'none', width: '150px' },
      },
    },
    useSearchForm: true,
    showTableSetting: false,
    bordered: true,
    showIndexColumn: false,
    actionColumn: {
      width: 150,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
    handleSearchInfoFn(info) {
      return info;
    },
  });

  /** 切换选项卡：更新列定义并刷新表格 */
  async function switchType(type: number) {
    if (activeType.value === type) return;
    activeType.value = type;
    await setProps({ columns: getColumns(type) });
    clearSelectedRowKeys();
    await reload();
  }

  function handleCreate() {
    openModal(true, {
      isUpdate: false,
      type: activeType.value,
    });
  }

  function handleEdit(record: Recordable) {
    openModal(true, {
      labelTemplateId: record.id,
      isUpdate: true,
      type: activeType.value,
    });
  }

  function handleSuccess() {
    // 清空所有选中的keys
    clearSelectedRowKeys();
    // 刷新页面
    reload();
  }

  const handleDelete = async () => {
    console.log(selectedKeys);
    Modal.confirm({
      title: () => '确认删除选中的' + selectedKeys.value.length + '条数据?',
      maskClosable: true,
      icon: () => createVNode(ExclamationCircleOutlined),
      okText: () => '确定',
      okType: 'danger',
      cancelText: () => '取消',
      async onOk() {
        const ids = selectedRows.value.map((row) => row.id);
        try {
          const url = getUrlPrefix(activeType.value) + '/delete';
          await maHttp.delete(
            {
              url,
              data: { ids },
              headers: {},
            },
            { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
          );
          createMessage.success('标签删除成功！');
          // 清空所有选中的keys
          clearSelectedRowKeys();
          // 刷新页面
          await reload();
        } catch (e) {
          console.log('error', e);
        }
      },
    });
  };

  // 分页变化
  function onPageChange() {
    clearSelectedRowKeys();
  }

  // 处理多选框选中事件
  const canDelete: Ref<boolean> = ref(true);
  const selectedKeys: Ref<Array<any>> = ref([]);
  const selectedRows: Ref<Array<any>> = ref([]);
  const selectionChange = ({ keys, rows }) => {
    selectedKeys.value = keys;
    selectedRows.value = rows;
    canDelete.value = keys.length <= 0;
  };
</script>

<style scoped>
  /* 搜索表单行内的分类切换（左：图片标签/点云标签；右：搜索过滤组件） */
  .template-type-switch {
    display: flex;
    align-items: center;
    height: 100%;
    min-height: 32px;
    gap: 0;
  }
  .template-tab {
    padding: 5px 18px;
    font-size: 14px;
    color: #cdd9e8;
    cursor: pointer;
    border: 1px solid #2a3850;
    border-right: none;
    transition: all 0.2s;
    user-select: none;
    line-height: 1.5;
  }
  .template-tab:first-child {
    border-radius: 4px 0 0 4px;
  }
  .template-tab:last-of-type {
    border-radius: 0 4px 4px 0;
    border-right: 1px solid #2a3850;
  }
  .template-tab:hover {
    color: #4fc3f7;
    background: rgba(64, 158, 255, 0.08);
  }
  .template-tab.active {
    color: #4fc3f7;
    font-weight: 500;
    background: rgba(64, 158, 255, 0.12);
    border-color: #4fc3f7;
  }
  .template-tab.active + .template-tab {
    border-left-color: #4fc3f7;
  }
</style>
