package ar.gob.cba.instalaciones.calculo.service.impl;

import ar.gob.cba.instalaciones.calculo.dto.SolicitudCalculoDto;
import ar.gob.cba.instalaciones.calculo.enums.GradoElectrificacion;
import ar.gob.cba.instalaciones.calculo.enums.TipoAcometida;
import ar.gob.cba.instalaciones.calculo.enums.TipoCircuito;
import ar.gob.cba.instalaciones.calculo.rules.NormativaReglasProvider;
import ar.gob.cba.instalaciones.calculo.service.InstalacionCalculoService;
import ar.gob.cba.instalaciones.vision.dto.AmbienteExtraido;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class InstalacionCalculoServiceImpl implements InstalacionCalculoService {

    private final NormativaReglasProvider reglas;

    public InstalacionCalculoServiceImpl(NormativaReglasProvider reglas) {
        this.reglas = reglas;
    }

    @Override
    public SolicitudCalculoDto.ResultadoCalculoDto calcular(SolicitudCalculoDto solicitud) {
        List<String> criterios = new ArrayList<>();
        List<String> advertencias = new ArrayList<>();

        TipoAcometida acometida = solicitud.tipoAcometida() == null
                ? TipoAcometida.CLASE_II_BAJADA
                : solicitud.tipoAcometida();
        criterios.add("Tipo de acometida / bajada adoptado: " + acometida + ".");

        double cubierta = 0.0;
        double semicubiertaComputable = 0.0;
        double factorSemi = reglas.factorSuperficieSemicubierta();

        for (AmbienteExtraido amb : solicitud.ambientes()) {
            double sup = resolverSuperficieAmbiente(amb, advertencias);
            AmbienteExtraido.TipoSuperficie tipoSup = amb.tipoSuperficie() == null
                    ? AmbienteExtraido.TipoSuperficie.NO_DEFINIDA
                    : amb.tipoSuperficie();

            switch (tipoSup) {
                case CUBIERTA -> cubierta += sup;
                case SEMICUBIERTA -> semicubiertaComputable += sup * factorSemi;
                case NO_DEFINIDA -> {
                    cubierta += sup;
                    advertencias.add("Ambiente '" + amb.nombre() + "' sin tipo de superficie definido; computado como cubierta.");
                }
            }

            if (!amb.medidaConfiable()) {
                advertencias.add("Ambiente '" + amb.nombre() + "' marcado con medida no confiable: " + amb.observacion());
            }
        }

        double slaM2 = redondear2(cubierta + semicubiertaComputable);
        criterios.add("SLA calculada = cubierta (" + redondear2(cubierta) + " m2) + semicubierta*" + factorSemi + " (" + redondear2(semicubiertaComputable) + " m2) = " + slaM2 + " m2.");

        GradoElectrificacion gradoPreliminar = reglas.gradoPorSla(slaM2);
        criterios.add("Grado de electrificación preliminar por SLA: " + gradoPreliminar);

        List<SolicitudCalculoDto.CircuitoCalculado> circuitos = generarCircuitos(solicitud, gradoPreliminar, slaM2, criterios);

        double potenciaTotalSinSimultaneidadVa = circuitos.stream()
                .mapToDouble(SolicitudCalculoDto.CircuitoCalculado::potenciaAsignadaVa)
                .sum();

        double dpmsVa = circuitos.stream()
                .mapToDouble(SolicitudCalculoDto.CircuitoCalculado::potenciaSimultaneaVa)
                .sum();

        double dpmsKw = redondear2(dpmsVa / 1000.0);

        GradoElectrificacion gradoPorPotencia = reglas.gradoPorDpms(dpmsVa);
        GradoElectrificacion gradoFinal = gradoPorPotencia.ordinal() > gradoPreliminar.ordinal()
                ? gradoPorPotencia
                : gradoPreliminar;

        if (gradoFinal != gradoPreliminar) {
            criterios.add("Grado elevado de " + gradoPreliminar + " a " + gradoFinal + " por DPMS de " + redondear2(dpmsVa) + " VA.");
        } else {
            criterios.add("Grado de electrificación final confirmado: " + gradoFinal + ".");
        }

        double corrienteTotalA = calcularCorrienteA(dpmsVa, solicitud.tensionNominalV(), solicitud.fases());

        List<SolicitudCalculoDto.ConductorTramo> conductores = dimensionarConductores(
                solicitud, gradoFinal, circuitos, corrienteTotalA, criterios
        );

        int polos = solicitud.fases() == 3 ? 4 : 2;
        int itmGeneralA = reglas.seleccionarCalibreTermomagneticaA(corrienteTotalA, corrienteTotalA * 1.25);

        SolicitudCalculoDto.ProteccionTablero tableroGeneral = construirTableroGeneral(
                acometida, itmGeneralA, polos, corrienteTotalA, criterios
        );

        int itmSeccionalA = reglas.seleccionarCalibreTermomagneticaA(corrienteTotalA, corrienteTotalA * 1.25);
        int idSeccionalA = reglas.seleccionarCalibreDisyuntorA(itmSeccionalA);

        SolicitudCalculoDto.ProteccionTablero tableroSeccional = new SolicitudCalculoDto.ProteccionTablero(
                "TABLERO_SECCIONAL",
                itmSeccionalA,
                polos,
                "Curva C",
                idSeccionalA,
                reglas.sensibilidadDiferencialMa(TipoCircuito.TUG),
                List.of("Disyuntor diferencial de 30 mA para protección de personas en circuitos terminales.")
        );

        return new SolicitudCalculoDto.ResultadoCalculoDto(
                solicitud.proyecto(),
                redondear2(cubierta),
                redondear2(semicubiertaComputable),
                slaM2,
                gradoPreliminar,
                gradoFinal,
                circuitos,
                redondear2(potenciaTotalSinSimultaneidadVa),
                redondear2(dpmsVa),
                dpmsKw,
                redondear2(corrienteTotalA),
                conductores,
                tableroGeneral,
                tableroSeccional,
                criterios,
                advertencias
        );
    }

    private SolicitudCalculoDto.ProteccionTablero construirTableroGeneral(
            TipoAcometida acometida,
            int itmGeneralA,
            int polos,
            double corrienteTotalA,
            List<String> criterios
    ) {
        if (acometida == TipoAcometida.CLASE_II_BAJADA) {
            criterios.add("Tablero General configurado bajo criterio Clase II (doble aislación): sin conexión de masa a PAT ni ID duplicado en TG; protección diferencial de 30 mA en TS.");
            return new SolicitudCalculoDto.ProteccionTablero(
                    "TABLERO_GENERAL",
                    itmGeneralA,
                    polos,
                    "Curva C",
                    null,
                    null,
                    List.of(
                            "Gabinete / acometida de doble aislación (Clase II): no se conecta PAT a masa del gabinete.",
                            "Protección diferencial de 30 mA asignada al Tablero Seccional."
                    )
            );
        }

        int idGeneralA = reglas.seleccionarCalibreDisyuntorA(corrienteTotalA);
        criterios.add("Tablero General configurado para acometida " + acometida + " con ID de " + idGeneralA + " A.");
        return new SolicitudCalculoDto.ProteccionTablero(
                "TABLERO_GENERAL",
                itmGeneralA,
                polos,
                "Curva C",
                idGeneralA,
                reglas.sensibilidadDiferencialMa(TipoCircuito.TUG),
                List.of("Verificar selectividad aguas abajo con Tablero Seccional y circuitos terminales.")
        );
    }

    private double resolverSuperficieAmbiente(AmbienteExtraido amb, List<String> advertencias) {
        if (amb.superficieInformadaM2() != null && amb.superficieInformadaM2() > 0) {
            return amb.superficieInformadaM2();
        }
        if (amb.largoM() != null && amb.anchoM() != null && amb.largoM() > 0 && amb.anchoM() > 0) {
            return amb.largoM() * amb.anchoM();
        }
        advertencias.add("Ambiente '" + amb.nombre() + "' sin medidas válidas; se toma 0 m2.");
        return 0.0;
    }

    private List<SolicitudCalculoDto.CircuitoCalculado> generarCircuitos(
            SolicitudCalculoDto solicitud,
            GradoElectrificacion grado,
            double slaM2,
            List<String> criterios
    ) {
        int minCircuitos = reglas.minimosCircuitosPorGrado(grado);
        int cantIug = Math.max(1, (int) Math.ceil((double) solicitud.cantidadBocasIluminacion() / 15));
        int cantTug = Math.max(1, (int) Math.ceil((double) solicitud.cantidadBocasITomaCorrientes() / 15));

        if (cantIug + cantTug < minCircuitos) {
            if (cantTug >= cantIug) {
                cantTug++;
            } else {
                cantIug++;
            }
        }

        List<SolicitudCalculoDto.CircuitoCalculado> lista = new ArrayList<>();
        double potIug = reglas.potenciaUnitariaCircuitoVa(TipoCircuito.IUG);
        double simIug = reglas.coeficienteSimultaneidad(TipoCircuito.IUG, cantIug);
        double secIug = reglas.seccionMinimaConductorMm2(TipoCircuito.IUG);
        double ipIug = calcularCorrienteA(potIug, solicitud.tensionNominalV(), 1);
        int itmIug = reglas.seleccionarCalibreTermomagneticaA(ipIug, 16.0);

        for (int i = 1; i <= cantIug; i++) {
            lista.add(new SolicitudCalculoDto.CircuitoCalculado(
                    "IUG-" + i,
                    TipoCircuito.IUG,
                    Math.max(4, solicitud.ambientes().size() / cantIug),
                    potIug,
                    simIug,
                    redondear2(potIug * simIug),
                    redondear2(ipIug),
                    secIug,
                    itmIug,
                    "Circuito de iluminación de uso general"
            ));
        }

        double potTug = reglas.potenciaUnitariaCircuitoVa(TipoCircuito.TUG);
        double simTug = reglas.coeficienteSimultaneidad(TipoCircuito.TUG, cantTug);
        double secTug = reglas.seccionMinimaConductorMm2(TipoCircuito.TUG);
        double ipTug = calcularCorrienteA(potTug, solicitud.tensionNominalV(), 1);
        int itmTug = reglas.seleccionarCalibreTermomagneticaA(ipTug, 20.0);

        for (int i = 1; i <= cantTug; i++) {
            lista.add(new SolicitudCalculoDto.CircuitoCalculado(
                    "TUG-" + i,
                    TipoCircuito.TUG,
                    Math.max(4, solicitud.ambientes().size() / cantTug),
                    potTug,
                    simTug,
                    redondear2(potTug * simTug),
                    redondear2(ipTug),
                    secTug,
                    itmTug,
                    "Circuito de tomacorrientes de uso general"
            ));
        }

        List<String> cargas = new ArrayList<>(solicitud.cargasEspecialesDetectadas());
        solicitud.ambientes().forEach(a -> {
            if (a.nombre() != null && (a.nombre().toLowerCase().contains("aire") || a.nombre().toLowerCase().contains("lavadero"))) {
                cargas.add(a.nombre());
            }
        });

        int cantEspeciales = cargas.size();
        if (cantEspeciales > 0) {
            double potTue = reglas.potenciaUnitariaCircuitoVa(TipoCircuito.TUE);
            double simTue = reglas.coeficienteSimultaneidad(TipoCircuito.TUE, cantEspeciales);
            double secTue = reglas.seccionMinimaConductorMm2(TipoCircuito.TUE);
            double ipTue = calcularCorrienteA(potTue, solicitud.tensionNominalV(), 1);
            int itmTue = reglas.seleccionarCalibreTermomagneticaA(ipTue, 25.0);

            for (int i = 1; i <= cantEspeciales; i++) {
                lista.add(new SolicitudCalculoDto.CircuitoCalculado(
                        "TUE-" + i,
                        TipoCircuito.TUE,
                        1,
                        potTue,
                        simTue,
                        redondear2(potTue * simTue),
                        redondear2(ipTue),
                        secTue,
                        itmTue,
                        "Circuito especial asociado a: " + cargas.get(i - 1)
                ));
            }
        }

        criterios.add("Generados " + lista.size() + " circuitos (" + cantIug + " IUG, " + cantTug + " TUG, " + cantEspeciales + " TUE).");
        return lista;
    }

    private List<SolicitudCalculoDto.ConductorTramo> dimensionarConductores(
            SolicitudCalculoDto solicitud,
            GradoElectrificacion grado,
            List<SolicitudCalculoDto.CircuitoCalculado> circuitos,
            double corrienteTotalA,
            List<String> criterios
    ) {
        List<SolicitudCalculoDto.ConductorTramo> tramos = new ArrayList<>();

        double secLp = reglas.seccionMinimaLineaPrincipalMm2(grado, corrienteTotalA);
        tramos.add(new SolicitudCalculoDto.ConductorTramo(
                "Línea Principal (LP)",
                secLp,
                redondear2(corrienteTotalA),
                "CU_AISLADO",
                "Mínimo por grado " + grado + " y corriente de proyecto"
        ));

        double secLs = reglas.seccionMinimaLineaSeccionalMm2(grado, corrienteTotalA);
        tramos.add(new SolicitudCalculoDto.ConductorTramo(
                "Línea Seccional (LS)",
                secLs,
                redondear2(corrienteTotalA),
                "CU_AISLADO",
                "Mínimo por grado " + grado + " y corriente de proyecto"
        ));

        for (SolicitudCalculoDto.CircuitoCalculado c : circuitos) {
            tramos.add(new SolicitudCalculoDto.ConductorTramo(
                    "Circuito " + c.codigo(),
                    c.seccionConductorMm2(),
                    c.corrienteProyectoA(),
                    "CU_AISLADO",
                    c.descripcion()
            ));
        }

        double secPe = reglas.seccionMinimaTierraMm2(secLp);
        tramos.add(new SolicitudCalculoDto.ConductorTramo(
                "Conductor de Protección / PAT (PE)",
                secPe,
                0.0,
                "CU_VERDE_AMARILLO",
                "Vinculado a sección de fase (" + secLp + " mm2)"
        ));

        criterios.add("Dimensionados " + tramos.size() + " tramos de conductores (LP, LS, circuitos terminales y PE).");
        return tramos;
    }

    private double calcularCorrienteA(double potenciaVa, int tensionV, int fases) {
        if (tensionV <= 0) tensionV = 220;
        if (fases == 3) {
            return potenciaVa / (Math.sqrt(3.0) * tensionV);
        }
        return potenciaVa / tensionV;
    }

    private double redondear2(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}