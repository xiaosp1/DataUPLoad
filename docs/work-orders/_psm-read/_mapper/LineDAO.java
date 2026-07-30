/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.baomidou.mybatisplus.core.mapper.BaseMapper
 *  com.baomidou.mybatisplus.core.metadata.IPage
 *  com.hikrobotics.solution.module.line.dto.LineDTO
 *  com.hikrobotics.solution.module.line.mapper.LineDAO
 *  com.hikrobotics.solution.module.line.model.LinePO
 */
package com.hikrobotics.solution.module.line.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hikrobotics.solution.module.line.dto.LineDTO;
import com.hikrobotics.solution.module.line.model.LinePO;
import java.util.List;

public interface LineDAO
extends BaseMapper<LinePO> {
    public List<LineDTO> listAll();

    public IPage<LineDTO> listAll(IPage<LineDTO> var1);

    public List<LinePO> selectLine();
}

