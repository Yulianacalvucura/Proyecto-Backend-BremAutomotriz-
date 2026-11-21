package cl.brem.taller.model;

/**
 * Estados posibles de una orden de trabajo. Coincide con el ENUM de MySQL.
 */
public enum EstadoOt {
	PENDIENTE,
	EN_PROCESO,
	LISTO,
	ENTREGADO,
	ANULADA
}
