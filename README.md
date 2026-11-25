# Instalaciones Eléctricas Backend (Java 21 + Spring Boot)

Base del proyecto con:
1. **Extracción de planos normalizados mediante IA multimodal** (`vision`):
   - `PlanoVisionController` (`POST /api/v1/planos/extraer`)
   - `PlanoVisionService`
   - `PlanoAiConfig`
   - `AmbienteExtraido`, `PlanoExtraidoDto`
   - `src/main/resources/prompts/extraccion-plano-system.st`
2. **Ingesta automática al iniciar la app y consulta de manuales normativos** (`rag`):
   - `ManualesStartupRunner`: lee automáticamente al levantar la aplicación:
     - `resources/PDF/Guía AEA 770.pdf` (`fuente = AEA_770`)
     - `resources/PDF/Manual ERSEP 2021- Entrega 1.pdf` (`fuente = ERSEP_2021_E1`)
     - `resources/PDF/Manual ERSEP 2021- Entrega 2.pdf` (`fuente = ERSEP_2021_E2`)
   - `ManualIngestaServiceImpl` (con verificación idempotente para no duplicar si ya están indexados en un `VectorStore` persistente)
   - `NormativaConsultaServiceImpl`
