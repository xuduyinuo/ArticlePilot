package com.xudu.articlepilot.mapper;

import com.xudu.articlepilot.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/**
* @author xudu
* @description 针对表【user(用户)】的数据库操作Mapper
* @createDate 2026-03-13 21:16:50
* @Entity generator.entity.User
*/
public interface UserMapper extends BaseMapper<User> {
    /**
     * 原子扣减用户配额
     * 使用 quota > 0 条件确保并发安全，避免超扣
     *
     * @param userId 用户ID
     * @return 影响行数，1表示成功，0表示配额不足
     */
    @Update("UPDATE user SET quota = quota - 1 WHERE id = #{userId} AND quota > 0")
    int decrementQuota(@Param("userId") Long userId);
}




