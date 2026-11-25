package ar.gob.cba.instalaciones.calculo.controller;

import ar.gob.cba.instalaciones.calculo.dto.SolicitudCalculoDto;
import ar.gob.cba.instalaciones.calculo.service.InstalacionCalculoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/instalaciones")
public class CalculoController {

    private final InstalacionCalculoService calculoService;

    public CalculoController(InstalacionCalculoService calculoService) {
        this.calculoService = calculoService;
    }

    @PostMapping("/calcular")
    public ResponseEntity<SolicitudCalculoDto.ResultadoCalculoDto> calcular(@RequestBody SolicitudCalculoDto solicitud) {
        if (solicitud == null || solicitud.ambientes().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(calculoService.calcular(solicitud));
    }
}