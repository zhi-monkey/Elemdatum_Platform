<template>
  <!--    <slot name="label">-->
  <!--      <LabelTip />-->
  <!--    </slot>-->
  <!--    <div class="flex flex-between">-->
  <!--      <InfoSelect-->
  <!--        :innerRef="innerRef"-->
  <!--        style="width: 80%"-->
  <!--        placeholder="选择系统预置标签"-->
  <!--        :dataSource="dataSource"-->
  <!--        value-key="value"-->
  <!--        default-first-option-->
  <!--        filterable-->
  <!--        @change="handleLabelSelect"-->
  <!--      />-->
  <!--    </div>-->
  <LabelEditor title="新建标签" @handle-ok="handleLabelCreate" />
</template>
<script>
  import { ref } from 'vue';
  // import InfoSelect from '/@/views/mineai/data/dataset-details2/components/InfoSelect/index.jsx';
  import LabelEditor from '/@/views/mineai/data/dataset-details2/components/labelEditor/index.vue';
  // import LabelTip from '/@/views/mineai/data/dataset-details2/annotate/settingContainer/labelTip.vue';

  export default {
    name: 'AddLabel',
    components: {
      // LabelTip,
      // InfoSelect,
      LabelEditor,
    },
    props: {
      dataSource: {
        type: Array,
        default: () => [],
      },
      handleLabelSelect: Function,
      handleLabelCreate: Function,
    },
    setup() {
      const selectRef = ref(null);

      return {
        innerRef: () => selectRef,
      };
    },
  };
</script>
