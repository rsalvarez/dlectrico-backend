package ar.gob.cba.instalaciones.calculo.rules;


import ar.gob.cba.instalaciones.calculo.enums.GradoElectrificacion;
import ar.gob.cba.instalaciones.calculo.enums.TipoCircuito;

public interface NormativaReglasProvider {

    double factorSuperficieSemicubierta();

    GradoElectrificacion gradoPorSla(double slaM2);

    double potenciaMaximaDpmsVa(GradoElectrificacion grado);

    GradoElectrificacion gradoPorDpms(double dpmsVa);

    int minimosCircuitosPorGrado(GradoElectrificacion grado);

    double potenciaUnitariaCircuitoVa(TipoCircuito tipo);

    double coeficienteSimultaneidad(TipoCircuito tipo, int cantidadCircuitosDelTipo);

    double seccionMinimaConductorMm2(TipoCircuito tipo);

    double seccionMinimaLineaPrincipalMm2(GradoElectrificacion grado, double corrienteA);

    double seccionMinimaLineaSeccionalMm2(GradoElectrificacion grado, double corrienteA);

    double seccionMinimaTierraMm2(double seccionFaseMm2);

    int seleccionarCalibreTermomagneticaA(double corrienteMinimaRequeridaA, double corrienteAdmisibleConductorA);

    int seleccionarCalibreDisyuntorA(double corrienteCorrienteAlimentacionA);

    int sensibilidadDiferencialMa(TipoCircuito tipo);
}
