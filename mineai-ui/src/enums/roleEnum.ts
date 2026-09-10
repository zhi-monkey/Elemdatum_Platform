export enum RoleEnum {
  // super admin
  // @ts-ignore
  SUPER = 'super',

  // tester
  TEST = 'test',

  // SystemManger = 'system:manager',
  // SystemInfo = 'system:info',
  // SystemUser = 'system:user',
  // SystemRole = 'system:role',
  // SystemMenu = 'system:menu',
  // SystemDepartment = 'system:department',
  // SystemIndividual = 'system:individual',
  //
  // ControllerManager = 'controller:manager',
  // ControllerInfo = 'controller:controllerInfo',
  // ControllerList = 'controller:controllerList',
  // ControllerMineList = 'controller:mineList',
  // ControllerSceneList = 'controller:sceneList',
  //
  // DataManager = 'data:manager',
  // DataInfo = 'data:info',
  // DatasetDetails = 'data:datasetDetails',
  // DatasetExplorer = 'data:DataDatasetExplorer',
  // AudioAlert = 'data:audioAlert',
  // DataUpload = 'data:upload',
  // DataApi = 'data:dataApi',
  // DataPreprocess = 'data:dataPreprocess',
  // DataLabeling = 'data:labeling',
  //
  // MonitorManager = 'monitor:manager',
  // MonitorHome = 'monitor:monitorHome',
  // MonitorList = 'monitor:monitorList',
  // MonitorInfoBtn = 'monitor:monitorInfoBtn',
  // MonitorInfo = 'monitor:monitorInfo',
  // MonitorModelList = 'monitor:modelList',
  // ModelUpload = 'model:upload',
  // JobDetails = 'job:details',
  // ElectricFence = 'monitor:electricFence',
  // WarningHistory = 'monitor:warningHistory',
  //
  // WorkerManager = 'worker:manager',
  // WorkerIndex = 'worker:index',
  //
  // BindManager = 'bind:manager',
  // AlgorithmManager = 'algorithm:manager',
  // IntelligentManager = 'intelligent:manager',
  // UserManager = 'user:manager',

  System = 'system',
  SystemUser = 'system:user',
  SystemUserCreate = 'system:user:create',
  SystemUserEdit = 'system:user:edit',
  SystemUserDelete = 'system:user:delete',
  SystemUserGroup = 'system:userGroup',
  SystemUserGroupCreate = 'system:userGroup:create',
  SystemUserGroupEdit = 'system:userGroup:edit',
  SystemUserGroupDelete = 'system:userGroup:delete',
  SystemUserGroupEditUser = 'system:userGroup:editUser',
  SystemUserGroupEditUserRole = 'system:userGroup:editUserRole',
  SystemUserGroupEditUserState = 'system:userGroup:editUserState',
  SystemUserGroupDeleteUser = 'system:userGroup:deleteUser',
  SystemAuthCode = 'system:authCode',
  SystemAuthCodeCreate = 'system:authCode:create',
  SystemAuthCodeEdit = 'system:authCode:edit',
  SystemAuthCodeDelete = 'system:authCode:delete',
  SystemPermissionCreate = 'system:permission:create',
  SystemPermissionEdit = 'system:permission:edit',
  SystemPermissionDelete = 'system:permission:delete',
  SystemRole = 'system:role',
  SystemRoleCreate = 'system:role:create',
  SystemRoleEdit = 'system:role:edit',
  SystemRoleDelete = 'system:role:delete',
  SystemRoleAuth = 'system:role:auth',
  SystemRoleMenu = 'system:role:menu',
  SystemMenu = 'system:menu',
  SystemMenuCreate = 'system:menu:create',
  SystemMenuEdit = 'system:menu:edit',
  SystemMenuDelete = 'system:menu:delete',

  Data = 'data',

  Monitor = 'monitor',
  MonitorCreate = 'monitor:create',
  MonitorConfigPublish = 'monitor:config:publish',
  MonitorBindDataset = 'monitor:bind:dataset',
  MonitorViewAll = 'monitor:view:all',
  MonitorViewDataset = 'monitor:view:dataset',

  IntelligentMonitor = 'intelligentMonitor',
  IntelligentMonitorView = 'intelligentMonitor:view',
  IntelligentMonitorViewSwitch = 'intelligentMonitor:view:switch',
  IntelligentMonitorViewSetting = 'intelligentMonitor:view:setting',

  //______________________下面是新的一些permission
  //1系统总览
  MaSystem = 'maSystem',

  //    1.1系统总览
  SystemInfo = 'maSystem:SystemInfo',
  // SystemInfo_Read = 'maSystem:SystemInfo:Read',
  SystemInfo_Write = 'maSystem:SystemInfo:Write',

  //2算法应用商城
  MaAlgorithmMall = 'maAlgorithmMall',

  //    2.1算法应用商城
  AlgorithmMall = 'maAlgorithmMall:AlgorithmMall',
  // AlgorithmMall_Read = 'maAlgorithmMall:AlgorithmMall:Read',
  AlgorithmMall_Write = 'maAlgorithmMall:AlgorithmMall:Write',

  //    2.2算法应用详情
  ModelApplicationDetail = 'maAlgorithmMall:ModelApplicationDetail',
  // ModelApplicationDetail_Read = 'maAlgorithmMall:ModelApplicationDetail:Read',
  ModelApplicationDetail_Write = 'maAlgorithmMall:ModelApplicationDetail:Write',

  //3应用中心
  MaApplication = 'maApplication',

  // 3.1 基础算法库
  BasicAlgorithm = 'maApplication:basicAlgorithm',
  // BasicAlgorithm_Read = 'maApplication:basicAlgorithm:Read',
  BasicAlgorithm_Write = 'maApplication:basicAlgorithm:Write',

  //3.2 信息管理
  Message = 'maApplication:message',
  //Message_Read = 'maApplication:message:Read',
  Message_Write = 'maApplication:message:Write',

  //3.3 应用任务管理
  TaskManagement = 'maApplication:taskManagement',
  // TaskManagement_Read = 'maApplication:taskManagement:Read',
  TaskManagement_Write = 'maApplication:taskManagement:Write',
  //4训练中心
  //5数据管理
  MaData = 'maData',
  // 5.1 数据集管理
  DatasetDetails = 'maData:datasetDetails',
  // DatasetDetails_Read = 'maData:datasetDetails:Read',
  DatasetDetails_Write = 'maData:datasetDetails:Write',
  //6用户中心
  //7场景中心
}
