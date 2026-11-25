package ar.gob.cba.instalaciones.unifilar.svg;

import java.util.Locale;

public final class SimbolosAeaSvg {

    private SimbolosAeaSvg() {}

    /**
     * Medidor de energía activa (KWh) intercalado en línea vertical.
     * Entrada superior: (cx, y) | Salida inferior: (cx, y + 48)
     */
    public static String medidorKwhVertical(int cx, int y, String etiquetaDerecha) {
        String label = (etiquetaDerecha != null && !etiquetaDerecha.isBlank())
                ? """
                  <text x="%d" y="%d" font-size="10" font-weight="bold" fill="#0f172a">%s</text>
                  """.formatted(cx + 22, y + 28, escapeXml(etiquetaDerecha))
                : "";

        return """
                <g class="simbolo-kwh-v">
                  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke="#0f172a" stroke-width="1.8"/>
                  <rect x="%d" y="%d" width="32" height="36" fill="#ffffff" stroke="#0f172a" stroke-width="1.8"/>
                  <text x="%d" y="%d" text-anchor="middle" font-size="10" font-weight="bold" fill="#0f172a">KWh</text>
                  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke="#0f172a" stroke-width="1.8"/>
                  %s
                </g>
                """.formatted(
                cx, y, cx, y + 6,
                cx - 16, y + 6,
                cx, y + 28,
                cx, y + 42, cx, y + 48,
                label
        );
    }

    /**
     * Interruptor Diferencial (ID) vertical estilo Figura 12 / 13.
     * Entrada superior: (cx, y) | Salida inferior: (cx, y + 64)
     */
    public static String diferencialVertical(int cx, int y, String tituloDerecha, String subtituloDerecha) {
        String sub = (subtituloDerecha != null && !subtituloDerecha.isBlank())
                ? """
                  <text x="%d" y="%d" font-size="10" fill="#0f172a">%s</text>
                  """.formatted(cx + 18, y + 36, escapeXml(subtituloDerecha))
                : "";

        String tit = (tituloDerecha != null && !tituloDerecha.isBlank())
                ? """
                  <text x="%d" y="%d" font-size="10.5" font-weight="bold" fill="#0f172a">%s</text>
                  """.formatted(cx + 18, y + 23, escapeXml(tituloDerecha))
                : "";

        return """
                <g class="simbolo-id-v" fill="none" stroke="#0f172a" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke-width="1.8"/>
                  <circle cx="%d" cy="%d" r="2.5" fill="#0f172a"/>
                  <!-- Contacto móvil -->
                  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke-width="1.8"/>
                  <!-- Cuadro de accionamiento a la derecha -->
                  <rect x="%d" y="%d" width="7" height="7" fill="#ffffff" stroke-width="1.4"/>
                  <!-- Aro / toroide diferencial en la salida -->
                  <circle cx="%d" cy="%d" r="5" fill="#ffffff" stroke-width="1.6"/>
                  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke-width="1.8"/>
                  %s
                  %s
                </g>
                """.formatted(
                cx, y, cx, y + 14,
                cx, y + 14,
                cx, y + 14, cx - 8, y + 42,
                cx + 4, y + 22,
                cx, y + 47,
                cx, y + 52, cx, y + 64,
                tit,
                sub
        );
    }

    /**
     * Interruptor Termomagnético (ITM) vertical con disparo térmico y flecha magnética (Figura 12 / 13).
     * Entrada superior: (cx, y) | Salida inferior: (cx, y + 68)
     */
    public static String termomagneticoVertical(int cx, int y, String calibreLabel, String iccLabel) {
        String icc = (iccLabel != null && !iccLabel.isBlank())
                ? """
                  <text x="%d" y="%d" text-anchor="end" font-size="9.5" fill="#0f172a">%s</text>
                  """.formatted(cx - 26, y + 44, escapeXml(iccLabel))
                : "";

        String cal = (calibreLabel != null && !calibreLabel.isBlank())
                ? """
                  <text x="%d" y="%d" text-anchor="end" font-size="10" font-weight="bold" fill="#0f172a">%s</text>
                  """.formatted(cx - 26, y + 32, escapeXml(calibreLabel))
                : "";

        return """
                <g class="simbolo-itm-v" fill="none" stroke="#0f172a" stroke-linecap="round" stroke-linejoin="round">
                  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke-width="1.8"/>
                  <circle cx="%d" cy="%d" r="2.5" fill="#0f172a"/>
                  <!-- Contacto móvil -->
                  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke-width="1.8"/>
                  <circle cx="%d" cy="%d" r="2.5" fill="#ffffff" stroke-width="1.5"/>
                  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke-width="1.8"/>

                  <!-- Disparo térmico (almena a la derecha) -->
                  <polyline points="%d,%d %d,%d %d,%d %d,%d %d,%d" stroke-width="1.5"/>

                  <!-- Disparo magnético (flecha hacia abajo-derecha) -->
                  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke-width="1.5"/>
                  <polygon points="%d,%d %d,%d %d,%d" fill="#0f172a" stroke="none"/>
                  %s
                  %s
                </g>
                """.formatted(
                cx, y, cx, y + 14,
                cx, y + 14,
                cx, y + 14, cx - 8, y + 42,
                cx, y + 45,
                cx, y + 48, cx, y + 68,
                cx - 4, y + 24, cx + 6, y + 21, cx + 8, y + 27, cx + 16, y + 24, cx + 18, y + 29,
                cx - 2, y + 33, cx + 14, y + 37,
                cx + 15, y + 37, cx + 9, y + 34, cx + 8, y + 39,
                cal,
                icc
        );
    }

    /**
     * Trazos oblicuos sobre línea vertical para indicar cantidad de conductores (ej. 2 o 3).
     */
    public static String trazosConductoresVertical(int cx, int cy, int cantidad, String etiquetaRotadaIzq) {
        StringBuilder sb = new StringBuilder("<g class=\"trazos-v\" stroke=\"#0f172a\" stroke-width=\"1.6\">");
        int inicio = -((cantidad - 1) * 3);
        for (int i = 0; i < cantidad; i++) {
            int y = cy + inicio + i * 6;
            sb.append("""
                    <line x1="%d" y1="%d" x2="%d" y2="%d"/>
                    """.formatted(cx - 6, y + 3, cx + 6, y - 3));
        }
        if (etiquetaRotadaIzq != null && !etiquetaRotadaIzq.isBlank()) {
            sb.append("""
                    <text x="%d" y="%d" font-size="10" fill="#0f172a" stroke="none" transform="rotate(-90 %d %d)">%s</text>
                    """.formatted(cx - 10, cy + 28, cx - 10, cy + 28, escapeXml(etiquetaRotadaIzq)));
        }
        sb.append("</g>");
        return sb.toString();
    }

    /**
     * Trazos oblicuos sobre línea horizontal para indicar conductores (ej. barra superior de acometida).
     */
    public static String trazosConductoresHorizontal(int cx, int cy, int cantidad) {
        StringBuilder sb = new StringBuilder("<g class=\"trazos-h\" stroke=\"#0f172a\" stroke-width=\"1.6\">");
        int inicio = -((cantidad - 1) * 3);
        for (int i = 0; i < cantidad; i++) {
            int x = cx + inicio + i * 6;
            sb.append("""
                    <line x1="%d" y1="%d" x2="%d" y2="%d"/>
                    """.formatted(x - 3, cy + 6, x + 3, cy - 6));
        }
        sb.append("</g>");
        return sb.toString();
    }

    /**
     * Terminal de circuito con flecha hacia abajo y código (TUG, IUG, ACU, etc.).
     * Entrada superior: (cx, y) | Punta de flecha en: (cx, y + 28)
     */
    public static String salidaCircuitoVertical(int cx, int y, String codigoCircuito) {
        return """
                <g class="salida-circuito" stroke="#0f172a" fill="none">
                  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke-width="1.8"/>
                  <polygon points="%d,%d %d,%d %d,%d" fill="#0f172a" stroke="none"/>
                  <text x="%d" y="%d" text-anchor="middle" font-size="10.5" font-weight="bold" fill="#0f172a" stroke="none">%s</text>
                </g>
                """.formatted(
                cx, y, cx, y + 26,
                cx, y + 28, cx - 4, y + 20, cx + 4, y + 20,
                cx, y + 42, escapeXml(codigoCircuito)
        );
    }

    /**
     * Símbolo de puesta a tierra (línea vertical y 3 trazos horizontales decrecientes).
     * Conexión superior: (cx, y)
     */
    public static String puestaATierraVertical(int cx, int y) {
        int baseY = y + 20;
        return """
                <g class="pat-v" stroke="#0f172a" stroke-linecap="round">
                  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke-width="1.8"/>
                  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke-width="2.2"/>
                  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke-width="1.8"/>
                  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke-width="1.5"/>
                </g>
                """.formatted(
                cx, y, cx, baseY,
                cx - 13, baseY, cx + 13, baseY,
                cx - 8, baseY + 5, cx + 8, baseY + 5,
                cx - 4, baseY + 10, cx + 4, baseY + 10
        );
    }

    /**
     * Contorno punteado de tablero (como el recuadro discontinuo de la Figura 13).
     */
    public static String recuadroTablero(int x, int y, int width, int height) {
        return """
                <rect x="%d" y="%d" width="%d" height="%d" fill="none" stroke="#0f172a" stroke-width="1.5" stroke-dasharray="8 5"/>
                """.formatted(x, y, width, height);
    }

    public static String formatSeccion(double mm2) {
        if (mm2 == Math.rint(mm2)) {
            return String.valueOf((long) mm2).replace('.', ',');
        }
        return String.format(Locale.US, "%.1f", mm2).replace('.', ',');
    }

    public static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }
}