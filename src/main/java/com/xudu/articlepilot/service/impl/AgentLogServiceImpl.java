package com.xudu.articlepilot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.xudu.articlepilot.mapper.AgentLogMapper;
import com.xudu.articlepilot.model.entity.AgentLog;
import com.xudu.articlepilot.model.vo.AgentExecutionStats;
import com.xudu.articlepilot.service.AgentLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* @author xudu
* @description 针对表【agent_log(智能体执行日志表)】的数据库操作Service实现
* @createDate 2026-04-07 13:23:35
*/
@Service
@Slf4j
public class AgentLogServiceImpl extends ServiceImpl<AgentLogMapper, AgentLog>
    implements AgentLogService{

    @Override
    @Async
    public void saveLogAsync(AgentLog agentLog) {
        try {
            this.save(agentLog);
            log.info("智能体日志已保存, taskId={}, agentName={}, status={}, durationMs={}",
                    agentLog.getTaskId(), agentLog.getAgentName(), agentLog.getStatus(), agentLog.getDurationMs());
        } catch (Exception e) {
            log.error("保存智能体日志失败, taskId={}, agentName={}",
                    agentLog.getTaskId(), agentLog.getAgentName(), e);
        }
    }

    @Override
    public List<AgentLog> getLogsByTaskId(String taskId) {

        LambdaQueryWrapper<AgentLog> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(AgentLog::getTaskId, taskId)
                .orderByAsc(AgentLog::getCreateTime);
        return this.list(queryWrapper);
    }

    @Override
    public AgentExecutionStats getExecutionStats(String taskId) {
        List<AgentLog> logs = getLogsByTaskId(taskId);

        if (logs == null || logs.isEmpty()) {
            return AgentExecutionStats.builder()
                    .taskId(taskId)
                    .agentCount(0)
                    .totalDurationMs(0)
                    .overallStatus("NOT_FOUND")
                    .build();
        }

        // 计算统计数据
        int totalDuration = 0;
        Map<String, Integer> agentDurations = new HashMap<>();
        String overallStatus = "SUCCESS";

        for (AgentLog log : logs) {
            // 累加总耗时
            if (log.getDurationMs() != null) {
                totalDuration += log.getDurationMs();
                agentDurations.put(log.getAgentName(), log.getDurationMs());
            }

            // 判断总体状态
            if ("FAILED".equals(log.getStatus())) {
                overallStatus = "FAILED";
            } else if ("RUNNING".equals(log.getStatus()) && !"FAILED".equals(overallStatus)) {
                overallStatus = "RUNNING";
            }
        }

        return AgentExecutionStats.builder()
                .taskId(taskId)
                .totalDurationMs(totalDuration)
                .agentCount(logs.size())
                .agentDurations(agentDurations)
                .overallStatus(overallStatus)
                .logs(logs)
                .build();
    }
}




