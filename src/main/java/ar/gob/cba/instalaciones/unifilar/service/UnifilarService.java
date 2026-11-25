package ar.gob.cba.instalaciones.unifilar.service;

import ar.gob.cba.instalaciones.unifilar.dto.CircuitoCalculadoDto;

public interface UnifilarService {
    CircuitoCalculadoDto.UnifilarResponseDto generarUnifilar(CircuitoCalculadoDto.CalculoRespuestaDto calculo);
}
