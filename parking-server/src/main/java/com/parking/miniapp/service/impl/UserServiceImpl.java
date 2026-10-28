package com.parking.miniapp.service.impl;

import cn.hutool.core.lang.Snowflake;
import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.common.BusinessException;
import com.parking.common.IdentityType;
import com.parking.system.dto.PasswordDTO;
import com.parking.system.dto.CampusVerifyResult;
import com.parking.system.dto.RechargeDTO;
import com.parking.system.dto.UpdateUserDTO;
import com.parking.system.dto.WechatLoginDTO;
import com.parking.system.domain.ParkingSpot;
import com.parking.system.domain.PaymentRecord;
import com.parking.system.domain.RechargeRecord;
import com.parking.system.domain.Reservation;
import com.parking.system.domain.User;
import com.parking.system.domain.VehicleRecord;
import com.parking.system.mapper.ParkingSpotMapper;
import com.parking.system.mapper.PaymentRecordMapper;
import com.parking.system.mapper.RechargeRecordMapper;
import com.parking.system.mapper.ReservationMapper;
import com.parking.system.mapper.UserMapper;
import com.parking.system.mapper.VehicleRecordMapper;
import com.parking.system.service.CampusRuleService;
import com.parking.miniapp.service.CampusVerifyService;
import com.parking.system.service.DepartmentService;
import com.parking.miniapp.service.UserService;
import com.parking.system.service.UserRoleService;
import com.parking.common.util.WechatUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    /** 手机号：中国大陆 11 位 */
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    /** 车牌号：省份简称 + 字母 + 5~6 位字母数字（兼容新能源 8 位） */
    private static final Pattern PLATE_PATTERN = Pattern.compile("^[\\u4e00-\\u9fa5][A-Z][A-Z0-9]{5,6}$");
    /** 学号 / 工号：4-20 位字母或数字 */
    private static final Pattern USER_NO_PATTERN = Pattern.compile("^[A-Za-z0-9]{4,20}$");
    private static final List<String> ALLOWED_IMAGE_EXT = List.of(".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp");
    private static final long MAX_AVATAR_SIZE = 2 * 1024 * 1024L;
    private static final String AVATAR_URL_PREFIX = "/uploads/avatar/";

    private final UserMapper userMapper;
    private final RechargeRecordMapper rechargeRecordMapper;
    private final PaymentRecordMapper paymentRecordMapper;
    private final ParkingSpotMapper parkingSpotMapper;
    private final ReservationMapper reservationMapper;
    private final VehicleRecordMapper vehicleRecordMapper;
    private final PasswordEncoder passwordEncoder;
    private final CampusRuleService campusRuleService;
    private final DepartmentService departmentService;
    private final UserRoleService userRoleService;
    private final CampusVerifyService campusVerifyService;

    @Value("${parking.upload-dir:./uploads}")
    private String uploadDir;

    /** 自助充值开关：本地演示为 true；生产未接入真实支付渠道时应设为 false，禁止绕过支付直接加余额 */
    @Value("${parking.payment.mock-enabled:true}")
    private boolean mockPaymentEnabled;

    @Override
    public User getById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) throw new BusinessException("用户不存在");
        return user;
    }

    @Override
    public User getByPhone(String phone) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getPhone, phone));
    }

    @Override
    public User updateInfo(Long userId, UpdateUserDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        if (dto.getNickname() != null) {
            String nickname = dto.getNickname().trim();
            if (nickname.isEmpty()) throw new BusinessException("昵称不能为空");
            if (nickname.length() > 20) throw new BusinessException("昵称长度不能超过 20 个字符");
            user.setNickname(nickname);
        }

        boolean clearPhone = false;
        if (dto.getPhone() != null) {
            String phone = dto.getPhone().trim();
            if (!phone.isEmpty() && !PHONE_PATTERN.matcher(phone).matches()) {
                throw new BusinessException("手机号格式不正确");
            }
            if (!phone.isEmpty() && !phone.equals(user.getPhone())) {
                Long count = userMapper.selectCount(new LambdaQueryWrapper<User>()
                        .eq(User::getPhone, phone)
                        .ne(User::getId, userId));
                if (count != null && count > 0) throw new BusinessException("该手机号已被其他账号使用");
            }
            // 清空手机号需显式置 NULL：updateById 默认忽略 null 字段，且空串会与 uk_phone 唯一索引冲突
            if (phone.isEmpty()) {
                clearPhone = true;
            } else {
                user.setPhone(phone);
            }
        }

        if (dto.getPlateNumber() != null) {
            String plate = dto.getPlateNumber().replace(" ", "").toUpperCase();
            // 允许传空字符串以解除车牌绑定
            if (!plate.isEmpty() && !PLATE_PATTERN.matcher(plate).matches()) {
                throw new BusinessException("车牌号格式不正确，例如：粤B12345");
            }
            user.setPlateNumber(plate);
        }

        if (dto.getAvatar() != null) {
            String avatar = dto.getAvatar().trim();
            if (!avatar.isEmpty() && !avatar.startsWith(AVATAR_URL_PREFIX) && !avatar.startsWith("http")) {
                throw new BusinessException("头像地址不合法");
            }
            user.setAvatar(avatar);
        }

        // ---- 校园身份信息：变更后需管理员重新审核 ----
        // 记录原身份关键字段，用于判断是否需要重新调用校园核验接口
        String originalUserNo = user.getUserNo();
        Integer originalIdentityType = user.getIdentityType();
        Integer originalIdentityStatus = user.getIdentityStatus();
        boolean clearUserNo = false;
        boolean identityChanged = false;
        if (dto.getUserNo() != null) {
            String userNo = dto.getUserNo().trim();
            if (userNo.isEmpty()) {
                clearUserNo = true;
            } else {
                if (!USER_NO_PATTERN.matcher(userNo).matches()) {
                    throw new BusinessException("学号/工号格式不正确（4-20 位字母或数字）");
                }
                Long dup = userMapper.selectCount(new LambdaQueryWrapper<User>()
                        .eq(User::getUserNo, userNo)
                        .ne(User::getId, userId));
                if (dup != null && dup > 0) throw new BusinessException("该学号/工号已被其他账号使用");
                user.setUserNo(userNo);
            }
            identityChanged = true;
        }
        if (dto.getIdentityType() != null) {
            int type = dto.getIdentityType();
            if (type < IdentityType.STUDENT || type > IdentityType.PARENT) {
                throw new BusinessException("身份类型不合法");
            }
            user.setIdentityType(type);
            // 用户端 RBAC：身份变更后按新身份重新映射默认角色
            user.setRoleCode(userRoleService.resolveRoleCodeByIdentity(type));
            identityChanged = true;
        }
        if (dto.getDepartment() != null) {
            String dept = dto.getDepartment().trim();
            if (dept.length() > 64) throw new BusinessException("院系/部门名称过长");

            int type = user.getIdentityType() == null ? 1 : user.getIdentityType();
            if (!dept.isEmpty() && type <= IdentityType.STAFF && !departmentService.isValidName(dept)) {
                throw new BusinessException("请选择有效的学院/部门");
            }
            user.setDepartment(dept);
            identityChanged = true;
        }
        if (dto.getClassName() != null) {
            String className = dto.getClassName().trim();
            if (className.length() > 64) throw new BusinessException("班级名称过长");
            user.setClassName(className);
            identityChanged = true;
        }

        if (identityChanged) {
            int type = user.getIdentityType() == null ? IdentityType.STUDENT : user.getIdentityType();
            if (type == IdentityType.VISITOR || type == IdentityType.PARENT) {
                // 访客/家长不属于校内成员，无需审核
                user.setIdentityStatus(IdentityType.AUDIT_APPROVED);
                user.setIdentityRemark("");
            } else {
                if (!clearUserNo && !StringUtils.hasText(user.getUserNo())) {
                    throw new BusinessException("学生/教师/职工请填写学号/工号");
                }
                if (clearUserNo) {
                    // 学号/工号已被清空，无法自动核验，转人工审核
                    user.setIdentityStatus(IdentityType.AUDIT_PENDING);
                    user.setIdentityRemark("身份信息已更新，等待管理员审核");
                } else if (isVerifiedIdentityUnchanged(originalUserNo, originalIdentityType,
                        originalIdentityStatus, user)) {
                    // 已认证用户仅修改院系/班级等展示信息：沿用已认证状态，省去一次校园接口调用
                    user.setIdentityStatus(IdentityType.AUDIT_APPROVED);
                } else {
                    // 先向校园系统核验：通过则自动认证，查不到或接口不可用时转人工审核
                    CampusVerifyResult verify = campusVerifyService.verify(user.getUserNo(), type);
                    if (verify.matched()) {
                        user.setIdentityStatus(IdentityType.AUDIT_APPROVED);
                        user.setIdentityRemark("校园管理系统自动核验通过");
                        if (StringUtils.hasText(verify.department())) user.setDepartment(verify.department());
                        if (StringUtils.hasText(verify.className())) user.setClassName(verify.className());
                    } else {
                        user.setIdentityStatus(IdentityType.AUDIT_PENDING);
                        user.setIdentityRemark(verify.status() == CampusVerifyResult.Status.NOT_FOUND
                                ? "校园管理系统未查询到该学号/工号，等待管理员审核"
                                : "校园管理系统暂不可用，等待管理员审核");
                    }
                }
            }
        }

        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        if (clearPhone || clearUserNo) {
            LambdaUpdateWrapper<User> wrapper = new LambdaUpdateWrapper<User>().eq(User::getId, userId);
            if (clearPhone) wrapper.set(User::getPhone, null);
            if (clearUserNo) wrapper.set(User::getUserNo, null);
            wrapper.set(User::getUpdatedAt, LocalDateTime.now());
            userMapper.update(null, wrapper);
        }
        return userMapper.selectById(userId);
    }

    /**
     * 已认证用户的学号/工号与身份类型均未发生变化时，无需重新调用校园核验接口。
     * 仅修改院系、班级、昵称、手机号等展示信息不会改变认证结果。
     */
    private boolean isVerifiedIdentityUnchanged(String originalUserNo, Integer originalIdentityType,
                                                Integer originalIdentityStatus, User user) {
        return originalIdentityStatus != null
                && originalIdentityStatus == IdentityType.AUDIT_APPROVED
                && Objects.equals(originalUserNo, user.getUserNo())
                && Objects.equals(originalIdentityType, user.getIdentityType());
    }

    @Override
    public java.util.Map<String, Object> benefits(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        return campusRuleService.benefitsOf(user);
    }

    @Override
    public User bindWechat(Long userId, WechatLoginDTO dto) {
        if (dto == null || (!StringUtils.hasText(dto.getDeviceId()) && !StringUtils.hasText(dto.getCode()))) {
            throw new BusinessException("微信授权失败，请重试");
        }
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        String openid = WechatUtil.resolveOpenid(dto.getCode(), dto.getDeviceId());
        // 已绑定同一个微信：幂等返回
        if (openid.equals(user.getOpenid())) {
            return user;
        }
        User bound = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));
        if (bound != null && !bound.getId().equals(userId)) {
            throw new BusinessException("该微信已绑定其他账号，请先在原账号中解除绑定");
        }
        user.setOpenid(openid);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return userMapper.selectById(userId);
    }

    @Override
    public User unbindWechat(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        if (!StringUtils.hasText(user.getOpenid())) {
            throw new BusinessException("当前账号未绑定微信");
        }
        // 避免解绑后账号无法登录
        if (!StringUtils.hasText(user.getPhone()) || !StringUtils.hasText(user.getPassword())) {
            throw new BusinessException("请先设置手机号和登录密码，否则解绑后将无法登录");
        }
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, userId)
                .set(User::getOpenid, null)
                .set(User::getUpdatedAt, LocalDateTime.now()));
        return userMapper.selectById(userId);
    }

    @Override
    public void changePassword(Long userId, PasswordDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        if (dto == null || dto.getOldPassword() == null || dto.getOldPassword().isEmpty()) {
            throw new BusinessException("请输入原密码");
        }
        String newPassword = dto.getNewPassword() == null ? "" : dto.getNewPassword().trim();
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            throw new BusinessException("新密码长度需为 6-20 位");
        }
        if (newPassword.equals(dto.getOldPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        if (user.getPassword() == null || !passwordEncoder.matches(dto.getOldPassword(), user.getPassword())) {
            throw new BusinessException("原密码不正确");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
    }

    @Override
    public String saveAvatar(Long userId, MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("请选择要上传的图片");
        if (file.getSize() > MAX_AVATAR_SIZE) throw new BusinessException("头像大小不能超过 2MB");

        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        int dot = original.lastIndexOf('.');
        String ext = dot > -1 ? original.substring(dot).toLowerCase() : ".jpg";
        if (!ALLOWED_IMAGE_EXT.contains(ext)) {
            throw new BusinessException("仅支持 jpg / jpeg / png / gif / webp / bmp 格式的图片");
        }

        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");

        try {
            Path dir = Paths.get(uploadDir, "avatar").toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String filename = userId + "_" + System.currentTimeMillis() + ext;
            file.transferTo(dir.resolve(filename).toFile());

            // 清理旧头像文件
            String old = user.getAvatar();
            if (old != null && old.startsWith(AVATAR_URL_PREFIX)) {
                try {
                    Files.deleteIfExists(dir.resolve(old.substring(AVATAR_URL_PREFIX.length())));
                } catch (IOException ignore) {
                    // 旧文件删除失败不影响本次上传
                }
            }

            String url = AVATAR_URL_PREFIX + filename;
            user.setAvatar(url);
            user.setUpdatedAt(LocalDateTime.now());
            userMapper.updateById(user);
            return url;
        } catch (IOException e) {
            throw new BusinessException("头像上传失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional
    public RechargeRecord recharge(Long userId, RechargeDTO dto) {
        // 生产未接入真实支付渠道时禁止自助充值，避免调用方直接指定金额凭空入账
        if (!mockPaymentEnabled) {
            throw new BusinessException("在线支付渠道未启用，暂不支持自助充值，请联系停车管理处");
        }
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("充值金额必须大于 0");
        }
        if (dto.getAmount().stripTrailingZeros().scale() > 2) {
            throw new BusinessException("充值金额最多保留两位小数");
        }
        User user = userMapper.selectById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        // 幂等保护：3 秒内同用户同金额的重复提交视为重复点击，直接拒绝，避免重复入账
        Long duplicated = rechargeRecordMapper.selectCount(new LambdaQueryWrapper<RechargeRecord>()
                .eq(RechargeRecord::getUserId, userId)
                .eq(RechargeRecord::getAmount, dto.getAmount())
                .eq(RechargeRecord::getStatus, 1)
                .ge(RechargeRecord::getCreatedAt, LocalDateTime.now().minusSeconds(3)));
        if (duplicated != null && duplicated > 0) {
            throw new BusinessException("充值请求提交过于频繁，请稍后查看余额确认是否已到账");
        }
        Snowflake sf = IdUtil.getSnowflake(1, 2);
        RechargeRecord record = new RechargeRecord();
        record.setId(sf.nextId());
        record.setUserId(userId);
        // 订单号使用雪花 ID, 保证并发下全局唯一 (时间戳+毫秒在并发时会撞 uk_order_no 唯一索引)
        record.setOrderNo("RECH" + sf.nextId());
        record.setAmount(dto.getAmount());
        record.setPayMethod(dto.getPayMethod() != null ? dto.getPayMethod() : "wechat");
        record.setStatus(1);
        rechargeRecordMapper.insert(record);
        userMapper.addBalance(userId, dto.getAmount());
        return record;
    }

    @Override
    public Page<RechargeRecord> getRechargeRecords(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<RechargeRecord> wrapper = new LambdaQueryWrapper<RechargeRecord>()
                .eq(RechargeRecord::getUserId, userId)
                .orderByDesc(RechargeRecord::getCreatedAt);
        return rechargeRecordMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public Page<PaymentRecord> getPaymentRecords(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<PaymentRecord> wrapper = new LambdaQueryWrapper<PaymentRecord>()
                .eq(PaymentRecord::getUserId, userId)
                .orderByDesc(PaymentRecord::getCreatedAt);
        return paymentRecordMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }

    @Override
    public Page<Reservation> getUserReservations(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<Reservation> wrapper = new LambdaQueryWrapper<Reservation>()
                .eq(Reservation::getUserId, userId)
                .orderByDesc(Reservation::getCreatedAt);
        Page<Reservation> result = reservationMapper.selectPage(new Page<>(page, pageSize), wrapper);
        // 补齐展示用的车位编号与区域（用户端「我的预约」列表直接展示）
        List<Reservation> records = result.getRecords();
        if (records != null && !records.isEmpty()) {
            java.util.Set<Long> spotIds = new java.util.HashSet<>();
            for (Reservation r : records) {
                if (r.getSpotId() != null) spotIds.add(r.getSpotId());
            }
            Map<Long, ParkingSpot> spotMap = new java.util.HashMap<>();
            if (!spotIds.isEmpty()) {
                for (ParkingSpot s : parkingSpotMapper.selectBatchIds(spotIds)) {
                    spotMap.put(s.getId(), s);
                }
            }
            for (Reservation r : records) {
                ParkingSpot s = spotMap.get(r.getSpotId());
                if (s != null) {
                    r.setSpotCode(s.getSpotCode());
                    r.setSpotArea(s.getArea());
                }
            }
        }
        return result;
    }

    @Override
    public Page<VehicleRecord> getUserVehicleRecords(Long userId, int page, int pageSize) {
        LambdaQueryWrapper<VehicleRecord> wrapper = new LambdaQueryWrapper<VehicleRecord>()
                .eq(VehicleRecord::getUserId, userId)
                .orderByDesc(VehicleRecord::getCreatedAt);
        return vehicleRecordMapper.selectPage(new Page<>(page, pageSize), wrapper);
    }
}
