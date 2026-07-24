package com.hikrobotics.solution.module.detect.model;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.hikrobotics.solution.module.detect.entity.DefectRecord;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 工单 W-D：defect_record_backup 表 PO。
 *
 * <p>1:1 抄自反编译 DefectRecordBackupPO，只调整两点：</p>
 * <ul>
 *   <li>入参构造方法的 record 类型由 PSM 的 {@code DefectRecordPO} 替换为本项目
 *       {@code com.hikrobotics.solution.module.detect.entity.DefectRecord}（PSM→DataupLoad 类名映射）；</li>
 *   <li>BeanUtil 拷贝后显式将 id 置 null，让 MyBatis-Plus 按自增主键插入新行。</li>
 * </ul>
 *
 * <p>PSM 用了自己的 {@code HBaseMapper}，DataupLoad 改为通用
 * {@code com.baomidou.mybatisplus.core.mapper.BaseMapper}（见 DefectRecordBackupDAO）。</p>
 */
@TableName(value = "defect_record_backup")
public class DefectRecordBackupPO implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    private String lineNo;
    private String faceNo;
    private String gloveNo;
    private Integer result;
    private String defectType;
    private String imgList;
    private LocalDateTime time;
    private LocalDateTime updateTime;
    private LocalDateTime createTime;
    private Integer exceptFlag;

    public DefectRecordBackupPO(DefectRecord record) {
        BeanUtil.copyProperties(record, this);
        this.id = null;
    }

    public DefectRecordBackupPO() {
    }

    public Integer getId() {
        return this.id;
    }

    public String getLineNo() {
        return this.lineNo;
    }

    public String getFaceNo() {
        return this.faceNo;
    }

    public String getGloveNo() {
        return this.gloveNo;
    }

    public Integer getResult() {
        return this.result;
    }

    public String getDefectType() {
        return this.defectType;
    }

    public String getImgList() {
        return this.imgList;
    }

    public LocalDateTime getTime() {
        return this.time;
    }

    public LocalDateTime getUpdateTime() {
        return this.updateTime;
    }

    public LocalDateTime getCreateTime() {
        return this.createTime;
    }

    public Integer getExceptFlag() {
        return this.exceptFlag;
    }

    public DefectRecordBackupPO setId(Integer id) {
        this.id = id;
        return this;
    }

    public DefectRecordBackupPO setLineNo(String lineNo) {
        this.lineNo = lineNo;
        return this;
    }

    public DefectRecordBackupPO setFaceNo(String faceNo) {
        this.faceNo = faceNo;
        return this;
    }

    public DefectRecordBackupPO setGloveNo(String gloveNo) {
        this.gloveNo = gloveNo;
        return this;
    }

    public DefectRecordBackupPO setResult(Integer result) {
        this.result = result;
        return this;
    }

    public DefectRecordBackupPO setDefectType(String defectType) {
        this.defectType = defectType;
        return this;
    }

    public DefectRecordBackupPO setImgList(String imgList) {
        this.imgList = imgList;
        return this;
    }

    public DefectRecordBackupPO setTime(LocalDateTime time) {
        this.time = time;
        return this;
    }

    public DefectRecordBackupPO setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
        return this;
    }

    public DefectRecordBackupPO setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    public DefectRecordBackupPO setExceptFlag(Integer exceptFlag) {
        this.exceptFlag = exceptFlag;
        return this;
    }
}
