package ar.gob.cba.instalaciones.unifilar.service.impl;

import ar.gob.cba.instalaciones.unifilar.service.SvgToPdfService;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.fop.svg.PDFTranscoder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.StringReader;

@Service
public class SvgToPdfServiceImpl implements SvgToPdfService {

    @Override
    public byte[] convertirSvgAPdf(String svgContent) {
        try (StringReader reader = new StringReader(svgContent);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            TranscoderInput input = new TranscoderInput(reader);
            TranscoderOutput output = new TranscoderOutput(out);

            PDFTranscoder transcoder = new PDFTranscoder();
            // Opcional: ajustar tamaño de hoja (ej. A3 apaisado en puntos: 1190 x 842)
            // transcoder.addTranscodingHint(PDFTranscoder.KEY_WIDTH, 1190f);
            // transcoder.addTranscodingHint(PDFTranscoder.KEY_HEIGHT, 842f);

            transcoder.transcode(input, output);
            out.flush();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Error al convertir el diagrama SVG a PDF", e);
        }
    }
}