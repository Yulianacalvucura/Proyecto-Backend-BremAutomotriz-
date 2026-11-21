package cl.brem.taller.service;

import cl.brem.taller.dto.ClienteRequest;
import cl.brem.taller.dto.ClienteResponse;
import cl.brem.taller.exception.ResourceNotFoundException;
import cl.brem.taller.model.Cliente;
import cl.brem.taller.repository.ClienteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ClienteService {

	private final ClienteRepository clienteRepository;

	public List<ClienteResponse> listar() {
		return clienteRepository.findAll().stream()
				.map(this::aResponse)
				.toList();
	}

	public ClienteResponse buscarPorId(Long id) {
		return clienteRepository.findById(id)
				.map(this::aResponse)
				.orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
	}

	@Transactional
	public ClienteResponse crear(ClienteRequest request) {
		String run = normalizarRun(request.getRun());
		if (run != null && clienteRepository.existsByRun(run)) {
			throw new IllegalArgumentException("El RUN ya está registrado: " + run);
		}

		Cliente cliente = Cliente.builder()
				.nombre(request.getNombre())
				.run(run)
				.telefono(request.getTelefono())
				.correo(request.getCorreo())
				.build();
		return aResponse(clienteRepository.save(cliente));
	}

	@Transactional
	public ClienteResponse actualizar(Long id, ClienteRequest request) {
		Cliente cliente = clienteRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));

		String run = normalizarRun(request.getRun());
		if (run != null && clienteRepository.existsByRunAndIdNot(run, id)) {
			throw new IllegalArgumentException("El RUN ya está registrado: " + run);
		}

		cliente.setNombre(request.getNombre());
		cliente.setRun(run);
		cliente.setTelefono(request.getTelefono());
		cliente.setCorreo(request.getCorreo());
		return aResponse(clienteRepository.save(cliente));
	}

	private String normalizarRun(String run) {
		if (run == null || run.isBlank()) {
			return null;
		}
		return run.trim();
	}

	private ClienteResponse aResponse(Cliente cliente) {
		return ClienteResponse.builder()
				.id(cliente.getId())
				.nombre(cliente.getNombre())
				.run(cliente.getRun())
				.telefono(cliente.getTelefono())
				.correo(cliente.getCorreo())
				.build();
	}

}
