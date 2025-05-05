import jwt from 'jsonwebtoken';
import db from "#src/models/index";
import { Op } from 'sequelize';

// Controlador para enviar solicitud de amistad
// Este controlador maneja el envío de solicitudes de amistad entre dos usuarios. Recibe los
// IDs de los usuarios involucrados, verifica si ambos existen, y si no hay solicitudes
// previas, crea una nueva solicitud en la base de datos.
// user1 es el sender y user2 es el receiver
export const sendFriendRequest = async (req, res) => {
    try {
        // Extraemos el token de autorización de las cabeceras
        const token = req.headers.authorization?.split(' ')[1];  // Obtenemos el token del header 'Authorization'

        if (!token) {
            return res.status(401).json({ error: "Token no proporcionado" });
        }

        // Verificamos y decodificamos el token para obtener el ID del usuario autenticado
        const decoded = jwt.verify(token, process.env.JWT_SECRET || 'aB1cD2eF3GhIjK4LmN5OpQr6StUvWxY7Z');

        // Si no se puede decodificar el token, respondemos con error
        if (!decoded) {
            return res.status(401).json({ error: "Token inválido" });
        }

        // Obtenemos el ID del usuario autenticado (user1)
        const user1_id = decoded.id;

        // Extraemos el ID del segundo usuario (user2) desde el cuerpo de la solicitud
        const { user2_id } = req.body;

        // Verificamos que el usuario no esté intentando enviarse una solicitud a sí mismo
        if (user1_id === user2_id) {
            return res.status(400).json({ error: "Ya existe una solicitud de amistad entre estos usuarios" });
        }

        // Verificamos si ambos usuarios existen en la base de datos
        const user1 = await db.user.findByPk(user1_id);
        const user2 = await db.user.findByPk(user2_id);

        // Si uno de los usuarios no existe, respondemos con un error 404
        if (!user1 || !user2) {
            return res.status(404).json({ error: "Uno o ambos usuarios no existen" });
        }

        // Verificamos si ya existe una solicitud de amistad entre los dos usuarios
        const existingRequest = await db.friendship.findOne({
            where: {
                // Buscamos en ambas direcciones, para cubrir ambas combinaciones de usuarios (user1 -> user2 y user2 -> user1)
                [Op.or]: [
                    { user1_id, user2_id },
                    { user1_id: user2_id, user2_id: user1_id }
                ]
            }
        });

        // Si ya existe una solicitud, devolvemos un error 400 indicando que ya hay una solicitud pendiente
        if (existingRequest) {
            return res.status(400).json({ error: "Ya existe una solicitud de amistad entre estos usuarios" });
        }

        // Si no existe una solicitud, creamos una nueva en la tabla de amistad
        const newFriendship = await db.friendship.create({
            user1_id,              // ID del primer usuario (sender)
            user2_id,              // ID del segundo usuario (receiver)
            state_friend_request: 'pending'  // El estado inicial de la solicitud será "pendiente"
        });

        // Respondemos con un mensaje de éxito y los detalles de la solicitud de amistad creada
        return res.status(201).json({
            message: "Solicitud de amistad enviada correctamente",
            friendship: newFriendship  // Información de la nueva solicitud
        });
    } catch (error) {
        // En caso de error, registramos el error en la consola y respondemos con un error 500
        console.error("Error al enviar solicitud de amistad:", error);
        return res.status(500).json({ error: "Error al enviar solicitud de amistad", details: error.message });
    }
};

// Controlador para aceptar solicitud de amistad
// Este controlador maneja la aceptación de solicitudes de amistad. Solo el receptor (user2)
// puede aceptar una solicitud. Verifica que la solicitud exista y esté en estado pendiente,
// y la actualiza a estado "accepted".
export const acceptFriendRequest = async (req, res) => {
    try {
        // Extraemos el token de autorización de las cabeceras
        const token = req.headers.authorization?.split(' ')[1];  // Obtenemos el token del header 'Authorization'

        if (!token) {
            return res.status(401).json({ error: "Token no proporcionado" });
        }

        // Verificamos y decodificamos el token para obtener el ID del usuario autenticado
        const decoded = jwt.verify(token, process.env.JWT_SECRET || 'aB1cD2eF3GhIjK4LmN5OpQr6StUvWxY7Z');

        // Si no se puede decodificar el token, respondemos con error
        if (!decoded) {
            return res.status(401).json({ error: "Token inválido" });
        }

        // Obtenemos el ID del usuario autenticado que debe ser el receptor (user2)
        const user2_id = decoded.id;

        // Extraemos el ID del remitente (user1) desde el cuerpo de la solicitud
        const { user1_id } = req.body;

        // Buscamos la solicitud de amistad donde el usuario autenticado es el receptor
        const friendship = await db.friendship.findOne({
            where: {
                user1_id: user1_id,
                user2_id: user2_id,
                state_friend_request: 'pending'  // La solicitud debe estar pendiente
            }
        });

        // Si no existe la solicitud de amistad o no está pendiente
        if (!friendship) {
            return res.status(404).json({
                error: "Solicitud de amistad no encontrada o no tienes permisos para aceptarla"
            });
        }

        // Actualizamos el estado de la solicitud a "accepted"
        await friendship.update({ state_friend_request: 'accepted' });

        // Respondemos con un mensaje de éxito y los detalles de la solicitud actualizada
        return res.status(200).json({
            message: "Solicitud de amistad aceptada correctamente",
            friendship: friendship  // Información de la solicitud actualizada
        });

    } catch (error) {
        // En caso de error, registramos el error en la consola y respondemos con un error 500
        console.error("Error al aceptar solicitud de amistad:", error);
        return res.status(500).json({
            error: "Error al aceptar solicitud de amistad",
            details: error.message
        });
    }
};

// Controlador para rechazar o eliminar solicitud de amistad
// Este controlador maneja el rechazo o eliminación de solicitudes de amistad. Cualquiera de los
// dos usuarios involucrados (sender o receiver) puede eliminar la solicitud. Verifica que la
// solicitud exista y la elimina de la base de datos.
export const rejectFriendRequest = async (req, res) => {
    try {
        // Extraemos el token de autorización de las cabeceras
        const token = req.headers.authorization?.split(' ')[1];

        if (!token) {
            return res.status(401).json({ error: "Token no proporcionado" });
        }

        // Verificamos y decodificamos el token para obtener el ID del usuario autenticado
        const decoded = jwt.verify(token, process.env.JWT_SECRET || 'aB1cD2eF3GhIjK4LmN5OpQr6StUvWxY7Z');

        if (!decoded) {
            return res.status(401).json({ error: "Token inválido" });
        }

        // Obtenemos el ID del usuario autenticado
        const userId = decoded.id;

        // Extraemos el ID del otro usuario involucrado en la solicitud
        const { friendId } = req.body;

        // Buscamos la solicitud de amistad que involucre a ambos usuarios
        const friendship = await db.friendship.findOne({
            where: {
                [Op.or]: [
                    { user1_id: userId, user2_id: friendId },
                    { user1_id: friendId, user2_id: userId }
                ]
            }
        });

        // Si no existe la solicitud de amistad
        if (!friendship) {
            return res.status(404).json({ error: "Solicitud de amistad no encontrada" });
        }

        // Verificamos que la solicitud esté en estado pendiente
        if (friendship.state_friend_request !== 'pending') {
            return res.status(400).json({
                error: "No se puede eliminar una relación de amistad ya establecida"
            });
        }

        // Eliminamos la solicitud de amistad pendiente
        await db.friendship.destroy({
            where: {
                [Op.or]: [
                    { user1_id: userId, user2_id: friendId },
                    { user1_id: friendId, user2_id: userId }
                ]
            }
        });

        // Respondemos con un mensaje de éxito
        return res.status(200).json({
            message: "Solicitud de amistad eliminada correctamente"
        });

    } catch (error) {
        console.error("Error al rechazar solicitud de amistad:", error);
        return res.status(500).json({
            error: "Error al rechazar solicitud de amistad",
            details: error.message
        });
    }
};

// Controlador para buscar usuarios por nickname similar que no sean amigos ni tengan solicitudes pendientes
// Este controlador permite buscar usuarios cuyo nickname contenga un texto de búsqueda
// y que no tengan ninguna relación con el usuario autenticado
export const searchNewFriends = async (req, res) => {
    try {
        // Extraemos el token de autorización de las cabeceras
        const token = req.headers.authorization?.split(' ')[1];

        if (!token) {
            return res.status(401).json({ error: "Token no proporcionado" });
        }

        // Verificamos y decodificamos el token para obtener el ID del usuario autenticado
        const decoded = jwt.verify(token, process.env.JWT_SECRET || 'aB1cD2eF3GhIjK4LmN5OpQr6StUvWxY7Z');

        if (!decoded) {
            return res.status(401).json({ error: "Token inválido" });
        }

        // Obtenemos el ID del usuario autenticado
        const userId = decoded.id;

        // Extraemos el texto de búsqueda de los parámetros de consulta
        const { search } = req.query;

        if (!search) {
            return res.status(400).json({ error: "Debes proporcionar un término de búsqueda" });
        }

        // Obtenemos todos los IDs de usuarios con los que tenemos cualquier tipo de relación
        const friendships = await db.friendship.findAll({
            where: {
                [Op.or]: [
                    { user1_id: userId },
                    { user2_id: userId }
                ]
            }
        });

        // Extraemos los IDs de los usuarios relacionados
        const relatedUserIds = friendships.map(friendship =>
            friendship.user1_id === userId ? friendship.user2_id : friendship.user1_id
        );

        // Añadimos nuestro propio ID para excluirnos de los resultados
        relatedUserIds.push(userId);

        // Buscamos usuarios cuyo nickname coincida parcialmente con el texto de búsqueda
        // y que no tengan ninguna relación con el usuario autenticado
        const potentialFriends = await db.user.findAll({
            where: {
                nickname: {
                    [Op.iLike]: `%${search}%` // Búsqueda case-insensitive
                },
                id: {
                    [Op.notIn]: relatedUserIds
                }
            },
            attributes: ['id', 'nickname', 'user_picture'] // Solo devolvemos información básica
        });

        // Respondemos con la lista de usuarios encontrados
        return res.status(200).json({
            users: potentialFriends,
            count: potentialFriends.length
        });

    } catch (error) {
        console.error("Error al buscar usuarios:", error);
        return res.status(500).json({
            error: "Error al buscar usuarios",
            details: error.message
        });
    }
};

// Controlador para obtener todos los usuarios que no son amigos del usuario autenticado
// Busca y devuelve todos los usuarios que no tienen una relación de amistad aceptada con el usuario
// Solo incluye información básica de cada usuario: id, nickname y foto de perfil
export const getNewFriends = async (req, res) => {
    try {
        // Extraemos el token de autorización de las cabeceras
        const token = req.headers.authorization?.split(' ')[1];

        if (!token) {
            return res.status(401).json({ error: "Token no proporcionado" });
        }

        // Verificamos y decodificamos el token para obtener el ID del usuario autenticado
        const decoded = jwt.verify(token, process.env.JWT_SECRET || 'aB1cD2eF3GhIjK4LmN5OpQr6StUvWxY7Z');

        if (!decoded) {
            return res.status(401).json({ error: "Token inválido" });
        }

        // Obtenemos el ID del usuario autenticado
        const userId = decoded.id;

        // Obtenemos todos los IDs de usuarios con los que tenemos cualquier tipo de relación
        const friendships = await db.friendship.findAll({
            where: {
                [Op.or]: [
                    { user1_id: userId },
                    { user2_id: userId }
                ]
            }
        });

        // Extraemos los IDs de los usuarios relacionados
        const relatedUserIds = friendships.map(friendship =>
            friendship.user1_id === userId ? friendship.user2_id : friendship.user1_id
        );

        // Añadimos nuestro propio ID para excluirnos de los resultados
        relatedUserIds.push(userId);

        // Buscamos todos los usuarios que no están en la lista de usuarios relacionados
        const potentialFriends = await db.user.findAll({
            where: {
                id: {
                    [Op.notIn]: relatedUserIds
                }
            },
            attributes: ['id', 'nickname', 'user_picture'] // Solo devolvemos información básica
        });

        // Respondemos con la lista de usuarios encontrados
        return res.status(200).json({
            users: potentialFriends,
            count: potentialFriends.length
        });

    } catch (error) {
        console.error("Error al obtener usuarios potenciales:", error);
        return res.status(500).json({
            error: "Error al obtener usuarios potenciales",
            details: error.message
        });
    }
};

// Controlador para obtener las solicitudes de amistad enviadas por el usuario
// Devuelve todas las solicitudes de amistad donde el usuario autenticado es el remitente
// y el estado es 'pending'.
export const getSentFriendRequests = async (req, res) => {
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

        // Consulta modificada para usar la relación correcta
        const user = await db.user.findByPk(userId, {
            include: [{
                model: db.user,
                as: "FriendRequests", // Nombre de la relación en user.js
                through: {
                    where: { state_friend_request: 'pending' }
                },
                attributes: ['id', 'nickname', 'user_picture']
            }]
        });

        if (!user) {
            return res.status(404).json({ error: "Usuario no encontrado" });
        }

        const sentRequests = user.FriendRequests.map(friend => ({
            friendId: friend.id,
            nickname: friend.nickname,
            user_picture: friend.user_picture,
            state: friend.friendship.state_friend_request
        }));

        return res.status(200).json({
            sentRequests,
            count: sentRequests.length
        });

    } catch (error) {
        console.error("Error al obtener solicitudes enviadas:", error);
        return res.status(500).json({
            error: "Error al obtener solicitudes enviadas",
            details: error.message
        });
    }
};

// Controlador para obtener las solicitudes de amistad recibidas por el usuario
// Devuelve todas las solicitudes de amistad donde el usuario autenticado es el receptor
// y el estado es 'pending'.
export const getReceivedFriendRequests = async (req, res) => {
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

        // Consulta modificada para usar la relación correcta
        const user = await db.user.findByPk(userId, {
            include: [{
                model: db.user,
                as: "FriendInvitations", // Nombre de la relación en user.js
                through: {
                    where: { state_friend_request: 'pending' }
                },
                attributes: ['id', 'nickname', 'user_picture']
            }]
        });

        if (!user) {
            return res.status(404).json({ error: "Usuario no encontrado" });
        }

        const receivedRequests = user.FriendInvitations.map(friend => ({
            friendId: friend.id,
            nickname: friend.nickname,
            user_picture: friend.user_picture,
            state: friend.friendship.state_friend_request
        }));

        return res.status(200).json({
            receivedRequests,
            count: receivedRequests.length
        });

    } catch (error) {
        console.error("Error al obtener solicitudes recibidas:", error);
        return res.status(500).json({
            error: "Error al obtener solicitudes recibidas",
            details: error.message
        });
    }
};

// Controlador para listar todos los amigos del usuario
// Devuelve todas las relaciones de amistad aceptadas donde el usuario autenticado
// es parte de la relación (ya sea como user1 o user2).
export const getFriendsList = async (req, res) => {
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

        // Obtenemos amigos donde el usuario es el remitente (user1)
        const sentFriendships = await db.user.findByPk(userId, {
            include: [{
                model: db.user,
                as: "FriendRequests",
                through: {
                    where: { state_friend_request: 'accepted' }
                },
                attributes: ['id', 'nickname', 'user_picture']
            }]
        });

        // Obtenemos amigos donde el usuario es el receptor (user2)
        const receivedFriendships = await db.user.findByPk(userId, {
            include: [{
                model: db.user,
                as: "FriendInvitations",
                through: {
                    where: { state_friend_request: 'accepted' }
                },
                attributes: ['id', 'nickname', 'user_picture']
            }]
        });

        // Combinamos ambos resultados
        const sentFriends = sentFriendships?.FriendRequests || [];
        const receivedFriends = receivedFriendships?.FriendInvitations || [];

        const allFriends = [
            ...sentFriends.map(friend => ({
                friendshipId: `${userId}_${friend.id}`,
                friendId: friend.id,
                nickname: friend.nickname,
                user_picture: friend.user_picture
            })),
            ...receivedFriends.map(friend => ({
                friendshipId: `${friend.id}_${userId}`,
                friendId: friend.id,
                nickname: friend.nickname,
                user_picture: friend.user_picture
            }))
        ];

        return res.status(200).json({
            friends: allFriends,
            count: allFriends.length
        });

    } catch (error) {
        console.error("Error al obtener lista de amigos:", error);
        return res.status(500).json({
            error: "Error al obtener lista de amigos",
            details: error.message
        });
    }
};

// Controlador para dejar de seguir a un amigo
// Este controlador permite eliminar una relación de amistad establecida
// entre el usuario autenticado y otro usuario
export const unfollowFriend = async (req, res) => {
    try {
        // Extraemos el token de autorización de las cabeceras
        const token = req.headers.authorization?.split(' ')[1];

        if (!token) {
            return res.status(401).json({ error: "Token no proporcionado" });
        }

        // Verificamos y decodificamos el token
        const decoded = jwt.verify(token, process.env.JWT_SECRET || 'aB1cD2eF3GhIjK4LmN5OpQr6StUvWxY7Z');

        if (!decoded) {
            return res.status(401).json({ error: "Token inválido" });
        }

        // Obtenemos el ID del usuario autenticado
        const userId = decoded.id;

        // Extraemos el ID del amigo a dejar de seguir
        const { friendId } = req.body;

        if (!friendId) {
            return res.status(400).json({ error: "Debes proporcionar un ID de amigo" });
        }

        // Buscamos la relación de amistad entre ambos usuarios
        const friendship = await db.friendship.findOne({
            where: {
                [Op.or]: [
                    { user1_id: userId, user2_id: friendId },
                    { user1_id: friendId, user2_id: userId }
                ],
                state_friend_request: 'accepted' // Solo eliminamos relaciones aceptadas
            }
        });

        // Si no existe la relación de amistad
        if (!friendship) {
            return res.status(404).json({ error: "Relación de amistad no encontrada" });
        }

        // Eliminamos la relación de amistad
        await friendship.destroy();

        // Respondemos con un mensaje de éxito
        return res.status(200).json({
            message: "Has dejado de seguir a este usuario correctamente"
        });

    } catch (error) {
        console.error("Error al dejar de seguir al usuario:", error);
        return res.status(500).json({
            error: "Error al dejar de seguir al usuario",
            details: error.message
        });
    }
};