package ar.gob.cba.instalaciones.rag.dto;

public record ManualIngestaRequest(
        String fuente,
        String temaPrincipal,
        String versionNormativa
) {}
