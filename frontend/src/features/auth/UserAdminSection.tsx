import { useCallback, useState, type FormEvent } from 'react';
import { errorMessage } from '../../api/http';
import { Notice, ResourceFeedback } from '../../components/Feedback';
import { useResource } from '../../hooks/useResource';
import { identityApi } from '../../services/identity';
import type { MonitoringPoint, Role, UserAccount } from '../../types/domain';

const ROLES: Role[] = ['ADMIN', 'SUPERVISOR', 'OPERADOR', 'CONDUCTOR'];

export function UserAdminSection({
  points,
  currentUserId,
}: {
  points: MonitoringPoint[];
  currentUserId?: string;
}) {
  const [revision, setRevision] = useState(0);
  const loadUsers = useCallback((signal: AbortSignal) => identityApi.listUsers(signal), []);
  const users = useResource('users', loadUsers, revision);

  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [fullName, setFullName] = useState('');
  const [role, setRole] = useState<Role>('OPERADOR');
  const [selectedPoints, setSelectedPoints] = useState<string[]>([]);
  const [feedback, setFeedback] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  function togglePoint(pointId: string) {
    setSelectedPoints((prev) =>
      prev.includes(pointId) ? prev.filter((id) => id !== pointId) : [...prev, pointId],
    );
  }

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    setSaving(true);
    setError(null);
    setFeedback(null);
    try {
      const created = await identityApi.createUser({
        username: username.trim(),
        password,
        fullName: fullName.trim(),
        role,
        allowedPointIds: selectedPoints,
      });
      setUsername('');
      setPassword('');
      setFullName('');
      setSelectedPoints([]);
      setFeedback(`Cuenta ${created.username} registrada con rol ${created.role}.`);
      setRevision((v) => v + 1);
    } catch (err) {
      setError(errorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  async function handleToggleStatus(user: UserAccount) {
    setError(null);
    setFeedback(null);
    try {
      const updated = await identityApi.updateStatus(user.id, !user.active);
      setFeedback(`Cuenta ${updated.username} ${updated.active ? 'activada' : 'desactivada'}.`);
      setRevision((v) => v + 1);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function handleToggleUserPoint(user: UserAccount, pointId: string) {
    setError(null);
    setFeedback(null);
    const nextPoints = user.allowedPointIds.includes(pointId)
      ? user.allowedPointIds.filter((id) => id !== pointId)
      : [...user.allowedPointIds, pointId];
    try {
      await identityApi.updatePermissions(user.id, { role: user.role, allowedPointIds: nextPoints });
      setFeedback(`Permisos actualizados para ${user.username}.`);
      setRevision((v) => v + 1);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  async function handleChangeUserRole(user: UserAccount, nextRole: Role) {
    setError(null);
    setFeedback(null);
    try {
      await identityApi.updatePermissions(user.id, {
        role: nextRole,
        allowedPointIds: nextRole === 'ADMIN' ? [] : user.allowedPointIds,
      });
      setFeedback(`Rol actualizado a ${nextRole} para ${user.username}.`);
      setRevision((v) => v + 1);
    } catch (err) {
      setError(errorMessage(err));
    }
  }

  return (
    <div className="feature-grid">
      <section className="card data-card">
        <div className="card-heading">
          <div>
            <span className="eyebrow">EP-01 · HU-02 / HU-03</span>
            <h2>Cuentas y permisos por punto</h2>
          </div>
        </div>
        {feedback && <Notice>{feedback}</Notice>}
        {error && <Notice tone="warning">{error}</Notice>}
        <ResourceFeedback resource={users} onRetry={users.reload} />
        {users.data && (
          <div style={{ display: 'grid', gap: '1rem', padding: '0 23px 23px' }}>
            {users.data.map((user) => {
              const isSelf = Boolean(currentUserId && user.id === currentUserId);
              return (
                <article key={user.id} className="card" style={{ padding: '1rem' }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: '0.75rem' }}>
                    <div>
                      <strong>{user.username}</strong> · {user.fullName}
                      {isSelf && <span className="section-description"> (tu cuenta)</span>}
                      <div className="section-description">
                        Estado: {user.active ? 'Activo' : 'Inactivo'}
                      </div>
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                      <label className="sr-only" htmlFor={`role-${user.id}`}>
                        Rol de {user.username}
                      </label>
                      <select
                        id={`role-${user.id}`}
                        value={user.role}
                        disabled={isSelf}
                        title={isSelf ? 'No puedes cambiar tu propio rol de administrador' : 'Cambiar rol'}
                        onChange={(e) => handleChangeUserRole(user, e.target.value as Role)}
                      >
                        {ROLES.map((item) => (
                          <option key={item} value={item}>
                            {item}
                          </option>
                        ))}
                      </select>
                      <button
                        type="button"
                        className="button button--secondary"
                        disabled={isSelf}
                        title={isSelf ? 'No puedes desactivar tu propia cuenta' : undefined}
                        onClick={() => handleToggleStatus(user)}
                      >
                        {user.active ? 'Desactivar' : 'Activar'}
                      </button>
                    </div>
                  </div>
                  {user.role !== 'ADMIN' && points.length > 0 && (
                    <div style={{ marginTop: '0.75rem', display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                      {points.map((pt) => (
                        <label key={pt.id} style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}>
                          <input
                            type="checkbox"
                            checked={user.allowedPointIds.includes(pt.id)}
                            onChange={() => handleToggleUserPoint(user, pt.id)}
                          />
                          <span>{pt.code}</span>
                        </label>
                      ))}
                    </div>
                  )}
                </article>
              );
            })}
          </div>
        )}
      </section>

      <section className="card form-card">
        <div className="card-heading">
          <div>
            <span className="eyebrow">NUEVA CUENTA</span>
            <h2>Registrar usuario</h2>
          </div>
        </div>
        <form className="form-grid" onSubmit={handleCreate}>
          <div className="field">
            <label htmlFor="user-username">Identificador</label>
            <input
              id="user-username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              disabled={saving}
              placeholder="ej. operador01"
              required
            />
          </div>
          <div className="field">
            <label htmlFor="user-fullname">Nombre completo</label>
            <input
              id="user-fullname"
              value={fullName}
              onChange={(e) => setFullName(e.target.value)}
              disabled={saving}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="user-password">Contraseña (mín. 8 caracteres)</label>
            <input
              id="user-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              disabled={saving}
              minLength={8}
              required
            />
          </div>
          <div className="field">
            <label htmlFor="user-role">Rol</label>
            <select
              id="user-role"
              value={role}
              onChange={(e) => setRole(e.target.value as Role)}
              disabled={saving}
            >
              {ROLES.map((item) => (
                <option key={item} value={item}>
                  {item}
                </option>
              ))}
            </select>
          </div>
          {role !== 'ADMIN' && points.length > 0 && (
            <div className="field">
              <span>Puntos permitidos</span>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: '0.5rem' }}>
                {points.map((pt) => (
                  <label key={pt.id} style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}>
                    <input
                      type="checkbox"
                      checked={selectedPoints.includes(pt.id)}
                      onChange={() => togglePoint(pt.id)}
                    />
                    <span>{pt.code}</span>
                  </label>
                ))}
              </div>
            </div>
          )}
          <button className="button button--primary" type="submit" disabled={saving}>
            {saving ? 'Guardando…' : 'Registrar cuenta'}
          </button>
        </form>
      </section>
    </div>
  );
}
