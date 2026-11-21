package cl.brem.taller.repository;

import cl.brem.taller.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {

	boolean existsByRun(String run);

	boolean existsByRunAndIdNot(String run, Long id);

	Optional<Cliente> findByRun(String run);

}
