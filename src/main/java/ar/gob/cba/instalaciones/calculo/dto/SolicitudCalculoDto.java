package ar.gob.cba.instalaciones.calculo.dto;

import ar.gob.cba.instalaciones.calculo.enums.GradoElectrificacion;
import ar.gob.cba.instalaciones.calculo.enums.TipoAcometida;
import ar.gob.cba.instalaciones.calculo.enums.TipoCircuito;
import ar.gob.cba.instalaciones.vision.dto.AmbienteExtraido;

import java.util.List;
import java.util.Objects;

public record SolicitudCalculoDto(
        String proyecto,
        int tensionNominalV,
        int fases, // 1 = monofásica, 3 = trifásica
        TipoAcometida tipoAcometida,
        List<AmbienteExtraido> ambientes,
        List<String> cargasEspecialesDetectadas
) {
    public int cantidadBocasIluminacion() {
        return (ambientes == null)
                ? 0
                : ambientes.stream()
                .filter(Objects::nonNull)
                .mapToInt(AmbienteExtraido::cantidadminimaDeiluminación)
                .sum();
    }

    public int cantidadBocasITomaCorrientes() {
        return (ambientes == null)
                ? 0
                : ambientes.stream()
                .filter(Objects::nonNull)
                .mapToInt(AmbienteExtraido::cantidadminimaDeTomacorriente)
                .sum();
    }

    public SolicitudCalculoDto {
        if (tensionNominalV <= 0) tensionNominalV = 220;
        if (fases != 1 && fases != 3) fases = 1;
        tipoAcometida = tipoAcometida == null ? TipoAcometida.CLASE_II_BAJADA : tipoAcometida;
        ambientes = ambientes == null ? List.of() : List.copyOf(ambientes);
        cargasEspecialesDetectadas = cargasEspecialesDetectadas == null
                ? List.of()
                : List.copyOf(cargasEspecialesDetectadas);
    }

    public record CircuitoCalculado(
            String codigo,
            TipoCircuito tipo,
            int cantidadBocas,
            double potenciaAsignadaVa,
            double factorSimultaneidad,
            double potenciaSimultaneaVa,
            double corrienteProyectoA,
            double seccionConductorMm2,
            int calibreTermomagneticaA,
            String descripcion
    ) {
    }

    public record ConductorTramo(
            String tramo, // Ej: "Línea Principal (LP)", "Línea Seccional (LS)", "Circuito IUG-1", "PE / PAT"
            double seccionMm2,
            double corrienteCalculoA,
            String materialAislacion,
            String criterio
    ) {
    }

    public record ProteccionTablero(
            String tablero, // "TABLERO_GENERAL" o "TABLERO_SECCIONAL"
            int interruptorTermomagneticoPrincipalA,
            int polosTermomagnetica,
            String curvaTermomagnetica,
            Integer disyuntorDiferencialNominalA,
            Integer sensibilidadDiferencialMa,
            List<String> observaciones
    ) {
    }

    public record ResultadoCalculoDto(
            String proyecto,
            double superficieCubiertaM2,
            double superficieSemicubiertaComputableM2,
            double slaM2,
            GradoElectrificacion gradoPreliminar,
            GradoElectrificacion gradoFinal,
            List<CircuitoCalculado> circuitos,
            double potenciaTotalSinSimultaneidadVa,
            double dpmsVa,
            double dpmsKw,
            double corrienteProyectoTotalA,
            List<ConductorTramo> conductores,
            ProteccionTablero tableroGeneral,
            ProteccionTablero tableroSeccional,
            List<String> criteriosAplicados,
            List<String> advertencias
    ) {
    }
}