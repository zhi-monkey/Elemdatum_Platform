<template>
  <div class="p-2">
    <div class="p-4 mb-2 bg-transparent">
      <BasicForm @register="registerForm" class="myForm" />
    </div>
    <div class="p-2 bg-transparent card-list" id="list">
      <List
        :grid="{ gutter: 5, xs: 1, sm: 2, md: 4, lg: 4, xl: 4, xxl: 4 }"
        :data-source="data"
        :pagination="paginationProp"
      >
        <template #renderItem="{ item }">
          <ListItem>
            <DvBorderBox2 :key="index">
              <Card>
                <template #title></template>
                <template #cover>
                  <div>
                    <a-image
                      :src="getfiledownloadUrl(item.imagePath)"
                      :preview="true"
                      widht="100%"
                      :height="275"
                    />
                  </div>
                </template>
                <template #actions>
                  <!--              <SettingOutlined key="setting" />-->
                  <div>
                    <a-tooltip trigger="hover" title="处理报警信息">
                      <Icon
                        color="SteelBlue"
                        icon="ant-design:edit-twotone"
                        :onClick="handleEdit.bind(null, item)"
                      />
                    </a-tooltip>
                  </div>
                  <div v-if="item.video">
                    <a-tooltip trigger="hover" title="查看视频报警内容">
                      <Icon
                        color="SteelBlue"
                        icon="ant-design:video-camera-twotone"
                        @click="
                          openAlertMsgModal(true, {
                            mediaType: 'video',
                            mediaPath: item.videoPath,
                            fileName:
                              item.subsystem === 'CENTRAL_PLATFORM'
                                ? item.monitor.scene.name +
                                  '_' +
                                  item.monitor.monitorName +
                                  '_' +
                                  item.model.modelName +
                                  '_' +
                                  item.createTime
                                    .replace(/-/g, '')
                                    .replace(/\s+/g, '')
                                    .replace(/:/g, '') +
                                  '.mp4'
                                : item.scene +
                                  '_' +
                                  item.monitorName +
                                  '_' +
                                  item.modelName +
                                  '_' +
                                  item.createTime
                                    .replace(/-/g, '')
                                    .replace(/\s+/g, '')
                                    .replace(/:/g, '') +
                                  '.mp4',
                            alertId: item.id,
                            description: item.description,
                            fullDescription: item.fullDescription,
                            fullMonitorName: item.fullMonitorName,
                            monitorName: item.monitorName,
                            fullModelName: item.fullModelName,
                            modelName: item.modelName,
                            status: item.status,
                            createTime: item.createTime,
                            alertMsg: item.description,
                          })
                        "
                      />
                    </a-tooltip>
                  </div>
                  <div v-else>-</div>
                  <div>
                    <a-tooltip trigger="hover" title="删除报警信息">
                      <a-popconfirm
                        title="确定要删除此报警信息吗？"
                        :onConfirm="handleDelete.bind(null, item)"
                        ok-text="是"
                        cancel-text="否"
                      >
                        <Icon color="SteelBlue" icon="ant-design:delete-twotone" />
                      </a-popconfirm>
                    </a-tooltip>
                  </div>
                </template>

                <CardMeta>
                  <template #description>
                    <div style="text-align: center">
                      <a-row>
                        <a-col :span="12">
                          <div style="text-align: left">报警ID：{{ item.id }}</div>
                        </a-col>
                        <a-col :span="12" style="text-align: left">
                          <span>处理状态：</span>
                          <span v-if="item.status === 1" style="color: red">未处理</span>
                          <span v-else-if="item.status === 2" style="color: blue">已忽略</span>
                          <span v-else-if="item.status === 3" style="color: green">已线下处理</span>
                        </a-col>
                      </a-row>
                    </div>
                    <div style="text-align: center">
                      <a-row>
                        <a-col :span="12">
                          <div style="text-align: left">
                            <span>报警内容：</span>
                            <a-tooltip trigger="hover">
                              <template #title>{{ item.fullDescription }}</template>
                              <span style="color: red">{{ item.description }}</span>
                            </a-tooltip>
                          </div>
                        </a-col>
                        <a-col :span="12" style="text-align: left">
                          <div>上报时间：{{ item.createTime }}</div>
                        </a-col>
                      </a-row>
                    </div>
                    <div style="text-align: center">
                      <a-row>
                        <a-col :span="12">
                          <div style="text-align: left">
                            <span>监控设备：</span>
                            <a-tooltip trigger="hover">
                              <template #title>{{ item.fullMonitorName }}</template>
                              <span>{{ item.monitorName }}</span>
                            </a-tooltip>
                          </div>
                        </a-col>
                        <a-col :span="12" style="text-align: left">
                          <span>算法名称： </span>
                          <a-tooltip trigger="hover">
                            <template #title>{{ item.fullModelName }}</template>
                            <span>{{ item.modelName }}</span>
                          </a-tooltip>
                        </a-col>
                      </a-row>
                    </div>
                  </template>
                </CardMeta>
              </Card>
            </DvBorderBox2>
          </ListItem>
        </template>
      </List>
      <AlertModal @register="registerModal" @success="handleSuccess" />
      <AlertMsgModal @register="registerAlertMsgModal" />
    </div>
  </div>
</template>
<script lang="ts" setup>
  import { onActivated, onMounted, ref } from 'vue';
  import {
    Card,
    Col as ACol,
    Image as AImage,
    List,
    Popconfirm as APopconfirm,
    Row as ARow,
    Tooltip as ATooltip,
  } from 'ant-design-vue';
  import { BasicForm, useForm } from '/@/components/Form';
  import { propTypes } from '/@/utils/propTypes';
  import { isFunction } from '/@/utils/is';
  import AlertModal from '/@/views/mineai/monitor/alertMsgNew/AlertModal.vue';
  import { maHttp } from '/@/utils/http/axios';
  import { MaBackendUrlEnum } from '/@/enums/mineaiEnum';
  import {
    getModelData,
    getMonitorData,
    searchFormSchema,
  } from '/@/views/mineai/monitor/alertMsgNew/alertData';
  import { useModal } from '/@/components/Modal';
  import { BorderBox2 as DvBorderBox2 } from '@kjgl77/datav-vue3';
  import elementResizeDetectorMaker from 'element-resize-detector';
  import debounce from 'lodash/debounce';
  import { useMessage } from '/@/hooks/web/useMessage';
  import AlertMsgModal from '/@/views/mineai/monitor/alertMsgNew/AlertMsgModal.vue';
  import Icon from '/@/components/Icon';

  const ListItem = List.Item;
  const CardMeta = Card.Meta;
  // 获取slider属性
  // const sliderProp = computed(() => useSlider(4));
  const { createMessage } = useMessage();
  // 组件接收参数
  const props = defineProps({
    // 请求API的参数
    params: propTypes.object.def({}),
    //api
    api: propTypes.func,
  });
  //暴露内部方法
  const emit = defineEmits(['getMethod', 'delete']);
  //数据
  const data = ref([]);
  //设置页数
  let page = ref(1);
  // 切换每行个数
  // cover图片自适应高度
  //修改pageSize并重新请求数据

  // const height = computed(() => {
  //   return `h-${120 - grid.value * 6}`;
  // });
  //表单
  const [registerForm, { validate }] = useForm({
    schemas: searchFormSchema,
    labelWidth: 100,
    autoSubmitOnEnter: false,
    submitFunc: handleSubmit,
    resetFunc: handleReset,
    showAdvancedButton: true,
    alwaysShowLines: 1,
  });

  const index = ref(0);

  let erd: elementResizeDetectorMaker.Erd = elementResizeDetectorMaker();

  //表单提交
  async function handleSubmit() {
    const data = await validate();
    page.value = 1;
    await fetch(data);
  }
  async function handleReset() {
    page.value = 1;
    await fetch();
  }

  //侧边栏变化
  // function sliderChange() {
  //   pageSize.value = 8;
  //   fetch();
  // }

  //打开处理报警信息弹窗
  const [registerModal, { openModal: openAlertModal }] = useModal();
  //打开视频报警信息弹窗
  const [registerAlertMsgModal, { openModal: openAlertMsgModal }] = useModal();

  async function handleEdit(record: Recordable) {
    openAlertModal(true, {
      record,
      isUpdate: true,
    });
  }

  async function handleDelete(record: Recordable) {
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
      .then(async () => {
        createMessage.success('删除成功！');
        const data = await validate();
        //重新请求接口，刷新页面
        await fetch(data);
      });
  }

  // 自动请求并暴露内部方法
  onMounted(() => {
    getMonitorData();
    getModelData(null);
    fetch();
    emit('getMethod', fetch);
    erd.listenTo(
      document.getElementById('list') as HTMLElement,
      // 防抖函数，只执行1s内最后一次回调
      debounce(function () {
        // div宽度变化，子组件重新渲染
        index.value += 1;
      }, 1000),
    );
  });
  onActivated(async () => {});

  //接口请求以及相关数据处理
  async function fetch(p = {}) {
    const { api, params } = props;
    if (api && isFunction(api)) {
      const res = await api({ ...params, page: page.value - 1, pageSize: pageSize.value, ...p });
      res.items.forEach((item) => {
        item.fullDescription = item?.description;
        if (item.fullDescription !== '' && item.fullDescription !== null) {
          item.description =
            item?.description.length >= 6
              ? item.description?.substring(0, 6) + '...'
              : item?.description;
        } else {
          item.description = '无';
        }
        if (item.subsystem === 'CENTRAL_PLATFORM') {
          item.fullMonitorName = item?.monitor?.monitorName;
          item.monitorName =
            item?.monitor?.monitorName.length >= 6
              ? item?.monitor?.monitorName.substring(0, 6) + '...'
              : item?.monitor?.monitorName;
          item.fullModelName = item?.model?.modelName;
          item.modelName =
            item?.model?.modelName.length >= 6
              ? item?.model?.modelName.substring(0, 6) + '...'
              : item?.model?.modelName;
        } else {
          item.fullMonitorName = item?.monitorName;
          item.monitorName =
            item?.monitorName.length >= 6
              ? item?.monitorName.substring(0, 6) + '...'
              : item?.monitorName;
          item.fullModelName = item?.modelName;
          item.modelName =
            item?.modelName.length >= 6 ? item?.modelName.substring(0, 6) + '...' : item?.modelName;
        }
      });
      data.value = res.items;
      total.value = res.total;
    }
  }

  async function handleSuccess() {
    const data = await validate();
    await fetch(data);
  }

  //分页相关

  const pageSize = ref(8);
  const total = ref(0);
  const paginationProp = ref({
    showSizeChanger: false,
    showQuickJumper: true,
    pageSize,
    current: page,
    total,
    showTotal: (total) => `总 ${total} 条`,
    onChange: pageChange,
    onShowSizeChange: pageSizeChange,
  });

  async function pageChange(p) {
    const data = await validate();
    page.value = p;
    await fetch(data);
  }

  function pageSizeChange(_current, size) {
    pageSize.value = size;
    fetch();
  }

  //图片接口
  function getfiledownloadUrl(filePath) {
    //正常使用的接口
    return 'raw/' + encodeURIComponent(filePath);
  }

  // function getLocalFile(filePath) {
  //   //保留本地图片下载接口，方便本地测试
  //   return (
  //     MaBackendUrlEnum.DATA_MANAGER +
  //     'storage/getFileStreamNew?' +
  //     'fileName=' +
  //     encodeURIComponent(filePath) +
  //     '&datasetName=' +
  //     encodeURIComponent('/')
  //   );
  // }
</script>

<style scoped>
  .card-list :deep(.ant-card-actions) {
    background-color: transparent;
    border-top: transparent;
  }

  .card-list :deep(.ant-card-cover) {
    padding: 10px 15px 0 15px;
  }

  .card-list :deep(.ant-card-bordered) {
    border: transparent;
  }

  .myForm >>> .ant-select-selection-item {
    width: 100px;
    overflow: hidden;
    text-overflow: ellipsis;
  }
</style>
