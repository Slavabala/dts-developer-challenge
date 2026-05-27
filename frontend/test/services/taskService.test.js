const axios = require('axios');

jest.mock('axios');

const mockClient = {
    get: jest.fn(),
    post: jest.fn(),
    patch: jest.fn(),
    delete: jest.fn(),
};
axios.create.mockReturnValue(mockClient);

const taskService = require('../../src/services/taskService');

beforeEach(() => jest.clearAllMocks());

describe('taskService', () => {
    it('getAllTasks requests /tasks and returns the data', async () => {
        mockClient.get.mockResolvedValue({ data: [{ id: '1' }] });
        const result = await taskService.getAllTasks();
        expect(mockClient.get).toHaveBeenCalledWith('/tasks');
        expect(result).toEqual([{ id: '1' }]);
    });

    it('getTaskById requests the task by id', async () => {
        mockClient.get.mockResolvedValue({ data: { id: 'abc' } });
        const result = await taskService.getTaskById('abc');
        expect(mockClient.get).toHaveBeenCalledWith('/tasks/abc');
        expect(result).toEqual({ id: 'abc' });
    });

    it('createTask posts the payload and returns the created task', async () => {
        const payload = { title: 'x', status: 'TODO', dueDate: '2099-01-01T00:00:00' };
        mockClient.post.mockResolvedValue({ data: { id: '1', ...payload } });
        const result = await taskService.createTask(payload);
        expect(mockClient.post).toHaveBeenCalledWith('/tasks', payload);
        expect(result.id).toBe('1');
    });

    it('updateTaskStatus patches the status endpoint', async () => {
        mockClient.patch.mockResolvedValue({ data: { id: '1', status: 'DONE' } });
        const result = await taskService.updateTaskStatus('1', 'DONE');
        expect(mockClient.patch).toHaveBeenCalledWith('/tasks/1/status', { status: 'DONE' });
        expect(result.status).toBe('DONE');
    });

    it('deleteTask issues a delete request', async () => {
        mockClient.delete.mockResolvedValue({});
        await taskService.deleteTask('1');
        expect(mockClient.delete).toHaveBeenCalledWith('/tasks/1');
    });

    it('propagates errors from the backend', async () => {
        const err = new Error('boom');
        mockClient.get.mockRejectedValue(err);
        await expect(taskService.getAllTasks()).rejects.toThrow('boom');
    });
});
