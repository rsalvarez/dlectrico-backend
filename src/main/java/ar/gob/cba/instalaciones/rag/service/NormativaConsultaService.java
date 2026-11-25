package ar.gob.cba.instalaciones.rag.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;


public interface NormativaConsultaService {


    String buscarContexto(String consulta, String fuenteOpcional, int topK);
}
