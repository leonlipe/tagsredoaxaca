package mx.gob.redoaxaca.tags

import groovy.sql.Sql

/**
 * Servicio para utilizar datatables en proyectos, los diversos metodos que expone, permiten
 * utilizar varias formas para consultar la base de datos.
 * */


class DataTablesService {

    def sessionFactory

/**
 * Metodo bÃ¡sico para consultar la base de datos a partir de una clase previamente definida en el
 * dominio de datos.
 * @param clase El dominio sobre el que se va a realizar la consulta
 * @param params
 * @param propertiesToRender Las propiedades del dominio a mostrar en la tabla
 * @param propertiesToFilter Las propiedades o elementos del dominio de datos sobre los cuales se va a realizar la consulta en la BD.
 * @param order La propiedad del dominio a ordenar por.
 * @return Un mapa con las propiedades a mostrar en la tabla, es responsabilidad del controlador convertirlo a la salida apropiada, por ejemplo JSON.
 * */
    def datosParaTabla(Class clase, def params, def propertiesToRender, def propertiesToFilter, def order) {
        def sQueryParaDatos = "from ${clase.getName()} as p  "
        def sQueryParaCuentas = "select count(*) from ${clase.getName()} as p  "

        def filters = []
        propertiesToRender.each { prop -> filters << "p.${prop} like :filter" }
        def filter = filters.join(" OR ")

        def filtersSearch = []
        propertiesToFilter.each { prop -> filtersSearch << "cast (p.${prop} as string) like :filterSearch" }
        def filterSearch = filtersSearch.join(" OR ")


        def dataToRender = [:]
        dataToRender.sEcho = params.sEcho
        dataToRender.aaData = []

        dataToRender.iTotalRecords = clase.count
        dataToRender.iTotalDisplayRecords = dataToRender.iTotalRecords


        def query = new StringBuilder(sQueryParaDatos)
        def conWherePrincipal = false
        def countQuery = new StringBuilder(sQueryParaCuentas)
        if (params.sSearch) {
            query.append(" where (${filterSearch})")
            conWherePrincipal = true
            if (params.id) {
                query.append(" and ${params.namedid}=${params.id}")
            }
        } else if (params.id) {
            query.append(" where ${params.namedid}=${params.id}")
            countQuery.append(" where ${params.namedid}=${params.id}")
            conWherePrincipal = true
        }

        if (params.whereadicional) {
            query.append((conWherePrincipal ? " and " : " where ") + params.whereadicional)
            countQuery.append((conWherePrincipal ? " and " : " where ") + params.whereadicional)
        }

        def sortDir = params.sSortDir_0?.equalsIgnoreCase('asc') ? 'asc' : 'desc'
        def sortProperty = propertiesToRender[order as int]
        query.append(" order by p.${sortProperty} ${sortDir}")

        def datos = []
        if (params.sSearch) {
            // Revise the number of total display records after applying the filter

            conWherePrincipal = false
            if (params.sSearch) {
                countQuery.append(" where (${filterSearch})")
                conWherePrincipal = true
                if (params.id) {
                    countQuery.append(" and ${params.namedid}=${params.id}")
                }

            } else if (params.id) {
                countQuery.append(" where ${params.namedid}=${params.id}")
                conWherePrincipal = true
            }


            if (params.whereadicional) {
                countQuery.append((conWherePrincipal ? " and " : " where ") + params.whereadicional)
            }


            def result = clase.executeQuery(countQuery.toString(), [filterSearch: "%${params.sSearch.toUpperCase()}%"])
            if (result) {
                dataToRender.iTotalRecords = result[0]
                dataToRender.iTotalDisplayRecords = result[0]
            }
            log.error(query.toString())
            datos = clase.findAll(query.toString(),
                    [filterSearch: "%${params.sSearch.toUpperCase()}%"],
                    [max: params.iDisplayLength as int, offset: params.iDisplayStart as int])
        } else {

            def result = clase.executeQuery(countQuery.toString())
            if (result) {
                dataToRender.iTotalRecords = result[0]
                dataToRender.iTotalDisplayRecords = result[0]
            }
            log.error(query.toString())
            datos = clase.findAll(query.toString(),
                    [max: params.iDisplayLength as int, offset: params.iDisplayStart as int])
        }
        def fila = []
        datos?.each { prod ->
            fila = []

            propertiesToRender.each { prop ->
                fila.add(prod[prop].toString())
            }

            dataToRender.aaData << fila
        }
        return dataToRender
    }

/**
 * Metodo alternativo para consulta con un query para ello.
 * dominio de datos.
 * @param clase El dominio sobre el que se va a realizar la consulta
 * @param params
 * @param propertiesToRender Las propiedades del dominio a mostrar en la tabla
 * @param propertiesToFilter Las propiedades o elementos del dominio de datos sobre los cuales se va a realizar la consulta en la BD.
 * @param order La propiedad del dominio a ordenar por.
 * @param casttext El cast del texto en la base de datos, depende del manejador, por ejemplo as TEXT, etc.
 * @return Un mapa con las propiedades a mostrar en la tabla, es responsabilidad del controlador convertirlo a la salida apropiada, por ejemplo JSON.
 * */
    def datosParaTablaQuery(String consulta, def params, def propertiesToRender, def propertiesToFilter, def order, def casttext) {
        def sQueryParaDatos = "select  "
        def sQueryParaCuentas = " select  count(*) as contador  " + consulta

        if (params.fieldtofilter){
            return datosParaTablaQuerySingleFieldSearch(consulta,params,propertiesToRender, propertiesToFilter, order, casttext)
        }


        propertiesToRender.each { prop ->  sQueryParaDatos += "${prop}  ," }

        sQueryParaDatos = sQueryParaDatos[0..-3] + consulta

		params.sSearch =  DataTablesService.removeCaracteres(params.sSearch)
        def filtersSearch = []
        propertiesToFilter.each { prop -> filtersSearch << "upper(cast(${prop} as " + casttext + ")) like '%" + params.sSearch.toUpperCase() + "%'"}
        def filterSearch = filtersSearch.join(" OR ")


        def dataToRender = [:]
        dataToRender.sEcho = params.sEcho
        dataToRender.aaData = []

        def countQuery = new StringBuilder(sQueryParaCuentas)
        def query = new StringBuilder(sQueryParaDatos)
        def conWherePrincipal = false

        if (params.sSearch) {
            query.append(" where (${filterSearch})")
            countQuery.append(" where (${filterSearch})")
            conWherePrincipal = true
            if (params.id) {
                query.append(" and ${params.namedid}=${params.id}")
                countQuery.append(" and ${params.namedid}=${params.id}")
            }
        } else if (params.id) {
            query.append(" where ${params.namedid}=${params.id}")
            countQuery.append(" where ${params.namedid}=${params.id}")
            conWherePrincipal = true
        }

        if (params.whereadicional) {
            query.append((conWherePrincipal ? " and " : " where ") + params.whereadicional)
            countQuery.append((conWherePrincipal ? " and " : " where ") + params.whereadicional)
        }

        def sortDir = params.sSortDir_0?.equalsIgnoreCase('asc') ? 'asc' : 'desc'
        def sortProperty = propertiesToRender[order as int]
        //query.append(" order by ${sortProperty} ${sortDir}")

        def datos = []
        if (params.sSearch) {
            // Revise the number of total display records after applying the filter


            if (params.whereadicional) {
                countQuery.append((conWherePrincipal ? " and " : " where ") + params.whereadicional)
            }



            def sql = new Sql(sessionFactory.currentSession.connection())

            query.append(" order by ${sortProperty} ${sortDir}")
            if (Integer.parseInt(params.iDisplayLength) != -1) query.append("  LIMIT " + params.iDisplayLength + "  OFFSET  " + params.iDisplayStart);

            def result = sql.rows(countQuery.toString());
            if (result) {
                dataToRender.iTotalRecords = result.contador[0]
                dataToRender.iTotalDisplayRecords = result.contador[0]
            }


            datos = sql.rows(query.toString())


        } else {

            def sql = new Sql(sessionFactory.currentSession.connection())

            query.append(" order by ${sortProperty} ${sortDir}")
            if (Integer.parseInt(params.iDisplayLength) != -1) query.append("  LIMIT " + params.iDisplayLength + "  OFFSET  " + params.iDisplayStart);

            def result = sql.rows(countQuery.toString());
            if (result) {


                dataToRender.iTotalRecords = result.contador[0]
                dataToRender.iTotalDisplayRecords = result.contador[0]
            }

            datos = sql.rows(query.toString())

        }


        def fila = []
        datos?.each { prod ->
            fila = []

            propertiesToFilter.each { prop ->
                fila.add(prod[prop].toString())
            }

			
			dataToRender.aaData << fila
		}
        return dataToRender
    }

/**
 * Metodo alternativo para consulta con un query para ello, con opciÃ³n a un parametro mas que permite cambiar de nombre a los campos consultados
 * dominio de datos.
 * @param clase El dominio sobre el que se va a realizar la consulta
 * @param params
 * @param propertiesToRender Las propiedades del dominio a mostrar en la tabla
 * @param propertiesToFilter Las propiedades o elementos del dominio de datos sobre los cuales se va a realizar la consulta en la BD.
 * @param propertiesToRename
 * @param order La propiedad del dominio a ordenar por.
 * @param casttext El cast del texto en la base de datos, depende del manejador, por ejemplo as TEXT, etc.
 * @return Un mapa con las propiedades a mostrar en la tabla, es responsabilidad del controlador convertirlo a la salida apropiada, por ejemplo JSON.
 * */
    def datosParaTablaQuery(String consulta, def params, def propertiesToRender, def propertiesToFilter, def propertiesToRename, def order, def casttext) {
        def sQueryParaDatos = "select  "
        def sQueryParaCuentas = " select  count(*) as contador  " + consulta



        propertiesToRender.each { prop ->  sQueryParaDatos += "${prop}  ," }

        sQueryParaDatos = sQueryParaDatos[0..-3] + consulta

		params.sSearch =  DataTablesService.removeCaracteres(params.sSearch)
		params.sSearch  = params.sSearch.replaceAll(" ", "%");
		
		def filtersSearch = []
		propertiesToFilter.each { prop -> filtersSearch << "upper(cast(${prop} as " + casttext + ")) like '%"  +params.sSearch.toUpperCase()+"%'"}
		def filterSearch = filtersSearch.join(" OR ")

        
		
		def dataToRender = [:]
        dataToRender.sEcho = params.sEcho
        dataToRender.aaData = []
		
		 def countQuery = new StringBuilder(sQueryParaCuentas)
        def query = new StringBuilder(sQueryParaDatos)
        def conWherePrincipal = false

        if (params.sSearch) {
            query.append(" where (${filterSearch})")
            countQuery.append(" where (${filterSearch})")
            conWherePrincipal = true
            if (params.id) {
                query.append(" and ${params.namedid}=${params.id}")
                countQuery.append(" and ${params.namedid}=${params.id}")
            }
        } else if (params.id) {
            query.append(" where ${params.namedid}=${params.id}")
            countQuery.append(" where ${params.namedid}=${params.id}")
            conWherePrincipal = true
        }

        if (params.whereadicional) {
            query.append((conWherePrincipal ? " and " : " where ") + params.whereadicional)
            countQuery.append((conWherePrincipal ? " and " : " where ") + params.whereadicional)
        }

        def sortDir = params.sSortDir_0?.equalsIgnoreCase('asc') ? 'asc' : 'desc'
        def sortProperty = propertiesToRename[order as int]
        //query.append(" order by ${sortProperty} ${sortDir}")

        def datos = []
        if (params.sSearch) {
            // Revise the number of total display records after applying the filter


            if (params.whereadicional) {
                countQuery.append((conWherePrincipal ? " and " : " where ") + params.whereadicional)
            }



            def sql = new Sql(sessionFactory.currentSession.connection())

            query.append(" order by ${sortProperty} ${sortDir}  ")
            if (Integer.parseInt(params.iDisplayLength) != -1) query.append("  LIMIT " + params.iDisplayLength + "  OFFSET  " + params.iDisplayStart);

            def result = sql.rows(countQuery.toString());
            if (result) {
                dataToRender.iTotalRecords = result.contador[0]
                dataToRender.iTotalDisplayRecords = result.contador[0]
            }

            datos = sql.rows(query.toString())


        } else {

            def sql = new Sql(sessionFactory.currentSession.connection())

            query.append(" order by ${sortProperty} ${sortDir}  ")
            if (Integer.parseInt(params.iDisplayLength) != -1) query.append("  LIMIT " + params.iDisplayLength + "  OFFSET  " + params.iDisplayStart);

            def result = sql.rows(countQuery.toString());
            if (result) {

                dataToRender.iTotalRecords = result.contador[0]
                dataToRender.iTotalDisplayRecords = result.contador[0]
            }


            datos = sql.rows(query.toString())

        }


        def fila = []
        datos?.each { prod ->
            fila = []

            propertiesToRename.each { prop ->
                fila.add(prod[prop].toString())
            }

            dataToRender.aaData << fila
        }
        return dataToRender
    }

	
	
	
	
	def datosParaTablaQueryOtherBD(String consulta, def params, def propertiesToRender, def propertiesToFilter, def propertiesToRename, def order, def casttext,def ssFactoy) {
		def sQueryParaDatos = "select  "
		def sQueryParaCuentas = " select  count(*) as contador  " + consulta



		propertiesToRender.each { prop ->  sQueryParaDatos += "${prop}  ," }

		sQueryParaDatos = sQueryParaDatos[0..-3] + consulta

		params.sSearch =  DataTablesService.removeCaracteres(params.sSearch)
		params.sSearch  = params.sSearch.replaceAll(" ", "%");
		
		def filtersSearch = []
		propertiesToFilter.each { prop -> filtersSearch << "cast(${prop} as " + casttext + ") like '%"  +params.sSearch.toUpperCase()+"%'"}
		def filterSearch = filtersSearch.join(" OR ")

		
		
		def dataToRender = [:]
		dataToRender.sEcho = params.sEcho
		dataToRender.aaData = []
		
		 def countQuery = new StringBuilder(sQueryParaCuentas)
		def query = new StringBuilder(sQueryParaDatos)
		def conWherePrincipal = false

		if (params.sSearch) {
			query.append(" where (${filterSearch})")
			countQuery.append(" where (${filterSearch})")
			conWherePrincipal = true
			if (params.id) {
				query.append(" and ${params.namedid}=${params.id}")
				countQuery.append(" and ${params.namedid}=${params.id}")
			}
		} else if (params.id) {
			query.append(" where ${params.namedid}=${params.id}")
			countQuery.append(" where ${params.namedid}=${params.id}")
			conWherePrincipal = true
		}

		if (params.whereadicional) {
			query.append((conWherePrincipal ? " and " : " where ") + params.whereadicional)
			countQuery.append((conWherePrincipal ? " and " : " where ") + params.whereadicional)
		}

		def sortDir = params.sSortDir_0?.equalsIgnoreCase('asc') ? 'asc' : 'desc'
		def sortProperty = propertiesToRename[order as int]
		//query.append(" order by ${sortProperty} ${sortDir}")

		def datos = []
		if (params.sSearch) {
			// Revise the number of total display records after applying the filter


			if (params.whereadicional) {
				countQuery.append((conWherePrincipal ? " and " : " where ") + params.whereadicional)
			}



			def sql = ssFactoy

			query.append(" order by ${sortProperty} ${sortDir}  ")
			if (Integer.parseInt(params.iDisplayLength) != -1) query.append("  LIMIT " + params.iDisplayLength + "  OFFSET  " + params.iDisplayStart);

			def result = sql.rows(countQuery.toString());
			if (result) {
				dataToRender.iTotalRecords = result.contador[0]
				dataToRender.iTotalDisplayRecords = result.contador[0]
			}

			datos = sql.rows(query.toString())


		} else {

			def sql = ssFactoy

			query.append(" order by ${sortProperty} ${sortDir}  ")
			if (Integer.parseInt(params.iDisplayLength) != -1) query.append("  LIMIT " + params.iDisplayLength + "  OFFSET  " + params.iDisplayStart);

			def result = sql.rows(countQuery.toString());
			if (result) {

				dataToRender.iTotalRecords = result.contador[0]
				dataToRender.iTotalDisplayRecords = result.contador[0]
			}


			datos = sql.rows(query.toString())

		}


		def fila = []
		datos?.each { prod ->
			fila = []

			propertiesToRename.each { prop ->
				fila.add(prod[prop].toString())
			}

			dataToRender.aaData << fila
		}
		return dataToRender
	}

	
	
	
	
/**
 * Metodo alternativo para consulta con un query para ello, con opciÃ³n a un parametro mas que permite cambiar de nombre a los campos consultados ademas de poder agrupar columnas.
 * dominio de datos.
 * @param clase El dominio sobre el que se va a realizar la consulta
 * @param params
 * @param propertiesToRender Las propiedades del dominio a mostrar en la tabla
 * @param propertiesToFilter Las propiedades o elementos del dominio de datos sobre los cuales se va a realizar la consulta en la BD.
 * @param propertiesToRename
 * @param order La propiedad del dominio a ordenar por.
 * @param group Las columnas para agrupar.
 * @param casttext El cast del texto en la base de datos, depende del manejador, por ejemplo as TEXT, etc.
 * @return Un mapa con las propiedades a mostrar en la tabla, es responsabilidad del controlador convertirlo a la salida apropiada, por ejemplo JSON.
 * */
    def datosParaTablaQuery(String consulta, def params, def propertiesToRender, def propertiesToFilter, def propertiesToRename, def order, def group, def casttext) {
        def sQueryParaDatos = "select  "
        def sQueryParaCuentas = " select  count(*) as contador  " + consulta



        propertiesToRender.each { prop ->  sQueryParaDatos += "${prop}  ," }

        sQueryParaDatos = sQueryParaDatos[0..-3] + consulta

		params.sSearch =  DataTablesService.removeCaracteres(params.sSearch)
		
		params.sSearch  = params.sSearch.replaceAll(" ", "%");
        def filtersSearch = []
        propertiesToFilter.each { prop -> filtersSearch << "cast(${prop} as " + casttext + ") like '%" + params.sSearch.toUpperCase() + "%'"}
        def filterSearch = filtersSearch.join(" OR ")


        def dataToRender = [:]
        dataToRender.sEcho = params.sEcho
        dataToRender.aaData = []


        def countQuery = new StringBuilder(sQueryParaCuentas)
        def query = new StringBuilder(sQueryParaDatos)
        def conWherePrincipal = false

        if (params.sSearch) {
            query.append(" where (${filterSearch})")
            countQuery.append(" where (${filterSearch})")
            conWherePrincipal = true
            if (params.id) {
                query.append(" and ${params.namedid}=${params.id}")
                countQuery.append(" and ${params.namedid}=${params.id}")
            }
        } else if (params.id) {
            query.append(" where ${params.namedid}=${params.id}")
            countQuery.append(" where ${params.namedid}=${params.id}")
            conWherePrincipal = true
        }


        if (params.whereadicional) {
            query.append((conWherePrincipal ? " and " : " where ") + params.whereadicional)
            countQuery.append((conWherePrincipal ? " and " : " where ") + params.whereadicional)
        }


        query.append("  group by " + group + "  ")
        countQuery.append("  group by " + group + "  ")

        def sortDir = params.sSortDir_0?.equalsIgnoreCase('asc') ? 'asc' : 'desc'
        def sortProperty = propertiesToRename[order as int]
        //query.append(" order by ${sortProperty} ${sortDir}")

        def datos = []
        if (params.sSearch) {
            // Revise the number of total display records after applying the filter


            if (params.whereadicional) {
                countQuery.append((conWherePrincipal ? " and " : " where ") + params.whereadicional)
            }



            def sql = new Sql(sessionFactory.currentSession.connection())

            query.append(" order by ${sortProperty} ${sortDir}  ")
            if (Integer.parseInt(params.iDisplayLength) != -1) query.append("  LIMIT " + params.iDisplayLength + "  OFFSET  " + params.iDisplayStart);

            def result = sql.rows(countQuery.toString());
            if (result) {
                dataToRender.iTotalRecords = result.contador[0]
                dataToRender.iTotalDisplayRecords = result.contador[0]
            }

            datos = sql.rows(query.toString())


        } else {

            def sql = new Sql(sessionFactory.currentSession.connection())

            query.append(" order by ${sortProperty} ${sortDir}  ")
            if (Integer.parseInt(params.iDisplayLength) != -1) query.append("  LIMIT " + params.iDisplayLength + "  OFFSET  " + params.iDisplayStart);

            def result = sql.rows(countQuery.toString());
            if (result) {

                dataToRender.iTotalRecords = result.contador[0]
                dataToRender.iTotalDisplayRecords = result.contador[0]
            }


            datos = sql.rows(query.toString())

        }


        def fila = []
        datos?.each { prod ->
            fila = []

            propertiesToRename.each { prop ->
                fila.add(prod[prop].toString())
            }

            dataToRender.aaData << fila
        }
        return dataToRender
    }


    /**
     * Metodo alternativo para consulta con un query para ello, y utilizando una consulta de un solo campo. El dato debe de venir en los params del request y se debe de
     * llamar fieldtofilter
     * dominio de datos.
     * @param clase El dominio sobre el que se va a realizar la consulta
     * @param params
     * @param propertiesToRender Las propiedades del dominio a mostrar en la tabla
     * @param propertiesToFilter Las propiedades o elementos del dominio de datos sobre los cuales se va a realizar la consulta en la BD.
     * @param order La propiedad del dominio a ordenar por.
     * @param casttext El cast del texto en la base de datos, depende del manejador, por ejemplo as TEXT, etc.
     * @return Un mapa con las propiedades a mostrar en la tabla, es responsabilidad del controlador convertirlo a la salida apropiada, por ejemplo JSON.
     * */
	
    def datosParaTablaQuerySingleFieldSearch(String consulta, def params, def propertiesToRender, def propertiesToFilter,def order, def casttext) {
        def sQueryParaDatos ="select  "
        def sQueryParaCuentas =" select  count(*) as contador  "+consulta



        propertiesToRender.each { prop ->  sQueryParaDatos+="${prop}  ," }

        sQueryParaDatos= sQueryParaDatos[0..-3] + consulta

		params.sSearch =  DataTablesService.removeCaracteres(params.sSearch)
		
        def filterSearch = "upper(cast("+params.fieldtofilter+" as  " + casttext +")) like '%"+params.sSearch.toUpperCase().replace(" ","%")+"%'"
        println(filterSearch)
        println(params.fieldtofilter)


		 def dataToRender = [:]
        dataToRender.sEcho = params.sEcho
        dataToRender.aaData=[]

        def countQuery = new StringBuilder(sQueryParaCuentas)
        def query = new StringBuilder(sQueryParaDatos)
        def conWherePrincipal = false

        if ( params.sSearch ) {
            query.append(" where (${filterSearch})")
            countQuery.append(" where (${filterSearch})")
            conWherePrincipal = true
            if (params.id){
                query.append (" and ${params.namedid}=${params.id}")
                countQuery.append (" and ${params.namedid}=${params.id}")
            }
        }else  if (params.id){
            query.append (" where ${params.namedid}=${params.id}")
            countQuery.append (" where ${params.namedid}=${params.id}")
            conWherePrincipal = true
        }

        if (params.whereadicional){
            query.append ( (conWherePrincipal?" and ":" where ")+params.whereadicional)
            countQuery.append ( (conWherePrincipal?" and ":" where ")+params.whereadicional)
        }

        def sortDir = params.sSortDir_0?.equalsIgnoreCase('asc') ? 'asc' : 'desc'
        def sortProperty = propertiesToRender[order as int]
        //query.append(" order by ${sortProperty} ${sortDir}")

        def datos = []
        if ( params.sSearch ) {
            // Revise the number of total display records after applying the filter




            if (params.whereadicional){
                countQuery.append ((conWherePrincipal?" and ":" where ")+params.whereadicional)
            }



            def sql = new Sql(sessionFactory.currentSession.connection())

            query.append(" order by ${sortProperty} ${sortDir}")
            if(Integer.parseInt(params.iDisplayLength)!=-1)query.append( "  LIMIT "+  params.iDisplayLength +  "  OFFSET  "+ params.iDisplayStart) ;

            def result = sql.rows(countQuery.toString());
            if ( result ) {
                dataToRender.iTotalRecords =  result.contador[0]
                dataToRender.iTotalDisplayRecords = result.contador[0]
            }


            datos= sql.rows(query.toString())


        } else {

            def sql = new Sql(sessionFactory.currentSession.connection())

            query.append(" order by ${sortProperty} ${sortDir}")
            if(Integer.parseInt(params.iDisplayLength)!=-1)query.append( "  LIMIT "+  params.iDisplayLength +  "  OFFSET  "+ params.iDisplayStart) ;

            def result = sql.rows(countQuery.toString());
            if ( result ) {

                dataToRender.iTotalRecords =  result.contador[0]
                dataToRender.iTotalDisplayRecords = result.contador[0]
            }


            datos= sql.rows(query.toString())

        }


        def fila = []
        datos?.each { prod ->
            fila = []

            propertiesToFilter.each{ prop ->
                fila.add(prod[prop].toString())
            }

            dataToRender.aaData << fila
        }
        return dataToRender
    }
	
	
	
	def datosParaTablaQuerySingleFieldSearchOtherDB(String consulta, def params, def propertiesToRender, def propertiesToFilter,def order, def casttext , def ssFactory) {
		def sQueryParaDatos ="select  "
		def sQueryParaCuentas =" select  count(*) as contador  "+consulta



		propertiesToRender.each { prop ->  sQueryParaDatos+="${prop}  ," }

		sQueryParaDatos= sQueryParaDatos[0..-3] + consulta

		params.sSearch =  DataTablesService.removeCaracteres(params.sSearch)
		
		 def filterSearch = "cast("+params.fieldtofilter+" as  " + casttext +") like '%"+params.sSearch.toUpperCase().replace(" ","%")+"%'"
		println(filterSearch)
		println(params.fieldtofilter)


		 def dataToRender = [:]
		dataToRender.sEcho = params.sEcho
		dataToRender.aaData=[]

		def countQuery = new StringBuilder(sQueryParaCuentas)
		def query = new StringBuilder(sQueryParaDatos)
		def conWherePrincipal = false

		if ( params.sSearch ) {
			query.append(" where (${filterSearch})")
			countQuery.append(" where (${filterSearch})")
			conWherePrincipal = true
			if (params.id){
				query.append (" and ${params.namedid}=${params.id}")
				countQuery.append (" and ${params.namedid}=${params.id}")
			}
		}else  if (params.id){
			query.append (" where ${params.namedid}=${params.id}")
			countQuery.append (" where ${params.namedid}=${params.id}")
			conWherePrincipal = true
		}

		if (params.whereadicional){
			query.append ( (conWherePrincipal?" and ":" where ")+params.whereadicional)
			countQuery.append ( (conWherePrincipal?" and ":" where ")+params.whereadicional)
		}

		def sortDir = params.sSortDir_0?.equalsIgnoreCase('asc') ? 'asc' : 'desc'
		def sortProperty = propertiesToRender[order as int]
		//query.append(" order by ${sortProperty} ${sortDir}")

		def datos = []
		if ( params.sSearch ) {
			// Revise the number of total display records after applying the filter




			if (params.whereadicional){
				countQuery.append ((conWherePrincipal?" and ":" where ")+params.whereadicional)
			}



			def sql = ssFactory

			query.append(" order by ${sortProperty} ${sortDir}")
			if(Integer.parseInt(params.iDisplayLength)!=-1)query.append( "  LIMIT "+  params.iDisplayLength +  "  OFFSET  "+ params.iDisplayStart) ;

			def result = sql.rows(countQuery.toString());
			if ( result ) {
				dataToRender.iTotalRecords =  result.contador[0]
				dataToRender.iTotalDisplayRecords = result.contador[0]
			}


			datos= sql.rows(query.toString())


		} else {

			def sql = ssFactory

			query.append(" order by ${sortProperty} ${sortDir}")
			if(Integer.parseInt(params.iDisplayLength)!=-1)query.append( "  LIMIT "+  params.iDisplayLength +  "  OFFSET  "+ params.iDisplayStart) ;

			def result = sql.rows(countQuery.toString());
			if ( result ) {

				dataToRender.iTotalRecords =  result.contador[0]
				dataToRender.iTotalDisplayRecords = result.contador[0]
			}


			datos= sql.rows(query.toString())

		}


		def fila = []
		datos?.each { prod ->
			fila = []

			propertiesToFilter.each{ prop ->
				fila.add(prod[prop].toString())
			}

			dataToRender.aaData << fila
		}
		return dataToRender
	}
	
	
	/**
	 * Metodo alternativo para consulta con un query para ello, y utilizando una consulta de un solo campo. El dato debe de venir en los params del request y se debe de
	 * llamar fieldtofilter
	 * dominio de datos.
	 * @param clase El dominio sobre el que se va a realizar la consulta
	 * @param params
	 * @param propertiesToRender Las propiedades del dominio a mostrar en la tabla
	 * @param propertiesToRename El nuevo nombre de las propiedades del dominio a mostrar en la tabla
	 * @param propertiesToFilter Las propiedades o elementos del dominio de datos sobre los cuales se va a realizar la consulta en la BD.
	 * @param order La propiedad del dominio a ordenar por.
	 * @param casttext El cast del texto en la base de datos, depende del manejador, por ejemplo as TEXT, etc.
	 * @return Un mapa con las propiedades a mostrar en la tabla, es responsabilidad del controlador convertirlo a la salida apropiada, por ejemplo JSON.
	 * */
	def datosParaTablaQuerySingleFieldSearch(String consulta, def params, def propertiesToRender, def propertiesToFilter,def propertiesToRename,def order, def casttext) {
		def sQueryParaDatos ="select  "
		def sQueryParaCuentas =" select  count(*) as contador  "+consulta

		propertiesToRender.each { prop ->  sQueryParaDatos+="${prop}  ," }

		sQueryParaDatos= sQueryParaDatos[0..-3] + consulta
		println params.enviaparametroextra + "nanananananan"

		params.sSearch =  DataTablesService.removeCaracteres(params.sSearch)
		
		def filterSearch = "upper(cast("+params.fieldtofilter+" as  " + casttext +")) like '%"+params.sSearch.toUpperCase().replace(" ","%")+"%'"
		
		def dataToRender = [:]
		dataToRender.sEcho = params.sEcho
		dataToRender.aaData=[]

		def countQuery = new StringBuilder(sQueryParaCuentas)
		def query = new StringBuilder(sQueryParaDatos)
		def conWherePrincipal = false

		if ( params.sSearch ) {
			query.append(" where (${filterSearch})")
			countQuery.append(" where (${filterSearch})")
			conWherePrincipal = true
			if (params.id){
				query.append (" and ${params.namedid}=${params.id}")
				countQuery.append (" and ${params.namedid}=${params.id}")
			}
		}else  if (params.id){
			query.append (" where ${params.namedid}=${params.id}")
			countQuery.append (" where ${params.namedid}=${params.id}")
			conWherePrincipal = true
		}
		
		if (params.whereadicional){
			query.append ( (conWherePrincipal?" and ":" where ")+params.whereadicional)
			countQuery.append ( (conWherePrincipal?" and ":" where ")+params.whereadicional)
		}

		def sortDir = params.sSortDir_0?.equalsIgnoreCase('asc') ? 'asc' : 'desc'
		def sortProperty = propertiesToRename[order as int]
		//query.append(" order by ${sortProperty} ${sortDir}")

		def datos = []
		if ( params.sSearch ) {
			// Revise the number of total display records after applying the filter




			if (params.whereadicional){
				countQuery.append ((conWherePrincipal?" and ":" where ")+params.whereadicional)
			}



			def sql = new Sql(sessionFactory.currentSession.connection())

			query.append(" order by ${sortProperty} ${sortDir}")
			if(Integer.parseInt(params.iDisplayLength)!=-1)query.append( "  LIMIT "+  params.iDisplayLength +  "  OFFSET  "+ params.iDisplayStart) ;

			def result = sql.rows(countQuery.toString());
			if ( result ) {
				dataToRender.iTotalRecords =  result.contador[0]
				dataToRender.iTotalDisplayRecords = result.contador[0]
			}


			datos= sql.rows(query.toString())


		} else {

			def sql = new Sql(sessionFactory.currentSession.connection())

			query.append(" order by ${sortProperty} ${sortDir}")
			if(Integer.parseInt(params.iDisplayLength)!=-1)query.append( "  LIMIT "+  params.iDisplayLength +  "  OFFSET  "+ params.iDisplayStart) ;

			def result = sql.rows(countQuery.toString());
			if ( result ) {

				dataToRender.iTotalRecords =  result.contador[0]
				dataToRender.iTotalDisplayRecords = result.contador[0]
			}


			datos= sql.rows(query.toString())

		}


		def fila = []
		datos?.each { prod ->
			fila = []

			propertiesToRename.each{ prop ->
				fila.add(prod[prop].toString())
			}

			dataToRender.aaData << fila
		}
		return dataToRender
	}
	
	
	
	def datosParaTablaQueryOracle(String consulta, def params, def propertiesToRender, def propertiesToFilter, def propertiesToRename, def order, def casttext) {
		def sQueryParaDatos = "select  "
		def sQueryParaCuentas = " select  count(*) as contador  " + consulta



		propertiesToRender.each { prop ->  sQueryParaDatos += "${prop}  ," }

		sQueryParaDatos = sQueryParaDatos[0..-3] + consulta

		params.sSearch =  DataTablesService.removeCaracteres(params.sSearch)
		params.sSearch  = params.sSearch.replaceAll(" ", "%");
		
		def filtersSearch = []
		propertiesToFilter.each { prop -> filtersSearch << "cast(${prop} as " + casttext + ") like '%"  +params.sSearch.toUpperCase()+"%'"}
		def filterSearch = filtersSearch.join(" OR ")

		
		
		def dataToRender = [:]
		dataToRender.sEcho = params.sEcho
		dataToRender.aaData = []
		
		 def countQuery = new StringBuilder(sQueryParaCuentas)
		def query = new StringBuilder(sQueryParaDatos)
		def conWherePrincipal = false

		if (params.sSearch) {
			query.append(" where (${filterSearch})")
			countQuery.append(" where (${filterSearch})")
			conWherePrincipal = true
			if (params.id) {
				query.append(" and ${params.namedid}=${params.id}")
				countQuery.append(" and ${params.namedid}=${params.id}")
			}
		} else if (params.id) {
			query.append(" where ${params.namedid}=${params.id}")
			countQuery.append(" where ${params.namedid}=${params.id}")
			conWherePrincipal = true
		}

		if (params.whereadicional) {
			query.append((conWherePrincipal ? " and " : " WHERE rownum < ") + params.iDisplayLength + "  AND  "+ params.whereadicional)
			countQuery.append((conWherePrincipal ? " and " : " WHERE rownum < ") + params.iDisplayLength  + "  AND  "+ params.whereadicional)
		}

		def sortDir = params.sSortDir_0?.equalsIgnoreCase('asc') ? 'asc' : 'desc'
		def sortProperty = propertiesToRename[order as int]
		//query.append(" order by ${sortProperty} ${sortDir}")

		def datos = []
		if (params.sSearch) {
			// Revise the number of total display records after applying the filter


			if (params.whereadicional) {
				countQuery.append((conWherePrincipal ? " and ":" WHERE rownum < ")+params.iDisplayLength + "  AND  "+ params.whereadicional)
			}



			def sql = new Sql(sessionFactory.currentSession.connection())

			query.append(" order by ${sortProperty} ${sortDir}  ")
			//if (Integer.parseInt(params.iDisplayLength) != -1) query.append("  LIMIT " + params.iDisplayLength + "  OFFSET  " + params.iDisplayStart);
			println  "Consulta count : "+ countQuery.toString()
			def result = sql.rows(countQuery.toString());
			if (result) {
				dataToRender.iTotalRecords = result.contador[0]
				dataToRender.iTotalDisplayRecords = result.contador[0]
			}
			println  "Consulta "+ query.toString()
			datos = sql.rows(query.toString())


		} else {

			def sql = new Sql(sessionFactory.currentSession.connection())

			query.append(" order by ${sortProperty} ${sortDir}  ")
			//if (Integer.parseInt(params.iDisplayLength) != -1) query.append("  LIMIT " + params.iDisplayLength + "  OFFSET  " + params.iDisplayStart);

			def result = sql.rows(countQuery.toString());
			if (result) {

				dataToRender.iTotalRecords = result.contador[0]
				dataToRender.iTotalDisplayRecords = result.contador[0]
			}

			println  "Consulta"+ query.toString()
			datos = sql.rows(query.toString())

		}


		def fila = []
		datos?.each { prod ->
			fila = []

			propertiesToRename.each { prop ->
				fila.add(prod[prop].toString())
			}

			dataToRender.aaData << fila
		}
		return dataToRender
	}
	
	
	
	
	def static String removeCaracteres(String input) {
		
		String original = "‡ˆŠŽ‘’“•—˜šœu–çË€ƒéèêíìîñ…òô†„‚";
		String ascii = "aaaeeeiiiooouuunAAAEEEIIIOOOUUUNcC";
		String output = input;
		for (int i=0; i<original.length(); i++) {
		
			output = output.replace(original.charAt(i), ascii.charAt(i));
		}
		return output;
	}	
	
}
