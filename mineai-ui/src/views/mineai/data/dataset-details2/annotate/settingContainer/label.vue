<template>
  <ValidationObserver ref="observerRef">
    <el-form-item>
      <slot name="label">
        <LabelTip />
      </slot>
      <ValidationProvider v-slot="{ errors }" rules="required|validName" name="标签">
        <el-input
          v-model.trim="label"
          placeholder="输入标签按 Enter 确认"
          @keyup.enter="handleInput"
        />
        <p class="error-message">{{ errors[0] }}</p>
      </ValidationProvider>
      <div class="tr" style="margin-top: 6px">
        <el-button type="primary" @click="handleCancel">取消</el-button>
        <el-button type="primary" @click="handleInput">确定</el-button>
      </div>
    </el-form-item>
  </ValidationObserver>
</template>

<script>
  import { ref } from 'vue';
  import LabelTip from '/@/views/mineai/data/dataset-details2/annotate/settingContainer/labelTip.vue';

  export default {
    name: 'Label',
    components: {
      LabelTip,
    },
    setup(props, ctx) {
      const labelRef = ref('');
      const observerRef = ref(null);

      const handleCancel = () => {
        labelRef.value = '';
        observerRef.value.reset();
      };
      const handleInput = () => {
        observerRef.value.validate().then((success) => {
          if (!success) {
            return;
          }
          ctx.emit('handleInput', labelRef.value, handleCancel);
        });
      };
      return {
        handleInput,
        handleCancel,
        observerRef,
        label: labelRef,
      };
    },
  };
</script>
