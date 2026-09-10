<template>
  <!--  <el-popover :visible="state.visible" :width="350" placement="right" trigger="click">-->
  <!-- 搜索条件 -->
  <el-form
    ref="formRef"
    v-model="state.formModel"
    v-bind="attrs"
    :inline="true"
    label-position="left"
  >
    <el-form-item
      v-for="item in mergedFormItems"
      :key="item.prop"
      :prop="item.prop"
      style="margin-right: 0 !important; margin-bottom: 0 !important"
    >
      <template v-if="item.type === 'select'">
        <el-select
          v-model="state.formModel[item.prop]"
          v-bind="item.attrs"
          size="small"
          @change="handleChange"
        >
          <el-option
            v-for="option of item.options"
            :key="option.value"
            :value="option.value"
            :label="option.label"
            :disabled="option.disabled"
          />
        </el-select>
      </template>
      <template v-if="item.type === 'checkboxGroup'">
        <el-checkbox-group
          v-model="state.formModel[item.prop]"
          class="flex flex-row justify-center"
          @change="handleChange"
        >
          <el-checkbox
            v-for="option of item.options"
            :key="option.value"
            :label="option.value"
            :disabled="option.disabled"
            style="margin-left: 5px; margin-right: 5px !important"
            @change="(val) => handleCheckboxChange(val, item.prop, option.value)"
          >
            {{ option.label }}
          </el-checkbox>
        </el-checkbox-group>
      </template>
    </el-form-item>
  </el-form>

  <!-- 搜索按钮 -->
  <!--    <div class="ml-20">-->
  <!--      <el-button v-if="btnOptions.includes('search')" size="small" @click="handleOk">-->
  <!--        完成-->
  <!--      </el-button>-->
  <!--      <el-button v-if="btnOptions.includes('reset')" size="small" type="primary" @click="onReset">-->
  <!--        重置-->
  <!--      </el-button>-->
  <!--    </div>-->
  <!--    <template #reference>-->
  <!--      <Button size="small" @click="state.visible = true">筛选</Button>-->
  <!--    </template>-->
  <!--  </el-popover>-->
</template>

<script lang="ts">
  import { computed, reactive, ref } from 'vue';
  import { without, pick } from 'lodash-es';
  // import { Button } from 'ant-design-vue';

  const formRef = ref(null);

  const defaultFormAttrs = {}; // 默认表单属性

  export default {
    name: 'SearchBox',
    components: {
      // Button,
    },
    props: {
      formItems: {
        type: Array,
        default: () => [],
      },
      btnOptions: {
        type: Array,
        default: () => ['search'],
        // default: () => ['search', 'reset'],
      },
      handleFilter: {
        type: Function,
      },
      // 重置后的选项值
      initialValue: {
        type: Object,
        default: () => ({}),
      },
      // 初次进入的默认值
      defaultValue: {
        type: Object,
      },
      klass: {
        type: String,
      },
    },
    emits: ['change'],

    setup(props, ctx) {
      // 选中全部
      const checkAll = (formItem) => formItem.options.map((d) => d.value);

      // 生成数据模型（对多选 all 做特殊处理）
      const buildModel = (values) => {
        return props.formItems.reduce((acc, cur) => {
          const item = pick(values, [cur.prop]);
          const itemValue = item[cur.prop];
          // 多选单独处理
          if (cur.type === 'checkboxGroup') {
            if (typeof itemValue === 'undefined') {
              return acc;
            }
            if (itemValue && itemValue.length === 1 && itemValue[0] === 'all') {
              // 全选
              return { ...acc, ...{ [cur.prop]: checkAll(cur) } };
            }
            return { ...acc, ...item };
          }

          return { ...acc, ...item };
        }, {});
      };

      const state = reactive({
        visible: false,
        formModel: buildModel(props.defaultValue || props.initialValue),
      });

      // 合并表单默认属性和 $attrs
      const attrs = computed(() => {
        return { ...defaultFormAttrs, ...ctx.attrs };
      });

      // 表单项预处理
      const mergedFormItems = computed(() => {
        return props.formItems.map((item) => {
          return { ...item };
        });
      });

      // change
      const handleChange = (...params) => {
        ctx.emit('change', ...params);
      };

      // 搜索
      const handleOk = () => {
        state.visible = false;
        props.handleFilter(state.formModel);
      };
      // 重置
      const onReset = () => {
        state.formModel = { ...props.initialValue };
        handleOk();
      };

      // 空串''表示搜索条件为不限
      const handleCheckboxChange = (checked, prop, value) => {
        const formItem = props.formItems.find((d) => d.prop === prop) || {};
        if (checked) {
          if (value === 'all') {
            const values = checkAll(formItem);
            state.formModel[prop] = values;
          } else if (value === '') {
            // 选中不限，清理其他参数
            state.formModel[prop] = [''];
          } else {
            // 选中其他，移除不限
            state.formModel[prop] = without(state.formModel[prop], '');
          }
        } else if (value === 'all') {
          // 取消全部选择
          state.formModel[prop] = [''];
        }
        handleOk();
      };

      // 供外部调用更改选项值
      const changeOption = (option, val) => {
        state.formModel[option] = val;
      };

      return {
        formRef,
        state,
        attrs,
        mergedFormItems,
        handleCheckboxChange,
        handleOk,
        onReset,
        handleChange,
        changeOption,
      };
    },
  };
</script>
