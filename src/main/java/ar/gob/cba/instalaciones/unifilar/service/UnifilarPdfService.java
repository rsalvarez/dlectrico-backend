package ar.gob.cba.instalaciones.unifilar.service;

import ar.gob.cba.instalaciones.unifilar.dto.CircuitoCalculadoDto;

public interface UnifilarPdfService {
    byte[] generarPdfUnifilarYMateriales(
            CircuitoCalculadoDto.CalculoRespuestaDto calculo,
            String svgUnifilar
    );

}
