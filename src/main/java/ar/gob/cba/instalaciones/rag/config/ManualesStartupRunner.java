package ar.gob.cba.instalaciones.rag.config;

import ar.gob.cba.instalaciones.rag.dto.ManualIngestaRequest;
import ar.gob.cba.instalaciones.rag.service.ManualIngestaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ManualesStartupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ManualesStartupRunner.class);

    private final ManualIngestaService ingestaService;
    private final ResourceLoader resourceLoader;
    private final boolean ingestaAlIniciarHabilitada;

    public ManualesStartupRunner(
            ManualIngestaService ingestaService,
            ResourceLoader resourceLoader,
            @Value("${app.normativa.ingesta-al-iniciar:true}") boolean ingestaAlIniciarHabilitada
    ) {
        this.ingestaService = ingestaService;
        this.resourceLoader = resourceLoader;
        this.ingestaAlIniciarHabilitada = ingestaAlIniciarHabilitada;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!ingestaAlIniciarHabilitada) {
            log.info("Ingesta automática de manuales al iniciar deshabilitada por configuración.");
            return;
        }

        List<ManualClasspathSpec> manuales = List.of(
                new ManualClasspathSpec(
                        "classpath:PDF/Guía AEA 770.pdf",
                        new ManualIngestaRequest("AEA_770", "GENERAL", "770")
                ),
                new ManualClasspathSpec(
                        "classpath:PDF/Manual ERSEP 2021- Entrega 1.pdf",
                        new ManualIngestaRequest("ERSEP_2021_E1", "GENERAL", "2021-E1")
                ),
                new ManualClasspathSpec(
                        "classpath:PDF/Manual ERSEP 2021- Entrega 2.pdf",
                        new ManualIngestaRequest("ERSEP_2021_E2", "GENERAL", "2021-E2")
                )
        );

        for (ManualClasspathSpec spec : manuales) {
            Resource resource = resourceLoader.getResource(spec.classpathLocation());
            if (!resource.exists()) {
                log.warn("No se encontró el manual en recursos: {}", spec.classpathLocation());
                continue;
            }
            int chunks = ingestaService.ingestarPdf(resource, spec.request());
            log.info("Manual '{}' ({}) indexado correctamente: {} chunks.",
                    spec.classpathLocation(), spec.request().fuente(), chunks);
        }
    }

    private record ManualClasspathSpec(
            String classpathLocation,
            ManualIngestaRequest request
    ) {}
}
