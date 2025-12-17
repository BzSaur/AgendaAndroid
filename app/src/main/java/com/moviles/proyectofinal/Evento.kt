package com.moviles.proyectofinal

data class Evento(
    val fecha: String,
    val hora: String,
    val categoria: String,
    val descripcion: String,
    val status: String,
    val ubicacion: String,
    val contacto: String
) {
    // Función para convertir el objeto Evento a una sola línea de texto
    fun toTxtLine(): String {
        return "$fecha$SEPARATOR$hora$SEPARATOR$categoria$SEPARATOR$descripcion$SEPARATOR$status$SEPARATOR$ubicacion$SEPARATOR$contacto\n"
    }

    // Objeto estático para crear un Evento desde una línea de texto
    companion object {
        // Definimos un separador único para serializar los datos
        private const val SEPARATOR = "~~"
        fun fromTxtLine(line: String): Evento? {
            // Limpiamos la línea de cualquier salto de línea, si existe
            val cleanLine = line.trim()
            if (cleanLine.isBlank()) return null
            
            val parts = cleanLine.split(SEPARATOR)
            
            // Debe tener exactamente 7 partes
            return if (parts.size == 7) {
                Evento(
                    fecha = parts[0],
                    hora = parts[1],
                    categoria = parts[2],
                    descripcion = parts[3],
                    status = parts[4],
                    ubicacion = parts[5],
                    contacto = parts[6]
                )
            } else {
                null // Error al parsear la línea
            }
        }
    }
}