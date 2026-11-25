package ar.gob.cba.instalaciones.calculo.rules;


import ar.gob.cba.instalaciones.calculo.enums.GradoElectrificacion;
import ar.gob.cba.instalaciones.calculo.enums.TipoCircuito;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReglasNormativasDefault implements NormativaReglasProvider {

    private static final List<Integer> CALIBRES_ITM_COMERCIALES = List.of(
            6, 10, 16, 20, 25, 32, 40, 50, 63
    );

    private static final List<Integer> CALIBRES_ID_COMERCIALES = List.of(
            25, 40, 63
    );

    @Override
    public double factorSuperficieSemicubierta() {
        // Parametrizable según criterio adoptado en el proyecto / normativa cargada
        return 0.50;
    }

    @Override
    public GradoElectrificacion gradoPorSla(double slaM2) {
        if (slaM2 <= 60.0) {
            return GradoElectrificacion.MINIMA;
        } else if (slaM2 <= 130.0) {
            return GradoElectrificacion.MEDIA;
        } else if (slaM2 <= 200.0) {
            return GradoElectrificacion.ELEVADA;
        }
        return GradoElectrificacion.SUPERIOR;
    }

    @Override
    public double potenciaMaximaDpmsVa(GradoElectrificacion grado) {
        return switch (grado) {
            case MINIMA -> 3700.0;
            case MEDIA -> 7000.0;
            case ELEVADA -> 11000.0;
            case SUPERIOR -> 18000.0;
        };
    }

    @Override
    public GradoElectrificacion gradoPorDpms(double dpmsVa) {
        if (dpmsVa <= 3700.0) {
            return GradoElectrificacion.MINIMA;
        } else if (dpmsVa <= 7000.0) {
            return GradoElectrificacion.MEDIA;
        } else if (dpmsVa <= 11000.0) {
            return GradoElectrificacion.ELEVADA;
        }
        return GradoElectrificacion.SUPERIOR;
    }

    @Override
    public int minimosCircuitosPorGrado(GradoElectrificacion grado) {
        return switch (grado) {
            case MINIMA -> 2;
            case MEDIA -> 3;
            case ELEVADA -> 5;
            case SUPERIOR -> 6;
        };
    }

    @Override
    public double potenciaUnitariaCircuitoVa(TipoCircuito tipo) {
        return switch (tipo) {
            case IUG -> 1500.0;
            case TUG -> 2200.0;
            case TUE -> 3300.0;
            case ACU -> 2500.0;
            case IUE -> 1800.0;
        };
    }

    @Override
    public double coeficienteSimultaneidad(TipoCircuito tipo, int cantidadCircuitosDelTipo) {
        if (cantidadCircuitosDelTipo <= 1) {
            return 1.0;
        }
        return switch (tipo) {
            case IUG -> cantidadCircuitosDelTipo == 2 ? 0.85 : 0.70;
            case TUG -> cantidadCircuitosDelTipo == 2 ? 0.80 : 0.65;
            case TUE, ACU -> cantidadCircuitosDelTipo == 2 ? 0.75 : 0.60;
            case IUE -> 0.80;
        };
    }

    @Override
    public double seccionMinimaConductorMm2(TipoCircuito tipo) {
        return switch (tipo) {
            case IUG, IUE -> 1.5;
            case TUG -> 2.5;
            case TUE, ACU -> 4.0;
        };
    }

    @Override
    public double seccionMinimaLineaPrincipalMm2(GradoElectrificacion grado, double corrienteA) {
        double base = switch (grado) {
            case MINIMA, MEDIA -> 4.0;
            case ELEVADA -> 6.0;
            case SUPERIOR -> 10.0;
        };
        return Math.max(base, seccionPorCorriente(corrienteA));
    }

    @Override
    public double seccionMinimaLineaSeccionalMm2(GradoElectrificacion grado, double corrienteA) {
        double base = switch (grado) {
            case MINIMA -> 2.5;
            case MEDIA -> 4.0;
            case ELEVADA -> 6.0;
            case SUPERIOR -> 10.0;
        };
        return Math.max(base, seccionPorCorriente(corrienteA));
    }

    @Override
    public double seccionMinimaTierraMm2(double seccionFaseMm2) {
        if (seccionFaseMm2 <= 16.0) {
            return Math.max(2.5, seccionFaseMm2);
        } else if (seccionFaseMm2 <= 35.0) {
            return 16.0;
        }
        return seccionFaseMm2 / 2.0;
    }

    @Override
    public int seleccionarCalibreTermomagneticaA(double corrienteMinimaRequeridaA, double corrienteAdmisibleConductorA) {
        for (int calibre : CALIBRES_ITM_COMERCIALES) {
            if (calibre >= corrienteMinimaRequeridaA && calibre <= corrienteAdmisibleConductorA) {
                return calibre;
            }
        }
        return CALIBRES_ITM_COMERCIALES.getLast();
    }

    @Override
    public int seleccionarCalibreDisyuntorA(double corrienteAlimentacionA) {
        for (int calibre : CALIBRES_ID_COMERCIALES) {
            if (calibre >= corrienteAlimentacionA) {
                return calibre;
            }
        }
        return CALIBRES_ID_COMERCIALES.getLast();
    }

    @Override
    public int sensibilidadDiferencialMa(TipoCircuito tipo) {
        // Protección complementaria / personas: 30 mA
        return 30;
    }

    private double seccionPorCorriente(double corrienteA) {
        if (corrienteA <= 15.0) return 1.5;
        if (corrienteA <= 21.0) return 2.5;
        if (corrienteA <= 28.0) return 4.0;
        if (corrienteA <= 36.0) return 6.0;
        if (corrienteA <= 50.0) return 10.0;
        return 16.0;
    }
}