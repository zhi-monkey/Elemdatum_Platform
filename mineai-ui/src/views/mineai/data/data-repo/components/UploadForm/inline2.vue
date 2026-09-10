<template>
  <Form ref="formRef" v-bind="$attrs" @file-change="handleFileChange" />
</template>
<script>
  import { reactive, ref } from 'vue';
  import Form from './form.vue';
  import { getFileOutputPath, minIOUpload, renameFile } from './util';
  import { useUploadStore } from '/@/store/modules/upload';

  export default {
    name: 'UploadInline',
    components: {
      Form,
    },
    inheritAttrs: false,
    props: {
      request: Function,
      autoUpload: {
        type: Boolean,
        default: false,
      },
      beforeUpload: Function,
      transformFile: Function,
      hash: {
        type: Boolean,
        default: false,
      },
      encode: {
        type: Boolean,
        default: true,
      },
      params: {
        type: Object,
        default: () => ({}),
      },
    },
    emits: ['uploadStart', 'uploadSuccess', 'uploadError', 'fileChange'],
    setup(props, ctx) {
      // eslint-disable-next-line vue/no-setup-props-destructure
      const { request, transformFile, beforeUpload, params } = props;
      const formRef = ref(null);
      const state = reactive({
        uploading: false,
      });
      const uploadStore = useUploadStore();

      // 基于文件上传
      const uploadByFile = (files, callback, result = {}, errCallback) => {
        // Form组件内的文件列表
        const fileList = Array.isArray(files) ? files : [files];
        // 重命名,给图片加上hash
        const renameFileList = fileList.map((file) => ({
          ...file,
          name: renameFile(file.name, { hash: props.hash, encode: props.encode }),
        }));
        // 非空判断
        if (!fileList || !fileList.length) {
          throw new Error('文件不能为空');
        }

        state.uploading = true;
        ctx.emit('uploadStart', files);
        // request没传，用的默认的minIOUpload
        const uploadReqeust = request || minIOUpload;
        // 开始调用上传接口
        return (
          uploadReqeust(
            { ...props.params, fileList: renameFileList, transformFile, ...result },
            callback,
            errCallback,
          )
            // 上传成功，触发父组件uploadSuccess
            .then((res) => {
              const outputPath = getFileOutputPath(renameFileList, props.params);
              state.uploading = false;
              // outputPath没用上？
              ctx.emit('uploadSuccess', res, outputPath);
            })
            // 上传失败，触发父组件uploadError
            .catch((err) => {
              state.uploading = false;
              ctx.emit('uploadError', err);
            })
        );
      };

      // 提交结果 （目前是直接调用uploadByFile,beforeUpload没用上）
      const uploadSubmit = () => {
        // 文件列表
        const fileList = formRef.value?.fileList || [];
        // 初始化上传任务
        uploadStore.initUploadState(
          props.params.datasetId,
          fileList,
          props.params.objectPath,
          props.params.type,
        );
        // 清空文件列表
        formRef.value.reset();
      };

      // 父组件没用上fileChange，autoUpload没传
      const handleFileChange = (file, fileList) => {
        ctx.emit('fileChange', file, fileList);
        // 自动触发上传命令
        if (props.autoUpload) {
          uploadByFile(file);
        }
      };

      return {
        state,
        formRef,
        uploadSubmit,
        uploadByFile,
        handleFileChange,
      };
    },
  };
</script>
