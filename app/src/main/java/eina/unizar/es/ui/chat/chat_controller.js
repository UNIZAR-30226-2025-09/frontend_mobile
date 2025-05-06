import jwt from 'jsonwebtoken';
import db from "#src/models/index";
import { Op } from 'sequelize';

// Controlador para enviar un mensaje a otro usuario
export const sendMessage = async (req, res) => {
    try {
        const token = req.headers.authorization?.split(' ')[1];

        if (!token) {
            return res.status(401).json({ error: "Token no proporcionado" });
        }

        const decoded = jwt.verify(token, process.env.JWT_SECRET || 'aB1cD2eF3GhIjK4LmN5OpQr6StUvWxY7Z');

        if (!decoded) {
            return res.status(401).json({ error: "Token inválido" });
        }

        const user1_id = decoded.id; // Remitente
        const { user2_id, message } = req.body; // Destinatario y mensaje

        if (!user2_id || !message) {
            return res.status(400).json({ error: "Se requiere ID de destinatario y mensaje" });
        }

        // Verificar que los usuarios son amigos
        const friendship = await db.friendship.findOne({
            where: {
                [Op.or]: [
                    { user1_id: user1_id, user2_id: user2_id },
                    { user1_id: user2_id, user2_id: user1_id }
                ],
                state_friend_request: 'accepted'
            }
        });

        if (!friendship) {
            return res.status(403).json({ error: "No puedes enviar mensajes a este usuario porque no son amigos" });
        }

        const Chat = db.chat;
        const newMessage = await Chat.build({
            user1_id: user1_id,
            user2_id: user2_id,
            txt_message: message,
            sent_at: new Date(),
            read: false
        });
        await newMessage.save();

        return res.status(201).json({
            message: "Mensaje enviado correctamente",
            data: newMessage
        });

    } catch (error) {
        console.error("Error al enviar mensaje:", error);
        return res.status(500).json({
            error: "Error al enviar mensaje",
            details: error.message
        });
    }
};

// Controlador para obtener la conversación con otro usuario
export const getConversation = async (req, res) => {
    try {
        const token = req.headers.authorization?.split(' ')[1];

        if (!token) {
            return res.status(401).json({ error: "Token no proporcionado" });
        }

        const decoded = jwt.verify(token, process.env.JWT_SECRET || 'aB1cD2eF3GhIjK4LmN5OpQr6StUvWxY7Z');

        if (!decoded) {
            return res.status(401).json({ error: "Token inválido" });
        }

        const userId = decoded.id;
        const { friendId } = req.params;

        if (!friendId) {
            return res.status(400).json({ error: "Se requiere ID del amigo" });
        }

        // Verificar que los usuarios son amigos
        const friendship = await db.friendship.findOne({
            where: {
                [Op.or]: [
                    { user1_id: userId, user2_id: friendId },
                    { user1_id: friendId, user2_id: userId }
                ],
                state_friend_request: 'accepted'
            }
        });

        if (!friendship) {
            return res.status(403).json({ error: "No puedes ver mensajes con este usuario porque no son amigos" });
        }

        // Obtener todos los mensajes entre los dos usuarios
        const messages = await db.chat.findAll({
            where: {
                [Op.or]: [
                    { user1_id: userId, user2_id: friendId },
                    { user1_id: friendId, user2_id: userId }
                ]
            },
            order: [['sent_at', 'ASC']] // Ordenar por fecha ascendente
        });

        // Marcar como leídos los mensajes enviados por el amigo
        await db.chat.update(
            { read: true },
            {
                where: {
                    user1_id: friendId,
                    user2_id: userId,
                    read: false
                }
            }
        );

        return res.status(200).json({
            messages,
            count: messages.length
        });

    } catch (error) {
        console.error("Error al obtener conversación:", error);
        return res.status(500).json({
            error: "Error al obtener conversación",
            details: error.message
        });
    }
};

// Controlador para obtener todas las conversaciones del usuario
export const getAllConversations = async (req, res) => {
    try {
        const token = req.headers.authorization?.split(' ')[1];

        if (!token) {
            return res.status(401).json({ error: "Token no proporcionado" });
        }

        const decoded = jwt.verify(token, process.env.JWT_SECRET || 'aB1cD2eF3GhIjK4LmN5OpQr6StUvWxY7Z');

        if (!decoded) {
            return res.status(401).json({ error: "Token inválido" });
        }

        const userId = decoded.id;

        // Obtener todos los amigos del usuario
        const friendships = await db.friendship.findAll({
            where: {
                [Op.or]: [
                    { user1_id: userId },
                    { user2_id: userId }
                ],
                state_friend_request: 'accepted'
            }
        });

        // Extraer IDs de amigos
        const friendIds = friendships.map(friendship => 
            friendship.user1_id === userId ? friendship.user2_id : friendship.user1_id
        );

        // Obtener información de todos los amigos
        const friendsInfo = await db.user.findAll({
            where: {
                id: {
                    [Op.in]: friendIds
                }
            },
            attributes: ['id', 'nickname', 'user_picture']
        });

        // Para cada amigo, obtener el último mensaje intercambiado
        const conversationPromises = friendsInfo.map(async (friend) => {
            const lastMessage = await db.chat.findOne({
                where: {
                    [Op.or]: [
                        { user1_id: userId, user2_id: friend.id },
                        { user1_id: friend.id, user2_id: userId }
                    ]
                },
                order: [['sent_at', 'DESC']]
            });

            // Contar mensajes no leídos
            const unreadCount = await db.chat.count({
                where: {
                    user1_id: friend.id,
                    user2_id: userId,
                    read: false
                }
            });

            return {
                friend: {
                    id: friend.id,
                    nickname: friend.nickname,
                    user_picture: friend.user_picture
                },
                lastMessage: lastMessage || null,
                unreadCount
            };
        });

        const conversations = await Promise.all(conversationPromises);

        // Ordenar por fecha del último mensaje (más reciente primero)
        conversations.sort((a, b) => {
            if (!a.lastMessage) return 1;
            if (!b.lastMessage) return -1;
            return new Date(b.lastMessage.sent_at) - new Date(a.lastMessage.sent_at);
        });

        return res.status(200).json({
            conversations,
            count: conversations.length
        });

    } catch (error) {
        console.error("Error al obtener conversaciones:", error);
        return res.status(500).json({
            error: "Error al obtener conversaciones",
            details: error.message
        });
    }
};