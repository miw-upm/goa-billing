package es.upm.api.adapter.in.resources.dtos;

import es.upm.api.adapter.in.resources.Quarter;
import es.upm.api.domain.model.report.VatSummary;

public record Model303Dto(
        int year,
        Quarter quarter,
        VatSummary vatSummary
) {
}
