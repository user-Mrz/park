package com.parking.miniapp.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.parking.system.dto.PasswordDTO;
import com.parking.system.dto.RechargeDTO;
import com.parking.system.dto.UpdateUserDTO;
import com.parking.system.dto.WechatLoginDTO;
import com.parking.system.domain.PaymentRecord;
import com.parking.system.domain.RechargeRecord;
import com.parking.system.domain.Reservation;
import com.parking.system.domain.User;
import com.parking.system.domain.VehicleRecord;
import org.springframework.web.multipart.MultipartFile;

public interface UserService {
    User getById(Long id);
    User getByPhone(String phone);
    /** 更新个人资料（昵称/手机号/车牌号/头像），返回更新后的用户信息 */
    User updateInfo(Long userId, UpdateUserDTO dto);
    /** 修改登录密码 */
    void changePassword(Long userId, PasswordDTO dto);
    /** 保存用户头像，返回可访问的相对路径 */
    String saveAvatar(Long userId, MultipartFile file);
    /** 将微信绑定到当前账号（已绑定时幂等） */
    User bindWechat(Long userId, WechatLoginDTO dto);
    /** 解除当前账号的微信绑定 */
    User unbindWechat(Long userId);
    /** 当前用户的校园身份权益与限制（费率、免费时长、预约上限等） */
    java.util.Map<String, Object> benefits(Long userId);
    RechargeRecord recharge(Long userId, RechargeDTO dto);
    Page<RechargeRecord> getRechargeRecords(Long userId, int page, int pageSize);
    Page<PaymentRecord> getPaymentRecords(Long userId, int page, int pageSize);
    Page<Reservation> getUserReservations(Long userId, int page, int pageSize);
    Page<VehicleRecord> getUserVehicleRecords(Long userId, int page, int pageSize);
}
