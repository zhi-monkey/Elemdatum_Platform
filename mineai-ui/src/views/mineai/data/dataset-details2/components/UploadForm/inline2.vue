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
    emits: ['uploadStart', 'uploadSuccess', 'uploadError', 'fileChange', 'upload-progress'],
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

        // 如果传入了自定义的request函数，使用原来的逻辑（uploadByFile）
        if (request) {
          // 真正开始上传文件
          if (fileList.length > 0) {
            uploadByFile(fileList, (percent) => {
              // 传递进度给父组件
              ctx.emit('upload-progress', percent);
            });
          }
          // 清空文件列表
          formRef.value.reset();
          return;
        }

        // 如果没有传入request，使用uploadStore的方式（会更新进度到store）
        // 初始化上传任务（这会调用newMinIOUpload，会更新uploadStore的进度）
        // 注意：这里设置autoInsertDatabase为false，因为upload-datafile.vue会在uploadSuccess中自己处理存库
        uploadStore.initUploadState(
          props.params.datasetId,
          fileList,
          props.params.objectPath,
          props.params.type,
          false, // 不自动存库，让父组件处理
        );

        // 监听上传完成（通过轮询uploadStore状态）
        // 注意：由于设置了autoInsertDatabase为false，newMinIOUpload不会自动存库
        // 我们需要在上传到minio完成后就触发uploadSuccess，以便父组件可以开始轮询任务状态
        // 所以我们需要等待newMinIOUpload完成，然后触发uploadSuccess
        const checkUploadComplete = () => {
          const uploadState = uploadStore.uploadMap.get(props.params.datasetId);
          if (uploadState && uploadState.fileList.length === uploadState.size) {
            // 所有文件上传完成，构造返回结果
            const result = uploadState.fileList.map((file) => ({
              err: null,
              data: file.data,
            }));
            const outputPath = getFileOutputPath(
              fileList.map((f) => ({
                ...f,
                name: renameFile(f.name, { hash: props.hash, encode: props.encode }),
              })),
              props.params,
            );
            state.uploading = false;
            ctx.emit('uploadSuccess', result, outputPath);
          } else if (uploadState) {
            // 发送进度更新事件
            if (uploadState.size > 0) {
              const progress = (uploadState.fileList.length / uploadState.size) * 100;
              ctx.emit('upload-progress', progress);
            }
            // 继续等待
            setTimeout(checkUploadComplete, 500);
          } else {
            // 上传状态不存在，可能出错了
            state.uploading = false;
            ctx.emit('uploadError', new Error('上传失败：上传状态丢失'));
          }
        };

        // 开始检查上传完成
        state.uploading = true;
        ctx.emit('uploadStart', fileList);
        setTimeout(checkUploadComplete, 500);

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
