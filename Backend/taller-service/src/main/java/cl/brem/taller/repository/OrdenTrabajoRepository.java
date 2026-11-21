package cl.brem.taller.repository;

import cl.brem.taller.model.OrdenTrabajo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrdenTrabajoRepository extends JpaRepository<OrdenTrabajo, Long> {

	List<OrdenTrabajo> findAllByOrderByIdDesc();

}
