package ar.gob.cba.instalaciones.vision.dto;

public record AmbienteExtraido(
    String nombre,
    Double largoM,
    Double anchoM,
    Double superficieInformadaM2,
    TipoSuperficie tipoSuperficie,
    boolean medidaConfiable,
    String observacion,
    int cantidadminimaDeiluminación,
    int cantidadminimaDeTomacorriente
) {
    public enum TipoSuperficie {
        CUBIERTA,
        SEMICUBIERTA,
        NO_DEFINIDA
    }
}
