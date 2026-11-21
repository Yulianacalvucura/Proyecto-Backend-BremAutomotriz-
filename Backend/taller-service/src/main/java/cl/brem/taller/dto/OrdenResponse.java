package cl.brem.taller.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdenResponse {

	private Long id;

	private Long vehiculoId;

	private String patente;

	private String nombreCliente;

	private Long creadoPorId;

	private LocalDateTime fechaIngreso;

	private LocalDateTime fechaRegistro;

	private Integer kilometraje;

	private String observacionIngreso;

	private String observacionEntrega;

	private String estado;

	private Integer neto;

	private Integer iva;

	private BigDecimal porcentajeIva;

	private Integer total;

}
