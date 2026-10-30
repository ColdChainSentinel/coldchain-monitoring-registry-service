# Frontend base

React + Vite + TypeScript. Requiere Node 22.12+ y npm. Desde esta carpeta:

```sh
npm ci
npm run dev
npm run typecheck
npm test
npm run build
```

Abrir http://localhost:5173. El botón consulta salud real; no se simulan datos.
No hay dashboard ni operaciones funcionales nuevas. `src/api/` contiene clientes;
`src/pages/` las páginas. Ver `.env.example`; copiar opcionalmente a `.env.local`.

El proxy de desarrollo evita cambiar CORS de Registry. En despliegue, configurar
proxy equivalente o URLs absolutas y CORS explícito. `VITE_*` se incorpora al bundle
en build: no incluir secretos. `npm run preview` sirve `dist` para revisión local
(no producción, sin garantía de proxy de APIs).
