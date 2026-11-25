package ar.gob.cba.instalaciones.unifilar.service.impl;

import ar.gob.cba.instalaciones.rag.service.NormativaConsultaService;
import ar.gob.cba.instalaciones.unifilar.dto.CircuitoCalculadoDto;
import ar.gob.cba.instalaciones.unifilar.service.UnifilarService;
import ar.gob.cba.instalaciones.unifilar.svg.SimbolosAeaSvg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static ar.gob.cba.instalaciones.unifilar.svg.SimbolosAeaSvg.escapeXml;
import static ar.gob.cba.instalaciones.unifilar.svg.SimbolosAeaSvg.formatSeccion;

@Service
@Slf4j
public class UnifilarServiceImpl implements UnifilarService {

    private final NormativaConsultaService normativaConsultaService;

    public UnifilarServiceImpl(NormativaConsultaService normativaConsultaService) {
        this.normativaConsultaService = normativaConsultaService;
    }

    @Override
    public CircuitoCalculadoDto.UnifilarResponseDto generarUnifilar(CircuitoCalculadoDto.CalculoRespuestaDto calculo) {
        Objects.requireNonNull(calculo, "Los datos de cálculo no pueden ser nulos");

        List<String> notasNormativas = construirNotasNormativas(calculo);
        String svg = construirDiagramaUnifilarSvg(calculo, notasNormativas);

        return new CircuitoCalculadoDto.UnifilarResponseDto(
                calculo.proyecto(),
                calculo.gradoFinal(),
                calculo.slaM2(),
                calculo.dpmsVa(),
                calculo.dpmsKw(),
                calculo.corrienteProyectoTotalA(),
                "SVG",
                svg,
                notasNormativas
        );
    }

    private List<String> construirNotasNormativas(CircuitoCalculadoDto.CalculoRespuestaDto calculo) {
        List<String> notas = new ArrayList<>();
        if (calculo.criteriosAplicados() != null) {
            notas.addAll(calculo.criteriosAplicados());
        }
        if (calculo.advertencias() != null) {
            for (String adv : calculo.advertencias()) {
                notas.add("ADVERTENCIA: " + adv);
            }
        }
        try {
            String ctx = normativaConsultaService.buscarContexto(
                    "esquema unifilar tablero general seccional interruptor diferencial termomagnetico puesta a tierra AEA 770, que ningun simbolo se superponga con otro, para poder verlos a todos",
                    null,
                    2
            );
            if (ctx != null && !ctx.isBlank()) {
                notas.add("Verificación normativa AEA 770 / ERSeP 2021 contrastada contra base documental.");
            }
        } catch (Exception e) {
            log.debug("RAG no disponible al generar notas del unifilar: {}", e.getMessage());
        }
        return notas;
    }

    private boolean esTableroGeneralClaseII(CircuitoCalculadoDto.TableroCalculadoDto tg, List<String> notas) {
        if (tg != null && tg.observaciones() != null) {
            for (String obs : tg.observaciones()) {
                if (obs != null && (obs.toUpperCase(Locale.ROOT).contains("CLASE II")
                        || obs.toUpperCase(Locale.ROOT).contains("DOBLE AISLACION"))) {
                    return true;
                }
            }
        }
        if (notas != null) {
            for (String nota : notas) {
                if (nota != null && (nota.toUpperCase(Locale.ROOT).contains("CLASE_II")
                        || nota.toUpperCase(Locale.ROOT).contains("CLASE II"))) {
                    return true;
                }
            }
        }
        return tg != null ;
    }

    private String construirDiagramaUnifilarSvg(
            CircuitoCalculadoDto.CalculoRespuestaDto c,
            List<String> notas
    ) {
        List<CircuitoCalculadoDto> circuitos = c.circuitos() != null ? c.circuitos() : List.of();
        int cols = Math.max(circuitos.size(), 1);
        int colSpacing = 220;
        int startX = 360;
        int width = Math.max(1460, startX + (cols - 1) * colSpacing + 280);

        var lp = buscarConductor(c.conductores(), "LP");
        var ls = buscarConductor(c.conductores(), "LS");
        var pe = buscarConductor(c.conductores(), "PE");
        var tg = c.tableroGeneral();
        var ts = c.tableroSeccional();

        String lpSec = lp != null ? formatSeccion(lp.seccionMm2()) : "4";
        String lsSec = ls != null ? formatSeccion(ls.seccionMm2()) : "4";
        String peSec = pe != null ? formatSeccion(pe.seccionMm2()) : "4";

        boolean tgClaseII = esTableroGeneralClaseII(tg, notas);
        boolean tieneIdTg = !tgClaseII && tg != null && tg.disyuntorDiferencialNominalA() > 0;

        int tgPolos = tg != null && tg.polosTermomagnetica() > 0 ? tg.polosTermomagnetica() : 2;
        int tgItmA = tg != null && tg.interruptorTermomagneticoPrincipalA() > 0 ? tg.interruptorTermomagneticoPrincipalA() : 32;
        int tgIdA = tg != null  ? tg.disyuntorDiferencialNominalA() : 25;
        int tgSensMa = tg != null  ? tg.sensibilidadDiferencialMa() : 30;

        int tsPolos = ts != null && ts.polosTermomagnetica() > 0 ? ts.polosTermomagnetica() : tgPolos;
        int tsItmA = ts != null && ts.interruptorTermomagneticoPrincipalA() > 0 ? ts.interruptorTermomagneticoPrincipalA() : tgItmA;
        boolean tieneIdTs = ts != null && ts.disyuntorDiferencialNominalA() > 0;
        int tsIdA = tieneIdTs ? ts.disyuntorDiferencialNominalA() : 25;
        int tsSensMa = ts != null  ? ts.sensibilidadDiferencialMa() : 30;

        int yBarraSup = 120;
        int xAcometida = 150;
        int xFinBarra = width - 80;

        // Posiciones verticales:
        // Medidor KWh aguas arriba -> ITM principal dentro del Tablero General -> (opcional ID TG) -> Barra TG
        int yMedidor = yBarraSup + 28;        // 148
        int yItmTg = yMedidor + 48 + 36;      // 232 (después del medidor y tramo LP)
        int yCursorTg = yItmTg + 68;         // 300
        if (tieneIdTg) {
            yCursorTg += 64;                 // 364 si lleva ID en TG
        }
        int yBarraTg = yCursorTg + 12;       // 312 (o 376)

        // Recuadro del Tablero General envuelve su ITM principal (y (opcional) su ID) hasta la barra TG
        int tgBoxX = 55;
        int tgBoxY = yItmTg - 22;            // 210
        int tgBoxW = 250;
        int tgBoxH = (yBarraTg - tgBoxY) + 22;

        // Tablero Seccional a la derecha:
        // Si lleva ID de cabecera en el TS, va primero (yCabeceraTs) y debajo la barra de circuitos terminales
        int yEntradaTs = yBarraTg + 52;
        int xTs = startX + 80;
        int yCabeceraTs = yEntradaTs + 16;
        int yBarraCirc = tieneIdTs ? (yCabeceraTs + 64 + 28) : (yCabeceraTs + 28);

        int firstCx = startX + 80;
        int lastCx = startX + (cols - 1) * colSpacing + 80;
        int tsBoxX = xTs - 50;
        int tsBoxY = yEntradaTs - 12;
        int tsBoxW = Math.max(260, (lastCx - firstCx) + 170);
        int tsBoxH = (yBarraCirc - tsBoxY) + 18;

        int yTramoBase = yBarraCirc + 84;
        int rotuloY = Math.max(tgBoxY + tgBoxH + 80, yTramoBase + 165);
        int rotuloH = 225;
        int height = rotuloY + rotuloH + 40;

        StringBuilder sb = new StringBuilder();

        sb.append("""
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 %d %d" width="%d" height="%d" style="font-family: Arial, Helvetica, sans-serif; background:#ffffff;">
                  <defs>
                    <pattern id="cad-grid" width="24" height="24" patternUnits="userSpaceOnUse">
                      <path d="M 24 0 L 0 0 0 24" fill="none" stroke="#f1f5f9" stroke-width="1"/>
                    </pattern>
                  </defs>

                  <!-- Fondo y marco -->
                  <rect width="100%%" height="100%%" fill="url(#cad-grid)"/>
                  <rect x="16" y="16" width="%d" height="%d" rx="4" fill="none" stroke="#0f172a" stroke-width="2"/>

                  <!-- Encabezado -->
                  <rect x="28" y="28" width="%d" height="52" rx="4" fill="#0f172a"/>
                  <text x="44" y="50" font-size="15" font-weight="bold" fill="#ffffff">ESQUEMA UNIFILAR — AEA 770 / ERSeP 2021</text>
                  <text x="44" y="69" font-size="11" fill="#cbd5e1">Proyecto: %s</text>
                """.formatted(
                width, height, width, height,
                width - 32, height - 32,
                width - 56,
                escapeXml(valorSeguro(c.proyecto(), "Instalación Eléctrica"))
        ));

        // 1. Barra horizontal superior de acometida / red
        sb.append("""
                  <line x1="60" y1="%d" x2="%d" y2="%d" stroke="#0f172a" stroke-width="2"/>
                """.formatted(yBarraSup, xFinBarra, yBarraSup));
        sb.append(SimbolosAeaSvg.trazosConductoresHorizontal(100, yBarraSup, 2));
        sb.append(SimbolosAeaSvg.trazosConductoresHorizontal(xAcometida + 60, yBarraSup, 2));

        // 2. Bajada principal hasta Medidor KWh y entrada al Tablero General
        sb.append("""
                  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke="#0f172a" stroke-width="1.8"/>
                """.formatted(xAcometida, yBarraSup, xAcometida, yMedidor));
        sb.append(SimbolosAeaSvg.medidorKwhVertical(xAcometida, yMedidor, "220V~ 50Hz"));

        int yDespuesMedidor = yMedidor + 48;
        sb.append("""
                  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke="#0f172a" stroke-width="1.8"/>
                """.formatted(xAcometida, yDespuesMedidor, xAcometida, yItmTg));
        sb.append(SimbolosAeaSvg.trazosConductoresVertical(
                xAcometida, yDespuesMedidor + 18, 2,
                "LP " + lpSec + "mm²"
        ));

        // Recuadro punteado Tablero General (envolviendo su ITM principal)
        sb.append(SimbolosAeaSvg.recuadroTablero(tgBoxX, tgBoxY, tgBoxW, tgBoxH));
        sb.append("""
                  <text x="%d" y="%d" font-size="10" font-weight="bold" fill="#0f172a">TABLERO GENERAL</text>
                """.formatted(tgBoxX + 12, tgBoxY + 14));
        if (tgClaseII) {
            sb.append("""
                      <text x="%d" y="%d" font-size="8.5" fill="#475569">Clase II (doble aislación — sin PAT en masa)</text>
                    """.formatted(tgBoxX + 12, tgBoxY + 26));
        }

        // ITM principal dentro del Tablero General
        sb.append(SimbolosAeaSvg.termomagneticoVertical(
                xAcometida, yItmTg,
                "C" + tgPolos + "x" + tgItmA + "A",
                "ITM Gral."
        ));

        int yActualTg = yItmTg + 68;
        if (tieneIdTg) {
            sb.append(SimbolosAeaSvg.diferencialVertical(
                    xAcometida, yActualTg,
                    "ID" + tgPolos + "x" + tgIdA + "A",
                    tgSensMa + "mA"
            ));
            yActualTg += 64;
        }

        // Barra horizontal interna del TG
        sb.append("""
                  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke="#0f172a" stroke-width="1.8"/>
                  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke="#0f172a" stroke-width="2.5"/>
                """.formatted(
                xAcometida, yActualTg, xAcometida, yBarraTg,
                tgBoxX + 20, yBarraTg, tgBoxX + tgBoxW - 20, yBarraTg
        ));

        // Si NO es Clase II, se muestra PAT vinculada al TG; si es Clase II, se omite PAT en el gabinete del TG
        if (!tgClaseII) {
            int xPat = tgBoxX + 45;
            sb.append("""
                      <line x1="%d" y1="%d" x2="%d" y2="%d" stroke="#0f172a" stroke-width="1.8"/>
                    """.formatted(xPat, yBarraTg, xPat, tgBoxY + tgBoxH));
            sb.append(SimbolosAeaSvg.puestaATierraVertical(xPat, tgBoxY + tgBoxH));
        }

        // 3. Línea Seccional (LS) desde barra TG hacia Tablero Seccional (TS)
        String etiquetaLs = tgClaseII
                ? "LS: 2x" + lsSec + "mm² + PE " + peSec + "mm²"
                : "LS: 2x" + lsSec + "mm2+T (" + peSec + "mm²)";

        sb.append("""
                  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke="#0f172a" stroke-width="1.8"/>
                  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke="#0f172a" stroke-width="1.8"/>
                """.formatted(xAcometida, yBarraTg, xTs, yBarraTg, xTs, yBarraTg, xTs, yEntradaTs));
        sb.append(SimbolosAeaSvg.trazosConductoresHorizontal((xAcometida + xTs) / 2, yBarraTg, 2));
        sb.append("""
                  <text x="%d" y="%d" text-anchor="middle" font-size="10" font-weight="bold" fill="#0f172a">%s</text>
                """.formatted((xAcometida + xTs) / 2, yBarraTg - 10, escapeXml(etiquetaLs)));

        // Recuadro punteado Tablero Seccional (TS)
        sb.append(SimbolosAeaSvg.recuadroTablero(tsBoxX, tsBoxY, tsBoxW, tsBoxH));
        sb.append("""
                  <text x="%d" y="%d" font-size="10" font-weight="bold" fill="#0f172a">TABLERO SECCIONAL</text>
                """.formatted(tsBoxX + 12, tsBoxY + 14));

        // En el TS: cabecera diferencial (ID 30 mA) antes de distribuir a los circuitos terminales
        if (tieneIdTs) {
            sb.append(SimbolosAeaSvg.diferencialVertical(
                    xTs, yCabeceraTs,
                    "ID" + tsPolos + "x" + tsIdA + "A",
                    tsSensMa + "mA (Cabecera TS)"
            ));
            sb.append("""
                      <line x1="%d" y1="%d" x2="%d" y2="%d" stroke="#0f172a" stroke-width="1.8"/>
                    """.formatted(xTs, yCabeceraTs + 64, xTs, yBarraCirc));
        } else {
            sb.append("""
                      <line x1="%d" y1="%d" x2="%d" y2="%d" stroke="#0f172a" stroke-width="1.8"/>
                    """.formatted(xTs, yEntradaTs, xTs, yBarraCirc));
        }

        // Barra de distribución de circuitos en el TS
        sb.append("""
                  <line x1="%d" y1="%d" x2="%d" y2="%d" stroke="#0f172a" stroke-width="2.5"/>
                """.formatted(firstCx, yBarraCirc, lastCx, yBarraCirc));

        // 4. Circuitos terminales con su termomagnética individual
        for (int i = 0; i < circuitos.size(); i++) {
            CircuitoCalculadoDto circ = circuitos.get(i);
            int cx = startX + i * colSpacing + 80;
            String secCirc = formatSeccion(circ.seccionConductorMm2());
            String cod = valorSeguro(circ.codigo(), "C" + (i + 1));
            String tipo = valorSeguro(circ.tipo(), "");
            String etiquetaCirc = tipo.isBlank() ? cod : cod + " (" + tipo + ")";

            sb.append("""
                      <line x1="%d" y1="%d" x2="%d" y2="%d" stroke="#0f172a" stroke-width="1.8"/>
                    """.formatted(cx, yBarraCirc, cx, yBarraCirc + 16));

            sb.append(SimbolosAeaSvg.termomagneticoVertical(
                    cx, yBarraCirc + 16,
                    "C" + tsPolos + "x" + circ.calibreTermomagneticaA() + "A",
                    String.format(Locale.US, "%.2fA", circ.corrienteProyectoA())
            ));

            int yTramo = yBarraCirc + 84;
            sb.append("""
                      <line x1="%d" y1="%d" x2="%d" y2="%d" stroke="#0f172a" stroke-width="1.8"/>
                    """.formatted(cx, yTramo, cx, yTramo + 62));

            sb.append(SimbolosAeaSvg.trazosConductoresVertical(
                    cx, yTramo + 22, 3,
                    "2x" + secCirc + "mm2+T"
            ));

            sb.append(SimbolosAeaSvg.salidaCircuitoVertical(cx, yTramo + 62, etiquetaCirc));

            sb.append("""
                      <text x="%d" y="%d" text-anchor="middle" font-size="9.5" fill="#334155">%.0f VA sim. (%d bocas)</text>
                    """.formatted(cx, yTramo + 118, circ.potenciaSimultaneaVa(), circ.cantidadBocas()));
        }

        // 5. Rótulo y observaciones inferiores
        int rotuloW = 390;
        int notasW = width - 56 - rotuloW - 14;
        int xRotulo = 28 + notasW + 14;

        sb.append("""
                  <rect x="28" y="%d" width="%d" height="%d" rx="4" fill="#f8fafc" stroke="#334155" stroke-width="1.5"/>
                  <text x="44" y="%d" font-size="11.5" font-weight="bold" fill="#0f172a">CRITERIOS Y OBSERVACIONES</text>

                  <rect x="%d" y="%d" width="%d" height="%d" rx="4" fill="#ffffff" stroke="#0f172a" stroke-width="1.6"/>
                  <rect x="%d" y="%d" width="%d" height="28" rx="4" fill="#0f172a"/>
                  <text x="%d" y="%d" font-size="11.5" font-weight="bold" fill="#ffffff">RÓTULO DE CÁLCULO</text>

                  <text x="%d" y="%d" font-size="10.5" fill="#0f172a">• SLA: %.2f m² | Grado: %s</text>
                  <text x="%d" y="%d" font-size="10.5" fill="#0f172a">• DPMS: %.0f VA (%.2f kW)</text>
                  <text x="%d" y="%d" font-size="10.5" fill="#0f172a">• Corriente de Proyecto Total: %.2f A</text>
                  <text x="%d" y="%d" font-size="10.5" fill="#0f172a">• PE / PAT: %s mm²%s</text>
                  <text x="%d" y="%d" font-size="10" font-weight="bold" fill="#475569">Normativa: AEA 770 / ERSeP 2021</text>
                """.formatted(
                rotuloY, notasW, rotuloH,
                rotuloY + 22,
                xRotulo, rotuloY, rotuloW, rotuloH,
                xRotulo, rotuloY, rotuloW,
                xRotulo + 12, rotuloY + 18,
                xRotulo + 12, rotuloY + 52, c.slaM2(), escapeXml(valorSeguro(c.gradoFinal(), "-")),
                xRotulo + 12, rotuloY + 76, c.dpmsVa(), c.dpmsKw(),
                xRotulo + 12, rotuloY + 100, c.corrienteProyectoTotalA(),
                xRotulo + 12, rotuloY + 124, peSec, tgClaseII ? " (TG Clase II)" : "",
                xRotulo + 12, rotuloY + 156
        ));

        int yNota = rotuloY + 44;
        for (int i = 0; i < Math.min(notas.size(), 8); i++) {
            sb.append("""
                      <text x="44" y="%d" font-size="10" fill="#334155">• %s</text>
                    """.formatted(yNota, escapeXml(recortar(notas.get(i), 105))));
            yNota += 22;
        }

        sb.append("</svg>");
        return sb.toString();
    }

    private CircuitoCalculadoDto.ConductorCalculadoDto buscarConductor(
            List<CircuitoCalculadoDto.ConductorCalculadoDto> conductores,
            String clave
    ) {
        if (conductores == null) return null;
        String upper = clave.toUpperCase(Locale.ROOT);
        for (var c : conductores) {
            if (c.tramo() != null && c.tramo().toUpperCase(Locale.ROOT).contains(upper)) {
                return c;
            }
        }
        return null;
    }

    private String valorSeguro(String val, String fallback) {
        return (val == null || val.isBlank()) ? fallback : val;
    }

    private String recortar(String texto, int maxLen) {
        if (texto == null) return "";
        return texto.length() <= maxLen ? texto : texto.substring(0, maxLen - 3) + "...";
    }
}