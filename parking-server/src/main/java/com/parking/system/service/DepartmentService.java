package com.parking.system.service;

import com.parking.system.domain.Department;

import java.util.List;

/**
 * 学院 / 部门管理
 */
public interface DepartmentService {

    /** 列表（enabledOnly=true 时只返回启用的，供注册/资料页下拉使用） */
    List<Department> list(boolean enabledOnly);

    Department create(Department department);

    Department update(Long id, Department department);

    void updateStatus(Long id, Integer status);

    void remove(Long id);

    /** 名称是否为有效（存在且启用）的学院 */
    boolean isValidName(String name);
}
