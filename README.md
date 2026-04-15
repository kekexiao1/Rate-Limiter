# Rate-Limiter 限流框架

基于 Spring Boot 3.x 的高性能限流框架，支持多种限流算法、动态配置、自动降级等特性。

## � 项目结构说明

这是一个多模块的 Spring Boot Starter 项目，采用标准的 Maven 多模块结构：

```
Rate-Limiter/ (父模块 - pom)
├── rate-limiter-core/ (核心模块)
├── rate-limiter-config/ (配置管理模块) 
├── rate-limiter-redis/ (Redis限流模块)
├── rate-limiter-fallback/ (降级模块)
├── rate-limiter-spring-boot-starter/ (Starter模块)
├── rate-limiter-test/ (测试示例模块)
└── src/main/resources/application.yml (父模块配置示例)
```

### 配置说明

**父模块配置 (`src/main/resources/application.yml`)**：
- 作为配置示例和开发环境参考
- 提供完整的配置模板
- 实际使用时，用户应在其应用中进行配置

**测试模块配置 (`rate-limiter-test/src/main/resources/application.yml`)**：
- 独立的测试应用配置
- 用于演示和功能测试

**作为 Starter 使用**：用户只需要引入 `rate-limiter-spring-boot-starter` 依赖，并在自己的应用中配置相关参数。

## �📋 目录

- [核心特性](#核心特性)
- [架构设计](#架构设计)
- [快速开始](#快速开始)
- [Nacos配置示例](#nacos配置示例)
- [使用示例](#使用示例)
- [降级机制](#降级机制)
- [技术栈](#技术栈)

## ✨ 核心特性

### 🎯 多种限流算法
- **滑动窗口算法 (SLIDING_WINDOW)**：精确控制时间窗口内的请求数量
- **令牌桶算法 (TOKEN_BUCKET)**：支持突发流量，平滑限流

### 🔄 动态配置管理
- 基于 Nacos 的分布式配置中心
- 实时推送限流规则变更
- 本地缓存机制，提高性能

### 🛡️ 高可用容错
- Redis 宕机自动降级到本地限流
- 完善的异常处理机制
- Redis 健康状态监控

### 🚀 易于集成
- Spring Boot Starter 开箱即用
- 注解式限流，零侵入
- 支持自定义降级处理

## 🏗️ 架构设计

```
┌─────────────────────────────────────────────────────────────┐
│                    Spring Boot Application                   │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│              rate-limiter-spring-boot-starter               │
│  ┌──────────────────────────────────────────────────────┐  │
│  │           @RateLimiter 注解 + AOP 切面                │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                              │
                ┌─────────────┴─────────────┐
                ▼                           ▼
┌──────────────────────────┐    ┌──────────────────────────┐
│   rate-limiter-redis     │    │  rate-limiter-fallback   │
│  ┌────────────────────┐  │    │  ┌────────────────────┐  │
│  │  Redis 限流算法    │  │    │  │  本地限流算法      │  │
│  │  - 滑动窗口        │  │    │  │  - 滑动窗口        │  │
│  │  - 令牌桶          │  │    │  └────────────────────┘  │
│  └────────────────────┘  │    └──────────────────────────┘
│  ┌────────────────────┐  │
│  │  健康监控          │  │
│  └────────────────────┘  │
└──────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────────────────────┐
│                   rate-limiter-config                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  Nacos 配置监听 + 规则管理器                         │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
                │
                ▼
┌─────────────────────────────────────────────────────────────┐
│                     rate-limiter-core                        │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  核心接口定义 + 限流模型 + 异常体系                   │  │
│  └──────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────┘
```

## 🚀 快速开始

### 1. 添加依赖

在你的 Spring Boot 项目中添加依赖：

```xml
<dependency>
    <groupId>com.xiao</groupId>
    <artifactId>rate-limiter-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### 2. 配置 Nacos（可选，用于动态配置）

在 `application.yml` 中配置 Nacos：

```yaml
spring:
  application:
    name: your-application
  cloud:
    nacos:
      config:
        server-addr: localhost:8848
        namespace: your-namespace
        group: DEFAULT_GROUP
        file-extension: yaml
  config:
    import:
      - nacos:rate-limiter-rules.yaml?group=DEFAULT_GROUP&refresh=true
```

### 3. 配置 Redis

```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      password: your-password
      database: 0
```

### 4. 使用注解

在需要限流的方法上添加 `@RateLimiter` 注解：

```java
@RestController
public class UserController {
    
    @GetMapping("/api/users")
    @RateLimiter(key = "getUsers", limit = 100, window = 60)
    public Result<List<User>> getUsers() {
        // 业务逻辑
    }
}
```

### 配置说明

**最小化配置**：如果不需要动态配置和分布式限流，可以只添加依赖和使用注解：

```yaml
# 最小配置示例
spring:
  application:
    name: your-app
```

**完整配置**：如果需要所有功能，参考父模块的配置示例：

```yaml
# 完整配置（参考父模块 application.yml）
spring:
  application:
    name: rate-limiter
  config:
    import: optional:rate-limiter-rules.yaml
  cloud:
    nacos:
      config:
        server-addr: 127.0.0.1:8848
        namespace: public
        group: DEFAULT_GROUP
        file-extension: yaml
        refresh-enabled: true
  data:
    redis:
      password: 123456
      database: 0
      host: 127.0.0.1
      port: 6379
```

## 📝 Nacos配置示例

### 完整配置示例

在 Nacos 配置中心创建 `rate-limiter-rules.yaml` 配置文件：

```yaml
rate-limit:
  enabled: true
  fallback:
    enabled: true 
    limit: 500      
    window: 1    
    algorithm: LOCAL  
  global:
    enabled: true
    limit: 50           
    window: 1          
    algorithm: SLIDING_WINDOW 
  rules:
    - key: "api:test:basic"
      limit: 40       
      window: 1         
      algorithm: TOKEN_BUCKET 
    - key: "'api:test:user:' + #userId"
      limit: 400
      window: 1
      algorithm: SLIDING_WINDOW
```

### 配置字段说明

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `enabled` | Boolean | 否 | 限流功能总开关，默认 true |
| `rules` | List | 是 | 限流规则列表 |
| `rules[].key` | String | 是 | 限流资源标识，需要与注解中的 key 对应 |
| `rules[].limit` | Integer | 是 | 限流阈值，在时间窗口内允许的最大请求数 |
| `rules[].window` | Integer | 是 | 时间窗口大小，单位：秒 |
| `rules[].type` | String | 是 | 限流算法类型：SLIDING_WINDOW 或 TOKEN_BUCKET |

### 算法选择建议

#### 滑动窗口算法 (SLIDING_WINDOW)
- **适用场景**：需要严格控制请求数量的场景
- **特点**：精确控制，无突发流量
- **推荐用于**：支付、短信发送、敏感操作等

#### 令牌桶算法 (TOKEN_BUCKET)
- **适用场景**：允许一定程度的突发流量
- **特点**：平滑限流，支持突发
- **推荐用于**：查询、搜索、普通业务接口等

## 💡 使用示例

### 基础用法

```java
@RestController
@RequestMapping("/api")
public class ApiController {
    
    // 使用 Nacos 配置的规则
    @GetMapping("/users/{id}")
    @RateLimiter(key = "getUserById", limit = 1000, window = 60)
    public Result<User> getUserById(@PathVariable Long id) {
        User user = userService.getById(id);
        return Result.success(user);
    }
    
    // 使用注解指定的规则（不使用 Nacos 配置）
    @PostMapping("/users")
    @RateLimiter(key = "createUser", limit = 500, window = 60)
    public Result<User> createUser(@RequestBody UserDTO userDTO) {
        User user = userService.create(userDTO);
        return Result.success(user);
    }
}
```

### 自定义降级处理

```java
@Component
public class CustomFallbackHandler implements RateLimiterFallbackHandler {
    
    @Override
    public Object handle(ProceedingJoinPoint joinPoint, RateLimitException e) {
        // 自定义降级逻辑
        return Result.error(429, "系统繁忙，请稍后重试");
    }
}

@RestController
public class OrderController {
    
    @PostMapping("/orders")
    @RateLimiter(
        key = "createOrder", 
        limit = 200, 
        window = 60,
        fallbackHandler = CustomFallbackHandler.class
    )
    public Result<Order> createOrder(@RequestBody OrderDTO orderDTO) {
        Order order = orderService.create(orderDTO);
        return Result.success(order);
    }
}
```

### 动态调整限流规则

在 Nacos 控制台修改 `rate-limiter-rules.yaml` 配置，系统会自动感知并应用新规则：

```yaml
# 实时调整限流阈值
rules:
  - key: getUserById
    limit: 2000  # 从 1000 调整为 2000
    window: 60
    algorithm: SLIDING_WINDOW
```

## 🛡️ 降级机制

### Redis 宕机自动降级

当 Redis 不可用时，系统会自动降级到本地限流算法，确保服务持续可用：

```
正常流程：
请求 → @RateLimiter → Redis限流算法 → Redis执行 → 返回结果

降级流程：
请求 → @RateLimiter → Redis限流算法 → Redis异常
     ↓
捕获异常 → 降级到LocalRateLimitAlgorithm → 本地限流 → 返回结果
```

### 降级特性

1. **自动降级**：Redis 连接失败、超时等异常时自动切换
2. **透明切换**：对业务代码完全透明，无感知
3. **详细日志**：记录降级过程，便于监控
4. **健康监控**：定期检查 Redis 状态（每30秒）
5. **自动恢复**：Redis 恢复后自动切回

### 监控日志

```
# Redis 宕机
2024-01-01 10:00:00 ERROR - Redis连接失败，无法执行限流脚本: Connection refused
2024-01-01 10:00:00 WARN  - Redis不可用，自动降级到本地限流算法: key=getUserById
2024-01-01 10:00:00 INFO  - 已降级到本地限流算法，限流检查完成: key=getUserById

# Redis 恢复
2024-01-01 10:05:00 INFO  - Redis连接已恢复，将恢复使用Redis限流算法
```

## 🔧 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.2.4 | 基础框架 |
| Spring Cloud | 2023.0.3 | 微服务框架 |
| Spring Cloud Alibaba | 2023.0.3.2 | 阿里巴巴微服务套件 |
| Nacos | 2.x | 配置中心和服务发现 |
| Redis | 3.3.0 | 分布式缓存和限流 |
| Lua | - | Redis 脚本执行 |
| AspectJ | - | AOP 切面编程 |
| Lombok | 1.18.38 | 简化代码 |
| Jackson | - | JSON 序列化 |

## 📦 模块说明

| 模块 | 说明 |
|------|------|
| rate-limiter-core | 核心接口定义、限流模型、异常体系 |
| rate-limiter-config | Nacos 配置监听、规则管理 |
| rate-limiter-redis | Redis 限流算法实现、健康监控 |
| rate-limiter-fallback | 本地限流算法、降级处理 |
| rate-limiter-spring-boot-starter | Spring Boot Starter，自动配置 |

## 🎯 最佳实践

### 1. 合理设置限流阈值
- 根据系统承载能力设置阈值
- 考虑突发流量，预留缓冲空间
- 不同接口设置不同阈值

### 2. 选择合适的算法
- 严格控制场景：使用滑动窗口
- 允许突发场景：使用令牌桶
- 查询类接口：可适当放宽限流

### 3. 监控和告警
- 监控限流触发频率
- 设置 Redis 宕机告警
- 关注降级切换日志

### 4. 测试验证
- 压测验证限流效果
- 测试 Redis 宕机降级
- 验证配置动态更新

## 📄 许可证

本项目采用 Apache 2.0 许可证。

## 👥 贡献

欢迎提交 Issue 和 Pull Request！
