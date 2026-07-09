package es.upm.api.adapter.in.resources.dtos;

import es.upm.api.domain.model.report.NetIncomeBreakdownReport;
import es.upm.api.domain.model.report.Quarter;

public record Model130Dto(
        int year,
        Quarter quarter,
        NetIncomeBreakdownReport netIncomeBreakdown
) {
}
