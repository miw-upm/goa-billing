package es.upm.api.adapter.in.resources.dtos;

import es.upm.api.adapter.in.resources.Quarter;
import es.upm.api.domain.model.report.NetIncomeBreakdownReport;

public record Model130Dto(
        int year,
        Quarter quarter,
        NetIncomeBreakdownReport netIncomeBreakdown
) {
}
