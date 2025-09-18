package com.meuservidor.webapi;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.File;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.management.GarbageCollectorMXBean;

// Importações OSHI para telemetria do sistema
import oshi.SystemInfo;
import oshi.hardware.CentralProcessor;
import oshi.hardware.GlobalMemory;
import oshi.hardware.HardwareAbstractionLayer;
import oshi.software.os.OperatingSystem;
import necesse.engine.GameEvents;
import necesse.engine.events.ServerStartEvent;
import necesse.engine.events.ServerStopEvent;
import necesse.engine.modLoader.annotations.ModEntry;
import necesse.engine.modLoader.ModSettings;
import necesse.engine.modLoader.setting.IntModSetting;
import necesse.engine.modLoader.setting.BooleanModSetting;
import necesse.engine.modLoader.setting.StringModSetting;
import necesse.engine.server.Server;
import necesse.engine.server.player.ServerPlayer;
import necesse.level.maps.biomes.Biome;

@ModEntry
public class WebInfoAPI {

    private static Server server;
    private HttpServer httpServer;
    private Properties config;
    private boolean apiEnabled = true;
    private boolean loggingEnabled = true;
    
    // Rate limiting e autenticação
    private final Map<String, Long> rateLimitMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> requestCountMap = new ConcurrentHashMap<>();

    public ModSettings initSettings() {
        // Criar arquivos de configuração automaticamente
        createConfigurationFiles();
        
        loadExternalConfig();
        
        return new ModSettings() {
            {
                addSetting(new IntModSetting("api_port", getConfigInt("api.port", 8080), (value) -> {
                    return "A porta deve ser uma das alocadas pelo seu provedor de hospedagem.";
                }));
                addSetting(new BooleanModSetting("api_enabled", getConfigBoolean("api.enabled", true), (value) -> {
                    return "Habilita ou desabilita a API completamente.";
                }));
                addSetting(new StringModSetting("api_host", getConfigString("api.host", "0.0.0.0"), (value) -> {
                    return "Endereço IP para bind da API (0.0.0.0 para todas as interfaces).";
                }));
                addSetting(new BooleanModSetting("logging_enabled", getConfigBoolean("api.logging.enabled", true), (value) -> {
                    return "Habilita logs detalhados da API.";
                }));
            }
        };
    }

    /**
     * Cria automaticamente os arquivos de configuração necessários
     */
    private void createConfigurationFiles() {
        try {
            // Determinar diretório de configuração do Necesse
            String userHome = System.getProperty("user.home");
            Path necesseConfigDir = Paths.get(userHome, "AppData", "Roaming", "Necesse", "cfg", "mods");
            
            // Criar diretório se não existir
            if (!Files.exists(necesseConfigDir)) {
                Files.createDirectories(necesseConfigDir);
                System.out.println("WebInfoAPI: Diretório de configuração criado: " + necesseConfigDir);
            }
            
            // Caminho para o arquivo de configuração
            Path configFilePath = necesseConfigDir.resolve("webapi.properties");
            
            // Verificar se arquivo já existe
            if (!Files.exists(configFilePath)) {
                createDefaultPropertiesFile(configFilePath);
                System.out.println("WebInfoAPI: Arquivo de configuração criado: " + configFilePath);
            } else {
                System.out.println("WebInfoAPI: Arquivo de configuração já existe: " + configFilePath);
            }
            
        } catch (IOException e) {
            System.err.println("WebInfoAPI: Erro ao criar arquivos de configuração: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Cria o arquivo webapi.properties com configurações padrão
     */
    private void createDefaultPropertiesFile(Path configFilePath) throws IOException {
        Properties defaultProps = new Properties();
        
        // Configurações básicas da API
        defaultProps.setProperty("api.enabled", "true");
        defaultProps.setProperty("api.port", "8080");
        defaultProps.setProperty("api.host", "0.0.0.0");
        
        // Configurações de CORS
        defaultProps.setProperty("api.cors.enabled", "true");
        defaultProps.setProperty("api.cors.allowed_origins", "*");
        defaultProps.setProperty("api.cors.allowed_methods", "GET, POST, OPTIONS");
        defaultProps.setProperty("api.cors.allowed_headers", "Content-Type, Authorization, X-API-Key");
        defaultProps.setProperty("api.cors.max_age", "3600");
        
        // Configurações de rate limiting
        defaultProps.setProperty("api.rate_limit.enabled", "true");
        defaultProps.setProperty("api.rate_limit.requests_per_minute", "60");
        defaultProps.setProperty("api.rate_limit.burst_size", "10");
        
        // Configurações de thread pool
        defaultProps.setProperty("api.thread_pool.core_size", "4");
        defaultProps.setProperty("api.thread_pool.max_size", "8");
        defaultProps.setProperty("api.thread_pool.keep_alive_seconds", "60");
        
        // Configurações de logging
        defaultProps.setProperty("api.logging.enabled", "true");
        defaultProps.setProperty("api.logging.requests", "true");
        defaultProps.setProperty("api.logging.responses", "false");
        defaultProps.setProperty("api.logging.errors", "true");
        
        // Controle individual de endpoints
        defaultProps.setProperty("endpoints.status.enabled", "true");
        defaultProps.setProperty("endpoints.players.enabled", "true");
        defaultProps.setProperty("endpoints.server_info.enabled", "true");
        defaultProps.setProperty("endpoints.world_info.enabled", "true");
        defaultProps.setProperty("endpoints.system.enabled", "true");
        defaultProps.setProperty("endpoints.health.enabled", "true");
        
        // Configurações de segurança
        defaultProps.setProperty("api.security.api_key", "your-secure-api-key-here");
        defaultProps.setProperty("api.security.require_auth", "false");
        
        // Configurações de cache
        defaultProps.setProperty("api.cache.enabled", "true");
        defaultProps.setProperty("api.cache.ttl_seconds", "30");
        
        // Configurações de dados
        defaultProps.setProperty("data.include_sensitive", "false");
        defaultProps.setProperty("data.include_coordinates", "true");
        defaultProps.setProperty("data.include_player_stats", "true");
        
        // Salvar arquivo com comentários
        try (FileOutputStream output = new FileOutputStream(configFilePath.toFile())) {
            defaultProps.store(output, "WebInfoAPI - Configurações do Mod\n" +
                "# Este arquivo foi criado automaticamente pelo mod WebInfoAPI\n" +
                "# Você pode editar estas configurações conforme necessário\n" +
                "# Reinicie o servidor após fazer alterações\n" +
                "#\n" +
                "# Configurações básicas:\n" +
                "# api.enabled - Habilita/desabilita a API\n" +
                "# api.port - Porta onde a API será executada\n" +
                "# api.host - Endereço IP para bind (0.0.0.0 para todas as interfaces)\n" +
                "#\n" +
                "# Configurações de CORS:\n" +
                "# api.cors.enabled - Habilita suporte a CORS\n" +
                "# api.cors.allowed_origins - Origens permitidas (* para todas)\n" +
                "#\n" +
                "# Configurações de segurança:\n" +
                "# api.security.api_key - Chave de API para autenticação\n" +
                "# api.security.require_auth - Exige autenticação para todos os endpoints\n" +
                "#\n" +
                "# Controle de endpoints:\n" +
                "# endpoints.*.enabled - Habilita/desabilita endpoints específicos\n" +
                "#\n" +
                "# Para mais informações, consulte a documentação do mod");
        }
    }

    private void loadExternalConfig() {
        config = new Properties();
        
        // Primeiro, tentar carregar do diretório de configuração do Necesse
        String userHome = System.getProperty("user.home");
        Path necesseConfigPath = Paths.get(userHome, "AppData", "Roaming", "Necesse", "cfg", "mods", "webapi.properties");
        
        if (Files.exists(necesseConfigPath)) {
            try (FileInputStream input = new FileInputStream(necesseConfigPath.toFile())) {
                config.load(input);
                System.out.println("WebInfoAPI: Configuração externa carregada de: " + necesseConfigPath);
                return;
            } catch (IOException e) {
                System.err.println("WebInfoAPI: Erro ao carregar configuração de " + necesseConfigPath + ": " + e.getMessage());
            }
        }
        
        // Fallback: tentar carregar do classpath (recursos do mod)
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("webapi.properties")) {
            if (input != null) {
                config.load(input);
                System.out.println("WebInfoAPI: Configuração externa carregada do classpath!");
            } else {
                System.out.println("WebInfoAPI: Nenhum arquivo webapi.properties encontrado, usando configurações padrão.");
            }
        } catch (IOException e) {
            System.err.println("WebInfoAPI: Erro ao carregar configuração do classpath: " + e.getMessage());
        }
    }

    private int getConfigInt(String key, int defaultValue) {
        if (config == null) return defaultValue;
        String value = config.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                System.err.println("WebInfoAPI: Valor inválido para " + key + ": " + value);
            }
        }
        return defaultValue;
    }

    private boolean getConfigBoolean(String key, boolean defaultValue) {
        if (config == null) return defaultValue;
        String value = config.getProperty(key);
        return value != null ? Boolean.parseBoolean(value) : defaultValue;
    }

    private String getConfigString(String key, String defaultValue) {
        if (config == null) return defaultValue;
        return config.getProperty(key, defaultValue);
    }

    public void init() {
        GameEvents.addListener(ServerStartEvent.class, (event) -> {
            server = event.server;
        });
    }

    public void postInit() {
        instance = this; // Definir instância estática para acesso aos métodos de configuração
        
        apiEnabled = getSettings().getBoolean("api_enabled");
        loggingEnabled = getSettings().getBoolean("logging_enabled");
        
        if (!apiEnabled) {
            if (loggingEnabled) {
                System.out.println("WebInfoAPI: API desabilitada por configuração.");
            }
            return;
        }

        int apiPort = getSettings().getInt("api_port");
        String apiHost = getSettings().getString("api_host");

        try {
            InetSocketAddress address = new InetSocketAddress(apiHost, apiPort);
            httpServer = HttpServer.create(address, 0);
            
            // Configurar endpoints baseado nas configurações
            if (getConfigBoolean("endpoints.status.enabled", true)) {
                httpServer.createContext("/status", new StatusHandler());
            }
            if (getConfigBoolean("endpoints.players.enabled", true)) {
                httpServer.createContext("/players", new PlayersHandler());
            }
            if (getConfigBoolean("endpoints.server_info.enabled", true)) {
                httpServer.createContext("/server-info", new ServerInfoHandler());
            }
            if (getConfigBoolean("endpoints.world_info.enabled", true)) {
                httpServer.createContext("/world-info", new WorldInfoHandler());
            }
            if (getConfigBoolean("endpoints.system.enabled", true)) {
                httpServer.createContext("/system", new SystemHandler());
            }
            if (getConfigBoolean("endpoints.health.enabled", true)) {
                httpServer.createContext("/health", new HealthHandler());
            }
            
            // Configurar thread pool baseado nas configurações
            int coreSize = getConfigInt("api.thread_pool.core_size", 4);
            int maxSize = getConfigInt("api.thread_pool.max_size", 8);
            int keepAlive = getConfigInt("api.thread_pool.keep_alive_seconds", 60);
            
            httpServer.setExecutor(new java.util.concurrent.ThreadPoolExecutor(
                coreSize, maxSize, keepAlive, java.util.concurrent.TimeUnit.SECONDS, 
                new java.util.concurrent.LinkedBlockingQueue<>()));
            
            Thread serverThread = new Thread(() -> httpServer.start());
            serverThread.setDaemon(true);
            serverThread.start();

            if (loggingEnabled) {
                System.out.println("WebInfoAPI iniciado em " + apiHost + ":" + apiPort);
            }

        } catch (IOException e) {
            System.err.println("Falha ao iniciar o servidor HTTP da WebInfoAPI: " + e.getMessage());
            e.printStackTrace();
        }

        GameEvents.addListener(ServerStopEvent.class, (event) -> {
            dispose();
        });
    }

    public void dispose() {
        if (httpServer != null) {
            httpServer.stop(0);
            System.out.println("WebInfoAPI parado.");
        }
    }

    static class StatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Verificar autenticação se necessário
            if (!authenticateRequest(exchange)) {
                sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }
            
            // Verificar rate limiting
            if (!checkRateLimit(exchange)) {
                sendResponse(exchange, 429, "{\"error\":\"Rate limit exceeded\"}");
                return;
            }
            
            // Tratar requisições OPTIONS para CORS preflight
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, "");
                return;
            }
            
            if (server == null) {
                sendResponse(exchange, 503, "{\"error\":\"Servidor não iniciado\"}");
                return;
            }

            long uptime = System.currentTimeMillis() - server.getStartTime();
            boolean hasPassword = server.getPassword() != null && !server.getPassword().isEmpty();
            String motd = server.getMotd() != null ? server.getMotd() : "";
            
            String jsonResponse = String.format(
                "{\"online\":true,\"players\":%d,\"max_players\":%d,\"world_time\":\"%s\",\"uptime\":%d,\"server_version\":\"%s\",\"api_version\":\"2.0.0\",\"server_name\":\"%s\",\"motd\":\"%s\",\"has_password\":%b,\"pvp_enabled\":%b,\"tps\":%.2f}",
                server.getPlayersOnline(),
                server.getSlots(),
                server.world.worldEntity.getTime().getTimeOfDayString(),
                uptime,
                server.getVersion(),
                server.getServerName(),
                motd,
                hasPassword,
                server.world.worldEntity.isPvPEnabled(),
                server.getTPS()
            );

            sendResponse(exchange, 200, jsonResponse);
        }
    }

    static class PlayersHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Verificar autenticação se necessário
            if (!authenticateRequest(exchange)) {
                sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }
            
            // Verificar rate limiting
            if (!checkRateLimit(exchange)) {
                sendResponse(exchange, 429, "{\"error\":\"Rate limit exceeded\"}");
                return;
            }
            
            // Tratar requisições OPTIONS para CORS preflight
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, "");
                return;
            }
            
            if (server == null) {
                sendResponse(exchange, 503, "{\"error\":\"Servidor não iniciado\"}");
                return;
            }

            StringBuilder jsonResponse = new StringBuilder();
            jsonResponse.append("{\"players\":[");

            boolean first = true;
            for (ServerPlayer player : server.getPlayers()) {
                if (!first) {
                    jsonResponse.append(",");
                }
                first = false;

                // Informações detalhadas do jogador
                int level = player.getLevel() != null ? player.getLevel().getIdentifier() : 0;
                float x = player.x;
                float y = player.y;
                int health = player.getHealth();
                int maxHealth = player.getMaxHealth();
                int mana = player.getMana();
                int maxMana = player.getMaxMana();
                long onlineTime = System.currentTimeMillis() - player.getJoinTime();
                String biome = player.getLevel() != null ? player.getLevel().biome.getStringID() : "unknown";
                boolean isAdmin = player.isServerClient();
                String playerClass = player.playerMob.getStringID();
                int playerLevel = player.getStats().getLevel();
                long experience = player.getStats().getExp();
                
                // Incluir coordenadas apenas se permitido na configuração
                boolean includeCoords = instance.getConfigBoolean("data.include_coordinates", true);
                boolean includeStats = instance.getConfigBoolean("data.include_player_stats", true);

                if (includeCoords && includeStats) {
                    jsonResponse.append(String.format(
                        "{\"name\":\"%s\",\"id\":\"%s\",\"latency\":%d,\"level_id\":%d,\"position\":{\"x\":%.2f,\"y\":%.2f},\"biome\":\"%s\",\"health\":{\"current\":%d,\"max\":%d},\"mana\":{\"current\":%d,\"max\":%d},\"online_time\":%d,\"is_admin\":%b,\"player_class\":\"%s\",\"player_level\":%d,\"experience\":%d}",
                        player.playerName,
                        player.getUUID().toString(),
                        player.latency,
                        level,
                        x, y,
                        biome,
                        health, maxHealth,
                        mana, maxMana,
                        onlineTime,
                        isAdmin,
                        playerClass,
                        playerLevel,
                        experience
                    ));
                } else if (includeCoords) {
                    jsonResponse.append(String.format(
                        "{\"name\":\"%s\",\"id\":\"%s\",\"latency\":%d,\"level_id\":%d,\"position\":{\"x\":%.2f,\"y\":%.2f},\"biome\":\"%s\",\"online_time\":%d,\"is_admin\":%b,\"player_class\":\"%s\",\"player_level\":%d}",
                        player.playerName,
                        player.getUUID().toString(),
                        player.latency,
                        level,
                        x, y,
                        biome,
                        onlineTime,
                        isAdmin,
                        playerClass,
                        playerLevel
                    ));
                } else {
                    jsonResponse.append(String.format(
                        "{\"name\":\"%s\",\"id\":\"%s\",\"latency\":%d,\"level_id\":%d,\"online_time\":%d,\"is_admin\":%b,\"player_class\":\"%s\",\"player_level\":%d}",
                        player.playerName,
                        player.getUUID().toString(),
                        player.latency,
                        level,
                        onlineTime,
                        isAdmin,
                        playerClass,
                        playerLevel
                    ));
                }
            }

            jsonResponse.append("],\"total_players\":").append(server.getPlayersOnline()).append("}");
            sendResponse(exchange, 200, jsonResponse.toString());
        }
    }

    static class ServerInfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Tratar requisições OPTIONS para CORS preflight
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, "");
                return;
            }
            
            if (server == null) {
                sendResponse(exchange, 503, "{\"error\":\"Servidor não iniciado\"}");
                return;
            }

            String jsonResponse = String.format(
                "{\"server_name\":\"%s\",\"server_version\":\"%s\",\"max_players\":%d,\"current_players\":%d,\"world_name\":\"%s\",\"difficulty\":\"%s\",\"pvp_enabled\":%b,\"server_port\":%d,\"uptime\":%d,\"tps\":%.2f}",
                server.getServerName(),
                server.getVersion(),
                server.getSlots(),
                server.getPlayersOnline(),
                server.world.worldEntity.getWorldName(),
                server.world.worldEntity.getDifficulty().name(),
                server.world.worldEntity.isPvPEnabled(),
                server.getPort(),
                System.currentTimeMillis() - server.getStartTime(),
                server.getTPS()
            );

            sendResponse(exchange, 200, jsonResponse);
        }
    }

    static class WorldInfoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Verificar autenticação se necessário
            if (!authenticateRequest(exchange)) {
                sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }
            
            // Verificar rate limiting
            if (!checkRateLimit(exchange)) {
                sendResponse(exchange, 429, "{\"error\":\"Rate limit exceeded\"}");
                return;
            }
            
            // Tratar requisições OPTIONS para CORS preflight
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, "");
                return;
            }
            
            if (server == null) {
                sendResponse(exchange, 503, "{\"error\":\"Servidor não iniciado\"}");
                return;
            }

            // Obter informações do mundo usando a API correta do Necesse
            String worldName = server.world.worldEntity.getWorldName();
            String worldTime = server.world.worldEntity.getTime().getTimeOfDayString();
            int worldDay = server.world.worldEntity.getTime().getDay();
            long worldSeed = server.world.worldEntity.getSeed();
            int biomeCount = server.world.biomeManager.getBiomes().size();
            int worldWidth = server.world.width;
            int worldHeight = server.world.height;
            int spawnX = server.world.worldEntity.getSpawnX();
            int spawnY = server.world.worldEntity.getSpawnY();
            String weather = server.world.weatherManager.getCurrentWeather().name();
            String season = server.world.worldEntity.getTime().getSeason().name();
            
            // Informações adicionais do mundo
            boolean isPvPEnabled = server.world.worldEntity.isPvPEnabled();
            String difficulty = server.world.worldEntity.getDifficulty().name();
            long gameTime = server.world.worldEntity.getTime().getTotalTicks();
            
            String jsonResponse = String.format(
                "{\"world_name\":\"%s\",\"world_time\":\"%s\",\"world_day\":%d,\"world_seed\":%d,\"biome_count\":%d,\"world_size\":{\"width\":%d,\"height\":%d},\"spawn_point\":{\"x\":%d,\"y\":%d},\"weather\":\"%s\",\"season\":\"%s\",\"pvp_enabled\":%b,\"difficulty\":\"%s\",\"game_time\":%d,\"api_version\":\"2.0.0\"}",
                worldName,
                worldTime,
                worldDay,
                worldSeed,
                biomeCount,
                worldWidth,
                worldHeight,
                spawnX,
                spawnY,
                weather,
                season,
                isPvPEnabled,
                difficulty,
                gameTime
            );

            sendResponse(exchange, 200, jsonResponse);
        }
    }

    static class SystemHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Verificar autenticação se necessário
            if (!authenticateRequest(exchange)) {
                sendResponse(exchange, 401, "{\"error\":\"Unauthorized\"}");
                return;
            }
            
            // Verificar rate limiting
            if (!checkRateLimit(exchange)) {
                sendResponse(exchange, 429, "{\"error\":\"Rate limit exceeded\"}");
                return;
            }
            
            // Tratar requisições OPTIONS para CORS preflight
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, "");
                return;
            }
            
            try {
                // Usar OSHI para obter informações detalhadas do sistema
                SystemInfo systemInfo = new SystemInfo();
                HardwareAbstractionLayer hal = systemInfo.getHardware();
                OperatingSystem os = systemInfo.getOperatingSystem();
                
                // Informações do processador
                CentralProcessor processor = hal.getProcessor();
                double cpuLoad = processor.getSystemCpuLoadBetweenTicks(processor.getSystemCpuLoadTicks()) * 100;
                
                // Informações de memória
                GlobalMemory memory = hal.getMemory();
                long totalMemory = memory.getTotal();
                long availableMemory = memory.getAvailable();
                long usedMemory = totalMemory - availableMemory;
                
                // Informações da JVM
                MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
                RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
                
                long jvmTotalMemory = memoryBean.getHeapMemoryUsage().getMax();
                long jvmUsedMemory = memoryBean.getHeapMemoryUsage().getUsed();
                long jvmFreeMemory = jvmTotalMemory - jvmUsedMemory;
                
                // Informações do sistema operacional
                String osName = os.getFamily();
                String osVersion = os.getVersionInfo().getVersion();
                int processCount = os.getProcessCount();
                int threadCount = os.getThreadCount();
                
                String jsonResponse = String.format(
                    "{\"system\":{\"os\":{\"name\":\"%s\",\"version\":\"%s\",\"arch\":\"%s\"},\"cpu\":{\"cores\":%d,\"logical_processors\":%d,\"usage\":%.2f,\"model\":\"%s\"},\"memory\":{\"total\":%d,\"used\":%d,\"available\":%d,\"usage_percent\":%.2f},\"processes\":{\"count\":%d,\"threads\":%d}},\"jvm\":{\"memory\":{\"heap_total\":%d,\"heap_used\":%d,\"heap_free\":%d,\"heap_usage_percent\":%.2f},\"uptime\":%d,\"version\":\"%s\",\"vendor\":\"%s\"},\"timestamp\":%d}",
                    osName,
                    osVersion,
                    System.getProperty("os.arch"),
                    processor.getPhysicalProcessorCount(),
                    processor.getLogicalProcessorCount(),
                    cpuLoad,
                    processor.getProcessorIdentifier().getName(),
                    totalMemory,
                    usedMemory,
                    availableMemory,
                    (double) usedMemory / totalMemory * 100,
                    processCount,
                    threadCount,
                    jvmTotalMemory,
                    jvmUsedMemory,
                    jvmFreeMemory,
                    (double) jvmUsedMemory / jvmTotalMemory * 100,
                    runtimeBean.getUptime(),
                    System.getProperty("java.version"),
                    System.getProperty("java.vendor"),
                    System.currentTimeMillis()
                );
                
                sendResponse(exchange, 200, jsonResponse);
                
            } catch (Exception e) {
                // Fallback para informações básicas se OSHI falhar
                MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
                RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
                
                long jvmTotalMemory = memoryBean.getHeapMemoryUsage().getMax();
                long jvmUsedMemory = memoryBean.getHeapMemoryUsage().getUsed();
                long jvmFreeMemory = jvmTotalMemory - jvmUsedMemory;
                
                String fallbackResponse = String.format(
                    "{\"system\":{\"os\":{\"name\":\"%s\",\"version\":\"%s\",\"arch\":\"%s\"},\"cpu\":{\"cores\":%d},\"memory\":{\"total\":\"N/A\",\"used\":\"N/A\",\"available\":\"N/A\"}},\"jvm\":{\"memory\":{\"heap_total\":%d,\"heap_used\":%d,\"heap_free\":%d,\"heap_usage_percent\":%.2f},\"uptime\":%d,\"version\":\"%s\",\"vendor\":\"%s\"},\"timestamp\":%d,\"note\":\"Limited system info - OSHI unavailable\"}",
                    System.getProperty("os.name"),
                    System.getProperty("os.version"),
                    System.getProperty("os.arch"),
                    Runtime.getRuntime().availableProcessors(),
                    jvmTotalMemory,
                    jvmUsedMemory,
                    jvmFreeMemory,
                    (double) jvmUsedMemory / jvmTotalMemory * 100,
                    runtimeBean.getUptime(),
                    System.getProperty("java.version"),
                    System.getProperty("java.vendor"),
                    System.currentTimeMillis()
                );
                
                sendResponse(exchange, 200, fallbackResponse);
            }
        }
    }

    static class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Tratar requisições OPTIONS para CORS preflight
            if ("OPTIONS".equals(exchange.getRequestMethod())) {
                sendResponse(exchange, 200, "");
                return;
            }
            
            String jsonResponse = "{\"status\":\"healthy\",\"timestamp\":" + System.currentTimeMillis() + ",\"api_version\":\"1.0.0\"}";
            sendResponse(exchange, 200, jsonResponse);
        }
    }

    private static WebInfoAPI instance;
    
    // Métodos de autenticação e rate limiting
    private static boolean authenticateRequest(HttpExchange exchange) {
        if (instance == null || !instance.getConfigBoolean("api.security.require_auth", false)) {
            return true; // Autenticação não necessária
        }
        
        String apiKey = instance.getConfigString("api.security.api_key", "");
        if (apiKey.isEmpty() || "your-secure-api-key-here".equals(apiKey)) {
            return true; // Chave não configurada adequadamente
        }
        
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        String apiKeyHeader = exchange.getRequestHeaders().getFirst("X-API-Key");
        
        return (authHeader != null && authHeader.equals("Bearer " + apiKey)) ||
               (apiKeyHeader != null && apiKeyHeader.equals(apiKey));
    }
    
    private static boolean checkRateLimit(HttpExchange exchange) {
        if (instance == null || !instance.getConfigBoolean("api.rate_limit.enabled", true)) {
            return true; // Rate limiting desabilitado
        }
        
        String clientIP = exchange.getRemoteAddress().getAddress().getHostAddress();
        long currentTime = System.currentTimeMillis();
        int requestsPerMinute = instance.getConfigInt("api.rate_limit.requests_per_minute", 60);
        
        // Limpar entradas antigas (mais de 1 minuto)
        instance.rateLimitMap.entrySet().removeIf(entry -> 
            currentTime - entry.getValue() > 60000);
        instance.requestCountMap.entrySet().removeIf(entry -> 
            !instance.rateLimitMap.containsKey(entry.getKey()));
        
        // Verificar rate limit
        int currentCount = instance.requestCountMap.getOrDefault(clientIP, 0);
        if (currentCount >= requestsPerMinute) {
            return false;
        }
        
        // Atualizar contadores
        instance.rateLimitMap.put(clientIP, currentTime);
        instance.requestCountMap.put(clientIP, currentCount + 1);
        
        return true;
    }
    
    private static void sendResponse(HttpExchange exchange, int statusCode, String response) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        
        // Configurar CORS baseado nas configurações
        if (instance != null && instance.getConfigBoolean("api.cors.enabled", true)) {
            String allowedOrigins = instance.getConfigString("api.cors.allowed_origins", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", allowedOrigins);
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        } else {
            // Fallback para CORS básico se não houver instância
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
        }
        
        byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
    }
}