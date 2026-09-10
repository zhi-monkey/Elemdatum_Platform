<template>
  <BasicTable @register="registerTable">
    <template #toolbar>
      <Button type="primary" @click="handleCreate">新增镜像</Button>
    </template>
    <template #action="{ record }">
      <TableAction
        :actions="[
          {
            icon: 'clarity:note-edit-line',
            tooltip: '编辑',
            onClick: handleEdit.bind(null, record),
            //已公开的镜像不允许编辑
            /*disabled: (record.canEdit && !record.public) !== true,*/
          },
          /* {
            icon: 'clarity:fast-forward-line',
            tooltip: '公开镜像',
            onClick: handleToPublic.bind(null, record),
            disabled: record.public == true,
          },*/

          {
            icon: 'ant-design:delete-outlined',
            tooltip: '删除',
            color: 'error',
            //已经公开的镜像不能删除，与之前的canDelete逻辑一起考虑
            disabled: !record.canDelete,
            popConfirm: { title: '是否确认删除', confirm: handleDelete.bind(null, record) },
          },
        ]"
      />
    </template>
  </BasicTable>

  <imageModel @register="registerCreateModal" @success="handleSuccess" />
</template>

<script setup lang="ts">
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { columns, searchSchema } from '/@/views/mineai/train/algorithmManage/imageCenter/data';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { useModal } from '/@/components/Modal';
  import ImageModel from '/@/views/mineai/train/imageCenter/imageModel.vue';
  import { Button } from '/@/components/Button';
  import { dynamicFindModelVersionPage } from '/@/views/mineai/train/algorithmManage/imageCenter/api';

  const { createMessage } = useMessage();
  const [registerCreateModal, { openModal: create }] = useModal();

  const [registerTable, { reload }] = useTable({
    title: '镜像中心',
    api: async (param) => {
      const res = await dynamicFindModelVersionPage(
        param.page,
        param.pageSize,
        param.order,
        param.vagueInfo,
        param.chipType,
        param.showName,
        param.description,
        param.userName,
      );
      res.content.forEach((item) => {
        item.usage = '';
        if (item.trainable) {
          item.usage += '训练';
        }
        if (item.inspectable) {
          if (item.usage) {
            item.usage += '-';
          }
          item.usage += '转换';
        }
        if (item.autoLabel) {
          if (item.usage) {
            item.usage += '-';
          }
          item.usage += '自动标注';
        }
      });
      return {
        items: res.content,
        total: res.totalElements,
      };
    },
    beforeFetch: (v) => {
      //发出分页查询请求前，将1-started页码（VBen）转换为0-started页码（Spring Page）
      v.page -= 1;
      return v;
    },
    afterFetch: async (v) => {
      //GET请求被解析为 VBen表格Table的行后，做一些操作
      return v;
    },
    columns,
    formConfig: {
      labelWidth: 120,
      schemas: searchSchema,
      showAdvancedButton: false,
      showResetButton: true,
    },
    useSearchForm: true,
    showTableSetting: false,
    bordered: true,
    showIndexColumn: false,
    handleSearchInfoFn(info) {
      console.log(info);
      return info;
    },
    actionColumn: {
      width: 150,
      title: '操作',
      dataIndex: 'action',
      slots: { customRender: 'action' },
      fixed: 'right',
    },
  });

  function handleCreate(record) {
    create(true, {
      record,
      isUpdate: false,
    });
  }

  function handleEdit(record) {
    create(true, {
      record,
      isUpdate: true,
    });
  }

  function handleDelete(record) {
    maHttp
      .get(
        {
          url: 'modelVersion/deleteImage',
          params: {
            modelVersionId: record.id,
          },
          headers: {
            // @ts-ignore
            ignoreCancelToken: true,
          },
        },
        { urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
      )
      .then(() => {
        createMessage.success('镜像删除成功！');
        reload();
      });
  }

  /*function handleToPublic(record) {
maHttp
.put(
{
  url: 'modelVersion/setPublic/' + record.id,
  params: {
    modelVersionId: record.id,
  },
  headers: {
    // @ts-ignore
    ignoreCancelToken: true,
  },
},
{ urlPrefix: MaBackendUrlEnum.MODEL_MANAGER },
)
.then(() => {
createMessage.success('公开镜像成功！');
reload();
});
}
*/
  function handleSuccess() {
    createMessage.success('成功！');
    reload();
  }
</script>
