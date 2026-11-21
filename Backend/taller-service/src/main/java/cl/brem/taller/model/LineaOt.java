package cl.brem.taller.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "lineas_ot")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LineaOt {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "ot_id", nullable = false)
	private Long otId;

	@Enumerated(EnumType.STRING)
	@Column(name = "tipo", nullable = false, length = 20)
	private TipoLinea tipo;

	@Column(name = "descripcion", nullable = false, length = 100)
	private String descripcion;

	@Column(name = "costo", nullable = false)
	private Integer costo;

	@Column(name = "factor", nullable = false, precision = 3, scale = 2)
	private BigDecimal factor;

	@Column(name = "cobrado", nullable = false)
	private Integer cobrado;

}
