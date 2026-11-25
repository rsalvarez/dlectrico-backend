package ar.gob.cba.instalaciones.unifilar.api;

import ar.gob.cba.instalaciones.unifilar.dto.CircuitoCalculadoDto;
import ar.gob.cba.instalaciones.unifilar.service.SvgToPdfService;
import ar.gob.cba.instalaciones.unifilar.service.UnifilarPdfService;
import ar.gob.cba.instalaciones.unifilar.service.UnifilarService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Locale;

@RestController
@RequestMapping("/api/unifilar")
@Slf4j
public class UnifilarController {

    private final UnifilarService unifilarService;

    private final UnifilarPdfService unifilarPdfService;

    public UnifilarController(UnifilarService unifilarService, UnifilarPdfService unifilarPdfService) {
        this.unifilarService = unifilarService;
        this.unifilarPdfService = unifilarPdfService;

    }

    @PostMapping(
            path = "/pdf-completo",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_PDF_VALUE
    )
    public ResponseEntity<byte[]> generarPdfCompleto(
            @RequestBody CircuitoCalculadoDto.CalculoRespuestaDto calculo
    ) {
        CircuitoCalculadoDto.UnifilarResponseDto unifilar = unifilarService.generarUnifilar(calculo);
        byte[] pdfBytes = unifilarPdfService.generarPdfUnifilarYMateriales(calculo, unifilar.svg());

        String nombreArchivo = sanitizarNombreArchivo(calculo.proyecto()) + "-unifilar-y-materiales.pdf";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .header("X-Grado-Electrificacion", valorSeguroHeader(calculo.gradoFinal(), "-"))
                .header("X-DPMS-VA", String.format(Locale.US, "%.0f", calculo.dpmsVa()))
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfBytes.length)
                .body(pdfBytes);
    }

    private String sanitizarNombreArchivo(String proyecto) {
        if (proyecto == null || proyecto.isBlank()) {
            return "instalacion";
        }
        String sinAcentos = Normalizer.normalize(proyecto, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        String limpio = sinAcentos.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        return limpio.isBlank() ? "instalacion" : limpio;
    }

    private String valorSeguroHeader(String valor, String fallback) {
        if (valor == null || valor.isBlank()) {
            return fallback;
        }
        return new String(valor.getBytes(StandardCharsets.US_ASCII), StandardCharsets.US_ASCII)
                .replaceAll("[\\r\\n]", " ");
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CircuitoCalculadoDto.UnifilarResponseDto> generar(@RequestBody CircuitoCalculadoDto.CalculoRespuestaDto calculo) {
        log.info("Entrando a generar unifiliar");
        return ResponseEntity.ok(unifilarService.generarUnifilar(calculo));
    }


}