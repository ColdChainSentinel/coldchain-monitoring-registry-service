import { services } from '../api/services';
import type { AuthSession, CreateUser, UpdatePermissions, UserAccount } from '../types/domain';

export const identityApi = {
  login: (username: string, password: string) =>
    services.identity.post<AuthSession>('/api/v1/auth/login', { username, password }),
  logout: () => services.identity.post<void>('/api/v1/auth/logout'),
  me: (signal?: AbortSignal) => services.identity.get<AuthSession>('/api/v1/auth/me', signal),
  listUsers: (signal?: AbortSignal) => services.identity.get<UserAccount[]>('/api/v1/users', signal),
  createUser: (input: CreateUser) => services.identity.post<UserAccount>('/api/v1/users', input),
  updateStatus: (id: string, active: boolean) =>
    services.identity.patch<UserAccount>(`/api/v1/users/${id}/status`, { active }),
  updatePermissions: (id: string, input: UpdatePermissions) =>
    services.identity.put<UserAccount>(`/api/v1/users/${id}/permissions`, input),
};
