<template>
  <el-popover
    v-model="state.visible"
    placement="bottom"
    width="200"
    trigger="click"
    title="修改标签名称"
  >
    <el-select v-model="state.value" placeholder="请选择" @change="handleEditLabel">
      <el-option v-for="item in labels" :key="item.id" :label="item.name" :value="item.id" />
    </el-select>
    <template #reference>
      <EditOutlined class="el-icon-edit" style="color: #108ee9; cursor: pointer" @click.stop />
    </template>
  </el-popover>
</template>
<script>
  import { reactive, watch } from 'vue';
  import { EditOutlined } from '@ant-design/icons-vue';

  export default {
    name: 'EditLabel',
    components: { EditOutlined },
    props: {
      row: {
        type: Object,
        default: () => ({}),
      },
      labels: {
        type: Array,
        default: () => [],
      },
      handleEditLabel: Function,
    },
    setup(props) {
      const { row } = props;
      const state = reactive({
        visible: false,
        value: row.data.categoryId,
      });

      watch(
        () => props.row,
        (next) => {
          Object.assign(state, {
            value: next?.data?.categoryId,
          });
        },
      );

      return {
        state,
      };
    },
  };
</script>
