package cl.brem.taller.service;

import cl.brem.taller.dto.ClienteRequest;
import cl.brem.taller.dto.ClienteResponse;
import cl.brem.taller.exception.ResourceNotFoundException;
import cl.brem.taller.model.Cliente;
import cl.brem.taller.repository.ClienteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

	@Mock
	private ClienteRepository clienteRepository;

	@InjectMocks
	private ClienteService clienteService;

	@Test
	void listarDevuelveTodosLosClientes() {
		when(clienteRepository.findAll()).thenReturn(List.of(
				Cliente.builder().id(1L).nombre("Cliente Uno").build(),
				Cliente.builder().id(2L).nombre("Cliente Dos").build()));

		List<ClienteResponse> resultado = clienteService.listar();

		assertEquals(2, resultado.size());
		assertEquals("Cliente Uno", resultado.get(0).getNombre());
		verify(clienteRepository).findAll();
	}

	@Test
	void buscarPorIdExistenteDevuelveElCliente() {
		when(clienteRepository.findById(1L)).thenReturn(Optional.of(Cliente.builder()
				.id(1L)
				.nombre("Cliente Ejemplo Uno")
				.run("11.111.111-1")
				.telefono("+56 9 1111 1111")
				.correo("cliente1@ejemplo.cl")
				.build()));

		ClienteResponse resultado = clienteService.buscarPorId(1L);

		assertEquals(1L, resultado.getId());
		assertEquals("11.111.111-1", resultado.getRun());
		verify(clienteRepository).findById(1L);
	}

	@Test
	void buscarPorIdInexistenteLanzaResourceNotFound() {
		when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class, () -> clienteService.buscarPorId(99L));
	}

	@Test
	void crearConRunNuevoGuardaElCliente() {
		when(clienteRepository.existsByRun("11.111.111-1")).thenReturn(false);
		when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
			Cliente guardado = inv.getArgument(0);
			guardado.setId(1L);
			return guardado;
		});

		ClienteResponse resultado = clienteService.crear(ClienteRequest.builder()
				.nombre("Cliente Nuevo")
				.run("11.111.111-1")
				.telefono("+56 9 1234 5678")
				.correo("nuevo@ejemplo.cl")
				.build());

		assertEquals(1L, resultado.getId());
		assertEquals("Cliente Nuevo", resultado.getNombre());
		verify(clienteRepository).save(any(Cliente.class));
	}

	@Test
	void crearConRunDuplicadoLanzaIllegalArgument() {
		when(clienteRepository.existsByRun("11.111.111-1")).thenReturn(true);

		IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
				() -> clienteService.crear(ClienteRequest.builder()
						.nombre("Cliente Duplicado")
						.run("11.111.111-1")
						.build()));

		assertEquals("El RUN ya está registrado: 11.111.111-1", ex.getMessage());
		verify(clienteRepository, never()).save(any(Cliente.class));
	}

	@Test
	void crearConRunEnBlancoLoGuardaComoNulo() {
		when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

		clienteService.crear(ClienteRequest.builder()
				.nombre("Cliente Sin RUN")
				.run("   ")
				.build());

		ArgumentCaptor<Cliente> captor = ArgumentCaptor.forClass(Cliente.class);
		verify(clienteRepository).save(captor.capture());
		assertNull(captor.getValue().getRun());
	}

	@Test
	void actualizarConRunPropioGuardaLosCambios() {
		Cliente existente = Cliente.builder()
				.id(1L)
				.nombre("Nombre Anterior")
				.run("11.111.111-1")
				.build();
		when(clienteRepository.findById(1L)).thenReturn(Optional.of(existente));
		when(clienteRepository.existsByRunAndIdNot("11.111.111-1", 1L)).thenReturn(false);
		when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

		ClienteResponse resultado = clienteService.actualizar(1L, ClienteRequest.builder()
				.nombre("Nombre Actualizado")
				.run("11.111.111-1")
				.build());

		assertEquals("Nombre Actualizado", resultado.getNombre());
		verify(clienteRepository).save(any(Cliente.class));
	}

	@Test
	void actualizarInexistenteLanzaResourceNotFound() {
		when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

		assertThrows(ResourceNotFoundException.class,
				() -> clienteService.actualizar(99L, ClienteRequest.builder().nombre("Cualquiera").build()));
		verify(clienteRepository, never()).save(any(Cliente.class));
	}

	@Test
	void actualizarConRunDeOtroClienteLanzaIllegalArgument() {
		when(clienteRepository.findById(1L)).thenReturn(Optional.of(Cliente.builder().id(1L).build()));
		when(clienteRepository.existsByRunAndIdNot("22.222.222-2", 1L)).thenReturn(true);

		assertThrows(IllegalArgumentException.class,
				() -> clienteService.actualizar(1L, ClienteRequest.builder()
						.nombre("Cliente")
						.run("22.222.222-2")
						.build()));
		verify(clienteRepository, never()).save(any(Cliente.class));
	}

}
