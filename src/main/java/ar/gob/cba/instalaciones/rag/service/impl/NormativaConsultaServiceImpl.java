package ar.gob.cba.instalaciones.rag.service.impl;

import ar.gob.cba.instalaciones.rag.service.NormativaConsultaService;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NormativaConsultaServiceImpl implements NormativaConsultaService {

    private final VectorStore vectorStore;

    public NormativaConsultaServiceImpl(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public String buscarContexto(String consulta, String fuenteOpcional, int topK) {
        FilterExpressionBuilder b = new FilterExpressionBuilder();

        SearchRequest.Builder reqBuilder = SearchRequest.builder()
                .query(consulta)
                .topK(topK)
                .similarityThreshold(0.55);

        if (fuenteOpcional != null && !fuenteOpcional.isBlank()) {
            reqBuilder.filterExpression(b.eq("fuente", fuenteOpcional).build());
        }

        List<Document> docs = vectorStore.similaritySearch(reqBuilder.build());

        return docs.stream()
                .map(d -> {
                    String fuente = String.valueOf(d.getMetadata().getOrDefault("fuente", "NORMA"));
                    String pagina = String.valueOf(d.getMetadata().getOrDefault("pagina", "?"));
                    return "[%s | pág. %s]\n%s".formatted(fuente, pagina, d.getText());
                })
                .collect(Collectors.joining("\n\n---\n\n"));
    }
}
