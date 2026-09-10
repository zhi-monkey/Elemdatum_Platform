<template>
  <BasicModal
    v-bind="$attrs"
    title="配置下发"
    :closable="true"
    width="400px"
    ok-text="确认"
    :centered="true"
    :body-style="{ 'max-height': '100px' }"
    @ok="handleConfirm"
    @register="registerConfigModal"
  >
    <div style="text-align: center; margin-top: 10px; font-size: 18px">
      配置信息已修改，请手动进行配置下发!
    </div>
  </BasicModal>
</template>
<script lang="ts">
  import { defineComponent } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';

  export default defineComponent({
    components: { BasicModal },
    props: {
      data: { type: Object },
    },
    emits: ['success', 'register'],
    setup(_, {}) {
      const [registerConfigModal, { closeModal }] = useModalInner(() => {});
      function handleConfirm() {
        closeModal();
      }
      return { registerConfigModal, handleConfirm };
    },
  });
</script>
