package es.upm.api.adapter.in.resources.dtos;

import es.upm.api.domain.model.report.Quarter;
import es.upm.api.domain.model.report.VatSummaryReport;

public record Model303Dto(
        int year,
        Quarter quarter,
        VatSummaryReport vatSummary
) {
}
