<template>
  <PageWrapper @back="goBack" style="margin: 0 16px 0 16px">
    <template #title>应用算法列表</template>

    <a-table :columns="columns" :data-source="data" class="components-table-demo-nested">
      <template #expandedRowRender="{ record }">
        <a-table :columns="innerColumns" :data-source="record.algorithms" :pagination="false">
          <template #isDefault="{ text }">
            {{ text ? '是' : '否' }}
          </template>
          <template #deviceFirmware="{ text }">
            {{ text.map((df) => `${df.device}-${df.firmware}`).join(', ') }}
          </template>
          <template #operation="{ record: innerRecord }">
            <a v-if="!innerRecord.isDefault">设为默认算法</a>
          </template>
        </a-table>
      </template>
    </a-table>
  </PageWrapper>
</template>

<script setup lang="ts">
  import { Table as ATable } from 'ant-design-vue';
  import { PageWrapper } from '/@/components/Page';
  import { router } from '/@/router';

  const columns = [
    { title: '软件平台', dataIndex: 'softwarePlatform', key: 'softwarePlatform' },
    { title: '硬件平台', dataIndex: 'hardwarePlatform', key: 'hardwarePlatform' },
  ];

  interface DataItem {
    key: number;
    softwarePlatform: string;
    hardwarePlatform: string;
    algorithms: AlgorithmItem[];
  }

  interface DeviceFirmware {
    device: string;
    firmware: string;
  }

  interface AlgorithmItem {
    key: number;
    name: string;
    deviceFirmware: DeviceFirmware[];
    isDefault: boolean;
  }

  const data: DataItem[] = [
    {
      key: 1,
      softwarePlatform: 'Pytorch',
      hardwarePlatform: 'GPU',
      algorithms: [
        {
          key: 1,
          name: 'YOLOv5',
          deviceFirmware: [
            { device: 'KBA12C', firmware: 'v1.0' },
            { device: 'KBA13D', firmware: 'v2.1' },
          ],
          isDefault: true,
        },
        {
          key: 2,
          name: 'Faster R-CNN',
          deviceFirmware: [{ device: 'KBA12C', firmware: 'v1.1' }],
          isDefault: false,
        },
      ],
    },
    {
      key: 2,
      softwarePlatform: 'RKNN',
      hardwarePlatform: 'RK3588',
      algorithms: [
        {
          key: 1,
          name: 'YOLOv4-tiny',
          deviceFirmware: [{ device: 'KBA14E', firmware: 'v1.0' }],
          isDefault: true,
        },
        {
          key: 2,
          name: 'SSD',
          deviceFirmware: [
            { device: 'KBA14E', firmware: 'v1.1' },
            { device: 'KBA15F', firmware: 'v1.0' },
          ],
          isDefault: false,
        },
      ],
    },
    {
      key: 3,
      softwarePlatform: 'TensorRT',
      hardwarePlatform: 'NVIDIA Jetson AGX Xavier',
      algorithms: [
        {
          key: 1,
          name: 'YOLOv3',
          deviceFirmware: [{ device: 'Xavier', firmware: 'JetPack 4.6' }],
          isDefault: true,
        },
        {
          key: 2,
          name: 'RetinaNet',
          deviceFirmware: [{ device: 'Xavier', firmware: 'JetPack 4.5.1' }],
          isDefault: false,
        },
      ],
    },
    {
      key: 4,
      softwarePlatform: 'TensorRT',
      hardwarePlatform: 'NVIDIA Jetson Nano',
      algorithms: [
        {
          key: 1,
          name: 'MobileNet-SSD',
          deviceFirmware: [{ device: 'Nano', firmware: 'JetPack 4.6' }],
          isDefault: true,
        },
        {
          key: 2,
          name: 'YOLOv5s',
          deviceFirmware: [{ device: 'Nano', firmware: 'JetPack 4.5' }],
          isDefault: false,
        },
      ],
    },
    {
      key: 5,
      softwarePlatform: 'AIPU',
      hardwarePlatform: '算能 SN1000',
      algorithms: [
        {
          key: 1,
          name: 'YOLOv5n',
          deviceFirmware: [{ device: 'SN1000', firmware: 'v2.3.0' }],
          isDefault: true,
        },
        {
          key: 2,
          name: 'EfficientDet-Lite',
          deviceFirmware: [{ device: 'SN1000', firmware: 'v2.2.1' }],
          isDefault: false,
        },
      ],
    },
    {
      key: 6,
      softwarePlatform: 'AIPU',
      hardwarePlatform: '算能 SN200',
      algorithms: [
        {
          key: 1,
          name: 'YOLOv8',
          deviceFirmware: [{ device: 'SN200', firmware: 'v1.5.0' }],
          isDefault: true,
        },
        {
          key: 2,
          name: 'YOLOX',
          deviceFirmware: [{ device: 'SN200', firmware: 'v1.4.2' }],
          isDefault: false,
        },
      ],
    },
  ];

  const innerColumns = [
    { title: '算法名称', dataIndex: 'name', key: 'name' },
    {
      title: '设备固件信息',
      dataIndex: 'deviceFirmware',
      key: 'deviceFirmware',
      slots: { customRender: 'deviceFirmware' },
    },
    {
      title: '是否默认',
      dataIndex: 'isDefault',
      key: 'isDefault',
      slots: { customRender: 'isDefault' },
    },
    {
      title: '操作',
      key: 'operation',
      slots: { customRender: 'operation' },
    },
  ];

  function goBack() {
    router.go(-1);
  }
</script>
