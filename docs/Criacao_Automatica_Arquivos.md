# Criação Automática de Arquivos de Configuração - WebInfoAPI

## Visão Geral

O WebInfoAPI foi aprimorado para criar automaticamente os arquivos de configuração necessários quando o servidor Necesse é iniciado. Esta funcionalidade garante que os administradores tenham acesso fácil às configurações do mod sem precisar criar manualmente os arquivos.

## Funcionalidades Implementadas

### 1. Criação Automática de Diretório
- **Localização**: `%APPDATA%\Necesse\cfg\mods\`
- **Comportamento**: O diretório é criado automaticamente se não existir
- **Compatibilidade**: Segue o padrão de configuração do Necesse

### 2. Geração do Arquivo de Configuração
- **Arquivo**: `webapi.properties`
- **Localização**: `%APPDATA%\Necesse\cfg\mods\webapi.properties`
- **Conteúdo**: Configurações padrão com comentários explicativos

### 3. Proteção de Arquivos Existentes
- **Comportamento**: Não sobrescreve arquivos existentes
- **Vantagem**: Preserva configurações personalizadas dos administradores

## Estrutura do Arquivo de Configuração

O arquivo `webapi.properties` criado automaticamente contém:

```properties
# Configurações básicas da API
api.enabled=true
api.port=8080
api.host=0.0.0.0

# Configurações de CORS
api.cors.enabled=true
api.cors.allowed_origins=*
api.cors.allowed_methods=GET, POST, OPTIONS
api.cors.allowed_headers=Content-Type, Authorization

# Configurações de Rate Limiting
api.rate_limit.enabled=true
api.rate_limit.requests_per_minute=60

# Configurações de Thread Pool
api.thread_pool.core_size=5
api.thread_pool.max_size=20

# Configurações de Logging
api.logging.enabled=true
api.logging.requests=false

# Controle individual de endpoints
endpoints.status.enabled=true
endpoints.players.enabled=true
endpoints.server_info.enabled=true
endpoints.world_info.enabled=true
endpoints.health.enabled=true

# Configurações de segurança
api.security.require_auth=false
api.security.api_key=
```

## Implementação Técnica

### Métodos Adicionados ao WebInfoAPI.java

#### `createConfigurationFiles()`
- **Função**: Coordena a criação do diretório e arquivo de configuração
- **Chamada**: Executada automaticamente no `initSettings()`
- **Logs**: Registra todas as operações para debugging

#### `createDefaultPropertiesFile(Path configFilePath)`
- **Função**: Cria o arquivo `webapi.properties` com configurações padrão
- **Parâmetros**: Caminho onde o arquivo será criado
- **Conteúdo**: Todas as configurações essenciais com valores padrão

#### `loadExternalConfig()` (Modificado)
- **Prioridade**: Carrega primeiro do diretório Necesse, depois do classpath
- **Fallback**: Usa configurações padrão se nenhum arquivo for encontrado
- **Flexibilidade**: Suporta múltiplas localizações de configuração

## Fluxo de Inicialização

1. **Início do Servidor**: Necesse inicia e carrega o mod WebInfoAPI
2. **initSettings()**: Método chamado automaticamente pelo Necesse
3. **createConfigurationFiles()**: Verifica e cria arquivos necessários
4. **loadExternalConfig()**: Carrega configurações do arquivo criado
5. **Inicialização da API**: API inicia com configurações carregadas

## Testes Realizados

### Teste 1: Criação Básica de Arquivos
- ✅ Diretório criado corretamente
- ✅ Arquivo de configuração gerado
- ✅ Conteúdo válido no arquivo

### Teste 2: Proteção de Arquivos Existentes
- ✅ Arquivos existentes não são sobrescritos
- ✅ Timestamps preservados
- ✅ Configurações personalizadas mantidas

### Teste 3: Carregamento de Configuração
- ✅ Propriedades carregadas corretamente
- ✅ Valores padrão aplicados
- ✅ Todas as configurações essenciais presentes

### Teste 4: Integração Completa
- ✅ Simulação completa do processo de inicialização
- ✅ Criação e carregamento em sequência
- ✅ Validação de todas as propriedades críticas

## Benefícios para Administradores

### 1. Facilidade de Uso
- Não é necessário criar arquivos manualmente
- Configurações aparecem automaticamente na primeira execução
- Comentários explicativos em cada configuração

### 2. Personalização Simples
- Arquivo de texto simples e editável
- Localização padrão conhecida do Necesse
- Mudanças aplicadas na próxima reinicialização

### 3. Segurança
- Configurações personalizadas nunca são perdidas
- Backup automático através do sistema de versionamento
- Valores padrão seguros para produção

## Localização dos Arquivos

### Windows
```
%APPDATA%\Necesse\cfg\mods\webapi.properties
```

### Linux/Mac
```
~/.necesse/cfg/mods/webapi.properties
```

## Logs de Depuração

O sistema registra as seguintes informações nos logs:

- Criação de diretório de configuração
- Geração de arquivo de configuração
- Carregamento de configurações externas
- Erros de I/O ou permissões

## Compatibilidade

- **Necesse**: Todas as versões que suportam ModSettings
- **Java**: Java 8 ou superior
- **Sistemas**: Windows, Linux, macOS
- **Permissões**: Requer acesso de escrita ao diretório de configuração

## Resolução de Problemas

### Arquivo não é criado
1. Verificar permissões de escrita no diretório
2. Confirmar que o diretório %APPDATA%\Necesse existe
3. Verificar logs do servidor para erros específicos

### Configurações não são aplicadas
1. Verificar sintaxe do arquivo .properties
2. Reiniciar o servidor após mudanças
3. Confirmar que o arquivo está na localização correta

### Arquivo é recriado constantemente
1. Verificar se há processos externos modificando o arquivo
2. Confirmar permissões de leitura no arquivo
3. Verificar logs para erros de carregamento

## Conclusão

A funcionalidade de criação automática de arquivos de configuração torna o WebInfoAPI mais acessível e fácil de usar para administradores de servidor Necesse. O sistema é robusto, seguro e segue as melhores práticas de desenvolvimento de mods para Necesse.

---

**Versão**: 1.0  
**Data**: 2024  
**Autor**: WebInfoAPI Development Team