<template>
  <div>
    <BasicModal
      v-bind="$attrs"
      @register="register"
      title="接口详细信息"
      :helpMessage="['提示1', '提示2']"
      width="700px"
    >
      <Card
        :tab-list="tabListTitle"
        v-bind="$attrs"
        :active-tab-key="activeKey"
        @tabChange="onTabChange"
      >
        <div v-if="activeKey === 'tab1'">
          <div class="flex flex-row" style="margin-bottom: 10px"
            ><img src="../../../../../assets/icons/titles.svg" /><span style="opacity: 0.8"
              >接口地址</span
            >
          </div>
          <a-typography-paragraph>
            <pre>{{ dataOfApi.apiDescription.value['url'] }}</pre>
          </a-typography-paragraph>
          <ApiInfoTable style="margin-top: 10px" :loading="loading" :type1="type1" :type2="type2" />
          <div class="flex flex-row" style="margin-bottom: 10px; margin-top: 20px"
            ><img src="../../../../../assets/icons/titles.svg" /><span style="opacity: 0.8"
              >返回值说明</span
            >
          </div>
          <a-typography-paragraph>
            <pre>{{ dataOfApi.resultInfo.value }}</pre>
          </a-typography-paragraph>
          <!--          <JsonPreview-->
          <!--            style="margin-left: 40px"-->
          <!--            :data="dataOfApi['apiDescription'].value['return']"-->
          <!--          />-->
        </div>

        <!--  json文件预览插件  -->
        <p v-if="activeKey === 'tab2'" style="width: 700px">
          <JsonPreview :data="dataOfApi['apiDescription'].value" />
        </p>
      </Card>
    </BasicModal>
  </div>
</template>
<script lang="ts">
  import { defineComponent, ref } from 'vue';
  import { BasicModal, useModalInner } from '/@/components/Modal';
  import { JsonPreview } from '/@/components/CodeEditor';
  import { Card } from 'ant-design-vue';
  import ApiInfoTable from './ApiInfoTable.vue';
  import { getDescription, getResultInfo } from './apiModalData';
  import { TypographyParagraph as ATypographyParagraph } from 'ant-design-vue';

  export default defineComponent({
    components: { BasicModal, JsonPreview, Card, ApiInfoTable, ATypographyParagraph },
    setup() {
      const loading = ref(true);
      setTimeout(() => {
        loading.value = false;
      }, 1500);

      const activeKey = ref('tab1');
      const tabListTitle = [
        {
          key: 'tab1',
          tab: '表格形式描述',
        },
        {
          key: 'tab2',
          tab: 'json格式描述',
        },
      ];

      function onTabChange(key) {
        activeKey.value = key;
      }
      let apiDescription = ref({});
      let resultInfo = ref('');
      let type1 = ref<Number>(1);
      let type2 = ref<Number>(1);
      const [register, { closeModal, setModalProps }] = useModalInner((dataStr: any) => {
        console.log(dataStr);
        var strs = dataStr.split('|');
        type1.value = parseInt(strs[0]);
        type2.value = parseInt(strs[1]);
        console.log('p1:' + type1.value + '  p2:' + type2.value);
        apiDescription.value = getDescription(type1.value, type2.value);
        // resultInfo.value = getResultInfo(type1, type2);
        //示例阶段，接口返回结果的文本信息只有一个，即参数为（1，1）对应接口的返回结果
        resultInfo.value = getResultInfo(1, 1);
      });
      return {
        dataOfApi: { apiDescription, resultInfo },
        tabListTitle,
        activeKey,
        type1,
        type2,
        loading,
        onTabChange,
        register,
        closeModal,
        setModalProps: () => {
          setModalProps({ title: 'Modal New Title' });
        },
      };
    },
  });
</script>
