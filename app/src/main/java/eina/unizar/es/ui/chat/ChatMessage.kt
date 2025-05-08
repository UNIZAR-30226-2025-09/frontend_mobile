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
 * @property sharedContent Contenido compartido opcional (canción, playlist, etc.)
 * @property isSending Opcional: para indicar estado de envío
 */
data class ChatMessage(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: Date,
    val isRead: Boolean,
    val sharedContent: String? = null,
    val isSending: Boolean = false // Opcional: para indicar estado de envío
)