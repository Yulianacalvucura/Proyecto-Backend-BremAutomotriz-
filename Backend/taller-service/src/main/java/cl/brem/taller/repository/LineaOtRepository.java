package cl.brem.taller.repository;

import cl.brem.taller.model.LineaOt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LineaOtRepository extends JpaRepository<LineaOt, Long> {

	List<LineaOt> findByOtIdOrderByIdAsc(Long otId);

}
