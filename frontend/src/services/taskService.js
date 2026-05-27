const axios = require('axios');

const API_BASE = process.env.API_BASE_URL || 'http://localhost:8080/api';

const client = axios.create({
    baseURL: API_BASE,
    timeout: 10000,
    headers: { 'Content-Type': 'application/json' },
});

async function getAllTasks() {
    const { data } = await client.get('/tasks');
    return data;
}

async function getTaskById(id) {
    const { data } = await client.get(`/tasks/${id}`);
    return data;
}

async function createTask(taskData) {
    const { data } = await client.post('/tasks', taskData);
    return data;
}

async function updateTaskStatus(id, status) {
    const { data } = await client.patch(`/tasks/${id}/status`, { status });
    return data;
}

async function deleteTask(id) {
    await client.delete(`/tasks/${id}`);
}

module.exports = {
    getAllTasks,
    getTaskById,
    createTask,
    updateTaskStatus,
    deleteTask,
};
