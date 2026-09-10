export enum MsResultEnum {
  SUCCEED = 'MS_20000',
  TIMEOUT = 'MS_40000',
  DUBHE_SUCCEED = '200',
}

export enum MaBackendUrlEnum {
  SYSTEM = 'ss/',
  DATA_MANAGER = 'dm/',
  CONTROLLER_MANAGER = 'cm/',
  MODEL_MANAGER = 'mm/',
  WORKER_MANAGER = 'wm/',
  MONITOR_ACCESSOR = 'ma/',
  PACKAGE_MANAGER = 'pm/',
}

export enum DubheBackendUrlEnum {
  DUBHE_ADMIN = 'api/v1/admin/',
  DUBHE_AUTH = 'api/v1/auth/',
  DUBHE_DATASET = 'api/v1/data/',
}
