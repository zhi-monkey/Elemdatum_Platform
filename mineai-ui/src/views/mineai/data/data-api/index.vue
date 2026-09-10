<template>
  <div>
    <PageWrapper title="开放接口信息查询" contentFullHeight style="margin: 0 16px 0 16px">
      <CollapseContainer title="选择接口相关信息" style="margin-top: 10px">
        <BasicForm
          autoFocusFirstItem
          :labelWidth="200"
          :schemas="schemas"
          :actionColOptions="{ span: 24 }"
          @submit="handleSubmit"
          @reset="handleReset"
        />
      </CollapseContainer>
      <IntroCard />
    </PageWrapper>
    <DataApiModal @register="register" />
  </div>
</template>

<script lang="ts">
  import { computed, defineComponent, unref, ref } from 'vue';
  import { BasicForm, FormSchema } from '/@/components/Form/index';
  import { CollapseContainer } from '/@/components/Container';
  import { useMessage } from '/@/hooks/web/useMessage';
  import { PageWrapper } from '/@/components/Page';
  import { optionsListApi } from '/@/api/demo/select';
  import { useDebounceFn } from '@vueuse/core';
  import { useModal } from '/@/components/Modal';
  import DataApiModal from './components/DataApiModal.vue';
  import { interfaceOptions, paramOptionsData } from './components/optionData';
  import IntroCard from './components/IntroCard.vue';

  const schemas: FormSchema[] = [
    {
      //字段名
      field: 'interfaceType',
      //组件类型
      component: 'Select',
      //标签名
      label: '接口类型',
      colProps: {
        span: 8,
      },
      componentProps: ({ formModel, formActionType }) => {
        return {
          options: interfaceOptions,
          placeholder: '接口查询',
          onChange: (e: any) => {
            console.log(e);
            //字段联动查询，根据下拉框中接口类型的选择来提供参数类型的选择框
            let paramOptions =
              e == 1
                ? paramOptionsData[interfaceOptions[0].id]
                : e == 2
                ? paramOptionsData[interfaceOptions[1].id]
                : e == 3
                ? paramOptionsData[interfaceOptions[2].id]
                : paramOptionsData[interfaceOptions[3].id];

            // console.log(citiesOptions)
            if (e === undefined) {
              paramOptions = [];
            }
            formModel.paramType = undefined; //  reset paramType value
            const { updateSchema } = formActionType;
            updateSchema({
              field: 'paramType',
              componentProps: {
                options: paramOptions,
              },
            });
          },
        };
      },
    },
    {
      field: 'paramType',
      component: 'Select',
      label: '参数',
      colProps: {
        span: 8,
      },
      componentProps: {
        options: [], // default []
        placeholder: '参数查询',
      },
    },
  ];

  export default defineComponent({
    name: 'DataDataApi',
    components: { BasicForm, CollapseContainer, PageWrapper, DataApiModal, IntroCard },
    setup() {
      const check = ref(null);
      const keyword = ref<string>('');
      const [register, { openModal, setModalProps }] = useModal();
      const searchParams = computed<Recordable>(() => {
        return { keyword: unref(keyword) };
      });

      function onSearch(value: string) {
        keyword.value = value;
        console.log('onSearch value:' + value);
      }
      return {
        schemas,
        optionsListApi,
        onSearch: useDebounceFn(onSearch, 300),
        searchParams,
        register,
        openModal,
        handleReset: () => {
          keyword.value = '';
        },
        handleSubmit: (values: any) => {
          const str = JSON.stringify(values);
          // console.log(str.split('"'));
          var strings = str.split('"');
          if (strings.length < 9) {
            console.log('第二个选型没选');
          } else {
            const iType = strings[3];
            const pType = strings[7];
            const dataStr = iType + '|' + pType;
            console.log(dataStr);
            openModal(true, dataStr);
            setModalProps({ showOkBtn: false, cancelText: '关闭' });
          }
        },
        check,
      };
    },
  });
</script>
