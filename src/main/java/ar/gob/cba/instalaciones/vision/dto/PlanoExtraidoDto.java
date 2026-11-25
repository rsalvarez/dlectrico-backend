package ar.gob.cba.instalaciones.vision.dto;

import java.util.List;

public record PlanoExtraidoDto(
    String tituloPlano,
    List<AmbienteExtraido> ambientes,
    List<String> cargasOEtiquetasDetectadas,
    List<String> advertencias
) {}
