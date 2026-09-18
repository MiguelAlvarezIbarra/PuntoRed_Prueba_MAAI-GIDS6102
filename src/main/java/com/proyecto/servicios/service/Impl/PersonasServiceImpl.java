package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.sf.Personas;
import com.proyecto.servicios.model.EliminaPersonaRequest;
import com.proyecto.servicios.model.GenericResponse;
import com.proyecto.servicios.model.ListaPersonasResponse;
import com.proyecto.servicios.model.PersonaData;
import com.proyecto.servicios.model.PersonaResponse;
import com.proyecto.servicios.model.PersonasRequest;
import com.proyecto.servicios.repositorys.sf.PersonasRepository;
import com.proyecto.servicios.service.PersonaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class PersonasServiceImpl implements PersonaService {
    @Autowired
    private PersonasRepository personasRepository;

    @Override
    public PersonaResponse creaPersona(PersonasRequest personasRequest) {
        Personas persona = new Personas();
        persona.setNombre(personasRequest.getNombre());
        persona.setApellidoMaterno(personasRequest.getApellidoMaterno());
        persona.setApellidoP(personasRequest.getApellidoP());
        personasRepository.save(persona);

        PersonaResponse person = new PersonaResponse();
        BeanUtils.copyProperties(persona, person);
        person.setCodigo(0);
        person.setMensaje("Exito");

        return person;
    }

    @Override
    public GenericResponse eliminaPersona(EliminaPersonaRequest eliminaPersonaRequest) {
        GenericResponse genericResponse = new GenericResponse();

        Optional<Personas> existePersona = personasRepository.findById(eliminaPersonaRequest.getCodigo());
        if (existePersona.isPresent()) {
            Personas personaElimina = existePersona.get();
            personasRepository.delete(personaElimina);
            genericResponse.setCodigo(0);
            genericResponse.setMensaje("La persona ha sido eliminada correctamente");

        } else {
            genericResponse.setCodigo(1);
            genericResponse.setMensaje("La persona no existe ");
        }
        return genericResponse;

    }

    @Override
    public GenericResponse actualizaPersona(PersonasRequest personasRequest) {
        GenericResponse genericResponse = new GenericResponse();
        Optional<Personas> existePersona = personasRepository.findById(personasRequest.getCodigo());
        if (existePersona.isPresent()) {
            Personas personaActualiza = existePersona.get();

            if (personasRequest.getNombre() != null && !personasRequest.getNombre().isBlank()) {
                personaActualiza.setNombre(personasRequest.getNombre());
            }
            if (personasRequest.getApellidoP() != null && !personasRequest.getApellidoP().isBlank()) {
                personaActualiza.setApellidoP(personasRequest.getApellidoP());
            }
            if (personasRequest.getApellidoMaterno() != null && !personasRequest.getApellidoMaterno().isBlank()) {
                personaActualiza.setApellidoMaterno(personasRequest.getApellidoMaterno());
            }

            personasRepository.save(personaActualiza);
            genericResponse.setCodigo(0);
            genericResponse.setMensaje("la persona ha sido actualizada correctamente");

        } else {
            genericResponse.setCodigo(1);
            genericResponse.setMensaje("La persona no existe ");
        }
        return genericResponse;
    }

    @Override
    public ListaPersonasResponse obtenerPersonas() {
        ListaPersonasResponse response = new ListaPersonasResponse();

        List<PersonaData> personas = personasRepository.findAll().stream()
                .map(this::toPersonaData)
                .collect(Collectors.toList());

        response.setPersonas(personas);
        response.setCodigo(0);
        response.setMensaje("Exito");
        return response;
    }

    @Override
    public PersonaResponse obtenerPersonaPorCodigo(Integer codigo) {
        Optional<Personas> existePersona = personasRepository.findById(codigo);

        PersonaResponse response = new PersonaResponse();
        if (existePersona.isPresent()) {
            BeanUtils.copyProperties(existePersona.get(), response);
            response.setCodigo(0);
            response.setMensaje("Exito");
        } else {
            response.setCodigo(1);
            response.setMensaje("La persona no existe ");
        }
        return response;
    }

    private PersonaData toPersonaData(Personas persona) {
        PersonaData data = new PersonaData();
        BeanUtils.copyProperties(persona, data);
        return data;
    }
}