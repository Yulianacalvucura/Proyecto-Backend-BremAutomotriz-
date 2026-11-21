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
import java.time.LocalDateTime;

@Entity
@Table(name = "ordenes_trabajo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenTrabajo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id")
	private Long id;

	@Column(name = "vehiculo_id", nullable = false)
	private Long vehiculoId;

	@Column(name = "creado_por", nullable = false)
	private Long creadoPorId;

	@Column(name = "fecha_ingreso", nullable = false)
	private LocalDateTime fechaIngreso;

	@Column(name = "fecha_registro", nullable = false)
	private LocalDateTime fechaRegistro;

	@Column(name = "kilometraje", nullable = false)
	private Integer kilometraje;

	@Column(name = "observacion_ingreso", nullable = false, length = 500)
	private String observacionIngreso;

	@Column(name = "observacion_entrega", length = 500)
	private String observacionEntrega;

	@Enumerated(EnumType.STRING)
	@Column(name = "estado", nullable = false, length = 20)
	private EstadoOt estado;

	@Column(name = "neto", nullable = false)
	private Integer neto;

	@Column(name = "iva", nullable = false)
	private Integer iva;

	@Column(name = "porcentaje_iva", nullable = false, precision = 5, scale = 2)
	private BigDecimal porcentajeIva;

	@Column(name = "total", nullable = false)
	private Integer total;

}
