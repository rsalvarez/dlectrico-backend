package ar.gob.cba.instalaciones.rag.service.impl;

import ar.gob.cba.instalaciones.rag.dto.ManualIngestaRequest;
import ar.gob.cba.instalaciones.rag.service.ManualIngestaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

@Service
public class ManualIngestaServiceImpl implements ManualIngestaService {

    private static final Logger log = LoggerFactory.getLogger(ManualIngestaServiceImpl.class);

    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;

    public ManualIngestaServiceImpl(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.textSplitter = new TokenTextSplitter(
                600,
                120,
                20,
                10000,
                true
        );
    }

    @Override
    public int ingestarPdf(Resource pdfResource, ManualIngestaRequest request) {
        if (yaFueIngestado(request.fuente())) {
            log.info("La fuente '{}' ya se encuentra indexada en el VectorStore. Se omite re-ingesta.", request.fuente());
            return 0;
        }

        PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(pdfResource);
        List<Document> paginas = pdfReader.get();

        List<Document> documentosEnriquecidos = paginas.stream()
                .map(doc -> enriquecerMetadata(doc, request))
                .toList();

        List<Document> chunks = textSplitter.apply(documentosEnriquecidos);

        List<Document> chunksFinales = chunks.stream()
                .map(chunk -> {
                    Map<String, Object> meta = new HashMap<>(chunk.getMetadata());
                    meta.put("chunk_hash", sha256(chunk.getText()));
                    return new Document(chunk.getText(), meta);
                })
                .toList();

        int tamanioLote = 10; // Ajustá según el tamaño de tus chunks (ej. 5 a 15)
        long pausaEntreLotesMs = 15_000L; // 15 segundos entre lotes para no superar los 30k tokens/min

        for (int i = 0; i < chunks.size(); i += tamanioLote) {
            int fin = Math.min(i + tamanioLote, chunks.size());
            List<Document> lote = chunks.subList(i, fin);

            vectorStore.add(lote);

            if (fin < chunks.size()) {
                try {
                    Thread.sleep(pausaEntreLotesMs);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("Ingesta interrumpida durante la pausa por rate limit", e);
                }
            }
        }
        return chunksFinales.size();
    }

    private boolean yaFueIngestado(String fuente) {
        try {
            FilterExpressionBuilder b = new FilterExpressionBuilder();
            List<Document> existentes = vectorStore.similaritySearch(
                    SearchRequest.builder()
                            .query("instalacion electrica")
                            .topK(1)
                            .similarityThresholdAll()
                            .filterExpression(b.eq("fuente", fuente).build())
                            .build()
            );
            return existentes != null && !existentes.isEmpty();
        } catch (Exception e) {
            log.debug("No se pudo verificar existencia previa para '{}': {}", fuente, e.getMessage());
            return false;
        }
    }

    private Document enriquecerMetadata(Document doc, ManualIngestaRequest request) {
        Map<String, Object> metadata = new HashMap<>(doc.getMetadata());
        metadata.put("fuente", request.fuente());
        metadata.put("tema", request.temaPrincipal());
        metadata.put("version_normativa", request.versionNormativa());
        return new Document(doc.getText(), metadata);
    }

    private String sha256(String texto) {
        if (texto == null) return "";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(texto.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Error calculando hash SHA-256", e);
        }
    }
}
