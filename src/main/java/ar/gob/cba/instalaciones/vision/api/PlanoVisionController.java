package ar.gob.cba.instalaciones.vision.api;

import ar.gob.cba.instalaciones.vision.dto.PlanoExtraidoDto;
import ar.gob.cba.instalaciones.vision.service.PlanoVisionService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/planos")
public class PlanoVisionController {

    private static final Set<String> CONTENT_TYPES_PERMITIDOS = Set.of(
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_JPEG_VALUE,
            "image/webp"
    );

    private final PlanoVisionService planoVisionService;

    public PlanoVisionController(PlanoVisionService planoVisionService) {
        this.planoVisionService = planoVisionService;
    }

    @PostMapping(value = "/extraer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlanoExtraidoDto> extraerPlano(
            @RequestPart("imagen") MultipartFile imagen,
            @RequestPart(value = "contextoAdicional", required = false) String contextoAdicional
    ) {
        if (imagen == null || imagen.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String contentType = imagen.getContentType();
        if (contentType != null && !CONTENT_TYPES_PERMITIDOS.contains(contentType.toLowerCase())) {
            return ResponseEntity.badRequest().build();
        }

        PlanoExtraidoDto resultado = planoVisionService.extraerDesdeImagen(imagen, contextoAdicional);
        return ResponseEntity.ok(resultado);
    }
}
