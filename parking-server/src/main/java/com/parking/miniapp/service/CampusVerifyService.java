package com.parking.miniapp.service;

import com.parking.system.dto.CampusVerifyResult;

/**
 * 第三方校园管理系统身份核验服务（我方主动出站调用）。
 * <p>学生/教师/职工提交学号/工号后，先向校园系统查询；核验通过则免人工审核，
 * 查询不到或接口不可用时由调用方按待审核处理，保留原有人工审核兜底。</p>
 */
public interface CampusVerifyService {

    /**
     * 按学号/工号与身份类型向校园系统核验。
     *
     * @param userNo       学号/工号
     * @param identityType 1学生 2教师 3职工
     * @return 核验结果，永不为 null
     */
    CampusVerifyResult verify(String userNo, int identityType);
}
