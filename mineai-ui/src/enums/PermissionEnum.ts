export enum PermissionEnum {
  // 数据集接口查询相关权限
  DATA = 'data',
  // 新增监控设备
  MONITOR_CREATE = 'monitor:create',
  // 配置下发
  MONITOR_CONFIG_PUBLISH = 'monitor:config:publish',
  // 绑定数据集
  MONITOR_BIND_DATASET = 'monitor:bind:dataset',
  // 查看监控设备绑定数据集
  MONITOR_VIEW_DATASET = 'monitor:view:dataset',
  // 查看监控设备所有信息
  MONITOR_VIEW_ALL = 'monitor:view:all',
  // 用户管理
  SYSTEM_USER = 'system:user',
  // 智能监控中心
  INTELLIGENT_MONITOR = 'intelligentMonitor',
  // 实时监控页面
  INTELLIGENT_MONITOR_VIEW = 'intelligentMonitor:view',
  // 切换实时监控视频流
  INTELLIGENT_MONITOR_VIEW_SWITCH = 'intelligentMonitor:view:switch',
  // 修改播放器配置
  INTELLIGENT_MONITOR_VIEW_SETTING = 'intelligentMonitor:view:setting',
  // 配置所有数据集
  DATA_MODIFY_DATASET = 'data:modifyDataset',
  // 发布标注任务
  DATA_PUBLISH_TASK = 'data:publishTask',
  // 接收标注任务
  DATA_ACCEPT_TASK = 'data:acceptTask',
}
