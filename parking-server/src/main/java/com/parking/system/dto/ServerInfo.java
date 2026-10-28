package com.parking.system.dto;

import lombok.Data;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.CentralProcessor.TickType;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.FileSystem;
import oshi.software.os.OSFileStore;
import oshi.software.os.OperatingSystem;
import oshi.util.Util;

/**
 * 服务器硬件监控信息（参考若依 RuoYi Server 设计）
 * 采集 CPU、内存、JVM、系统、磁盘五部分数据
 */
@Data
public class ServerInfo {

    private static final int OSHI_WAIT_SECOND = 1000;

    /** CPU 相关信息 */
    private Cpu cpu = new Cpu();

    /** 内存相关信息 */
    private Mem mem = new Mem();

    /** JVM 相关信息 */
    private Jvm jvm = new Jvm();

    /** 服务器相关信息 */
    private Sys sys = new Sys();

    /** 磁盘相关信息 */
    private List<SysFile> sysFiles = new LinkedList<>();

    /** 采集全部硬件信息 */
    public ServerInfo copyTo() throws Exception {
        SystemInfo si = new SystemInfo();
        HardwareAbstractionLayer hal = si.getHardware();

        setCpuInfo(hal.getProcessor());
        setMemInfo(hal.getMemory());
        setSysInfo();
        setJvmInfo();
        setSysFiles(si.getOperatingSystem());
        return this;
    }

    /** 设置 CPU 信息（两次采样间隔 1s 计算 CPU 占用） */
    private void setCpuInfo(CentralProcessor processor) {
        long[] prevTicks = processor.getSystemCpuLoadTicks();
        Util.sleep(OSHI_WAIT_SECOND);
        long[] ticks = processor.getSystemCpuLoadTicks();
        long nice = ticks[TickType.NICE.getIndex()] - prevTicks[TickType.NICE.getIndex()];
        long irq = ticks[TickType.IRQ.getIndex()] - prevTicks[TickType.IRQ.getIndex()];
        long softirq = ticks[TickType.SOFTIRQ.getIndex()] - prevTicks[TickType.SOFTIRQ.getIndex()];
        long steal = ticks[TickType.STEAL.getIndex()] - prevTicks[TickType.STEAL.getIndex()];
        long cSys = ticks[TickType.SYSTEM.getIndex()] - prevTicks[TickType.SYSTEM.getIndex()];
        long user = ticks[TickType.USER.getIndex()] - prevTicks[TickType.USER.getIndex()];
        long iowait = ticks[TickType.IOWAIT.getIndex()] - prevTicks[TickType.IOWAIT.getIndex()];
        long idle = ticks[TickType.IDLE.getIndex()] - prevTicks[TickType.IDLE.getIndex()];
        long totalCpu = user + nice + cSys + idle + iowait + irq + softirq + steal;
        cpu.setCpuNum(processor.getLogicalProcessorCount());
        cpu.setTotal(totalCpu);
        cpu.setSys(cSys);
        cpu.setUsed(user);
        cpu.setWait(iowait);
        cpu.setFree(idle);
    }

    /** 设置内存信息 */
    private void setMemInfo(GlobalMemory memory) {
        mem.setTotal(memory.getTotal());
        mem.setUsed(memory.getTotal() - memory.getAvailable());
        mem.setFree(memory.getAvailable());
    }

    /** 设置服务器信息 */
    private void setSysInfo() throws java.net.UnknownHostException {
        Properties props = System.getProperties();
        InetAddress addr = InetAddress.getLocalHost();
        sys.setComputerName(addr.getHostName());
        sys.setComputerIp(addr.getHostAddress());
        sys.setOsName(props.getProperty("os.name"));
        sys.setOsArch(props.getProperty("os.arch"));
        sys.setUserDir(props.getProperty("user.dir"));
    }

    /** 设置 JVM 信息 */
    private void setJvmInfo() {
        Properties props = System.getProperties();
        jvm.setTotal(Runtime.getRuntime().totalMemory());
        jvm.setMax(Runtime.getRuntime().maxMemory());
        jvm.setFree(Runtime.getRuntime().freeMemory());
        jvm.setVersion(props.getProperty("java.version"));
        jvm.setHome(props.getProperty("java.home"));
        jvm.setStartTime(System.currentTimeMillis() - java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime());
        jvm.setRunTime(java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime());
    }

    /** 设置磁盘信息 */
    private void setSysFiles(OperatingSystem os) {
        FileSystem fileSystem = os.getFileSystem();
        List<OSFileStore> fsArray = fileSystem.getFileStores();
        for (OSFileStore fs : fsArray) {
            long free = fs.getUsableSpace();
            long total = fs.getTotalSpace();
            if (total <= 0) {
                continue;
            }
            long used = total - free;
            SysFile sysFile = new SysFile();
            sysFile.setDirName(fs.getMount());
            sysFile.setSysTypeName(fs.getType());
            sysFile.setTypeName(fs.getName());
            sysFile.setTotal(convertFileSize(total));
            sysFile.setFree(convertFileSize(free));
            sysFile.setUsed(convertFileSize(used));
            sysFile.setUsage(Math.round(used * 10000.0 / total) / 100.0);
            sysFiles.add(sysFile);
        }
    }

    /** 字节大小转换 */
    public String convertFileSize(long size) {
        long kb = 1024;
        long mb = kb * 1024;
        long gb = mb * 1024;
        if (size >= gb) {
            return String.format("%.1f GB", (float) size / gb);
        } else if (size >= mb) {
            float f = (float) size / mb;
            return String.format(f > 100 ? "%.0f MB" : "%.1f MB", f);
        } else if (size >= kb) {
            float f = (float) size / kb;
            return String.format(f > 100 ? "%.0f KB" : "%.1f KB", f);
        } else {
            return String.format("%d B", size);
        }
    }

    @Data
    public static class Cpu {
        /** 核心数 */
        private int cpuNum;
        /** CPU 总占比 */
        private long total;
        /** CPU 系统占比 */
        private long sys;
        /** CPU 用户占比 */
        private long used;
        /** CPU 当前等待率 */
        private long wait;
        /** CPU 当前空闲率 */
        private long free;
    }

    @Data
    public static class Mem {
        /** 内存总量 */
        private long total;
        /** 内存已用 */
        private long used;
        /** 内存剩余 */
        private long free;
    }

    @Data
    public static class Jvm {
        /** JVM 总内存 */
        private long total;
        /** JVM 最大内存 */
        private long max;
        /** JVM 空闲内存 */
        private long free;
        /** JDK 版本 */
        private String version;
        /** JDK 路径 */
        private String home;
        /** JVM 启动时间（毫秒时间戳） */
        private long startTime;
        /** JVM 运行时长（毫秒） */
        private long runTime;
    }

    @Data
    public static class Sys {
        /** 服务器名称 */
        private String computerName;
        /** 服务器 Ip */
        private String computerIp;
        /** 项目路径 */
        private String userDir;
        /** 操作系统 */
        private String osName;
        /** 系统架构 */
        private String osArch;
    }

    @Data
    public static class SysFile {
        /** 盘符路径 */
        private String dirName;
        /** 盘符类型 */
        private String sysTypeName;
        /** 文件类型 */
        private String typeName;
        /** 总大小 */
        private String total;
        /** 剩余大小 */
        private String free;
        /** 已经使用量 */
        private String used;
        /** 资源的使用率 */
        private double usage;
    }

    /** 供前端展示的 JVM 已用内存 */
    public long getJvmUsed() {
        return jvm.getTotal() - jvm.getFree();
    }

    public List<SysFile> getSysFileList() {
        return new ArrayList<>(sysFiles);
    }
}
