package mx.gob.redoaxaca.tags


import grails.converters.JSON

class DatosController {
	def dataTablesService

	/**
	 * Generates JSON needed for a DataTables table.
	 */
	def dataTablesSource = {
		render dataTablesService.datosParaTabla(Producto.class, params, [
			'numero',
			'titulo',
			'precio',
			'link'
		]) as JSON
	}

	def tabla() {

	}
}
