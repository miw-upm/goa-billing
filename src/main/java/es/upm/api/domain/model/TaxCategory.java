package es.upm.api.domain.model;

public enum TaxCategory {
    COMPRAS,                    // material consumible para la actividad
    SUMINISTROS,                // luz, agua, gas (local)
    OTROS_SUMINISTROS,          // internet, teléfono
    ARRENDAMIENTOS,             // alquiler local/oficina
    SERVICIOS_PROFESIONALES,    // gestoría, otros profesionales contratados
    SEGUROS,                    // RC profesional, seguros del despacho
    MUTUALIDAD,                 // cuota mutualidad / Seguridad Social
    INTERESES,                  // intereses de préstamos profesionales
    MANUTENCION,                // dietas y comidas con justificante
    MOBILIARIO,                 // bienes de inversión (amortizables)
    REPARACIONES,               // reparaciones y conservación
    TRIBUTOS,                   // IAE, IBI proporcional, tasas
    FORMACION,                  // cursos, libros, congresos
    PUBLICIDAD,                 // marketing, publicidad
    PERSONAL,                   // NOMINAS CON APORTACIONES DE EMPRESA
    OTROS
}
