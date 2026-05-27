const express = require('express');
const { body, validationResult } = require('express-validator');
const taskService = require('../services/taskService');

const router = express.Router();

const STATUSES = ['TODO', 'IN_PROGRESS', 'DONE'];
const STATUS_LABELS = { TODO: 'To Do', IN_PROGRESS: 'In Progress', DONE: 'Done' };

router.get('/', async (req, res, next) => {
    try {
        const tasks = await taskService.getAllTasks();
        res.render('tasks/index', { tasks, statusLabels: STATUS_LABELS, title: 'Tasks' });
    } catch (err) {
        next(err);
    }
});

router.get('/tasks/new', (req, res) => {
    res.render('tasks/new', { title: 'Create Task', errors: [], values: {} });
});

router.post(
    '/tasks',
    [
        body('title').trim().notEmpty().withMessage('Title is required'),
        body('dueDate')
            .notEmpty().withMessage('Due date is required')
            .isISO8601().withMessage('Due date must be a valid date')
            .custom(value => new Date(value) > new Date()).withMessage('Due date must be in the future'),
    ],
    async (req, res, next) => {
        const errors = validationResult(req);
        if (!errors.isEmpty()) {
            return res.status(400).render('tasks/new', {
                title: 'Create Task',
                errors: errors.array(),
                values: req.body,
            });
        }

        try {
            await taskService.createTask({
                title: req.body.title,
                description: req.body.description || null,
                status: STATUSES.includes(req.body.status) ? req.body.status : 'TODO',
                dueDate: new Date(req.body.dueDate).toISOString().replace('Z', ''),
            });
            res.redirect('/?success=Task+created+successfully');
        } catch (err) {
            next(err);
        }
    }
);

router.get('/tasks/:id', async (req, res, next) => {
    try {
        const task = await taskService.getTaskById(req.params.id);
        res.render('tasks/detail', { task, statusLabels: STATUS_LABELS, statuses: STATUSES, title: task.title });
    } catch (err) {
        if (err.response && err.response.status === 404) {
            return res.status(404).render('error', { title: 'Task not found', message: `No task with id ${req.params.id}` });
        }
        next(err);
    }
});

router.post('/tasks/:id/status', async (req, res, next) => {
    try {
        await taskService.updateTaskStatus(req.params.id, req.body.status);
        res.redirect(`/tasks/${req.params.id}?success=Status+updated`);
    } catch (err) {
        if (err.response && err.response.status === 404) {
            return res.status(404).render('error', { title: 'Task not found', message: `No task with id ${req.params.id}` });
        }
        next(err);
    }
});

router.post('/tasks/:id/delete', async (req, res, next) => {
    try {
        await taskService.deleteTask(req.params.id);
        res.redirect('/?success=Task+deleted');
    } catch (err) {
        if (err.response && err.response.status === 404) {
            return res.status(404).render('error', { title: 'Task not found', message: `No task with id ${req.params.id}` });
        }
        next(err);
    }
});

module.exports = router;
