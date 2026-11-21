package cl.brem.taller.service;

import cl.brem.taller.dto.VehiculoDetalleResponse;
import cl.brem.taller.dto.VehiculoRequest;
import cl.brem.taller.dto.VehiculoResponse;
import cl.brem.taller.exception.ResourceNotFoundException;
import cl.brem.taller.model.Cliente;
import cl.brem.taller.model.Vehiculo;
import cl.brem.taller.repository.ClienteRepository;
import cl.brem.taller.repository.VehiculoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehiculoServiceTest {

	@Mock
	private VehiculoRepository vehiculoRepository;

	@Mock
	private ClienteRepository clienteRepository;

	@InjectMocks
	private VehiculoService vehiculoService;

	@Test
	void listarDevuelveTodosLosVehiculos() {
		when(vehiculoRepository.findAll()).thenReturn(List.of(
				Vehiculo.builder().id(1L).patente("ABCD12").clienteId(1L).build(),
				Vehiculo.builder().id(2L).patente("WXYZ34").clienteId(1L).build()));

		List<VehiculoResponse> resultado = vehiculoService.listar();

		assertEquals(2, resultado.size());
		assertEquals("ABCD12", resultado.get(0).getPatente());
		verify(vehiculoRepository).findAll();
	}

	@Test
	void buscarPorIdExistenteDevuelveElVehiculo() {
		when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(Vehiculo.builder()
				.id(1L)
				.patente("ABCD12")
				.marca("Toyota")
				.modelo("Yaris")
				.anio(2018)
				.tipo("Auto")
				.clienteId(1L)
				.build()));

		VehiculoResponse resultado = vehiculoService.buscarPorId(1L);

		assertEquals("Toyota", resultado.getMarca());
		assertEquals(2018, resultado.getAnio());
	}

	@Test
	void buscarPorIdInexistenteLanzaResourceNotFound() {
		when(vehiculoRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> vehiculoService.buscarPorId(99L));
	}

	@Test
	void listarPorClienteDevuelveSusVehiculos() {
		when(clienteRepository.existsById(1L)).thenReturn(true);
		when(vehiculoRepository.findByClienteId(1L)).thenReturn(List.of(
				Vehiculo.builder().id(1L).patente("ABCD12").clienteId(1L).build()));

		List<VehiculoResponse> resultado = vehiculoService.listarPorCliente(1L);

		assertEquals(1, resultado.size());
		assertEquals("ABCD12", resultado.get(0).getPatente());
		verify(vehiculoRepository).findByClienteId(1L);
	}

	@Test
	void listarPorClienteInexistenteLanzaResourceNotFound() {
		when(clienteRepository.existsById(99L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class, () -> vehiculoService.listarPorCliente(99L));
		verify(vehiculoRepository, never()).findByClienteId(any());
	}

	@Test
	void crearNormalizaLaPatenteYGuarda() {
		when(vehiculoRepository.existsByPatente("ABCD12")).thenReturn(false);
		when(clienteRepository.existsById(1L)).thenReturn(true);
		when(vehiculoRepository.save(any(Vehiculo.class))).thenAnswer(inv -> {
			Vehiculo guardado = inv.getArgument(0);
			guardado.setId(3L);
			return guardado;
		});

		VehiculoResponse resultado = vehiculoService.crear(request(" abcd12 ", 1L));

		ArgumentCaptor<Vehiculo> captor = ArgumentCaptor.forClass(Vehiculo.class);
		verify(vehiculoRepository).save(captor.capture());
		assertEquals("ABCD12", captor.getValue().getPatente());
		assertEquals("ABCD12", resultado.getPatente());
		assertEquals(3L, resultado.getId());
	}

	@Test
	void crearConPatenteInvalidaLanzaIllegalArgument() {
		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> vehiculoService.crear(request("ABC12", 1L)));

		assertEquals("La patente debe tener 6 caracteres entre A-Z y 0-9", ex.getMessage());
		verifyNoInteractions(vehiculoRepository, clienteRepository);
	}

	@Test
	void crearConPatenteDuplicadaLanzaIllegalArgument() {
		when(vehiculoRepository.existsByPatente("ABCD12")).thenReturn(true);

		assertThrows(IllegalArgumentException.class, () -> vehiculoService.crear(request("abcd12", 1L)));
		verify(vehiculoRepository, never()).save(any(Vehiculo.class));
	}

	@Test
	void crearConClienteInexistenteLanzaResourceNotFound() {
		when(vehiculoRepository.existsByPatente("ABCD12")).thenReturn(false);
		when(clienteRepository.existsById(99L)).thenReturn(false);

		assertThrows(ResourceNotFoundException.class, () -> vehiculoService.crear(request("ABCD12", 99L)));
		verify(vehiculoRepository, never()).save(any(Vehiculo.class));
	}

	@Test
	void actualizarNormalizaLaPatenteYGuardaLosCambios() {
		when(vehiculoRepository.findById(1L)).thenReturn(Optional.of(Vehiculo.builder()
				.id(1L)
				.patente("ABCD12")
				.clienteId(1L)
				.build()));
		when(vehiculoRepository.existsByPatenteAndIdNot("WXYZ34", 1L)).thenReturn(false);
		when(clienteRepository.existsById(2L)).thenReturn(true);
		when(vehiculoRepository.save(any(Vehiculo.class))).thenAnswer(inv -> inv.getArgument(0));

		VehiculoResponse resultado = vehiculoService.actualizar(1L, request(" wxyz34 ", 2L));

		assertEquals("WXYZ34", resultado.getPatente());
		assertEquals(2L, resultado.getClienteId());
	}

	@Test
	void actualizarInexistenteLanzaResourceNotFound() {
		when(vehiculoRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> vehiculoService.actualizar(99L, request("ABCD12", 1L)));
		verify(vehiculoRepository, never()).save(any(Vehiculo.class));
	}

	@Test
	void buscarPorPatenteNormalizaYDevuelveDatosDelCliente() {
		when(vehiculoRepository.findByPatente("ABCD12")).thenReturn(Optional.of(Vehiculo.builder()
				.id(1L)
				.patente("ABCD12")
				.marca("Toyota")
				.modelo("Yaris")
				.anio(2018)
				.clienteId(1L)
				.build()));
		when(clienteRepository.findById(1L)).thenReturn(Optional.of(Cliente.builder()
				.id(1L)
				.nombre("Cliente Ejemplo Uno")
				.run("11.111.111-1")
				.build()));

		VehiculoDetalleResponse resultado = vehiculoService.buscarPorPatente(" abcd12 ");

		assertEquals("ABCD12", resultado.getPatente());
		assertEquals("Toyota", resultado.getMarca());
		assertEquals("Cliente Ejemplo Uno", resultado.getClienteNombre());
		assertEquals("11.111.111-1", resultado.getClienteRun());
		verify(vehiculoRepository).findByPatente("ABCD12");
	}

	@Test
	void buscarPorPatenteInexistenteLanzaResourceNotFound() {
		when(vehiculoRepository.findByPatente("ABCD12")).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> vehiculoService.buscarPorPatente("abcd12"));
		verify(clienteRepository, never()).findById(any());
	}

	private VehiculoRequest request(String patente, Long clienteId) {
		return VehiculoRequest.builder()
				.patente(patente)
				.marca("Toyota")
				.modelo("Yaris")
				.anio(2018)
				.tipo("Auto")
				.clienteId(clienteId)
				.build();
	}

}
