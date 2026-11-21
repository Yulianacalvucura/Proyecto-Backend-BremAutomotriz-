package cl.brem.taller.repository;

import cl.brem.taller.model.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {

	List<HistorialEstado> findByOtIdOrderByIdAsc(Long otId);

}
