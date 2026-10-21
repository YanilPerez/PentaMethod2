package com.example.pentametod2.methods

/** Entrada inválida para un método numérico (tamaños distintos, nodos repetidos, matriz singular, etc.). */
class MetodoNumericoException(mensaje: String) : Exception(mensaje)
