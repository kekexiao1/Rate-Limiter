-- 滑动窗口限流Lua脚本
-- 在 Redis 服务端原子执行
-- KEYS[1]: 限流key (rate_limit:{key})
-- ARGV[1]: 时间窗口(毫秒)
-- ARGV[2]: 限流阈值
-- ARGV[3]: 唯一请求ID

-- 获取 Redis 服务端当前时间（防客户端时钟漂移）
local currentTime = redis.call('TIME')
-- 先拼成带小数的秒，再乘 1000 转毫秒，最后 floor 确保绝对是整数
local serverTime = math.floor((tonumber(currentTime[1]) + tonumber(currentTime[2]) / 1000000) * 1000)


-- 计算窗口起始时间
local windowStartTime = serverTime - tonumber(ARGV[1])

-- 使用 ZREMRANGEBYSCORE 清理窗口外的旧请求记录
redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, windowStartTime)

-- 使用 ZCARD 统计当前窗口内的请求数
local currentCount = redis.call('ZCARD', KEYS[1])

-- 判断是否超限
if tonumber(currentCount) >= tonumber(ARGV[2]) then
    -- 超限，返回拒绝结果
    return {0, 0, 0}  -- {allowed, remaining, waitMs}
else
    -- 未超限，添加新记录
    redis.call('ZADD', KEYS[1], serverTime, ARGV[3])
    
    -- 设置过期时间（窗口时间+1秒缓冲）
    redis.call('EXPIRE', KEYS[1], math.floor(tonumber(ARGV[1]) / 1000) + 1)
    
    -- 返回放行结果
    local remaining = tonumber(ARGV[2]) - currentCount - 1
    return {1, remaining, 0}  -- {allowed, remaining, waitMs}
end