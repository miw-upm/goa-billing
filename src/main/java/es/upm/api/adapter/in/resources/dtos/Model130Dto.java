package es.upm.api.adapter.in.resources.dtos;

import es.upm.api.adapter.in.resources.Quarter;
import es.upm.api.domain.model.report.NetIncomeBreakdown;

public record Model130Dto(
        int year,
        Quarter quarter,
        NetIncomeBreakdown netIncomeBreakdown
) {
}
