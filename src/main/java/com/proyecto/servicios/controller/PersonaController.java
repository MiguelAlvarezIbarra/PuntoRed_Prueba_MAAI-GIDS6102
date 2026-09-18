package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.ListaPersonasResponse;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.service.PersonaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class PersonaController {


    @Autowired
    private PersonaService personaService;


    @PostMapping(value = "/personas",produces =MediaType.APPLICATION_JSON_VALUE,consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> crearUser(@Valid @RequestBody PersonasRequest personasRequest){

        return new ResponseEntity<>(personaService.creaPersona(personasRequest), HttpStatus.OK);
    }

    @PutMapping(value = "/personasActualiza", produces =MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> actualizUser(@Valid @RequestBody PersonasRequest personasRequest){

        return new ResponseEntity<>(personaService.actualizaPersona(personasRequest), HttpStatus.OK);
    }
    @PutMapping(value = "/personasElimina", produces =MediaType.APPLICATION_JSON_VALUE, consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<GenericResponse> actualizUser(@Valid @RequestBody EliminaPersonaRequest personasRequest){

        return new ResponseEntity<>(personaService.eliminaPersona(personasRequest), HttpStatus.OK);
    }

    @GetMapping(value = "/personas", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ListaPersonasResponse> obtenerPersonas(){

        return new ResponseEntity<>(personaService.obtenerPersonas(), HttpStatus.OK);
    }

    @GetMapping(value = "/personas/{codigo}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PersonaResponse> obtenerPersonaPorCodigo(@PathVariable Integer codigo){

        return new ResponseEntity<>(personaService.obtenerPersonaPorCodigo(codigo), HttpStatus.OK);
    }
}