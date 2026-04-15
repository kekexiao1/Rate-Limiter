-- 令牌桶限流Lua脚本
-- 在 Redis 服务端原子执行
-- KEYS[1]: 令牌数key (rate_limit:{key}_tokens)
-- KEYS[2]: 时间戳key (rate_limit:{key}_timestamp)
-- ARGV[1]: 桶容量
-- ARGV[2]: 补充速率(每秒补充的令牌数)
-- ARGV[3]: 时间窗口(秒)

-- 获取 Redis 服务端当前时间（防客户端时钟漂移）
-- redis.call('TIME') 返回一个包含两个元素的数组
-- currentTime[1]：当前 Unix 时间戳（秒，从 1970-01-01 开始）
-- currentTime[2]：当前秒内的微秒数（0 ~ 999999）
local currentTime = redis.call('TIME')
local serverTime = tonumber(currentTime[1]) * 1000 + math.floor(tonumber(currentTime[2]) / 1000)

-- 读取当前桶内剩余令牌数和上次填充时间
local currentTokens = redis.call('GET', KEYS[1])
local lastRefillTime = redis.call('GET', KEYS[2])

-- 初始化或补充令牌
if not currentTokens or not lastRefillTime then
    -- 第一次使用，初始化桶
    currentTokens = ARGV[1]
    lastRefillTime = serverTime
    redis.call('SET', KEYS[1], currentTokens)
    redis.call('SET', KEYS[2], lastRefillTime)
    -- 设置过期时间（窗口时间+1秒缓冲）
    redis.call('EXPIRE', KEYS[1], tonumber(ARGV[3]) + 1)
    redis.call('EXPIRE', KEYS[2], tonumber(ARGV[3]) + 1)
else
    -- 计算距离上次填充过去了多少秒
    local timePassed = (serverTime - tonumber(lastRefillTime)) / 1000.0
    if timePassed > 0 then
        -- 按比例计算出应增加的令牌数（不能超过桶容量上限）
        local tokensToAdd = math.floor(timePassed * tonumber(ARGV[2]))
        if tokensToAdd > 0 then
            currentTokens = math.min(tonumber(ARGV[1]), tonumber(currentTokens) + tokensToAdd)
            lastRefillTime = serverTime
            redis.call('SET', KEYS[1], currentTokens)
            redis.call('SET', KEYS[2], lastRefillTime)
        end
    end
end

-- 判断剩余令牌是否大于等于申请数
local tokensToAcquire = 1  -- 默认申请1个令牌
if tonumber(currentTokens) >= tokensToAcquire then
    -- 足够则扣减并返回放行及剩余数
    redis.call('DECRBY', KEYS[1], math.floor(tokensToAcquire))
    local remaining = math.floor(tonumber(currentTokens) - tokensToAcquire)
    return {1, remaining, 0}  -- {allowed, remaining, waitMs}
else
    -- 不足则返回拒绝及预计等待时间
    local tokensNeeded = tokensToAcquire - tonumber(currentTokens)
    local waitMs = math.ceil(tokensNeeded / tonumber(ARGV[3]) * 1000)
    return {0, 0, waitMs}  -- {allowed, remaining, waitMs}
end