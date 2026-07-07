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
│  ShoppingCartController ─ /user/shoppingCart/** (購物車增刪查) │
│  AddressBookController ─ /user/addressBook/** (收貨地址 CRUD)  │
│  OrderController    ── /user/order/**    (用戶下單 + 訂單支付)  │
│                                                               │
│  [支付回調 notify]（微信伺服器回呼，不過 JWT 攔截器）           │
│  PayNotifyController ── /notify/paySuccess (支付成功回調)      │
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
| `user.ShoppingCartController` | `/user/shoppingCart` | 添加商品（菜品/套餐，已存在則 +1）、查看列表、清空、刪減單品（數量 -1，減到 0 刪行）|
| `user.AddressBookController` | `/user/addressBook` | 新增地址、列表查詢、按 ID 查詢、修改、刪除、設為預設、查詢預設地址 |
| `user.OrderController` | `/user/order` | 用戶下單（提交訂單，校驗地址/購物車 → 寫入 orders + order_detail → 清空購物車）、訂單支付（呼叫微信支付生成預支付交易單） |
| `nofity.PayNotifyController` | `/notify` | 微信支付成功回調（解密報文 → 修改訂單狀態）— 不經 JWT 攔截器，由微信伺服器直接回呼 |

> 同名 Controller 衝突規避：`OrderController` 亦在 admin / user 端各有一份，
> 以 `@RestController("userOrderController")` 顯式命名避免衝突。其餘
> `ShopController`、`CategoryController`、`DishController`、`SetmealController`
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
| `ShoppingCartMapper` | shopping_cart | 註解 + XML | list(XML動態條件), updateNumberById(註解), insert(註解), deleteByUserId(註解), deleteById(註解) |
| `AddressBookMapper` | address_book | 註解 | insert, list(動態條件), getById, update, updateIsDefaultByUserId, deleteById |
| `OrderMapper` | orders | XML | insert(XML, useGeneratedKeys 回填主鍵) |
| `OrderDetailMapper` | order_detail | XML | insertBatch(XML foreach 批量插入) |

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

#### 實際應用：菜品列表快取 (Cache-Aside)

顧客端菜品瀏覽（`GET /user/dish/list`）是高頻讀取、低頻寫入的典型場景，
因此以 **Cache-Aside（旁路快取）** 模式將每個分類的菜品列表存入 Redis，
避免每次請求都打穿資料庫。

**快取 key 設計**：`dish_{categoryId}`，例如 `dish_12`。

```
GET /user/dish/list?categoryId=12
         │
         ▼
  Redis.get("dish_12")
         │
    ┌────┴────┐
  存在        不存在
    │              │
    ▼              ▼
直接回傳      DishService.listWithFlavor()
                   │  查 dish + dish_flavor
                   ▼
            Redis.set("dish_12", list)
                   │
                   ▼
              回傳結果
```

**快取失效策略（admin 端寫入時主動清除）**：

| 操作 | 失效範圍 | 說明 |
|---|---|---|
| 新增菜品 `POST /admin/dish` | 精確刪除 `dish_{categoryId}` | 只影響該分類 |
| 修改菜品 `PUT /admin/dish` | 刪除全部 `dish_*` | 分類可能變更，保守清除 |
| 批量刪除 `DELETE /admin/dish` | 刪除全部 `dish_*` | 跨分類操作，保守清除 |
| 起售/停售 `POST /admin/dish/status/{status}` | 刪除全部 `dish_*` | 狀態變更影響展示，保守清除 |

```java
// admin/DishController — 新增時精確清除
String key = "dish_" + dishDTO.getCategoryId();
clearCache(key);

// admin/DishController — 其他寫操作全量清除
redisTemplate.delete(redisTemplate.keys("dish_*"));

// user/DishController — 讀取時 Cache-Aside
String key = "dish_" + categoryId;
List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);
if (list != null && list.size() > 0) return Result.success(list);
list = dishService.listWithFlavor(dish);
redisTemplate.opsForValue().set(key, list);
```

> **注意事項**
> - 目前快取無設 TTL，資料永久保留直至顯式清除或 Redis 重啟。
> - `redisTemplate.keys("dish_*")` 是 O(N) 阻塞操作，生產環境建議改用 SCAN 迭代器。

#### 實際應用：套餐列表快取 (Spring Cache / @Cacheable)

套餐列表（`GET /user/setmeal/list`）與菜品列表場景相同：高頻讀取、低頻寫入。
與菜品快取手寫 `RedisTemplate` 的 Cache-Aside 不同，套餐快取改用 **Spring Cache 註解方式**，
由框架自動管理「讀取時存入、寫入時清除」的快取生命週期，不需要在業務代碼裡直接操作 Redis。

**啟用快取（`SkyApplication.java`）**

```java
@SpringBootApplication
@EnableCaching  // ← 開啟 Spring Cache 支援
public class SkyApplication { ... }
```

**讀取端（`user/SetmealController.java`）**

```java
@GetMapping("/list")
@Cacheable(cacheNames = "setmealCache", key = "#categoryId")
// Redis key 格式：setmealCache::100
public Result<List<Setmeal>> list(Long categoryId) { ... }
```

首次請求時，Spring Cache 呼叫 Service 查資料庫，然後自動將結果序列化並寫入 Redis；
後續相同 `categoryId` 的請求直接從 Redis 返回，不進 Service。

**寫入端（`admin/SetmealController.java`）**

| 操作 | 快取清除範圍 | 說明 |
|---|---|---|
| 新增套餐 `POST /admin/setmeal` | 全量刪除 `setmealCache` | `allEntries = true` |
| 批量刪除 `DELETE /admin/setmeal` | 全量刪除 `setmealCache` | 跨分類，保守清除 |
| 起售/停售 `POST /admin/setmeal/status/{status}` | 全量刪除 `setmealCache` | 狀態變更影響展示 |
| 修改套餐 `PUT /admin/setmeal` | 全量刪除 `setmealCache` | 分類可能變更 |

```java
@CacheEvict(cacheNames = "setmealCache", allEntries = true)
public Result delete(@RequestParam List<Long> ids) { ... }
```

**與菜品快取的方式對比**

| | 菜品快取 | 套餐快取 |
|---|---|---|
| 實作方式 | 手寫 `RedisTemplate` (Cache-Aside) | Spring Cache 註解 (`@Cacheable` / `@CacheEvict`) |
| 代碼侵入性 | 業務方法內有顯式 Redis 操作 | 業務方法保持純粹，快取邏輯靠 AOP 代理 |
| 序列化 | 需確保 value 實作 `Serializable` | 同左，由 Spring Cache + Redis 序列化器處理 |
| Key 格式 | `dish_{categoryId}`（手動拼接） | `setmealCache::{categoryId}`（框架自動生成） |
| 靈活度 | 高（可自訂 TTL、條件、SCAN 等） | 低（需配置 `RedisCacheManager` 才能設 TTL） |

> **何時選哪種？**
> 快取邏輯簡單（讀存/寫清）時優先用 Spring Cache 註解，代碼更整潔；
> 需要細粒度控制（TTL、條件快取、SCAN 清除）時改用手動 `RedisTemplate`。

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

### 5.7 購物車（增 / 刪 / 查）

用戶端購物車是「以當前登入用戶為維度」的暫存資料：同一用戶重複加入同一商品時不再新增列，
而是把既有列的 `number + 1`；不存在時才插入一條，並從 `dish` / `setmeal` 主表把
**名稱、圖片、單價**冗餘快照寫入購物車列（下單前商品可能改價，購物車保留加入當下的展示值）。

#### 接口一覽

| 方法 | 路徑 | 功能 | 入參 |
|---|---|---|---|
| `POST` | `/user/shoppingCart/add` | 添加商品（已存在則 +1） | `ShoppingCartDTO` |
| `GET` | `/user/shoppingCart/list` | 查看當前用戶購物車 | — |
| `DELETE` | `/user/shoppingCart/clean` | 清空當前用戶購物車 | — |
| `POST` | `/user/shoppingCart/sub` | 刪減單品（-1，減到 0 刪行） | `ShoppingCartDTO`（`dishId` / `setmealId` / `dishFlavor`） |

> 「刪減」採官方小程序契約 `POST /user/shoppingCart/sub`，以「商品維度」
> （`dishId` / `setmealId` / `dishFlavor`）而非購物車行主鍵定位購物車項，
> 與 `add` 對稱、可直接對接官方前端。

#### 添加判斷邏輯（存在則 +1，否則插入）

```
POST /user/shoppingCart/add
Body: { dishId / setmealId, dishFlavor }
  │
  ▼
ShoppingCartServiceImpl.addShoppingCart()
  │
  ① DTO → ShoppingCart，並補上 userId = BaseContext.getCurrentId()
  │
  ② shoppingCartMapper.list(cart)   ← 以 userId + dishId/setmealId + dishFlavor 動態查詢
  │
  ├── 查到（已存在）────▶ cart.number + 1 → updateNumberById()  (UPDATE)
  │
  └── 沒查到（不存在）──┐
                       ├─ dishId != null  → dishMapper.getById()  → 取 name/image/price
                       ├─ 否則（套餐）      → setmealMapper.getById()→ 取 name/image/price
                       ├─ number = 1、createTime = now
                       └─ shoppingCartMapper.insert()             (INSERT)
```

#### 商品唯一性如何判定？

`list()` 的動態 SQL 把 `user_id` + `dish_id` / `setmeal_id` + `dish_flavor` 一併作為條件：
同一用戶、同一道菜、同一口味才算「同一個購物車項」。因此「宮保雞丁（微辣）」與
「宮保雞丁（重辣）」會是兩條獨立的購物車列，符合實際點餐情境。

```xml
<!-- ShoppingCartMapper.xml -->
<select id="list" resultType="...ShoppingCart" parameterType="ShoppingCart">
    select * from shopping_cart
    <where>
        <if test="userId   != null"> and user_id   = #{userId}</if>
        <if test="setmealId!= null"> and setmeal_id= #{setmealId}</if>
        <if test="dishId   != null"> and dish_id   = #{dishId}</if>
        <if test="dishFlavor!=null"> and dish_flavor=#{dishFlavor}</if>
    </where>
</select>
```

#### 查看與清空

- **查看** `GET /list`：以 `BaseContext.getCurrentId()` 取當前用戶，呼叫 `list()` 返回該用戶全部購物車列。
- **清空** `DELETE /clean`：`deleteByUserId(currentId)` 一次刪光當前用戶的所有列（如結帳後、手動清空）。

#### 刪減單品（sub，與 add 對稱）

「刪減」是「添加」的逆操作,流程刻意與 `addShoppingCart` 對稱:都用
`user_id + dishId/setmealId + dishFlavor` 透過 `list()` 動態查詢定位購物車項,
差別只在查到之後是 +1 還是 -1。由於 `list()` 的 SQL 永遠帶 `user_id` 條件,
**越權問題天然不存在** —— 用戶只可能定位到自己的購物車列。

```
POST /user/shoppingCart/sub
Body: { dishId / setmealId, dishFlavor }
  │
  ▼
ShoppingCartServiceImpl.subShoppingCart(dto)
  │
  ① DTO → ShoppingCart,並補上 userId = BaseContext.getCurrentId()
  │
  ② list = shoppingCartMapper.list(cart)   ← 與 add 共用同一條動態查詢(含 user_id)
  │
  ├── list 為空（不存在於我的購物車）──▶ 直接返回
  │
  └── cart = list.get(0)
        ├── number > 1  ──▶ number - 1 → updateNumberById()   (UPDATE)
        └── number == 1 ──▶ deleteById(cart.getId())          (DELETE 整列)
```

> **為什麼用 `list()` 而不是直接用行主鍵刪?**
> 官方前端「-」按鈕手上只有商品資訊(`dishId` / `setmealId` / `dishFlavor`),未必持有
> 購物車行主鍵;以商品維度定位才能與 `add` 共用同一套查詢、並與官方契約相容。
> 需要先讀出該列當前 `number` 才能決定「減 1」還是「刪整列」,這次讀取無法省略。
> `deleteById` 憑主鍵刪除,但主鍵來自上一步含 `user_id` 條件的查詢結果,故仍屬當前用戶。

> ⚠️ **前置依賴（尚未完成）**：本功能透過 `BaseContext.getCurrentId()` 取得當前用戶，
> 但用戶端攔截器 `JwtTokenUserInterceptor` 目前**仍未實作、也未在 `WebMvcConfiguration` 註冊**
> （現只註冊 admin 攔截器，攔 `/admin/**`）。在補上用戶端攔截器把 `userId` 寫入
> `ThreadLocal` 之前，`/user/shoppingCart/**` 取到的 `userId` 會是 `null`，功能無法真正跑通。
> 詳見第七節「待開發」。

---

### 5.8 微信支付（下單支付 / 支付回調）

接續「下單」之後的環節：前端拿到訂單號後呼叫支付接口，後端透過微信支付 **JSAPI 統一下單**
換取「預支付交易單」並二次簽名回傳給小程序調起收銀台；用戶付款後，微信伺服器**主動回呼**
後端的 `/notify/paySuccess`，後端解密報文、把訂單狀態改為「待接單 / 已支付」。

> ⚠️ **本機無法實跑**：本倉庫**不具備微信支付商戶資質**，也沒有對應的商戶證書 /
> `apiV3Key` / 私鑰，`application-dev.yml` 的 `sky.wechat` 全為空佔位。因此本節描述的是
> **程式碼鏈路與邏輯完整性**，呼叫 `weChatPayUtil.pay()` 在真實環境才能成功；
> 支付金額目前也**寫死為 0.01 元**（`OrderServiceImpl.payment`），未取用真實訂單金額。

#### 接口一覽

| 方法 | 路徑 | 功能 | 入參 / 觸發方 |
|---|---|---|---|
| `PUT` | `/user/order/payment` | 訂單支付：生成預支付交易單 | `OrdersPaymentDTO`（前端調用） |
| `POST` | `/notify/paySuccess` | 支付成功回調：改訂單狀態 | 加密報文（**微信伺服器**回呼，不過 JWT 攔截器） |

#### 整體時序

```
 小程序前端              sky-server                    微信支付平台
    │                       │                              │
    │ PUT /user/order/payment(orderNumber)                 │
    │──────────────────────▶│                              │
    │                       │ ① userMapper.getById → openid │
    │                       │ ② weChatPayUtil.pay()         │
    │                       │   jsapi 統一下單 ─────────────▶│
    │                       │◀──────────── prepay_id ───────│
    │                       │ ③ 二次簽名(SHA256withRSA)      │
    │◀── OrderPaymentVO ────│   (timeStamp/nonceStr/        │
    │   (調起收銀台所需參數)  │    package/signType/paySign)  │
    │                       │                              │
    │ ④ 用戶輸入密碼付款 ───────────────────────────────────▶│
    │                       │                              │
    │                       │◀═══ POST /notify/paySuccess ══│ (微信主動回呼，加密)
    │                       │ ⑤ 解密(AesUtil + apiV3Key)    │
    │                       │ ⑥ orderService.paySuccess     │
    │                       │   status=待接單, payStatus=已付 │
    │                       │ ⑦ 回 {code:SUCCESS} 給微信 ───▶│
```

#### 程式碼鏈路

**(A) 發起支付** — `OrderServiceImpl.payment(OrdersPaymentDTO)`

```
① userId = BaseContext.getCurrentId()  → userMapper.getById(userId) 取 openid
② weChatPayUtil.pay(orderNumber, 0.01, "苍穹外卖订单", openid)
       └─ jsapi() 統一下單 → 拿 prepay_id → 二次簽名組裝 JSONObject
③ 若回傳含 code == "ORDERPAID" → 拋 OrderBusinessException("该订单已支付")
④ jsonObject.toJavaObject(OrderPaymentVO) + 手動 setPackageStr(getString("package"))
       （"package" 是保留字，無法直接映射到 packageStr，故單獨塞）
⑤ return OrderPaymentVO {nonceStr, paySign, timeStamp, signType, packageStr}
```

**(B) 支付回調** — `PayNotifyController.paySuccessNotify`

```
① readData(request)            讀取微信 POST 的原始 JSON 報文
② decryptData(body)            取 resource.{ciphertext,nonce,associated_data}
                               AesUtil(apiV3Key).decryptToString(...) 解出明文
③ 解析 out_trade_no(商戶訂單號) / transaction_id(微信交易號)
④ orderService.paySuccess(outTradeNo)
       └─ getByNumber(outTradeNo) → update{status=待接單, payStatus=已付, checkoutTime=now}
⑤ responseToWeixin(response)   回 200 + {code:SUCCESS, message:SUCCESS}
                               （未回 SUCCESS 微信會持續重發回調）
```

#### 配置欄位（`sky.wechat`，對應 `WeChatProperties`）

| YAML key | 欄位 | 說明 |
|---|---|---|
| `appid` / `secret` | appid / secret | 小程序 appid 與密鑰（與微信登錄共用） |
| `mchid` | mchid | 商戶號 |
| `mchSerialNo` | mchSerialNo | 商戶 API 證書序列號 |
| `privateKeyFilePath` | privateKeyFilePath | 商戶私鑰檔路徑（下單簽名用） |
| `apiV3Key` | apiV3Key | APIv3 密鑰（**回調報文解密用**） |
| `weChatPayCertFilePath` | weChatPayCertFilePath | 微信支付平台證書路徑 |
| `notifyUrl` | notifyUrl | 支付成功回調地址 |
| `refundNotifyUrl` | refundNotifyUrl | 退款成功回調地址 |

> ⚠️ 早期 YAML 把後兩個 key 誤寫為 `weChatCertfilePath` / `noteifyUrl`，與 `WeChatProperties`
> 欄位名不符，Spring 寬鬆綁定無法匹配 → 填入真實值時會綁成 `null`（下單 `notify_url` 為空、
> 載入平台證書 NPE）。**現已更正為 `weChatPayCertFilePath` / `notifyUrl`。**

#### 已知限制 / 待補（誠實標註）

- **「來單提醒」未實現**：回調註解寫了「修改訂單狀態、來單提醒」，但 `paySuccess` 只更新狀態；
  全專案尚無任何 WebSocket，付款後商家端的來單推送這一步是缺的（見第七節）。
- **回調非冪等**：微信會重發回調，`paySuccess` 未判斷 `payStatus` 是否已為「已付」即無條件 update。
- **回調未判空**：`getByNumber` 查不到訂單時 `ordersDB.getId()` 會 NPE。
- **未驗簽**：僅用 `apiV3Key` 解密，未校驗 `Wechatpay-Signature` 請求頭，無法完全防偽造回調。

---

### 5.9 訂單流轉（用戶端歷史訂單 / 商家端訂單管理）

支付完成後，訂單進入完整的狀態機流轉：用戶端可查歷史、看詳情、取消、再來一單；商家端可搜尋、
統計、接單、拒單、派送、完成。兩端共用同一套 `OrderService` / `OrderMapper`，用 `Orders` 的
**狀態常量**驅動整個生命週期。

#### 訂單狀態機（`Orders` 常量）

```
 PENDING_PAYMENT(1)  待付款
        │ 支付回調 paySuccess
        ▼
 TO_BE_CONFIRMED(2)  待接單 ──── 商家 rejection 拒單 ──▶ CANCELLED(6) 已取消
        │ 商家 confirm 接單                              ▲
        ▼                                                │ 用戶 cancel（僅 1/2 狀態）
 CONFIRMED(3)        已接單 ──── 商家 cancel 取消 ───────┘
        │ 商家 delivery 派送（僅 3 狀態）
        ▼
 DELIVERY_IN_PROGRESS(4) 派送中
        │ 商家 complete 完成（僅 4 狀態）
        ▼
 COMPLETED(5)        已完成

 付款狀態 payStatus：UN_PAID(0) / PAID(1) / REFUND(2)
```

#### 接口一覽

| 端 | 方法 | 路徑 | 功能 | Service 方法 |
|---|---|---|---|---|
| User | `GET` | `/user/order/historyOrders` | 歷史訂單分頁查詢（可按狀態） | `pageQuery4User` |
| User | `GET` | `/user/order/orderDetail/{id}` | 訂單詳情（含明細） | `details` |
| User | `PUT` | `/user/order/cancel/{id}` | 用戶取消訂單（待接單需退款） | `userCancelById` |
| User | `POST` | `/user/order/repetition/{id}` | 再來一單（明細回灌購物車） | `repetition` |
| Admin | `GET` | `/admin/order/conditionSearch` | 訂單搜尋（號碼/手機/狀態/時間） | `conditionSearch` |
| Admin | `GET` | `/admin/order/statistics` | 各狀態訂單數量統計 | `statistics` |
| Admin | `GET` | `/admin/order/details/{id}` | 訂單詳情 | `details` |
| Admin | `PUT` | `/admin/order/confirm` | 接單 | `confirm` |
| Admin | `PUT` | `/admin/order/rejection` | 拒單（已付需退款） | `rejection` |
| Admin | `PUT` | `/admin/order/cancel` | 商家取消（已付需退款） | `cancel` |
| Admin | `PUT` | `/admin/order/delivery/{id}` | 派送訂單 | `delivery` |
| Admin | `PUT` | `/admin/order/complete/{id}` | 完成訂單 | `complete` |

> 商家端 Controller 為新建的 `admin/OrderController`，以 `@RestController("adminOrderController")`
> 命名避免與用戶端 `userOrderController` 的 Bean 名衝突。

#### 業務規則重點

- **用戶取消**（`userCancelById`）：僅 `待付款/待接單`（status ≤ 2）可直接取消；`待接單` 已付需
  呼叫 `weChatPayUtil.refund()` 退款並把 `payStatus` 改為 `REFUND`；狀態轉 `CANCELLED`，寫入
  取消原因「用戶取消」與取消時間。
- **再來一單**（`repetition`）：依訂單 id 撈 `order_detail`，`BeanUtils.copyProperties(x, sc, "id")`
  轉回 `ShoppingCart`（排除主鍵）、補 `userId` 與 `createTime`，再 `insertBatch` 批量寫回購物車。
- **訂單搜尋**（`conditionSearch`）：`PageHelper` 分頁後，額外把每筆訂單的明細拼成
  `宮保雞丁*3;` 字串塞進 `OrderVO.orderDishes`，供列表頁展示。
- **接單/派送/完成**：本質是狀態機推進，`delivery` 僅接受 `CONFIRMED(3)`、`complete` 僅接受
  `DELIVERY_IN_PROGRESS(4)`，狀態不符拋 `ORDER_STATUS_ERROR`。
- **拒單/商家取消**：若 `payStatus == PAID` 先退款再改 `CANCELLED`，並記錄拒單/取消原因。

#### 程式碼鏈路（歷史訂單為例）

```
GET /user/order/historyOrders?page=1&pageSize=10&status=
  │
  ▼ OrderController.page → orderService.pageQuery4User
① PageHelper.startPage(page, pageSize)
② 組 OrdersPageQueryDTO：userId = BaseContext.getCurrentId()、status
③ orderMapper.pageQuery(dto)          → OrderMapper.xml 動態 SQL，order_time desc
④ 遍歷每筆 Orders：
       orderDetailMapper.getByOrderId(id) 撈明細
       BeanUtils.copyProperties → OrderVO.setOrderDetailList
⑤ return PageResult(total, List<OrderVO>)
```

新增的持久層方法：`OrderMapper.pageQuery / getById / countStatus`（後兩者為 `@Select` 註解）、
`OrderMapper.xml` 的 `<select id="pageQuery">`、`OrderDetailMapper.getByOrderId`、
`ShoppingCartMapper.insertBatch`（＋ `ShoppingCartMapper.xml` 的 `<insert id="insertBatch">`）。

#### 已知限制 / 待補（誠實標註）

- **用戶端仍無 JWT 攔截器**：`/user/**` 尚未註冊攔截器（僅 `/admin/**` 有），`historyOrders` /
  `orderDetail` / `cancel` / `repetition` 依賴的 `BaseContext.getCurrentId()` 取不到 `userId`，
  與購物車同屬待補此項才能真正跑通（見第七節）。
- **退款不可實跑**：`userCancelById` / `rejection` / `cancel` 呼叫的 `weChatPayUtil.refund()`
  與微信支付同樣缺商戶資質，金額亦寫死 `0.01`。
- **`cancel` 未判空**：商家取消 `getById` 查不到訂單時 `getPayStatus()` 會 NPE（拒單有判空、取消無）。

---

### 5.10 配送範圍校驗（百度地圖）

下單前校驗收貨地址與店鋪的**駕車距離**是否超過 5km，超出則拒絕下單。已接入
`OrderServiceImpl.submitOrder`（地址判空之後、寫訂單之前）。

#### 呼叫鏈（`checkOutOfRange`，三次 HTTP）

```
① geocoding/v3(address = 店鋪地址)  → 解析店鋪經緯度  shopLngLat
② geocoding/v3(address = 收貨地址)  → 解析用戶經緯度  userLngLat
③ directionlite/v1/driving(origin=店鋪, destination=用戶)
       → result.routes[0].distance（公尺）
   distance > 5000 → 拋 OrderBusinessException("超出配送范围")
   任一步 status != "0" → 拋「店铺地址解析失败 / 收货地址解析失败 / 配送路线规划失败」
```

透過 `HttpClientUtil.doGet` 呼叫百度 Web 服務 API，回應以 fastjson `JSONObject/JSONArray` 解析。

#### 配置欄位

| YAML key | 注入欄位 | 說明 |
|---|---|---|
| `sky.shop.address` | `@Value shopAddress` | 外賣商家店鋪地址（校驗起點） |
| `sky.baidu.ak` | `@Value ak` | 百度地圖開放平台 AK |

> ⚠️ **AK 為空時無法下單**：`application-dev.yml` 的 `sky.baidu.ak` 留空佔位，因 `checkOutOfRange`
> 已接入 `submitOrder`，AK 未填時第一次 `geocoding` 即回非 `0` 狀態 → 拋「店铺地址解析失败」，
> 導致下單中斷。練習環境若無 AK，需自行申請填入，或暫時註解掉 `submitOrder` 內的
> `checkOutOfRange(...)` 呼叫。

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
| 購物車 | User (C端) | 添加（已存在則 +1）、查看列表、清空、刪減單品（`sub`，-1，減到 0 刪行，與 add 對稱） | 部分（依賴用戶端攔截器） |
| 地址管理 | User (C端) | 收貨地址新增、列表、按 ID 查詢、修改、刪除、設為預設、查詢預設地址 | Done |
| 微信支付 | User (C端) | 下單支付（JSAPI 統一下單 + 二次簽名）、支付成功回調（解密 → 改訂單狀態） | 程式碼完成（無商戶資質，無法實跑；金額寫死 0.01、缺來單提醒/冪等/驗簽） |
| 訂單下單 | User (C端) | `submitOrder`：地址/購物車校驗 → 寫 `orders` + 批量 `order_detail` → 清空購物車（`@Transactional`） | Done |
| 歷史訂單 | User (C端) | 分頁查詢（可按狀態）、訂單詳情、取消訂單（待接單退款）、再來一單（回灌購物車） | 程式碼完成（依賴用戶端攔截器補齊 `userId`；退款無法實跑） |
| 訂單管理 | Admin | 訂單搜尋、各狀態統計、詳情、接單、拒單、商家取消、派送、完成（狀態機推進 + 退款位） | 程式碼完成（退款無法實跑；商家取消未判空） |
| 配送範圍校驗 | User (C端) | 下單前經百度地圖 geocoding + 路線規劃校驗店鋪↔收貨距離 ≤ 5km | 程式碼完成（需真實 AK；AK 空值會中斷下單） |
| 套餐列表快取 | User / Admin | Spring Cache `@Cacheable` + `@CacheEvict`，key = `setmealCache::{categoryId}` | Done |
| 橫切功能 | — | JWT 認證(Admin)、AOP 自動填充、全域異常處理、Swagger 雙分組文檔、Redis 配置 | Done |

### 待開發

| 模組 | 功能 | 說明 |
|---|---|---|
| 用戶端 JWT 攔截器 | `JwtTokenUserInterceptor` | 微信登入已完成；尚缺攔截器校驗 `/user/**` 的 user token 並把 `userId` 寫入 `BaseContext`。**購物車、歷史訂單、取消、再來一單各接口已實作但取不到 `userId`，必須先補此項才能跑通** |
| 百度地圖 AK | `sky.baidu.ak` | 配送範圍校驗程式碼已完成，但 AK 留空佔位；未填時 `checkOutOfRange` 會中斷下單，需申請真實 AK |
| 數據統計 | 營業額、訂單、用戶、銷量 Top10 | VO 已定義 (`TurnoverReportVO` 等) |
| WebSocket | 來單提醒、催單 | 未開始；支付回調 `paySuccess` 已預留「來單提醒」位置但尚未推送 |

---

## 八、技術棧

| 層面 | 技術 |
|---|---|
| 框架 | SpringBoot 2.x + Spring + SpringMVC + MyBatis |
| 資料庫 | MySQL 8.x (Druid 連接池) |
| 快取 | Redis (Spring Data Redis + Lettuce) + Spring Cache (`@Cacheable` / `@CacheEvict`) |
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
4. （選配）於 `application-dev.yml` 填入 `sky.baidu.ak`（百度地圖 AK）；**留空時 `/user/order/submit`
   下單會因配送範圍校驗失敗而中斷**，練習環境可暫時註解 `OrderServiceImpl.submitOrder` 內的
   `checkOutOfRange(...)` 呼叫
5. 啟動 `SkyApplication.java` (**須用 JDK 17**；Lombok 1.18.30 無法在過新的 JDK（實測 JDK 25）
   上跑注解處理器，會出現大量 `@Data`/`@Slf4j`「找不到符號」編譯錯誤。可 `export JAVA_HOME` 指向
   JDK 17 後再 `mvn clean compile`)
6. 訪問 Swagger 文檔：`http://localhost:8080/doc.html`
