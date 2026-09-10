// Modal.vue
<template>
  <BasicModal v-bind="$attrs" @register="registerModal" title="视频抽帧" @ok="handleSubmit">
    <BasicForm
      :labelWidth="100"
      :schemas="schemas"
      :actionColOptions="{ span: 24 }"
      :showResetButton="false"
      :showSubmitButton="false"
      @register="registerForm"
    />
    <div style="padding-left: 25px; color: #666; font-size: 13px">
      <p style="margin: 0">预计生成的图片数量为：{{ imgNum }} 张</p>
      <p style="margin: 5px 0 0 0; font-size: 12px"
        >（基于 25fps 计算，实际抽帧数量可能会有出入）</p
      >
    </div>
  </BasicModal>
</template>

<script lang="ts" setup>
  import { BasicForm, FormSchema, useForm } from '/@/components/Form';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import moment from 'moment';
  import { ref, watch } from 'vue';
  import { videoSample } from '../../../api/index';
  import { ElMessage as Message } from 'element-plus';
  import { calSeconds } from '/@/utils';
  import { useVideoStore } from '/@/store/modules/video';
  import { bucketHost, bucketName } from '/@/utils/dubhe';

  const videoStore = useVideoStore();
  const maxTime = ref(0);
  const startTime = ref(0);
  const endTime = ref(0);
  const splitType = ref(1);
  const splitInterval = ref(1);
  const imgNum = ref(0);
  const status = ref(0);
  const videoWidth = ref(0);
  const videoHeight = ref(0);
  const videoFrameRate = ref(25); // 硬编码为25fps
  const resolutionType = ref(1); // 1: 原分辨率, 2: 自定义分辨率
  const customWidth = ref(0);
  const customHeight = ref(0);
  const videoName = ref('');
  const videoUrl = ref('');
  const emits = defineEmits(['submit']);

  const schemas: FormSchema[] = [
    {
      field: 'datasetId',
      label: '数据集编号',
      component: 'Input',
      show: false,
      required: true,
      componentProps: { readonly: true },
    },
    {
      field: 'fileId',
      label: '视频文件编号',
      component: 'Input',
      required: true,
      show: false,
      componentProps: { readonly: true },
    },
    {
      field: 'videoResolution',
      label: '视频分辨率',
      component: 'Input',
      componentProps: {
        readonly: true,
        placeholder: '加载中...',
      },
    },
    {
      field: 'resolutionType',
      label: '分辨率选项',
      component: 'RadioButtonGroup',
      defaultValue: 1,
      required: true,
      componentProps: {
        options: [
          { label: '原分辨率', value: 1 },
          { label: '自定义分辨率', value: 2 },
        ],
        onChange: (e) => {
          resolutionType.value = e;
        },
      },
    },
    {
      field: 'customWidth',
      label: '自定义宽度',
      component: 'InputNumber',
      defaultValue: 0,
      ifShow: ({ values }) => values.resolutionType === 2,
      componentProps: {
        min: 1,
        max: 7680,
        placeholder: '请输入宽度',
        onChange: (e) => {
          customWidth.value = e;
        },
      },
      rules: [
        {
          required: true,
          validator: async (_rule, value) => {
            if (resolutionType.value === 2 && (!value || value <= 0)) {
              return Promise.reject('请输入有效的宽度');
            }
            return Promise.resolve();
          },
          trigger: 'change',
        },
      ],
    },
    {
      field: 'customHeight',
      label: '自定义高度',
      component: 'InputNumber',
      defaultValue: 0,
      ifShow: ({ values }) => values.resolutionType === 2,
      componentProps: {
        min: 1,
        max: 4320,
        placeholder: '请输入高度',
        onChange: (e) => {
          customHeight.value = e;
        },
      },
      rules: [
        {
          required: true,
          validator: async (_rule, value) => {
            if (resolutionType.value === 2 && (!value || value <= 0)) {
              return Promise.reject('请输入有效的高度');
            }
            return Promise.resolve();
          },
          trigger: 'change',
        },
      ],
    },
    {
      field: 'splitType',
      label: '抽帧间隔单位',
      component: 'RadioButtonGroup',
      defaultValue: 1,
      required: true,
      componentProps: {
        options: [
          { label: '帧', value: 1 },
          { label: '秒', value: () => videoFrameRate.value || 25 },
        ],
        onChange: (e) => {
          splitType.value = e;
        },
      },
    },
    {
      field: 'frameInterval',
      label: '抽帧间隔',
      component: 'InputNumber',
      defaultValue: 1,
      componentProps: {
        min: 1,
        onChange: (e) => {
          splitInterval.value = e;
        },
      },
      required: true,
    },
    {
      field: 'startTime',
      label: '起始时间',
      component: 'TimePicker',
      componentProps: {
        allowClear: false,
        onChange: (e) => {
          startTime.value = calSeconds(e);
        },
      },
      rules: [
        {
          required: true,
          // @ts-ignore
          validator: async (rule, value) => {
            if (calSeconds(value) > maxTime.value) {
              /* eslint-disable-next-line */
              return Promise.reject('不能超过视频最大时间');
            }
            if (calSeconds(value) > endTime.value) {
              /* eslint-disable-next-line */
              return Promise.reject('不能大于抽帧结束时间');
            }
            return Promise.resolve();
          },
          trigger: 'change',
        },
      ],
    },
    {
      field: 'endTime',
      label: '结束时间',
      component: 'TimePicker',
      componentProps: {
        allowClear: false,
        onChange: (e) => {
          endTime.value = calSeconds(e);
        },
      },
      rules: [
        {
          required: true,
          // @ts-ignore
          validator: async (rule, value) => {
            if (calSeconds(value) > maxTime.value) {
              /* eslint-disable-next-line */
              return Promise.reject('不能超过视频最大时间');
            }
            if (calSeconds(value) < startTime.value) {
              /* eslint-disable-next-line */
              return Promise.reject('不能小于抽帧起始时间');
            }
            return Promise.resolve();
          },
          trigger: 'change',
        },
      ],
    },
  ];

  const [registerForm, { setFieldsValue, validate, updateSchema }] = useForm({
    labelWidth: 100,
    schemas: schemas,
    showActionButtonGroup: false,
  });

  // 统一拼接 MinIO 文件地址：老 dubhe 数据 url 已含 bucket 前缀，新数据是 objectKey（不含 bucket），
  // MinIO 直链必须带上 bucket 名（例如 /cz-dev/），否则返回 403。
  function getFullFileUrl(filePath?: string): string {
    if (!filePath) {
      return '';
    }
    if (/^https?:\/\//i.test(filePath)) {
      return filePath;
    }
    if (filePath.startsWith(`${bucketName}/`)) {
      return `${bucketHost}/${filePath}`;
    }
    return `${bucketHost}/${bucketName}/${filePath}`;
  }

  const [registerModal, { setModalProps, closeModal }] = useModalInner(async (data) => {
    // 重置抽帧配置
    resetSplitInfo();

    // 先设置加载状态
    setModalProps({ confirmLoading: true });

    // 保存视频URL
    videoUrl.value = data.url || '';
    // 使用HTML5 Video元素获取视频分辨率
    try {
      const resolution = await getVideoResolution(getFullFileUrl(data.url));
      videoWidth.value = resolution.width;
      videoHeight.value = resolution.height;
    } catch (error) {
      console.error('获取视频分辨率失败:', error);
      Message.warning('获取视频分辨率失败，将使用默认值');
      videoWidth.value = 1920;
      videoHeight.value = 1080;
    } finally {
      setModalProps({ confirmLoading: false });
    }
    // 可选的最大时间
    maxTime.value = calSeconds(moment(data.endTime, 'HH:mm:ss'));
    startTime.value = moment('00:00:00', 'HH:mm:ss').seconds();
    endTime.value = maxTime.value;
    status.value = data.status;
    videoName.value = data.name || '';
    if (maxTime.value >= 60) {
      const frameRate = videoFrameRate.value || 25;
      await updateSchema({
        field: 'splitType',
        componentProps: {
          options: [
            { label: '帧', value: 1 },
            { label: '秒', value: frameRate },
            { label: '分', value: frameRate * 60 },
          ],
        },
      });
    } else if (maxTime.value >= 60 * 60) {
      const frameRate = videoFrameRate.value || 25;
      await updateSchema({
        field: 'splitType',
        componentProps: {
          options: [
            { label: '帧', value: 1 },
            { label: '秒', value: frameRate },
            { label: '分', value: frameRate * 60 },
            { label: '小时', value: frameRate * 60 * 60 },
          ],
        },
      });
    } else {
      const frameRate = videoFrameRate.value || 25;
      await updateSchema({
        field: 'splitType',
        componentProps: {
          options: [
            { label: '帧', value: 1 },
            { label: '秒', value: frameRate },
          ],
        },
      });
    }

    await setFieldsValue({
      fileId: data.id,
      datasetId: data.datasetId,
      splitType: 1,
      frameInterval: 1,
      startTime: moment('00:00:00', 'HH:mm:ss'),
      endTime: moment(data.endTime, 'HH:mm:ss'),
      videoResolution:
        videoWidth.value && videoHeight.value
          ? `${videoWidth.value} × ${videoHeight.value}`
          : '未知',
      resolutionType: 1,
      customWidth: videoWidth.value || 1920,
      customHeight: videoHeight.value || 1080,
    });
  });

  async function handleSubmit() {
    try {
      const values = await validate();
      setModalProps({ confirmLoading: true });

      const requestData: any = {
        datasetId: values.datasetId,
        fileId: values.fileId,
        startTime: calSeconds(values.startTime),
        endTime: calSeconds(values.endTime),
        frameInterval: values.splitType * values.frameInterval,
        resolutionType: values.resolutionType,
      };

      // 如果选择自定义分辨率，添加宽高参数
      if (values.resolutionType === 2) {
        requestData.customWidth = values.customWidth;
        requestData.customHeight = values.customHeight;
      }
      await videoSample(requestData);
      Message.success('下达抽帧任务成功');
      // 添加当前video
      videoStore.addVideo(Number(values.fileId), status.value, videoName.value);
      emits('submit');
      closeModal();
    } catch (e) {
      console.error(e);
      Message.error('下达抽帧任务失败');
    } finally {
      setModalProps({ confirmLoading: false });
    }
  }

  // 重置抽帧配置
  function resetSplitInfo() {
    splitInterval.value = 1;
    splitType.value = 1;
    resolutionType.value = 1;
    customWidth.value = 0;
    customHeight.value = 0;
  }

  // 使用HTML5 Video元素获取视频分辨率
  function getVideoResolution(url: string): Promise<{ width: number; height: number }> {
    return new Promise((resolve, reject) => {
      const video = document.createElement('video');
      video.crossOrigin = 'anonymous';
      video.preload = 'metadata';

      video.onloadedmetadata = () => {
        resolve({
          width: video.videoWidth,
          height: video.videoHeight,
        });
        // 清理
        video.src = '';
      };

      video.onerror = () => {
        reject(new Error('无法加载视频'));
      };

      video.src = url;
    });
  }

  watch(
    [splitType, splitInterval, startTime, endTime],
    ([newVal1, newVal2, newVal3, newVal4]) => {
      // 使用硬编码帧率25fps
      const actualFrameRate = 25;
      imgNum.value = Math.floor(
        ((newVal4 - newVal3) * actualFrameRate) / newVal1 / (newVal2 === 0 ? 1 : newVal2),
      );
    },
    { immediate: true },
  );
</script>
