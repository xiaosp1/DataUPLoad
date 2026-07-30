// =============================================================================
// W-FRONT-02-D i18n（替换旧的 index.js stub）
// 三语：zh-CN / en-US / id-ID
// 菜单、面包屑、通用按钮、报警、字段名全覆盖（200+ keys）
// 所有中文文本 UTF-8 无 BOM（直接 write，不经 PowerShell Out-File）
// =============================================================================

import { createI18n } from 'vue-i18n'

export type AppLocale = 'zh-CN' | 'en-US' | 'id-ID'

export const SUPPORTED_LOCALES: AppLocale[] = ['zh-CN', 'en-US', 'id-ID']

export const LOCALE_LABELS: Record<AppLocale, string> = {
  'zh-CN': '简体中文',
  'en-US': 'English',
  'id-ID': 'Bahasa Indonesia'
}

// ---------------------------------------------------------------------------
// 中文 zh-CN
// ---------------------------------------------------------------------------
const zhCN = {
  app: {
    name: 'DataupLoad',
    slogan: '实时数据采集 · 缺陷检测 · 报警管理',
    loading: '加载中…',
    welcome: '欢迎'
  },

  // 顶部栏
  topbar: {
    home: '首页',
    logout: '退出登录',
    logoutConfirm: '确定要退出登录吗？',
    profile: '个人中心',
    language: '语言',
    refresh: '刷新',
    fullscreen: '全屏',
    exitFullscreen: '退出全屏'
  },

  // 面包屑
  breadcrumb: {
    home: '首页',
    realtime: '实时数据',
    alarm: '报警管理',
    defect: '缺陷处理',
    account: '账号管理',
    systemConfig: '系统配置',
    log: '操作日志',
    userManage: '用户管理',
    screen: '大屏模式',
    forbidden: '禁止访问'
  },

  // 侧边栏菜单分组 + 菜单项
  menu: {
    groupMonitor: '实时监控',
    groupSystem: '系统管理',

    realtime: '实时数据',
    alarm: '报警管理',
    defect: '缺陷处理',

    account: '账号管理',
    systemConfig: '系统配置',
    log: '操作日志',
    userManage: '用户管理',
    screen: '大屏模式'
  },

  // 通用按钮 / 操作
  common: {
    confirm: '确定',
    cancel: '取消',
    save: '保存',
    edit: '编辑',
    delete: '删除',
    add: '新增',
    search: '搜索',
    reset: '重置',
    refresh: '刷新',
    export: '导出',
    import: '导入',
    submit: '提交',
    back: '返回',
    next: '下一步',
    prev: '上一步',
    close: '关闭',
    more: '更多',
    yes: '是',
    no: '否',
    all: '全部',
    success: '操作成功',
    failed: '操作失败',
    comingSoon: '即将上线',
    pending: '待处理',
    processing: '处理中',
    done: '已完成',
    loading: '加载中…',
    noData: '暂无数据',
    updateTime: '更新时间',
    action: '操作'
  },

  // 登录页
  login: {
    title: 'DataupLoad',
    subtitle: '实时数据采集 · 缺陷检测 · 报警管理',
    username: '用户名',
    password: '密码',
    remember: '记住我',
    submit: '登录',
    submitting: '登录中…',
    usernamePlaceholder: '请输入用户名',
    passwordPlaceholder: '请输入密码',
    errorRequired: '请填写用户名和密码',
    errorCredential: '用户名或密码错误',
    errorNetwork: '网络异常，请稍后再试',
    copyright: '© 2026 DataupLoad',
    tip: '默认账号：super_admin / Abc12345'
  },

  // 403 / 404 / 500
  forbidden: {
    title: '禁止访问',
    subtitle: '您没有权限访问该页面',
    description: '请联系管理员申请相应权限，或返回首页。 肖升平 16653658509',
    backHome: '返回首页',
    contactAdmin: '联系管理员'
  },
  notFound: {
    title: '页面走丢了',
    subtitle: '404 — 你访问的页面不存在',
    backHome: '返回首页'
  },
  serverError: {
    title: '服务器异常',
    subtitle: '500 — 系统繁忙，请稍后再试',
    retry: '重试'
  },

  // 实时数据
  realtime: {
    title: '实时数据',
    subtitle: '实时查看采集点数据流、设备状态与生产节拍',
    online: '在线',
    offline: '离线',
    deviceCount: '设备总数',
    activeAlarms: '活动告警',
    todayOutput: '今日产量',
    throughput: '吞吐量',
    lastUpdate: '最近更新',
    chart: '实时趋势',
    table: '采集点列表',
    placeholder: '🚧 业务对齐期（E 子单实现）',
    // ===== W-FRONT-02-E1 新增 KPI / 图表 / 表格 字段 =====
    kpi: {
      onlineLines: '在线线别',
      todayOutput: '今日产量',
      todayDefect: '今日缺陷',
      todayAlarm: '今日报警',
      onlineLinesHint: '当前活跃产线 / 面',
      fromLines: '聚合自 {n} 条产线',
      defectRate: '缺陷率 {rate}',
      alarmHint: '今日新增告警',
      // ===== W-RT-4: PSM 多出来的 KPI 字段（11 个 key） =====
      productTotal: '生产总数',
      efficiency: '实时效率',
      efficiencyUnit: '个/分',
      efficiencyHint: '当前产线节拍',
      occupancy: '上座数量',
      occupancyRate: '上座率',
      occupancyRateHint: '上座率 {rate}',
      occupancyHint: '上座数 {n}',
      failCount: '次品数量',
      failRate: '次品率',
      failRateHint: '次品率 {rate}',
      failCountHint: '次品数 {n}',
      successCount: '良品数量',
      successRateHint: '良品率 {rate}',
      removeTotal: '剔除总数',
      removeFailNum: '剔除失败数',
      removeFailRate: '剔除失败率',
      removeFailRateHint: '剔除失败率 {rate}',
      removeFailHint: '剔除 {num} 颗 / 失败率 {rate}',
      deviceOpenTime: '开机时间',
      deviceOpenTimeHint: '原始数据：{raw}',
      selectedLine: '已选：{line}'
    },
    chartTitle: '实时趋势',
    chart: {
      title: '实时趋势',
      plan: '计划',
      actual: '实际',
      defect: '缺陷',
      allLines: '全部线别',
      allLinesSub: '展示全部线别近 2 小时趋势',
      selectedSub: '已选：{lines}'
    },
    tableTitle: '线别状态',
    table: {
      title: '线别状态',
      total: '共',
      line: '线别',
      status: '状态',
      output: '产量',
      defect: '缺陷',
      progress: '进度',
      stateRunning: '运行',
      stateIdle: '停机',
      stateDown: '故障'
    },
    // ===== W-RT-2 左栏线别列表卡片 =====
    lineList: {
      title: '产线列表',
      total: '共',
      defect: '缺陷',
      remove: '剔除',
      // ===== W-RT-7: 拖拽排序新增键 =====
      dragHint: '拖拽调整顺序',
      reorderSuccess: '排序已保存',
      reorderFail: '排序失败: {msg}'
    },
    // ===== W-RT-5: 实时页新功能键 =====
    // realtime.ws.*  (W-RT-1 / W-PERF-B 用)
    ws: {
      connected: 'WS 已连接',
      connecting: '连接中...',
      disconnected: 'WS 已断开 (重连中)'
    },
    // realtime.detail.*  (W-RT-3 用 — 14 keys)
    detail: {
      production: '生产信息',
      defect: '缺陷网格',
      device: '设备状态',
      time: '时间信息',
      total: '总数',
      good: '良品',
      bad: '次品',
      efficiency: '效率',
      camera: '摄像机',
      encoder: '编码器',
      plc: 'PLC',
      online: '在线',
      offline: '离线',
      runtime: '运行时长'
    },
    error: {
      network: '网络异常，请稍后重试',
      loadLineFailed: '线别数据加载失败'
    },
    metric: {
      temperature: '温度',
      pressure: '压力',
      humidity: '湿度',
      speed: '转速',
      voltage: '电压'
    }
  },

  // 报警管理
  alarm: {
    title: '报警管理',
    subtitle: '查看、处理与归档历史告警事件',
    newAlarm: '新报警',
    acknowledged: '已确认',
    resolved: '已处理',
    ignored: '已忽略',
    level: '等级',
    levelCritical: '严重',
    levelMajor: '主要',
    levelMinor: '次要',
    levelWarning: '提示',
    device: '设备',
    time: '发生时间',
    message: '报警内容',
    action: '操作',
    acknowledge: '确认',
    resolve: '处理',
    ignore: '忽略',
    detail: '详情',
    placeholder: '🚧 业务对齐期（E 子单实现）',
    // ----- W-FRONT-02-E2 新增 -----
    filter: {
      timeRange: '时间范围',
      range1h: '近 1 小时',
      range24h: '近 24 小时',
      range7d: '近 7 天',
      rangeCustom: '自定义',
      line: '线别',
      type: '类型',
      status: '状态',
      allLine: '全部线别',
      allType: '全部类型',
      allStatus: '全部状态',
      reset: '重置',
      query: '查询',
      customRange: '自定义时段'
    },
    typeOption: {
      defect: '缺陷报警',
      system: '系统报警',
      device: '设备报警'
    },
    table: {
      triggerTime: '触发时间',
      line: '线别',
      face: '工位',
      type: '类型',
      level: '等级',
      desc: '描述',
      status: '状态',
      action: '操作',
      detail: '详情',
      ignore: '忽略',
      index: '#'
    },
    levelOption: {
      normal: '普通',
      serious: '严重'
    },
    status: {
      pending: '未处理',
      handled: '已处理',
      ignored: '已忽略'
    },
    detail: {
      title: '报警详情',
      id: '报警 ID',
      uuid: 'UUID',
      triggerTime: '触发时间',
      type: '类型',
      level: '等级',
      line: '线别',
      face: '工位',
      duration: '持续时长',
      desc: '完整描述',
      defect: '缺陷名称',
      image: '关联图像',
      noImage: '暂无图像',
      ignore: '忽略',
      handle: '处理',
      close: '关闭'
    },
    ws: {
      connected: '实时连接已建立',
      disconnected: '实时连接已断开',
      connecting: '正在连接实时通道…',
      reconnecting: '连接断开，正在重连…'
    },
    list: {
      refresh: '刷新列表',
      empty: '暂无报警记录',
      loadFailed: '报警列表加载失败',
      ignoreSuccess: '已忽略',
      ignoreFailed: '忽略失败',
      ignoreConfirm: '确定要忽略该报警吗？',
      multiIgnoreConfirm: '确定要忽略全部未处理报警吗？'
    },
    lineTree: {
      loadFailed: '线别数据加载失败'
    },
    sort: {
      asc: '按时间升序',
      desc: '按时间降序'
    },
    // ===== W-RT-8 报警徽章悬浮窗 =====
    hint: {
      title: '未处理报警',
      empty: '无未处理报警',
      viewAll: '查看全部',
      recent: '最近',
      dragHint: '可拖动',
      clickHint: '点击查看详情'
    },
    // ===== W-DEFECT-CFG 子单 C：报警页子 tab =====
    tab: {
      records: '报警记录',
      defectConfig: '缺陷配置'
    }
  },

  // ===== W-DEFECT-CFG 子单 C：缺陷配置子页面 i18n =====
  defectConfig: {
    search: {
      name: '缺陷名称',
      namePlaceholder: '请输入缺陷名称（模糊）',
      category: '缺陷类型',
      categoryPlaceholder: '全部类型'
    },
    category: {
      defect: '缺陷报警',
      system: '系统报警',
      device: '设备报警'
    },
    table: {
      index: '#',
      name: '缺陷名称',
      type: '缺陷类型',
      alarmEnable: '推送大屏',
      soundEnable: '声音报警',
      sendYkEnable: '推送英科',
      createTime: '创建时间',
      action: '操作'
    },
    action: {
      add: '新增缺陷',
      edit: '编辑缺陷'
    },
    form: {
      name: '缺陷名称',
      namePlaceholder: '请输入缺陷名称（1-20 字符）',
      nameRequired: '缺陷名称不能为空',
      nameLength: '缺陷名称长度需在 1-20 字符',
      category: '缺陷类型',
      categoryPlaceholder: '请选择缺陷类型',
      categoryRequired: '请选择缺陷类型',
      alarmEnable: '推送大屏',
      alarmEnableRequired: '请选择是否推送大屏',
      soundEnable: '声音报警',
      soundEnableRequired: '请选择是否开启声音',
      soundEnableHint: '仅当推送大屏开启时有效',
      sendYkEnable: '推送英科',
      sendYkEnableRequired: '请选择是否推送英科',
      sendYkEnableHint: '开启后将通过英科推送到 MES'
    },
    confirm: {
      title: '确认',
      delete: '确定要删除该缺陷吗？'
    },
    apiMsg: {
      addSuccess: '新增成功',
      addFailed: '新增失败',
      addError: '新增异常',
      editSuccess: '编辑成功',
      editFailed: '编辑失败',
      editError: '编辑异常',
      deleteSuccess: '删除成功',
      deleteFailed: '删除失败',
      deleteError: '删除异常'
    },
    list: {
      loadFailed: '缺陷列表加载失败',
      empty: '暂无缺陷配置'
    }
  },

  // 缺陷处理（W-FRONT-02-E3）
  defect: {
    title: '缺陷处理',
    subtitle: '缺陷分类、复核与闭环跟踪',
    newDefect: '新建缺陷',
    pendingReview: '待复核',
    reviewing: '复核中',
    closed: '已闭环',
    type: '缺陷类型',
    severity: '严重度',
    image: '缺陷图像',
    location: '位置',
    createTime: '创建时间',
    operator: '处理人',
    review: '复核',
    close: '关闭',
    placeholder: '🚧 业务对齐期（E 子单实现）',
    // ===== W-FRONT-02-E3 新增 =====
    kpi: {
      total: '当日总缺陷',
      severe: '严重缺陷',
      handled: '已处理',
      missRate: '漏检率'
    },
    filter: {
      date: '日期',
      line: '线别',
      type: '缺陷类型',
      level: '严重度',
      query: '查询',
      reset: '重置'
    },
    level: {
      severe: '严重',
      normal: '一般'
    },
    status: {
      pending: '待处理',
      handled: '已处理'
    },
    table: {
      id: '记录 ID',
      time: '时间',
      line: '线别',
      type: '缺陷类型',
      level: '严重度',
      image: '样本图',
      status: '处理状态',
      handler: '处理人',
      action: '操作',
      detail: '详情',
      handle: '处理'
    },
    detail: {
      title: '缺陷详情',
      detectTime: '检测时间',
      handleTime: '处理时间',
      operator: '检测员',
      handler: '处理人',
      remark: '处理备注',
      save: '保存',
      close: '关闭',
      noImage: '暂无样本图',
      saved: '备注已保存'
    },
    trend: {
      title: '近 7 日趋势',
      defectCount: '缺陷数',
      noData: '暂无趋势数据'
    }
  },

  // 账号管理（W-FRONT-02-E4 扩展）
  account: {
    title: '账号管理',
    subtitle: '管理登录账号、分配角色与重置密码',
    name: '账号',
    displayName: '显示名',
    role: '角色',
    status: '状态',
    active: '启用',
    disabled: '禁用',
    lastLogin: '最近登录',
    resetPassword: '重置密码',
    changeRole: '更换角色',
    placeholder: '🚧 业务对齐期（E 子单实现）',

    // 当前用户卡
    current: {
      title: '当前用户',
      username: '用户名',
      role: '角色',
      permission: '权限',
      lastLogin: '最后登录',
      changePwd: '修改密码'
    },

    // 列表列
    table: {
      id: 'ID',
      username: '用户名',
      role: '角色',
      permission: '权限',
      realName: '姓名',
      contactInfo: '联系方式',
      createdAt: '创建时间',
      status: '状态',
      action: '操作'
    },

    // 弹窗标题
    add: {
      title: '新增账号'
    },
    edit: {
      title: '编辑账号'
    },

    // 表单字段
    form: {
      username: '用户名',
      role: '角色',
      realName: '姓名',
      contactInfo: '联系方式',
      rolePlaceholder: '请选择角色'
    },

    // 修改密码弹窗
    pwd: {
      title: '修改密码',
      old: '旧密码',
      new: '新密码',
      confirm: '确认密码',
      oldPlaceholder: '请输入当前密码',
      newPlaceholder: '至少 6 位',
      confirmPlaceholder: '再次输入新密码',
      mismatch: '两次输入的新密码不一致',
      wrongOld: '旧密码错误',
      success: '密码修改成功'
    },

    // 操作按钮
    action: {
      add: '新增账号',
      edit: '编辑',
      resetPwd: '重置',
      delete: '删除'
    },

    // 确认对话框
    confirm: {
      delete: '确定要删除账号「{name}」吗？此操作不可撤销。',
      reset: '确定要重置账号「{name}」的密码吗？将重置为系统默认密码。'
    },

    // 空 / 错误
    empty: '暂无账号',
    loadFailed: '加载账号列表失败',
    deleteFailed: '删除失败',
    saveFailed: '保存失败',
    noRole: '暂无可用角色',

    // 测试账号（重置后的默认密码）
    defaultPwdNote: '注：重置后默认密码由后端决定，新账号可联系管理员获取'
  },

  // 系统配置
  systemConfig: {
    title: '系统配置',
    subtitle: '采集策略、报警阈值与系统参数',
    groupCollect: '采集配置',
    groupAlarm: '报警阈值',
    groupGeneral: '通用设置',
    interval: '采样间隔',
    retention: '数据保留天数',
    maxAlarm: '最大报警并发',
    theme: '主题',
    placeholder: '🚧 业务对齐期（E 子单实现）'
  },

  // W-FRONT-02-E5 系统配置业务页（追加）
  config: {
    title: '系统配置',
    subtitle: '系统参数 / 线别配置 / 缺陷类型映射',
    tab: {
      system: '系统参数',
      line: '线别配置',
      defectType: '缺陷类型映射'
    },
    form: {
      alarmSoundDevice: '设备报警音频',
      alarmSoundDefect: '缺陷报警音频',
      alarmSoundSystem: '系统报警音频',
      soundPlayCount: '重复播报次数',
      save: '保存',
      saving: '保存中…',
      saveOk: '保存成功',
      saveFail: '保存失败',
      loadOk: '已加载 {n} 条配置',
      loadEmpty: '暂无配置项',
      uploadTip: '请上传 MP3 音频文件，文件名不超过 60 字符，单文件最大 200MB',
      playCountTip: '范围 1-10'
    },
    line: {
      title: '线别列表',
      name: '线别名',
      code: '编码',
      faceNo: '面号',
      color: '颜色',
      desc: '描述',
      add: '新增线别',
      edit: '编辑线别',
      addTitle: '新增线别',
      editTitle: '编辑线别',
      confirmDelete: '确定删除线别「{name}」吗？',
      required: '线别名 / 编码 / 面号 必填',
      empty: '暂无线别',
      createdOk: '新增成功',
      updatedOk: '修改成功',
      deletedOk: '删除成功'
    },
    defectType: {
      title: '缺陷类型列表',
      name: '类型名',
      level: '等级',
      enabled: '启用',
      disabled: '禁用',
      line: '所属线别',
      add: '新增缺陷类型',
      edit: '编辑缺陷类型',
      addTitle: '新增缺陷类型',
      editTitle: '编辑缺陷类型',
      confirmDelete: '确定删除缺陷类型「{name}」吗？',
      required: '类型名 / 等级 / 线别 / 面号 必填',
      empty: '暂无缺陷类型',
      createdOk: '新增成功',
      updatedOk: '修改成功',
      deletedOk: '删除成功'
    },
    action: {
      add: '新增',
      edit: '编辑',
      delete: '删除',
      save: '保存',
      cancel: '取消',
      enabled: '启用',
      disabled: '禁用',
      refresh: '刷新'
    },

    // W-FRONT-02-E5 brief placeholder keys
    briefKeys: {
      alarmSound: '报警声音',
      alarmRetainDays: '报警保留天数',
      syncInterval: '数据同步间隔（秒）',
      screenRefresh: '大屏刷新间隔（秒）',
      defaultLang: '默认语言',
      formSave: '保存',
      lineName: '线别名',
      lineCode: '编码',
      lineDesc: '描述',
      defectTypeName: '类型名',
      defectTypeLevel: '等级',
      defectTypeEnabled: '启用',
      actionAdd: '新增',
      actionEdit: '编辑',
      actionDelete: '删除'
    }
  },

  // 操作日志
  log: {
    title: '操作日志',
    subtitle: '审计所有用户的关键操作',
    user: '用户',
    action: '操作',
    module: '模块',
    ip: 'IP',
    time: '时间',
    result: '结果',
    success: '成功',
    failure: '失败',
    placeholder: '🚧 业务对齐期（E 子单实现）',

    // ===== W-FRONT-02-E6 新增 =====
    filter: {
      operator: '操作者',
      operation: '操作描述',
      module: '模块',
      ip: 'IP',
      result: '结果',
      allResult: '全部结果',
      successOnly: '成功',
      failureOnly: '失败',
      timeRange: '时间范围',
      startTime: '开始时间',
      endTime: '结束时间',
      query: '查询',
      reset: '重置',
      expandMore: '展开更多筛选'
    },
    table: {
      index: '#',
      operator: '操作者',
      module: '模块',
      operation: '操作',
      ip: 'IP',
      uri: '请求路径',
      cost: '耗时',
      result: '结果',
      createTime: '调用时间',
      updateTime: '完成时间',
      action: '操作',
      view: '查看',
      longTimeWarning: '耗时超过 1 秒',
      unitMs: 'ms'
    },
    detail: {
      title: '日志详情',
      basicInfo: '基础信息',
      requestAddress: '客户端 IP',
      requestPath: '请求路径',
      requestCost: '耗时',
      operator: '操作者',
      module: '模块',
      operation: '操作',
      callTime: '调用时间',
      completeTime: '完成时间',
      requestParam: '请求参数',
      responseData: '响应数据',
      none: '（空）',
      copy: '复制',
      copied: '已复制',
      copyFail: '复制失败',
      invalidJson: '非 JSON 字符串，按原文显示'
    },
    list: {
      empty: '暂无操作日志',
      loadFailed: '加载日志失败',
      networkError: '网络异常，请稍后再试',
      longTimeThreshold: '耗时超过 1s 的请求会以红字高亮'
    }
  },

  // 用户管理
  userManage: {
    title: '用户管理',
    subtitle: '维护用户档案、权限与角色绑定',
    realName: '姓名',
    department: '部门',
    email: '邮箱',
    phone: '电话',
    createTime: '创建时间',
    placeholder: '🚧 业务对齐期（E 子单实现）'
  },

  // W-FRONT-02-E7 用户管理业务页（追加）
  user: {
    title: '用户管理',
    subtitle: '车间操作员档案管理',
    kpi: {
      total: '总操作员',
      online: '在线操作员',
      newToday: '今日入职'
    },
    filter: {
      name: '姓名',
      workNo: '工号',
      shift: '班组',
      line: '负责线别'
    },
    table: {
      workNo: '工号',
      name: '姓名',
      shift: '班组',
      line: '负责线别',
      phone: '联系电话',
      hireDate: '入职日期',
      status: '状态',
      detail: '详情',
      edit: '编辑',
      resign: '离职'
    },
    detail: {
      title: '档案详情',
      historyTitle: '操作历史',
      noHistory: '暂无操作记录',
      empty: '暂无操作员数据',
      loadFailed: '加载失败',
      // ===== W-PERF-F 新增：log/list 失败时优雅降级文案 =====
      logUnavailable: '操作历史暂不可用',
      noLogs: '暂无操作记录'
    },
    status: {
      active: '在职',
      resigned: '离职'
    }
  },

  // 大屏模式（W-FRONT-02-E8）
  screen: {
    title: '大屏模式',
    subtitle: '车间可视化的全屏看板',
    enterFullscreen: '进入大屏',
    exitFullscreen: '退出大屏',
    exit: '退出',
    refresh: '刷新',
    placeholder: '🚧 业务对齐期（E 子单实现）',

    // 顶部条
    header: {
      connection: '实时连接',
      connected: '已连接',
      connecting: '连接中',
      disconnected: '已断开',
      lastUpdate: '最近更新',
      fullscreen: '全屏',
      exitFullscreen: '退出全屏',
      back: '返回'
    },

    // KPI 区
    kpi: {
      onlineLines: '在线线别',
      todayOutput: '今日产量',
      todayDefect: '今日缺陷',
      todayAlarm: '今日报警',
      unit: '件'
    },

    // 4 个图表
    chart: {
      trend: '实时趋势',
      defectPie: '缺陷分布',
      alarmList: '最新报警',
      lineGrid: '线别状态',
      trendSub: '计划 / 实际 / 缺陷 — 近 2 小时',
      pieSub: '按缺陷类型聚合（当日）',
      alarmSub: '今日最新 10 条',
      gridSub: '设备在线 / 产量 / 进度'
    },

    // 跑马灯
    ticker: {
      title: '实时播报',
      noData: '暂无最新报警播报'
    },

    // 报警列表
    alarm: {
      time: '时间',
      line: '线别',
      face: '面',
      type: '类型',
      level: '等级',
      message: '描述',
      status: '状态',
      noData: '暂无报警',
      typeDefect: '缺陷',
      typeSystem: '系统',
      typeDevice: '设备',
      levelNormal: '一般',
      levelSerious: '严重',
      statusPending: '未处理',
      statusHandled: '已处理',
      statusIgnored: '已忽略'
    },

    // 线别状态卡
    line: {
      output: '产量',
      defect: '缺陷',
      progress: '进度',
      online: '在线',
      offline: '离线',
      idle: '停机',
      down: '故障',
      running: '运行'
    },

    // 错误兜底
    error: {
      loadFailed: '大屏数据加载失败',
      empty: '暂无数据'
    }
  },

  // 角色 / 权限（路由守卫用）
  role: {
    super_admin: '超级管理员',
    admin: '管理员',
    operator: '操作员',
    viewer: '观察者'
  },
  permission: {
    realtime: '实时数据',
    alarm: '报警管理',
    defect: '缺陷处理',
    account: '账号管理',
    systemConfig: '系统配置',
    log: '操作日志',
    userManage: '用户管理',
    screen: '大屏模式'
  }
}

// ---------------------------------------------------------------------------
// 英文 en-US
// ---------------------------------------------------------------------------
const enUS = {
  app: {
    name: 'DataupLoad',
    slogan: 'Realtime data acquisition · Defect detection · Alarm management',
    loading: 'Loading…',
    welcome: 'Welcome'
  },

  topbar: {
    home: 'Home',
    logout: 'Sign out',
    logoutConfirm: 'Are you sure you want to sign out?',
    profile: 'Profile',
    language: 'Language',
    refresh: 'Refresh',
    fullscreen: 'Fullscreen',
    exitFullscreen: 'Exit fullscreen'
  },

  breadcrumb: {
    home: 'Home',
    realtime: 'Realtime',
    alarm: 'Alarms',
    defect: 'Defects',
    account: 'Accounts',
    systemConfig: 'System',
    log: 'Logs',
    userManage: 'Users',
    screen: 'Big Screen',
    forbidden: 'Forbidden'
  },

  menu: {
    groupMonitor: 'Monitoring',
    groupSystem: 'System',

    realtime: 'Realtime Data',
    alarm: 'Alarms',
    defect: 'Defects',

    account: 'Accounts',
    systemConfig: 'System Config',
    log: 'Audit Logs',
    userManage: 'Users',
    screen: 'Big Screen'
  },

  common: {
    confirm: 'OK',
    cancel: 'Cancel',
    save: 'Save',
    edit: 'Edit',
    delete: 'Delete',
    add: 'Add',
    search: 'Search',
    reset: 'Reset',
    refresh: 'Refresh',
    export: 'Export',
    import: 'Import',
    submit: 'Submit',
    back: 'Back',
    next: 'Next',
    prev: 'Previous',
    close: 'Close',
    more: 'More',
    yes: 'Yes',
    no: 'No',
    all: 'All',
    success: 'Success',
    failed: 'Failed',
    comingSoon: 'Coming soon',
    pending: 'Pending',
    processing: 'Processing',
    done: 'Done',
    loading: 'Loading…',
    noData: 'No data',
    updateTime: 'Updated',
    action: 'Action'
  },

  login: {
    title: 'DataupLoad',
    subtitle: 'Realtime data · Defect detection · Alarm management',
    username: 'Username',
    password: 'Password',
    remember: 'Remember me',
    submit: 'Sign in',
    submitting: 'Signing in…',
    usernamePlaceholder: 'Enter username',
    passwordPlaceholder: 'Enter password',
    errorRequired: 'Please enter username and password',
    errorCredential: 'Wrong username or password',
    errorNetwork: 'Network error, please retry',
    copyright: '© 2026 DataupLoad',
    tip: 'Default: super_admin / Abc12345'
  },

  forbidden: {
    title: 'Forbidden',
    subtitle: 'You do not have access to this page',
    description: 'Please contact the administrator for permission, or return home.',
    backHome: 'Back home',
    contactAdmin: 'Contact admin'
  },
  notFound: {
    title: 'Page not found',
    subtitle: '404 — The page you visited does not exist',
    backHome: 'Back home'
  },
  serverError: {
    title: 'Server error',
    subtitle: '500 — System busy, please retry',
    retry: 'Retry'
  },

  realtime: {
    title: 'Realtime Data',
    subtitle: 'Live data streams, device status, and production cadence',
    online: 'Online',
    offline: 'Offline',
    deviceCount: 'Devices',
    activeAlarms: 'Active alarms',
    todayOutput: 'Today output',
    throughput: 'Throughput',
    lastUpdate: 'Last update',
    chart: 'Live trend',
    table: 'Acquisition points',
    placeholder: '🚧 Pending business alignment (subtask E)',
    // ===== W-FRONT-02-E1 新增 KPI / 图表 / 表格 字段 =====
    kpi: {
      onlineLines: 'Online Lines',
      todayOutput: "Today's Output",
      todayDefect: "Today's Defect",
      todayAlarm: "Today's Alarms",
      onlineLinesHint: 'active lines / faces',
      fromLines: 'aggregated from {n} lines',
      defectRate: 'defect rate {rate}',
      alarmHint: 'new alarms today',
      // ===== W-RT-4: extra KPI keys (11 fields) =====
      productTotal: 'Total Output',
      efficiency: 'Realtime Efficiency',
      efficiencyUnit: 'pcs/min',
      efficiencyHint: 'current line cadence',
      occupancy: 'Occupancy',
      occupancyRate: 'Occupancy Rate',
      occupancyRateHint: 'occupancy {rate}',
      occupancyHint: 'occupancy count {n}',
      failCount: 'Defect Count',
      failRate: 'Defect Rate',
      failRateHint: 'defect rate {rate}',
      failCountHint: 'defect count {n}',
      successCount: 'Good Count',
      successRateHint: 'good rate {rate}',
      removeTotal: 'Removed Total',
      removeFailNum: 'Remove Failures',
      removeFailRate: 'Remove Fail Rate',
      removeFailRateHint: 'fail rate {rate}',
      removeFailHint: 'removed {num} / fail rate {rate}',
      deviceOpenTime: 'Device Start Time',
      deviceOpenTimeHint: 'raw: {raw}',
      selectedLine: 'selected: {line}'
    },
    chartTitle: 'Realtime Trend',
    chart: {
      title: 'Realtime Trend',
      plan: 'Plan',
      actual: 'Actual',
      defect: 'Defect',
      allLines: 'All lines',
      allLinesSub: 'last 2 hours across all lines',
      selectedSub: 'selected: {lines}'
    },
    tableTitle: 'Line Status',
    table: {
      title: 'Line Status',
      total: 'total',
      line: 'Line',
      status: 'Status',
      output: 'Output',
      defect: 'Defect',
      progress: 'Progress',
      stateRunning: 'Running',
      stateIdle: 'Idle',
      stateDown: 'Fault'
    },
    // ===== W-RT-2 Line list card =====
    lineList: {
      title: 'Production Lines',
      total: 'Total',
      defect: 'Defect',
      remove: 'Remove',
      // ===== W-RT-7: drag-to-reorder keys =====
      dragHint: 'Drag to reorder',
      reorderSuccess: 'Order saved',
      reorderFail: 'Reorder failed: {msg}'
    },
    // ===== W-RT-5: realtime page new feature keys =====
    // realtime.ws.*  (W-RT-1 / W-PERF-B)
    ws: {
      connected: 'WS Connected',
      connecting: 'Connecting...',
      disconnected: 'Disconnected (Reconnecting)'
    },
    // realtime.detail.*  (W-RT-3 — 14 keys)
    detail: {
      production: 'Production',
      defect: 'Defect Grid',
      device: 'Device Status',
      time: 'Time Info',
      total: 'Total',
      good: 'Good',
      bad: 'Bad',
      efficiency: 'Efficiency',
      camera: 'Camera',
      encoder: 'Encoder',
      plc: 'PLC',
      online: 'Online',
      offline: 'Offline',
      runtime: 'Runtime'
    },
    error: {
      network: 'Network error, please retry',
      loadLineFailed: 'Failed to load lines'
    },
    metric: {
      temperature: 'Temperature',
      pressure: 'Pressure',
      humidity: 'Humidity',
      speed: 'Speed',
      voltage: 'Voltage'
    }
  },

  alarm: {
    title: 'Alarms',
    subtitle: 'View, handle, and archive alarm events',
    newAlarm: 'New',
    acknowledged: 'Acknowledged',
    resolved: 'Resolved',
    ignored: 'Ignored',
    level: 'Level',
    levelCritical: 'Critical',
    levelMajor: 'Major',
    levelMinor: 'Minor',
    levelWarning: 'Warning',
    device: 'Device',
    time: 'Time',
    message: 'Message',
    action: 'Action',
    acknowledge: 'Ack',
    resolve: 'Resolve',
    ignore: 'Ignore',
    detail: 'Detail',
    placeholder: '🚧 Pending business alignment (subtask E)',
    // ----- W-FRONT-02-E2 new keys -----
    filter: {
      timeRange: 'Time Range',
      range1h: 'Last 1 hour',
      range24h: 'Last 24 hours',
      range7d: 'Last 7 days',
      rangeCustom: 'Custom',
      line: 'Line',
      type: 'Type',
      status: 'Status',
      allLine: 'All lines',
      allType: 'All types',
      allStatus: 'All statuses',
      reset: 'Reset',
      query: 'Query',
      customRange: 'Custom range'
    },
    typeOption: {
      defect: 'Defect',
      system: 'System',
      device: 'Device'
    },
    table: {
      triggerTime: 'Trigger Time',
      line: 'Line',
      face: 'Face',
      type: 'Type',
      level: 'Level',
      desc: 'Description',
      status: 'Status',
      action: 'Action',
      detail: 'Detail',
      ignore: 'Ignore',
      index: '#'
    },
    levelOption: {
      normal: 'Normal',
      serious: 'Serious'
    },
    status: {
      pending: 'Pending',
      handled: 'Handled',
      ignored: 'Ignored'
    },
    detail: {
      title: 'Alarm Detail',
      id: 'Alarm ID',
      uuid: 'UUID',
      triggerTime: 'Trigger Time',
      type: 'Type',
      level: 'Level',
      line: 'Line',
      face: 'Face',
      duration: 'Duration',
      desc: 'Full Description',
      defect: 'Defect Name',
      image: 'Related Image',
      noImage: 'No image available',
      ignore: 'Ignore',
      handle: 'Handle',
      close: 'Close'
    },
    ws: {
      connected: 'Realtime Connected',
      disconnected: 'Realtime Disconnected',
      connecting: 'Connecting to realtime channel…',
      reconnecting: 'Disconnected, reconnecting…'
    },
    list: {
      refresh: 'Refresh',
      empty: 'No alarm records',
      loadFailed: 'Failed to load alarm list',
      ignoreSuccess: 'Ignored',
      ignoreFailed: 'Ignore failed',
      ignoreConfirm: 'Ignore this alarm?',
      multiIgnoreConfirm: 'Ignore all pending alarms?'
    },
    lineTree: {
      loadFailed: 'Failed to load line tree'
    },
    sort: {
      asc: 'Time ascending',
      desc: 'Time descending'
    },
    // ===== W-RT-8 Alarm Hint Badge =====
    hint: {
      title: 'Pending Alarms',
      empty: 'No pending alarms',
      viewAll: 'View All',
      recent: 'Recent',
      dragHint: 'Draggable',
      clickHint: 'Click for details'
    },
    // ===== W-DEFECT-CFG 子单 C =====
    tab: {
      records: 'Alarm Records',
      defectConfig: 'Defect Config'
    }
  },

  // ===== W-DEFECT-CFG 子单 C =====
  defectConfig: {
    search: {
      name: 'Defect Name',
      namePlaceholder: 'Enter defect name (fuzzy)',
      category: 'Category',
      categoryPlaceholder: 'All categories'
    },
    category: {
      defect: 'Defect',
      system: 'System',
      device: 'Device'
    },
    table: {
      index: '#',
      name: 'Defect Name',
      type: 'Category',
      alarmEnable: 'To Screen',
      soundEnable: 'Sound',
      sendYkEnable: 'To Yingke',
      createTime: 'Created',
      action: 'Actions'
    },
    action: {
      add: 'New Defect',
      edit: 'Edit Defect'
    },
    form: {
      name: 'Defect Name',
      namePlaceholder: 'Enter defect name (1-20 chars)',
      nameRequired: 'Defect name is required',
      nameLength: 'Name length must be 1-20 chars',
      category: 'Category',
      categoryPlaceholder: 'Select category',
      categoryRequired: 'Category is required',
      alarmEnable: 'Push to Screen',
      alarmEnableRequired: 'Choose whether to push to screen',
      soundEnable: 'Sound Alarm',
      soundEnableRequired: 'Choose whether to enable sound',
      soundEnableHint: 'Only effective when screen push is enabled',
      sendYkEnable: 'Push to Yingke',
      sendYkEnableRequired: 'Choose whether to push to Yingke',
      sendYkEnableHint: 'When enabled, push to MES via Yingke'
    },
    confirm: {
      title: 'Confirm',
      delete: 'Delete this defect?'
    },
    apiMsg: {
      addSuccess: 'Created',
      addFailed: 'Create failed',
      addError: 'Create error',
      editSuccess: 'Updated',
      editFailed: 'Update failed',
      editError: 'Update error',
      deleteSuccess: 'Deleted',
      deleteFailed: 'Delete failed',
      deleteError: 'Delete error'
    },
    list: {
      loadFailed: 'Failed to load defect list',
      empty: 'No defect configuration'
    }
  },

  defect: {
    title: 'Defects',
    subtitle: 'Classify, review, and close defects',
    newDefect: 'New defect',
    pendingReview: 'Pending review',
    reviewing: 'Reviewing',
    closed: 'Closed',
    type: 'Type',
    severity: 'Severity',
    image: 'Image',
    location: 'Location',
    createTime: 'Created',
    operator: 'Operator',
    review: 'Review',
    close: 'Close',
    placeholder: '🚧 Pending business alignment (subtask E)',
    // ===== W-FRONT-02-E3 additions =====
    kpi: {
      total: "Today's Total Defects",
      severe: 'Severe Defects',
      handled: 'Handled',
      missRate: 'Miss Rate'
    },
    filter: {
      date: 'Date',
      line: 'Line',
      type: 'Defect Type',
      level: 'Severity',
      query: 'Query',
      reset: 'Reset'
    },
    level: {
      severe: 'Severe',
      normal: 'Normal'
    },
    status: {
      pending: 'Pending',
      handled: 'Handled'
    },
    table: {
      id: 'Record ID',
      time: 'Time',
      line: 'Line',
      type: 'Defect Type',
      level: 'Severity',
      image: 'Sample',
      status: 'Status',
      handler: 'Handler',
      action: 'Action',
      detail: 'Detail',
      handle: 'Handle'
    },
    detail: {
      title: 'Defect Detail',
      detectTime: 'Detect Time',
      handleTime: 'Handle Time',
      operator: 'Operator',
      handler: 'Handler',
      remark: 'Remark',
      save: 'Save',
      close: 'Close',
      noImage: 'No sample image',
      saved: 'Remark saved'
    },
    trend: {
      title: '7-Day Trend',
      defectCount: 'Defects',
      noData: 'No trend data'
    }
  },

  account: {
    title: 'Accounts',
    subtitle: 'Manage sign-in accounts, roles and password resets',
    name: 'Account',
    displayName: 'Display name',
    role: 'Role',
    status: 'Status',
    active: 'Active',
    disabled: 'Disabled',
    lastLogin: 'Last login',
    resetPassword: 'Reset password',
    changeRole: 'Change role',
    placeholder: '🚧 Pending business alignment (subtask E)',

    // Current user card
    current: {
      title: 'Current User',
      username: 'Username',
      role: 'Role',
      permission: 'Permissions',
      lastLogin: 'Last Login',
      changePwd: 'Change Password'
    },

    // Table columns
    table: {
      id: 'ID',
      username: 'Username',
      role: 'Role',
      permission: 'Permissions',
      realName: 'Real Name',
      contactInfo: 'Contact',
      createdAt: 'Created',
      status: 'Status',
      action: 'Actions'
    },

    // Dialog titles
    add: {
      title: 'Add Account'
    },
    edit: {
      title: 'Edit Account'
    },

    // Form fields
    form: {
      username: 'Username',
      role: 'Role',
      realName: 'Real Name',
      contactInfo: 'Contact',
      rolePlaceholder: 'Select role'
    },

    // Change password dialog
    pwd: {
      title: 'Change Password',
      old: 'Old Password',
      new: 'New Password',
      confirm: 'Confirm Password',
      oldPlaceholder: 'Enter current password',
      newPlaceholder: 'At least 6 characters',
      confirmPlaceholder: 'Re-enter new password',
      mismatch: 'New passwords do not match',
      wrongOld: 'Old password is incorrect',
      success: 'Password updated'
    },

    // Action buttons
    action: {
      add: 'Add Account',
      edit: 'Edit',
      resetPwd: 'Reset',
      delete: 'Delete'
    },

    // Confirm dialogs
    confirm: {
      delete: 'Delete account "{name}"? This cannot be undone.',
      reset: 'Reset password for "{name}"? The password will revert to the system default.'
    },

    // Empty / error
    empty: 'No accounts',
    loadFailed: 'Failed to load accounts',
    deleteFailed: 'Delete failed',
    saveFailed: 'Save failed',
    noRole: 'No roles available',

    defaultPwdNote: 'Note: After reset, the default password is determined by the backend; contact admin for the new password.'
  },

  systemConfig: {
    title: 'System Config',
    subtitle: 'Sampling strategy, alarm thresholds, and system params',
    groupCollect: 'Acquisition',
    groupAlarm: 'Alarm thresholds',
    groupGeneral: 'General',
    interval: 'Sample interval',
    retention: 'Data retention (days)',
    maxAlarm: 'Max concurrent alarms',
    theme: 'Theme',
    placeholder: '🚧 Pending business alignment (subtask E)'
  },

  // W-FRONT-02-E5 system config business page (added)
  config: {
    title: 'System Config',
    subtitle: 'System params / Line config / Defect type mapping',
    tab: {
      system: 'System Params',
      line: 'Line Config',
      defectType: 'Defect Type Mapping'
    },
    form: {
      alarmSoundDevice: 'Device alarm sound',
      alarmSoundDefect: 'Defect alarm sound',
      alarmSoundSystem: 'System alarm sound',
      soundPlayCount: 'Sound repeat count',
      save: 'Save',
      saving: 'Saving…',
      saveOk: 'Saved successfully',
      saveFail: 'Save failed',
      loadOk: 'Loaded {n} config items',
      loadEmpty: 'No config items',
      uploadTip: 'Upload MP3 audio, name ≤ 60 chars, max 200MB per file',
      playCountTip: 'Range 1-10'
    },
    line: {
      title: 'Line list',
      name: 'Line name',
      code: 'Code',
      faceNo: 'Face',
      color: 'Color',
      desc: 'Description',
      add: 'Add line',
      edit: 'Edit line',
      addTitle: 'Add line',
      editTitle: 'Edit line',
      confirmDelete: 'Delete line "{name}"?',
      required: 'Name / Code / Face are required',
      empty: 'No lines',
      createdOk: 'Created',
      updatedOk: 'Updated',
      deletedOk: 'Deleted'
    },
    defectType: {
      title: 'Defect type list',
      name: 'Type name',
      level: 'Level',
      enabled: 'Enabled',
      disabled: 'Disabled',
      line: 'Line',
      add: 'Add defect type',
      edit: 'Edit defect type',
      addTitle: 'Add defect type',
      editTitle: 'Edit defect type',
      confirmDelete: 'Delete defect type "{name}"?',
      required: 'Name / Level / Line / Face are required',
      empty: 'No defect types',
      createdOk: 'Created',
      updatedOk: 'Updated',
      deletedOk: 'Deleted'
    },
    action: {
      add: 'Add',
      edit: 'Edit',
      delete: 'Delete',
      save: 'Save',
      cancel: 'Cancel',
      enabled: 'Enable',
      disabled: 'Disable',
      refresh: 'Refresh'
    },

    // W-FRONT-02-E5 brief placeholder keys
    briefKeys: {
      alarmSound: 'Alarm Sound',
      alarmRetainDays: 'Alarm Retain Days',
      syncInterval: 'Sync Interval (s)',
      screenRefresh: 'Screen Refresh (s)',
      defaultLang: 'Default Lang',
      formSave: 'Save',
      lineName: 'Line Name',
      lineCode: 'Code',
      lineDesc: 'Description',
      defectTypeName: 'Type Name',
      defectTypeLevel: 'Level',
      defectTypeEnabled: 'Enabled',
      actionAdd: 'Add',
      actionEdit: 'Edit',
      actionDelete: 'Delete'
    }
  },

  log: {
    title: 'Audit Logs',
    subtitle: 'Audit every key user operation',
    user: 'User',
    action: 'Action',
    module: 'Module',
    ip: 'IP',
    time: 'Time',
    result: 'Result',
    success: 'Success',
    failure: 'Failure',
    placeholder: '🚧 Pending business alignment (subtask E)',

    // ===== W-FRONT-02-E6 additions =====
    filter: {
      operator: 'Operator',
      operation: 'Operation',
      module: 'Module',
      ip: 'IP',
      result: 'Result',
      allResult: 'All results',
      successOnly: 'Success',
      failureOnly: 'Failure',
      timeRange: 'Time range',
      startTime: 'Start time',
      endTime: 'End time',
      query: 'Query',
      reset: 'Reset',
      expandMore: 'More filters'
    },
    table: {
      index: '#',
      operator: 'Operator',
      module: 'Module',
      operation: 'Operation',
      ip: 'IP',
      uri: 'Request path',
      cost: 'Cost',
      result: 'Result',
      createTime: 'Call time',
      updateTime: 'Complete time',
      action: 'Action',
      view: 'View',
      longTimeWarning: 'Cost > 1s',
      unitMs: 'ms'
    },
    detail: {
      title: 'Log Detail',
      basicInfo: 'Basic Info',
      requestAddress: 'Client IP',
      requestPath: 'Request path',
      requestCost: 'Cost',
      operator: 'Operator',
      module: 'Module',
      operation: 'Operation',
      callTime: 'Call time',
      completeTime: 'Complete time',
      requestParam: 'Request body',
      responseData: 'Response body',
      none: '(empty)',
      copy: 'Copy',
      copied: 'Copied',
      copyFail: 'Copy failed',
      invalidJson: 'Not a JSON string, showing raw text'
    },
    list: {
      empty: 'No audit logs',
      loadFailed: 'Failed to load logs',
      networkError: 'Network error, please retry',
      longTimeThreshold: 'Requests slower than 1s are highlighted in red'
    }
  },

  userManage: {
    title: 'Users',
    subtitle: 'Maintain user profiles, permissions, and roles',
    realName: 'Real name',
    department: 'Department',
    email: 'Email',
    phone: 'Phone',
    createTime: 'Created',
    placeholder: '🚧 Pending business alignment (subtask E)'
  },

  // W-FRONT-02-E7 user management business page (added)
  user: {
    title: 'User Management',
    subtitle: 'Workshop operator profile management',
    kpi: {
      total: 'Total Operators',
      online: 'Online Operators',
      newToday: 'New Today'
    },
    filter: {
      name: 'Name',
      workNo: 'Work No.',
      shift: 'Shift',
      line: 'Assigned Line'
    },
    table: {
      workNo: 'Work No.',
      name: 'Name',
      shift: 'Shift',
      line: 'Assigned Line',
      phone: 'Phone',
      hireDate: 'Hire Date',
      status: 'Status',
      detail: 'Detail',
      edit: 'Edit',
      resign: 'Resign'
    },
    detail: {
      title: 'Profile Detail',
      historyTitle: 'Operation History',
      noHistory: 'No operation records',
      empty: 'No operator data',
      loadFailed: 'Load failed',
      // ===== W-PERF-F additions: graceful degradation when log/list fails =====
      logUnavailable: 'Operation history temporarily unavailable',
      noLogs: 'No operation records'
    },
    status: {
      active: 'Active',
      resigned: 'Resigned'
    }
  },

  // Big Screen (W-FRONT-02-E8)
  screen: {
    title: 'Big Screen',
    subtitle: 'Workshop visualization dashboard',
    enterFullscreen: 'Enter big screen',
    exitFullscreen: 'Exit big screen',
    exit: 'Exit',
    refresh: 'Refresh',
    placeholder: '🚧 Pending business alignment (subtask E)',

    header: {
      connection: 'Realtime',
      connected: 'Connected',
      connecting: 'Connecting',
      disconnected: 'Disconnected',
      lastUpdate: 'Updated',
      fullscreen: 'Fullscreen',
      exitFullscreen: 'Exit fullscreen',
      back: 'Back'
    },

    kpi: {
      onlineLines: 'Online lines',
      todayOutput: 'Today output',
      todayDefect: 'Today defects',
      todayAlarm: 'Today alarms',
      unit: 'pcs'
    },

    chart: {
      trend: 'Realtime trend',
      defectPie: 'Defect distribution',
      alarmList: 'Latest alarms',
      lineGrid: 'Line status',
      trendSub: 'Plan / Actual / Defect — last 2h',
      pieSub: 'Aggregated by defect type (today)',
      alarmSub: 'Latest 10 alarms today',
      gridSub: 'Device / output / progress'
    },

    ticker: {
      title: 'Live ticker',
      noData: 'No new alarms to broadcast'
    },

    alarm: {
      time: 'Time',
      line: 'Line',
      face: 'Face',
      type: 'Type',
      level: 'Level',
      message: 'Message',
      status: 'Status',
      noData: 'No alarms',
      typeDefect: 'Defect',
      typeSystem: 'System',
      typeDevice: 'Device',
      levelNormal: 'Normal',
      levelSerious: 'Serious',
      statusPending: 'Pending',
      statusHandled: 'Handled',
      statusIgnored: 'Ignored'
    },

    line: {
      output: 'Output',
      defect: 'Defect',
      progress: 'Progress',
      online: 'Online',
      offline: 'Offline',
      idle: 'Idle',
      down: 'Down',
      running: 'Running'
    },

    error: {
      loadFailed: 'Screen data load failed',
      empty: 'No data'
    }
  },

  role: {
    super_admin: 'Super Admin',
    admin: 'Admin',
    operator: 'Operator',
    viewer: 'Viewer'
  },
  permission: {
    realtime: 'Realtime',
    alarm: 'Alarms',
    defect: 'Defects',
    account: 'Accounts',
    systemConfig: 'System Config',
    log: 'Audit Logs',
    userManage: 'Users',
    screen: 'Big Screen'
  }
}

// ---------------------------------------------------------------------------
// 印尼语 id-ID
// ---------------------------------------------------------------------------
const idID = {
  app: {
    name: 'DataupLoad',
    slogan: 'Akuisisi data realtime · Deteksi cacat · Manajemen alarm',
    loading: 'Memuat…',
    welcome: 'Selamat datang'
  },

  topbar: {
    home: 'Beranda',
    logout: 'Keluar',
    logoutConfirm: 'Apakah Anda yakin ingin keluar?',
    profile: 'Profil',
    language: 'Bahasa',
    refresh: 'Segarkan',
    fullscreen: 'Layar penuh',
    exitFullscreen: 'Keluar layar penuh'
  },

  breadcrumb: {
    home: 'Beranda',
    realtime: 'Realtime',
    alarm: 'Alarm',
    defect: 'Cacat',
    account: 'Akun',
    systemConfig: 'Sistem',
    log: 'Log',
    userManage: 'Pengguna',
    screen: 'Layar Besar',
    forbidden: 'Dilarang'
  },

  menu: {
    groupMonitor: 'Pemantauan',
    groupSystem: 'Sistem',

    realtime: 'Data Realtime',
    alarm: 'Manajemen Alarm',
    defect: 'Penanganan Cacat',

    account: 'Manajemen Akun',
    systemConfig: 'Konfigurasi Sistem',
    log: 'Log Operasi',
    userManage: 'Manajemen Pengguna',
    screen: 'Layar Besar'
  },

  common: {
    confirm: 'OK',
    cancel: 'Batal',
    save: 'Simpan',
    edit: 'Ubah',
    delete: 'Hapus',
    add: 'Tambah',
    search: 'Cari',
    reset: 'Reset',
    refresh: 'Segarkan',
    export: 'Ekspor',
    import: 'Impor',
    submit: 'Kirim',
    back: 'Kembali',
    next: 'Berikutnya',
    prev: 'Sebelumnya',
    close: 'Tutup',
    more: 'Lainnya',
    yes: 'Ya',
    no: 'Tidak',
    all: 'Semua',
    success: 'Berhasil',
    failed: 'Gagal',
    comingSoon: 'Segera hadir',
    pending: 'Tertunda',
    processing: 'Memproses',
    done: 'Selesai',
    loading: 'Memuat…',
    noData: 'Tidak ada data',
    updateTime: 'Diperbarui',
    action: 'Aksi'
  },

  login: {
    title: 'DataupLoad',
    subtitle: 'Akuisisi data realtime · Deteksi cacat · Manajemen alarm',
    username: 'Nama pengguna',
    password: 'Kata sandi',
    remember: 'Ingat saya',
    submit: 'Masuk',
    submitting: 'Masuk…',
    usernamePlaceholder: 'Masukkan nama pengguna',
    passwordPlaceholder: 'Masukkan kata sandi',
    errorRequired: 'Harap isi nama pengguna dan kata sandi',
    errorCredential: 'Nama pengguna atau kata sandi salah',
    errorNetwork: 'Kesalahan jaringan, coba lagi',
    copyright: '© 2026 DataupLoad',
    tip: 'Default: super_admin / Abc12345'
  },

  forbidden: {
    title: 'Dilarang',
    subtitle: 'Anda tidak memiliki akses ke halaman ini',
    description: 'Hubungi administrator untuk izin, atau kembali ke beranda.',
    backHome: 'Kembali',
    contactAdmin: 'Hubungi admin'
  },
  notFound: {
    title: 'Halaman tidak ditemukan',
    subtitle: '404 — Halaman yang Anda cari tidak ada',
    backHome: 'Kembali'
  },
  serverError: {
    title: 'Kesalahan server',
    subtitle: '500 — Sistem sibuk, coba lagi',
    retry: 'Coba lagi'
  },

  realtime: {
    title: 'Data Realtime',
    subtitle: 'Lihat aliran data, status perangkat, dan ritme produksi',
    online: 'Online',
    offline: 'Offline',
    deviceCount: 'Perangkat',
    activeAlarms: 'Alarm aktif',
    todayOutput: 'Output hari ini',
    throughput: 'Throughput',
    lastUpdate: 'Pembaruan',
    chart: 'Tren realtime',
    table: 'Titik akuisisi',
    placeholder: '🚧 Penyelarasan bisnis (subtugas E)',
    // ===== W-FRONT-02-E1 新增 KPI / 图表 / 表格 字段 =====
    kpi: {
      onlineLines: 'Lanes Aktif',
      todayOutput: 'Output Hari Ini',
      todayDefect: 'Cacat Hari Ini',
      todayAlarm: 'Alarm Hari Ini',
      onlineLinesHint: 'lanes / face aktif',
      fromLines: 'agregat dari {n} lanes',
      defectRate: 'tingkat cacat {rate}',
      alarmHint: 'alarm baru hari ini',
      // ===== W-RT-4: extra KPI keys (11 fields) =====
      productTotal: 'Total Produksi',
      efficiency: 'Efisiensi Realtime',
      efficiencyUnit: 'pcs/menit',
      efficiencyHint: 'irama lane saat ini',
      occupancy: 'Jumlah Occupancy',
      occupancyRate: 'Tingkat Occupancy',
      occupancyRateHint: 'occupancy {rate}',
      occupancyHint: 'occupancy {n}',
      failCount: 'Jumlah Cacat',
      failRate: 'Tingkat Cacat',
      failRateHint: 'tingkat cacat {rate}',
      failCountHint: 'cacat {n}',
      successCount: 'Jumlah Bagus',
      successRateHint: 'tingkat bagus {rate}',
      removeTotal: 'Total Pembuangan',
      removeFailNum: 'Gagal Buang',
      removeFailRate: 'Tingkat Gagal Buang',
      removeFailRateHint: 'tingkat gagal {rate}',
      removeFailHint: 'buang {num} / gagal {rate}',
      deviceOpenTime: 'Waktu Mulai Perangkat',
      deviceOpenTimeHint: 'mentah: {raw}',
      selectedLine: 'dipilih: {line}'
    },
    chartTitle: 'Tren Realtime',
    chart: {
      title: 'Tren Realtime',
      plan: 'Rencana',
      actual: 'Aktual',
      defect: 'Cacat',
      allLines: 'Semua lanes',
      allLinesSub: '2 jam terakhir lintas semua lanes',
      selectedSub: 'dipilih: {lines}'
    },
    tableTitle: 'Status Lane',
    table: {
      title: 'Status Lane',
      total: 'total',
      line: 'Lane',
      status: 'Status',
      output: 'Output',
      defect: 'Cacat',
      progress: 'Progres',
      stateRunning: 'Berjalan',
      stateIdle: 'Berhenti',
      stateDown: 'Gangguan'
    },
    // ===== W-RT-2 Daftar lane (kartu daftar line) =====
    lineList: {
      title: 'Daftar Lane',
      total: 'Total',
      defect: 'Cacat',
      remove: 'Buang',
      // ===== W-RT-7: kunci seret-untuk-menyusun =====
      dragHint: 'Seret untuk menyusun ulang',
      reorderSuccess: 'Urutan disimpan',
      reorderFail: 'Penyusunan ulang gagal: {msg}'
    },
    // ===== W-RT-5: tombol fitur baru halaman realtime =====
    // realtime.ws.*  (W-RT-1 / W-PERF-B)
    ws: {
      connected: 'WS Terhubung',
      connecting: 'Menghubungkan...',
      disconnected: 'Terputus (Menyambungkan ulang)'
    },
    // realtime.detail.*  (W-RT-3 — 14 kunci)
    detail: {
      production: 'Produksi',
      defect: 'Kisi Cacat',
      device: 'Status Perangkat',
      time: 'Informasi Waktu',
      total: 'Total',
      good: 'Bagus',
      bad: 'Cacat',
      efficiency: 'Efisiensi',
      camera: 'Kamera',
      encoder: 'Encoder',
      plc: 'PLC',
      online: 'Online',
      offline: 'Offline',
      runtime: 'Durasi'
    },
    error: {
      network: 'Kesalahan jaringan, coba lagi',
      loadLineFailed: 'Gagal memuat lanes'
    },
    metric: {
      temperature: 'Suhu',
      pressure: 'Tekanan',
      humidity: 'Kelembaban',
      speed: 'Kecepatan',
      voltage: 'Tegangan'
    }
  },

  alarm: {
    title: 'Manajemen Alarm',
    subtitle: 'Lihat, tangani, dan arsipkan kejadian alarm',
    newAlarm: 'Baru',
    acknowledged: 'Dikonfirmasi',
    resolved: 'Selesai',
    ignored: 'Diabaikan',
    level: 'Tingkat',
    levelCritical: 'Kritis',
    levelMajor: 'Mayor',
    levelMinor: 'Minor',
    levelWarning: 'Peringatan',
    device: 'Perangkat',
    time: 'Waktu',
    message: 'Pesan',
    action: 'Aksi',
    acknowledge: 'Konfirmasi',
    resolve: 'Selesaikan',
    ignore: 'Abaikan',
    detail: 'Detail',
    placeholder: '🚧 Penyelarasan bisnis (subtugas E)',
    // ----- W-FRONT-02-E2 kunci baru -----
    filter: {
      timeRange: 'Rentang Waktu',
      range1h: '1 jam terakhir',
      range24h: '24 jam terakhir',
      range7d: '7 hari terakhir',
      rangeCustom: 'Kustom',
      line: 'Lini',
      type: 'Tipe',
      status: 'Status',
      allLine: 'Semua lini',
      allType: 'Semua tipe',
      allStatus: 'Semua status',
      reset: 'Reset',
      query: 'Kueri',
      customRange: 'Rentang kustom'
    },
    typeOption: {
      defect: 'Cacat',
      system: 'Sistem',
      device: 'Perangkat'
    },
    table: {
      triggerTime: 'Waktu Trigger',
      line: 'Lini',
      face: 'Stasiun',
      type: 'Tipe',
      level: 'Tingkat',
      desc: 'Deskripsi',
      status: 'Status',
      action: 'Aksi',
      detail: 'Detail',
      ignore: 'Abaikan',
      index: '#'
    },
    levelOption: {
      normal: 'Normal',
      serious: 'Serius'
    },
    status: {
      pending: 'Tertunda',
      handled: 'Ditangani',
      ignored: 'Diabaikan'
    },
    detail: {
      title: 'Detail Alarm',
      id: 'ID Alarm',
      uuid: 'UUID',
      triggerTime: 'Waktu Trigger',
      type: 'Tipe',
      level: 'Tingkat',
      line: 'Lini',
      face: 'Stasiun',
      duration: 'Durasi',
      desc: 'Deskripsi Lengkap',
      defect: 'Nama Cacat',
      image: 'Gambar Terkait',
      noImage: 'Tidak ada gambar',
      ignore: 'Abaikan',
      handle: 'Tangani',
      close: 'Tutup'
    },
    ws: {
      connected: 'Realtime Terhubung',
      disconnected: 'Realtime Terputus',
      connecting: 'Menghubungkan ke channel realtime…',
      reconnecting: 'Terputus, menyambungkan ulang…'
    },
    list: {
      refresh: 'Segarkan',
      empty: 'Tidak ada catatan alarm',
      loadFailed: 'Gagal memuat daftar alarm',
      ignoreSuccess: 'Diabaikan',
      ignoreFailed: 'Gagal mengabaikan',
      ignoreConfirm: 'Abaikan alarm ini?',
      multiIgnoreConfirm: 'Abaikan semua alarm yang tertunda?'
    },
    lineTree: {
      loadFailed: 'Gagal memuat data lini'
    },
    sort: {
      asc: 'Waktu naik',
      desc: 'Waktu turun'
    },
    // ===== W-RT-8 Lencana Alarm =====
    hint: {
      title: 'Alarm Tertunda',
      empty: 'Tidak ada alarm tertunda',
      viewAll: 'Lihat Semua',
      recent: 'Terkini',
      dragHint: 'Dapat diseret',
      clickHint: 'Klik untuk detail'
    },
    // ===== W-DEFECT-CFG 子单 C =====
    tab: {
      records: 'Catatan Alarm',
      defectConfig: 'Konfigurasi Cacat'
    }
  },

  // ===== W-DEFECT-CFG 子单 C =====
  defectConfig: {
    search: {
      name: 'Nama Cacat',
      namePlaceholder: 'Masukkan nama cacat (fuzzy)',
      category: 'Tipe Cacat',
      categoryPlaceholder: 'Semua tipe'
    },
    category: {
      defect: 'Cacat',
      system: 'Sistem',
      device: 'Perangkat'
    },
    table: {
      index: '#',
      name: 'Nama Cacat',
      type: 'Tipe',
      alarmEnable: 'Ke Layar',
      soundEnable: 'Suara',
      sendYkEnable: 'Ke Yingke',
      createTime: 'Dibuat',
      action: 'Aksi'
    },
    action: {
      add: 'Cacat Baru',
      edit: 'Edit Cacat'
    },
    form: {
      name: 'Nama Cacat',
      namePlaceholder: 'Masukkan nama cacat (1-20 karakter)',
      nameRequired: 'Nama cacat wajib diisi',
      nameLength: 'Panjang nama 1-20 karakter',
      category: 'Tipe',
      categoryPlaceholder: 'Pilih tipe',
      categoryRequired: 'Tipe wajib dipilih',
      alarmEnable: 'Kirim ke Layar',
      alarmEnableRequired: 'Pilih apakah kirim ke layar',
      soundEnable: 'Alarm Suara',
      soundEnableRequired: 'Pilih apakah aktifkan suara',
      soundEnableHint: 'Hanya efektif jika kirim ke layar diaktifkan',
      sendYkEnable: 'Kirim ke Yingke',
      sendYkEnableRequired: 'Pilih apakah kirim ke Yingke',
      sendYkEnableHint: 'Jika diaktifkan, kirim ke MES via Yingke'
    },
    confirm: {
      title: 'Konfirmasi',
      delete: 'Hapus cacat ini?'
    },
    apiMsg: {
      addSuccess: 'Berhasil ditambah',
      addFailed: 'Gagal menambah',
      addError: 'Kesalahan tambah',
      editSuccess: 'Berhasil diperbarui',
      editFailed: 'Gagal memperbarui',
      editError: 'Kesalahan perbarui',
      deleteSuccess: 'Berhasil dihapus',
      deleteFailed: 'Gagal menghapus',
      deleteError: 'Kesalahan hapus'
    },
    list: {
      loadFailed: 'Gagal memuat daftar cacat',
      empty: 'Tidak ada konfigurasi cacat'
    }
  },

  defect: {
    title: 'Penanganan Cacat',
    subtitle: 'Klasifikasi, tinjau, dan tutup cacat',
    newDefect: 'Cacat baru',
    pendingReview: 'Menunggu tinjauan',
    reviewing: 'Ditinjau',
    closed: 'Ditutup',
    type: 'Jenis',
    severity: 'Tingkat',
    image: 'Gambar',
    location: 'Lokasi',
    createTime: 'Dibuat',
    operator: 'Operator',
    review: 'Tinjau',
    close: 'Tutup',
    placeholder: '🚧 Penyelarasan bisnis (subtugas E)',
    // ===== W-FRONT-02-E3 additions =====
    kpi: {
      total: 'Total Cacat Hari Ini',
      severe: 'Cacat Serius',
      handled: 'Ditangani',
      missRate: 'Tingkat Miss'
    },
    filter: {
      date: 'Tanggal',
      line: 'Lanes',
      type: 'Tipe Cacat',
      level: 'Tingkat',
      query: 'Cari',
      reset: 'Reset'
    },
    level: {
      severe: 'Serius',
      normal: 'Normal'
    },
    status: {
      pending: 'Tertunda',
      handled: 'Ditangani'
    },
    table: {
      id: 'ID Catatan',
      time: 'Waktu',
      line: 'Lanes',
      type: 'Tipe Cacat',
      level: 'Tingkat',
      image: 'Contoh',
      status: 'Status',
      handler: 'Penangan',
      action: 'Aksi',
      detail: 'Detail',
      handle: 'Tangani'
    },
    detail: {
      title: 'Detail Cacat',
      detectTime: 'Waktu Deteksi',
      handleTime: 'Waktu Tangani',
      operator: 'Operator',
      handler: 'Penangan',
      remark: 'Catatan',
      save: 'Simpan',
      close: 'Tutup',
      noImage: 'Tidak ada gambar contoh',
      saved: 'Catatan disimpan'
    },
    trend: {
      title: 'Tren 7 Hari',
      defectCount: 'Jumlah Cacat',
      noData: 'Tidak ada data tren'
    }
  },

  account: {
    title: 'Manajemen Akun',
    subtitle: 'Kelola akun masuk, peran, dan reset kata sandi',
    name: 'Akun',
    displayName: 'Nama tampilan',
    role: 'Peran',
    status: 'Status',
    active: 'Aktif',
    disabled: 'Nonaktif',
    lastLogin: 'Login terakhir',
    resetPassword: 'Reset kata sandi',
    changeRole: 'Ubah peran',
    placeholder: '🚧 Penyelarasan bisnis (subtugas E)',

    // Kartu pengguna saat ini
    current: {
      title: 'Pengguna Saat Ini',
      username: 'Nama Pengguna',
      role: 'Peran',
      permission: 'Hak Akses',
      lastLogin: 'Login Terakhir',
      changePwd: 'Ubah Sandi'
    },

    // Kolom tabel
    table: {
      id: 'ID',
      username: 'Nama Pengguna',
      role: 'Peran',
      permission: 'Hak Akses',
      realName: 'Nama Lengkap',
      contactInfo: 'Kontak',
      createdAt: 'Dibuat',
      status: 'Status',
      action: 'Aksi'
    },

    // Judul dialog
    add: {
      title: 'Tambah Akun'
    },
    edit: {
      title: 'Edit Akun'
    },

    // Field formulir
    form: {
      username: 'Nama Pengguna',
      role: 'Peran',
      realName: 'Nama Lengkap',
      contactInfo: 'Kontak',
      rolePlaceholder: 'Pilih peran'
    },

    // Dialog ubah sandi
    pwd: {
      title: 'Ubah Sandi',
      old: 'Sandi Lama',
      new: 'Sandi Baru',
      confirm: 'Konfirmasi Sandi',
      oldPlaceholder: 'Masukkan sandi saat ini',
      newPlaceholder: 'Minimal 6 karakter',
      confirmPlaceholder: 'Masukkan ulang sandi baru',
      mismatch: 'Sandi baru tidak cocok',
      wrongOld: 'Sandi lama salah',
      success: 'Sandi berhasil diubah'
    },

    // Tombol aksi
    action: {
      add: 'Tambah Akun',
      edit: 'Edit',
      resetPwd: 'Reset',
      delete: 'Hapus'
    },

    // Konfirmasi
    confirm: {
      delete: 'Hapus akun "{name}"? Tindakan ini tidak dapat dibatalkan.',
      reset: 'Reset sandi "{name}"? Sandi akan kembali ke default sistem.'
    },

    // Kosong / error
    empty: 'Tidak ada akun',
    loadFailed: 'Gagal memuat akun',
    deleteFailed: 'Hapus gagal',
    saveFailed: 'Simpan gagal',
    noRole: 'Tidak ada peran',

    defaultPwdNote: 'Catatan: Setelah reset, sandi default ditentukan oleh backend; hubungi admin untuk sandi baru.'
  },

  systemConfig: {
    title: 'Konfigurasi Sistem',
    subtitle: 'Strategi sampling, ambang alarm, dan parameter sistem',
    groupCollect: 'Akuisisi',
    groupAlarm: 'Ambang alarm',
    groupGeneral: 'Umum',
    interval: 'Interval sampling',
    retention: 'Retensi data (hari)',
    maxAlarm: 'Alarm bersamaan maks',
    theme: 'Tema',
    placeholder: '🚧 Penyelarasan bisnis (subtugas E)'
  },

  // W-FRONT-02-E5 halaman konfigurasi sistem bisnis (ditambahkan)
  config: {
    title: 'Konfigurasi Sistem',
    subtitle: 'Parameter sistem / Konfigurasi lanes / Pemetaan tipe cacat',
    tab: {
      system: 'Parameter Sistem',
      line: 'Konfigurasi Lanes',
      defectType: 'Pemetaan Tipe Cacat'
    },
    form: {
      alarmSoundDevice: 'Audio alarm perangkat',
      alarmSoundDefect: 'Audio alarm cacat',
      alarmSoundSystem: 'Audio alarm sistem',
      soundPlayCount: 'Jumlah pengulangan suara',
      save: 'Simpan',
      saving: 'Menyimpan…',
      saveOk: 'Berhasil disimpan',
      saveFail: 'Gagal menyimpan',
      loadOk: '{n} item konfigurasi dimuat',
      loadEmpty: 'Belum ada konfigurasi',
      uploadTip: 'Unggah file audio MP3, nama ≤ 60 karakter, maks 200MB per file',
      playCountTip: 'Rentang 1-10'
    },
    line: {
      title: 'Daftar lanes',
      name: 'Nama lane',
      code: 'Kode',
      faceNo: 'Sisi',
      color: 'Warna',
      desc: 'Deskripsi',
      add: 'Tambah lane',
      edit: 'Ubah lane',
      addTitle: 'Tambah lane',
      editTitle: 'Ubah lane',
      confirmDelete: 'Hapus lane "{name}"?',
      required: 'Nama / Kode / Sisi wajib diisi',
      empty: 'Belum ada lane',
      createdOk: 'Berhasil ditambah',
      updatedOk: 'Berhasil diubah',
      deletedOk: 'Berhasil dihapus'
    },
    defectType: {
      title: 'Daftar tipe cacat',
      name: 'Nama tipe',
      level: 'Level',
      enabled: 'Aktif',
      disabled: 'Nonaktif',
      line: 'Lane',
      add: 'Tambah tipe cacat',
      edit: 'Ubah tipe cacat',
      addTitle: 'Tambah tipe cacat',
      editTitle: 'Ubah tipe cacat',
      confirmDelete: 'Hapus tipe cacat "{name}"?',
      required: 'Nama / Level / Lane / Sisi wajib diisi',
      empty: 'Belum ada tipe cacat',
      createdOk: 'Berhasil ditambah',
      updatedOk: 'Berhasil diubah',
      deletedOk: 'Berhasil dihapus'
    },
    action: {
      add: 'Tambah',
      edit: 'Ubah',
      delete: 'Hapus',
      save: 'Simpan',
      cancel: 'Batal',
      enabled: 'Aktifkan',
      disabled: 'Nonaktifkan',
      refresh: 'Segarkan'
    },

    // W-FRONT-02-E5 placeholder kunci brief
    briefKeys: {
      alarmSound: 'Suara Alarm',
      alarmRetainDays: 'Hari Retensi Alarm',
      syncInterval: 'Interval Sync (s)',
      screenRefresh: 'Refresh Layar (s)',
      defaultLang: 'Bahasa Default',
      formSave: 'Simpan',
      lineName: 'Nama Lanes',
      lineCode: 'Kode',
      lineDesc: 'Deskripsi',
      defectTypeName: 'Nama Tipe',
      defectTypeLevel: 'Level',
      defectTypeEnabled: 'Diaktifkan',
      actionAdd: 'Tambah',
      actionEdit: 'Ubah',
      actionDelete: 'Hapus'
    }
  },

  log: {
    title: 'Log Operasi',
    subtitle: 'Audit setiap operasi pengguna',
    user: 'Pengguna',
    action: 'Aksi',
    module: 'Modul',
    ip: 'IP',
    time: 'Waktu',
    result: 'Hasil',
    success: 'Berhasil',
    failure: 'Gagal',
    placeholder: '🚧 Penyelarasan bisnis (subtugas E)',

    // ===== W-FRONT-02-E6 additions =====
    filter: {
      operator: 'Operator',
      operation: 'Operasi',
      module: 'Modul',
      ip: 'IP',
      result: 'Hasil',
      allResult: 'Semua hasil',
      successOnly: 'Berhasil',
      failureOnly: 'Gagal',
      timeRange: 'Rentang waktu',
      startTime: 'Waktu mulai',
      endTime: 'Waktu akhir',
      query: 'Cari',
      reset: 'Reset',
      expandMore: 'Filter lainnya'
    },
    table: {
      index: '#',
      operator: 'Operator',
      module: 'Modul',
      operation: 'Operasi',
      ip: 'IP',
      uri: 'Path request',
      cost: 'Durasi',
      result: 'Hasil',
      createTime: 'Waktu panggil',
      updateTime: 'Waktu selesai',
      action: 'Aksi',
      view: 'Lihat',
      longTimeWarning: 'Durasi > 1s',
      unitMs: 'ms'
    },
    detail: {
      title: 'Detail Log',
      basicInfo: 'Info Dasar',
      requestAddress: 'IP Klien',
      requestPath: 'Path Request',
      requestCost: 'Durasi',
      operator: 'Operator',
      module: 'Modul',
      operation: 'Operasi',
      callTime: 'Waktu Panggil',
      completeTime: 'Waktu Selesai',
      requestParam: 'Body Request',
      responseData: 'Body Respons',
      none: '(kosong)',
      copy: 'Salin',
      copied: 'Disalin',
      copyFail: 'Gagal menyalin',
      invalidJson: 'Bukan JSON, tampilkan teks asli'
    },
    list: {
      empty: 'Tidak ada log operasi',
      loadFailed: 'Gagal memuat log',
      networkError: 'Kesalahan jaringan, coba lagi',
      longTimeThreshold: 'Request > 1s disorot merah'
    }
  },

  userManage: {
    title: 'Manajemen Pengguna',
    subtitle: 'Kelola profil, izin, dan peran pengguna',
    realName: 'Nama lengkap',
    department: 'Departemen',
    email: 'Email',
    phone: 'Telepon',
    createTime: 'Dibuat',
    placeholder: '🚧 Penyelarasan bisnis (subtugas E)'
  },

  // W-FRONT-02-E7 halaman manajemen pengguna (ditambahkan)
  user: {
    title: 'Manajemen Pengguna',
    subtitle: 'Manajemen profil operator bengkel',
    kpi: {
      total: 'Total Operator',
      online: 'Operator Online',
      newToday: 'Baru Hari Ini'
    },
    filter: {
      name: 'Nama',
      workNo: 'No. Kerja',
      shift: 'Shift',
      line: 'Lane'
    },
    table: {
      workNo: 'No. Kerja',
      name: 'Nama',
      shift: 'Shift',
      line: 'Lane',
      phone: 'Telepon',
      hireDate: 'Tgl Masuk',
      status: 'Status',
      detail: 'Detail',
      edit: 'Ubah',
      resign: 'Resign'
    },
    detail: {
      title: 'Detail Profil',
      historyTitle: 'Riwayat Operasi',
      noHistory: 'Tidak ada catatan operasi',
      empty: 'Tidak ada data operator',
      loadFailed: 'Gagal memuat',
      // ===== W-PERF-F additions: graceful degradation when log/list fails =====
      logUnavailable: 'Riwayat operasi sementara tidak tersedia',
      noLogs: 'Tidak ada catatan operasi'
    },
    status: {
      active: 'Aktif',
      resigned: 'Resign'
    }
  },

  // Layar Besar (W-FRONT-02-E8)
  screen: {
    title: 'Layar Besar',
    subtitle: 'Dashboard visualisasi bengkel',
    enterFullscreen: 'Masuk layar besar',
    exitFullscreen: 'Keluar layar besar',
    exit: 'Keluar',
    refresh: 'Segarkan',
    placeholder: '🚧 Penyelarasan bisnis (subtugas E)',

    header: {
      connection: 'Realtime',
      connected: 'Terhubung',
      connecting: 'Menghubungkan',
      disconnected: 'Terputus',
      lastUpdate: 'Diperbarui',
      fullscreen: 'Layar penuh',
      exitFullscreen: 'Keluar layar penuh',
      back: 'Kembali'
    },

    kpi: {
      onlineLines: 'Line online',
      todayOutput: 'Output hari ini',
      todayDefect: 'Cacat hari ini',
      todayAlarm: 'Alarm hari ini',
      unit: 'pcs'
    },

    chart: {
      trend: 'Tren realtime',
      defectPie: 'Distribusi cacat',
      alarmList: 'Alarm terbaru',
      lineGrid: 'Status line',
      trendSub: 'Rencana / Aktual / Cacat — 2 jam terakhir',
      pieSub: 'Agregat per tipe cacat (hari ini)',
      alarmSub: '10 alarm terbaru hari ini',
      gridSub: 'Perangkat / output / progres'
    },

    ticker: {
      title: 'Siaran langsung',
      noData: 'Tidak ada alarm baru'
    },

    alarm: {
      time: 'Waktu',
      line: 'Line',
      face: 'Sisi',
      type: 'Tipe',
      level: 'Level',
      message: 'Pesan',
      status: 'Status',
      noData: 'Tidak ada alarm',
      typeDefect: 'Cacat',
      typeSystem: 'Sistem',
      typeDevice: 'Perangkat',
      levelNormal: 'Normal',
      levelSerious: 'Serius',
      statusPending: 'Belum',
      statusHandled: 'Selesai',
      statusIgnored: 'Abaikan'
    },

    line: {
      output: 'Output',
      defect: 'Cacat',
      progress: 'Progres',
      online: 'Online',
      offline: 'Offline',
      idle: 'Diam',
      down: 'Gangguan',
      running: 'Berjalan'
    },

    error: {
      loadFailed: 'Gagal memuat data layar besar',
      empty: 'Tidak ada data'
    }
  },

  role: {
    super_admin: 'Super Admin',
    admin: 'Admin',
    operator: 'Operator',
    viewer: 'Pengamat'
  },
  permission: {
    realtime: 'Realtime',
    alarm: 'Alarm',
    defect: 'Cacat',
    account: 'Akun',
    systemConfig: 'Konfigurasi',
    log: 'Log',
    userManage: 'Pengguna',
    screen: 'Layar Besar'
  }
}

// ---------------------------------------------------------------------------
// 创建 i18n 实例
// ---------------------------------------------------------------------------
const stored = typeof localStorage !== 'undefined'
  ? localStorage.getItem('app.locale')
  : null

const initialLocale: AppLocale =
  stored === 'zh-CN' || stored === 'en-US' || stored === 'id-ID'
    ? stored
    : 'zh-CN'

const i18n = createI18n({
  legacy: false,
  locale: initialLocale,
  fallbackLocale: 'en-US',
  messages: {
    'zh-CN': zhCN,
    'en-US': enUS,
    'id-ID': idID
  }
})

export default i18n
