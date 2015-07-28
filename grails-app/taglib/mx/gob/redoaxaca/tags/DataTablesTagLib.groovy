package mx.gob.redoaxaca.tags;

class DataTablesTagLib {

	/**
	 * Muestra una tabla para usar con datatables
	 * ATTRS:
	 * ctrlid: Id de la tabla
	 * clase: Class de la tabla
	 * 
	 */
	def datatablehelper = { attrs, body ->
	def columnas = attrs.cols as List
		def anchos = attrs.anchos as List
		def tabla = ""


		tabla ="<table "+ (attrs.class?"class='"+attrs.class+"'":"") +" cellpadding='0' cellspacing='0' border='0' id='${attrs.ctrlid}' style=' width:${attrs.width} ;' >"
		tabla +="<thead> <tr>"
		// LOOP
		if(anchos){
		int i=0 
			columnas.each {  tabla +="<th width='"+anchos[i]+"%' >${it}</th>"
			i++
			 }
		
		}else{
			columnas.each {  tabla +="<th >${it}</th>"
	
			 }
		
		}

		tabla += "</tr> </thead> <tbody></tbody>"
		tabla +="</table>"

		out << body() << tabla
	}

    /**
     Implementa el codigo de javascript necesario para un datatables
     * ATTRS:
     * ctrlid: Id de la tabla
     * clase: Class de la tabla
     * fnServerParams: Funcion para agregar atributos extra, solo es el nombre, la definicion debe de ir de la
     *                 siguiente manera: function test ( aoData ) {aoData.push( { "name": "more_data", "value": "my_value" } );
     }

     **/
    def datatablehelperjs ={attrs, body ->


        if (attrs.context == "/"){
            attrs.context = ''
        }

        if (!attrs.namedid){
            attrs.namedid = "id"
        }

        def prevParams = false
        if (attrs.id){
            attrs.action = attrs.action + "/"+attrs.id+"?namedid="+attrs.namedid
            prevParams = true
        }

        if (attrs.whereadicional){

            attrs.action = attrs.action +(prevParams?"&":"?") +"whereadicional="+attrs.whereadicional
        }

        def js = "<script type='text/javascript'>"
        js += "var oTable;"
        js += "\$(document).ready(function() {oTable = \$('#${attrs.ctrlid}').dataTable({"
        //js += "sScrollY: '70%',"

        js += "bProcessing: true,"
        js += "bServerSide: true,"
        js += "sAjaxSource: '${attrs.context}/${attrs.controller}/${attrs.action}' ,"
        js += "bJQueryUI: ${attrs.jqueryui},"
        if (attrs.bootstrap) {
            js += "sDom: \"<'row-fluid'<'span4'l><'span8'f>r>t<'row-fluid'<'span4'i><'span8'p>>\","
            js += "sPaginationType: 'bootstrap',"
        }else {
        js += "sPaginationType: 'full_numbers',"
        }

        if (attrs.lang){
            js += "oLanguage : {"

            js += "sUrl : '${attrs.lang}'},"
        }

        if (attrs.aoColumns){
            def columnas = attrs.aoColumns as List
            def first = true
            js += "aoColumns : ["
            columnas.each { col ->
                if (!first){
                    js += ","
                }else {
                    first = false
                }
                js += col
            }
            js += "],"
        }

        if (attrs.aoColumnsDef){
            def columnas = attrs.aoColumnsDef as List
            def first = true
            js += "aoColumnsDef : ["
            columnas.each { col ->
                if (!first){
                    js += ","
                }else {
                    first = false
                }
                js += col
            }
            js += "],"
        }


        js += "aLengthMenu: [[10, 20, 30, 40,50, -1], [10, 20, 30, 40, 50, 'Todos']],"
        js += "iDisplayLength: 10, "

        if (attrs.fnServerParams){
            js += "\"fnServerParams\": "+attrs.fnServerParams;
        }
        js += "}); "  // Funcion del datatables
        if (attrs.delay){

          js += "setTimeout( function(){oTable.fnSetFilteringDelay("+attrs.delay+"); }, 500 );"

        }

        js += "});"   // Document ready




        //js += "\$('#example tbody').dblclick(function(event) {"
        //js += "	var anSelected = fnGetSelected(oTable);"
        //js += "	var aData = oTable.fnGetData(anSelected[0]);"
        //js += "	alert('Doble click');"
        //js += "	});"


        //js += "	function fnGetSelected(oTableLocal) {"
        //js += "	var aReturn = new Array();"
        //js += "	var aTrs = oTableLocal.fnGetNodes();"

        //js += "	for ( var i = 0; i < aTrs.length; i++) {"
        //js += "	if (\$(aTrs[i]).hasClass('row_selected')) {"
        //js += "	aReturn.push(aTrs[i]);"
        //js += "	}	}"
        //js += "	return aReturn;"
        //js += "	} "



        js += "</script>"


        out << body() << js

    }

	def optGroup = {attrs ->
		Map dataMap = attrs['dataMap']
		out << g.render(template: 'layouts/optSelect', model: [dataMap:dataMap])
	}
}
