# WebInfoAPI - Documentação para Desenvolvedores Next.js/React

## Visão Geral

O **WebInfoAPI** é um mod para servidores Necesse que expõe informações detalhadas do servidor através de uma API REST. Esta documentação foi criada especificamente para desenvolvedores que trabalham com **Next.js** e **React** e desejam integrar dados do servidor Necesse em suas aplicações web.

## Configuração Base

### URL Base da API
```
http://[IP_DO_SERVIDOR]:[PORTA_API]/api
```

**Porta padrão:** 8080 (configurável no mod)

### Autenticação
A API utiliza autenticação por API Key para segurança. Inclua o header de autenticação em todas as requisições:

```javascript
headers: {
  'Authorization': 'Bearer YOUR_API_KEY_HERE',
  'Content-Type': 'application/json'
}
```

### Headers CORS
A API já está configurada com headers CORS apropriados para permitir requisições de qualquer origem:
- `Access-Control-Allow-Origin: *`
- `Access-Control-Allow-Methods: GET, POST, OPTIONS`
- `Access-Control-Allow-Headers: Content-Type, Authorization`

### Rate Limiting
A API implementa rate limiting para prevenir abuso:
- **Limite padrão:** 60 requisições por minuto por IP
- **Header de resposta:** `X-RateLimit-Remaining` indica requisições restantes
- **Código de erro:** 429 (Too Many Requests) quando o limite é excedido

## Endpoints Disponíveis

### 1. `/api/status` - Status Geral do Servidor
**Método:** GET  
**Descrição:** Informações básicas e status atual do servidor

**Resposta:**
```json
{
  "api_version": "2.0.0",
  "timestamp": 1758211414130,
  "server": {
    "name": "Meu Servidor Necesse",
    "online_players": 5,
    "max_players": 20,
    "uptime": 3600000,
    "version": "0.21.20",
    "port": 14242
  },
  "status": "success"
}
```

**Campos:**
- `api_version`: Versão da API (2.0.0)
- `timestamp`: Timestamp da resposta em milissegundos
- `server.name`: Nome do servidor
- `server.online_players`: Número atual de jogadores online
- `server.max_players`: Capacidade máxima do servidor
- `server.uptime`: Tempo de atividade em milissegundos
- `server.version`: Versão do Necesse
- `server.port`: Porta do servidor de jogo
- `status`: Status da requisição

### 2. `/api/players` - Lista de Jogadores
**Método:** GET  
**Descrição:** Informações detalhadas sobre jogadores conectados

**Resposta:**
```json
{
  "api_version": "2.0.0",
  "timestamp": 1758211414130,
  "players": {
    "total_players": 2,
    "online_players": [
      {
        "name": "PlayerOne",
        "uuid": "550e8400-e29b-41d4-a716-446655440000",
        "position": {
          "x": 150.5,
          "y": 200.75
        },
        "level_id": 25,
        "player_class": "Warrior",
        "player_level": 30,
        "experience": 15000,
        "online": true
      },
      {
        "name": "PlayerTwo",
        "uuid": "550e8400-e29b-41d4-a716-446655440001",
        "position": {
          "x": 175.25,
          "y": 180.0
        },
        "level_id": 18,
        "player_class": "Mage",
        "player_level": 22,
        "experience": 8500,
        "online": true
      }
    ]
  },
  "status": "success"
}
```

**Campos por jogador:**
- `name`: Nome do jogador
- `uuid`: Identificador único do jogador
- `position.x`, `position.y`: Posição no mundo (em tiles)
- `level_id`: ID do nível atual
- `player_class`: Classe do personagem
- `player_level`: Nível do jogador
- `experience`: Experiência atual
- `online`: Status online (sempre true para jogadores retornados)

### 3. `/api/world` - Informações do Mundo
**Método:** GET  
**Descrição:** Dados específicos sobre o mundo do jogo

**Resposta:**
```json
{
  "api_version": "2.0.0",
  "timestamp": 1758211414130,
  "world": {
    "world_name": "Mundo Principal",
    "world_time": "12:34",
    "world_day": 15,
    "world_seed": 1234567890,
    "biome_count": 8,
    "world_size": {
      "width": 2048,
      "height": 2048
    },
    "spawn_point": {
      "x": 1024,
      "y": 1024
    },
    "weather": "CLEAR",
    "season": "SPRING",
    "pvp_enabled": true,
    "difficulty": "NORMAL",
    "game_time": 86400000
  },
  "status": "success"
}
```

**Campos:**
- `world.world_name`: Nome do mundo
- `world.world_time`: Hora atual no jogo
- `world.world_day`: Dia atual no jogo
- `world.world_seed`: Seed do mundo
- `world.biome_count`: Número de biomas
- `world.world_size`: Dimensões do mundo
- `world.spawn_point`: Ponto de spawn
- `world.weather`: Clima atual
- `world.season`: Estação atual
- `world.pvp_enabled`: Se PvP está habilitado
- `world.difficulty`: Dificuldade do mundo
- `world.game_time`: Tempo total de jogo em milissegundos

### 4. `/api/system` - Telemetria do Sistema
**Método:** GET  
**Descrição:** Informações sobre o sistema e performance do servidor

**Resposta:**
```json
{
  "api_version": "2.0.0",
  "timestamp": 1758211414130,
  "system": {
    "cpu": {
      "usage_percent": 25.5,
      "cores": 8,
      "model": "Intel Core i7-9700K"
    },
    "memory": {
      "used_mb": 2048,
      "total_mb": 8192,
      "usage_percent": 25.0
    },
    "jvm": {
      "heap_used_mb": 512,
      "heap_max_mb": 2048,
      "heap_usage_percent": 25.0,
      "version": "17.0.2"
    },
    "os": {
      "name": "Windows 11",
      "version": "10.0",
      "arch": "amd64"
    }
  },
  "status": "success"
}
```

**Campos:**
- `system.cpu`: Informações da CPU (uso, cores, modelo)
- `system.memory`: Uso de memória do sistema
- `system.jvm`: Estatísticas da JVM (heap, versão)
- `system.os`: Informações do sistema operacional

## Tratamento de Erros

### Códigos de Status HTTP
- `200`: Sucesso
- `401`: Não autorizado (API key inválida ou ausente)
- `429`: Muitas requisições (rate limit excedido)
- `500`: Erro interno do servidor
- `503`: Servidor não iniciado

### Resposta de Erro
```json
{
  "api_version": "2.0.0",
  "timestamp": 1758211414130,
  "error": {
    "code": 401,
    "message": "API key inválida ou ausente",
    "details": "Inclua o header Authorization: Bearer YOUR_API_KEY"
  },
  "status": "error"
}
```

### Exemplo de Rate Limit Excedido
```json
{
  "api_version": "2.0.0",
  "timestamp": 1758211414130,
  "error": {
    "code": 429,
    "message": "Rate limit excedido",
    "details": "Limite de 60 requisições por minuto atingido"
  },
  "status": "error"
}
```

## Implementação em Next.js/React

### 1. Configuração do Cliente API

Crie um arquivo `lib/api.js` para centralizar as chamadas da API:

```javascript
const API_BASE_URL = 'http://localhost:8080/api';
const API_KEY = process.env.NEXT_PUBLIC_API_KEY || 'your-api-key-here';

class WebInfoAPIClient {
  constructor() {
    this.baseURL = API_BASE_URL;
    this.apiKey = API_KEY;
  }

  async request(endpoint, options = {}) {
    const url = `${this.baseURL}${endpoint}`;
    
    const config = {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.apiKey}`,
        ...options.headers,
      },
      ...options,
    };

    try {
      const response = await fetch(url, config);
      
      // Verificar rate limit
      const rateLimitRemaining = response.headers.get('X-RateLimit-Remaining');
      if (rateLimitRemaining !== null) {
        console.log(`Rate limit restante: ${rateLimitRemaining}`);
      }
      
      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(`API Error ${response.status}: ${errorData.error?.message || 'Erro desconhecido'}`);
      }
      
      return await response.json();
    } catch (error) {
      console.error('Erro na requisição da API:', error);
      throw error;
    }
  }

  // Métodos específicos para cada endpoint
  async getStatus() {
    return this.request('/status');
  }

  async getPlayers() {
    return this.request('/players');
  }

  async getWorld() {
    return this.request('/world');
  }

  async getSystem() {
    return this.request('/system');
  }
}

export default new WebInfoAPIClient();
```

### 2. Hook React Personalizado

Crie um hook `hooks/useWebInfoAPI.js` para facilitar o uso nos componentes:

```javascript
import { useState, useEffect, useCallback } from 'react';
import apiClient from '../lib/api';

export function useWebInfoAPI(endpoint, options = {}) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { autoRefresh = false, refreshInterval = 30000 } = options;

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      
      let result;
      switch (endpoint) {
        case 'status':
          result = await apiClient.getStatus();
          break;
        case 'players':
          result = await apiClient.getPlayers();
          break;
        case 'world':
          result = await apiClient.getWorld();
          break;
        case 'system':
          result = await apiClient.getSystem();
          break;
        default:
          throw new Error(`Endpoint não suportado: ${endpoint}`);
      }
      
      setData(result);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  }, [endpoint]);

  useEffect(() => {
    fetchData();
    
    if (autoRefresh) {
      const interval = setInterval(fetchData, refreshInterval);
      return () => clearInterval(interval);
    }
  }, [fetchData, autoRefresh, refreshInterval]);

  return { data, loading, error, refetch: fetchData };
}

// Hook específico para status do servidor
export function useServerStatus(autoRefresh = true) {
  return useWebInfoAPI('status', { autoRefresh, refreshInterval: 10000 });
}

// Hook específico para lista de jogadores
export function usePlayersList(autoRefresh = true) {
  return useWebInfoAPI('players', { autoRefresh, refreshInterval: 15000 });
}

// Hook específico para informações do mundo
export function useWorldInfo(autoRefresh = true) {
  return useWebInfoAPI('world', { autoRefresh, refreshInterval: 30000 });
}

// Hook específico para telemetria do sistema
export function useSystemInfo(autoRefresh = true) {
  return useWebInfoAPI('system', { autoRefresh, refreshInterval: 20000 });
}
```

## Exemplos de Implementação

### 1. Componente de Status do Servidor

```jsx
import { useServerStatus } from '../hooks/useWebInfoAPI';

export default function ServerStatus() {
  const { data, loading, error } = useServerStatus();

  if (loading) return <div className="animate-pulse">Carregando status...</div>;
  if (error) return <div className="text-red-500">Erro: {error}</div>;

  const { server } = data;

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h2 className="text-xl font-bold mb-4">Status do Servidor</h2>
      
      <div className="grid grid-cols-2 gap-4">
        <div>
          <span className="text-gray-600">Nome:</span>
          <p className="font-semibold">{server.name}</p>
        </div>
        
        <div>
          <span className="text-gray-600">Jogadores:</span>
          <p className="font-semibold">{server.online_players}/{server.max_players}</p>
        </div>
        
        <div>
          <span className="text-gray-600">Versão:</span>
          <p className="font-semibold">{server.version}</p>
        </div>
        
        <div>
          <span className="text-gray-600">Uptime:</span>
          <p className="font-semibold">{formatUptime(server.uptime)}</p>
        </div>
      </div>
    </div>
  );
}

function formatUptime(milliseconds) {
  const seconds = Math.floor(milliseconds / 1000);
  const hours = Math.floor(seconds / 3600);
  const minutes = Math.floor((seconds % 3600) / 60);
  return `${hours}h ${minutes}m`;
}
```

### 2. Lista de Jogadores Online

```jsx
import { usePlayersList } from '../hooks/useWebInfoAPI';

export default function PlayersList() {
  const { data, loading, error, refetch } = usePlayersList();

  if (loading) return <div className="animate-pulse">Carregando jogadores...</div>;
  if (error) return <div className="text-red-500">Erro: {error}</div>;

  const { players } = data;

  return (
    <div className="bg-white rounded-lg shadow p-6">
      <div className="flex justify-between items-center mb-4">
        <h2 className="text-xl font-bold">Jogadores Online ({players.total_players})</h2>
        <button 
          onClick={refetch}
          className="px-3 py-1 bg-blue-500 text-white rounded hover:bg-blue-600"
        >
          Atualizar
        </button>
      </div>
      
      {players.online_players.length === 0 ? (
        <p className="text-gray-500">Nenhum jogador online</p>
      ) : (
        <div className="space-y-2">
          {players.online_players.map((player, index) => (
            <div key={index} className="flex items-center justify-between p-3 bg-gray-50 rounded">
              <div>
                <p className="font-semibold">{player.name}</p>
                <p className="text-sm text-gray-600">
                  Posição: ({player.position.x.toFixed(1)}, {player.position.y.toFixed(1)})
                </p>
              </div>
              <div className="text-right">
                <p className="text-sm text-gray-600">Level {player.player_level}</p>
                <p className="text-xs text-gray-500">{player.player_class}</p>
                <div className="flex items-center">
                  <div className="w-2 h-2 bg-green-500 rounded-full mr-2"></div>
                  <span className="text-xs text-green-600">Online</span>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
```

### 3. Dashboard Completo

```jsx
import { useServerStatus, usePlayersList, useWorldInfo, useSystemInfo } from '../hooks/useWebInfoAPI';

export default function ServerDashboard() {
  const serverStatus = useServerStatus();
  const playersList = usePlayersList();
  const worldInfo = useWorldInfo();
  const systemInfo = useSystemInfo();

  return (
    <div className="min-h-screen bg-gray-100 p-6">
      <div className="max-w-7xl mx-auto">
        <h1 className="text-3xl font-bold text-gray-900 mb-8">Dashboard do Servidor Necesse</h1>
        
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6 mb-6">
          {/* Status do Servidor */}
          <ServerStatusCard data={serverStatus} />
          
          {/* Informações do Sistema */}
          <SystemInfoCard data={systemInfo} />
        </div>
        
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {/* Lista de Jogadores */}
          <PlayersListCard data={playersList} />
          
          {/* Informações do Mundo */}
          <WorldInfoCard data={worldInfo} />
        </div>
      </div>
    </div>
  );
}

function ServerStatusCard({ data }) {
  const { data: serverData, loading, error } = data;
  
  if (loading) return <CardSkeleton title="Status do Servidor" />;
  if (error) return <CardError title="Status do Servidor" error={error} />;
  
  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h2 className="text-xl font-bold mb-4">Status do Servidor</h2>
      {/* Conteúdo do status */}
    </div>
  );
}

function SystemInfoCard({ data }) {
  const { data: systemData, loading, error } = data;
  
  if (loading) return <CardSkeleton title="Sistema" />;
  if (error) return <CardError title="Sistema" error={error} />;
  
  const { system } = systemData;
  
  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h2 className="text-xl font-bold mb-4">Performance do Sistema</h2>
      
      <div className="space-y-4">
        <div>
          <div className="flex justify-between mb-1">
            <span className="text-sm text-gray-600">CPU</span>
            <span className="text-sm font-medium">{system.cpu.usage_percent.toFixed(1)}%</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div 
              className="bg-blue-600 h-2 rounded-full" 
              style={{ width: `${system.cpu.usage_percent}%` }}
            ></div>
          </div>
        </div>
        
        <div>
          <div className="flex justify-between mb-1">
            <span className="text-sm text-gray-600">Memória</span>
            <span className="text-sm font-medium">{system.memory.usage_percent.toFixed(1)}%</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div 
              className="bg-green-600 h-2 rounded-full" 
              style={{ width: `${system.memory.usage_percent}%` }}
            ></div>
          </div>
        </div>
        
        <div>
          <div className="flex justify-between mb-1">
            <span className="text-sm text-gray-600">JVM Heap</span>
            <span className="text-sm font-medium">{system.jvm.heap_usage_percent.toFixed(1)}%</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div 
              className="bg-purple-600 h-2 rounded-full" 
              style={{ width: `${system.jvm.heap_usage_percent}%` }}
            ></div>
          </div>
        </div>
      </div>
    </div>
  );
}

function CardSkeleton({ title }) {
  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h2 className="text-xl font-bold mb-4">{title}</h2>
      <div className="animate-pulse space-y-3">
        <div className="h-4 bg-gray-200 rounded w-3/4"></div>
        <div className="h-4 bg-gray-200 rounded w-1/2"></div>
        <div className="h-4 bg-gray-200 rounded w-5/6"></div>
      </div>
    </div>
  );
}

function CardError({ title, error }) {
  return (
    <div className="bg-white rounded-lg shadow p-6">
      <h2 className="text-xl font-bold mb-4">{title}</h2>
      <div className="text-red-500 text-sm">Erro: {error}</div>
    </div>
  );
}
```

## Configuração e Variáveis de Ambiente

### 1. Arquivo `.env.local`

Crie um arquivo `.env.local` na raiz do seu projeto Next.js:

```env
# API Configuration
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080/api
NEXT_PUBLIC_API_KEY=your-secure-api-key-here

# Optional: Custom refresh intervals (in milliseconds)
NEXT_PUBLIC_STATUS_REFRESH_INTERVAL=10000
NEXT_PUBLIC_PLAYERS_REFRESH_INTERVAL=15000
NEXT_PUBLIC_WORLD_REFRESH_INTERVAL=30000
NEXT_PUBLIC_SYSTEM_REFRESH_INTERVAL=20000
```

### 2. Configuração do CORS no Servidor

Para que o navegador permita as requisições, certifique-se de que o servidor Necesse com o mod WebInfoAPI esteja configurado com os headers CORS corretos. O mod já inclui suporte para:

- `Access-Control-Allow-Origin: *` (ou domínio específico)
- `Access-Control-Allow-Methods: GET, OPTIONS`
- `Access-Control-Allow-Headers: Content-Type, Authorization`
- `Access-Control-Expose-Headers: X-RateLimit-Remaining`

### 3. Configuração de Produção

Para ambiente de produção, atualize as variáveis:

```env
# Production API Configuration
NEXT_PUBLIC_API_BASE_URL=https://seu-servidor.com:8080/api
NEXT_PUBLIC_API_KEY=sua-chave-api-segura-de-producao
```

### Hook React Básico

```typescript
// hooks/useNeceseServer.ts
import { useState, useEffect } from 'react';

interface ServerStatus {
  online: boolean;
  players: number;
  max_players: number;
  world_time: string;
  uptime: number;
  server_version: string;
  api_version: string;
}

interface Player {
  name: string;
  latency: number;
  level: number;
  x: number;
  y: number;
  health: number;
  max_health: number;
  mana: number;
  max_mana: number;
  online_time: number;
}

interface PlayersData {
  count: number;
  players: Player[];
}

export const useNeceseServer = (baseUrl: string) => {
  const [status, setStatus] = useState<ServerStatus | null>(null);
  const [players, setPlayers] = useState<PlayersData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchData = async () => {
    try {
      setLoading(true);
      setError(null);

      // Fetch server status
      const statusResponse = await fetch(`${baseUrl}/status`);
      if (!statusResponse.ok) throw new Error('Failed to fetch status');
      const statusData = await statusResponse.json();
      setStatus(statusData);

      // Fetch players
      const playersResponse = await fetch(`${baseUrl}/players`);
      if (!playersResponse.ok) throw new Error('Failed to fetch players');
      const playersData = await playersResponse.json();
      setPlayers(playersData);

    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
    const interval = setInterval(fetchData, 5000); // Update every 5 seconds
    return () => clearInterval(interval);
  }, [baseUrl]);

  return { status, players, loading, error, refetch: fetchData };
};
```

### Componente React de Exemplo

```tsx
// components/ServerDashboard.tsx
import React from 'react';
import { useNeceseServer } from '../hooks/useNeceseServer';

const ServerDashboard: React.FC = () => {
  const { status, players, loading, error } = useNeceseServer('http://localhost:8080');

  if (loading) return <div>Carregando...</div>;
  if (error) return <div>Erro: {error}</div>;

  return (
    <div className="server-dashboard">
      <h1>Status do Servidor Necesse</h1>
      
      {status && (
        <div className="server-status">
          <h2>Informações Gerais</h2>
          <p>Status: {status.online ? 'Online' : 'Offline'}</p>
          <p>Jogadores: {status.players}/{status.max_players}</p>
          <p>Hora do Mundo: {status.world_time}</p>
          <p>Uptime: {Math.floor(status.uptime / 1000 / 60)} minutos</p>
          <p>Versão: {status.server_version}</p>
        </div>
      )}

      {players && (
        <div className="players-list">
          <h2>Jogadores Online ({players.count})</h2>
          {players.players.map((player, index) => (
            <div key={index} className="player-card">
              <h3>{player.name}</h3>
              <p>Nível: {player.level}</p>
              <p>Posição: ({player.x.toFixed(1)}, {player.y.toFixed(1)})</p>
              <p>Vida: {player.health}/{player.max_health}</p>
              <p>Mana: {player.mana}/{player.max_mana}</p>
              <p>Ping: {player.latency}ms</p>
              <p>Online: {Math.floor(player.online_time / 1000 / 60)} min</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default ServerDashboard;
```

### Hook Avançado com Múltiplos Endpoints

```typescript
// hooks/useNeceseServerAdvanced.ts
import { useState, useEffect, useCallback } from 'react';

interface ServerInfo {
  server_name: string;
  server_version: string;
  max_players: number;
  current_players: number;
  world_name: string;
  difficulty: string;
  pvp_enabled: boolean;
  server_port: number;
  uptime: number;
  tps: number;
}

interface WorldInfo {
  world_name: string;
  world_time: string;
  world_day: number;
  world_seed: number;
  biome_count: number;
  world_size: { width: number; height: number };
  spawn_point: { x: number; y: number };
  weather: string;
  season: string;
}

export const useNeceseServerAdvanced = (baseUrl: string, refreshInterval = 5000) => {
  const [serverInfo, setServerInfo] = useState<ServerInfo | null>(null);
  const [worldInfo, setWorldInfo] = useState<WorldInfo | null>(null);
  const [isHealthy, setIsHealthy] = useState<boolean>(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const fetchAllData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);

      // Health check
      const healthResponse = await fetch(`${baseUrl}/health`);
      setIsHealthy(healthResponse.ok);

      // Server info
      const serverResponse = await fetch(`${baseUrl}/server-info`);
      if (serverResponse.ok) {
        const serverData = await serverResponse.json();
        setServerInfo(serverData);
      }

      // World info
      const worldResponse = await fetch(`${baseUrl}/world-info`);
      if (worldResponse.ok) {
        const worldData = await worldResponse.json();
        setWorldInfo(worldData);
      }

    } catch (err) {
      setError(err instanceof Error ? err.message : 'Unknown error');
      setIsHealthy(false);
    } finally {
      setLoading(false);
    }
  }, [baseUrl]);

  useEffect(() => {
    fetchAllData();
    const interval = setInterval(fetchAllData, refreshInterval);
    return () => clearInterval(interval);
  }, [fetchAllData, refreshInterval]);

  return {
    serverInfo,
    worldInfo,
    isHealthy,
    loading,
    error,
    refetch: fetchAllData
  };
};
```

## Considerações de Performance e Boas Práticas

### 1. Rate Limiting
- A API tem limite de **60 requisições por minuto** por IP
- Use intervalos de refresh apropriados para cada tipo de dados
- Monitore o header `X-RateLimit-Remaining` nas respostas

### 2. Otimizações Recomendadas

```javascript
// Use React.memo para evitar re-renders desnecessários
import React from 'react';

const PlayerCard = React.memo(({ player }) => {
  return (
    <div className="player-card">
      <h3>{player.name}</h3>
      <p>Level: {player.player_level}</p>
    </div>
  );
});

// Use useMemo para cálculos pesados
import { useMemo } from 'react';

function PlayersStats({ players }) {
  const stats = useMemo(() => {
    return {
      totalPlayers: players.length,
      averageLevel: players.reduce((sum, p) => sum + p.player_level, 0) / players.length,
      maxLevel: Math.max(...players.map(p => p.player_level))
    };
  }, [players]);

  return <div>{/* Render stats */}</div>;
}
```

### 3. Tratamento de Erros Robusto

```javascript
// Hook com retry automático
import { useState, useEffect, useCallback } from 'react';

export function useWebInfoAPIWithRetry(endpoint, options = {}) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [retryCount, setRetryCount] = useState(0);
  
  const { maxRetries = 3, retryDelay = 5000 } = options;

  const fetchData = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      
      const result = await apiClient.request(`/${endpoint}`);
      setData(result);
      setRetryCount(0); // Reset retry count on success
      
    } catch (err) {
      console.error(`Erro ao buscar ${endpoint}:`, err);
      
      if (retryCount < maxRetries) {
        setRetryCount(prev => prev + 1);
        setTimeout(fetchData, retryDelay);
      } else {
        setError(err.message);
      }
    } finally {
      setLoading(false);
    }
  }, [endpoint, retryCount, maxRetries, retryDelay]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  return { data, loading, error, refetch: fetchData, retryCount };
}
```

### 4. Caching com React Query (Opcional)

Para aplicações mais complexas, considere usar React Query:

```bash
npm install @tanstack/react-query
```

```javascript
import { useQuery } from '@tanstack/react-query';
import apiClient from '../lib/api';

export function useServerStatusQuery() {
  return useQuery({
    queryKey: ['serverStatus'],
    queryFn: () => apiClient.getStatus(),
    refetchInterval: 10000, // Refetch a cada 10 segundos
    staleTime: 5000, // Dados são considerados "fresh" por 5 segundos
    retry: 3,
    retryDelay: attemptIndex => Math.min(1000 * 2 ** attemptIndex, 30000),
  });
}
```

### Frequência de Requisições
- **Recomendado:** 5-10 segundos para dados gerais
- **Mínimo:** 1 segundo (evite requisições muito frequentes)
- **Dados de jogadores:** Podem ser atualizados com mais frequência (2-5 segundos)

### Exemplo com Cache

```typescript
// hooks/useNeceseServerWithCache.ts
import { useState, useEffect, useMemo } from 'react';

export const useNeceseServerWithCache = (baseUrl: string) => {
  const [data, setData] = useState(null);
  const [lastFetch, setLastFetch] = useState(0);
  
  const cachedData = useMemo(() => {
    // Process and cache expensive calculations
    if (!data) return null;
    
    return {
      ...data,
      uptimeFormatted: formatUptime(data.uptime),
      playersOnlinePercentage: (data.players / data.max_players) * 100
    };
  }, [data]);

  // ... rest of the hook logic
  
  return cachedData;
};

const formatUptime = (uptime: number): string => {
  const hours = Math.floor(uptime / 1000 / 60 / 60);
  const minutes = Math.floor((uptime / 1000 / 60) % 60);
  return `${hours}h ${minutes}m`;
};
```

## Segurança

### 1. Proteção da API Key
- **NUNCA** commite a API key no código
- Use variáveis de ambiente (`NEXT_PUBLIC_API_KEY`)
- Em produção, considere implementar rotação de chaves

### 2. Validação de Dados
```javascript
// Validação básica dos dados recebidos
function validateServerData(data) {
  if (!data || typeof data !== 'object') {
    throw new Error('Dados inválidos recebidos da API');
  }
  
  if (!data.api_version || !data.timestamp) {
    throw new Error('Estrutura de resposta inválida');
  }
  
  return data;
}

// Use no cliente API
async request(endpoint, options = {}) {
  // ... código existente ...
  
  const data = await response.json();
  return validateServerData(data);
}
```

### 3. Sanitização de Dados
```javascript
// Sanitize player names para prevenir XSS
function sanitizePlayerName(name) {
  return name.replace(/[<>\"'&]/g, '');
}

// Use nos componentes
<p className="font-semibold">{sanitizePlayerName(player.name)}</p>
```

## Troubleshooting

### Problemas Comuns

1. **Erro CORS**: Verifique se o servidor está configurado corretamente
2. **401 Unauthorized**: Verifique a API key nas variáveis de ambiente
3. **429 Rate Limited**: Reduza a frequência das requisições
4. **Conexão recusada**: Verifique se o servidor Necesse está rodando na porta correta

### Debug Mode

```javascript
// Adicione logs detalhados em desenvolvimento
const DEBUG = process.env.NODE_ENV === 'development';

class WebInfoAPIClient {
  async request(endpoint, options = {}) {
    if (DEBUG) {
      console.log(`[API] Requesting: ${this.baseURL}${endpoint}`);
    }
    
    // ... resto do código ...
    
    if (DEBUG) {
      console.log(`[API] Response:`, data);
    }
    
    return data;
  }
}
```

## Próximos Passos

1. **Teste a API:** Use ferramentas como Postman ou curl para testar os endpoints
2. **Implemente gradualmente:** Comece com endpoints básicos e expanda conforme necessário
3. **Monitore performance:** Observe o impacto das requisições no servidor
4. **Considere autenticação:** Para ambientes de produção, implemente autenticação se necessário

## Suporte

Para dúvidas ou problemas com a integração, verifique:
1. Se o mod está instalado e configurado corretamente
2. Se a porta da API está acessível
3. Se não há bloqueios de firewall
4. Os logs do servidor Necesse para erros relacionados ao mod

---

**Versão da Documentação:** 1.0.0  
**Compatível com:** WebInfoAPI v1.0.0  
**Última atualização:** Janeiro 2024