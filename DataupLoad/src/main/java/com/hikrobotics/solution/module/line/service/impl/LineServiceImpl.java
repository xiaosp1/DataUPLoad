package com.hikrobotics.solution.module.line.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.common.query.PageQuery;
import com.hikrobotics.solution.module.line.entity.Line;
import com.hikrobotics.solution.module.line.mapper.LineMapper;
import com.hikrobotics.solution.module.line.service.ILineService;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 线体服务实现（W-B03 + W-B05 子集）。
 *
 * <p>本工单只实现 listAll / getByLineNo / getByLineNoAndFaceNo；其它 PSM 反编译产物中的方法
 * （add/modify/delete/bindPlan/...）依赖 defect/plan/alarm 模块，待 W-B06/W-B07/W-B08 上线后再补齐。</p>
 */
@Service
public class LineServiceImpl extends ServiceImpl<LineMapper, Line> implements ILineService {

    @Override
    public BaseResult listAll(PageQuery pageQuery) {
        if (pageQuery != null && pageQuery.isPaged()) {
            return BaseResult.build().data(this.baseMapper.selectPage(pageQuery.getPage(), null));
        }
        return BaseResult.build().data(this.baseMapper.selectList(Wrappers.lambdaQuery(Line.class).orderByAsc(Line::getId)));
    }

    @Override
    public List<Line> listByLineNo(String lineNo) {
        if (lineNo == null || lineNo.isEmpty()) {
            return List.of();
        }
        LambdaQueryWrapper<Line> wrapper = Wrappers.<Line>lambdaQuery()
            .eq(Line::getLineNo, lineNo)
            .orderByAsc(Line::getId);
        return this.list(wrapper);
    }

    @Override
    public Line getByLineNo(String lineNo) {
        List<Line> lines = listByLineNo(lineNo);
        return lines.isEmpty() ? null : lines.get(0);
    }

    @Override
    public Line getByLineNoAndFaceNo(String lineNo, String faceNo) {
        if (lineNo == null || lineNo.isEmpty() || faceNo == null || faceNo.isEmpty()) {
            return null;
        }
        LambdaQueryWrapper<Line> wrapper = Wrappers.<Line>lambdaQuery()
            .eq(Line::getLineNo, lineNo)
            .eq(Line::getFaceNo, faceNo)
            .orderByAsc(Line::getId)
            .last("limit 1");
        return this.getOne(wrapper);
    }

    @Override
    public List<Line> listLine() {
        return this.list(Wrappers.<Line>lambdaQuery().orderByAsc(Line::getId));
    }
}

