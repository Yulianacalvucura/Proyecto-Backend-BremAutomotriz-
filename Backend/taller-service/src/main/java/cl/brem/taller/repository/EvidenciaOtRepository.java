package cl.brem.taller.repository;

import cl.brem.taller.model.EvidenciaOt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvidenciaOtRepository extends JpaRepository<EvidenciaOt, Long> {

	List<EvidenciaOt> findByOtIdOrderByIdAsc(Long otId);

}
