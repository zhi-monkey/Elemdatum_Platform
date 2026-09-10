<template>
  <div>
    <div class="p-1 myForm">
      <BasicTable @register="registerTable">
        <template #toolbar
          ><a-button type="primary" @click="handAllRows"> 全部处理</a-button>
          <a-button type="primary" @click="handSelectedRows" :disabled="showButton">
            批量处理</a-button
          ><PopConfirmButton
            type="primary"
            disabled="true"
            @confirm="deleteSelectedRows"
            title="是否要删除这些报警信息?"
          >
            批量删除</PopConfirmButton
          ></template
        >
        <template #image="{ record }">
          <div class="flex justify-center" v-if="record.image">
            <Icon
              icon="ant-design:file-image-twotone"
              color="SteelBlue"
              @click="
                openAlertMsgModal(true, {
                  mediaType: 'image',
                  mediaPath: record.imagePath,
                  fileName:
                    record.subsystem === 'CENTRAL_PLATFORM'
                      ? record.monitor.scene.name +
                        '_' +
                        record.monitor.monitorName +
                        '_' +
                        record.model.modelName +
                        '_' +
                        record.createTime.replace(/-/g, '').replace(/\s+/g, '').replace(/:/g, '') +
                        '.jpg'
                      : record.scene +
                        '_' +
                        record.monitorName +
                        '_' +
                        record.modelName +
                        '_' +
                        record.createTime.replace(/-/g, '').replace(/\s+/g, '').replace(/:/g, '') +
                        '.jpg',
                  alertId: record.id,
                  description:
                    record.description.length > 5
                      ? record.description.substr(0, 6) + '...'
                      : record.description,
                  monitor:
                    record.subsystem === 'CENTRAL_PLATFORM'
                      ? record.monitor.monitorName
                      : record.monitorName,
                  alg:
                    record.subsystem === 'CENTRAL_PLATFORM'
                      ? record.model.modelName
                      : record.modelName,
                  status: record.status,
                  createTime: record.createTime,
                  alertMsg: record.description,
                })
              "
            />
          </div>
          <div v-else>-</div>
        </template>
        <template #video="{ record }">
          <div class="flex justify-center" v-if="record.video">
            <Icon
              color="SteelBlue"
              icon="ant-design:video-camera-twotone"
              @click="
                openAlertMsgModal(true, {
                  mediaType: 'video',
                  mediaPath: record.videoPath,
                  fileName:
                    record.subsystem === 'CENTRAL_PLATFORM'
                      ? record.monitor.scene.name +
                        '_' +
                        record.monitor.monitorName +
                        '_' +
                        record.model.modelName +
                        '_' +
                        record.createTime.replace(/-/g, '').replace(/\s+/g, '').replace(/:/g, '') +
                        '.mp4'
                      : record.scene +
                        '_' +
                        record.monitorName +
                        '_' +
                        record.modelName +
                        '_' +
                        record.createTime.replace(/-/g, '').replace(/\s+/g, '').replace(/:/g, '') +
                        '.mp4',
                  alertId: record.id,
                  description:
                    record.description.length > 5
                      ? record.description.substr(0, 6) + '...'
                      : record.description,
                  monitor:
                    record.subsystem === 'CENTRAL_PLATFORM'
                      ? record.monitor.monitorName
                      : record.monitorName,
                  alg:
                    record.subsystem === 'CENTRAL_PLATFORM'
                      ? record.model.modelName
                      : record.modelName,
                  status: record.status,
                  createTime: record.createTime,
                  alertMsg: record.description,
                })
              "
            />
          </div>
          <div v-else>-</div>
        </template>
        <template #audio="{ record }">
          <div class="flex justify-center" v-if="record.audio">
            <Icon
              color="SteelBlue"
              icon="ant-design:audio-twotone"
              @click="
                openAlertMsgModal(true, {
                  mediaType: 'audio',
                  mediaPath: record.audioPath,
                  fileName:
                    record.subsystem === 'CENTRAL_PLATFORM'
                      ? record.monitor.scene.name +
                        '_' +
                        record.monitor.monitorName +
                        '_' +
                        record.model.modelName +
                        '_' +
                        record.createTime.replace(/-/g, '').replace(/\s+/g, '').replace(/:/g, '') +
                        '.mp3'
                      : record.scene +
                        '_' +
                        record.monitorName +
                        '_' +
                        record.modelName +
                        '_' +
                        record.createTime.replace(/-/g, '').replace(/\s+/g, '').replace(/:/g, '') +
                        '.mp3',
                  alertId: record.id,
                  description:
                    record.description.length > 5
                      ? record.description.substr(0, 6) + '...'
                      : record.description,
                  monitor:
                    record.subsystem === 'CENTRAL_PLATFORM'
                      ? record.monitor.monitorName
                      : record.monitorName,
                  alg:
                    record.subsystem === 'CENTRAL_PLATFORM'
                      ? record.model.modelName
                      : record.modelName,
                  status: record.status,
                  createTime: record.createTime,
                  alertMsg: record.description,
                })
              "
            />
          </div>
          <div v-else>-</div>
        </template>
        <template #action="{ record }">
          <TableAction
            :actions="[
              {
                icon: 'clarity:note-edit-line',
                tooltip: '处理',
                onClick: handleEdit.bind(null, record),
              },
              {
                icon: 'ant-design:delete-outlined',
                tooltip: '删除',
                color: 'error',
                popConfirm: {
                  title: '是否确认删除',
                  confirm: handleDelete.bind(null, record),
                },
              },
            ]"
          />
        </template>
      </BasicTable>
    </div>
    <AlertMsgModalList @register="registerAlertModalList" @success="handleSuccess" />
    <AlertMsgModal @register="registerAlertMsgModal" :modalData="modalData" />
    <AlertModal @register="registerModal" @success="handleSuccess" />
    <AllAlertModal @register="registerAllAlertModal" @success="handleSuccess" />
  </div>
</template>

<script lang="ts" setup>
  import { onMounted, ref } from 'vue';
  import { BasicColumn, BasicTable, TableAction, useTable } from '/@/components/Table';
  import { PopConfirmButton } from '/@/components/Button';
  import { useModal } from '/@/components/Modal';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { Icon } from '/@/components/Icon';
  import { searchFormSchema } from '/@/views/mineai/monitor/alert-msg/alert.data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import AlertModal from './AlertModal.vue';
  import AlertMsgModal from '/@/views/mineai/monitor/alert-msg/AlertMsgModal.vue';
  import AlertMsgModalList from '/@/views/mineai/monitor/alert-msg/AlertModalList.vue';
  import AllAlertModal from './AllAlertModal.vue';
  import { getModelData, getMonitorData } from '/@/views/mineai/monitor/alert-msg/alert.data';
  import AButton from '/@/components/Button/src/BasicButton.vue';

  const props = defineProps<{ columns: BasicColumn[] }>();
  const { createMessage } = useMessage();
  const [registerModal, { openModal: openAlertModal }] = useModal();
  const [registerAlertMsgModal, { openModal: openAlertMsgModal }] = useModal();
  const [registerAlertModalList, { openModal: openAlertModalList }] = useModal();
  const [registerAllAlertModal, { openModal: openAllAlertModal }] = useModal();

  // const visible = ref(false);

  interface alertFileInfo {
    mediaType: string;
    mediaPath: string;
    fileName: string;
  }
  const showButton = ref(true);
  const modalData = ref<alertFileInfo>();
  async function getData(params) {
    const v = await maHttp.get(
      {
        url: 'modelAlert/dynamicFindModelAlert',
        params,
        headers: {
          // @ts-ignore
          ignoreCancelToken: true,
        },
      },
      { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
    );
    //完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
    v.items = v.content;
    v.total = v.totalElements;
    return v;
  }

  const [registerTable, { reload, clearSelectedRowKeys, getSelectRows }] = useTable({
    title: '报警列表',
    api: async (params) => {
      return await getData(params);
    },
    beforeFetch: (v) => {
      //发出分页查询请求前，将1-started页码（VBen）转换为0-started页码（Spring Page）
      v.page -= 1;
      return v;
    },
    afterFetch: (itemList) => {
      //GET请求被解析为 Vben表格Table的行后，做一些操作
      return itemList;
    },
    columns: props.columns,
    formConfig: {
      schemas: searchFormSchema,
      labelWidth: 80,
      baseColProps: { span: 4 },
      actionColOptions: { span: 3 },
    },
    rowSelection: {
      type: 'checkbox',
      onChange: (selectedKeys) => {
        showButton.value = selectedKeys.length == 0;
      },
    },
    rowKey: 'id',
    useSearchForm: true,
    showTableSetting: true,
    bordered: true,
    showIndexColumn: false,
    striped: false,
    handleSearchInfoFn(info) {
      return info;
    },
    actionColumn: {
      width: 60,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
  });

  function handleEdit(record: Recordable) {
    openAlertModal(true, {
      record,
      isUpdate: true,
    });
  }

  function handleDelete(record: Recordable) {
    maHttp
      .post(
        {
          url: 'modelAlert/deleteModelAlert',
          params: record,
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(() => {
        createMessage.success('删除成功！');

        reload();
      });
  }
  //全部处理
  function handAllRows() {
    openAllAlertModal();
  }
  //批量处理接口
  function handSelectedRows() {
    let array = getSelectRows();
    let modelAlertList = '';
    array.forEach((v) => {
      modelAlertList += v.id + ' ';
    });
    if (array.length === 0) {
      createMessage.error('请先选择要处理的报警信息');
      return false;
    }
    openAlertModalList(true, { ids: modelAlertList });
  }
  //批量删除功能
  function deleteSelectedRows() {
    let array = getSelectRows();
    let modelAlertList = '';
    array.forEach((v) => {
      modelAlertList += v.id + ' ';
    });
    if (array.length === 0) {
      createMessage.error('请先选择要处理的报警信息');
      return false;
    }
    maHttp
      .post(
        {
          url: 'modelAlert/deleteModelAlertList',
          data: { modelAlertLists: modelAlertList },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(() => {
        createMessage.success('删除成功！');
        clearSelectedRowKeys();
        reload();
      });
  }

  function handleSuccess() {
    reload();
    clearSelectedRowKeys();
  }

  onMounted(async () => {
    await getMonitorData();
    await getModelData(null);
  });
</script>

<style lang="less" scoped>
  .myForm :deep(.ant-select-selection-item) {
    width: 100px;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .myForm :deep(.ant-table-wrapper),
  :deep(.ant-table-thead > tr > th),
  :deep(.vben-basic-table-form-container .ant-form) {
    background-color: transparent;
  }

  .myForm :deep(.ant-table),
  :deep(.ant-table-header),
  :deep(.ant-table-fixed-header > .ant-table-content > .ant-table-scroll > .ant-table-body),
  :deep(.ant-table-cell-fix-left),
  :deep(.ant-table-cell-fix-right) {
    background: transparent;
  }
</style>
