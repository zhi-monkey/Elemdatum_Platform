<template>
  <a-typography-paragraph>
    <a-typography-title style="font-size: medium; margin-top: 15px">接口类型</a-typography-title>
    <a-typography-paragraph>
      <blockquote>{{ interfaceContent }}</blockquote>
    </a-typography-paragraph>
    <a-typography-title :level="2" style="font-size: medium; margin-top: 30px"
      >参数</a-typography-title
    >
    <a-typography-paragraph>
      <blockquote>{{ paramContent }}</blockquote>
      <blockquote>{{ paramExample }}</blockquote>
    </a-typography-paragraph>
    <a-typography-title style="font-size: medium; margin-top: 30px">结果说明</a-typography-title>
    <a-typography-paragraph>
      <blockquote>{{ resultContent }}</blockquote>
    </a-typography-paragraph>
  </a-typography-paragraph>
</template>
<script lang="ts">
  import { defineComponent } from 'vue';
  import {
    TypographyTitle as ATypographyTitle,
    TypographyParagraph as ATypographyParagraph,
  } from 'ant-design-vue';

  export default defineComponent({
    components: { ATypographyTitle, ATypographyParagraph },
    setup() {
      return {
        interfaceContent: `根据需要选择所查找的接口的类型`,
        paramContent: `对于每个接口类型，选择具体的参数类型可以获得目标接口的详细信息`,
        paramExample:
          '例如接口类型选择框选择‘批量查询数据集’，而参数选择框选择‘某用户创建的’，则查询到的接口是用于根据数据集创建者的id来批量查询数据集',
        resultContent: '查询结果有表格和json格式两种方式说明接口的相关参数及其返回值。',
      };
    },
  });
</script>
