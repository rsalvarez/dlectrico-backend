package ar.gob.cba.instalaciones.calculo.enums;

import java.util.Locale;

public enum TipoAcometida {
    CLASE_II_BAJADA,
    AEREA_CONVENCIONAL,
    SUBTERRANEA;

    public static TipoAcometida fromStringOrDefault(String valor, TipoAcometida porDefecto) {
        if (valor == null || valor.isBlank()) {
            return porDefecto;
        }
        String normalizado = valor.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
        if (normalizado.contains("CLASE_II") || normalizado.contains("DOBLE_AISLACION") || normalizado.contains("BAJADA")) {
            return CLASE_II_BAJADA;
        }
        if (normalizado.contains("SUBTERRANEA")) {
            return SUBTERRANEA;
        }
        if (normalizado.contains("AEREA")) {
            return AEREA_CONVENCIONAL;
        }
        try {
            return TipoAcometida.valueOf(normalizado);
        } catch (IllegalArgumentException ex) {
            return porDefecto;
        }
    }
}