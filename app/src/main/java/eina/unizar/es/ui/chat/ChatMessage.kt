// Crear un archivo ChatMessage.kt en el paquete eina.unizar.es.ui.chat
package eina.unizar.es.ui.chat

import java.util.Date

/**
 * Representa un mensaje en una conversación de chat.
 * 
 * @property id Identificador único del mensaje
 * @property senderId ID del usuario que envió el mensaje
 * @property receiverId ID del usuario que recibe el mensaje
 * @property content Contenido del mensaje
 * @property timestamp Fecha y hora en que se envió el mensaje
 * @property isRead Si el mensaje ha sido leído
 * @property sharedContent Contenido compartido opcional (playlist en formato JSON)
 */
data class ChatMessage(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: Date,
    val isRead: Boolean,
    val sharedContent: String? = null // Nuevo campo para guardar info de playlist en formato JSON
)