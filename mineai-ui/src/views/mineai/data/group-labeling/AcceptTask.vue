<template>
  <div>
    <BasicTable @register="registerAcceptTable">
      <template #action="{ record }">
        <TableAction
          :actions="[
            {
              icon: 'ant-design:highlight-outlined',
              tooltip: '标注',
              onClick: handleLabel.bind(null, record),
              disabled: record.status === 2 || record.status === 3,
            },
            {
              icon: 'ant-design:check-outlined',
              tooltip: '提交',
              popConfirm: {
                title: '是否确认提交',
                confirm: handleSubmit.bind(null, record),
              },
              ifShow:
                record.currentOffset === record.endOffset - record.startOffset + 1 &&
                record.status !== 2 &&
                record.status === 1,
            },
          ]"
        />
      </template>
    </BasicTable>
    <GroupLabelingModal @register="registerModal" />
  </div>
</template>
<script setup lang="ts">
  import { BasicTable, TableAction, useTable } from '/@/components/Table';
  import GroupLabelingModal from './GroupLabelingModal.vue';
  import { workerColumns } from './data';
  import { maHttp } from '/@/utils/http/axios';
  import { DubheBackendUrlEnum } from '/@/enums/mineaiEnum';
  import { useModal } from '/@/components/Modal';
  import { detectFileList, submitSubtask } from '/@/views/mineai/data/dataset-details2/api';
  import { ElMessage as Message } from 'element-plus';
  import { onBeforeRouteLeave, useRoute, useRouter } from 'vue-router';
  import { ref, watch } from 'vue';

  const [registerModal] = useModal();

  // 路由和状态
  const route = useRoute();
  const router = useRouter();

  // 恢复或初始化 tabKey
  const savedTabKey = localStorage.getItem('activeTabKey');
  const activeKey = ref(route.state?.tabKey || savedTabKey || '1');

  // 保存 tabKey 到 localStorage
  watch(
    () => activeKey.value,
    (key) => {
      localStorage.setItem('activeTabKey', key);
    },
  );

  // 监听路由离开，保存 tab 状态
  onBeforeRouteLeave((to, from, next) => {
    to.state = {
      ...to.state,
      tabKey: activeKey.value,
    };
    next();
  });
  const [registerAcceptTable, { reload }] = useTable({
    api: async (params) => {
      const v = await maHttp.get(
        {
          url: '/datasets/team/subtask',
          params,
          headers: {},
        },
        { urlPrefix: DubheBackendUrlEnum.DUBHE_DATASET },
      );
      // 完成前端GET方法后，将Spring Page的字段转换为 VBen所需字段
      v.items = v.result;
      v.total = v.page.total;
      for (let i = 0; i < v.items.length; i++) {
        v.items[i].process =
          v.items[i].currentOffset + '/' + (v.items[i].endOffset - v.items[i].startOffset + 1);
        v.items[i].present =
          v.items[i].endOffset - v.items[i].startOffset + 1 === 0
            ? '0.00%'
            : (
                (v.items[i].currentOffset / (v.items[i].endOffset - v.items[i].startOffset + 1)) *
                100
              ).toFixed(2) + '%';
      }
      return v;
    },
    beforeFetch: (v) => {
      //发出分页查询请求前，将1-started页码（VBen）转换为0-started页码（Spring Page）
      v = Object.assign(v, {
        current: v.page,
        size: v.pageSize,
        type: 0,
      });
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
    columns: workerColumns,
    formConfig: {
      labelWidth: 120,
      showAdvancedButton: false,
    },
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

  // 跳转标注
  const handleLabel = async (item) => {
    const { result: img } = await detectFileList(item.datasetId, {
      limit: 1,
      offset: item.startOffset,
      requestParams: {},
    });
    await router.push({
      path: `/maData/annotate/${item.datasetId}/${item.name}`,
      state: {
        imgId: img[0].id,
        team: true,
        taskId: item.taskId,
        subtaskId: item.id,
        subtaskUpdateTime: item.updateTime ? new Date(item.updateTime).getTime() : undefined,
        tabKey: activeKey.value,
        startOffset: item.startOffset,
        endOffset: item.endOffset,
      },
    });
  };

  // 提交子任务
  const handleSubmit = async (record) => {
    try {
      await submitSubtask(record.taskId, record.id);
      Message.success('子任务提交成功！');
    } catch (e) {
      Message.error('子任务提交失败！');
    } finally {
      await reload();
    }
  };
</script>
<style scoped lang="less"></style>
