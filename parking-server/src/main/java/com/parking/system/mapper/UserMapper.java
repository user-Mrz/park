package com.parking.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.parking.system.domain.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Update("UPDATE t_user SET balance = balance + #{amount} WHERE id = #{userId}")
    int addBalance(@Param("userId") Long userId, @Param("amount") java.math.BigDecimal amount);

    @Update("UPDATE t_user SET balance = balance - #{amount} WHERE id = #{userId} AND balance >= #{amount}")
    int deductBalance(@Param("userId") Long userId, @Param("amount") java.math.BigDecimal amount);

    @org.apache.ibatis.annotations.Select("SELECT COALESCE(SUM(balance), 0) FROM t_user WHERE status = 1")
    java.math.BigDecimal sumActiveUserBalance();
}
