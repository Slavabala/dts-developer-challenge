const request = require('supertest');
const app = require('../../src/app');
const taskService = require('../../src/services/taskService');

jest.mock('../../src/services/taskService');

const SAMPLE_TASK = {
    id: '550e8400-e29b-41d4-a716-446655440000',
    title: 'Review case file',
    description: 'Check documents for case #1234',
    status: 'TODO',
    dueDate: '2099-12-31T12:00:00',
    createdAt: '2024-01-01T10:00:00',
    updatedAt: '2024-01-01T10:00:00',
};

beforeEach(() => jest.clearAllMocks());

describe('GET /', () => {
    it('renders task list when tasks exist', async () => {
        taskService.getAllTasks.mockResolvedValue([SAMPLE_TASK]);
        const res = await request(app).get('/');
        expect(res.status).toBe(200);
        expect(res.text).toContain('Review case file');
    });

    it('shows empty state when no tasks', async () => {
        taskService.getAllTasks.mockResolvedValue([]);
        const res = await request(app).get('/');
        expect(res.status).toBe(200);
        expect(res.text).toContain('No tasks found');
    });
});

describe('GET /tasks/new', () => {
    it('renders the create task form', async () => {
        const res = await request(app).get('/tasks/new');
        expect(res.status).toBe(200);
        expect(res.text).toContain('Create new task');
    });
});

describe('POST /tasks', () => {
    it('redirects to home on success', async () => {
        taskService.createTask.mockResolvedValue(SAMPLE_TASK);
        const res = await request(app).post('/tasks').send({
            title: 'Review case file',
            dueDate: '2099-12-31T12:00',
        });
        expect(res.status).toBe(302);
        expect(res.headers.location).toContain('/?success=');
    });

    it('returns 400 when title is missing', async () => {
        const res = await request(app).post('/tasks').send({ dueDate: '2099-12-31T12:00' });
        expect(res.status).toBe(400);
        expect(res.text).toContain('Title is required');
    });

    it('returns 400 when due date is in the past', async () => {
        const res = await request(app).post('/tasks').send({
            title: 'Test task',
            dueDate: '2000-01-01T12:00',
        });
        expect(res.status).toBe(400);
        expect(res.text).toContain('Due date must be in the future');
    });

    it('passes the selected status through to the service', async () => {
        taskService.createTask.mockResolvedValue(SAMPLE_TASK);
        await request(app).post('/tasks').send({
            title: 'With status',
            status: 'IN_PROGRESS',
            dueDate: '2099-12-31T12:00',
        });
        expect(taskService.createTask).toHaveBeenCalledWith(
            expect.objectContaining({ title: 'With status', status: 'IN_PROGRESS' })
        );
    });

    it('defaults to TODO when status is missing or invalid', async () => {
        taskService.createTask.mockResolvedValue(SAMPLE_TASK);
        await request(app).post('/tasks').send({
            title: 'No status',
            status: 'NONSENSE',
            dueDate: '2099-12-31T12:00',
        });
        expect(taskService.createTask).toHaveBeenCalledWith(
            expect.objectContaining({ status: 'TODO' })
        );
    });

    it('re-renders the form preserving values on validation error', async () => {
        const res = await request(app).post('/tasks').send({ dueDate: '2099-12-31T12:00' });
        expect(res.status).toBe(400);
        expect(res.text).toContain('value="2099-12-31T12:00"');
    });
});

describe('GET /tasks/:id', () => {
    it('renders task detail page', async () => {
        taskService.getTaskById.mockResolvedValue(SAMPLE_TASK);
        const res = await request(app).get(`/tasks/${SAMPLE_TASK.id}`);
        expect(res.status).toBe(200);
        expect(res.text).toContain('Review case file');
    });

    it('returns 404 when task not found', async () => {
        const err = new Error('Not found');
        err.response = { status: 404 };
        taskService.getTaskById.mockRejectedValue(err);
        const res = await request(app).get('/tasks/unknown-id');
        expect(res.status).toBe(404);
    });
});

describe('POST /tasks/:id/status', () => {
    it('redirects after status update', async () => {
        taskService.updateTaskStatus.mockResolvedValue({ ...SAMPLE_TASK, status: 'IN_PROGRESS' });
        const res = await request(app)
            .post(`/tasks/${SAMPLE_TASK.id}/status`)
            .send({ status: 'IN_PROGRESS' });
        expect(res.status).toBe(302);
    });
});

describe('POST /tasks/:id/status', () => {
    it('returns 404 when updating a task that does not exist', async () => {
        const err = new Error('Not found');
        err.response = { status: 404 };
        taskService.updateTaskStatus.mockRejectedValue(err);
        const res = await request(app)
            .post(`/tasks/${SAMPLE_TASK.id}/status`)
            .send({ status: 'DONE' });
        expect(res.status).toBe(404);
    });
});

describe('POST /tasks/:id/delete', () => {
    it('redirects to home after deletion', async () => {
        taskService.deleteTask.mockResolvedValue();
        const res = await request(app).post(`/tasks/${SAMPLE_TASK.id}/delete`);
        expect(res.status).toBe(302);
        expect(res.headers.location).toContain('/?success=Task+deleted');
    });

    it('returns 404 when deleting a task that does not exist', async () => {
        const err = new Error('Not found');
        err.response = { status: 404 };
        taskService.deleteTask.mockRejectedValue(err);
        const res = await request(app).post(`/tasks/${SAMPLE_TASK.id}/delete`);
        expect(res.status).toBe(404);
    });
});

describe('error handling and success banner', () => {
    it('renders the 500 error page when the backend fails', async () => {
        taskService.getAllTasks.mockRejectedValue(new Error('backend unavailable'));
        const res = await request(app).get('/');
        expect(res.status).toBe(500);
        expect(res.text).toContain('Something went wrong');
    });

    it('renders the success banner from the query string', async () => {
        taskService.getAllTasks.mockResolvedValue([]);
        const res = await request(app).get('/?success=Task+created+successfully');
        expect(res.status).toBe(200);
        expect(res.text).toContain('govuk-notification-banner--success');
        expect(res.text).toContain('Task created successfully');
    });

    it('returns the 404 page for unknown routes', async () => {
        const res = await request(app).get('/does-not-exist');
        expect(res.status).toBe(404);
        expect(res.text).toContain('Page not found');
    });
});
