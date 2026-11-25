package ar.gob.cba.instalaciones.vision.service;

import ar.gob.cba.instalaciones.vision.dto.AmbienteExtraido;
import ar.gob.cba.instalaciones.vision.dto.PlanoExtraidoDto;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Collections;
import java.util.Objects;

@Service
public class PlanoVisionService {

    private final ChatClient planoVisionChatClient;

    public PlanoVisionService(ChatClient planoVisionChatClient) {
        this.planoVisionChatClient = planoVisionChatClient;
    }

    public PlanoExtraidoDto extraerDesdeImagen(MultipartFile imagen, String contextoAdicional) {
        byte[] bytes = leerBytes(imagen);
        MimeType mimeType = resolverMimeType(imagen);
        ByteArrayResource imageResource = new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return imagen.getOriginalFilename() != null
                        ? imagen.getOriginalFilename()
                        : "plano.png";
            }
        };

        String instruccionesUsuario = construirUserPrompt(contextoAdicional);

        int maxIntentos = 2;


        try {
            PlanoExtraidoDto extraido = planoVisionChatClient.prompt()
                    .user(userSpec -> userSpec
                            .text(instruccionesUsuario)
                            .media(mimeType, imageResource)
                    )
                    .call()
                    .entity(PlanoExtraidoDto.class);
            return Objects.requireNonNull(extraido, "El modelo no devolvió datos estructurados del plano");
        } catch (Exception ex) {

            throw new IllegalStateException(
                    "El modelo de visión está temporalmente saturado (503). Reintentá en unos segundos.",
                    ex
            );

        }

    }


    private String construirUserPrompt(String contextoAdicional) {
        String extra = (contextoAdicional == null || contextoAdicional.isBlank())
                ? "Ninguno."
                : contextoAdicional.trim();

        return """
                Analizá la imagen adjunta de un plano normalizado y extraé todos los espacios con sus medidas.
                Contexto adicional provisto por el usuario: %s
                """.formatted(extra);
    }

    private byte[] leerBytes(MultipartFile imagen) {
        try {
            return imagen.getBytes();
        } catch (IOException e) {
            throw new IllegalStateException("No se pudo leer el archivo de imagen del plano", e);
        }
    }

    private MimeType resolverMimeType(MultipartFile imagen) {
        String contentType = imagen.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            return MimeTypeUtils.parseMimeType(contentType);
        }
        return MimeTypeUtils.IMAGE_PNG;
    }
}
