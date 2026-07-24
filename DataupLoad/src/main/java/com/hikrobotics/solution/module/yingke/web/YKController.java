package com.hikrobotics.solution.module.yingke.web;

import com.hikrobotics.solution.framework.common.base.BaseResult;
import com.hikrobotics.solution.module.yingke.dto.SearchDefectRecordDTO;
import com.hikrobotics.solution.module.yingke.service.IYKService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value={"/client/yk"})
public class YKController {
    @Autowired
    private IYKService ymService;

    @GetMapping(value={"/line-defect"})
    public BaseResult searchLineAndDefect() {
        return this.ymService.handleLineAndDefectSearch();
    }

    @PostMapping(value={"/defect-record"})
    public BaseResult searchDefectRecord(@Validated @RequestBody SearchDefectRecordDTO form) {
        return this.ymService.searchDefectRecord(form);
    }
}
