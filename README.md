# 智能校园停车场管理系统（Smart Campus Parking Lot Management System）

一个完整的智慧校园停车场管理平台，包含**后端服务**、**管理员前端**和**用户微信小程序**三大模块，实现车辆进出管理、车位预约、在线支付、数据统计等核心功能。

---

## 目录

- [项目架构](#项目架构)
- [技术栈](#技术栈)
- [功能模块](#功能模块)
  - [管理员后台](#管理员后台)
  - [用户小程序](#用户小程序)
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
│  │  Spring Boot 3.3 + Java 21               │   │
│  │  ├─ Controller  (REST API)               │   │
│  │  ├─ Service     (业务逻辑)                │   │
│  │  ├─ Mapper      (MyBatis-Plus 数据访问)    │   │
│  │  ├─ Interceptor (JWT 鉴权)               │   │
│  │  └─ Config      (Security / Redis 等)     │   │
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
| React | 18.3+ | 前端框架 |
| TypeScript | 5.4+ | 类型安全 |
| Vite | 5.2+ | 构建工具 |
| React Router | 6.26+ | 路由管理 |
| Zustand | 4.5+ | 状态管理 |
| Ant Design | 5.21+ | UI 组件库 |
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
| **首页** | 核心数据看板：总车位数、空闲/占用/预约数量、今日收入、今日进出次数等统计指标 |
| **用户管理** | 查看注册用户列表（头像/昵称/**校园身份**/学号工号/手机号/车牌/余额/状态）、按**身份类型与认证状态**筛选、搜索、启用/禁用账号；**身份认证审核**：顶部「待审核身份」统计卡与列表「待审核身份 N 人」标签可一键筛选，**每一行都有「身份审核 / 复核·驳回」入口**（弹窗可选 通过认证 / 驳回 / 重置为待审核 + 审核意见）；侧边栏「用户管理」显示待审核数量徽章，首页「待审核身份」卡片可直接跳转；**用户信息抽屉**展示账号资料（校园身份、学号工号、院系班级、微信绑定、头像、注册/更新时间）与业务统计 |
| **访客与家长报备** | 师生提交的访客/家长车辆报备审核：通过（自动分配访客与家长临时停车区车位并创建访客账号）、驳回、**登记入场**、**出场结算**（按身份免费时长与费率），以及报备统计（待审核/已通过/在停/访客区空闲车位） |
| **违规记录** | 出场超时自动生成的违规记录（超时占用时长、扣分），支持筛选、处理与处理意见 |
| **车位管理** | 车位列表（分页，含**校园分区 / 可停身份 / 区域免费时长**列）、新增/编辑车位（可选校园分区、可停身份多选、区域免费时长）、修改车位状态（空闲/占用/已预约/维护中）、可视化车位地图（**点击车位卡片展开详情**：车位信息、在停车牌与入场时间/已停时长/预估费用/车主、进行中预约信息，并支持强制出场结算） |
| **预约管理** | 查看所有预约记录、按状态筛选、手动修改预约状态 |
| **车辆进出** | 查看停车记录（入场/离场时间、停车时长、费用）、手动编辑记录、强制出场操作 |
| **充值记录** | 查看用户充值明细（金额、支付方式、状态、时间） |
| **扣费记录** | 查看停车扣费明细（扣费金额、余额变动） |
| **校园统计与报表** | 首页展示：校园身份分布（学生/教师/职工/访客/家长环形图）、校园分区车位使用（堆叠柱状图）、校园管理概览（已认证/待审核身份、待审报备、待处理违规、黑名单数、累计营收）、**院系统计表**（用户数/认证数/预约次数/停车次数/消费金额）、分区车位与收入表；支持一键**导出 CSV 报表**（用户/预约/扣费/违规/访客报备，含 UTF-8 BOM，Excel 直接打开） |
| **车辆黑名单** | 新增/移除黑名单（车牌 + 原因 + 有效期，留空为永久），命中后该车辆无法预约车位与提交访客/家长报备 |
| **车辆黑名单** | 新增/移除黑名单（车牌 + 原因 + 有效期，留空为永久），命中后该车辆无法预约车位与提交访客/家长报备 |
| **学院管理** | 学院/部门维护：新增、编辑（名称/学院代码/联系人/联系电话/排序/备注）、启用停用、删除（学院下仍有用户时禁止删除）；**改名时自动同步该学院下用户的院系名称**；停用后不再出现在小程序注册与账号信息页的下拉列表。学生、教师、职工注册与修改资料时**必须从启用的学院列表中选择**，后端会校验 |
| **系统配置** | 动态系统参数配置（键值对形式）、支持在线修改；**校园规则**（学生折扣、教师/职工/家长/访客免费时长、访客加价系数、每日预约上限、夜间禁停时段、学期/假期模式、假期折扣、超时违规宽限期）均在此在线调整；保存时会**校验取值合法性**（如免费时长必须在 0~1440 分钟内），并在读取时做兜底限制 |

### 用户小程序

| 页面 | 功能 |
|------|------|
| **首页** | 实时概览：当前停放的车辆信息（车牌、已停时长）、活跃预约一览、各区域车位状态（空闲/占用/预约） |
| **登录** | 微信一键登录（获取 openid）、手机号注册/绑定、账号密码登录 |
| **预约车位** | 选择车位 → 选择开始日期时间（**仅可查看/选择当前时间之后的时段**：今天只列出「当前时间 + 10 分钟」起的时段；若今天已无可用时段则自动定位到明天）→ 选择时长（1小时~1个月）→ 自动计算费用 → 确认预约 |
| **预约详情** | 查看单条预约的完整信息（预约编号、车位号、时间段、金额、状态） |
| **入场登记** | 选择已预约车位快速入场，系统自动记录入场时间 |
| **出场结算** | 查看当前停车详情、实时计算停车费用、一键出场并自动从余额扣费 |
| **余额充值** | 展示充值套餐（50/100/200/500/1000元）、在线充值（模拟支付） |
| **停车记录** | 停车历史记录列表（车牌、入场/出场时间、时长、费用、支付状态） |
| **个人中心** | 用户信息展示（头像、昵称、余额、车牌）、快捷入口（我的预约、充值记录、扣费记录、停车记录等） |
| **账号信息** | 个人资料管理：更换/移除头像、修改昵称、手机号、车牌号；**校园身份**（学生/教师/职工/访客/家长 + 学号工号 + 院系班级，提交后由管理员审核）；**我的校园权益**（计费身份、费率系数、免费停车时长、今日预约剩余次数、夜间禁停时段、学期/假期模式）；**绑定/解绑微信** |
| **访客/家长报备** | 校内师生代来访家长或访客提前报备车牌与到校时段，审核通过后自动分配访客区车位；可查看报备状态（待审核/已通过/已入场/已完成）与撤销报备 |
| **我的预约** | 预约列表（车位编号与区域、预约时段、时长、金额、状态），支持查看详情、去入场、**取消预约**（自动释放车位） |
| **我的违规** | 个人违规记录（超时占用 / 违规区域等类型、扣分、说明、处理状态），并提示累计违规可能被列入车辆黑名单 |
| **修改密码** | 校验原密码后设置新密码（6-20 位），成功后需重新登录 |

---

## 项目结构

```
parking-system/
├── docs/
│   └── parking.sql                # 数据库初始化脚本（建表 + 种子数据）
├── parking-server/                # 后端服务（Spring Boot）
│   ├── pom.xml                    # Maven 依赖配置
│   └── src/main/
│       ├── java/com/parking/
│       │   ├── ParkingApplication.java       # 启动类
│       │   ├── common/                       # 通用模块
│       │   │   ├── R.java                    # 统一响应格式 {code, message, data}
│       │   │   ├── ResultCode.java           # 响应状态码常量
│       │   │   ├── BusinessException.java    # 业务异常类
│       │   │   ├── GlobalExceptionHandler.java # 全局异常处理
│       │   │   ├── UserContext.java          # 请求上下文（ThreadLocal 存储用户ID）
│       │   │   └── interceptor/
│       │   │       └── JwtInterceptor.java   # JWT 拦截器（校验 Token）
│       │   ├── config/                       # 配置类
│       │   │   ├── SecurityConfig.java       # Spring Security 配置（放行 + BCrypt）
│       │   │   ├── WebConfig.java            # Web MVC 配置（CORS + 拦截器注册）
│       │   │   ├── RedisConfig.java          # Redis 序列化配置
│       │   │   └── MyBatisPlusConfig.java    # MyBatis-Plus 分页插件配置
│       │   ├── util/                         # 工具类
│       │   │   └── JwtUtil.java              # JWT 生成/解析/校验
│       │   ├── controller/                   # REST 控制器
│       │   │   ├── AuthController.java       # 认证接口（登录/注册/验证码/微信登录）
│       │   │   ├── UserController.java       # 用户接口（信息/充值/记录查询）
│       │   │   ├── AdminController.java      # 管理后台接口（全模块CRUD）
│       │   │   ├── SpotController.java       # 车位公开接口（列表/概览）
│       │   │   ├── ReservationController.java # 预约接口（创建/取消/详情）
│       │   │   └── VehicleController.java    # 车辆进出接口（入场/出场/当前状态）
│       │   ├── service/                      # 业务逻辑层
│       │   │   ├── AuthService.java
│       │   │   ├── UserService.java
│       │   │   ├── AdminService.java
│       │   │   ├── SpotService.java
│       │   │   ├── ReservationService.java
│       │   │   ├── VehicleService.java
│       │   │   ├── CaptchaService.java
│       │   │   ├── ScheduledTaskService.java  # 定时任务（自动过期预约）
│       │   │   └── impl/                      # 服务实现类
│       │   ├── mapper/                       # MyBatis-Plus Mapper
│       │   │   ├── AdminMapper.java
│       │   │   ├── UserMapper.java
│       │   │   ├── ParkingSpotMapper.java
│       │   │   ├── ReservationMapper.java
│       │   │   ├── VehicleRecordMapper.java
│       │   │   ├── RechargeRecordMapper.java
│       │   │   ├── PaymentRecordMapper.java
│       │   │   └── SystemConfigMapper.java
│       │   ├── entity/                       # 数据库实体类
│       │   │   ├── User.java                 # 用户表 t_user
│       │   │   ├── Admin.java                # 管理员表 t_admin
│       │   │   ├── ParkingSpot.java           # 车位表 t_parking_spot
│       │   │   ├── Reservation.java           # 预约表 t_reservation
│       │   │   ├── VehicleRecord.java         # 停车记录表 t_vehicle_record
│       │   │   ├── PaymentRecord.java         # 扣费记录表 t_payment_record
│       │   │   ├── RechargeRecord.java        # 充值记录表 t_recharge_record
│       │   │   └── SystemConfig.java          # 系统配置表 t_system_config
│       │   └── dto/                          # 数据传输对象
│       │       ├── LoginDTO.java
│       │       ├── RegisterDTO.java
│       │       ├── AdminLoginDTO.java
│       │       ├── WechatLoginDTO.java
│       │       ├── VehicleEnterDTO.java
│       │       ├── ReservationDTO.java
│       │       ├── RechargeDTO.java
│       │       ├── UpdateUserDTO.java
│       │       ├── PasswordDTO.java
│       │       ├── VisitReportDTO.java
│       │       ├── BlacklistDTO.java
│       │       └── SpotOverviewDTO.java
│       └── resources/
│           └── application.yml               # 主配置文件
│
├── parking-admin/                  # 管理员前端（React 18 + TS）
│   ├── package.json                # Node 依赖
│   ├── vite.config.ts              # Vite 配置（代理到8080后端）
│   ├── index.html
│   └── src/
│       ├── App.tsx                 # 根组件（RouterProvider）
│       ├── main.tsx                # 入口
│       ├── api/
│       │   ├── index.ts            # 所有管理后台 API 封装
│       │   └── auth.ts             # 登录认证 API
│       ├── router/
│       │   └── index.tsx           # 路由配置 + 登录守卫
│       ├── stores/
│       │   └── user.ts             # 用户状态（Zustand）
│       ├── utils/
│       │   └── request.ts          # Axios 实例（拦截器、Token 注入）
│       ├── layouts/
│       │   └── MainLayout.tsx      # 主布局（侧边栏 + 顶栏）
│       ├── components/
│       │   └── SpotMap.tsx         # 车位地图可视化组件（卡片显示车牌 + 点击展开车位详情抽屉）
│       └── views/                  # 页面视图
│           ├── login/index.tsx     # 登录页
│           ├── dashboard/index.tsx # 首页/数据看板
│           ├── user/index.tsx      # 用户管理
│           ├── spot/index.tsx      # 车位管理
│           ├── reservation/index.tsx # 预约管理
│           ├── vehicle/index.tsx   # 车辆进出记录
│           ├── recharge/index.tsx  # 充值记录
│           ├── payment/index.tsx   # 扣费记录
│           └── config/index.tsx    # 系统配置
│
└── parking-miniapp/                # 微信小程序（原生框架）
    ├── app.js                      # 小程序入口（globalData: baseUrl）
    ├── app.json                    # 全局配置（路由/TabBar/窗口样式）
    ├── app.wxss                    # 全局样式
    ├── project.config.json         # 微信开发者工具项目配置
    ├── project.private.config.json # 开发者工具本地私有配置
    ├── sitemap.json                # 站点地图
    ├── utils/
    │   ├── request.js              # 网络请求封装（wx.request + Token 管理 + 文件上传）
    │   ├── device.js               # 微信登录/绑定：稳定 deviceId 与 wx.login code
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
| phone | VARCHAR(20) | 手机号 |
| password | VARCHAR(128) | 密码（BCrypt 加密） |
| plate_number | VARCHAR(16) | 默认车牌号 |
| balance | DECIMAL(10,2) | 账户余额 |
| status | TINYINT | 状态（0-禁用, 1-正常） |
| created_at | DATETIME | 注册时间 |
| updated_at | DATETIME | 更新时间 |

#### 2. `t_admin` — 管理员表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| username | VARCHAR(32) | 登录用户名 |
| password | VARCHAR(128) | 密码（BCrypt 加密） |
| nickname | VARCHAR(32) | 显示昵称 |
| role | VARCHAR(16) | 角色 |
| status | TINYINT | 状态 |
| created_at | DATETIME | 创建时间 |

#### 3. `t_parking_spot` — 车位表

| 字段 | 类型 | 说明 |
|------|------|------|
| id | BIGINT | 主键（雪花ID） |
| spot_code | VARCHAR(16) | 车位编号（如 A-001） |
| area | VARCHAR(32) | 所属区域（如 A区） |
| floor | VARCHAR(16) | 楼层 |
| status | TINYINT | 状态：0-空闲, 1-占用, 2-已预约, 3-维护中 |
| price_per_hour | DECIMAL(10,2) | 每小时价格（元） |
| remark | VARCHAR(255) | 备注 |
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

### ER 关系图

```
t_user ──1:N──→ t_reservation ──N:1──→ t_parking_spot
  │                  │
  │                  └──N:1──→ t_vehicle_record
  │
  ├──1:N──→ t_recharge_record
  ├──1:N──→ t_payment_record
  └──1:N──→ t_vehicle_record

t_admin (独立)
t_system_config (独立，键值对存储)
```

### 种子数据

项目提供了完整的数据库初始化脚本 `docs/parking.sql`，包含以下预设数据：

| 数据 | 内容 |
|------|------|
| **默认管理员** | 用户名 `admin`，密码 `admin123`，角色 `super` |
| **演示用户** | 手机号 `13800138000`，密码 `123456`，余额 ¥500.00，车牌 `粤B12345` |
| **350 个车位** | 教学楼区 (B1, A-001~A-015, ¥5/时) + 图书馆区 (B2, B-001~B-015, ¥4/时) + 宿舍区 (B3, C-001~C-020, ¥3/时) + 访客与家长临时停车区 (B1~B3, V-001~V-300, ¥6/时) |
| **7 个学院/部门** | 计算机与信息工程学院(CS)、机械与电气工程学院(ME)、经济管理学院(EM)、外国语学院(FL)、体育学院(PE)、图书馆(LIB)、后勤保障处(LOG) |
| **系统配置** | 默认小时费率 5.00 元、最大预约 30 天、最小预约 1 小时、宽限期 15 分钟 + 16 项校园规则 |

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

### 演示数据一键生成（`docs/seed-demo-data.sql`）

用于答辩/演示时快速铺满各页面数据，可**重复执行**（脚本会先释放并清理上一次的演示数据）：

```bash
mysql -u root -p parking_system < docs/seed-demo-data.sql
```

生成内容（实测）：

| 数据 | 数量 | 覆盖状态 |
|------|------|----------|
| 违规记录 | 5 条 | 超时占用 3（2 待处理 / 1 已处理）、违规区域 1（未处理）、占用他人车位 1（已处理） |
| 车辆黑名单 | 4 条 | 永久车牌 2、临时车牌 1（30 天）、按用户限制 1（90 天） |
| 访客与家长报备 | 8 条 | 待审核 2、已通过 2（已分配 V-002/V-003）、已入场 1（占用 V-001）、已完成 1、已驳回 1、已撤销 1 |
| 预约记录 | 4 条 | 进行中 2、已完成 1、已过期 1 |
| 停车记录 | 10 条 | 在停 3（A-001 / C-001 / V-001）、历史出场 7（含费用与扣费记录） |
| 充值 / 扣费 | 5 / 7 条 | 微信、支付宝；自动扣费、预约结算、管理端强制出场 |
| 演示账号 | 5 个 | 2 名学生（有手机号可登录）+ 3 个访客/家长账号（用于报备入场结算） |

数据一致性：占用车位数与在停车辆数一致、同一车位不会同时被预约和占用、总车位 350（空闲 342 / 占用 3 / 已预约 4 / 维护 1）。

---

## API 接口文档

启动后端后，访问 Knife4j 在线文档：**http://localhost:8080/doc.html**

### 接口总览（共 6 组，30+ 接口）

#### Auth — 认证接口 (`/api/auth`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/auth/captcha` | 获取图形验证码（返回验证码图片 Base64 + 验证码Key） |
| POST | `/api/auth/register` | 用户注册（手机号 + 密码 + 验证码；**学生/教师/职工需填写学号/工号与院系，待管理员审核；访客/家长无需审核**） |
| POST | `/api/auth/login` | 用户登录（手机号 + 密码 + 验证码）→ 返回 JWT Token |
| POST | `/api/auth/admin/login` | 管理员登录（用户名 + 密码 + 验证码）→ 返回管理端 JWT Token |
| POST | `/api/auth/wechat-login` | 微信小程序登录（openid / 手机号 + 密码）→ 返回 JWT Token |

#### User — 用户接口 (`/api/user`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/user/info` | 获取当前用户信息 |
| PUT | `/api/user/info` | 更新用户信息（昵称、手机号、车牌号、头像、**校园身份：userNo / identityType / department / className**，身份变更后需重新审核） |
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

#### Spot — 车位接口 (`/api/spot`)

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/spot/list` | 可用车位列表（分页 + 按区域筛选） |
| GET | `/api/spot/overview` | 公开概览（所有车位分区状态 + 已停放车辆 + 活跃预约） |

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

> **入场门禁校验**（`/api/vehicle/enter`）：命中车辆黑名单直接拒绝；按身份校验可停区域（不匹配则拒绝入场并自动生成 `illegal_zone` 违规记录，扣 2 分）。

#### Admin — 管理后台接口 (`/api/admin`)

| 方法 | 路径 | 说明 |
|------|------|------|
| **首页** | | |
| GET | `/api/admin/dashboard` | 首页统计数据 |
| **用户管理** | | |
| GET | `/api/admin/users` | 用户列表（分页 + 搜索 + 状态筛选，含头像字段） |
| GET | `/api/admin/user/{id}` | 用户详情：账号资料（含头像、微信绑定、注册/更新时间）+ 业务统计（停车次数、在场车辆、预约数、充值/消费合计、最近停车） |
| PUT | `/api/admin/user/{id}/status` | 修改用户状态（启用/禁用） |
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
| **车位管理** | | |
| GET | `/api/admin/spots` | 车位列表（分页） |
| POST | `/api/admin/spot` | 新增车位 |
| PUT | `/api/admin/spot/{id}` | 编辑车位 |
| PUT | `/api/admin/spot/{id}/status` | 修改车位状态 |
| GET | `/api/admin/spot-map` | 车位地图：每个车位含状态、校园分区（zoneCode / 可停身份 / 区域免费时长）、**在停车辆详情**（车牌、入场时间、已停时长、预估费用、车主昵称/手机号、停车记录号）与**进行中预约详情**（预约编号、时段、时长、金额） |
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
| **车辆黑名单** | | |
| GET | `/api/admin/blacklist` | 黑名单列表（车牌/原因搜索） |
| POST | `/api/admin/blacklist` | 加入黑名单（车牌或用户、原因、有效期） |
| DELETE | `/api/admin/blacklist/{id}` | 移出黑名单 |

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
| Redis | 6.0+ | 缓存 & 验证码 |
| Node.js | 18+ | 管理后台构建 |
| 微信开发者工具 | 最新稳定版 | 小程序开发调试 |

### 后端启动

**1. 初始化数据库**

导入项目自带的 SQL 初始化脚本（包含建表语句和种子数据）：

```bash
mysql -u root -p < docs/parking.sql
```

> **已有数据库升级**：
> 1. 若 `t_user` 表没有 `avatar` 列（账号信息页无法保存头像），执行：
>
> ```sql
> ALTER TABLE t_user ADD COLUMN avatar VARCHAR(255) DEFAULT '' COMMENT 'Avatar url (/uploads/avatar/xxx.jpg)' AFTER plate_number;
> ```
>
> 2. **校园化改造**（校园身份、车位分区、访客/家长报备、违规与黑名单、校园规则配置）执行一次：
>
> ```bash
> mysql -u root -p parking_system < docs/migration-campus.sql
> ```
>
> 该脚本会新增 `user_no / identity_type / department / class_name / identity_status / identity_remark` 字段、
> 车位 `zone_code / allow_identities / free_minutes` 字段、`t_visit_report / t_violation / t_blacklist` 三张表、
> 16 项校园规则配置，并新增「访客与家长临时停车区」车位。
>
> 3. **分区名称中文化与访客区扩容到 300 个车位**：
>
> ```bash
> mysql -u root -p parking_system < docs/migration-campus-area-visitor.sql
> ```
>
> 4. **学院/部门管理 + 多身份演示用户**（新增 `t_department` 表、7 个学院/部门种子数据，并创建教师/职工/访客/家长演示账号）：
>
> ```bash
> mysql -u root -p parking_system < docs/migration-department.sql
> ```
>
> 5. **演示数据（违规 / 黑名单 / 访客报备 / 预约 / 停车与扣费记录）**，可重复执行：
>
> ```bash
> mysql -u root -p parking_system < docs/seed-demo-data.sql
> ```

或者手动创建数据库后导入：

```sql
CREATE DATABASE IF NOT EXISTS parking_system
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE parking_system;
SOURCE docs/parking.sql;
```

**2. 修改配置**

编辑 `parking-server/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/parking_system?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root        # 改为你的 MySQL 用户名
    password: 123456      # 改为你的 MySQL 密码
  data:
    redis:
      host: localhost     # Redis 地址
      port: 6379
      password:           # Redis 密码（如有）
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
| `app.js` → `globalData.baseUrl` | 后端 API 基础地址 |
| `app.json` | TabBar、窗口样式、页面路由 |
| `project.config.json` | 微信开发者工具配置 |

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
}
```

### 小程序部署

在微信开发者工具中点击"上传"，提交审核后发布即可。

---

## 开发说明

### 代码规范

- **后端**：标准 Java 分层架构（Controller → Service → Mapper），统一使用 `R<T>` 作为响应格式
- **管理后台**：Vue 3 Composition API + TypeScript + Element Plus 组件库
- **小程序**：原生微信小程序语法（Page/App 风格），通过 `wx.request` 与后端通信

### 关键设计

1. **双 Token 体系**：用户 Token 和管理员 Token 独立签发，通过不同接口登录获取。JwtInterceptor 根据请求路径判断角色权限——`/api/admin/**` 路径要求 Token 中 `role=admin`，普通用户接口要求 `role=user`
2. **无状态鉴权**：JwtInterceptor 从请求头 `Authorization: Bearer <token>` 提取 Token，解析后将 `userId`、`phone`、`role` 存入 ThreadLocal（UserContext），请求完成后自动清理，全程不依赖 Session
3. **余额乐观扣款**：扣费使用 SQL 乐观锁 `UPDATE t_user SET balance = balance - #{amount} WHERE id = #{userId} AND balance >= #{amount}`，在 Mapper 层判断影响行数，防止并发场景下超额扣款
4. **预约过期机制**：`ScheduledTaskService` 每 60 秒扫描一次，将 `endTime + gracePeriodMinutes(15分钟)` 已过的活跃预约自动标记为过期（status=4），同时释放对应车位（status 从 2 重置为 0）
  - **预约时间约束（禁止预约过去时段）**：前端 `utils/timeSlot.js` 按 30 分钟粒度生成候选时段，选择「今天」时只保留「当前时间 + 10 分钟」之后的时段（历史时段不会出现在 picker 里，因此无法查看与选择）；今天已无可用时段时自动切换到次日；提交前再做一次本地时间戳校验。服务端 `ReservationServiceImpl.createReservation` 兜底校验 `startTime` 不得早于当前时间、结束时间必须晚于开始时间、时长 1 小时 ~ 30 天，防止绕过前端直接调接口
  - **校园规则引擎**：`CampusRuleService` 集中管理身份相关规则——① 身份差异化费率（学生五折 `rate_student_discount`、访客加价 `rate_visitor_multiplier`、假期统一折扣 `holiday_discount`）；② 身份免费时长（教师 120 / 职工 60 / 家长 30 / 学生·访客 15 分钟），出场结算先从停放时长中扣减免费时长再按「不足 1 小时按 1 小时」计费；③ 预约约束（**身份未认证不可预约**、区域权限、每日预约上限、夜间禁停 `23:00-06:00`、假期模式关闭教学区/图书馆区预约、黑名单拦截）；④ 规则值全部存放在 `t_system_config`，管理员可在「系统配置」在线调整，无需改代码
  - **校园分区与访客/家长临时停车**：车位新增 `zone_code` 与 `allow_identities`，分为教学楼区(A区)/图书馆区(B区)/宿舍区(C区)/访客与家长临时停车区(V区，仅 4/5 身份)；小程序预约列表按当前身份过滤可停区域。师生可在小程序为家长或访客**提前报备车牌**，管理员审核通过后系统自动创建访客账号并分配 V 区车位，访客到达校门由管理员「登记入场」，出场时家长享受 30 分钟免费、超时按访客费率计费；超过预计离校时间 + 宽限期自动生成违规记录（`t_violation`）
5. **车辆出场自动结算**：出场接口计算 `(出场时间 - 入场时间)` 得出停车时长和费用 → 乐观锁扣款 → 生成 `t_payment_record` 扣费记录 → 释放车位 → 更新 `t_vehicle_record` 状态
6. **预约提前离场按预约结算**：预约车辆可提前离场，出场结算费用取 `max(实际停车费, 预约金额)`——预约时段已被锁定，即使提前离场也按预约金额结算（扣费类型 `reservation_settle`）；超时停车则按实际时长计费（多占多付）。`/api/vehicle/current` 会返回关联预约金额 `reservationAmount` 供前端展示
7. **分布式雪花ID**：所有实体主键使用 MyBatis-Plus 的 `IdType.ASSIGN_ID`（基于 Hutool 的 Snowflake 算法），不同实体通过不同 workerId/datacenterId 组合生成全局唯一 ID
8. **BCrypt 密码加密**：所有密码（用户 + 管理员）使用 `BCryptPasswordEncoder` 加密存储，原始密码永不出现在数据库
9. **微信登录模拟**：生产环境微信 OAuth 待对接，当前开发阶段生成模拟 `openid = "wx_" + code` 用于调试
9. **CORS 跨域**：`WebConfig` 中配置所有来源对 `/api/**` 的跨域请求放行，方便前后端分离开发
10. **验证码防护**：登录/注册接口需验证图形验证码，验证码通过 Easy Captcha 生成，Base64 图片 + UUID Key 返回，验证码文本以 Key 为键存储在 Redis 中（5分钟过期）

### 扩展方向

- [ ] 真实微信支付对接（当前为模拟支付）
- [ ] 车牌照识别（OCR/摄像头联动）
- [ ] WebSocket 实时推送车位状态
- [ ] 多停车场支持
- [ ] 会员体系 / 月卡季卡
- [ ] 黑白名单管理
- [ ] 车位引导屏对接
- [ ] 语音播报出入场
- [ ] Docker 容器化部署
- [ ] 单元测试 & 集成测试覆盖

---

## License

MIT License

---

> 🤖 此文档最后更新于 2026-07-28
