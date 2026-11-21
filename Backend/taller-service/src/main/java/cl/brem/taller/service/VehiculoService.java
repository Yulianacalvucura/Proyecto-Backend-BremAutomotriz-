package cl.brem.taller.service;

import cl.brem.taller.dto.VehiculoDetalleResponse;
import cl.brem.taller.dto.VehiculoRequest;
import cl.brem.taller.dto.VehiculoResponse;
import cl.brem.taller.exception.ResourceNotFoundException;
import cl.brem.taller.model.Cliente;
import cl.brem.taller.model.Vehiculo;
import cl.brem.taller.repository.ClienteRepository;
import cl.brem.taller.repository.VehiculoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehiculoService {

	private static final Pattern PATENTE = Pattern.compile("^[A-Z0-9]{6}$");

	private final VehiculoRepository vehiculoRepository;

	private final ClienteRepository clienteRepository;

	public List<VehiculoResponse> listar() {
		return vehiculoRepository.findAll().stream()
				.map(this::aResponse)
				.toList();
	}

	public VehiculoResponse buscarPorId(Long id) {
		return vehiculoRepository.findById(id)
				.map(this::aResponse)
				.orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado con id: " + id));
	}

	public List<VehiculoResponse> listarPorCliente(Long clienteId) {
		validarClienteExiste(clienteId);
		return vehiculoRepository.findByClienteId(clienteId).stream()
				.map(this::aResponse)
				.toList();
	}

	public VehiculoDetalleResponse buscarPorPatente(String patente) {
		String normalizada = normalizarPatente(patente);
		Vehiculo vehiculo = vehiculoRepository.findByPatente(normalizada)
				.orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado con patente: " + normalizada));
		Cliente cliente = clienteRepository.findById(vehiculo.getClienteId())
				.orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con id: " + vehiculo.getClienteId()));

		return VehiculoDetalleResponse.builder()
				.id(vehiculo.getId())
				.patente(vehiculo.getPatente())
				.marca(vehiculo.getMarca())
				.modelo(vehiculo.getModelo())
				.anio(vehiculo.getAnio())
				.vin(vehiculo.getVin())
				.tipo(vehiculo.getTipo())
				.clienteId(cliente.getId())
				.clienteNombre(cliente.getNombre())
				.clienteRun(cliente.getRun())
				.clienteTelefono(cliente.getTelefono())
				.clienteCorreo(cliente.getCorreo())
				.build();
	}

	@Transactional
	public VehiculoResponse crear(VehiculoRequest request) {
		String patente = normalizarPatente(request.getPatente());
		if (vehiculoRepository.existsByPatente(patente)) {
			throw new IllegalArgumentException("La patente ya está registrada: " + patente);
		}
		validarClienteExiste(request.getClienteId());

		Vehiculo vehiculo = Vehiculo.builder()
				.patente(patente)
				.marca(request.getMarca())
				.modelo(request.getModelo())
				.anio(request.getAnio())
				.vin(request.getVin())
				.tipo(request.getTipo())
				.clienteId(request.getClienteId())
				.build();
		return aResponse(vehiculoRepository.save(vehiculo));
	}

	@Transactional
	public VehiculoResponse actualizar(Long id, VehiculoRequest request) {
		Vehiculo vehiculo = vehiculoRepository.findById(id)
				.orElseThrow(() -> new ResourceNotFoundException("Vehículo no encontrado con id: " + id));

		String patente = normalizarPatente(request.getPatente());
		if (vehiculoRepository.existsByPatenteAndIdNot(patente, id)) {
			throw new IllegalArgumentException("La patente ya está registrada: " + patente);
		}
		validarClienteExiste(request.getClienteId());

		vehiculo.setPatente(patente);
		vehiculo.setMarca(request.getMarca());
		vehiculo.setModelo(request.getModelo());
		vehiculo.setAnio(request.getAnio());
		vehiculo.setVin(request.getVin());
		vehiculo.setTipo(request.getTipo());
		vehiculo.setClienteId(request.getClienteId());
		return aResponse(vehiculoRepository.save(vehiculo));
	}

	private void validarClienteExiste(Long clienteId) {
		if (!clienteRepository.existsById(clienteId)) {
			throw new ResourceNotFoundException("Cliente no encontrado con id: " + clienteId);
		}
	}

	private String normalizarPatente(String patente) {
		if (patente == null) {
			throw new IllegalArgumentException("La patente es obligatoria");
		}
		String normalizada = patente.trim().toUpperCase(Locale.ROOT);
		if (!PATENTE.matcher(normalizada).matches()) {
			throw new IllegalArgumentException("La patente debe tener 6 caracteres entre A-Z y 0-9");
		}
		return normalizada;
	}

	private VehiculoResponse aResponse(Vehiculo vehiculo) {
		return VehiculoResponse.builder()
				.id(vehiculo.getId())
				.patente(vehiculo.getPatente())
				.marca(vehiculo.getMarca())
				.modelo(vehiculo.getModelo())
				.anio(vehiculo.getAnio())
				.vin(vehiculo.getVin())
				.tipo(vehiculo.getTipo())
				.clienteId(vehiculo.getClienteId())
				.build();
	}

}
