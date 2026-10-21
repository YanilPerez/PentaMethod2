package com.example.pentametod2.ui.formularios

import com.example.pentametod2.catalogo.*
import com.example.pentametod2.model.MetodoId

data class EstadoLagrange(
    val puntos: List<PuntoEntradaLagrange> = listOf(PuntoEntradaLagrange(0), PuntoEntradaLagrange(1)),
    val x: String = "",
    val grados: List<Int> = listOf(1),
    val modo: ModoNodos = ModoNodos.AUTOMATICO,
    val selecciones: Map<Int, List<Int>> = emptyMap(),
    val funcionHabilitada: Boolean = false,
    val funcion: String = "",
    val vista: VistaDatos = VistaDatos.LISTA,
    val textoListaX: String? = null,
    val textoListaY: String? = null,
    val gradoPredeterminado: Boolean = puntos.all { it.x.isBlank() && it.y.isBlank() },
) : EstadoFormulario {
    override fun aEntrada() = EntradaLagrange(puntos, x, grados, modo, selecciones, funcion.takeIf { funcionHabilitada })
    fun validacion(): String? = DatosLagrange.validar(aEntrada())
    fun maximoGrado(): Int = minOf(DatosLagrange.MAX_GRADO,
        puntos.filter { DatosLagrange.numeroCelda(it.x) != null && DatosLagrange.numeroCelda(it.y) != null }
            .map { DatosLagrange.numeroCelda(it.x) }.distinct().size - 1).coerceAtLeast(0)

    fun conCelda(id: Int, columna: Columna, valor: String) = copy(puntos = puntos.map {
        if (it.id != id) it else if (columna == Columna.X) it.copy(x = valor) else it.copy(y = valor)
    }, textoListaX = null, textoListaY = null).conGradoPredeterminado()

    /** Sigue el máximo disponible hasta que el usuario elige sus propios grados o puntos. */
    fun conGradoPredeterminado(): EstadoLagrange {
        if (!gradoPredeterminado || modo != ModoNodos.AUTOMATICO) return this
        return copy(grados = listOf(maximoGrado().coerceAtLeast(1)))
    }

    fun textoLista(columna: Columna): String {
        val crudo = if (columna == Columna.X) textoListaX else textoListaY
        if (crudo != null) return crudo
        val filas = DatosLagrange.filasConDatos(puntos)
        val valores = filas.map { if (columna == Columna.X) it.x else it.y }
        return if (valores.all { it.isBlank() }) "" else valores.joinToString(", ")
    }

    /** Editar una lista conserva la identidad y la posición de las parejas existentes. */
    fun conLista(columna: Columna, texto: String): EstadoLagrange {
        val valores = leerListaLagrange(texto)
        val otros = puntos.map { if (columna == Columna.X) it.y else it.x }.dropLastWhile { it.isBlank() }
        val xs = if (columna == Columna.X) valores else otros
        val ys = if (columna == Columna.Y) valores else otros
        val cantidad = maxOf(xs.size, ys.size, 2)
        var siguiente = (puntos.maxOfOrNull { it.id } ?: -1) + 1
        val nuevos = List(cantidad) { i ->
            PuntoEntradaLagrange(puntos.getOrNull(i)?.id ?: siguiente++, xs.getOrElse(i) { "" }, ys.getOrElse(i) { "" })
        }
        val ids = nuevos.map { it.id }.toSet()
        return copy(puntos = nuevos, selecciones = selecciones.mapValues { (_, elegidos) -> elegidos.filter { it in ids } },
            textoListaX = if (columna == Columna.X) texto else textoListaX,
            textoListaY = if (columna == Columna.Y) texto else textoListaY).conGradoPredeterminado()
    }

    /** Cambiar de vista conserva pares, huecos y selecciones; normaliza la coma decimal en Lista. */
    fun conVista(nueva: VistaDatos): EstadoLagrange {
        if (nueva == vista) return this
        val nuevos = if (nueva == VistaDatos.LISTA) puntos.map { p ->
            fun paraLista(s: String) = if (DatosLagrange.numeroCelda(s) != null) s.trim().replace(',', '.') else s
            p.copy(x = paraLista(p.x), y = paraLista(p.y))
        } else puntos
        return copy(vista = nueva, puntos = nuevos, textoListaX = null, textoListaY = null)
    }

    fun agregarPunto(): EstadoLagrange = if (puntos.size >= DatosLagrange.MAX_PUNTOS) this else
        copy(puntos = puntos + PuntoEntradaLagrange((puntos.maxOfOrNull { it.id } ?: -1) + 1), textoListaX = null, textoListaY = null)

    fun quitarPunto(id: Int): EstadoLagrange {
        if (puntos.size <= 1) return this
        return copy(puntos = puntos.filterNot { it.id == id },
            selecciones = selecciones.mapValues { (_, ids) -> ids.filterNot { it == id } }, textoListaX = null, textoListaY = null)
            .conGradoPredeterminado()
    }

    fun conModo(nuevo: ModoNodos): EstadoLagrange = copy(modo = nuevo,
        gradoPredeterminado = if (nuevo == ModoNodos.MANUAL) false else gradoPredeterminado,
        selecciones = if (nuevo == ModoNodos.MANUAL) selecciones + grados.filterNot { it in selecciones }
            .associateWith { DatosLagrange.sugeridos(puntos, x, it) } else selecciones)

    fun alternarGrado(g: Int): EstadoLagrange {
        if (g in grados) return copy(grados = grados - g, gradoPredeterminado = false)
        if (g !in 1..maximoGrado()) return this
        return copy(grados = (grados + g).sorted(), gradoPredeterminado = false, selecciones = if (modo == ModoNodos.MANUAL && g !in selecciones)
            selecciones + (g to DatosLagrange.sugeridos(puntos, x, g)) else selecciones)
    }

    fun hastaGradoTres(): EstadoLagrange = (1..minOf(3, maximoGrado())).fold(copy(grados = emptyList())) { e, g -> e.alternarGrado(g) }
    fun seleccion(g: Int): List<Int> = if (modo == ModoNodos.AUTOMATICO) DatosLagrange.sugeridos(puntos, x, g) else selecciones[g].orEmpty()
    fun sugerir(g: Int) = copy(selecciones = selecciones + (g to DatosLagrange.sugeridos(puntos, x, g)))

    fun alternarPunto(g: Int, id: Int): EstadoLagrange {
        val actuales = selecciones[g].orEmpty()
        if (modo != ModoNodos.MANUAL || g !in grados || puntos.none { it.id == id }) return this
        val nuevos = if (id in actuales) actuales - id else if (actuales.size < g + 1) actuales + id else actuales
        return copy(selecciones = selecciones + (g to nuevos))
    }

    /** Reemplazar datos no conserva asignaciones a puntos diferentes. Los grados siguen visibles para revisarlos. */
    fun reemplazar(xs: List<String>, ys: List<String>): EstadoLagrange {
        val cantidad = maxOf(xs.size, ys.size, 2)
        return copy(puntos = List(cantidad) { PuntoEntradaLagrange(it, xs.getOrElse(it) { "" }, ys.getOrElse(it) { "" }) },
            selecciones = grados.associateWith { emptyList() }, textoListaX = null, textoListaY = null).conGradoPredeterminado()
    }

    fun guardar(): List<String> = listOf("L3", x, modo.name, grados.joinToString(","), funcionHabilitada.toString(), funcion,
        puntos.size.toString(), selecciones.size.toString(), vista.name, textoListaX.orEmpty(), textoListaY.orEmpty(),
        (textoListaX != null).toString(), (textoListaY != null).toString(), gradoPredeterminado.toString()) + puntos.flatMap { listOf(it.id.toString(), it.x, it.y) } +
        selecciones.entries.sortedBy { it.key }.flatMap { listOf(it.key.toString(), it.value.joinToString(",")) }

    companion object {
        fun restaurar(l: List<String>): EstadoLagrange? = runCatching {
            require(l.size >= 8)
            val np = l[6].toInt()
            val ns = l[7].toInt()
            val inicio = when (l[0]) { "L3" -> 14; "L2" -> 13; "L1" -> 8; else -> error("Formato desconocido") }
            require(np in 1..((l.size - inicio) / 3) && ns in 0..DatosLagrange.MAX_GRADO && l.size == inicio + np * 3 + ns * 2)
            val puntos = l.subList(inicio, inicio + np * 3).chunked(3).map { PuntoEntradaLagrange(it[0].toInt(), it[1], it[2]) }
            val selecciones = l.drop(inicio + np * 3).chunked(2).associate { it[0].toInt() to enteros(it[1]) }
            EstadoLagrange(puntos, l[1], enteros(l[3]), ModoNodos.valueOf(l[2]), selecciones, l[4].toBooleanStrict(), l[5],
                if (inicio >= 13) VistaDatos.valueOf(l[8]) else VistaDatos.LISTA,
                if (inicio >= 13 && l[11].toBooleanStrict()) l[9] else null,
                if (inicio >= 13 && l[12].toBooleanStrict()) l[10] else null,
                if (inicio == 14) l[13].toBooleanStrict() else false)
        }.getOrNull()

        private fun enteros(s: String) = if (s.isBlank()) emptyList() else s.split(',').map { it.toInt() }
    }
}

/** Migra únicamente el formulario antiguo de Lagrange; Newton conserva su formulario. */
fun EstadoFormulario.paraMetodo(id: MetodoId): EstadoFormulario {
    if (id != MetodoId.LAGRANGE || this !is EstadoInterpolacion) return this
    val xs = valoresDeTexto(xs)
    val ys = valoresDeTexto(ys)
    val puntos = List(maxOf(xs.size, ys.size, 2)) { PuntoEntradaLagrange(it, xs.getOrElse(it) { "" }, ys.getOrElse(it) { "" }) }
    val g = if (modo == ModoNodos.MANUAL) (xs.size - 1).coerceAtLeast(1) else grado.toIntOrNull() ?: 1
    return EstadoLagrange(puntos, x, listOf(g), modo,
        if (modo == ModoNodos.MANUAL) mapOf(g to puntos.filter { it.x.isNotBlank() || it.y.isNotBlank() }.map { it.id }) else emptyMap())
}

/** Pegado estricto: dos columnas; nunca descarta una columna extra silenciosamente. */
fun leerPegadoLagrange(texto: String): Pair<List<String>, List<String>> {
    val filas = texto.lines().filter { it.isNotBlank() }.map { fila ->
        if ('\t' in fila) fila.split('\t').map { it.trim() }
        else fila.trim().split(Regex("[; ]+")).map { it.trim() }
    }.toMutableList()
    if (filas.firstOrNull()?.map { it.lowercase() } == listOf("x", "y")) filas.removeAt(0)
    require(filas.isNotEmpty()) { "Pega dos columnas con los valores X e Y." }
    require(filas.size <= DatosLagrange.MAX_PUNTOS) { "El máximo es ${DatosLagrange.MAX_PUNTOS} puntos." }
    filas.forEachIndexed { i, fila ->
        require(fila.size == 2) { "Fila ${i + 1}: se necesitan exactamente dos columnas (X e Y)." }
        require(fila.all { DatosLagrange.numeroCelda(it) != null }) { "Revisa los números de la fila ${i + 1}." }
    }
    return filas.map { it[0] } to filas.map { it[1] }
}
