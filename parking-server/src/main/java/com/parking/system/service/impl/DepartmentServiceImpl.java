package com.parking.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.parking.common.BusinessException;
import com.parking.system.domain.Department;
import com.parking.system.domain.User;
import com.parking.system.mapper.DepartmentMapper;
import com.parking.system.mapper.UserMapper;
import com.parking.system.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl implements DepartmentService {

    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;

    @Override
    public List<Department> list(boolean enabledOnly) {
        LambdaQueryWrapper<Department> wrapper = new LambdaQueryWrapper<Department>()
                .orderByAsc(Department::getSortOrder)
                .orderByAsc(Department::getId);
        if (enabledOnly) wrapper.eq(Department::getStatus, 1);
        return departmentMapper.selectList(wrapper);
    }

    @Override
    public Department create(Department department) {
        String name = department.getName() == null ? "" : department.getName().trim();
        if (!StringUtils.hasText(name)) throw new BusinessException("请填写学院/部门名称");
        if (name.length() > 64) throw new BusinessException("学院名称不能超过 64 个字符");
        Long dup = departmentMapper.selectCount(new LambdaQueryWrapper<Department>().eq(Department::getName, name));
        if (dup != null && dup > 0) throw new BusinessException("该学院/部门已存在");

        Department entity = new Department();
        entity.setName(name);
        entity.setCode(trim(department.getCode()));
        entity.setContact(trim(department.getContact()));
        entity.setPhone(trim(department.getPhone()));
        entity.setRemark(trim(department.getRemark()));
        entity.setStatus(department.getStatus() == null ? 1 : department.getStatus());
        entity.setSortOrder(department.getSortOrder() == null ? 99 : department.getSortOrder());
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        departmentMapper.insert(entity);
        return entity;
    }

    @Override
    public Department update(Long id, Department department) {
        Department entity = departmentMapper.selectById(id);
        if (entity == null) throw new BusinessException("学院/部门不存在");
        if (department.getName() != null) {
            String name = department.getName().trim();
            if (!StringUtils.hasText(name)) throw new BusinessException("请填写学院/部门名称");
            if (!name.equals(entity.getName())) {
                Long dup = departmentMapper.selectCount(new LambdaQueryWrapper<Department>()
                        .eq(Department::getName, name).ne(Department::getId, id));
                if (dup != null && dup > 0) throw new BusinessException("该学院/部门已存在");
                // 同步更新该学院下用户的院系名称，避免历史数据变成"野字符串"
                List<User> users = userMapper.selectList(new LambdaQueryWrapper<User>()
                        .eq(User::getDepartment, entity.getName()));
                for (User u : users) {
                    u.setDepartment(name);
                    userMapper.updateById(u);
                }
                entity.setName(name);
            }
        }
        if (department.getCode() != null) entity.setCode(trim(department.getCode()));
        if (department.getContact() != null) entity.setContact(trim(department.getContact()));
        if (department.getPhone() != null) entity.setPhone(trim(department.getPhone()));
        if (department.getRemark() != null) entity.setRemark(trim(department.getRemark()));
        if (department.getStatus() != null) entity.setStatus(department.getStatus());
        if (department.getSortOrder() != null) entity.setSortOrder(department.getSortOrder());
        entity.setUpdatedAt(LocalDateTime.now());
        departmentMapper.updateById(entity);
        return entity;
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        Department entity = departmentMapper.selectById(id);
        if (entity == null) throw new BusinessException("学院/部门不存在");
        entity.setStatus(status == null ? 1 : status);
        entity.setUpdatedAt(LocalDateTime.now());
        departmentMapper.updateById(entity);
    }

    @Override
    public void remove(Long id) {
        Department entity = departmentMapper.selectById(id);
        if (entity == null) throw new BusinessException("学院/部门不存在");
        Long users = userMapper.selectCount(new LambdaQueryWrapper<User>().eq(User::getDepartment, entity.getName()));
        if (users != null && users > 0) {
            throw new BusinessException("该学院/部门下还有 " + users + " 名用户，不能删除；可先停用");
        }
        departmentMapper.deleteById(id);
    }

    @Override
    public boolean isValidName(String name) {
        if (!StringUtils.hasText(name)) return false;
        Long count = departmentMapper.selectCount(new LambdaQueryWrapper<Department>()
                .eq(Department::getName, name.trim())
                .eq(Department::getStatus, 1));
        return count != null && count > 0;
    }

    private String trim(String s) {
        return s == null ? "" : s.trim();
    }
}
