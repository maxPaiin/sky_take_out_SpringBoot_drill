# Sky Take Out - MVC 架構解析

> 基於 SpringBoot + SSM (Spring + SpringMVC + MyBatis) 的外賣後台管理系統（開發中）

---

## 一、多模組架構總覽

本專案採用 Maven 多模組結構，將職責分離為三個子模組：

```
sky-take-out (父工程 pom)
├── sky-common   ── 公共模組：工具類、常量、異常、配置屬性
├── sky-pojo     ── 數據模型：Entity、DTO、VO
└── sky-server   ── 業務核心：Controller、Service、Mapper、配置
```

### 模組依賴關係

```
┌─────────────┐
│  sky-server  │  ← 啟動入口 (SkyApplication)
│  (業務核心)   │
└──────┬───────┘
       │ 依賴
       ▼
┌─────────────┐     ┌──────────────┐
│  sky-pojo   │────▶│  sky-common  │
│ (數據模型)   │依賴  │  (公共工具)    │
└─────────────┘     └──────────────┘
```

- **sky-server** 依賴 sky-pojo 和 sky-common
- **sky-pojo** 依賴 sky-common
- **sky-common** 無內部依賴，為最底層模組

---

## 二、MVC 分層架構

### 請求處理流程（全景圖）

```
                         HTTP Request
                              │
                              ▼
                   ┌─────────────────────┐
                   │  JwtTokenAdmin      │  ← 攔截器：JWT 令牌校驗
                   │  Interceptor        │     攔截 /admin/** (排除 login)
                   └──────────┬──────────┘     解析 token → empId → ThreadLocal
                              │ 放行
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                    Controller 層 (View 層)                    │
│  接收 HTTP 請求，調用 Service，返回統一 Result<T> 響應           │
│                                                               │
│  [管理端 admin]                                                │
│  EmployeeController ── /admin/employee/**                    │
│  CategoryController ── /admin/category/**                    │
│  DishController     ── /admin/dish/**                        │
│  SetmealController  ── /admin/setmeal/**  (套餐管理)          │
│  CommonController   ── /admin/common/** (圖片上傳)            │
│  ShopController     ── /admin/shop/**    (店鋪營業狀態)        │
│                                                               │
│  [用戶端 user]                                                 │
│  ShopController     ── /user/shop/**     (查詢店鋪狀態)        │
│  UserController     ── /user/user/**     (微信登錄)            │
│  CategoryController ── /user/category/** (查詢分類列表)        │
│  DishController     ── /user/dish/**     (按分類查詢菜品+口味)  │
│  SetmealController  ── /user/setmeal/**  (按分類查詢套餐)      │
└─────────────────────────┬───────────────────────────────────┘
                          │ 調用
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                     Service 層 (業務邏輯)                     │
│  處理業務規則、資料轉換 (DTO → Entity)、事務管理                  │
│                                                               │
│  EmployeeServiceImpl ── 員工 CRUD + 登入驗證 + MD5 加密        │
│  CategoryServiceImpl ── 分類 CRUD + 關聯檢查 (菜品/套餐)       │
│  DishServiceImpl     ── 菜品 CRUD + 口味管理 + @Transactional  │
│  SetmealServiceImpl  ── 套餐 CRUD + 套餐-菜品關聯管理           │
│                         起售前校驗套餐內菜品均已起售              │
└─────────────────────────┬───────────────────────────────────┘
                          │ 調用
                          ▼
┌─────────────────────────────────────────────────────────────┐
│                     Mapper 層 (持久層 / DAO)                  │
│  MyBatis 接口 + XML 映射檔，操作資料庫                          │
│                                                               │
│  EmployeeMapper   ── employee 表                             │
│  CategoryMapper   ── category 表                             │
│  DishMapper       ── dish 表                                 │
│  DishFlavorMapper ── dish_flavor 表                           │
│  SetmealMapper    ── setmeal 表                              │
│  SetmealDishMapper── setmeal_dish 表 (套餐-菜品關聯)          │
└─────────────────────────┬───────────────────────────────────┘
                          │ SQL
                          ▼
                   ┌──────────────┐
                   │  MySQL 資料庫  │
                   │ sky_take_out  │
                   └──────────────┘
```

---

## 三、各層詳細說明

### 3.1 Controller 層 — 請求入口

| Controller | 路由前綴 | 已實現的功能 |
|---|---|---|
| `EmployeeController` | `/admin/employee` | 登入、登出、新增員工、分頁查詢、啟用/禁用、按 ID 查詢、修改員工 |
| `admin.CategoryController` | `/admin/category` | 新增分類、分頁查詢、刪除分類、修改分類、啟用/禁用、按類型查詢列表 |
| `admin.DishController` | `/admin/dish` | 新增菜品(含口味)、分頁查詢、批量刪除、按 ID 查詢(含口味)、修改菜品(含口味)、起售/停售 |
| `admin.SetmealController` | `/admin/setmeal` | 新增套餐(含菜品)、分頁查詢、批量刪除、按 ID 查詢(回顯)、修改套餐、起售/停售 |
| `CommonController` | `/admin/common` | 圖片上傳 (阿里雲 OSS) |
| `admin.ShopController` | `/admin/shop` | 設置店鋪營業狀態、查詢店鋪營業狀態 (寫入 Redis) |
| `user.ShopController` | `/user/shop` | 查詢店鋪營業狀態 (從 Redis 讀取) |
| `user.UserController` | `/user/user` | 微信登錄 (code → openid → JWT)，首次登錄自動註冊 |
| `user.CategoryController` | `/user/category` | 按類型查詢分類列表 (供顧客端瀏覽) |
| `user.DishController` | `/user/dish` | 按分類 ID 查詢起售菜品（含口味），無口味菜品返回空 `[]` |
| `user.SetmealController` | `/user/setmeal` | 按分類 ID 查詢起售套餐；按套餐 ID 查詢套餐包含的菜品詳情 |

> 同名 Controller 衝突規避：`ShopController`、`CategoryController`、`DishController`、`SetmealController`
> 在 admin / user 套件中各有一份，均透過 `@RestController("adminXxxController")` /
> `@RestController("userXxxController")` 顯式指定 Bean 名稱以避免 Spring 容器衝突。

**統一回應格式**：所有接口均返回 `Result<T>` 物件

```
{ "code": 1, "msg": null, "data": ... }   ← 成功
{ "code": 0, "msg": "錯誤訊息", "data": null } ← 失敗
```

### 3.2 Service 層 — 業務邏輯

```
┌──────────────────────────────────────────────────┐
│              Service Interface                    │
│  EmployeeService / CategoryService / DishService  │
└──────────────────────┬───────────────────────────┘
                       │ implements
                       ▼
┌──────────────────────────────────────────────────────────────┐
│                   ServiceImpl (核心業務)                       │
│                                                                │
│  EmployeeServiceImpl:                                         │
│    login()      → 查帳號 → MD5比對密碼 → 檢查帳號狀態           │
│    save()       → DTO→Entity + 預設密碼(123456) + 預設狀態     │
│    pageQuery()  → PageHelper 分頁                              │
│    startOrStop()→ Builder 模式構建 Entity → update             │
│    getById()    → 查詢 + 密碼脫敏("****")                     │
│    update()     → BeanUtils.copyProperties → update           │
│                                                                │
│  CategoryServiceImpl:                                         │
│    save()       → 預設狀態=禁用(0)                              │
│    deleteById() → 先檢查菜品關聯 → 再檢查套餐關聯 → 刪除       │
│    startOrStop()→ Builder 模式                                 │
│                                                                │
│  DishServiceImpl:                                             │
│    saveWithFlavor()   → @Transactional                        │
│      插入菜品 → 取回主鍵 → 設置口味的 dishId → 批量插入口味     │
│    deleteBatch()      → @Transactional                        │
│      檢查起售狀態 → 檢查套餐關聯 → 批量刪菜品 → 批量刪口味      │
│    getByIdWithFlavor()→ 查菜品主表 + 查口味子表 → 組裝 DishVO  │
│    updateWithFlavor() → 動態更新菜品 → 先刪舊口味 → 再批量插入  │
│    startOrStop()      → 停售時連帶停售包含該菜品的套餐          │
│    listWithFlavor()   → 按分類+狀態查詢菜品 → 逐條查口味組裝   │
│                                                                │
│  SetmealServiceImpl:                                          │
│    saveWithDish()     → @Transactional                        │
│      插入套餐 → 取回主鍵 → 批量插入 setmeal_dish 關聯         │
│    deleteBatch()      → 起售中套餐不可刪 → 刪套餐+刪關聯行      │
│    getByIdWithDish()  → 查套餐主表 + 查關聯菜品 → 組裝 SetmealVO│
│    update()           → 更新套餐 → 刪舊關聯 → 插入新關聯        │
│    startOrStop()      → 起售前校驗套餐內所有菜品均為起售狀態    │
│    list()             → 按分類+狀態條件查詢套餐                 │
│    getDishItemById()  → 查套餐包含的菜品名稱/份數/圖片等詳情    │
└──────────────────────────────────────────────────────────────┘
```

### 3.3 Mapper 層 — 數據訪問

| Mapper | 對應表 | SQL 方式 | 主要操作 |
|---|---|---|---|
| `EmployeeMapper` | employee | 註解 + XML | insert, getByUsername, getById, pageQuery(XML動態SQL), update(XML動態SQL) |
| `CategoryMapper` | category | 註解 + XML | insert, deleteById, pageQuery(XML), update(XML), list(XML) |
| `DishMapper` | dish | 註解 + XML | insert(XML), getById, deleteById, deleteByIds(XML), countByCategoryId, pageQuery(XML), list(XML動態條件), update(XML全欄位動態), getBySetmealId |
| `DishFlavorMapper` | dish_flavor | 註解 + XML | insertBatch(XML), deleteByDishId, deleteByDishIds(XML批量), getByDishId |
| `SetmealMapper` | setmeal | 註解 + XML | insert(XML), getById, deleteById, update(XML全欄位動態), pageQuery(XML), list(XML動態條件), countByCategoryId, getDishItemBySetmealId |
| `SetmealDishMapper` | setmeal_dish | 註解 + XML | insertBatch(XML), deleteBySetmealId, getBySetmealId, getDishIdsByDishIds(XML), getSetmealIdsByDishIds(XML) |

---

## 四、資料庫 ER 關係圖

```
┌──────────────┐       ┌──────────────┐       ┌──────────────────┐
│   employee   │       │   category   │       │      user        │
├──────────────┤       ├──────────────┤       ├──────────────────┤
│ id (PK)      │       │ id (PK)      │       │ id (PK)          │
│ name         │       │ type         │       │ openid           │
│ username     │       │ name         │       │ name             │
│ password     │       │ sort         │       │ phone / sex      │
│ phone / sex  │       │ status       │       │ id_number        │
│ id_number    │       │ create_time  │       │ avatar           │
│ status       │       │ update_time  │       │ create_time      │
│ create_time  │       │ create_user  │       └──────────────────┘
│ update_time  │       │ update_user  │              │
│ create_user  │       └──────┬───────┘              │ (1:N)
│ update_user  │              │                      ▼
└──────────────┘              │ (1:N)         ┌──────────────┐
                              │               │   orders     │
                    ┌─────────┴────────┐      ├──────────────┤
                    │                  │      │ id (PK)      │
                    ▼                  ▼      │ user_id (FK) │
             ┌───────────┐    ┌──────────┐   │ number       │
             │   dish     │    │ setmeal  │   │ status       │
             ├───────────┤    ├──────────┤   │ amount       │
             │ id (PK)   │    │ id (PK)  │   │ pay_method   │
             │ name      │    │ name     │   │ ...          │
             │ category_id│◄──│category_id│  └──────┬───────┘
             │ price     │    │ price    │          │ (1:N)
             │ image     │    │ image    │          ▼
             │ status    │    │ status   │   ┌──────────────┐
             │ ...       │    │ ...      │   │ order_detail │
             └─────┬─────┘    └────┬─────┘   └──────────────┘
                   │               │
                   │ (1:N)         │ (M:N)
                   ▼               ▼
            ┌─────────────┐  ┌──────────────┐
            │ dish_flavor  │  │ setmeal_dish │  ← 中間表
            ├─────────────┤  ├──────────────┤
            │ id (PK)     │  │ id (PK)      │
            │ dish_id (FK)│  │ setmeal_id   │
            │ name        │  │ dish_id      │
            │ value       │  │ name (冗餘)   │
            └─────────────┘  │ price / copies│
                             └──────────────┘
```

**核心關聯**：
- `category` 1:N `dish` — 一個分類下有多個菜品
- `category` 1:N `setmeal` — 一個分類下有多個套餐
- `dish` 1:N `dish_flavor` — 一個菜品有多個口味選項
- `setmeal` M:N `dish`（透過 `setmeal_dish` 中間表）— 套餐包含多個菜品

---

## 五、橫切關注點 (Cross-Cutting Concerns)

### 5.1 JWT 認證攔截器

```
HTTP Request
    │
    ▼
JwtTokenAdminInterceptor.preHandle()
    │
    ├── 非 Controller 方法？ → 放行
    │
    ├── 從 Header 取 "token" 字段
    │
    ├── JwtUtil.parseJWT() 解析
    │   │
    │   ├── 成功 → 取出 empId → BaseContext.setCurrentId(empId) → 放行
    │   │                        (存入 ThreadLocal，供後續業務層使用)
    │   │
    │   └── 失敗 → 返回 401
    │
    └── 排除路徑：/admin/employee/login
```

### 5.2 AOP 公共字段自動填充

```
Mapper 方法調用 (帶 @AutoFill 註解)
    │
    ▼
AutoFillAspect.autoFill() — @Before 前置通知
    │
    ├── 切入點: execution(* com.sky.mapper.*.*(..)) && @annotation(AutoFill)
    │
    ├── INSERT 操作 → 反射設置 4 個字段:
    │     setCreateTime(now), setCreateUser(currentId)
    │     setUpdateTime(now), setUpdateUser(currentId)
    │
    └── UPDATE 操作 → 反射設置 2 個字段:
          setUpdateTime(now), setUpdateUser(currentId)
```

### 5.3 全域異常處理

```
GlobalExceptionHandler (@RestControllerAdvice)
    │
    ├── BaseException (自定義業務異常)
    │     → Result.error(ex.getMessage())
    │
    └── SQLIntegrityConstraintViolationException
          → 含 "Duplicate entry"? → 解析重複欄位名 → "xxx已存在"
          → 否則 → "未知错误"
```

### 5.4 Redis 配置 (Spring Data Redis)

引入 `spring-boot-starter-data-redis`，透過 `RedisConfiguration` 自訂 `RedisTemplate`，
將 key 的序列化器改為 `StringRedisSerializer`，避免預設 JDK 序列化產生的亂碼字節字首，
讓 Redis 端的 key 以人類可讀的純字串保存。

```
RedisConfiguration.redisTemplate(RedisConnectionFactory)
    │
    ├── new RedisTemplate<>()
    ├── setConnectionFactory(factory)    ← 由 SpringBoot 自動裝配 (Lettuce)
    └── setKeySerializer(StringRedisSerializer)
         ↑ 僅指定 key 的序列化器;value 保持預設 (後續若要存物件可再補上 JSON 序列化器)
```

#### 配置檔 (application.yml / application-dev.yml)

```yaml
# application.yml  (框架層，引用變數)
spring:
  redis:
    host: ${sky.redis.host}
    port: ${sky.redis.port}
    database: ${sky.redis.database}   # Redis 預設 16 個庫 (0~15)，彼此資料隔離

# application-dev.yml  (環境層，實際值)
sky:
  redis:
    host: localhost
    port: 6379
    database: 0
```

> 採用「`spring.redis.*` 引用 `sky.redis.*`」的兩段式配置，
> 延續本專案資料源 / 阿里雲 OSS 一致的風格 —
> 框架相關欄位集中在 `application.yml`，環境相關實際值放在 `application-dev.yml`，
> 切換環境時只需替換後者。

#### 使用方式

```java
@Autowired
private RedisTemplate redisTemplate;

redisTemplate.opsForValue().set("key", "value");   // String
redisTemplate.opsForHash()  // Hash
redisTemplate.opsForList()  // List
redisTemplate.opsForSet()   // Set
redisTemplate.opsForZSet()  // Sorted Set
```

測試入口：`sky-server/src/test/java/com/sky/test/springDataRedisTest.java`

#### 實際應用：店鋪營業狀態

店鋪營業狀態是第一個落地使用 Redis 的業務功能。它的特性很適合用快取保存而非資料表：
**單一全域標誌、頻繁讀取、極少寫入、不需歷史紀錄**。

```
┌─────────────────────────┐         ┌──────────────────────────┐
│  管理端 (員工/老闆)        │         │  用戶端 (顧客小程序)        │
│  PUT  /admin/shop/{1|0} │  寫入    │  GET  /user/shop/status   │
│       (1=營業 / 0=打烊)  │ ──────▶│       讀取當前狀態          │
│  GET  /admin/shop/status│ ◀────── │                           │
└────────────┬────────────┘  共用    └─────────────┬────────────┘
             │              key                    │
             ▼                                     ▼
        ┌──────────────────────────────────────────────┐
        │  Redis  key = "shop_status"  value = 1 或 0   │
        │  (StringRedisSerializer 確保 key 可讀)         │
        └──────────────────────────────────────────────┘
```

```java
// 兩端共用的 key 常量（各自宣告於對應 Controller）
public static final String KEY = "shop_status";

// 寫入（管理端）
redisTemplate.opsForValue().set(KEY, status);

// 讀取（兩端共用）
Integer status = (Integer) redisTemplate.opsForValue().get(KEY);
```

> **為什麼不寫資料表？**
> 店鋪狀態本質上是一個全域開關，既不需要分頁、查詢歷史，也不參與任何 JOIN，
> 走資料庫只會增加一次磁碟 IO。Redis 的 in-memory 讀取對顧客端首頁載入更友善，
> 同時管理端切換狀態的寫入頻率極低，不存在快取一致性壓力。

### 5.5 微信登錄（用戶端 C 端入口）

用戶端的身份識別不走帳號密碼，而是基於微信小程序的 **`code2session`** 機制：
小程序前端透過 `wx.login()` 拿到一次性憑證 `code`，後端用 `code` + `appid` + `secret`
向微信伺服器換取使用者唯一識別 `openid`，再以 `openid` 為主鍵建立/查詢本地 `user` 表，
最後簽發 JWT 回傳給小程序。

#### 整體時序

```
┌────────────────┐                    ┌─────────────────┐                ┌──────────────────┐
│  微信小程序前端  │                    │   sky-server     │                │   微信開放平台    │
│                │                    │  (我們的後端)     │                │ api.weixin.qq.com│
└───────┬────────┘                    └────────┬────────┘                └─────────┬────────┘
        │ ① wx.login()                          │                                   │
        │   取得 code (5min 內有效)              │                                   │
        │                                       │                                   │
        │ ② POST /user/user/login              │                                   │
        │   Body: { "code": "xxxxx" }          │                                   │
        ├──────────────────────────────────────▶│                                   │
        │                                       │                                   │
        │                                       │ ③ GET sns/jscode2session         │
        │                                       │   ?appid=...&secret=...          │
        │                                       │   &js_code=code                  │
        │                                       │   &grant_type=authorization_code │
        │                                       ├──────────────────────────────────▶│
        │                                       │                                   │
        │                                       │ ④ { openid, session_key, ... }   │
        │                                       │◀──────────────────────────────────┤
        │                                       │                                   │
        │                                       │ ⑤ openid == null ?               │
        │                                       │      └─ throw LoginFailedException│
        │                                       │                                   │
        │                                       │ ⑥ SELECT * FROM user             │
        │                                       │      WHERE openid = ?            │
        │                                       │                                   │
        │                                       │ ⑦ user == null ? (首次登錄)       │
        │                                       │      └─ INSERT INTO user         │
        │                                       │           (openid, create_time)  │
        │                                       │                                   │
        │                                       │ ⑧ JwtUtil.createJWT()            │
        │                                       │     claims = { USER_ID: user.id }│
        │                                       │     signKey = user-secret-key     │
        │                                       │     ttl     = user-ttl            │
        │                                       │                                   │
        │ ⑨ Result<UserLoginVO>                 │                                   │
        │   { id, openid, token }              │                                   │
        │◀──────────────────────────────────────┤                                   │
        │                                       │                                   │
        │ ⑩ 後續請求附帶 token                  │                                   │
        │   Header: authentication: <jwt>       │                                   │
        ├──────────────────────────────────────▶│                                   │
        │                                       │ JwtTokenUserInterceptor          │
        │                                       │ 解析 token → BaseContext          │
        │                                       │ (待實作)                          │
```

#### 為什麼要這樣設計？

- **`code` 是一次性的**：小程序產生的 `code` 只能換一次 `session_key`，且 5 分鐘過期，
  即使被攔截也無法重複使用。真正的長效憑證是後端簽發的 JWT。
- **`openid` 是用戶在「本小程序」的唯一識別**：同一個微信用戶在不同小程序的 `openid`
  不同；它由微信平台給定，後端用它當作本地 `user` 表的天然主鍵候選。
- **首次登錄即註冊**：使用者不需要事先填表，第一次點開小程序就完成「無感註冊」。
  其他欄位（手機、頭像、姓名）在後續流程中（如下單填地址）逐步補齊。
- **`secret` 必須留在後端**：`appid` 是小程序公開識別，但 `secret` 是商家密鑰，
  絕不能放到小程序前端。`code → openid` 這一跳必須由後端發起。

#### 程式碼鏈路

**Step 1 — Controller 接收 `code`**

```java
// UserController.java   @RequestMapping("/user/user")
@PostMapping("/login")
@ApiOperation("wechat用戶登錄")
public Result<UserLoginVO> login(@RequestBody UserLoginDTO userLoginDTO){
    log.info("微信用戶登錄:{}",userLoginDTO.getCode());
    User user = userService.wxLogin(userLoginDTO);

    //為微信用戶生成jwt令牌
    Map<String,Object> claims = new HashMap<>();
    //用戶的唯一標識
    claims.put(JwtClaimsConstant.USER_ID,user.getId());
    String token = JwtUtil.createJWT(jwtProperties.getUserSecretKey(), jwtProperties.getUserTtl(), claims);

    UserLoginVO userLoginVO = UserLoginVO.builder()
            .id(user.getId())
            .openid(user.getOpenid())
            .token(token)
            .build();

    return Result.success(userLoginVO);
}
```

> Controller 只做兩件事：① 委託 Service 完成「換取 openid + 註冊」；② 簽發 JWT 並組裝 VO。
> 任何業務細節（HTTP 呼叫、資料庫存取）都不在這層出現。

**Step 2 — Service 呼叫微信 + 落本地用戶**

```java
// UserServiceImpl.java
public static final String WX_LOGIN = "https://api.weixin.qq.com/sns/jscode2session";

@Override
public User wxLogin(UserLoginDTO userLoginDTO) {
    //調用微信服務器的接口,獲取當前用戶的openID
    Map<String,String> map = new HashMap<>();
    map.put("appid",weChatProperties.getAppid());
    map.put("secret",weChatProperties.getSecret());
    map.put("js_code",userLoginDTO.getCode());
    map.put("grant_type","authorization_code");
    String json = HttpClientUtil.doGet(WX_LOGIN, map);

    JSONObject jsonObject = JSON.parseObject(json);
    String openid = jsonObject.getString("openid");

    //判斷OpenID是否獲取到了
    if(openid == null ) throw new LoginFailedException(MessageConstant.LOGIN_FAILED);
    //當前用戶是否是新的用戶(對外賣系統來說)
    User user = userMapper.getByOpenid(openid);

    //如果是新用戶,那麼應該完成註冊(保存到數據庫中)
    if(user == null) {
      user = User.builder()
                .openid(openid)
                .createTime(LocalDateTime.now())
                .build();
      userMapper.insert(user);
    }
    //返回一個用戶對象
    return user;
}
```

> 這一層體現了三件事：
> - **外部 IO（微信 API）封裝在 Service 內**：Controller 不需要知道微信 API 長什麼樣。
> - **「先查再插」的 upsert 模式**：以 `openid` 為唯一鍵，已存在則沿用、不存在則註冊。
> - **失敗回拋自定義異常**：`LoginFailedException` 由 `GlobalExceptionHandler`
>   統一轉成 `Result.error(...)`，不需要 Controller 處理錯誤分支。

**Step 3 — Mapper 兩個方法**

```java
// UserMapper.java
@Mapper
public interface UserMapper {

    @Select("select * from user where openid = #{openid}")
    User getByOpenid(String openid);                // ← 簡單查詢用註解

    void insert(User user);                          // ← 插入走 XML（之後可能擴欄位）
}
```

```xml
<!-- UserMapper.xml -->
<insert id="insert" useGeneratedKeys="true" keyProperty="id">
    insert into user (openid, name, phone, sex, id_number, avatar, create_time)
    values (#{openid}, #{name}, #{phone},#{sex}, #{idNumber}, #{avatar}, #{createTime})
</insert>
```

> `useGeneratedKeys="true" keyProperty="id"` 確保 `INSERT` 後主鍵回填到 `user.id`，
> 這樣 Service 才能把 `user.getId()` 寫進 JWT claims。
> 雖然目前只有 `openid` 與 `createTime` 有值，其他欄位先預留位置，未來補頭像、手機號時不必再改 SQL。

#### 配置欄位

```yaml
# application.yml — 框架層引用
sky:
  jwt:
    user-secret-key: itcast        # 用戶端 JWT 簽名密鑰（建議與 admin 不同）
    user-ttl: 7200000              # 兩小時
    user-token-name: authentication # 前端在 Header 帶這個欄位送 token
  wechat:
    appid:  ${sky.wechat.appid}
    secret: ${sky.wechat.secret}

# application-dev.yml — 環境層實值（appid/secret 由商家提供）
sky:
  wechat:
    appid:  
    secret: 
```

> 與 Redis、阿里雲 OSS 一致，仍是「`spring.*` / `sky.jwt.*` / `sky.wechat.*` 在 application.yml
> 引用變數，application-dev.yml 放實值」的兩段式結構。

#### Token 簽發後做什麼？

簽出去的 JWT 之後會由用戶端攔截器 `JwtTokenUserInterceptor` 校驗（待實作）。
攔截器解析 token 後把 `userId` 寫入 `BaseContext`，後續 `購物車`、`下單`、`地址簿`
等業務 Service 直接 `BaseContext.getCurrentId()` 即可拿到當前用戶。
管理端的 `JwtTokenAdminInterceptor` 可作為對照範本。

> 注意：管理端與用戶端的密鑰、TTL、Header 名稱都各自獨立，
> 兩個攔截器互不干涉、各自只攔自己的路徑前綴（`/admin/**` vs `/user/**`）。

---

### 5.6 Swagger 雙分組（管理端 / 用戶端）

隨著用戶端 (`controller.user`) 開始出現第一個 Controller，原本單一的 Knife4j 文檔
拆成兩組，讓兩端介面在 `/doc.html` 中分頁顯示，避免管理端與用戶端 API 混雜。

```
WebMvcConfiguration
    │
    ├── @Bean docket()   → groupName("管理端")
    │                      basePackage("com.sky.controller.admin")
    │
    └── @Bean docket2()  → groupName("用戶端")
                           basePackage("com.sky.controller.user")
```

兩個 `Docket` 透過不同的 `basePackage` 過濾各自掃描的 Controller，
`groupName` 則決定 Knife4j 左上角的下拉切換標籤。

> 攔截器 `JwtTokenAdminInterceptor` 仍只攔截 `/admin/**`，
> 用戶端路由 `/user/**` 目前不會經過 JWT 校驗（後續用戶端登入功能完成後會另行新增）。

---

## 六、數據流轉模型 (DTO / Entity / VO)

```
前端 Request                    後端處理                       前端 Response
    │                              │                              ▲
    │  ┌──────┐                    │                   ┌──────┐  │
    └─▶│  DTO │─── Controller ───▶ Service ──▶ Mapper  │  VO  │──┘
       └──────┘   (接收請求參數)    (業務轉換)   (持久化)  └──────┘
                                    │                   (返回展示數據)
                               ┌────┴────┐
                               │ Entity  │
                               └─────────┘
                             (對應資料庫表結構)
```

| 類型 | 用途 | 範例 |
|---|---|---|
| **DTO** (Data Transfer Object) | 接收前端請求參數，欄位與前端表單對齊 | `EmployeeDTO`, `DishDTO`, `DishPageQueryDTO` |
| **Entity** | 對應資料庫表結構，ORM 映射 | `Employee`, `Dish`, `Category` |
| **VO** (View Object) | 返回給前端的展示數據，可跨表組合 | `EmployeeLoginVO`, `DishVO` |

### 6.1 完整請求生命週期：從 Controller 到資料庫再返回

以下以 **「新增菜品（含口味）」** 這個實際功能為例，完整追蹤一次寫入請求如何穿越每一層。

#### 總覽流程圖

```
前端 POST /admin/dish
Body: { name:"宮保雞丁", categoryId:1, price:38, flavors:[{name:"辣度",value:"..."}] }
  │
  ▼
╔═══════════════════════════════════════════════════════════════════════════╗
║ ① Controller 層 — DishController.save()                                 ║
║    接收 JSON → 反序列化為 DishDTO（含 flavors 列表）                      ║
║    職責：僅做接收與回應，不處理任何業務邏輯                                 ║
╚════════════════════════════════╤══════════════════════════════════════════╝
                                 │ dishService.saveWithFlavor(dishDTO)
                                 ▼
╔═══════════════════════════════════════════════════════════════════════════╗
║ ② Service 層 — DishServiceImpl.saveWithFlavor()   [@Transactional]      ║
║                                                                           ║
║    Step A: DTO → Entity 轉換                                             ║
║            BeanUtils.copyProperties(dishDTO, dish)                       ║
║            DishDTO 有 flavors 欄位，Dish Entity 沒有 → 需要分開處理       ║
║                                                                           ║
║    Step B: 呼叫 dishMapper.insert(dish) → 插入菜品主表                    ║
║            ↓↓↓  此時觸發 AOP + MyBatis  ↓↓↓                              ║
║                                                                           ║
║    Step C: 取回自動生成的主鍵 dish.getId()                                ║
║            遍歷 flavors 列表，為每個口味設定 dishId                        ║
║                                                                           ║
║    Step D: 呼叫 dishFlavorMapper.insertBatch(flavors) → 批量插入口味表    ║
╚════════════════╤══════════════════════════════════╤═══════════════════════╝
                 │ dishMapper.insert(dish)          │ dishFlavorMapper.insertBatch(flavors)
                 ▼                                  ▼
╔═══════════════════════════════════════════════════════════════════════════╗
║ ③ Mapper 接口層 — DishMapper / DishFlavorMapper                          ║
║                                                                           ║
║    DishMapper.insert(dish)                                                ║
║      → 帶有 @AutoFill(INSERT) 註解                                       ║
║      → AOP 切面攔截，反射填充 createTime / updateTime / createUser /      ║
║        updateUser 四個公共字段                                             ║
║      → 然後進入 MyBatis XML 執行 SQL                                      ║
║                                                                           ║
║    DishFlavorMapper.insertBatch(flavors)                                  ║
║      → 無 @AutoFill（口味表無公共字段），直接進入 XML                       ║
╚════════════════╤══════════════════════════════════╤═══════════════════════╝
                 │                                  │
                 ▼                                  ▼
╔═══════════════════════════════════════════════════════════════════════════╗
║ ④ MyBatis XML 映射檔 — 實際 SQL 執行                                     ║
║                                                                           ║
║  DishMapper.xml:                                                         ║
║  ┌─────────────────────────────────────────────────────────────────────┐  ║
║  │ <insert id="insert" useGeneratedKeys="true" keyProperty="id">     │  ║
║  │   INSERT INTO dish (name, category_id, price, image, ...)         │  ║
║  │   VALUES (#{name}, #{categoryId}, #{price}, #{image}, ...)        │  ║
║  │ </insert>                                                          │  ║
║  └─────────────────────────────────────────────────────────────────────┘  ║
║    useGeneratedKeys="true" → 執行後自動將主鍵回填到 dish.id               ║
║                                                                           ║
║  DishFlavorMapper.xml:                                                   ║
║  ┌─────────────────────────────────────────────────────────────────────┐  ║
║  │ <insert id="insertBatch">                                          │  ║
║  │   INSERT INTO dish_flavor (dish_id, name, value) VALUES            │  ║
║  │   <foreach collection="flavors" item="df" separator=",">          │  ║
║  │     (#{df.dishId}, #{df.name}, #{df.value})                       │  ║
║  │   </foreach>                                                       │  ║
║  │ </insert>                                                          │  ║
║  └─────────────────────────────────────────────────────────────────────┘  ║
║    foreach 動態生成批量 VALUES → 一條 SQL 插入所有口味                     ║
╚════════════════╤══════════════════════════════════╤═══════════════════════╝
                 │ INSERT INTO dish ...             │ INSERT INTO dish_flavor ...
                 ▼                                  ▼
          ┌─────────────────────────────────────────────┐
          │              MySQL: sky_take_out             │
          │                                             │
          │  dish 表      ← 新增 1 筆菜品記錄            │
          │  dish_flavor 表 ← 新增 N 筆口味記錄          │
          │                                             │
          │  兩張表在同一個事務中 (@Transactional)         │
          │  任一失敗 → 全部回滾                          │
          └─────────────────────────────────────────────┘
                 │
                 ▼ 返回 (逐層回傳)
          Service → Controller → Result.success() → 前端收到 {"code":1}
```

#### 各層職責與對應原始碼的逐步拆解

**Step 1 — Controller 接收請求**

```java
// DishController.java
@PostMapping
public Result save(@RequestBody DishDTO dishDTO) {
    dishService.saveWithFlavor(dishDTO);  // 僅做轉發，不處理業務
    return Result.success();
}
```

> `@RequestBody` 將 JSON 反序列化為 `DishDTO`。Controller 不做任何資料轉換或校驗邏輯，
> 只負責「接收」和「回應」，業務細節全部委託給 Service。

**Step 2 — Service 處理業務 + 資料轉換**

```java
// DishServiceImpl.java
@Transactional
public void saveWithFlavor(DishDTO dishDTO) {
    // DTO → Entity（只拷貝同名欄位，flavors 不會被拷貝）
    Dish dish = new Dish();
    BeanUtils.copyProperties(dishDTO, dish);

    // 插入菜品主表（AOP 會自動填充公共字段）
    dishMapper.insert(dish);

    // insert 後 MyBatis 回填主鍵 → dish.getId() 有值了
    Long dishId = dish.getId();

    // 將菜品主鍵寫入每條口味記錄，建立關聯
    List<DishFlavor> flavors = dishDTO.getFlavors();
    if (flavors != null && !flavors.isEmpty()) {
        flavors.forEach(f -> f.setDishId(dishId));
        dishFlavorMapper.insertBatch(flavors);
    }
}
```

> 這裡體現了 **Service 層是 DTO 和 Entity 之間的橋樑**：
> - 把前端傳來的 `DishDTO`（含 `flavors` 列表）拆分為 `Dish` Entity + `List<DishFlavor>` Entity
> - 先插入主表拿到主鍵，再用主鍵串聯子表 — 這是典型的「主從表寫入」模式
> - `@Transactional` 確保兩張表要麼一起成功，要麼一起回滾

**Step 3 — Mapper 接口 + AOP 攔截**

```java
// DishMapper.java
@AutoFill(value = OperationType.INSERT)   // ← AOP 切入標記
void insert(Dish dish);                    // ← 對應 DishMapper.xml 的 <insert>
```

> 當 MyBatis 呼叫 `insert()` 時，`AutoFillAspect` 的 `@Before` 通知會先觸發，
> 透過反射為 `dish` 物件設置 `createTime`、`updateTime`、`createUser`、`updateUser`，
> 然後才真正執行 XML 中的 INSERT SQL。

**Step 4 — XML 映射檔執行 SQL**

```xml
<!-- DishMapper.xml -->
<insert id="insert" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO dish (name, category_id, price, ..., create_time, update_time, createUser, updateUser)
    VALUES (#{name}, #{categoryId}, #{price}, ..., #{createTime}, #{updateTime}, #{createUser}, #{updateUser})
</insert>
```

> `useGeneratedKeys="true" keyProperty="id"` 是關鍵配置 —
> 讓 MyBatis 在 INSERT 執行完畢後，自動將 MySQL 生成的自增主鍵回寫到 `dish.id`，
> 這樣 Service 層才能拿到 `dish.getId()` 去設置口味的 `dishId`。

### 6.2 完整查詢生命週期：分頁查詢菜品

再以 **「菜品分頁查詢」** 為例，展示一次讀取請求如何穿越各層並返回 VO。

```
前端 GET /admin/dish?page=1&pageSize=10&name=雞
  │
  ▼
┌──────────────────────────────────────────────────────────────┐
│ ① Controller — DishController.page()                         │
│    Spring 自動將 query 參數綁定到 DishPageQueryDTO            │
│    (不需要 @RequestBody，因為是 GET 查詢參數)                  │
└──────────────────────┬───────────────────────────────────────┘
                       │ dishService.pageQuery(dto)
                       ▼
┌──────────────────────────────────────────────────────────────┐
│ ② Service — DishServiceImpl.pageQuery()                      │
│                                                                │
│    PageHelper.startPage(page, pageSize)                       │
│      ↑ 基於 ThreadLocal 攔截下一條 SQL，自動加上 LIMIT         │
│                                                                │
│    Page<DishVO> page = dishMapper.pageQuery(dto)              │
│      ↑ 返回的直接就是 DishVO（含 categoryName）                │
│                                                                │
│    return new PageResult(page.getTotal(), page.getResult())   │
└──────────────────────┬───────────────────────────────────────┘
                       │ dishMapper.pageQuery(dto)
                       ▼
┌──────────────────────────────────────────────────────────────┐
│ ③ Mapper + XML — DishMapper.pageQuery()                      │
│                                                                │
│    SELECT d.*, c.name AS categoryName                         │
│    FROM dish d                                                │
│    LEFT JOIN category c ON d.category_id = c.id               │
│    WHERE d.name LIKE '%雞%'      ← 動態 SQL <if> 條件拼接     │
│    ORDER BY d.update_time DESC                                │
│    LIMIT 0, 10                   ← PageHelper 自動追加        │
│                                                                │
│    resultType="DishVO" → MyBatis 直接映射到 VO                 │
│    其中 categoryName 來自 JOIN，不屬於 dish 表                  │
└──────────────────────┬───────────────────────────────────────┘
                       │
                       ▼
                    MySQL
                       │
                       ▼ 結果集
┌──────────────────────────────────────────────────────────────┐
│ 返回路徑：                                                     │
│                                                                │
│ MySQL → Page<DishVO> → PageResult(total, List<DishVO>)        │
│       → Result.success(pageResult) → JSON Response            │
│                                                                │
│ { "code":1, "data": { "total":25, "records":[                 │
│     { "id":1, "name":"宮保雞丁", "categoryName":"川菜",        │
│       "price":38, "status":1, "updateTime":"..." }, ...       │
│ ]}}                                                            │
└──────────────────────────────────────────────────────────────┘
```

### 6.3 完整修改生命週期：修改菜品（含口味）

修改菜品需同時維護 `dish` 主表與 `dish_flavor` 子表，採用「先刪後插」策略重建口味列表。

```
前端 PUT /admin/dish
Body: { id:10, name:"宮保雞丁(改)", price:42, flavors:[{name:"辣度",value:"..."}] }
  │
  ▼
┌──────────────────────────────────────────────────────────────┐
│ ① Controller — DishController.update()                       │
│    @PutMapping + @RequestBody → 反序列化為 DishDTO           │
│    呼叫 dishService.updateWithFlavor(dishDTO)                 │
└──────────────────────┬───────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────┐
│ ② Service — DishServiceImpl.updateWithFlavor()               │
│                                                                │
│    Step A: BeanUtils.copyProperties(dishDTO, dish)            │
│    Step B: dishMapper.update(dish)                            │
│              ↑ XML <update> + <set> 動態更新有值的欄位         │
│              ↑ @AutoFill(UPDATE) 自動填 updateTime/updateUser │
│    Step C: dishFlavorMapper.deleteByDishId(dish.getId())      │
│              ↑ 刪除該菜品所有舊口味                            │
│    Step D: 為每條新口味設定 dishId → insertBatch()            │
│              ↑ 口味採「整批替換」策略，不做 diff 比對          │
└──────────────────────┬───────────────────────────────────────┘
                       │
                       ▼
┌──────────────────────────────────────────────────────────────┐
│ ③ Mapper + XML                                                │
│    DishMapper.update → <set><if>...</if></set>                │
│      僅更新非 null 欄位，避免誤清零其他欄位                     │
│    DishFlavorMapper.deleteByDishId → delete where dish_id = ? │
│    DishFlavorMapper.insertBatch → foreach 批量 INSERT         │
└──────────────────────────────────────────────────────────────┘
```

> **為什麼「先刪後插」？**
> 修改口味時前端直接送來「完整的新口味列表」，若要做 diff（新增/更新/刪除哪幾條）會相當複雜；
> 而口味表資料量小、沒有外鍵依賴，直接清空重建是最簡單穩定的方案。
> 整個方法應當加上 `@Transactional`（若尚未加上需補上），避免刪除成功但插入失敗造成口味遺失。

### 6.4 按 ID 查詢菜品詳情（含口味）

菜品詳情頁需同時回傳基本資訊與口味列表，採「分兩次查詢 → 組裝 VO」。

```
GET /admin/dish/{id}
  │
  ▼
DishController.getById(@PathVariable Long id)
  │ → dishService.getByIdWithFlavor(id)
  ▼
DishServiceImpl.getByIdWithFlavor():
    ① Dish dish = dishMapper.getById(id)           ← SELECT * FROM dish WHERE id=?
    ② List<DishFlavor> flavors =                    ← SELECT * FROM dish_flavor
           dishFlavorMapper.geByDishId(id)               WHERE dish_id=?
    ③ DishVO dishVO = new DishVO()
       BeanUtils.copyProperties(dish, dishVO)      ← 拷貝同名欄位
       dishVO.setFlavors(flavors)                   ← 手動補上口味列表
    return dishVO
```

> 這裡沒有使用 JOIN + 一次查詢，是因為一個菜品可能對應多條口味，JOIN 會產生笛卡兒積式重複資料，
> 反而需要額外處理去重與組裝；分兩次 SQL 在邏輯上更清晰，且口味表通常資料量極小，效能差異可忽略。

### 6.5 三種資料物件的欄位對比（以菜品為例）

```
         DishDTO (前端→後端)          Dish Entity (對應DB)           DishVO (後端→前端)
        ┌─────────────────┐         ┌─────────────────┐          ┌──────────────────┐
        │ id              │         │ id              │          │ id               │
        │ name            │ ──copy──▶ name            │          │ name             │
        │ categoryId      │         │ categoryId      │          │ categoryId       │
        │ price           │         │ price           │          │ price            │
        │ image           │         │ image           │          │ image            │
        │ description     │         │ description     │          │ description      │
        │ status          │         │ status          │          │ status           │
        │                 │         │                 │          │ updateTime       │
        │                 │         │ createTime  ◄───── AOP填充  │                  │
        │                 │         │ updateTime  ◄───── AOP填充  │ categoryName ◄─── JOIN查詢
        │                 │         │ createUser  ◄───── AOP填充  │                  │
        │                 │         │ updateUser  ◄───── AOP填充  │                  │
        │ flavors (List)──┼─拆分──▶ │     (無此欄位)   │          │ flavors (List)   │
        └─────────────────┘         └─────────────────┘          └──────────────────┘
              │                                                         ▲
              │              DishFlavor Entity                          │
              │             ┌─────────────────┐                        │
              └──拆出來───▶ │ id              │                        │
                            │ dishId ◄──回填主鍵                        │
                            │ name            │  ───查詢時組裝回去────── ┘
                            │ value           │
                            └─────────────────┘
```

> **為什麼需要三種物件？**
>
> - **DishDTO**：前端新增菜品時，會把菜品基本資訊和口味列表「打包」在一起傳過來。
>   DTO 的結構是「方便前端傳輸」的。
> - **Dish Entity**：資料庫的 `dish` 表沒有 `flavors` 欄位（口味存在另一張表），
>   但多了 `createTime` 等審計字段。Entity 的結構是「對齊資料庫表」的。
> - **DishVO**：前端查詢列表時，需要顯示分類名稱 `categoryName`（來自 JOIN），
>   這個欄位既不在 DTO 裡也不在 Entity 裡。VO 的結構是「方便前端展示」的。
>
> 三者各司其職，避免單一物件同時承擔接收、持久化、展示三種責任而變得臃腫。

---

## 七、已完成 vs 待開發功能

### 已完成

| 模組 | 端 | 功能 | 狀態 |
|---|---|---|---|
| 員工管理 | Admin | 登入/登出、新增、分頁查詢、啟用/禁用、查詢、修改 | Done |
| 分類管理 | Admin | 新增、分頁查詢、刪除(含關聯檢查)、修改、啟用/禁用、按類型列表 | Done |
| 菜品管理 | Admin | 新增(含口味)、分頁查詢、批量刪除(含業務校驗)、按 ID 查詢(含口味)、修改菜品(含口味)、起售/停售 | Done |
| 套餐管理 | Admin | 新增(含菜品關聯)、分頁查詢、批量刪除(起售中不可刪)、按 ID 查詢(回顯)、修改套餐、起售/停售(起售前校驗菜品狀態) | Done |
| 通用功能 | Admin | 圖片上傳 (OSS) | Done |
| 店鋪營業狀態 | Admin / User | 管理端讀寫 + 用戶端讀取，狀態存於 Redis | Done |
| 微信登錄 | User | code → openid 換取、首次登錄自動註冊、簽發 user JWT | Done |
| 分類瀏覽 | User (C端) | 按類型查詢啟用中分類列表 | Done |
| 菜品瀏覽 | User (C端) | 按分類 ID 查詢起售菜品（含口味，無口味返回空列表） | Done |
| 套餐瀏覽 | User (C端) | 按分類 ID 查詢起售套餐；按套餐 ID 查詢套餐菜品詳情 | Done |
| 橫切功能 | — | JWT 認證(Admin)、AOP 自動填充、全域異常處理、Swagger 雙分組文檔、Redis 配置 | Done |

### 待開發

| 模組 | 功能 | 說明 |
|---|---|---|
| 用戶端 JWT 攔截器 | `JwtTokenUserInterceptor` | 微信登入已完成；尚缺攔截器校驗 `/user/**` 的 user token，購物車/下單等需要身份校驗的接口依賴此項 |
| 購物車 | 添加/清空購物車 | Entity `ShoppingCart` 已定義 |
| 訂單管理 | 下單、支付、訂單狀態流轉 | Entity `Orders` + `OrderDetail` 已定義 |
| 地址管理 | 收貨地址 CRUD | Entity `AddressBook` 已定義 |
| 數據統計 | 營業額、訂單、用戶、銷量 Top10 | VO 已定義 (`TurnoverReportVO` 等) |
| WebSocket | 來單提醒、催單 | 未開始 |

---

## 八、技術棧

| 層面 | 技術 |
|---|---|
| 框架 | SpringBoot 2.x + Spring + SpringMVC + MyBatis |
| 資料庫 | MySQL 8.x (Druid 連接池) |
| 快取 | Redis (Spring Data Redis + Lettuce) |
| 分頁 | PageHelper |
| 認證 | JWT (jsonwebtoken) |
| API 文檔 | Knife4j (Swagger 增強) |
| 對象儲存 | 阿里雲 OSS |
| 日誌 | SLF4J + Logback |
| 工具 | Lombok, BeanUtils |
| 構建 | Maven 多模組 |
| JDK | Java 17 |

---

## 九、快速啟動

1. 建立 MySQL 資料庫 `sky_take_out` 並匯入建表 SQL
2. 啟動本機 Redis (預設 `localhost:6379`，使用 DB 0)
3. 修改 `sky-server/src/main/resources/application-dev.yml` 中的資料庫 / Redis 連線資訊
4. 啟動 `SkyApplication.java` (需 JDK 17)
5. 訪問 Swagger 文檔：`http://localhost:8080/doc.html`
