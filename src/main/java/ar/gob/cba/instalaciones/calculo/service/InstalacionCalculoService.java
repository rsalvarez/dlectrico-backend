package ar.gob.cba.instalaciones.calculo.service;

import ar.gob.cba.instalaciones.calculo.dto.SolicitudCalculoDto;

public interface InstalacionCalculoService {
    SolicitudCalculoDto.ResultadoCalculoDto calcular(SolicitudCalculoDto solicitud);
}
