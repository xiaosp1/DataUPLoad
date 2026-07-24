package com.hikrobotics.solution.module.line.web;

import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.framework.common.query.PageQuery;
import com.hikrobotics.solution.module.line.entity.Line;
import com.hikrobotics.solution.module.line.service.ILineService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 线体管理 Controller（W-B05）
 *
 * <p>URL 命名按工单约定：</p>
 * <ul>
 *   <li>{@code GET /web/line/list} — 列出全部线体（支持分页）</li>
 *   <li>{@code GET /web/line/{lineNo}} — 按 lineNo 取一条</li>
 * </ul>
 *
 * <p>注意：本 controller 与 PSM 反编译产物中
 * {@code com.hikrobotics.solution.module.line.web.LineController}
 * （路径 {@code /web/line}）并存，不冲突（路径不同）。</p>
 */
@RestController
@RequestMapping("/web/line")
public class LineController {

    @Autowired
    private ILineService lineService;

    /**
     * 列全部线体（分页可选，参数 pageNum / pageSize）。
     */
    @GetMapping("/list")
    public BaseResult listAll(PageQuery pageQuery) {
        return this.lineService.listAll(pageQuery);
    }

    /**
     * 按 lineNo 取第一条匹配的线体记录。
     */
    @GetMapping("/{lineNo}")
    public BaseResult getByLineNo(@PathVariable String lineNo) {
        Line line = this.lineService.getByLineNo(lineNo);
        if (line == null) {
            // 未找到：返回 200 + 空列表（避免框架把 msg 当 i18n key 解析导致 NoSuchMessageException）
            return BaseResult.build().data(List.of());
        }
        return BaseResult.build().data(line);
    }
}
