# WebInfoAPI - Configuração Externa

## Visão Geral

O mod **WebInfoAPI** agora suporta configuração externa através do arquivo `webapi.properties`. Este arquivo permite personalizar o comportamento da API sem precisar recompilar o mod.

## Localização do Arquivo

O arquivo de configuração deve estar localizado em:
```
src/main/resources/webapi.properties
```

Quando o mod for compilado, este arquivo será incluído no JAR e carregado automaticamente.

## Arquivo de Configuração Completo

```properties
# WebInfoAPI - Configuração Externa
# Este arquivo permite configurar o mod sem precisar recompilar

# Configurações da API
api.enabled=true
api.port=8080
api.host=0.0.0.0

# Configurações de Segurança
api.cors.enabled=true
api.cors.allowed_origins=*
api.rate_limit.enabled=false
api.rate_limit.requests_per_minute=60

# Configurações de Performance
api.thread_pool.core_size=4
api.thread_pool.max_size=8
api.thread_pool.keep_alive_seconds=60

# Configurações de Logging
api.logging.enabled=true
api.logging.level=INFO
api.logging.requests=false

# Configurações de Cache
api.cache.enabled=false
api.cache.ttl_seconds=30

# Configurações de Dados
api.data.include_player_positions=true
api.data.include_player_stats=true
api.data.include_world_details=true
api.data.update_interval_ms=1000

# Configurações de Endpoints
endpoints.status.enabled=true
endpoints.players.enabled=true
endpoints.server_info.enabled=true
endpoints.world_info.enabled=true
endpoints.health.enabled=true
```

## Descrição das Configurações

### Configurações da API

| Propriedade | Tipo | Padrão | Descrição |
|-------------|------|--------|-----------|
| `api.enabled` | boolean | `true` | Habilita/desabilita a API completamente |
| `api.port` | int | `8080` | Porta onde a API será executada |
| `api.host` | string | `0.0.0.0` | Endereço IP para bind (0.0.0.0 = todas as interfaces) |

### Configurações de Segurança

| Propriedade | Tipo | Padrão | Descrição |
|-------------|------|--------|-----------|
| `api.cors.enabled` | boolean | `true` | Habilita headers CORS |
| `api.cors.allowed_origins` | string | `*` | Origens permitidas para CORS |
| `api.rate_limit.enabled` | boolean | `false` | Habilita limitação de taxa (futuro) |
| `api.rate_limit.requests_per_minute` | int | `60` | Limite de requisições por minuto |

### Configurações de Performance

| Propriedade | Tipo | Padrão | Descrição |
|-------------|------|--------|-----------|
| `api.thread_pool.core_size` | int | `4` | Número mínimo de threads |
| `api.thread_pool.max_size` | int | `8` | Número máximo de threads |
| `api.thread_pool.keep_alive_seconds` | int | `60` | Tempo de vida das threads ociosas |

### Configurações de Logging

| Propriedade | Tipo | Padrão | Descrição |
|-------------|------|--------|-----------|
| `api.logging.enabled` | boolean | `true` | Habilita logs da API |
| `api.logging.level` | string | `INFO` | Nível de log (futuro) |
| `api.logging.requests` | boolean | `false` | Log de todas as requisições (futuro) |

### Configurações de Cache

| Propriedade | Tipo | Padrão | Descrição |
|-------------|------|--------|-----------|
| `api.cache.enabled` | boolean | `false` | Habilita cache de respostas (futuro) |
| `api.cache.ttl_seconds` | int | `30` | Tempo de vida do cache em segundos |

### Configurações de Dados

| Propriedade | Tipo | Padrão | Descrição |
|-------------|------|--------|-----------|
| `api.data.include_player_positions` | boolean | `true` | Inclui posições dos jogadores (futuro) |
| `api.data.include_player_stats` | boolean | `true` | Inclui estatísticas dos jogadores (futuro) |
| `api.data.include_world_details` | boolean | `true` | Inclui detalhes do mundo (futuro) |
| `api.data.update_interval_ms` | int | `1000` | Intervalo de atualização dos dados |

### Configurações de Endpoints

| Propriedade | Tipo | Padrão | Descrição |
|-------------|------|--------|-----------|
| `endpoints.status.enabled` | boolean | `true` | Habilita endpoint `/status` |
| `endpoints.players.enabled` | boolean | `true` | Habilita endpoint `/players` |
| `endpoints.server_info.enabled` | boolean | `true` | Habilita endpoint `/server-info` |
| `endpoints.world_info.enabled` | boolean | `true` | Habilita endpoint `/world-info` |
| `endpoints.health.enabled` | boolean | `true` | Habilita endpoint `/health` |

## Como Usar

### 1. Configuração Básica
Para uma configuração simples, você pode usar apenas:

```properties
api.enabled=true
api.port=9090
api.host=127.0.0.1
```

### 2. Configuração de Produção
Para um ambiente de produção:

```properties
# API básica
api.enabled=true
api.port=8080
api.host=0.0.0.0

# Segurança
api.cors.enabled=true
api.cors.allowed_origins=https://meusite.com,https://www.meusite.com

# Performance otimizada
api.thread_pool.core_size=8
api.thread_pool.max_size=16
api.thread_pool.keep_alive_seconds=120

# Logging detalhado
api.logging.enabled=true
api.logging.requests=true
```

### 3. Configuração de Desenvolvimento
Para desenvolvimento local:

```properties
# API para desenvolvimento
api.enabled=true
api.port=3001
api.host=localhost

# CORS liberado
api.cors.enabled=true
api.cors.allowed_origins=*

# Logging completo
api.logging.enabled=true
api.logging.requests=true
```

### 4. Desabilitar Endpoints Específicos
Para desabilitar endpoints que você não precisa:

```properties
# Manter apenas status e players
endpoints.status.enabled=true
endpoints.players.enabled=true
endpoints.server_info.enabled=false
endpoints.world_info.enabled=false
endpoints.health.enabled=false
```

## Configurações do Mod vs Arquivo Externo

O mod possui duas camadas de configuração:

### 1. **Configurações do Mod** (Interface do Necesse)
- `api_port`: Porta da API
- `api_enabled`: Habilitar/desabilitar API
- `api_host`: Host da API
- `logging_enabled`: Habilitar logs

### 2. **Arquivo Externo** (webapi.properties)
- Configurações avançadas
- Controle granular de endpoints
- Configurações de performance
- Opções de segurança

### Prioridade
1. **Arquivo externo** define valores padrão
2. **Configurações do mod** sobrescrevem os valores do arquivo
3. Se não houver arquivo externo, usa valores padrão do código

## Exemplo de Uso Prático

### Cenário: Servidor de Produção com Múltiplos Sites

```properties
# Configuração para servidor de produção
api.enabled=true
api.port=8080
api.host=0.0.0.0

# Permitir apenas sites específicos
api.cors.enabled=true
api.cors.allowed_origins=https://site1.com,https://site2.com,https://painel.servidor.com

# Performance para servidor com muitos jogadores
api.thread_pool.core_size=6
api.thread_pool.max_size=12
api.thread_pool.keep_alive_seconds=300

# Desabilitar endpoints desnecessários
endpoints.world_info.enabled=false
endpoints.health.enabled=false

# Logging moderado
api.logging.enabled=true
api.logging.requests=false
```

## Troubleshooting

### Arquivo não carregado
Se você ver a mensagem: `"WebInfoAPI: Arquivo webapi.properties não encontrado"`
- Verifique se o arquivo está em `src/main/resources/webapi.properties`
- Recompile o mod para incluir o arquivo no JAR

### Configurações não aplicadas
- Verifique a sintaxe do arquivo (chave=valor)
- Não use espaços ao redor do `=`
- Valores booleanos devem ser `true` ou `false` (minúsculas)
- Valores numéricos devem ser números válidos

### Logs de Debug
Para ver se as configurações estão sendo carregadas:
```properties
api.logging.enabled=true
```

Isso mostrará no console do servidor:
```
WebInfoAPI: Configuração externa carregada com sucesso!
WebInfoAPI iniciado em 0.0.0.0:8080
```

## Futuras Implementações

Algumas configurações estão preparadas para futuras versões:
- Rate limiting
- Cache de respostas
- Logs detalhados de requisições
- Controle granular de dados expostos
- Autenticação por API key

---

**Versão:** 1.0.0  
**Compatível com:** WebInfoAPI v1.0.0+  
**Última atualização:** Janeiro 2024