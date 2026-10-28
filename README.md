# 智能校园停车场管理系统（Smart Campus Parking Lot Management System）

一个完整的智慧校园停车场管理平台，包含**后端服务**、**管理员前端**和**用户微信小程序**三大模块，实现车辆进出管理、车位预约、在线支付、月卡/长租、校园身份核验、第三方校园系统对接、运营数据统计与自动化任务等核心功能。

---

## 目录

- [项目架构](#项目架构)
- [技术栈](#技术栈)
- [功能模块](#功能模块)
  - [管理员后台](#管理员后台)
  - [用户小程序](#用户小程序)
- [自动化任务与外部对接](#自动化任务与外部对接)
- [项目结构](#项目结构)
- [数据库设计](#数据库设计)
- [API 接口文档](#api-接口文档)
- [快速开始](#快速开始)
  - [环境要求](#环境要求)
  - [后端启动](#后端启动)
  - [管理后台启动](#管理后台启动)
  - [微信小程序启动](#微信小程序启动)
- [配置说明](#配置说明)
- [部署指南](#部署指南)
- [开发说明](#开发说明)

---

## 项目架构

```
┌─────────────────────────────────────────────────┐
│                   用户层                          │
│  ┌──────────────┐  ┌──────────────────────────┐ │
│  │  管理员后台    │  │   微信小程序（用户端）      │ │
│  │  Vue 3 + TS   │  │   原生微信小程序           │ │
│  │  + Element Plus│  │                          │ │
│  └──────┬───────┘  └────────────┬─────────────┘ │
└─────────┼────────────────────────┼───────────────┘
          │        HTTP/REST       │
          └───────────┬────────────┘
┌─────────────────────┴───────────────────────────┐
│                后端服务层                          │
│  ┌──────────────────────────────────────────┐   │
│  │  Spring Boot 3.3 + Java 21（单进程单端口） │   │
│  │  ├─ admin      (管理端 REST API)          │   │
│  │  ├─ miniapp    (小程序端 REST API)        │   │
│  │  ├─ system     (共享 Service / Mapper)    │   │
│  │  ├─ framework  (拦截器 / 配置 / 日志)      │   │
│  │  └─ common     (R / 异常 / 工具 / 上下文)  │   │
│  └──────────────────────────────────────────┘   │
└─────────────────────┬───────────────────────────┘
          ┌───────────┴───────────┐
┌─────────┴─────────┐  ┌─────────┴─────────┐
│    MySQL 8.0      │  │    Redis          │
│   (业务数据)       │  │  (缓存 / 验证码)   │
└───────────────────┘  └───────────────────┘
```

---

## 技术栈

### 后端（parking-server）

| 技术 | 版本 | 说明 |
|------|------|------|
| JDK | 21 | Java 运行环境 |
| Spring Boot | 3.3.0 | 核心框架 |
| MyBatis-Plus | 3.5.7 | ORM 框架（MyBatis 增强） |
| Spring Security | 6.x | 安全框架（密码加密） |
| Spring WebSocket | 6.x | 车位状态实时推送（原生 WebSocket，`/ws/spots`） |
| JWT (jjwt) | 0.12.6 | 无状态身份认证 |
| Knife4j | 4.5.0 | API 文档（Swagger 增强） |
| Redis | — | 缓存 & 验证码存储 |
| MySQL | 8.0+ | 关系型数据库 |
| Hutool | 5.8.28 | Java 工具集 |
| Easy Captcha | 1.6.2 | 图形验证码 |
| Lombok | 1.18.36 | 代码简化 |

### 管理后台（parking-admin）

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.5+ | 前端框架（组合式 API + `<script setup>`） |
| TypeScript | 5.4+ | 类型安全 |
| Vite | 5.2+ | 构建工具 |
| Vue Router | 4.4+ | 路由管理（含登录守卫与路由级懒加载） |
| Pinia | 2.2+ | 状态管理 |
| Element Plus | 2.14+ | UI 组件库（浅色主题，`styles/global.css` 定制主题变量） |
| ECharts | 5.5+ | 数据可视化 |
| Axios | 1.7+ | HTTP 请求 |
| Day.js | 1.11+ | 日期处理 |
| Sass | 1.72+ | CSS 预处理 |

### 用户端（parking-miniapp）

> 用户端展示名称：**用户校园停车小程序**（小程序导航栏标题、登录页与首页标题均使用此名称；管理后台仍为「智能校园停车场管理系统」）。

| 技术 | 说明 |
|------|------|
| 原生微信小程序框架 | 无需额外依赖，即开即用 |
| 微信 API | wx.request / wx.getStorageSync 等 |

---

## 功能模块

### 管理员后台

| 模块 | 功能 |
|------|------|
| **首页** | 核心数据看板：总车位数、空闲/占用/预约数量、今日收入、今日预约等统计指标；车位地图、校园身份/院系/分区统计、最近违规。统计与车位地图带 5 秒短缓存、校园统计带 30 秒短缓存，降低多管理员同时刷新时的数据库压力。**车位状态通过 WebSocket 实时推送**（业务变更 1 秒内更新地图与空闲数），断线自动回退为 15 秒轮询 |
| **用户管理** | 查看注册用户列表（头像/昵称/**校园身份**/学号工号/手机号/车牌/余额/状态）、按**身份类型与认证状态**筛选、搜索、启用/禁用账号；**身份认证审核**：学生/教师/职工提交学号/工号后，系统优先调用第三方校园管理系统自动核验，**核验通过直接认证**，查不到或接口不可用时才转为待审核由管理员处理；列表提供「身份审核 / 复核·驳回」入口（通过认证 / 驳回 / 重置为待审核 + 审核意见）；侧边栏「用户管理」显示待审核数量徽章；**用户信息抽屉**展示账号资料、累计支出与月卡信息 |
| **访客与家长报备** | 师生提交的访客/家长车辆报备审核：通过（自动分配访客与家长临时停车区车位并创建访客账号）、驳回、**登记入场**、**出场结算**（按身份免费时长与费率），以及报备统计（待审核/已通过/在停/访客区空闲车位） |
| **违规记录** | 出场超时自动生成的违规记录（超时占用时长、扣分），支持筛选、处理与处理意见 |
| **车位管理** | 车位列表（分页，含**校园分区 / 可停身份 / 区域免费时长**列）、新增/编辑车位（所属区域下拉从 `t_parking_zone` 动态加载）、修改车位状态（空闲/占用/已预约/维护中）、可视化车位地图（**点击车位卡片展开详情**：车位信息、在停车牌与入场时间/已停时长/预估费用/车主、进行中预约信息，并支持强制出场结算） |
| **停车区域管理** | 维护校园停车区域（区域编码、名称、说明、排序、启停），车位通过 `zone_code` 归属区域。区域编码创建后不可修改；区域下仍有车位时禁止删除；系统内置潮汐区（tidal）禁止删除与停用，避免用户端潮汐车位整体不可见 |
| **预约管理** | 查看所有预约记录、按状态筛选、手动修改预约状态 |
| **车辆进出** | 查看停车记录（入场/离场时间、停车时长、费用）、手动编辑记录、强制出场操作 |
| **充值记录** | 查看用户充值明细（金额、支付方式、状态、时间） |
| **扣费记录** | 查看停车扣费明细（扣费金额、余额变动、类型：停车/预约结算/月卡开通） |
| **月卡管理** | 月卡/季卡/年卡经营视图：顶部经营概览（生效中、7 天内到期、已过期、已退款、累计/本月营收），支持按关键词（车牌/昵称/手机号/学号工号）、状态、卡类型筛选；明细列表与详情弹窗展示生效车牌、有效期、实付金额、剩余天数；用户详情抽屉同步展示月卡与累计支出 |
| **用户角色** | 小程序用户端 RBAC：角色 = 校园身份（学生/教师/职工/访客/家长），权限 = 可见停车区域。支持修改角色名称/描述、为角色分配可见区域；用户注册、身份变更时按身份自动映射角色，后台可再微调单个用户所属角色 |
| **校园统计与报表** | 首页展示：校园身份分布（学生/教师/职工/访客/家长环形图）、校园分区车位使用（堆叠柱状图）、校园管理概览（已认证/待审核身份、待审报备、待处理违规、黑名单数、累计营收）、**院系统计表**（用户数/认证数/预约次数/停车次数/消费金额）、分区车位与收入表；支持一键**导出 CSV 报表**（用户/预约/扣费/违规/访客报备，含 UTF-8 BOM，Excel 直接打开） |
| **黑白名单** | **黑名单**：按车牌或用户加入，支持永久/限期、手动/违规自动来源，命中后禁止预约、入场与访客报备；**白名单**：豁免身份区域、假期模式、夜间禁停与每日预约上限，访客报备自动通过（费用照常）；**黑名单优先于白名单**。支持状态（生效中/已过期）与来源筛选、详情查看、一键移出；违规累计达到阈值时自动拉黑（白名单车辆除外） |
| **学院管理** | 学院/部门维护：新增、编辑（名称/学院代码/联系人/联系电话/排序/备注）、启用停用、删除（学院下仍有用户时禁止删除）；**改名时自动同步该学院下用户的院系名称**；停用后不再出现在小程序注册与账号信息页的下拉列表。学生、教师、职工注册与修改资料时**必须从启用的学院列表中选择**，后端会校验 |
| **潮汐车位** | 面向开学迎新、考试、比赛等高峰期的**临时车位批次**管理：新建批次即按「编号前缀 + 数量」批量生成临时车位，并设定**精确到分钟的开放时间段**（`YYYY-MM-DD HH:mm`）。列表展示批次名称/区域/编号前缀/车位数（总数·空闲·占用·预约）/单价/允许身份/开放时段/当前阶段（待开放·开放中·已结束·已取消）；支持查看批次内车位明细、**修改（含增减车位数、延长或提前结束时间）**、取消批次、删除批次。开放前 N 天（默认 7 天，可在系统配置中调整 `tidal_reserve_advance_days`）用户端即可见并可预约；到达开放时间后自动可入场；批次结束后其空闲车位自动停用 |
| **公告管理** | 发布面向全校的通知（放假安排、施工绕行、停车规则调整等），支持**携带 1 个附件**（PDF / Word / Excel / PPT / 文本 / 压缩包 / 图片，单个 ≤ 20MB）。列表含标题、状态（草稿/已发布）、附件（文件名 + 大小 + 下载）、发布人、发布时间、浏览量；支持关键字与状态筛选、**发布/下架**、编辑（可替换或移除附件）、删除（一并删除附件文件）。附件保存在项目内 `parking-server/uploads/announcement/` 目录，磁盘文件名为雪花 ID（纯 ASCII），原始文件名单独存库并在下载时通过 `Content-Disposition` 还原 |
| **账号与权限** | 完整 RBAC：`t_role` 维护角色（内置 `super_admin` / `admin`，编码创建后不可修改），`t_role_menu` 维护角色可访问菜单；管理员账号绑定角色编码，不再直接持有菜单。超级管理员拥有全部权限，普通管理员只看到并只能访问被授权的菜单；前端不渲染未授权菜单，后端由菜单权限拦截器按「请求路径 → 菜单编码」映射统一拦截并在角色变更后清理权限缓存 |
| **登录日志 / 操作日志** | **登录日志**记录管理员每次登录的时间、IP、浏览器与结果；**操作日志**由自定义注解 + AOP 环绕切面自动采集敏感操作（操作人、菜单、操作类型、请求 URI、IP、参数、响应状态、耗时），在 finally 块中独立落库，采集失败不影响业务。两个页面均支持按关键字与时间筛选，并可查看单条详情 |
| **个人中心** | 任意管理员均可访问（不受菜单授权限制）：查看本人账号信息、修改昵称；修改密码需校验原密码，避免令牌被盗后直接篡改登录凭据 |
| **硬件监控** | 服务器与 Redis 运行状态监控：CPU / 内存 / 磁盘使用率、JVM 堆内存、数据库与 Redis 连接池、接口耗时统计，用于部署后的运行观测 |
| **定时任务** | 动态定时任务管理：列表展示任务名称、分组、执行器、Cron、状态、上次执行时间与结果；支持**新增 / 编辑 / 删除**、**启用 / 暂停**、**立即执行一次**。内置预约超时释放、潮汐批次同步、车位缓存对账、月卡到期流转四个任务；执行器为后端白名单，Cron 采用「秒 分 时 日 月 周」六位格式，修改后即时重新调度；同一任务上次未执行完时自动跳过本次触发 |
| **系统配置** | 动态系统参数配置（键值对形式）、支持在线修改；**校园规则**（学生折扣、教师/职工/家长/访客免费时长、访客加价系数、每日预约上限、夜间禁停时段、学期/假期模式、假期折扣、超时违规宽限期、**潮汐车位提前预约天数**）均在此在线调整；**月卡/季卡/年卡价格**与**微信订阅消息模板 ID**（预约提醒、余额不足、身份审核结果、月卡到期提醒）也在此动态维护；保存时校验取值合法性，读取时做兜底限制 |

### 用户小程序

| 页面 | 功能 |
|------|------|
| **首页** | 实时概览：当前停放的车辆信息（车牌、已停时长）、活跃预约一览、各区域车位状态（空闲/占用/预约）、**校园余位环**（空闲占比，>50% 绿 / 20%-50% 橙 / <20% 红）；有可用潮汐车位时显示入口卡片；**车位状态通过 WebSocket 实时推送**（入场/出场/管理员操作 1 秒内更新），连接正常时自动跳过 5 秒概览轮询，断线后自动恢复轮询兜底 |
| **车位列表** | 按校园分区列出全部车位及其状态（空闲/占用/已预约/维护中）、单价与允许停放身份，可按区域筛选，点击空闲车位直接进入预约；支持关键字模糊搜索（车位号/区域/分区名）并自动展开匹配区域；页面内车位状态由 WebSocket 实时更新 |
| **登录** | 微信一键登录（获取 openid）、手机号注册/绑定、账号密码登录 |
| **预约车位** | 选择车位 → 选择开始日期时间（**仅可查看/选择当前时间之后的时段**：今天只列出「当前时间 + 10 分钟」起的时段；若今天已无可用时段则自动定位到明天）→ 选择时长（1小时~1个月）→ 自动计算费用 → 确认预约 |
| **预约详情** | 查看单条预约的完整信息（预约编号、车位号、时间段、金额、状态） |
| **入场登记** | 选择已预约车位快速入场，系统自动记录入场时间 |
| **出场结算** | 查看当前停车详情、实时计算停车费用、一键出场并自动从余额扣费 |
| **余额充值** | 展示充值套餐（50/100/200/500/1000元）、在线充值（模拟支付） |
| **停车记录** | 停车历史记录列表（车牌、入场/出场时间、时长、费用、支付状态） |
| **账号信息** | 个人资料管理：更换/移除头像、修改昵称、手机号、车牌号；**校园身份**（学生/教师/职工/访客/家长 + 学号工号 + 院系班级）：提交后优先由校园管理系统自动核验，通过即认证，查不到或接口不可用时转管理员审核；**我的校园权益**（计费身份、费率系数、免费停车时长、今日预约剩余次数、夜间禁停时段、学期/假期模式）；**绑定/解绑微信** |
| **月卡 / 长租** | 查看当前生效月卡（卡类型、到期日、车牌、剩余天数）与历史记录；按月卡/季卡/年卡开通或续费（余额扣款并写入扣费记录）。月卡仅豁免按次停车费，预约费不豁免；生效需同时匹配用户与车牌。页面提供「开启到期提醒」，用户点击后授权微信订阅消息 |
| **微信订阅消息** | 四类通知：预约生效前 15 分钟提醒、余额不足提醒、身份审核结果通知、月卡到期前 7/3/1 天提醒。模板 ID 存储在系统配置中；授权必须在用户点击时上报，未授权或模板未配置时静默跳过 |
| **我的（个人中心）** | 用户信息展示（头像、昵称、余额、车牌、校园身份与认证状态）、当前角色与可见停车区域提示；快捷入口（我的预约、月卡、充值、扣费、停车记录、我的违规、访客报备等），余额每 10 秒静默同步 |
| **访客/家长报备** | 校内师生代来访家长或访客提前报备车牌与到校时段，审核通过后自动分配访客区车位；可查看报备状态（待审核/已通过/已入场/已完成）与撤销报备 |
| **我的预约** | 预约列表（车位编号与区域、预约时段、时长、金额、状态），支持查看详情、去入场、**取消预约**（自动释放车位） |
| **我的违规** | 个人违规记录（超时占用 / 违规区域等类型、扣分、说明、处理状态），并提示累计违规可能被列入车辆黑名单 |
| **潮汐车位** | 高峰期临时车位专区：列出开放前 7 天至结束时间之间的临时车位批次，显示阶段（开放前·仅可预约 / 开放中·可入场）、开放时段、单价、允许身份、车位格子与空闲数。**开放前只能预约、不能入场**；**到达开放时间后该批车位自动出现在预约页与入场页，可直接入场停车**；点车位或「去预约」会跳到预约页并把可预约时段自动收敛到该批次的开放时间段内；批次内车位状态由 WebSocket 实时更新 |
| **公告**（底部导航第 2 个 tab，位于「首页」与「我的」之间） | 展示管理员**已发布**的公告列表（标题、发布时间、正文摘要、附件标记），支持下拉刷新、上拉加载更多；点进详情可阅读完整正文（保留换行分段）与发布人/发布时间/浏览量。**附件可点击浏览与下载**：图片类直接预览（可长按保存/转发），PDF/Word/Excel/PPT 等下载后调 `wx.openDocument` 打开，不支持打开的类型（如 zip/rar）提示并支持一键复制下载链接 |
| **修改密码** | 校验原密码后设置新密码（6-20 位），成功后需重新登录 |

---

## 自动化任务与外部对接

### 定时任务

| 任务 | 频率 | 说明 |
|------|------|------|
| 预约过期释放 | 每 60 秒 | 扫描超过结束时间 + 宽限期（默认 15 分钟）的活跃预约，置为已过期并释放车位 |
| 潮汐批次状态同步 | 每 60 秒 | 待开放 → 开放中 → 已结束；已结束批次中仍空闲的车位自动停用 |
| Redis 车位缓存对账 | 每 30 秒 | 比对 Redis 与 MySQL 车位状态，以 MySQL 为准修复缓存漂移 |
| 预约前提醒扫描 | 每分钟 | 向已授权的用户发送「预约生效前 15 分钟」订阅消息 |
| 月卡到期状态流转 | 每日 00:05 | 把 `expire_date < 今天` 且仍为「生效中」的月卡批量置为「已过期」 |
| 月卡到期提醒 | 每日 09:00 | 对到期前 7 / 3 / 1 天的生效月卡，按用户授权发送续费提醒 |

### 校园身份自动核验（出站调用）

学生/教师/职工注册或提交学号/工号时，后端调用第三方校园管理系统的核验接口：

- 配置项：`parking.campus-verify.url / app-id / secret / timeout-ms`（环境变量 `CAMPUS_VERIFY_URL` 等）。
- 请求：`POST`，JSON `{"userNo":"S2024001","identityType":1}`，签名头 `X-Campus-App-Id / Timestamp / Nonce / Signature`，签名原文 `appId\ntimestamp\nnonce\nbody`。
- 核验通过：直接置为「已认证」，并同步校园系统返回的院系/班级；返回的学号与身份类型必须与提交一致，防止冒用身份。
- 查无此人或接口不可用：保持「待审核」，转入原管理员审核流程，功能不中断。
- 已认证用户仅修改院系/班级等展示信息时不再重复调用；成功核验结果缓存 5 分钟。

### 第三方校园系统单点登录（入站接口）

`POST /api/open/campus/sso/login`，供校园管理系统服务端调用，免去用户二次身份认证：

- 使用预共享 `appId + secret` 做 HMAC-SHA256 签名鉴权，时间戳允许 ±5 分钟，nonce 通过 Redis 防重放。
- 按学号/工号查找或创建账号，同步身份并直接标记「已认证」，返回本系统 JWT 与用户信息。
- 配置项：`parking.campus-sso.app-id / secret`（环境变量 `CAMPUS_SSO_APP_ID` / `CAMPUS_SSO_SECRET`）；未配置时接口返回 503。
- 原有注册、手机号登录、微信登录与管理员审核流程全部保留。

---

## 项目结构

```
parking-system/
├── docs/
│   └── parking_system.sql         # 数据库全量导出（20 张表 + 索引 + 演示数据）
│                                  # 注意：该文件是本机运行库导出，含真实账号/日志/IP 等数据，
│                                  # 已写入 .gitignore，不随仓库提交，仅在本地保留
├── parking-server/                # 后端服务（Spring Boot）
│   ├── pom.xml                    # Maven 依赖配置
│   └── src/main/
│       ├── java/com/parking/
│       │   ├── ParkingApplication.java        # 唯一启动类（管理端与小程序端共用，端口 8080）
│       │   ├── admin/                         # 【管理端后端】仅管理后台调用
│       │   │   ├── controller/                # /api/admin/** + 管理员登录
│       │   │   │   ├── AdminAuthController.java         # 验证码 + 管理员登录
│       │   │   │   ├── AdminController.java             # 用户/预约/车辆/车位/统计/配置
│       │   │   │   ├── AdminAccountController.java      # 管理员账号与菜单权限
│       │   │   │   ├── AdminLoginLogController.java     # 登录日志审计
│       │   │   │   ├── AdminOperationLogController.java # 操作日志审计
│       │   │   │   ├── AnnouncementAdminController.java # 公告管理
│       │   │   │   ├── ParkingZoneController.java       # 停车区域管理
│       │   │   │   ├── ProfileController.java           # 管理员个人中心
│       │   │   │   ├── RoleController.java              # 管理员角色 RBAC
│       │   │   │   ├── TidalAdminController.java        # 潮汐批次管理
│       │   │   │   ├── ScheduledJobController.java      # 定时任务增删改查/启停/立即执行
│       │   │   │   └── UserRoleController.java          # 小程序用户角色/可见区域
│       │   │   └── service/                   # 管理端专用业务接口 + impl
│       │   │       ├── AdminService.java                # 用户/预约/车辆/车位/统计/配置
│       │   │       ├── AdminAuthService.java            # 管理员登录与登录日志
│       │   │       ├── AdminAccountService.java         # 管理员账号与菜单权限
│       │   │       ├── CampusAdminService.java          # 校园统计/黑白名单/CSV 导出
│       │   │       ├── ParkingZoneService.java          # 停车区域
│       │   │       ├── ScheduledJobService.java         # 动态定时任务管理
│       │   │       └── RoleService.java                 # 管理员角色 RBAC
│       │   ├── miniapp/                       # 【小程序端后端】仅微信小程序调用
│       │   │   ├── controller/                # /api/auth、/api/user、/api/spot、/api/open
│       │   │   │   ├── AuthController.java              # 注册 / 手机号登录 / 微信登录
│       │   │   │   ├── UserController.java              # 个人信息/充值/记录/访客报备
│       │   │   │   ├── SpotController.java              # 车位列表/概览
│       │   │   │   ├── TidalSpotController.java         # 潮汐车位
│       │   │   │   ├── ReservationController.java       # 预约
│       │   │   │   ├── VehicleController.java           # 车辆进出
│       │   │   │   ├── MonthlyPassController.java       # 月卡/长租
│       │   │   │   ├── SubscribeController.java         # 微信订阅消息
│       │   │   │   ├── AnnouncementController.java      # 公告（公开）
│       │   │   │   ├── DepartmentController.java        # 学院/部门（公开）
│       │   │   │   └── CampusSsoController.java         # 第三方校园 SSO（签名鉴权）
│       │   │   └── service/                   # 小程序端专用业务接口 + impl
│       │   │       ├── AuthService.java                 # 注册 / 手机号登录 / 微信登录
│       │   │       ├── UserService.java                 # 个人信息/充值/记录
│       │   │       ├── SpotService.java                 # 车位列表/概览
│       │   │       ├── ReservationService.java          # 预约
│       │   │       ├── MonthlyPassService.java          # 月卡/长租
│       │   │       ├── CampusSsoService.java            # 校园单点登录
│       │   │       └── CampusVerifyService.java         # 校园身份核验
│       │   ├── system/                        # 【共享业务与数据层】两端复用
│       │   │   ├── domain/                    # 数据库实体（t_user、t_admin 等）
│       │   │   ├── dto/                       # 请求 / 响应 DTO
│       │   │   ├── mapper/                    # MyBatis-Plus Mapper
│       │   │   └── service/                   # 共享业务 + impl（车辆/报备/潮汐/公告/规则/RBAC/缓存…）
│       │   ├── framework/                     # 【框架层】参考若依 ruoyi-framework
│       │   │   ├── config/                    # Security / Web / Redis / MyBatisPlus / WebSocket
│       │   │   ├── interceptor/               # JWT 登录校验 + 后台菜单权限拦截
│       │   │   ├── aspect/                    # 操作日志切面
│       │   │   ├── scheduler/                 # 动态调度器 + 执行器白名单
│       │   │   └── websocket/                 # /ws/spots 车位状态实时推送
│       │   └── common/                        # 【通用层】参考若依 ruoyi-common
│       │       ├── R.java / ResultCode.java / BusinessException.java
│       │       ├── GlobalExceptionHandler.java / UserContext.java / IdentityType.java
│       │       ├── annotation/                # @OperationLog / OperationType
│       │       ├── menu/                      # 后台菜单权限映射
│       │       ├── user/                      # 用户可见区域模型
│       │       └── util/                      # JWT / 微信 / 校园签名工具
│       └── resources/
│           ├── application.yml               # 主配置文件
│           └── db/scheduled_job.sql          # 定时任务表与内置任务（启动时幂等初始化）
│   └── uploads/                    # 运行期上传目录（.gitignore 忽略内容，仅保留目录占位）
│       ├── avatar/                 # 用户头像
│       └── announcement/           # ★ 公告附件目录（管理员上传的公告附件存放在这里）
│
├── parking-admin/                  # 管理员前端（Vue 3 + TS + Element Plus）
│   ├── package.json                # Node 依赖
│   ├── vite.config.ts              # Vite 配置（Element Plus 按需引入 + /api 代理到 8080）
│   ├── index.html
│   └── src/
│       ├── App.vue                 # 根组件（el-config-provider 中文语言包 + router-view）
│       ├── main.ts                 # 入口（Pinia + Router + 图标注册，Element Plus 按需加载）
│       ├── api/
│       │   ├── index.ts            # 所有管理后台 API 封装
│       │   └── auth.ts             # 登录认证 API
│       ├── router/
│       │   └── index.ts            # 路由配置 + 登录守卫
│       ├── stores/
│       │   └── user.ts             # 用户状态（Pinia）
│       ├── utils/
│       │   ├── request.ts          # Axios 实例（拦截器、Token 注入）
│       │   └── duration.ts         # 时长格式化（与小程序同口径）
│       ├── hooks/
│       │   ├── autoRefresh.ts      # 自动刷新核心（纯 TS 轮询器）
│       │   ├── useAutoRefresh.ts   # Vue 3 组合式封装（标签页后台自动暂停）
│       │   └── useSpotSocket.ts    # 车位状态 WebSocket（单例 + 断线重连 + 心跳）
│       ├── styles/
│       │   └── global.css          # 全局浅色主题样式 + Element Plus 主题变量
│       ├── layouts/
│       │   └── MainLayout.vue      # 主布局（侧边栏 + 顶栏 + 路由出口）
│       ├── components/
│       │   ├── SpotMap.vue         # 车位地图可视化组件（卡片显示车牌 + 点击展开车位详情抽屉）
│       │   ├── AutoRefreshChip.vue # 自动刷新开关 + 最后更新时间
│       │   ├── LiveDuration.vue    # 已停时长实时走字
│       │   └── EChart.vue          # ECharts 轻量封装（按需引入 + 自动 resize）
│       └── views/                  # 25 个功能页面（+ 登录页，全部路由级懒加载）
│           ├── login/index.vue     # 登录页
│           ├── dashboard/index.vue # 首页/数据看板
│           ├── user/index.vue      # 用户管理（含身份认证审核/角色调整）
│           ├── user-role/index.vue # 用户角色与可见区域
│           ├── department/index.vue # 学院管理
│           ├── spot/index.vue      # 车位管理
│           ├── zone/index.vue      # 停车区域管理
│           ├── tidal/index.vue     # 潮汐（临时）车位批次管理
│           ├── reservation/index.vue # 预约管理
│           ├── vehicle/index.vue   # 车辆进出记录
│           ├── visit/index.vue     # 访客与家长报备
│           ├── announcement/index.vue # 公告管理（含附件上传）
│           ├── violation/index.vue # 违规记录
│           ├── blacklist/index.vue # 黑白名单
│           ├── recharge/index.vue  # 充值记录
│           ├── payment/index.vue   # 扣费记录
│           ├── monthly-pass/index.vue # 月卡管理（经营概览/筛选/详情）
│           ├── config/index.vue    # 系统配置
│           ├── rules/index.vue     # 停车规则
│           ├── server/index.vue    # 硬件监控
│           ├── job/index.vue       # 定时任务管理（动态 Cron / 启停 / 立即执行）
│           ├── account/index.vue   # 管理员账号
│           ├── role/index.vue      # 管理员角色与菜单权限
│           ├── profile/index.vue   # 管理员个人中心
│           ├── login-log/index.vue # 登录日志
│           └── operation-log/index.vue # 操作日志
│
└── parking-miniapp/                # 微信小程序（原生框架）
    ├── app.js                      # 小程序入口（globalData: baseUrl，统一从 utils/request.js 解析）
    ├── app.json                    # 全局配置（路由/TabBar/窗口样式/lazyCodeLoading 按需注入）
    ├── app.wxss                    # 全局样式
    ├── project.config.json         # 微信开发者工具项目配置
    ├── project.private.config.json # 开发者工具本地私有配置
    ├── sitemap.json                # 站点地图
    ├── utils/
    │   ├── request.js              # 网络请求封装（wx.request + Token 管理 + 超时 + 文件上传）
    │   ├── device.js               # 微信登录/绑定：稳定 deviceId 与 wx.login code
    │   ├── subscribe.js            # 微信订阅消息：模板获取、授权上报、本地防抖
    │   ├── poller.js               # 页面静默轮询器（自动同步）
    │   ├── spotSocket.js           # 车位状态 WebSocket（订阅/重连/心跳/轮询兜底判断）
    │   └── timeSlot.js             # 预约时段计算（过滤历史时段，禁止查看/预约过去时间）
    └── pages/
        ├── index/                  # 首页 — 概览看板
        │   ├── index.js            # 逻辑（加载概览 / 车位地图 / 回退）
        │   ├── index.wxml          # 模板
        │   └── index.wxss          # 样式
        ├── login/                  # 登录 — 微信登录 / 手机号注册
        │   ├── login.js
        │   ├── login.wxml
        │   └── login.wxss
        ├── profile/                # 账号信息 — 头像 / 昵称 / 手机号 / 车牌编辑
        │   ├── profile.js
        │   ├── profile.wxml
        │   └── profile.wxss
        ├── password/               # 修改密码 — 校验原密码
        │   ├── password.js
        │   ├── password.wxml
        │   └── password.wxss
        ├── reserve/                # 预约 — 选车位 / 选日期时间 / 计算费用
        │   ├── reserve.js
        │   ├── reserve.wxml
        │   └── reserve.wxss
        ├── reserve-detail/         # 预约详情页
        │   ├── reserve-detail.js
        │   ├── reserve-detail.json
        │   ├── reserve-detail.wxml
        │   └── reserve-detail.wxss
        ├── enter/                  # 入场 — 选择已预约车位，登记入场
        │   ├── enter.js
        │   ├── enter.wxml
        │   └── enter.wxss
        ├── exit/                   # 出场 — 实时计费 / 结算
        │   ├── exit.js
        │   ├── exit.wxml
        │   └── exit.wxss
        ├── mine/                   # 我的 — 个人信息 / 功能入口
        │   ├── mine.js
        │   ├── mine.wxml
        │   └── mine.wxss
        ├── recharge/               # 充值 — 套餐选择 / 模拟支付
        │   ├── recharge.js
        │   ├── recharge.wxml
        │   └── recharge.wxss
        └── record/                 # 记录 — 停车历史
            ├── record.js
            ├── record.wxml
            └── record.wxss

    # 另有以下页面（结构同上：.js / .json / .wxml / .wxss）
    #   announcement/        公告列表（TabBar 第 2 项，位于「首页」与「我的」之间）
    #   announcement-detail/ 公告详情（正文 + 附件预览/下载）
    #   spots/               车位列表（按分区查看全部车位状态 + 搜索）
    #   tidal/               潮汐车位（批次、开放时段、到点可入场）
    #   my-reservations/     我的预约
    #   my-violations/       我的违规
    #   my-pass/             月卡 / 长租（开通、续费、到期提醒授权）
    #   visit/               访客与家长报备
    #   rules/               校园规则
```

---

## 数据库设计

### 表结构概览

#### 1. `t_user` — 用户表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| openid | VARCHAR(64) | 微信 openid |
| nickname | VARCHAR(64) | 昵称 |
| user_no | VARCHAR(32) | 学号 / 工号（访客账号可为空） |
| identity_type | TINYINT | 1学生 2教师 3职工 4访客 5家长 |
| department | VARCHAR(64) | 院系 / 部门 |
| class_name | VARCHAR(64) | 班级 |
| identity_status | TINYINT | 身份认证：0待审核 1已认证 2已驳回 |
| identity_remark | VARCHAR(255) | 身份审核备注（自动核验/人工审核说明） |
| phone | VARCHAR(20) | 手机号 |
| password | VARCHAR(128) | 密码（BCrypt 加密） |
| plate_number | VARCHAR(20) | 默认车牌号 |
| balance | DECIMAL(10,2) | 账户余额 |
| status | TINYINT | 状态（0-禁用, 1-正常） |
| role_code | VARCHAR(32) | 用户角色编码（t_user_role.role_code），按身份自动映射 |
| created_at | DATETIME | 注册时间 |
| updated_at | DATETIME | 更新时间 |

#### 2. `t_admin` — 管理员表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| username | VARCHAR(32) | 登录用户名 |
| password | VARCHAR(128) | 密码（BCrypt 加密） |
| nickname | VARCHAR(32) | 显示昵称 |
| role | VARCHAR(32) | 角色编码（关联 `t_role.role_code`，内置 `super_admin` / `admin`） |
| status | TINYINT | 状态 |
| created_at | DATETIME | 创建时间 |

#### 3. `t_parking_spot` — 车位表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| spot_code | VARCHAR(32) | 车位编号（固定车位如 A-001，潮汐车位如 TD-001） |
| area | VARCHAR(64) | 所属区域（固定车位如 A区，潮汐车位为批次设定的显示区域名） |
| zone_code | VARCHAR(32) | 校园分区：teaching/library/dorm/gym/visitor/**tidal（潮汐临时区）** |
| allow_identities | VARCHAR(64) | 允许停放的身份类型（逗号分隔，空表示不限） |
| free_minutes | INT | 本区域免费时长（分钟），0 表示按身份规则 |
| **spot_type** | TINYINT | **车位类型：0-固定车位, 1-潮汐临时车位** |
| **tidal_batch_id** | BIGINT | **所属潮汐批次 ID（固定车位为 NULL）** |
| **open_time** | DATETIME | **潮汐车位开放时间（固定车位为 NULL）** |
| **close_time** | DATETIME | **潮汐车位结束时间（固定车位为 NULL）** |
| floor | VARCHAR(32) | 楼层 |
| status | TINYINT | 状态：0-空闲, 1-占用, 2-已预约, 3-维护中（批次取消/结束后空闲车位会置为 3） |
| price_per_hour | DECIMAL(10,2) | 每小时价格（元） |
| remark | VARCHAR(256) | 备注 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

#### 4. `t_reservation` — 预约表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| user_id | BIGINT | 用户ID |
| spot_id | BIGINT | 车位ID |
| reservation_no | VARCHAR(32) | 预约编号 |
| start_time | DATETIME | 开始时间 |
| end_time | DATETIME | 结束时间 |
| duration_hours | DECIMAL(10,2) | 预约时长（小时） |
| total_amount | DECIMAL(10,2) | 预定金额 |
| status | TINYINT | 状态：0-待支付, 1-进行中, 2-已完成, 3-已取消, 4-已过期 |
| cancel_reason | VARCHAR(255) | 取消原因 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

#### 5. `t_vehicle_record` — 停车记录表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| user_id | BIGINT | 用户ID |
| spot_id | BIGINT | 车位ID |
| reservation_id | BIGINT | 关联预约ID（可为空） |
| plate_number | VARCHAR(16) | 车牌号 |
| entry_time | DATETIME | 入场时间 |
| exit_time | DATETIME | 出场时间 |
| duration_minutes | INT | 停车时长（分钟） |
| total_fee | DECIMAL(10,2) | 总费用 |
| payment_status | TINYINT | 支付状态：0-未支付, 1-已支付 |
| status | TINYINT | 记录状态：0-停车中, 1-已出场, 2-管理员强制出场 |
| created_at | DATETIME | 创建时间 |
| updated_at | DATETIME | 更新时间 |

#### 6. `t_recharge_record` — 充值记录表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| user_id | BIGINT | 用户ID |
| order_no | VARCHAR(32) | 订单编号 |
| amount | DECIMAL(10,2) | 充值金额 |
| pay_method | VARCHAR(16) | 支付方式 |
| status | TINYINT | 状态：0-待支付, 1-成功, 2-失败 |
| created_at | DATETIME | 创建时间 |

#### 7. `t_payment_record` — 扣费记录表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| user_id | BIGINT | 用户ID |
| vehicle_record_id | BIGINT | 关联停车记录ID |
| amount | DECIMAL(10,2) | 扣费金额 |
| balance_before | DECIMAL(10,2) | 扣费前余额 |
| balance_after | DECIMAL(10,2) | 扣费后余额 |
| type | VARCHAR(16) | 扣费类型 |
| created_at | DATETIME | 创建时间 |

#### 8. `t_system_config` — 系统配置表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | INT | 主键（自增） |
| config_key | VARCHAR(64) | 配置键 |
| config_value | VARCHAR(512) | 配置值 |
| description | VARCHAR(255) | 配置说明 |
| updated_at | DATETIME | 更新时间 |

#### 9. `t_tidal_batch` — 潮汐（临时）车位批次表

管理员为高峰期（开学迎新、考试、比赛等）开设的一批临时车位。批次只存"这一批"的元信息与开放时间段，实际车位由 `t_parking_spot` 承载（`spot_type=1` 且 `tidal_batch_id` 指向本表）。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| batch_name | VARCHAR(64) | 批次名称（如「2026 春季开学临时停车区」） |
| area | VARCHAR(64) | 用户端展示的区域名（如「东门广场临时停车区」） |
| zone_code | VARCHAR(32) | 分区编码，固定为 `tidal` |
| spot_prefix | VARCHAR(16) | 车位编号前缀，最终编号形如 `TD-001` |
| spot_count | INT | 该批次车位数 |
| floor | VARCHAR(32) | 楼层/位置说明 |
| price_per_hour | DECIMAL(10,2) | 每小时单价 |
| allow_identities | VARCHAR(64) | 允许停放的身份类型（逗号分隔，空表示不限） |
| free_minutes | INT | 本批次免费时长（分钟），0 表示按身份规则 |
| open_time | DATETIME | **开放时间**（精确到分钟） |
| close_time | DATETIME | **结束时间**（精确到分钟） |
| status | TINYINT | 0-待开放, 1-开放中, 2-已结束, 3-已取消（仅供管理端展示，用户端可见性按时间实时计算） |
| remark | VARCHAR(256) | 备注 |
| created_at / updated_at | DATETIME | 创建 / 更新时间 |

#### 10. `t_announcement` — 公告表

管理员发布的通知，**一条公告最多携带一个附件**。附件二进制不存在数据库里，只存存储文件名与原始文件名，文件本身位于 `parking-server/uploads/announcement/`（该目录由服务在首次上传时自动创建）。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| title | VARCHAR(128) | 公告标题 |
| content | TEXT | 公告正文 |
| attachment_name | VARCHAR(255) | **附件原始文件名**（含扩展名，展示与下载时用） |
| attachment_path | VARCHAR(255) | **附件存储文件名**（雪花ID.ext，位于 uploads/announcement 下） |
| attachment_size | BIGINT | 附件大小（字节） |
| attachment_ext | VARCHAR(16) | 附件扩展名（小写，不含点） |
| status | TINYINT | 0-草稿, 1-已发布（小程序端只能看到已发布） |
| publisher | VARCHAR(64) | 发布人（取当前登录管理员的昵称） |
| publish_time | DATETIME | 发布时间（首次发布时写入，后续编辑不覆盖） |
| view_count | INT | 浏览量（详情接口原子自增） |
| created_at / updated_at | DATETIME | 创建 / 更新时间 |

#### 11. `t_admin_login_log` — 管理员登录日志表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| admin_id / username | BIGINT / VARCHAR | 登录账号 |
| login_ip / user_agent | VARCHAR | 登录 IP 与浏览器标识 |
| status | TINYINT | 登录结果（成功 / 失败） |
| fail_reason | VARCHAR | 失败原因 |
| login_time | DATETIME | 登录时间 |

#### 12. `t_admin_operation_log` — 管理员操作日志表

由 `@OperationLog` 注解 + `OperationLogAspect` 环绕切面自动写入，记录后台敏感操作。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| admin_id / username | BIGINT / VARCHAR | 操作人 |
| operation / menu_code / type | VARCHAR | 操作名称、所属菜单、操作类型（新增/修改/删除/查询等） |
| uri / ip | VARCHAR | 请求路径与来源 IP |
| params | TEXT | 请求参数（JSON，截断 2000 字符） |
| response_status / cost_ms | INT | 响应状态码与耗时（毫秒） |
| created_at | DATETIME | 操作时间 |

#### 13. `t_admin_menu` — 旧版账号菜单表（已废弃）

> 当前版本已升级为角色 RBAC：管理员通过 `t_admin.role` 绑定 `t_role.role_code`，菜单授权存放在 `t_role_menu`。该表不再参与鉴权，可从旧库中安全删除。

#### 14. `t_role` — 管理员角色表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| role_code | VARCHAR(32) | 角色编码（`super_admin` / `admin` 等，创建后不可修改） |
| role_name | VARCHAR(64) | 角色名称 |
| description | VARCHAR(255) | 描述 |
| builtin | TINYINT | 是否内置角色（内置角色不可删除） |
| created_at / updated_at | DATETIME | 创建 / 更新时间 |

#### 15. `t_role_menu` — 角色菜单权限表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| role_id | BIGINT | 角色 ID |
| menu_code | VARCHAR(64) | 菜单编码（dashboard / users / spots / monthly_passes 等），与 `MenuPermissionMapping` 配合由 `MenuAuthInterceptor` 校验 |

#### 16. `t_parking_zone` — 校园停车区域表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| zone_code | VARCHAR(32) | 区域编码（teaching / library / dorm / gym / visitor / tidal，创建后不可修改） |
| zone_name | VARCHAR(64) | 区域名称 |
| description | VARCHAR(255) | 说明 |
| sort_order | INT | 排序 |
| status | TINYINT | 1 启用 / 0 停用 |
| created_at / updated_at | DATETIME | 创建 / 更新时间 |

车位通过 `t_parking_spot.zone_code` 归属区域；系统内置 `tidal` 区域禁止删除与停用。

#### 17. `t_user_role` — 小程序用户角色表

角色 = 校园身份，权限 = 可见停车区域。内置角色与身份类型一一对应，`role_code` 创建后不可修改。

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| role_code | VARCHAR(32) | 角色编码 |
| role_name | VARCHAR(64) | 角色名称 |
| identity_type | TINYINT | 对应校园身份 1~5 |
| description | VARCHAR(255) | 描述 |
| builtin | TINYINT | 是否内置 |

#### 18. `t_user_role_perm` — 用户角色可见区域表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| role_id | BIGINT | 用户角色 ID |
| perm_code | VARCHAR(32) | 可见停车区域编码（`zone_code`） |

#### 19. `t_monthly_pass` — 月卡 / 长租表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| user_id / plate_number | BIGINT / VARCHAR(20) | 持有人与生效车牌（两者同时匹配才生效） |
| pass_type | TINYINT | 1 月卡 / 2 季卡 / 3 年卡 |
| zone_code | VARCHAR(32) | 允许停放区域，空表示全场 |
| start_date / expire_date | DATE | 生效日 / 到期日 |
| price_paid | DECIMAL(10,2) | 实付金额 |
| status | TINYINT | 1 生效 / 2 已过期 / 3 已退款 |
| remark | VARCHAR(255) | 备注 |

#### 20. `t_subscribe_auth` — 微信订阅消息授权表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| user_id | BIGINT | 用户 ID |
| template_id | VARCHAR(64) | 微信模板 ID |
| scene | VARCHAR(32) | reserve_remind / balance_low / identity_result / monthly_pass_expire |
| subscribed | TINYINT | 1 同意 / 0 拒绝 |
| send_status | TINYINT | 0 未发送 / 1 成功 / 2 失败 |
| send_time | DATETIME | 实际发送时间 |
| biz_ref | VARCHAR(64) | 关联业务号（预约号 / 月卡ID:剩余天数等） |

#### 21. `t_blacklist` — 车辆/用户黑名单表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| plate_number / user_id | VARCHAR(20) / BIGINT | 命中对象，至少填一个 |
| reason | VARCHAR(255) | 加入原因 |
| until_time | DATETIME | 有效期截止，NULL 表示永久 |
| source | VARCHAR(20) | 来源：manual 手动 / auto_violation 违规自动 |
| operator_id | BIGINT | 操作管理员 ID（自动拉黑为空） |
| created_at / updated_at | DATETIME | 创建 / 更新时间 |

命中后禁止预约、入场与访客报备；**黑名单优先于白名单**。

#### 22. `t_whitelist` — 车辆/用户白名单表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| plate_number / user_id | VARCHAR(20) / BIGINT | 放行对象，至少填一个 |
| reason | VARCHAR(255) | 加入原因 |
| until_time | DATETIME | 有效期截止，NULL 表示永久 |
| source | VARCHAR(20) | 来源：manual 手动 / system 系统 |
| operator_id | BIGINT | 操作管理员 ID |
| created_at / updated_at | DATETIME | 创建 / 更新时间 |

白名单豁免身份区域、假期模式、夜间禁停与每日预约上限，访客报备自动通过；费用照常计算。

### ER 关系图

```
t_user ──1:N──→ t_reservation ──N:1──→ t_parking_spot
  │                  │                     ↑
  │                  └──N:1──→ t_vehicle_record
  │
  ├──1:N──→ t_recharge_record
  ├──1:N──→ t_payment_record
  ├──1:N──→ t_vehicle_record
  ├──1:N──→ t_monthly_pass
  ├──1:N──→ t_subscribe_auth
  └──N:1──→ t_user_role ──1:N──→ t_user_role_perm ──N:1──→ t_parking_zone

t_parking_spot ──N:1──→ t_parking_zone    （zone_code）
t_tidal_batch ──1:N──→ t_parking_spot   （spot_type=1 的潮汐临时车位）
t_user ──1:N──→ t_blacklist / t_whitelist （按 user_id；也可仅按 plate_number 关联）

t_admin ──N:1──→ t_role ──1:N──→ t_role_menu
t_admin ──1:N──→ t_admin_login_log      （登录日志）
t_admin ──1:N──→ t_admin_operation_log  （操作日志）

t_system_config (独立，键值对存储)
t_announcement (独立，附件文件存放在 uploads/announcement/)
```

### 种子数据

项目提供**单文件数据库脚本** `docs/parking_system.sql`：包含 20 张核心业务表的建表语句与演示数据，并已整合 `t_role` / `t_role_menu` / `t_user_role` / `t_user_role_perm` / `t_parking_zone` 及高频查询索引，**一次导入即可完成全部初始化**。

> ⚠️ 该导出是**本机运行库的完整快照**（含真实用户手机号、车牌、登录日志、来源 IP 等运行数据），因此**不随仓库提交**（见 `.gitignore`）。开源仓库只包含代码；需要初始化数据库时，请使用自己导出的版本（见「后端启动 → 初始化数据库」）。预设演示数据包括：

| 数据 | 内容 |
|------|------|
| **默认管理员** | 用户名 `admin`，密码 `admin123`，角色 `super_admin` |
| **演示用户** | 手机号 `13800138000`，密码 `123456`，余额 ¥500.00，车牌 `粤B12345` |
| **350 个车位** | 教学楼区 (B1, A-001~A-015, ¥5/时) + 图书馆区 (B2, B-001~B-015, ¥4/时) + 宿舍区 (B3, C-001~C-020, ¥3/时) + 访客与家长临时停车区 (B1~B3, V-001~V-300, ¥6/时) |
| **7 个学院/部门** | 计算机与信息工程学院(CS)、机械与电气工程学院(ME)、经济管理学院(EM)、外国语学院(FL)、体育学院(PE)、图书馆(LIB)、后勤保障处(LOG) |
| **系统配置** | 共 26 项：默认小时费率 5.00 元、最大预约 30 天、最小预约 1 小时、宽限期 15 分钟，以及学生折扣、各身份免费时长、访客加价系数、每日预约上限、夜间禁停时段、假期模式与折扣、超时违规宽限期、潮汐车位提前预约天数、月卡/季卡/年卡价格、四类订阅消息模板 ID，以及自动拉黑开关/阈值/统计窗口/持续天数等 |

### 多身份演示账号（密码均为 `123456`）

| 昵称 | 手机号 | 身份 | 学号/工号 | 学院/部门 | 认证状态 | 车牌 | 权益（实测） |
|------|--------|------|-----------|-----------|----------|------|--------------|
| 王老师 | 13900000001 | 教师 | T2024002 | 计算机与信息工程学院 | 已认证 | 粤B10001 | 免费 120 分钟、每日 5 次 |
| 李老师（待审核） | 13900000002 | 教师 | T2024003 | 机械与电气工程学院 | **待审核** | 粤B10002 | 暂按访客费率（1.5 倍、15 分钟） |
| 张职工 | 13900000003 | 职工 | Z2024001 | 后勤保障处 | 已认证 | 粤B10003 | 免费 60 分钟、每日 3 次 |
| 访客刘先生 | 13900000004 | 访客 | — | 图书馆 | 已认证 | 粤B10004 | 费率 1.5 倍、免费 15 分钟 |
| 家长陈女士 | 13900000005 | 家长 | — | — | 已认证 | 粤B10005 | 免费 30 分钟（接送临时停车） |
| **学生·刘同学** | **13900000006** | 学生 | S2024001 | 计算机与信息工程学院 | 已认证 | 粤B20001 | 五折、免费 15 分钟、每日 2 次；**当前在停 A-001**，有违规/预约/停车/充值/扣费记录 |
| **学生·陈同学** | **13900000007** | 学生 | S2024002 | 经济管理学院 | 已认证 | 粤B20002 | 五折；**当前在停 C-001**，有违规与历史记录 |
| Demo User | 13800138000 | 教师 | T2024001 | 计算机与信息工程学院 | 已认证 | 粤B12345 | 余额 ¥500 |

> 另有 1 个「待审核示例学生」账号用于演示管理端身份认证审核流程。
> 无名手机号的「访客·张先生 / 家长·李女士 / 访客·周先生」为访客报备自动生成的访客账号，仅用于停车记录与结算演示。

### 演示数据（位于本地 `docs/parking_system.sql`，不随仓库提交）

`docs/parking_system.sql` 是当前运行库的完整导出，答辩/演示所需数据已全部包含在内，导入后各页面即有数据，无需再单独执行种子脚本；但该文件含真实用户与日志数据，**已加入 `.gitignore`，不会发布到仓库**。
若需要「恢复一份干净的演示数据」，在本地重新导入该导出文件即可（脚本内含 `DROP TABLE IF EXISTS`，会先清空再重建）：

```bash
mysql -u root -p parking_system < docs/parking_system.sql
```

导出内容（实测）：

| 数据 | 数量 | 覆盖状态 |
|------|------|----------|
| 违规记录 | 5 条 | 超时占用 3（2 待处理 / 1 已处理）、违规区域 1（未处理）、占用他人车位 1（已处理） |
| 车辆黑名单 | 3 条 | 永久车牌 2、临时车牌 1（30 天） |
| 车辆白名单 | 1 条 | 校务公务车（永久） |
| 访客与家长报备 | 8 条 | 待审核 2、已通过 2（已分配 V-002/V-003）、已入场 1（占用 V-001）、已完成 1、已驳回 1、已撤销 1 |
| 预约记录 | 4 条 | 进行中 2、已完成 1、已过期 1 |
| 停车记录 | 10 条 | 在停 3（A-001 / C-001 / V-001）、历史出场 7（含费用与扣费记录） |
| 充值 / 扣费 | 5 / 7 条 | 微信、支付宝；自动扣费、预约结算、管理端强制出场 |
| 演示账号 | 5 个 | 2 名学生（有手机号可登录）+ 3 个访客/家长账号（用于报备入场结算） |

数据一致性：占用车位数与在停车辆数一致、同一车位不会同时被预约和占用、总车位 350（空闲 342 / 占用 3 / 已预约 4 / 维护 1）。

---

## API 接口文档

启动后端后，访问 Knife4j 在线文档：**http://localhost:8080/doc.html**

### 接口一览表

> 鉴权说明：**公开** = 无需 Token；**用户** = 小程序 JWT；**管理员** = 管理端 JWT（受菜单权限控制，`/api/admin/profile` 除外）；**签名** = HMAC 请求签名，不走 JWT。

| 模块 | 方法 | 路径 | 鉴权 | 说明 |
|------|------|------|------|------|
| 认证 | GET | `/api/auth/captcha` | 公开 | 获取图形验证码 |
| 认证 | POST | `/api/auth/register` | 公开 | 用户注册（校内身份自动核验） |
| 认证 | POST | `/api/auth/login` | 公开 | 手机号 + 密码登录 |
| 认证 | POST | `/api/auth/admin/login` | 公开 | 管理员登录 |
| 认证 | POST | `/api/auth/wechat-login` | 公开 | 微信一键登录 |
| 用户 | GET | `/api/user/info` | 用户 | 当前用户信息 |
| 用户 | PUT | `/api/user/info` | 用户 | 更新资料 / 校园身份 |
| 用户 | GET | `/api/user/my-perms` | 用户 | 角色与可见停车区域 |
| 用户 | GET | `/api/user/benefits` | 用户 | 校园身份权益 |
| 用户 | PUT | `/api/user/password` | 用户 | 修改登录密码 |
| 用户 | POST | `/api/user/avatar` | 用户 | 上传头像 |
| 用户 | POST | `/api/user/bind-wechat` | 用户 | 绑定微信 |
| 用户 | POST | `/api/user/unbind-wechat` | 用户 | 解绑微信 |
| 用户 | POST | `/api/user/visit-report` | 用户 | 提交访客/家长报备 |
| 用户 | GET | `/api/user/visit-reports` | 用户 | 我的报备记录 |
| 用户 | POST | `/api/user/visit-report/{id}/cancel` | 用户 | 撤销报备 |
| 用户 | POST | `/api/user/recharge` | 用户 | 余额充值 |
| 用户 | GET | `/api/user/recharge/records` | 用户 | 充值记录 |
| 用户 | GET | `/api/user/payment/records` | 用户 | 扣费记录 |
| 用户 | GET | `/api/user/reservations` | 用户 | 我的预约 |
| 用户 | GET | `/api/user/vehicle-records` | 用户 | 我的停车记录 |
| 用户 | GET | `/api/user/rules` | 用户 | 公开校园停车规则 |
| 月卡 | POST | `/api/user/monthly-pass/open` | 用户 | 开通 / 续费月卡 |
| 月卡 | GET | `/api/user/monthly-pass/current` | 用户 | 当前生效月卡 |
| 月卡 | GET | `/api/user/monthly-pass/list` | 用户 | 月卡历史记录 |
| 月卡 | GET | `/api/user/monthly-pass/prices` | 用户 | 月卡价格配置 |
| 订阅消息 | GET | `/api/user/subscribe/templates` | 用户 | 四类场景模板 ID |
| 订阅消息 | POST | `/api/user/subscribe/report` | 用户 | 授权结果上报 |
| 校园 SSO | POST | `/api/open/campus/sso/login` | 签名 | 校园系统单点登录 |
| 车位 | GET | `/api/spot/list` | 用户 | 可用车位列表 |
| 车位 | GET | `/api/spot/overview` | 用户 | 本人停车概览 |
| 车位 | GET | `/api/spot/tidal` | 用户 | 潮汐车位批次 |
| 公告 | GET | `/api/announcement/list` | 公开 | 已发布公告列表 |
| 公告 | GET | `/api/announcement/{id}` | 公开 | 公告详情（浏览量自增） |
| 公告 | GET | `/api/announcement/{id}/attachment` | 公开 | 下载附件 |
| 公告 | GET | `/api/announcement/attachment-types` | 公开 | 附件类型与大小限制 |
| 学院 | GET | `/api/departments` | 公开 | 启用中的学院/部门 |
| 学院 | GET | `/api/departments/{id}` | 公开 | 学院/部门详情 |
| 预约 | POST | `/api/reservation/create` | 用户 | 创建预约 |
| 预约 | POST | `/api/reservation/cancel/{id}` | 用户 | 取消预约 |
| 预约 | GET | `/api/reservation/{id}` | 用户 | 预约详情 |
| 车辆 | POST | `/api/vehicle/enter` | 用户 | 车辆入场 |
| 车辆 | POST | `/api/vehicle/exit/{recordId}` | 用户 | 出场结算 |
| 车辆 | GET | `/api/vehicle/current` | 用户 | 当前停车状态 |
| 车辆 | GET | `/api/vehicle/violations` | 用户 | 我的违规记录 |
| 后台·首页 | GET | `/api/admin/dashboard` | 管理员 | 首页统计 |
| 后台·用户 | GET | `/api/admin/users` | 管理员 | 用户列表 |
| 后台·用户 | GET | `/api/admin/user/{id}` | 管理员 | 用户详情 |
| 后台·用户 | PUT | `/api/admin/user/{id}/status` | 管理员 | 启用 / 禁用用户 |
| 后台·用户 | PUT | `/api/admin/user/{id}/identity` | 管理员 | 身份审核 |
| 后台·预约 | GET | `/api/admin/reservations` | 管理员 | 预约列表 |
| 后台·预约 | PUT | `/api/admin/reservation/{id}/status` | 管理员 | 修改预约状态 |
| 后台·车辆 | GET | `/api/admin/vehicle-records` | 管理员 | 停车记录列表 |
| 后台·车辆 | PUT | `/api/admin/vehicle-record/{id}` | 管理员 | 编辑停车记录 |
| 后台·车辆 | POST | `/api/admin/vehicle-record/{id}/force-exit` | 管理员 | 强制出场 |
| 后台·资金 | GET | `/api/admin/recharge-records` | 管理员 | 充值记录 |
| 后台·资金 | GET | `/api/admin/payment-records` | 管理员 | 扣费记录 |
| 后台·月卡 | GET | `/api/admin/monthly-passes` | 管理员 | 月卡列表 |
| 后台·月卡 | GET | `/api/admin/monthly-passes/stats` | 管理员 | 月卡经营统计 |
| 后台·车位 | GET | `/api/admin/spots` | 管理员 | 车位列表 |
| 后台·车位 | POST | `/api/admin/spot` | 管理员 | 新增车位 |
| 后台·车位 | PUT | `/api/admin/spot/{id}` | 管理员 | 编辑车位 |
| 后台·车位 | PUT | `/api/admin/spot/{id}/status` | 管理员 | 修改车位状态 |
| 后台·车位 | DELETE | `/api/admin/spot/{id}` | 管理员 | 删除车位 |
| 后台·车位 | GET | `/api/admin/spot-map` | 管理员 | 车位地图 |
| 后台·区域 | GET | `/api/admin/zones` | 管理员 | 停车区域列表 |
| 后台·区域 | POST | `/api/admin/zone` | 管理员 | 新增停车区域 |
| 后台·区域 | PUT | `/api/admin/zone/{id}` | 管理员 | 编辑停车区域 |
| 后台·区域 | PUT | `/api/admin/zone/{id}/status` | 管理员 | 启停停车区域 |
| 后台·区域 | DELETE | `/api/admin/zone/{id}` | 管理员 | 删除停车区域 |
| 后台·学院 | GET | `/api/admin/departments` | 管理员 | 学院列表（含停用） |
| 后台·学院 | POST | `/api/admin/department` | 管理员 | 新增学院 |
| 后台·学院 | PUT | `/api/admin/department/{id}` | 管理员 | 编辑学院 |
| 后台·学院 | PUT | `/api/admin/department/{id}/status` | 管理员 | 启停学院 |
| 后台·学院 | DELETE | `/api/admin/department/{id}` | 管理员 | 删除学院 |
| 后台·配置 | GET | `/api/admin/config` | 管理员 | 读取系统配置 |
| 后台·配置 | PUT | `/api/admin/config` | 管理员 | 批量更新配置 |
| 后台·报备 | GET | `/api/admin/visit-reports` | 管理员 | 访客报备列表 |
| 后台·报备 | GET | `/api/admin/visit-reports/stats` | 管理员 | 访客报备统计 |
| 后台·报备 | PUT | `/api/admin/visit-report/{id}/approve` | 管理员 | 审核通过 |
| 后台·报备 | PUT | `/api/admin/visit-report/{id}/reject` | 管理员 | 驳回 |
| 后台·报备 | POST | `/api/admin/visit-report/{id}/check-in` | 管理员 | 登记入场 |
| 后台·报备 | POST | `/api/admin/visit-report/{id}/check-out` | 管理员 | 出场结算 |
| 后台·违规 | GET | `/api/admin/violations` | 管理员 | 违规列表 |
| 后台·违规 | GET | `/api/admin/violations/stats` | 管理员 | 违规统计 |
| 后台·违规 | PUT | `/api/admin/violation/{id}/handle` | 管理员 | 处理违规 |
| 后台·统计 | GET | `/api/admin/campus-stats` | 管理员 | 校园统计 |
| 后台·统计 | GET | `/api/admin/export/{type}` | 管理员 | 导出 CSV 报表 |
| 后台·黑名单 | GET | `/api/admin/blacklist` | 管理员 | 黑名单列表（支持状态/来源筛选） |
| 后台·黑名单 | POST | `/api/admin/blacklist` | 管理员 | 加入黑名单 |
| 后台·黑名单 | DELETE | `/api/admin/blacklist/{id}` | 管理员 | 移出黑名单 |
| 后台·白名单 | GET | `/api/admin/whitelist` | 管理员 | 白名单列表（支持状态筛选） |
| 后台·白名单 | POST | `/api/admin/whitelist` | 管理员 | 加入白名单 |
| 后台·白名单 | DELETE | `/api/admin/whitelist/{id}` | 管理员 | 移出白名单 |
| 后台·潮汐 | GET | `/api/admin/tidal-batches` | 管理员 | 潮汐批次列表 |
| 后台·潮汐 | GET | `/api/admin/tidal-batch/{id}` | 管理员 | 潮汐批次详情 |
| 后台·潮汐 | POST | `/api/admin/tidal-batch` | 管理员 | 新建潮汐批次 |
| 后台·潮汐 | PUT | `/api/admin/tidal-batch/{id}` | 管理员 | 修改潮汐批次 |
| 后台·潮汐 | PUT | `/api/admin/tidal-batch/{id}/cancel` | 管理员 | 取消批次 |
| 后台·潮汐 | DELETE | `/api/admin/tidal-batch/{id}` | 管理员 | 删除批次 |
| 后台·潮汐 | PUT | `/api/admin/tidal-batch/{id}/restore` | 管理员 | 恢复批次 |
| 后台·潮汐 | PUT | `/api/admin/tidal-batch/{id}/soft-delete` | 管理员 | 软删除批次 |
| 后台·潮汐 | DELETE | `/api/admin/tidal-batch/{id}/force` | 管理员 | 彻底删除批次 |
| 后台·公告 | GET | `/api/admin/announcements` | 管理员 | 公告列表（含草稿） |
| 后台·公告 | GET | `/api/admin/announcement/{id}` | 管理员 | 公告详情 |
| 后台·公告 | POST | `/api/admin/announcement` | 管理员 | 新建公告 |
| 后台·公告 | PUT | `/api/admin/announcement/{id}` | 管理员 | 编辑公告 |
| 后台·公告 | PUT | `/api/admin/announcement/{id}/status` | 管理员 | 发布 / 下架 |
| 后台·公告 | DELETE | `/api/admin/announcement/{id}` | 管理员 | 删除公告 |
| 后台·账号 | GET | `/api/admin/accounts` | 管理员 | 管理员账号列表 |
| 后台·账号 | POST | `/api/admin/account` | 管理员 | 新增管理员 |
| 后台·账号 | PUT | `/api/admin/account/{id}` | 管理员 | 编辑管理员 |
| 后台·账号 | DELETE | `/api/admin/account/{id}` | 管理员 | 删除管理员 |
| 后台·账号 | PUT | `/api/admin/account/{id}/reset-password` | 管理员 | 重置密码 |
| 后台·账号 | PUT | `/api/admin/account/{id}/status` | 管理员 | 启停管理员 |
| 后台·账号 | GET | `/api/admin/account/my-menus` | 管理员 | 当前账号菜单 |
| 后台·角色 | GET | `/api/admin/roles` | 管理员 | 角色列表 |
| 后台·角色 | POST | `/api/admin/role` | 管理员 | 新建角色 |
| 后台·角色 | PUT | `/api/admin/role/{id}` | 管理员 | 修改角色 |
| 后台·角色 | DELETE | `/api/admin/role/{id}` | 管理员 | 删除角色 |
| 后台·角色 | GET | `/api/admin/role/{id}/menus` | 管理员 | 角色菜单查询 |
| 后台·角色 | PUT | `/api/admin/role/{id}/menus` | 管理员 | 分配角色菜单 |
| 后台·用户角色 | GET | `/api/admin/user-roles` | 管理员 | 用户角色列表 |
| 后台·用户角色 | PUT | `/api/admin/user-role/{id}` | 管理员 | 修改用户角色 |
| 后台·用户角色 | GET | `/api/admin/user-role/{id}/perms` | 管理员 | 角色可见区域查询 |
| 后台·用户角色 | PUT | `/api/admin/user-role/{id}/perms` | 管理员 | 分配角色可见区域 |
| 后台·用户角色 | PUT | `/api/admin/user/{id}/role` | 管理员 | 调整用户所属角色 |
| 后台·个人 | GET | `/api/admin/profile` | 管理员 | 本人资料 |
| 后台·个人 | PUT | `/api/admin/profile` | 管理员 | 修改本人昵称 |
| 后台·个人 | PUT | `/api/admin/profile/password` | 管理员 | 修改本人密码 |
| 后台·日志 | GET | `/api/admin/login-logs` | 管理员 | 登录日志列表 |
| 后台·日志 | GET | `/api/admin/login-log/{id}` | 管理员 | 登录日志详情 |
| 后台·日志 | GET | `/api/admin/operation-logs` | 管理员 | 操作日志列表 |
| 后台·日志 | GET | `/api/admin/operation-log/{id}` | 管理员 | 操作日志详情 |
| 后台·监控 | GET | `/api/admin/server-monitor` | 管理员 | 服务器硬件监控 |

### 分模块详细说明

#### Auth — 认证接口 (`/api/auth`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/auth/captcha` | 获取图形验证码（返回验证码图片 Base64 + 验证码Key） |
| POST | `/api/auth/register` | 用户注册（手机号 + 密码 + 验证码；**学生/教师/职工填写学号/工号与院系后优先调用校园系统自动核验，通过即认证；查不到或接口不可用时转管理员审核**；访客/家长无需审核） |
| POST | `/api/auth/login` | 用户登录（手机号 + 密码 + 验证码）→ 返回 JWT Token |
| POST | `/api/auth/admin/login` | 管理员登录（用户名 + 密码 + 验证码）→ 返回管理端 JWT Token |
| POST | `/api/auth/wechat-login` | 微信小程序登录（openid / 手机号 + 密码）→ 返回 JWT Token |

#### User — 用户接口 (`/api/user`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/info` | 获取当前用户信息 |
| GET | `/api/user/my-perms` | 当前用户角色、身份与可见停车区域（RBAC） |
| PUT | `/api/user/info` | 更新用户信息（昵称、手机号、车牌号、头像、**校园身份：userNo / identityType / department / className**；身份变化时优先自动核验，未通过则转人工审核） |
| GET | `/api/user/benefits` | **校园身份权益**：计费身份、费率系数、免费停车时长、每日预约上限与剩余次数、夜间禁停时段、学期/假期模式、校园分区名称 |
| POST | `/api/user/visit-report` | 提交访客/家长车辆报备（visitType 1访客 2家长、车牌、到校时段） |
| GET | `/api/user/visit-reports` | 我的报备记录（分页，含审核意见、分配车位） |
| POST | `/api/user/visit-report/{id}/cancel` | 撤销报备（待审核/已通过状态可撤销，并释放车位） |
| PUT | `/api/user/password` | 修改登录密码（校验原密码，新密码 6-20 位） |
| POST | `/api/user/avatar` | 上传头像（multipart/form-data，字段名 `file`，≤2MB，jpg/png/gif/webp/bmp） |
| POST | `/api/user/bind-wechat` | 把微信绑定到当前登录账号（`{code, deviceId}`；已绑定同一微信幂等返回；被其他账号占用时报错） |
| POST | `/api/user/unbind-wechat` | 解除微信绑定（要求账号已设置手机号与登录密码，避免失去登录方式） |
| POST | `/api/user/recharge` | 余额充值 |
| GET | `/api/user/recharge/records` | 充值记录（分页） |
| GET | `/api/user/payment/records` | 扣费记录（分页） |
| GET | `/api/user/reservations` | 我的预约（分页） |
| GET | `/api/user/vehicle-records` | 我的停车记录（分页） |

#### MonthlyPass — 月卡 / 长租接口 (`/api/user/monthly-pass`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/user/monthly-pass/open` | 开通或续费月卡/季卡/年卡（余额扣款，写入扣费记录） |
| GET | `/api/user/monthly-pass/current` | 当前生效月卡（含卡类型、到期日、剩余天数） |
| GET | `/api/user/monthly-pass/list` | 我的月卡历史（分页） |
| GET | `/api/user/monthly-pass/prices` | 月卡/季卡/年卡价格配置 |

#### Subscribe — 微信订阅消息接口 (`/api/user/subscribe`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/subscribe/templates` | 四类场景的模板 ID（reserve_remind / balance_low / identity_result / monthly_pass_expire） |
| POST | `/api/user/subscribe/report` | 授权结果上报（同意/拒绝均需上报，落库后方可发送） |

#### CampusSSO — 第三方校园系统单点登录 (`/api/open`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/open/campus/sso/login` | 校园系统服务端调用：HMAC 签名鉴权，按学号/工号建号或同步身份并直接认证，返回本系统 JWT。**不走 JWT 拦截器** |

> 请求头：`X-Campus-App-Id / X-Campus-Timestamp / X-Campus-Nonce / X-Campus-Signature`；
> 签名原文：`appId + "\n" + timestamp + "\n" + nonce + "\n" + 原始JSON请求体`；
> 时间戳允许 ±5 分钟，nonce 通过 Redis 做 5 分钟防重放。

#### Spot — 车位接口 (`/api/spot`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/spot/list` | 可用车位列表（分页 + 按区域筛选）；**潮汐车位仅在其开放时间段内出现** |
| GET | `/api/spot/overview` | 公开概览（所有车位分区状态 + 已停放车辆 + 活跃预约） |
| GET | `/api/spot/tidal` | **潮汐车位批次**：返回开放前 N 天至结束之间的批次（含车位明细与 `phase`/`canReserve`/`canEnter`）。需要登录 |

#### Announcement — 公告接口 (`/api/announcement`)

> **公开接口，无需登录**（公告属于面向全校的公开信息，已在 JwtInterceptor 白名单中）。

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/announcement/list` | 已发布公告列表（分页 + 关键字匹配标题/正文，按发布时间倒序） |
| GET | `/api/announcement/{id}` | 公告详情（草稿不可见；会原子自增浏览量） |
| GET | `/api/announcement/{id}/attachment` | **下载附件**：以原始文件名返回（`Content-Disposition: attachment; filename*=UTF-8''…`），并按扩展名给出正确的 Content-Type |
| GET | `/api/announcement/attachment-types` | 允许的附件扩展名与大小上限（供前端提示，避免两端硬编码不一致） |

#### Reservation — 预约接口 (`/api/reservation`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/reservation/create` | 创建预约 |
| POST | `/api/reservation/cancel/{id}` | 取消预约 |
| GET | `/api/reservation/{id}` | 预约详情 |

#### Vehicle — 车辆进出接口 (`/api/vehicle`)

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/vehicle/enter` | 车辆入场登记 |
| POST | `/api/vehicle/exit/{recordId}` | 车辆出场结算（自动扣费） |
| GET | `/api/vehicle/current` | 获取当前停车状态 |
| GET | `/api/vehicle/violations` | 我的违规记录（分页，用户端「我的违规」） |

> **入场门禁校验**（`/api/vehicle/enter`）：命中车辆黑名单直接拒绝（黑名单优先）；**白名单车辆跳过身份区域校验**；普通车辆按身份校验可停区域（不匹配则拒绝入场并自动生成 `illegal_zone` 违规记录，扣 2 分）。同一车牌近 N 天违规累计达到阈值时自动加入限期黑名单（白名单车辆除外）。

#### Admin — 管理后台接口 (`/api/admin`)

| 方法 | 路径 | 说明 |
|------|------|------|
| **首页** | | |
| GET | `/api/admin/dashboard` | 首页统计数据 |
| **用户管理** | | |
| GET | `/api/admin/users` | 用户列表（分页 + 搜索 + 状态筛选，含头像字段） |
| GET | `/api/admin/user/{id}` | 用户详情：账号资料（含头像、微信绑定、注册/更新时间）+ 业务统计（停车次数、在场车辆、预约数、充值/消费合计、最近停车） |
| PUT | `/api/admin/user/{id}/status` | 修改用户状态（启用/禁用） |
| PUT | `/api/admin/user/{id}/role` | 调整用户所属小程序角色（`roleCode`） |
| **用户角色（小程序 RBAC）** | | |
| GET | `/api/admin/user-roles` | 用户角色列表（含可见区域、关联用户数、区域字典） |
| PUT | `/api/admin/user-role/{id}` | 修改角色名称/描述（编码与身份类型不可改） |
| GET / PUT | `/api/admin/user-role/{id}/perms` | 查询 / 全量覆盖角色可见停车区域 |
| **预约管理** | | |
| GET | `/api/admin/reservations` | 所有预约（分页 + 状态筛选） |
| PUT | `/api/admin/reservation/{id}/status` | 修改预约状态 |
| **车辆记录** | | |
| GET | `/api/admin/vehicle-records` | 停车记录列表（分页 + 状态筛选） |
| PUT | `/api/admin/vehicle-record/{id}` | 编辑停车记录 |
| POST | `/api/admin/vehicle-record/{id}/force-exit` | 强制出场 |
| **充值/扣费** | | |
| GET | `/api/admin/recharge-records` | 充值记录（分页） |
| GET | `/api/admin/payment-records` | 扣费记录（分页） |
| **月卡管理** | | |
| GET | `/api/admin/monthly-passes` | 月卡列表（关键词匹配车牌/昵称/手机号/学号工号 + 状态/卡类型筛选） |
| GET | `/api/admin/monthly-passes/stats` | 月卡经营统计（生效中/7 天内到期/已过期/已退款/累计与本月营收） |
| **车位管理** | | |
| GET | `/api/admin/spots` | 车位列表（分页，可选 `spotType`：0 固定车位 / 1 潮汐临时车位） |
| POST | `/api/admin/spot` | 新增车位 |
| PUT | `/api/admin/spot/{id}` | 编辑车位 |
| PUT | `/api/admin/spot/{id}/status` | 修改车位状态 |
| GET | `/api/admin/spot-map` | 车位地图：每个车位含状态、校园分区（zoneCode / 可停身份 / 区域免费时长）、**在停车辆详情**（车牌、入场时间、已停时长、预估费用、车主昵称/手机号、停车记录号）与**进行中预约详情**（预约编号、时段、时长、金额） |
| **停车区域管理** | | |
| GET | `/api/admin/zones` | 停车区域列表（含各区域车位数、内置标记） |
| POST | `/api/admin/zone` | 新建停车区域（编码创建后不可修改） |
| PUT | `/api/admin/zone/{id}` | 修改区域名称/说明/排序 |
| PUT | `/api/admin/zone/{id}/status` | 启用 / 停用（内置 tidal 区域禁止停用） |
| DELETE | `/api/admin/zone/{id}` | 删除区域（区域下仍有车位时拒绝） |
| **学院管理** | | |
| GET | `/api/admin/departments` | 学院/部门列表（全部，含停用） |
| POST | `/api/admin/department` | 新增学院/部门 |
| PUT | `/api/admin/department/{id}` | 编辑学院/部门（改名会同步该学院下用户） |
| PUT | `/api/admin/department/{id}/status` | 启用/停用 |
| DELETE | `/api/admin/department/{id}` | 删除（学院下有用户时禁止） |
| GET | `/api/departments` | **公开接口**：启用中的学院/部门（注册页与账号信息页下拉，无需登录） |
| **系统配置** | | |
| GET | `/api/admin/config` | 获取所有配置项（含校园规则配置） |
| PUT | `/api/admin/config` | 批量更新配置项（可在线调整学生折扣、教师免费时长、每日预约上限、夜间禁停、假期模式等） |
| **校园身份审核** | | |
| PUT | `/api/admin/user/{id}/identity` | 学号/工号认证审核（`identityStatus` 1通过 2驳回 0待审核，`remark` 审核意见） |
| **访客与家长报备** | | |
| GET | `/api/admin/visit-reports` | 报备列表（状态/车牌/姓名筛选，含报备人姓名与分配车位） |
| GET | `/api/admin/visit-reports/stats` | 报备统计（待审核/已通过/在停/已完成/访客区空闲车位/家长免费时长） |
| PUT | `/api/admin/visit-report/{id}/approve` | 审核通过（可指定 `spotId`，默认自动分配访客区空闲车位并创建访客账号） |
| PUT | `/api/admin/visit-report/{id}/reject` | 驳回（`remark` 驳回原因） |
| POST | `/api/admin/visit-report/{id}/check-in` | 访客/家长车辆登记入场 |
| POST | `/api/admin/visit-report/{id}/check-out` | 出场结算（按身份免费时长与费率，超时自动记违规） |
| **违规记录** | | |
| GET | `/api/admin/violations` | 违规记录列表（状态/车牌筛选） |
| GET | `/api/admin/violations/stats` | 违规统计（总数/待处理/已处理） |
| GET | `/api/admin/violation/{id}/handle` | 处理违规记录（附处理意见） |
| **校园统计与报表** | | |
| GET | `/api/admin/campus-stats` | 校园统计：`totals` 总览 + `byIdentity` 按身份 + `byDepartment` 按院系 + `byZone` 按分区（含分区收入） |
| GET | `/api/admin/export/{type}` | 导出 CSV 报表（`type`：users / reservations / payments / violations / visits，UTF-8 + BOM） |
| **黑白名单** | | |
| GET | `/api/admin/blacklist` | 黑名单列表（车牌/原因搜索，`status`：active/expired/all，`source`：manual/auto_violation） |
| POST | `/api/admin/blacklist` | 加入黑名单（车牌或用户、原因、有效期；记录来源与操作人） |
| DELETE | `/api/admin/blacklist/{id}` | 移出黑名单 |
| GET | `/api/admin/whitelist` | 白名单列表（车牌/原因搜索，`status`：active/expired/all） |
| POST | `/api/admin/whitelist` | 加入白名单（车牌或用户、原因、有效期） |
| DELETE | `/api/admin/whitelist/{id}` | 移出白名单 |
| **潮汐车位（临时车位批次）** | | |
| GET | `/api/admin/tidal-batches` | 批次列表（分页 + 状态筛选 + 名称/区域搜索，含 phase/statusLabel 与空闲·占用·预约计数） |
| GET | `/api/admin/tidal-batch/{id}` | 批次详情（含该批次全部车位明细） |
| POST | `/api/admin/tidal-batch` | 新建批次：按「编号前缀 + 数量」批量生成临时车位（`spotPrefix`/`spotCount`/`openTime`/`closeTime`/`pricePerHour`/`allowIdentities`/`freeMinutes`/`area`/`floor`/`remark`） |
| PUT | `/api/admin/tidal-batch/{id}` | 修改批次：同步更新时段与价格到全部车位；车位数增加则追加车位、减少则从编号最大的空闲车位开始删除（会删到在场/已预约车位时拒绝）；把已结束批次的结束时间改到未来可重新开放 |
| PUT | `/api/admin/tidal-batch/{id}/cancel` | 取消批次（存在在场车辆或进行中预约时拒绝）；取消后批次车位全部置为「维护中」并立即对用户端不可见 |
| DELETE | `/api/admin/tidal-batch/{id}` | 删除批次（仅当该批次未产生任何预约/停车记录，且车位均不在场、未被预约时允许） |
| PUT | `/api/admin/tidal-batch/{id}/restore` | 恢复已取消批次（按开放/结束时间重算状态，自动恢复车位） |
| PUT | `/api/admin/tidal-batch/{id}/soft-delete` | 软删除已取消批次（仅从列表隐藏，车位与历史保留） |
| DELETE | `/api/admin/tidal-batch/{id}/force` | 彻底删除已取消批次（物理删除批次与车位，存在在场/预约时拒绝） |
| **公告管理** | | |
| GET | `/api/admin/announcements` | 公告列表（分页 + 标题/正文关键字 + 状态筛选，**含草稿**） |
| GET | `/api/admin/announcement/{id}` | 公告详情 |
| POST | `/api/admin/announcement` | 新建公告（`multipart/form-data`：title / content / status / 可选 file），status 1 立即发布、0 存为草稿 |
| PUT | `/api/admin/announcement/{id}` | 修改公告（`multipart/form-data`：字段同上；传新 file 则替换附件，传 `removeAttachment=true` 则移除现有附件） |
| PUT | `/api/admin/announcement/{id}/status?status=1` | 发布 / 下架 |
| DELETE | `/api/admin/announcement/{id}` | 删除公告（同时删除其附件文件） |
| **账号与权限** | | |
| GET | `/api/admin/accounts` | 管理员账号列表（分页 + 关键字 + 状态筛选） |
| POST | `/api/admin/account` | 新增管理员账号 |
| PUT | `/api/admin/account/{id}` | 编辑账号（昵称/角色/状态） |
| DELETE | `/api/admin/account/{id}` | 删除账号（超级管理员账号与自身账号受保护） |
| PUT | `/api/admin/account/{id}/reset-password` | 重置密码 |
| PUT | `/api/admin/account/{id}/status` | 启用 / 停用 |
| GET | `/api/admin/account/my-menus` | 当前登录账号可访问的菜单（前端据此渲染侧边栏） |
| **角色管理（管理员 RBAC）** | | |
| GET | `/api/admin/roles` | 角色列表（含内置标记与账号数） |
| POST | `/api/admin/role` | 新建角色（编码创建后不可修改） |
| PUT | `/api/admin/role/{id}` | 修改角色名称 / 描述 / 状态 |
| DELETE | `/api/admin/role/{id}` | 删除角色（内置角色或仍有账号绑定时拒绝） |
| GET / PUT | `/api/admin/role/{id}/menus` | 查询 / 分配角色可访问菜单 |
| **个人中心** | | |
| GET / PUT | `/api/admin/profile` | 查看 / 修改本人资料（昵称），不受菜单权限限制 |
| PUT | `/api/admin/profile/password` | 修改本人密码（校验原密码） |
| **日志审计** | | |
| GET | `/api/admin/login-logs` | 管理员登录日志（分页 + 关键字 + 时间范围） |
| GET | `/api/admin/login-log/{id}` | 登录日志详情 |
| GET | `/api/admin/operation-logs` | 管理员操作日志（分页 + 关键字 + 时间范围） |
| GET | `/api/admin/operation-log/{id}` | 操作日志详情 |

### 统一响应格式

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

| code | 说明 |
|------|------|
| 200 | 成功 |
| 401 | 未认证（Token 无效/过期） |
| 500 | 服务器内部错误 |
| 其他 | 业务异常（具体错误信息见 message） |

### 鉴权方式

除认证接口和公开概览接口外，所有接口需在请求头携带 Token：

```
Authorization: Bearer <token>
```

- 用户 Token 有效期：24 小时（`parking.jwt.expiration: 86400000`）
- 管理员 Token 有效期：2 小时（`parking.jwt.admin-expiration: 7200000`）

---

## 快速开始

### 环境要求

| 环境 | 版本要求 | 说明 |
|------|---------|------|
| JDK | 21 或更高 | 后端编译运行 |
| Maven | 3.8+ | 后端构建 |
| MySQL | 8.0+ | 数据存储 |
| Redis | 7.0+ | 缓存 & 验证码 |
| Node.js | 18+ | 管理后台构建 |
| 微信开发者工具 | 最新稳定版 | 小程序开发调试 |

### 后端启动

**1. 初始化数据库**

导入**单文件数据库脚本**（20 张核心业务表 + 演示数据 + 管理员/用户 RBAC + 停车区域 + 索引，可直接还原到运行状态）：

> ⚠️ 仓库**不包含** `docs/parking_system.sql`：该文件是运行库完整导出，含真实用户数据与日志，已列入 `.gitignore`。请自行导出后放到 `docs/parking_system.sql`——需要演示数据时导出全量，只需要表结构时用 `mysqldump --no-data parking_system > docs/parking_system.sql`。

```bash
mysql -u root -p < docs/parking_system.sql
```

> 脚本内已含 `CREATE DATABASE IF NOT EXISTS parking_system` 与 `USE parking_system`，无需手工建库，也**无需再执行任何额外 schema 脚本**（原 `schema_admin_management.sql` / `schema_user_rbac.sql` / `schema_indexes.sql` 已全部整合）。
> 早期的拆分脚本（`parking.sql` / `migration-campus.sql` / `migration-campus-area-visitor.sql` / `migration-department.sql` / `seed-demo-data.sql`）也已全部合并进该文件；若你手上是旧版数据库，直接重新导入该文件覆盖即可。

**已有运行库升级黑白名单功能**（无需重新导入全量脚本，执行一次即可）：

```sql
CREATE TABLE IF NOT EXISTS `t_whitelist` (
  `id` bigint NOT NULL COMMENT '白名单ID(雪花)',
  `plate_number` varchar(20) DEFAULT NULL,
  `user_id` bigint DEFAULT NULL,
  `reason` varchar(255) DEFAULT '',
  `until_time` datetime DEFAULT NULL COMMENT 'NULL 表示永久',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `source` varchar(20) NOT NULL DEFAULT 'manual' COMMENT 'manual / system',
  `operator_id` bigint DEFAULT NULL,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_wl_plate` (`plate_number`),
  KEY `idx_wl_user` (`user_id`),
  KEY `idx_wl_until` (`until_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='白名单';

ALTER TABLE `t_blacklist`
  ADD COLUMN `source` varchar(20) NOT NULL DEFAULT 'manual' COMMENT 'manual / auto_violation',
  ADD COLUMN `operator_id` bigint DEFAULT NULL,
  ADD COLUMN `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  ADD KEY `idx_bl_source` (`source`);

INSERT IGNORE INTO `t_system_config` (`config_key`, `config_value`, `description`) VALUES
('blacklist_auto_enabled', '1', '是否启用违规累计自动拉黑：1 启用 0 关闭'),
('blacklist_auto_violation_threshold', '3', '统计窗口内达到该违规次数后自动拉黑'),
('blacklist_auto_window_days', '30', '违规次数统计窗口（天）'),
('blacklist_auto_duration_days', '30', '自动拉黑持续天数，0 表示永久');
```

> 若 `t_blacklist` 已通过旧版 `ALTER` 加过上述字段，请拆分执行、跳过已存在的列。

或者手动创建数据库后导入：

```sql
CREATE DATABASE IF NOT EXISTS parking_system
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE parking_system;
SOURCE docs/parking_system.sql;
```

**2. 修改配置**

编辑 `parking-server/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/parking_system?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root        # 改为你的 MySQL 用户名
    password: your-mysql-password      # 改为你的 MySQL 密码
  data:
    redis:
      host: localhost     # Redis 地址
      port: 6379
      password: your-redis-password  # Redis 密码
```

**3. 启动项目**

```bash
cd parking-server

# 编译打包
mvn clean package -DskipTests

# 启动
mvn spring-boot:run

# 或直接运行 JAR
java -jar target/parking-server-1.0.0.jar
```

**4. 验证**

- API 服务：http://localhost:8080
- Swagger 文档：http://localhost:8080/doc.html
- 健康检查：http://localhost:8080/api/auth/captcha（返回验证码即正常）

### 管理后台启动

```bash
cd parking-admin

# 安装依赖
npm install

# 开发模式运行（默认端口 3000）
npm run dev

# 生产构建
npm run build
```

开发模式下，Vite 会自动将 `/api` 请求代理到 `http://localhost:8080`（配置见 `vite.config.ts`）。

访问地址：**http://localhost:3000**

> **注意**：首次使用需要数据库中 `t_admin` 表存在管理员账号。如果没有，可以手动插入一条记录（密码用 BCrypt 加密），或通过后端初始化脚本创建。

### 微信小程序启动

1. 打开**微信开发者工具**
2. 选择"导入项目"
3. 项目目录选择 `parking-miniapp/`
4. 填入 AppID（或使用测试号）
5. 在 `app.js` 中确认 `baseUrl` 指向后端地址：

```javascript
// parking-miniapp/app.js
App({
  globalData: {
    // 开发环境：后端地址
    baseUrl: 'http://localhost:8080/api'
  }
})
```

> **提示**：微信小程序开发时，需在微信公众平台配置服务器域名白名单，开发阶段可以在开发者工具中勾选"不校验合法域名"。

---

## 配置说明

### 后端核心配置 (`application.yml`)

```yaml
parking:
  jwt:
    secret: <your-jwt-secret>        # JWT 签名密钥（生产环境务必修改）
    expiration: 86400000             # 用户 Token 过期时间（毫秒），默认24小时
    admin-expiration: 7200000        # 管理员 Token 过期时间（毫秒），默认2小时
  reservation:
    grace-period-minutes: 15         # 预约宽限期：结束后延迟多久自动过期（分钟）
```

### 管理后台配置

| 文件 | 说明 |
|------|------|
| `vite.config.ts` | 开发服务器端口（3000）、API 代理目标 |
| `src/utils/request.ts` | Axios 基地址、Token 注入、响应拦截 |
| `src/router/index.ts` | 路由守卫（未登录跳转登录页） |

### 小程序配置

| 文件 | 说明 |
|------|------|
| `utils/request.js` → `LAN_HOST` / `PORT` | **服务器地址的唯一来源**：开发者工具自动用 `http://localhost:8080`，真机预览用 `http://<LAN_HOST>:8080`。仓库中 `LAN_HOST` 为占位值 `localhost`，真机调试时请在本地改成自己的局域网 IP，**改动不要提交** |
| `app.js` → `globalData.baseUrl` | 从 `utils/request.js` 导入，不再单独硬编码（两处地址不一致会出现「部分页面能连、部分页面超时」） |
| `app.json` | TabBar（首页 / 公告 / 我的）、窗口样式、页面路由 |
| `project.config.json` | 微信开发者工具配置 |

#### 连不上后端时怎么排查

1. **先确认后端在跑**：浏览器或命令行访问 `http://localhost:8080/api/announcement/list` 应返回 JSON。
2. **换网络后 IP 会变**：执行 `ipconfig`，找到 **WLAN 适配器的 IPv4 地址**，把它填到 `utils/request.js` 的 `LAN_HOST`。
   - 开发者工具里不受影响（自动走 `localhost`），只有**真机预览/体验版**依赖 `LAN_HOST`。
3. **临时覆盖（不改代码）**：在开发者工具 Console 执行一次，然后重新编译小程序：
   ```js
   wx.setStorageSync('serverBase', 'http://<你自己的局域网IP>:8080')  // 不要带 /api
   wx.removeStorageSync('serverBase')                                  // 清除覆盖
   ```
4. **手机与电脑必须在同一局域网**；Windows 防火墙需放行 8080（首次运行 Java 时会弹窗，选「允许访问」）。
5. **真机下载附件还需配置合法域名**：微信要求 HTTPS，`http://<局域网IP>` 无法用于正式版 `downloadFile`；开发阶段请在「详情 → 本地设置」勾选 **不校验合法域名**，或在公众平台配置 downloadFile 合法域名。

---

## 部署指南

### 后端生产部署

```bash
# 1. 打包
cd parking-server
mvn clean package -DskipTests

# 2. 上传 JAR 到服务器
scp target/parking-server-1.0.0.jar user@server:/opt/parking/

# 3. 生产环境配置（通过外部配置文件或环境变量）
java -jar parking-server-1.0.0.jar \
  --spring.datasource.url=jdbc:mysql://<db-host>:3306/parking_system \
  --spring.datasource.username=<db-user> \
  --spring.datasource.password=<db-password> \
  --spring.data.redis.host=<redis-host> \
  --spring.data.redis.password=<redis-password> \
  --parking.jwt.secret=<your-production-secret>
```

### 管理后台生产部署

```bash
cd parking-admin
npm run build
# 将 dist/ 目录部署到 Nginx / Apache 等 Web 服务器
```

Nginx 配置示例：

```nginx
server {
    listen 80;
    server_name admin.parking.com;

    root /var/www/parking-admin/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # 用户头像等上传文件（后端 uploads 静态目录）
    location /uploads {
        proxy_pass http://127.0.0.1:8080;
        proxy_set_header Host $host;
    }

    # 车位状态实时推送（WebSocket）
    location /ws {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
        proxy_set_header Host $host;
        proxy_read_timeout 120s;
    }
}
```

### 小程序部署

在微信开发者工具中点击"上传"，提交审核后发布即可。

---

## 开发说明

### 代码规范

- **后端**：参考若依（RuoYi）的分层架构，`admin` 与 `miniapp` 各自包含独立的 Controller + Service，`system` 存放两端共享的领域对象、Mapper 与业务服务，`framework` 放配置/拦截器/切面，`common` 放通用工具；管理端与小程序端共用同一个启动类与端口，统一使用 `R<T>` 作为响应格式
- **管理后台**：Vue 3 组合式 API（`<script setup lang="ts">`）+ TypeScript + Element Plus 组件库，路由由 Vue Router 管理（含登录守卫与路由级懒加载），全局状态用 Pinia，图表统一走 `components/EChart.vue`
- **管理端主题**：白色浅色主题。`styles/global.css` 中以 `:root` 定制 Element Plus 浅色 CSS 变量（主色 `#409eff`），页面、侧边栏、顶栏均为纯白底色，卡片/面板用 `#ebeef5` 边框与浅阴影区分层次，未引入 `theme-chalk/dark/css-vars.css`
- **小程序**：原生微信小程序语法（Page/App 风格），通过 `wx.request` 与后端通信

### 关键设计

1. **双 Token 体系**：用户 Token 和管理员 Token 独立签发，通过不同接口登录获取。JwtInterceptor 根据请求路径判断角色权限——`/api/admin/**` 路径要求 Token 中 `role=admin`，普通用户接口要求 `role=user`
2. **无状态鉴权**：JwtInterceptor 从请求头 `Authorization: Bearer <token>` 提取 Token，解析后将 `userId`、`phone`、`role` 存入 ThreadLocal（UserContext），请求完成后自动清理，全程不依赖 Session
3. **余额乐观扣款**：扣费使用 SQL 乐观锁 `UPDATE t_user SET balance = balance - #{amount} WHERE id = #{userId} AND balance >= #{amount}`，在 Mapper 层判断影响行数，防止并发场景下超额扣款
4. **预约过期机制**：`ScheduledTaskService` 每 60 秒扫描一次，将 `endTime + gracePeriodMinutes(15分钟)` 已过的活跃预约自动标记为过期（status=4），同时释放对应车位（status 从 2 重置为 0）
  - **预约时间约束（禁止预约过去时段）**：前端 `utils/timeSlot.js` 按 30 分钟粒度生成候选时段，选择「今天」时只保留「当前时间 + 10 分钟」之后的时段（历史时段不会出现在 picker 里，因此无法查看与选择）；今天已无可用时段时自动切换到次日；提交前再做一次本地时间戳校验。服务端 `ReservationServiceImpl.createReservation` 兜底校验 `startTime` 不得早于当前时间、结束时间必须晚于开始时间、时长 1 小时 ~ 30 天，防止绕过前端直接调接口
  - **校园规则引擎**：`CampusRuleService` 集中管理身份相关规则——① 身份差异化费率（学生五折 `rate_student_discount`、访客加价 `rate_visitor_multiplier`、假期统一折扣 `holiday_discount`）；② 身份免费时长（教师 120 / 职工 60 / 家长 30 / 学生·访客 15 分钟），出场结算先从停放时长中扣减免费时长再按「不足 1 小时按 1 小时」计费；③ 预约约束（**身份未认证不可预约**、区域权限、每日预约上限、夜间禁停 `23:00-06:00`、假期模式关闭教学区/图书馆区预约、黑名单拦截）；④ 规则值全部存放在 `t_system_config`，管理员可在「系统配置」在线调整，无需改代码
  - **校园分区与访客/家长临时停车**：车位新增 `zone_code` 与 `allow_identities`，分为教学楼区(A区)/图书馆区(B区)/宿舍区(C区)/访客与家长临时停车区(V区，仅 4/5 身份)；小程序预约列表按当前身份过滤可停区域。师生可在小程序为家长或访客**提前报备车牌**，管理员审核通过后系统自动创建访客账号并分配 V 区车位，访客到达校门由管理员「登记入场」，出场时家长享受 30 分钟免费、超时按访客费率计费；超过预计离校时间 + 宽限期自动生成违规记录（`t_violation`）
  - **潮汐（临时）车位**：面向开学迎新、考试、比赛等高峰期的临时车位。管理员在「潮汐车位」页新建批次，按「编号前缀 + 数量」批量生成 `spot_type=1` 的车位并设定 `open_time` / `close_time`（精确到分钟，存 `t_tidal_batch`，同时冗余到每个车位）。**可见性与门禁一律按时间实时判定，不依赖定时任务**：`/api/spot/tidal` 返回 `open_time - tidal_reserve_advance_days`（默认 7 天，可配置）至 `close_time` 之间的批次并给出 `phase`（upcoming 仅可预约 / open 可预约可入场）；`/api/spot/list` 只在 `[open_time, close_time]` 区间内才把潮汐车位算作可用车位，因此**到达开放时间后它们会自动出现在小程序的可入场车位里**；`ReservationServiceImpl` 与 `VehicleServiceImpl` 分别调用 `TidalSpotService.validateReservationWindow / validateEnterWindow` 做服务端兜底（开放前预约会被拒、不可入场；预约时段必须完整落在开放时间段内；潮汐车位不受全局夜间禁停限制，以管理员设定的时段为准）。`ScheduledTaskService` 每 60 秒同步批次状态（待开放→开放中→已结束），并把已结束批次中仍空闲的车位置为「维护中」，避免继续被统计为可用车位；取消批次时同样把车位置为维护中而非删除，以保留预约/停车历史的可追溯性
5. **车辆出场自动结算**：出场接口计算 `(出场时间 - 入场时间)` 得出停车时长和费用 → 乐观锁扣款 → 生成 `t_payment_record` 扣费记录 → 释放车位 → 更新 `t_vehicle_record` 状态
6. **预约提前离场按预约结算**：预约车辆可提前离场，出场结算费用取 `max(实际停车费, 预约金额)`——预约时段已被锁定，即使提前离场也按预约金额结算（扣费类型 `reservation_settle`）；超时停车则按实际时长计费（多占多付）。`/api/vehicle/current` 会返回关联预约金额 `reservationAmount` 供前端展示
7. **分布式雪花ID**：所有实体主键使用 MyBatis-Plus 的 `IdType.ASSIGN_ID`（基于 Hutool 的 Snowflake 算法），不同实体通过不同 workerId/datacenterId 组合生成全局唯一 ID
8. **BCrypt 密码加密**：所有密码（用户 + 管理员）使用 `BCryptPasswordEncoder` 加密存储，原始密码永不出现在数据库
9. **微信登录模拟**：生产环境微信 OAuth 待对接，当前开发阶段生成模拟 `openid = "wx_" + code` 用于调试
9. **CORS 跨域**：`WebConfig` 中配置所有来源对 `/api/**` 的跨域请求放行，方便前后端分离开发
10. **验证码防护**：登录/注册接口需验证图形验证码，验证码通过 Easy Captcha 生成，Base64 图片 + UUID Key 返回，验证码文本以 Key 为键存储在 Redis 中（5分钟过期）
11. **公告附件存储**：附件保存在 `parking-server/uploads/announcement/`（`parking.upload-dir` 的子目录，与头像同级），磁盘文件名用雪花 ID + 扩展名（**纯 ASCII**），原始文件名单独存库，下载接口用 `Content-Disposition: attachment; filename="…"; filename*=UTF-8''…` 还原中文名 —— 这样既避免了中文文件名在 URL/下载头里的编码问题，也不会互相覆盖。上传时做三重校验：扩展名白名单（**刻意排除 html/htm/js/svg/xml 等可被浏览器渲染执行的类型**，因为 uploads 是静态目录直接暴露的）、单个 ≤ 20MB、保存路径必须落在附件目录内（防路径穿越）；替换与删除附件时同步清理旧文件。`/api/announcement/**` 在 JwtInterceptor 白名单中（公告为公开信息），`/api/admin/announcement*` 仍要求管理员角色；上传接口单独把前端 axios 超时放宽到 120s（20MB 附件在慢网络下可能超过默认 15s）。
12. **车位状态 Redis 缓存与高峰削峰**：`SpotCacheService` 以 Redis Hash（key `parking:spot:status`，field = spotId）缓存全量车位状态，服务启动时预热；高峰时段预约/入场先在 Redis 完成「空闲→已预约」「空闲/已预约→已占用」的原子状态流转，再通过 `@Async("parkingSyncExecutor")` 异步回写 MySQL，调用方不阻塞；非高峰时段仍走原有数据库条件更新流程；Redis 不可达或缓存缺失时自动穿透回 MySQL 并重建，**缓存故障不影响业务正确性**。是否高峰由 `cache_peak_hours` 配置判定，未配置时视为全程高峰。固定车位与潮汐车位共用同一张缓存（仅靠 `spot_type` 区分业务规则）。
13. **管理员菜单权限与操作审计**：`t_admin.role` 区分 `super_admin` / `admin`；`t_admin_menu` 保存账号可访问的菜单编码，`common/menu/MenuPermissionMapping` 维护「请求路径 → 菜单编码」映射，`MenuAuthInterceptor` 在请求进入业务层前统一校验并返回 403；前端登录后拉取 `/api/admin/account/my-menus` 只渲染被授权的菜单。敏感操作由 `common/annotation/OperationLog` 注解标记 `OperationType`，`OperationLogAspect` 环绕采集操作人、菜单、类型、URI、IP、参数、响应状态与耗时，在 finally 块独立落库（采集失败仅打 warn，绝不影响业务）；管理员登录结果单独记录到 `t_admin_login_log`。
14. **潮汐（临时）车位的收尾策略**：`ScheduledTaskService` 每 60 秒同步批次状态（待开放→开放中→已结束），并把已结束批次中**仍空闲**的车位置为「维护中」，避免它们继续被统计为可用车位；因此在场车辆与已被预约的车位不会被误停用（预约到期后由预约过期任务释放，再于下一轮被收尾）。管理员把已结束批次的结束时间改到未来即可重新开放，此时这些车位会被自动恢复为空闲。
15. **车位状态 WebSocket 实时推送**：新增 `/ws/spots` 原生 WebSocket 端点（管理端与小程序共用），握手时通过查询参数 `token` 完成 JWT 鉴权——管理员接收全量车位，小程序用户按 `UserVisibility`（区域 + 身份）与白名单过滤，维护中车位和未开放潮汐车位不推送。业务侧（入场/出场/预约/管理员操作/报备/潮汐批次/定时任务/缓存回写）仅调用 `notifyChanged()` 置脏标记，`SpotRealtimeService` 每秒读库比对并广播 `patch`（只带 `{id,status}`），每 30 秒广播全量 `snapshot` 兜底，管理端附带 `counts` 汇总；客户端断线按 2s→4s→8s→16s→30s 退避重连。HTTP 轮询保留为兜底，连接正常时页面自动跳过。单实例会话保存在 JVM 内存，多实例部署可扩展 Redis Pub/Sub。

### 扩展方向

- [ ] 真实微信支付对接（当前为模拟支付）
- [ ] 车牌照识别（OCR/摄像头联动）
- [x] WebSocket 实时推送车位状态
- [ ] 多停车场支持
- [ ] 车位引导屏对接
- [ ] 语音播报出入场
- [ ] Docker 容器化部署
- [ ] 单元测试 & 集成测试覆盖

---

## License

MIT License

---
