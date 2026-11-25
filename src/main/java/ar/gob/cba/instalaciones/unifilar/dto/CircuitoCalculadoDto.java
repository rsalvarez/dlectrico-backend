package ar.gob.cba.instalaciones.unifilar.dto;

import java.util.List;

public record CircuitoCalculadoDto(
        String codigo,
        String tipo,
        int cantidadBocas,
        double potenciaAsignadaVa,
        double factorSimultaneidad,
        double potenciaSimultaneaVa,
        double corrienteProyectoA,
        double seccionConductorMm2,
        int calibreTermomagneticaA,
        String descripcion
) {

    public record ConductorCalculadoDto(
            String tramo,
            double seccionMm2,
            double corrienteCalculoA,
            String materialAislacion,
            String criterio
    ) {
    }

    public record TableroCalculadoDto(
            String tablero,
            int interruptorTermomagneticoPrincipalA,
            int polosTermomagnetica,
            String curvaTermomagnetica,
            int disyuntorDiferencialNominalA,
            int sensibilidadDiferencialMa,
            List<String> observaciones
    ) {
    }

    public record CalculoRespuestaDto(
            String proyecto,
            double superficieCubiertaM2,
            double superficieSemicubiertaComputableM2,
            double slaM2,
            String gradoPreliminar,
            String gradoFinal,
            List<CircuitoCalculadoDto> circuitos,
            double potenciaTotalSinSimultaneidadVa,
            double dpmsVa,
            double dpmsKw,
            double corrienteProyectoTotalA,
            List<ConductorCalculadoDto> conductores,
            TableroCalculadoDto tableroGeneral,
            TableroCalculadoDto tableroSeccional,
            List<String> criteriosAplicados,
            List<String> advertencias
    ) {
    }

    public record UnifilarResponseDto(
            String proyecto,
            String gradoFinal,
            double slaM2,
            double dpmsVa,
            double dpmsKw,
            double corrienteProyectoTotalA,
            String formato,
            String svg,
            List<String> notasNormativas
    ) {
    }
}