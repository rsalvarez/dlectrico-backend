package ar.gob.cba.instalaciones.unifilar.service.impl;

import ar.gob.cba.instalaciones.unifilar.dto.CircuitoCalculadoDto;
import ar.gob.cba.instalaciones.unifilar.service.UnifilarPdfService;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static ar.gob.cba.instalaciones.unifilar.svg.SimbolosAeaSvg.escapeXml;
import static ar.gob.cba.instalaciones.unifilar.svg.SimbolosAeaSvg.formatSeccion;

@Service
public class UnifilarPdfServiceImpl implements UnifilarPdfService {

    public record MaterialItem(
            String rubro,
            String descripcion,
            String especificacion,
            String unidad,
            String cantidad
    ) {
    }

    @Override
    public byte[] generarPdfUnifilarYMateriales(
            CircuitoCalculadoDto.CalculoRespuestaDto calculo,
            String svgUnifilar
    ) {
        Objects.requireNonNull(calculo, "Los datos de cálculo no pueden ser nulos");
        Objects.requireNonNull(svgUnifilar, "El SVG del unifilar no puede ser nulo");

        String svgNormalizado = asegurarNamespaceSvg(svgUnifilar);
        String pngBase64 = svgToPngBase64(svgNormalizado, 1800f);

        List<MaterialItem> materiales = construirListadoDesdeCalculo(calculo);
        String filasHtml = construirFilasTabla(materiales);

        String html = """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8"/>
                  <style>
                    @page {
                      size: A3 landscape;
                      margin: 10mm;
                    }
                    body {
                      font-family: Arial, Helvetica, sans-serif;
                      color: #0f172a;
                      font-size: 9.5pt;
                      margin: 0;
                      padding: 0;
                    }
                    h1 {
                      font-size: 13.5pt;
                      margin: 0 0 3px 0;
                    }
                    .subtitulo {
                      font-size: 8.8pt;
                      color: #475569;
                      margin-bottom: 8px;
                    }
                    .hoja-unifilar {
                      width: 100%%;
                    }
                    .plano-box {
                      border: 1px solid #cbd5e1;
                      padding: 4px;
                      background: #ffffff;
                      text-align: center;
                    }
                    .plano-box img {
                      width: 100%%;
                      max-height: 245mm;
                      height: auto;
                      display: block;
                      margin: 0 auto;
                    }
                    .salto-hoja {
                      display: block;
                      page-break-before: always;
                      break-before: page;
                      height: 0;
                      margin: 0;
                      padding: 0;
                    }
                    .hoja-materiales {
                      width: 100%%;
                    }
                    table {
                      width: 100%%;
                      border-collapse: collapse;
                      margin-top: 8px;
                    }
                    th, td {
                      border: 1px solid #334155;
                      padding: 6px 8px;
                      text-align: left;
                      font-size: 9.2pt;
                    }
                    th {
                      background: #0f172a;
                      color: #ffffff;
                    }
                    tr {
                      page-break-inside: avoid;
                      break-inside: avoid;
                    }
                    tr:nth-child(even) td {
                      background: #f8fafc;
                    }
                    .pie-nota {
                      margin-top: 10px;
                      font-size: 8.5pt;
                      color: #475569;
                    }
                  </style>
                </head>
                <body>
                  <!-- HOJA 1: ESQUEMA UNIFILAR -->
                  <div class="hoja-unifilar">
                    <h1>Hoja 1 de 2 — Esquema Unifilar: %s</h1>
                    <div class="subtitulo">
                      Grado: %s | SLA: %.2f m² | DPMS: %.0f VA (%.2f kW) | Ip total: %.2f A | AEA 770 / ERSeP 2021
                    </div>
                    <div class="plano-box">
                      <img src="data:image/png;base64,%s" alt="Esquema Unifilar"/>
                    </div>
                  </div>

                  <!-- SALTO A HOJA 2 DENTRO DEL MISMO PDF -->
                  <div class="salto-hoja"></div>

                  <!-- HOJA 2: LISTADO DE MATERIALES CALCULADOS -->
                  <div class="hoja-materiales">
                    <h1>Hoja 2 de 2 — Listado de Materiales según Cálculo Eléctrico</h1>
                    <div class="subtitulo">
                      Proyecto: %s | Alimentación: Monofásica 220V ~ 50Hz
                    </div>
                    <table>
                      <thead>
                        <tr>
                          <th style="width: 16%%;">Rubro</th>
                          <th style="width: 36%%;">Descripción</th>
                          <th style="width: 26%%;">Especificación calculada</th>
                          <th style="width: 10%%;">Unidad</th>
                          <th style="width: 12%%;">Cantidad</th>
                        </tr>
                      </thead>
                      <tbody>
                        %s
                      </tbody>
                    </table>
                    <div class="pie-nota">
                      * Cantidades de protecciones y tableros derivadas del cálculo; longitudes de cañerías y conductores sujetas al relevamiento definitivo de obra y normativa AEA 770 / ERSeP 2021.
                    </div>
                  </div>
                </body>
                </html>
                """.formatted(
                escapeXml(valorSeguro(calculo.proyecto(), "Instalación Eléctrica")),
                escapeXml(valorSeguro(calculo.gradoFinal(), "-")),
                calculo.slaM2(),
                calculo.dpmsVa(),
                calculo.dpmsKw(),
                calculo.corrienteProyectoTotalA(),
                pngBase64,
                escapeXml(valorSeguro(calculo.proyecto(), "Instalación Eléctrica")),
                filasHtml
        );

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error al generar el PDF del unifilar y listado de materiales", e);
        }
    }

    private String asegurarNamespaceSvg(String svg) {
        String trimmed = svg.trim();
        if (trimmed.startsWith("<svg") && !trimmed.contains("xmlns=")) {
            return trimmed.replaceFirst("<svg", "<svg xmlns=\"http://www.w3.org/2000/svg\"");
        }
        return trimmed;
    }

    private String svgToPngBase64(String svgXml, float widthPx) {
        try {
            PNGTranscoder transcoder = new PNGTranscoder();
            transcoder.addTranscodingHint(PNGTranscoder.KEY_WIDTH, widthPx);

            TranscoderInput input = new TranscoderInput(
                    new ByteArrayInputStream(svgXml.getBytes(StandardCharsets.UTF_8))
            );
            ByteArrayOutputStream pngOut = new ByteArrayOutputStream();
            TranscoderOutput output = new TranscoderOutput(pngOut);

            transcoder.transcode(input, output);
            return Base64.getEncoder().encodeToString(pngOut.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Error al rasterizar el SVG del unifilar con Batik", e);
        }
    }

    private String estimarCantidadConductor(
            CircuitoCalculadoDto.CalculoRespuestaDto c,
            CircuitoCalculadoDto.ConductorCalculadoDto cond
    ) {
        String tramo = cond.tramo() != null ? cond.tramo().toUpperCase(Locale.ROOT) : "";
        double sla = Math.max(c.slaM2(), 20.0);

        if (tramo.contains("LP") || tramo.contains("LS")) {
            double metros = Math.ceil(Math.sqrt(sla) * 1.5);
            return String.format(Locale.US, "%.0f m (est.)", metros);
        }
        if (tramo.contains("PE")) {
            double metros = Math.ceil(Math.sqrt(sla) * 1.2 + 3.0);
            return String.format(Locale.US, "%.0f m (est.)", metros);
        }
        return "Según obra";
    }

    private boolean esTgClaseII(CircuitoCalculadoDto.TableroCalculadoDto tg) {
        if (tg == null) return false;
        if (tg.disyuntorDiferencialNominalA() == 0) return true;
        if (tg.observaciones() != null) {
            for (String obs : tg.observaciones()) {
                if (obs != null && (obs.toUpperCase(Locale.ROOT).contains("CLASE II")
                        || obs.toUpperCase(Locale.ROOT).contains("DOBLE AISLACION"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<MaterialItem> construirListadoDesdeCalculo(CircuitoCalculadoDto.CalculoRespuestaDto c) {
        List<MaterialItem> items = new ArrayList<>();

        var tg = c.tableroGeneral();
        var ts = c.tableroSeccional();
        List<CircuitoCalculadoDto> circuitos = c.circuitos() != null ? c.circuitos() : List.of();
        List<CircuitoCalculadoDto.ConductorCalculadoDto> conductores =
                c.conductores() != null ? c.conductores() : List.of();

        if (tg != null) {
            boolean claseII = esTgClaseII(tg);
            items.add(new MaterialItem(
                    "Tablero General",
                    "Gabinete para Tablero General (TG)",
                    claseII
                            ? "Aislación Clase II (doble aislación), sin conexión de masa a PE"
                            : "Con riel DIN y barra de distribución",
                    "u",
                    "1"
            ));
            items.add(new MaterialItem(
                    "Protección TG",
                    "Interruptor termomagnético principal (ITM)",
                    "%dx%d A (%s)".formatted(
                            tg.polosTermomagnetica(),
                            tg.interruptorTermomagneticoPrincipalA(),
                            valorSeguro(tg.curvaTermomagnetica(), "Curva C")
                    ),
                    "u",
                    "1"
            ));
            if (!claseII && tg.disyuntorDiferencialNominalA() != 0 && tg.disyuntorDiferencialNominalA() > 0) {
                int sens = tg.sensibilidadDiferencialMa() != 0 ? tg.sensibilidadDiferencialMa() : 30;
                items.add(new MaterialItem(
                        "Protección TG",
                        "Interruptor diferencial (ID)",
                        "%dx%d A / %d mA".formatted(
                                tg.polosTermomagnetica(),
                                tg.disyuntorDiferencialNominalA(),
                                sens
                        ),
                        "u",
                        "1"
                ));
            }
        }

        if (ts != null) {
            items.add(new MaterialItem(
                    "Tablero Seccional",
                    "Gabinete para Tablero Seccional (TS)",
                    "Con riel DIN para %d circuitos terminales".formatted(circuitos.size()),
                    "u",
                    "1"
            ));
            if (ts.interruptorTermomagneticoPrincipalA() > 0) {
                items.add(new MaterialItem(
                        "Protección TS",
                        "Interruptor termomagnético seccional (ITM)",
                        "%dx%d A (%s)".formatted(
                                ts.polosTermomagnetica(),
                                ts.interruptorTermomagneticoPrincipalA(),
                                valorSeguro(ts.curvaTermomagnetica(), "Curva C")
                        ),
                        "u",
                        "1"
                ));
            }
            if ( ts.disyuntorDiferencialNominalA() > 0) {
                int sens = ts.sensibilidadDiferencialMa() != 0 ? ts.sensibilidadDiferencialMa() : 30;
                items.add(new MaterialItem(
                        "Protección TS",
                        "Interruptor diferencial seccional (ID)",
                        "%dx%d A / %d mA".formatted(
                                ts.polosTermomagnetica(),
                                ts.disyuntorDiferencialNominalA(),
                                sens
                        ),
                        "u",
                        "1"
                ));
            }
        }

        int polosCirc = tg != null && tg.polosTermomagnetica() > 0 ? tg.polosTermomagnetica() : 2;
        for (CircuitoCalculadoDto circ : circuitos) {
            String cod = valorSeguro(circ.codigo(), "CIRC");
            String tipo = valorSeguro(circ.tipo(), "");
            String desc = cod + (tipo.isBlank() ? "" : " (" + tipo + ")");
            items.add(new MaterialItem(
                    "Circuitos",
                    "ITM terminal para circuito " + desc,
                    String.format(
                            Locale.US,
                            "%dx%d A — Ip %.2f A / %.0f VA sim.",
                            polosCirc,
                            circ.calibreTermomagneticaA(),
                            circ.corrienteProyectoA(),
                            circ.potenciaSimultaneaVa()
                    ),
                    "u",
                    "1"
            ));
        }

        for (CircuitoCalculadoDto.ConductorCalculadoDto cond : conductores) {
            String tramo = valorSeguro(cond.tramo(), "Tramo");
            String sec = formatSeccion(cond.seccionMm2());
            String mat = valorSeguro(cond.materialAislacion(), "Cu aislado");
            String cant = estimarCantidadConductor(c, cond);
            String esp = cond.corrienteCalculoA() > 0
                    ? String.format(Locale.US, "Sección %s mm² (%s) — Ic %.2f A", sec, mat, cond.corrienteCalculoA())
                    : "Sección %s mm² (%s)".formatted(sec, mat);

            items.add(new MaterialItem(
                    "Conductores",
                    "Conductor tramo " + tramo,
                    esp,
                    "m",
                    cant
            ));
        }

        String secPe = buscarSeccion(conductores, "PE", "4");
        items.add(new MaterialItem(
                "Puesta a Tierra",
                "Electrodo de dispersión (jabalina), cámara de inspección y morseto",
                "Vinculado a conductor PE %s mm²".formatted(secPe),
                "cjto",
                "1"
        ));

        int totalBocas = circuitos.stream().mapToInt(CircuitoCalculadoDto::cantidadBocas).sum();
        if (totalBocas > 0) {
            items.add(new MaterialItem(
                    "Bocas y Cajas",
                    "Cajas, bocas y cañerías de circuitos terminales",
                    "%d bocas totales en %d circuitos".formatted(totalBocas, circuitos.size()),
                    "bocas",
                    String.valueOf(totalBocas)
            ));
        }

        return items;
    }

    private String construirFilasTabla(List<MaterialItem> items) {
        StringBuilder sb = new StringBuilder();
        for (MaterialItem item : items) {
            sb.append("""
                    <tr>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                      <td>%s</td>
                    </tr>
                    """.formatted(
                    escapeXml(item.rubro()),
                    escapeXml(item.descripcion()),
                    escapeXml(item.especificacion()),
                    escapeXml(item.unidad()),
                    escapeXml(item.cantidad())
            ));
        }
        return sb.toString();
    }

    private String buscarSeccion(
            List<CircuitoCalculadoDto.ConductorCalculadoDto> conductores,
            String tramo,
            String fallback
    ) {
        if (conductores == null) return fallback;
        for (var cond : conductores) {
            if (cond.tramo() != null && cond.tramo().toUpperCase(Locale.ROOT).contains(tramo)) {
                return formatSeccion(cond.seccionMm2());
            }
        }
        return fallback;
    }

    private String valorSeguro(String val, String fallback) {
        return (val == null || val.isBlank()) ? fallback : val;
    }
}