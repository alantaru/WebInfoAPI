

# **Guia de Implementação: Extração de Dados de Servidor Dedicado Necesse via Mod e API HTTP para Integração Web com Next.js**

## **Seção 1: Blueprint Arquitetural: Conectando Necesse e a Web**

A integração de um servidor de jogo dedicado, como o de Necesse, com uma aplicação web moderna requer uma arquitetura que priorize segurança, escalabilidade e manutenibilidade. A solicitação para criar um "mod que usa http" para comunicação direta com um site React/Next.js, embora funcional, expõe o servidor de jogo a riscos desnecessários e introduz complexidade em áreas onde as frameworks web já oferecem soluções robustas. Portanto, a abordagem profissional não é uma conexão direta, mas sim uma arquitetura de três camadas bem definida que isola responsabilidades e protege os componentes críticos.

### **1.1 O Modelo de Arquitetura de Três Camadas**

A solução proposta é estruturada em três camadas distintas, cada uma com uma responsabilidade clara, garantindo que o sistema como um todo seja seguro e eficiente.

* **Camada 1: A Fonte de Dados (Servidor Necesse e Mod):** No núcleo do sistema está o servidor dedicado de Necesse, executando um mod Java personalizado. A única responsabilidade desta camada é acessar o estado interno do jogo (game state) em tempo real e expor esses dados através de um servidor HTTP embarcado, escutando em uma porta de rede local (por exemplo, localhost:8080). Esta camada nunca deve ser exposta diretamente à internet pública.  
* **Camada 2: O Gateway Seguro (Rota de API Next.js):** Esta camada atua como um *Backend-for-Frontend* (BFF). Implementada como uma rota de API serverless dentro da aplicação Next.js, ela é o único ponto de contato com a internet pública. Suas responsabilidades incluem: receber requisições do cliente web, autenticar essas requisições, gerenciar o controle de acesso e atuar como um proxy seguro que se comunica com a Camada 1 através da rede privada. Toda a lógica de segurança web, como validação de API keys, rate limiting e CORS, é centralizada aqui.1  
* **Camada 3: A Camada de Apresentação (Frontend React):** Esta é a aplicação React do usuário, renderizada no navegador. Ela se comunica exclusivamente com o Gateway Seguro (Camada 2\) através de chamadas de API relativas (por exemplo, /api/server-status). A camada de apresentação nunca tem conhecimento direto do endereço IP, porta ou protocolos de autenticação do servidor de jogo, garantindo um desacoplamento completo e maior segurança.

### **1.2 Fluxo de Dados e Fronteiras de Segurança**

O ciclo de vida de uma requisição de dados demonstra a robustez desta arquitetura:

1. O navegador de um usuário carrega a aplicação React/Next.js.  
2. Um componente React (por exemplo, um painel de status do servidor) dispara uma chamada para buscar dados de uma rota de API local, como /api/necesse/status.  
3. A rota de API Next.js (Camada 2\) recebe a requisição. Ela valida a requisição e, em seguida, anexa uma chave de API secreta, que está armazenada de forma segura como uma variável de ambiente no servidor Next.js (por exemplo, NECESSE\_API\_KEY).  
4. O backend Next.js então faz uma requisição HTTP, através da rede privada ou localhost, para o servidor HTTP embarcado no mod Java (Camada 1), por exemplo, para http://\<IP\_DO\_SERVIDOR\_DE\_JOGO\>:8080/status, incluindo a chave de API em um cabeçalho (X-API-Key).  
5. O mod Java recebe a requisição e valida a chave de API recebida.  
6. Se a chave for válida, o mod acessa o objeto principal do servidor Necesse, recupera os dados solicitados (como contagem de jogadores), serializa esses dados para o formato JSON e os retorna na resposta HTTP.  
7. O backend Next.js recebe a resposta JSON da Camada 1\. Neste ponto, ele pode realizar transformações, aplicar caching ou agregar dados antes de enviar a resposta final de volta para o frontend React.  
8. O componente React recebe os dados e atualiza a interface do usuário para exibir as informações mais recentes do servidor.

Este fluxo estabelece uma fronteira de segurança clara. O servidor de jogo, que não é um servidor web robustecido, fica protegido da internet pública, prevenindo ataques de negação de serviço (DDoS), varreduras de portas e potenciais explorações na biblioteca HTTP Java utilizada.4 A complexidade da segurança web é delegada à framework Next.js, que é projetada especificamente para essa finalidade.6

### **1.3 Diagrama da Arquitetura do Sistema**

A visualização a seguir ilustra a separação entre a rede pública e a privada, e o fluxo de comunicação entre as três camadas.

\+--------------------------------+

| Internet Público |  
\+--------------------------------+  
             ^

| HTTPS (Porta 443\)  
             v  
\+--------------------------------+      \+--------------------------------+

| Camada 3: Frontend React | | Camada 2: Gateway Seguro |  
| (Navegador do Usuário) |-----\>| (Next.js API Route) |  
| \- Faz fetch para /api/necesse/\*| | \- Valida requisição |  
| \- Renderiza dados | | \- Adiciona X-API-Key |  
\+--------------------------------+ | \- Faz proxy para a Camada 1 |  
                                        \+--------------------------------+  
|  
| HTTP (Rede Privada/Localhost)  
                                                       v  
\+-------------------------------------------------------------------------+

| Rede Privada do Servidor |  
\+-------------------------------------------------------------------------+

| \+---------------------------------------------------------------------+ |  
| | Camada 1: Fonte de Dados | |  
| | (Servidor Dedicado Necesse) | |  
| | \+--------------------------+   \+----------------------------------+ | |  
| | | Core do Jogo | | Mod Java Personalizado | | |  
| | | \- Objeto 'Server' |\<-\>| \- Servidor HTTP Embarcado | | |  
| | | \- Estado do Mundo/Players| | \- Valida X-API-Key | | |  
| | \+--------------------------+ | \- Acessa dados e serializa JSON | | |  
| | \+----------------------------------+ | |  
| \+---------------------------------------------------------------------+ |  
\+-------------------------------------------------------------------------+

Esta estrutura arquitetural transforma o projeto de um simples mod para um sistema de informação robusto e de nível de produção, estabelecendo a base para todas as etapas de implementação subsequentes.

## **Seção 2: Construindo o Mod Extrator de Dados do Necesse**

Esta seção serve como um guia prático para desenvolver o mod Java, partindo do pressuposto de um desenvolvedor web experiente, mas novato no ecossistema Java e na modificação de jogos. O objetivo é criar a fundação do mod: a capacidade de acessar o objeto principal do servidor do jogo.

### **2.1 Configurando o Ambiente de Desenvolvimento**

Um ambiente de desenvolvimento adequado é crucial para a eficiência. A comunidade de modding de Necesse e a documentação oficial convergem em um conjunto de ferramentas padrão.7

* **Ferramentas Necessárias:**  
  * **IntelliJ IDEA Community Edition:** Uma IDE (Integrated Development Environment) poderosa e gratuita para desenvolvimento Java. Ela oferece excelente integração com o sistema de build Gradle, depuração e um descompilador embutido, que será vital para a exploração do código do jogo.  
  * **Java Development Kit (JDK):** O conjunto de ferramentas da Oracle ou de código aberto (como o OpenJDK) para compilar e executar aplicações Java. A versão deve ser compatível com a exigida pelo Necesse.  
* Configuração do Projeto:  
  A maneira mais eficaz de iniciar é utilizando o projeto de exemplo oficial fornecido pelos desenvolvedores do Necesse.  
  1. Clone o repositório do projeto de exemplo do GitHub, que pode ser encontrado através da Wiki oficial do Necesse.7  
  2. Abra o projeto clonado no IntelliJ IDEA. A IDE detectará automaticamente que é um projeto Gradle e fará o download das dependências necessárias.  
* Entendendo o Sistema de Build Gradle:  
  O arquivo build.gradle na raiz do projeto é o coração do sistema de build. Para um iniciante, os pontos mais importantes são:  
  * **dependencies:** Este bloco define as bibliotecas das quais o seu mod depende. Inicialmente, ele conterá uma referência ao Necesse.jar, permitindo que seu código acesse as classes e métodos do jogo.10 Posteriormente, adicionaremos nossas próprias dependências aqui, como o servidor HTTP e a biblioteca JSON.  
  * **task buildModJar:** Esta tarefa, definida no arquivo, compila seu código Java e o empacota em um arquivo .jar final, que é o formato de mod que o Necesse carrega.7

### **2.2 A Anatomia de um Mod de Necesse**

Um mod de Necesse é composto por dois elementos principais que o carregador de mods do jogo utiliza para identificá-lo e inicializá-lo.

* Arquivo mod.info:  
  Localizado em src/main/resources, este arquivo JSON informa ao Necesse sobre o seu mod. Cada campo tem um propósito específico 7:  
  * id: Um identificador único, em letras minúsculas, sem espaços ou caracteres especiais. É crucial que este ID nunca mude entre as versões do seu mod. Uma boa prática é usar o formato seunome.nomedomod.  
  * name: O nome de exibição do mod.  
  * version: A versão do seu mod.  
  * gameVersion: A versão do Necesse para a qual o mod foi desenvolvido.  
  * description: Uma breve descrição.  
  * author: Seu nome ou pseudônimo.  
  * dependencies (opcional): Uma lista de IDs de outros mods que são necessários para o seu mod funcionar.  
* A Classe @ModEntry:  
  Esta é a porta de entrada para o seu mod. É uma classe Java que deve ser anotada com @ModEntry para que o jogo a reconheça. O jogo procurará por métodos específicos dentro desta classe para executar durante seu ciclo de vida de inicialização.7 Os métodos mais importantes são:  
  * public void init(): Chamado durante a fase principal de inicialização do jogo. É aqui que a maior parte da lógica de inicialização, como o registro de eventos, será colocada.  
  * public void postInit(): Chamado após todos os mods terem sido inicializados. Útil para interações entre mods ou para garantir que todos os recursos do jogo estejam carregados.

### **2.3 A Chave de Ouro: Acessando o Objeto Server**

O objetivo central do mod é obter uma referência ao objeto necesse.engine.network.server.Server. Este objeto é a principal interface para interagir com o estado do servidor dedicado. A abordagem correta para obter essa referência não é procurá-la ativamente, mas sim escutar por um evento que o jogo dispara quando o servidor está pronto. Esta abordagem orientada a eventos é um padrão de design deliberado dos desenvolvedores do jogo para evitar condições de corrida e garantir que os mods só interajam com sistemas que já foram totalmente inicializados.

O evento chave é o ServerStartEvent. Ao registrar um "ouvinte" (listener) para este evento, o mod pode capturar a instância do servidor no momento exato em que ela se torna disponível e segura para uso.10

A seguir, o código completo para uma classe @ModEntry básica que implementa essa lógica:

Java

package com.example.necessemod;

import necesse.engine.GameEventListener;  
import necesse.engine.GameEvents;  
import necesse.engine.events.ServerStartEvent;  
import necesse.engine.modLoader.annotations.ModEntry;  
import necesse.engine.network.server.Server;

@ModEntry  
public class DataExtractorMod {

    // Variável estática para armazenar a instância do servidor.  
    // 'static' permite que seja acessada de qualquer lugar dentro do mod.  
    // 'public' permite o acesso por outras classes no mesmo pacote.  
    public static Server SERVER\_INSTANCE;

    public void init() {  
        System.out.println("Data Extractor Mod: Inicializando...");

        // Registra um ouvinte para o evento ServerStartEvent.  
        // Este código será executado quando o servidor dedicado terminar de iniciar.  
        GameEvents.addListener(ServerStartEvent.class, new GameEventListener\<ServerStartEvent\>() {  
            @Override  
            public void onEvent(ServerStartEvent event) {  
                // O evento contém a instância do servidor.  
                // Armazenamos essa instância na nossa variável estática para uso posterior.  
                SERVER\_INSTANCE \= event.server;

                // Log de confirmação no console do servidor.  
                // Isso confirma que o mod capturou a instância com sucesso.  
                System.out.println("Data Extractor Mod: Instância do servidor capturada com sucesso\!");

                // É aqui que iniciaremos nosso servidor HTTP.  
                // (A ser implementado na próxima seção)  
            }  
        });  
    }

    public void postInit() {  
        System.out.println("Data Extractor Mod: Pós-inicialização concluída.");  
    }  
}

Com este código, o mod está funcional. Ao ser carregado por um servidor dedicado Necesse, ele imprimirá mensagens no console, confirmando que foi inicializado e que capturou com sucesso a instância do Server. Esta instância, armazenada em SERVER\_INSTANCE, é a chave para desbloquear todas as informações que a aplicação web necessita.

## **Seção 3: Engenharia da API HTTP In-Game**

Com o acesso ao objeto Server garantido, o próximo passo é transformar o mod em um microserviço autocontido, capaz de responder a requisições HTTP. Isso envolve a integração de bibliotecas externas para funcionalidade de servidor web e serialização JSON, uma capacidade que vai além da API de modding padrão do Necesse.

### **3.1 Escolhendo as Ferramentas Certas**

Para um desenvolvedor Java novato, a simplicidade e a clareza da API são primordiais.

* Biblioteca de Servidor HTTP: Javalin  
  Javalin é uma framework web leve e moderna para Java e Kotlin. Sua API é extremamente simples e requer um mínimo de configuração, tornando-a ideal para ser embarcada em outra aplicação, como um mod de jogo. A sintaxe para definir rotas é concisa e intuitiva, semelhante a frameworks como Express.js, o que facilita a transição para desenvolvedores web.  
* Biblioteca de Serialização JSON: Gson  
  Gson, desenvolvida pelo Google, é uma biblioteca robusta para converter objetos Java em sua representação JSON e vice-versa.11 Sua principal vantagem para este projeto é a simplicidade de uso para casos comuns. A serialização de um objeto pode ser feita com uma única linha de código (  
  new Gson().toJson(meuObjeto)), o que é perfeito para a tarefa de expor os dados do jogo.13 Embora a biblioteca Jackson seja mais poderosa e configurável, sua complexidade inicial é maior, tornando Gson a escolha mais pragmática aqui.15

### **3.2 Integrando Dependências com o Gradle**

Para usar Javalin e Gson, é preciso declará-las como dependências no arquivo build.gradle. Mais importante, é necessário configurar a tarefa de build para criar um "fat jar" (ou "uber jar"), que é um arquivo .jar que contém não apenas o código do mod, mas também todas as suas dependências. Sem isso, o Necesse não saberia como encontrar e carregar as classes do Javalin e do Gson, resultando em um ClassNotFoundException.10

Adicione as seguintes linhas ao bloco dependencies do seu build.gradle:

Groovy

dependencies {  
    // Dependência existente do Necesse  
    implementation files(gameDirectory \+ "/Necesse.jar")

    // Adiciona a biblioteca Javalin  
    implementation 'io.javalin:javalin:5.6.3'  
    // Adiciona o logger SLF4J, uma dependência comum do Javalin  
    implementation 'org.slf4j:slf4j-simple:2.0.7'

    // Adiciona a biblioteca Gson  
    implementation 'com.google.code.gson:gson:2.10.1'  
}

Agora, modifique a tarefa buildModJar para incluir essas dependências no .jar final:

Groovy

task buildModJar(type: Jar) {  
    //... configurações existentes...

    // Adiciona esta linha para empacotar as dependências  
    from { configurations.runtimeClasspath.collect { it.isDirectory()? it : zipTree(it) } }

    // Evita a duplicação de arquivos de licença que podem causar conflitos  
    duplicatesStrategy \= DuplicatesStrategy.EXCLUDE  
}

Após fazer essas alterações, sincronize o projeto Gradle no IntelliJ IDEA. Agora, ao executar a tarefa buildModJar, o .jar resultante conterá tudo o que é necessário para o mod funcionar de forma autônoma.

### **3.3 Implementando o Servidor HTTP**

A inicialização do servidor HTTP deve ocorrer logo após a captura bem-sucedida da instância do Server. O local ideal para este código é dentro do listener do ServerStartEvent, que foi criado na seção anterior.

Modifique a classe DataExtractorMod para incluir a inicialização do Javalin:

Java

//... imports existentes...  
import io.javalin.Javalin;  
import com.google.gson.Gson;

@ModEntry  
public class DataExtractorMod {

    public static Server SERVER\_INSTANCE;  
    private Javalin httpApp;  
    private final Gson gson \= new Gson(); // Instância de Gson para reutilização

    public void init() {  
        System.out.println("Data Extractor Mod: Inicializando...");

        GameEvents.addListener(ServerStartEvent.class, new GameEventListener\<ServerStartEvent\>() {  
            @Override  
            public void onEvent(ServerStartEvent event) {  
                SERVER\_INSTANCE \= event.server;  
                System.out.println("Data Extractor Mod: Instância do servidor capturada.");

                // Inicia o servidor HTTP após capturar a instância do jogo  
                startHttpServer();  
            }  
        });  
    }

    private void startHttpServer() {  
        // Cria e configura a instância do Javalin na porta 8080  
        httpApp \= Javalin.create().start(8080);  
        System.out.println("Data Extractor Mod: Servidor HTTP iniciado na porta 8080.");

        // Define as rotas da API  
        defineApiEndpoints();  
    }

    private void defineApiEndpoints() {  
        // Endpoint de teste para verificar se a API está no ar  
        httpApp.get("/", ctx \-\> {  
            ctx.result("API do Servidor Necesse está online\!");  
        });

        // Endpoints de dados (a serem implementados na próxima seção)  
        //...  
    }

    // Método para parar o servidor HTTP quando o jogo for desligado  
    public void dispose() {  
        if (httpApp\!= null) {  
            httpApp.stop();  
            System.out.println("Data Extractor Mod: Servidor HTTP parado.");  
        }  
    }

    //... outros métodos...  
}

### **3.4 Desenhando os Endpoints da API**

Uma API bem projetada é lógica e fácil de consumir. Em vez de um único endpoint que retorna um grande volume de dados, é melhor segmentar as informações em rotas RESTful, cada uma com um propósito claro.

A estrutura de endpoints proposta é:

* GET /status: Para informações de alto nível e que mudam com frequência, ideais para polling rápido. Ex: nome do servidor, contagem de jogadores.  
* GET /world: Para dados específicos do mundo do jogo. Ex: nome do mundo, tempo no jogo, estação.  
* GET /players: Retorna uma lista detalhada de todos os jogadores online. Ex: nome, ping, localização.  
* GET /system: Para métricas de telemetria do hardware do servidor, independentes do jogo. Ex: uso de CPU, RAM.

A implementação do roteamento no Javalin dentro do método defineApiEndpoints ficaria assim:

Java

private void defineApiEndpoints() {  
    httpApp.get("/", ctx \-\> ctx.result("API do Servidor Necesse está online\!"));

    // Rota para status geral do servidor  
    httpApp.get("/status", ctx \-\> {  
        // Lógica para buscar e retornar dados de status  
        // ctx.json(objetoDeStatus);  
    });

    // Rota para informações do mundo  
    httpApp.get("/world", ctx \-\> {  
        // Lógica para buscar e retornar dados do mundo  
        // ctx.json(objetoDeMundo);  
    });

    // Rota para a lista de jogadores  
    httpApp.get("/players", ctx \-\> {  
        // Lógica para buscar e retornar a lista de jogadores  
        // ctx.json(listaDeJogadores);  
    });

    // Rota para telemetria do sistema  
    httpApp.get("/system", ctx \-\> {  
        // Lógica para buscar e retornar dados do sistema  
        // ctx.json(objetoDeSistema);  
    });  
}

Com esta estrutura, o mod agora é um servidor de API funcional. A próxima seção preencherá a lógica de cada um desses endpoints, extraindo os dados reais do jogo.

## **Seção 4: Um Catálogo Abrangente de Dados Extraíveis do Servidor**

Esta seção é o núcleo do relatório, abordando diretamente a questão central: "quais informações podem ser extraídas do servidor?". Devido à ausência de uma documentação oficial detalhada da API de modding 17, a abordagem necessária é investigativa, tratando o código-fonte do jogo como a documentação primária.

### **4.1 O Kit de Ferramentas do Investigador: Descompilando o Jogo**

A falta de uma referência formal da API torna a descompilação uma etapa essencial. Este processo permite examinar diretamente as classes e métodos do jogo, revelando as funções disponíveis para extrair dados.

* **Por que Descompilar?** Sem uma lista de métodos disponíveis, a única maneira de descobrir o que o objeto Server pode fazer é olhar seu código-fonte.  
* **Como Descompilar:** O IntelliJ IDEA possui um descompilador Java integrado. Para usá-lo, navegue até a seção "External Libraries" no painel do projeto, encontre Necesse.jar, e abra as classes de interesse. A IDE descompilará o bytecode para um código Java legível.  
* **Alvo Principal:** A classe a ser investigada é necesse.engine.network.server.Server. Analisar seus campos (variáveis) e métodos públicos revelará os pontos de acesso aos dados do servidor.

### **4.2 Catálogo de Dados e Implementação da API**

Esta subseção detalha a implementação de cada endpoint da API definido anteriormente. Para cada rota, será apresentada a lógica de extração dos dados, o código Java completo para o manipulador (handler) do Javalin, e a estrutura JSON resultante.

* **Estrutura de Dados (POJOs):** Para organizar os dados antes de serializá-los, criaremos classes Java simples (Plain Old Java Objects \- POJOs) para cada tipo de resposta.

#### **4.2.1 Endpoint: GET /status**

Este endpoint fornece um resumo de alto nível do estado do servidor.

* **Dados Extraíveis:**  
  * Nome do Servidor: SERVER\_INSTANCE.settings.serverName  
  * Mensagem do Dia (MOTD): SERVER\_INSTANCE.settings.motd  
  * Versão do Jogo: necesse.engine.GlobalData.GAME\_VERSION.string  
  * Contagem de Jogadores Online: SERVER\_INSTANCE.getPlayers().size()  
  * Máximo de Jogadores: SERVER\_INSTANCE.settings.slots  
  * Status de Proteção por Senha: SERVER\_INSTANCE.settings.password\!= null  
  * Status de PvP: SERVER\_INSTANCE.settings.pvp  
* **Implementação:**

Java

// POJO para a resposta de /status  
class ServerStatus {  
    String name;  
    String motd;  
    String gameVersion;  
    int playersOnline;  
    int maxPlayers;  
    boolean passwordProtected;  
    boolean pvpEnabled;  
}

// Dentro de defineApiEndpoints()  
httpApp.get("/status", ctx \-\> {  
    if (SERVER\_INSTANCE \== null) {  
        ctx.status(503).result("Servidor ainda não está pronto.");  
        return;  
    }

    ServerStatus status \= new ServerStatus();  
    status.name \= SERVER\_INSTANCE.settings.serverName;  
    status.motd \= SERVER\_INSTANCE.settings.motd;  
    status.gameVersion \= necesse.engine.GlobalData.GAME\_VERSION.string;  
    status.playersOnline \= SERVER\_INSTANCE.getPlayers().size();  
    status.maxPlayers \= SERVER\_INSTANCE.settings.slots;  
    status.passwordProtected \= SERVER\_INSTANCE.settings.password\!= null &&\!SERVER\_INSTANCE.settings.password.isEmpty();  
    status.pvpEnabled \= SERVER\_INSTANCE.settings.pvp;

    ctx.json(status);  
});

* **Resposta JSON de Exemplo:**  
  JSON  
  {  
    "name": "Meu Servidor Necesse",  
    "motd": "Bem-vindo\!",  
    "gameVersion": "0.24.2",  
    "playersOnline": 5,  
    "maxPlayers": 10,  
    "passwordProtected": true,  
    "pvpEnabled": false  
  }

#### **4.2.2 Endpoint: GET /world**

Este endpoint fornece informações sobre o estado atual do mundo do jogo.

* **Dados Extraíveis:**  
  * Nome do Mundo: SERVER\_INSTANCE.world.displayName  
  * Tempo do Mundo: SERVER\_INSTANCE.world.worldEntity.getTime()  
  * Estação Atual: SERVER\_INSTANCE.world.worldEntity.getSeason().name()  
  * Frequência de Raids: SERVER\_INSTANCE.world.settings.raidFrequency.name()  
* **Implementação:**

Java

// POJO para a resposta de /world  
class WorldInfo {  
    String worldName;  
    String gameTime;  
    String season;  
    String raidFrequency;  
}

// Dentro de defineApiEndpoints()  
httpApp.get("/world", ctx \-\> {  
    if (SERVER\_INSTANCE \== null |

| SERVER\_INSTANCE.world \== null) {  
        ctx.status(503).result("Mundo ainda não está pronto.");  
        return;  
    }

    WorldInfo info \= new WorldInfo();  
    info.worldName \= SERVER\_INSTANCE.world.displayName;  
      
    // Formata o tempo do jogo para um formato legível  
    long totalSeconds \= SERVER\_INSTANCE.world.worldEntity.getTime();  
    int day \= (int)(totalSeconds / 86400) \+ 1;  
    long secondsInDay \= totalSeconds % 86400;  
    int hours \= (int)(secondsInDay / 3600);  
    int minutes \= (int)((secondsInDay % 3600) / 60);  
    info.gameTime \= String.format("Dia %d, %02d:%02d", day, hours, minutes);

    info.season \= SERVER\_INSTANCE.world.worldEntity.getSeason().name();  
    info.raidFrequency \= SERVER\_INSTANCE.world.settings.raidFrequency.name();

    ctx.json(info);  
});

* **Resposta JSON de Exemplo:**  
  JSON  
  {  
    "worldName": "Mundo Principal",  
    "gameTime": "Dia 5, 14:30",  
    "season": "SUMMER",  
    "raidFrequency": "OFTEN"  
  }

#### **4.2.3 Endpoint: GET /players**

Este endpoint retorna uma lista detalhada de todos os jogadores conectados.

* **Dados Extraíveis (por jogador):**  
  * Nome do Jogador: player.getName()  
  * ID de Autenticação (SteamID64): player.authentication  
  * Latência (Ping): player.latency  
  * Coordenadas da Ilha/Nível: player.getLevelIdentifier().stringID  
  * Posição do Personagem (X, Y): player.playerMob.x, player.playerMob.y  
* **Implementação:**

Java

// POJO para um único jogador  
class PlayerInfo {  
    String name;  
    long authId;  
    int ping;  
    String islandCoords;  
    float posX;  
    float posY;  
}

// Dentro de defineApiEndpoints()  
httpApp.get("/players", ctx \-\> {  
    if (SERVER\_INSTANCE \== null) {  
        ctx.status(503).result("Servidor ainda não está pronto.");  
        return;  
    }

    java.util.List\<PlayerInfo\> playerList \= new java.util.ArrayList\<\>();  
    for (necesse.engine.network.server.ServerClient client : SERVER\_INSTANCE.getPlayers()) {  
        if (client\!= null && client.playerMob\!= null) {  
            PlayerInfo pInfo \= new PlayerInfo();  
            pInfo.name \= client.getName();  
            pInfo.authId \= client.authentication;  
            pInfo.ping \= client.latency;  
            pInfo.islandCoords \= client.getLevelIdentifier().stringID;  
            pInfo.posX \= client.playerMob.x;  
            pInfo.posY \= client.playerMob.y;  
            playerList.add(pInfo);  
        }  
    }

    ctx.json(playerList);  
});

* **Resposta JSON de Exemplo:**  
  JSON

### **4.3 Telemetria Avançada com OSHI**

Para dados que não são do jogo, como o desempenho do hardware do servidor, a biblioteca OSHI (Operating System and Hardware Information) é uma ferramenta poderosa.18 Ela permite o acesso a informações do sistema operacional de forma independente de plataforma.

* Integração: Adicione a dependência do OSHI ao build.gradle:  
  implementation 'com.github.oshi:oshi-core:6.4.8'  
  Não se esqueça de sincronizar o Gradle.  
* **Endpoint: GET /system**

Java

// Imports para OSHI  
import oshi.SystemInfo;  
import oshi.hardware.CentralProcessor;  
import oshi.hardware.GlobalMemory;

// POJO para a resposta de /system  
class SystemTelemetry {  
    double cpuLoad;  
    double ramUsedGB;  
    double ramTotalGB;  
    long jvmUsedMB;  
    long jvmMaxMB;  
}

// Instâncias do OSHI (podem ser inicializadas uma vez)  
private final SystemInfo si \= new SystemInfo();  
private final CentralProcessor processor \= si.getHardware().getProcessor();  
private long prevTicks \= new long;

// Dentro de defineApiEndpoints()  
httpApp.get("/system", ctx \-\> {  
    SystemTelemetry telemetry \= new SystemTelemetry();  
    GlobalMemory memory \= si.getHardware().getMemory();

    // Carga da CPU  
    telemetry.cpuLoad \= processor.getSystemCpuLoadBetweenTicks(prevTicks) \* 100;  
    this.prevTicks \= processor.getSystemCpuLoadTicks(); // Atualiza os ticks para a próxima medição

    // Uso de RAM  
    long totalRam \= memory.getTotal();  
    long availableRam \= memory.getAvailable();  
    telemetry.ramTotalGB \= totalRam / (1024.0 \* 1024.0 \* 1024.0);  
    telemetry.ramUsedGB \= (totalRam \- availableRam) / (1024.0 \* 1024.0 \* 1024.0);  
      
    // Uso de memória da JVM  
    Runtime runtime \= Runtime.getRuntime();  
    telemetry.jvmUsedMB \= (runtime.totalMemory() \- runtime.freeMemory()) / (1024 \* 1024);  
    telemetry.jvmMaxMB \= runtime.maxMemory() / (1024 \* 1024);

    ctx.json(telemetry);  
});

* **Resposta JSON de Exemplo:**  
  JSON  
  {  
    "cpuLoad": 15.7,  
    "ramUsedGB": 6.2,  
    "ramTotalGB": 16.0,  
    "jvmUsedMB": 512,  
    "jvmMaxMB": 2048  
  }

### **4.4 Tabela de Referência da API**

A tabela a seguir consolida as informações descobertas, servindo como um guia de referência rápido que conecta os dados desejados com sua implementação técnica.

| Ponto de Dados | Chamada na API Necesse (Hipótese) | Endpoint da API | Campo na Resposta JSON de Exemplo |
| :---- | :---- | :---- | :---- |
| Nome do Servidor | SERVER\_INSTANCE.settings.serverName | GET /status | {"name": "Meu Servidor Necesse"} |
| Contagem de Jogadores | SERVER\_INSTANCE.getPlayers().size() | GET /status | {"playersOnline": 5} |
| Máximo de Jogadores | SERVER\_INSTANCE.settings.slots | GET /status | {"maxPlayers": 10} |
| Tempo do Mundo | SERVER\_INSTANCE.world.worldEntity.getTime() | GET /world | {"gameTime": "Dia 5, 14:30"} |
| Lista de Jogadores | SERVER\_INSTANCE.getPlayers().stream()... | GET /players | {"players": \[{"name": "Jogador1",...}\]} |
| Carga da CPU (%) | oshi.getHardware().getProcessor()... | GET /system | {"cpuLoad": 15.7} |
| RAM Usada (GB) | oshi.getHardware().getMemory()... | GET /system | {"ramUsedGB": 6.2} |

## **Seção 5: Fortificando a API: Segurança e Controle de Acesso**

Uma API funcional é apenas metade da solução; uma API segura é um requisito não-funcional crítico. Esta seção detalha as camadas de segurança necessárias para proteger a comunicação entre o servidor de jogo e a aplicação web. A segurança deve ser uma responsabilidade distribuída por toda a arquitetura.

### **5.1 Autenticação por Chave de API (API Key)**

O primeiro nível de defesa é garantir que apenas a sua aplicação Next.js possa solicitar dados ao mod do Necesse. A forma mais simples e eficaz de conseguir isso é através de uma chave de API, que funciona como uma senha compartilhada.

* Implementação no Mod Java:  
  A chave de API deve ser verificada em cada requisição recebida. O Javalin facilita isso com um filtro before(), que é executado antes de qualquer manipulador de rota. A chave secreta deve ser lida de um arquivo de configuração no servidor do jogo para evitar que seja codificada diretamente no mod.  
  Java  
  // Dentro de startHttpServer()  
  httpApp \= Javalin.create().start(8080);

  // Lê a chave de API de uma variável de ambiente ou arquivo de configuração  
  // Para simplicidade, estamos usando uma variável de ambiente aqui.  
  final String aPI\_KEY \= System.getenv("NECESSE\_MOD\_API\_KEY");  
  if (aPI\_KEY \== null |

| aPI\_KEY.isEmpty()) {  
System.err.println("ERRO CRÍTICO: A variável de ambiente NECESSE\_MOD\_API\_KEY não está definida\!");  
// Considere parar o servidor ou a inicialização do mod aqui.  
return;  
}

// Filtro de segurança que será executado antes de cada requisição  
httpApp.before(ctx \-\> {  
    // Ignora o endpoint raiz, que pode ser público  
    if (ctx.path().equals("/")) {  
        return;  
    }

    String providedKey \= ctx.header("X-API-Key");  
    if (\!aPI\_KEY.equals(providedKey)) {  
        // Se a chave estiver ausente ou for inválida, recusa a requisição  
        ctx.status(401).result("Unauthorized");  
        ctx.skipRemainingHandlers(); // Impede a execução da rota solicitada  
    }  
});

defineApiEndpoints();  
\`\`\`

* Implementação no Backend Next.js:  
  O backend Next.js é responsável por armazenar a chave de API de forma segura e adicioná-la às requisições que faz para o mod. Use variáveis de ambiente para isso.1  
  1. Crie um arquivo .env.local na raiz do seu projeto Next.js.  
  2. Adicione a chave de API:  
     NECESSE\_MOD\_API\_KEY="sua-chave-secreta-muito-longa-e-segura"  
     NECESSE\_SERVER\_URL="http://\<IP\_DO\_SERVIDOR\_DE\_JOGO\>:8080"  
  3. Na sua rota de API Next.js que atua como proxy, leia a variável de ambiente e adicione o cabeçalho X-API-Key à requisição fetch:

TypeScript  
// Exemplo em uma rota de API do Next.js (app/api/necesse/\[...slug\]/route.ts)  
export async function GET(request: Request, { params }: { params: { slug: string } }) {  
    const slug \= params.slug.join('/');  
    const targetUrl \= \`${process.env.NECESSE\_SERVER\_URL}/${slug}\`;  
    const apiKey \= process.env.NECESSE\_MOD\_API\_KEY;

    if (\!apiKey) {  
        return new Response(JSON.stringify({ error: 'Configuração do servidor incompleta.' }), { status: 500 });  
    }

    try {  
        const response \= await fetch(targetUrl, {  
            headers: {  
                'X-API-Key': apiKey,  
            },  
        });

        //... lógica para retransmitir a resposta...  
    } catch (error) {  
        //... tratamento de erro...  
    }  
}

### **5.2 Segurança da Camada de Transporte (HTTPS)**

A comunicação através da internet pública (entre o navegador do usuário e o servidor Next.js) deve ser criptografada usando HTTPS. A comunicação entre o servidor Next.js e o servidor de jogo, por estar em uma rede privada, pode usar HTTP, mas a criptografia é sempre uma camada adicional de segurança.

* **Para a Aplicação Next.js:** Plataformas de hospedagem modernas como Vercel ou Netlify configuram HTTPS automaticamente. Se a aplicação for auto-hospedada, é essencial configurar um certificado SSL/TLS.  
* **Para o Servidor de Jogo:** Implementar TLS/SSL diretamente no mod Java é complexo e propenso a erros. A abordagem padrão da indústria é usar um **proxy reverso**.  
  * **Conceito:** Um servidor web robusto, como Nginx ou Caddy, é colocado na frente do servidor de jogo. Este proxy é exposto à rede, lida com as conexões HTTPS e termina a criptografia. Em seguida, ele encaminha a requisição como HTTP simples para o mod Java, que está escutando em uma porta local.  
  * **Vantagens:**  
    1. **Separação de Responsabilidades:** O mod foca na lógica do jogo, enquanto o proxy reverso foca na segurança da web.  
    2. **Robustez:** Servidores como Nginx são altamente otimizados e testados para segurança e desempenho.  
    3. **Simplicidade:** Simplifica drasticamente o código do mod, que não precisa se preocupar com certificados, handshakes TLS ou cifras de criptografia.

### **5.3 Outras Considerações de Segurança**

* **Rate Limiting (Limitação de Taxa):** Para prevenir abuso e ataques de força bruta, implemente a limitação de taxa. Isso é mais facilmente feito no gateway seguro (Next.js), usando middleware para limitar o número de requisições que um cliente pode fazer em um determinado período.  
* **Validação de Entrada:** Embora a comunicação seja interna, nunca confie em dados de entrada. Se a API for expandida para aceitar parâmetros, eles devem ser rigorosamente validados no mod Java.  
* **Tratamento de Erros:** Configure o servidor Javalin para não vazar stack traces ou outras informações sensíveis em caso de erro. Retorne mensagens de erro genéricas e registre os detalhes completos apenas nos logs do servidor.

Ao adotar essa abordagem de "defesa em profundidade", onde cada camada da arquitetura tem seu próprio conjunto de responsabilidades de segurança, o sistema se torna significativamente mais resiliente e seguro do que uma conexão direta.

## **Seção 6: Integração e Consumo com Next.js**

Esta seção final foca na área de especialidade do usuário: o desenvolvimento frontend. Serão fornecidos padrões e códigos prontos para consumir a API de forma eficiente e moderna dentro de uma aplicação React/Next.js.

### **6.1 A Rota de API Proxy Segura**

A peça central da integração é a rota de API do Next.js que atua como um proxy dinâmico. Esta rota abstrai todos os detalhes de comunicação com o servidor de jogo, fornecendo ao frontend uma interface limpa e consistente.

A seguir, um exemplo de implementação para o App Router do Next.js, que captura todas as requisições para /api/necesse/\* e as encaminha.

TypeScript

// Arquivo: app/api/necesse/\[...slug\]/route.ts

import { NextResponse } from 'next/server';

export async function GET(  
  request: Request,  
  { params }: { params: { slug: string } }  
) {  
  const slugPath \= params.slug.join('/');  
  const targetUrl \= \`${process.env.NECESSE\_SERVER\_URL}/${slugPath}\`;  
  const apiKey \= process.env.NECESSE\_MOD\_API\_KEY;

  if (\!process.env.NECESSE\_SERVER\_URL ||\!apiKey) {  
    return NextResponse.json(  
      { error: 'A configuração da API do servidor de jogo está incompleta.' },  
      { status: 500 }  
    );  
  }

  try {  
    const gameServerResponse \= await fetch(targetUrl, {  
      method: 'GET',  
      headers: {  
        'X-API-Key': apiKey,  
      },  
      // Recomenda-se um cache baixo para dados dinâmicos do servidor  
      next: { revalidate: 5 },   
    });

    if (\!gameServerResponse.ok) {  
      return NextResponse.json(  
        { error: \`Erro ao comunicar com o servidor Necesse: ${gameServerResponse.statusText}\` },  
        { status: gameServerResponse.status }  
      );  
    }

    const data \= await gameServerResponse.json();  
    return NextResponse.json(data);

  } catch (error) {  
    console.error("Erro no proxy da API Necesse:", error);  
    return NextResponse.json(  
      { error: 'Não foi possível conectar ao servidor do jogo.' },  
      { status: 502 } // Bad Gateway  
    );  
  }  
}

Esta única rota de API gerencia a comunicação para /api/necesse/status, /api/necesse/players, etc., tornando o código do frontend limpo e a arquitetura de backend escalável.6

### **6.2 Busca de Dados no Frontend com React**

Para consumir os dados no frontend, a utilização de uma biblioteca de data-fetching moderna como SWR ou TanStack Query (anteriormente React Query) é altamente recomendada. Essas bibliotecas simplificam o gerenciamento de estado assíncrono, lidando automaticamente com cache, revalidação em intervalos, tratamento de estados de carregamento e erro.

A seguir, um exemplo usando SWR para criar um hook personalizado que busca o status do servidor.

1. **Instale o SWR:** npm install swr  
2. **Crie um hook de busca de dados:**

TypeScript

// Arquivo: hooks/useNecesseApi.ts  
import useSWR from 'swr';

// Um 'fetcher' simples que pode ser reutilizado  
const fetcher \= (url: string) \=\> fetch(url).then((res) \=\> res.json());

// Hook para buscar o status do servidor  
export function useServerStatus() {  
  const { data, error, isLoading } \= useSWR('/api/necesse/status', fetcher, {  
    // Revalida os dados a cada 5 segundos  
    refreshInterval: 5000,  
  });

  return {  
    status: data,  
    isLoading,  
    isError: error,  
  };  
}

// Hook para buscar a lista de jogadores  
export function usePlayerList() {  
    const { data, error, isLoading } \= useSWR('/api/necesse/players', fetcher, {  
      refreshInterval: 10000, // Atualiza a lista de jogadores a cada 10 segundos  
    });  
    
    return {  
      players: data ||, // Retorna um array vazio se os dados ainda não chegaram  
      isLoading,  
      isError: error,  
    };  
}

### **6.3 Construindo um Painel de Servidor em Tempo Real**

Com os hooks personalizados prontos, construir um componente de painel que exibe os dados e se atualiza automaticamente torna-se trivial.

TypeScript

// Arquivo: components/ServerDashboard.tsx  
'use client';

import { useServerStatus, usePlayerList } from '@/hooks/useNecesseApi';

export default function ServerDashboard() {  
  const { status, isLoading: isStatusLoading, isError: isStatusError } \= useServerStatus();  
  const { players, isLoading: arePlayersLoading, isError: isPlayersError } \= usePlayerList();

  if (isStatusLoading) return \<div\>Carregando status do servidor...\</div\>;  
  if (isStatusError) return \<div\>Falha ao carregar o status do servidor.\</div\>;

  return (  
    \<div style={{ fontFamily: 'sans-serif', border: '1px solid \#ccc', padding: '16px', borderRadius: '8px' }}\>  
      \<h1\>{status.name}\</h1\>  
      \<p\>{status.motd}\</p\>  
        
      \<div style={{ display: 'flex', gap: '20px', marginBottom: '20px' }}\>  
        \<span\>\<strong\>Status:\</strong\> \<span style={{ color: 'green' }}\>Online\</span\>\</span\>  
        \<span\>\<strong\>Jogadores:\</strong\> {status.playersOnline} / {status.maxPlayers}\</span\>  
        \<span\>\<strong\>Versão:\</strong\> {status.gameVersion}\</span\>  
        \<span\>\<strong\>PvP:\</strong\> {status.pvpEnabled? 'Ativado' : 'Desativado'}\</span\>  
      \</div\>

      \<h2\>Jogadores Online\</h2\>  
      {arePlayersLoading? (  
        \<p\>Carregando lista de jogadores...\</p\>  
      ) : isPlayersError? (  
        \<p\>Falha ao carregar a lista de jogadores.\</p\>  
      ) : (  
        \<ul\>  
          {players.length \> 0? (  
            players.map((player: any) \=\> (  
              \<li key={player.authId}\>  
                {player.name} (Ping: {player.ping}ms)  
              \</li\>  
            ))  
          ) : (  
            \<p\>Nenhum jogador online.\</p\>  
          )}  
        \</ul\>  
      )}  
    \</div\>  
  );  
}

Este componente usa os hooks useServerStatus e usePlayerList. O SWR gerencia automaticamente as chamadas de API em segundo plano nos intervalos definidos, mantendo a interface do usuário atualizada com os dados mais recentes do servidor de jogo sem a necessidade de lógica manual de polling ou gerenciamento de estado complexo.

## **Conclusão: Implantação e Considerações Futuras**

A arquitetura e os códigos apresentados neste relatório fornecem uma solução completa e robusta para a extração e exibição de dados de um servidor dedicado Necesse. A implementação bem-sucedida requer a atenção a detalhes durante a implantação.

### **Lista de Verificação para Implantação**

1. **Compilação do Mod:** Execute a tarefa buildModJar do Gradle para gerar o arquivo .jar final do mod.  
2. **Instalação do Mod no Servidor:** Copie o arquivo .jar para a pasta mods do seu servidor dedicado Necesse. A localização exata pode variar dependendo do sistema operacional e do provedor de hospedagem.24  
3. **Configuração do Servidor de Jogo:**  
   * Defina a variável de ambiente NECESSE\_MOD\_API\_KEY no ambiente de execução do servidor de jogo com a chave secreta escolhida.  
   * Garanta que a porta utilizada pelo mod (ex: 8080\) não esteja bloqueada por um firewall e esteja acessível a partir do servidor que hospeda a aplicação Next.js.  
4. **Configuração da Aplicação Next.js:**  
   * Preencha o arquivo .env.local com a NECESSE\_MOD\_API\_KEY correspondente e a NECESSE\_SERVER\_URL apontando para o endereço IP e porta do servidor de jogo.  
5. **Implantação da Aplicação Next.js:** Implante a aplicação em uma plataforma de sua escolha (ex: Vercel, Netlify, AWS).  
6. **(Opcional, mas Recomendado) Configuração do Proxy Reverso:** Se o servidor de jogo e o servidor Next.js estiverem em máquinas diferentes, configure um proxy reverso (Nginx, Caddy) na frente do servidor de jogo para gerenciar HTTPS e fornecer uma camada adicional de segurança.

### **Além do Polling: O Caminho para o Tempo Real**

A solução atual, baseada em polling HTTP, é eficaz e relativamente simples de implementar. No entanto, para um painel verdadeiramente "ao vivo", onde as atualizações aparecem instantaneamente, o polling tem limitações, como latência inerente e sobrecarga de requisições.

A evolução natural desta arquitetura é a adoção de **WebSockets**. Um futuro aprimoramento do mod Java poderia incluir a integração de uma biblioteca de WebSocket (como Java-WebSocket). Isso permitiria que o mod estabelecesse uma conexão persistente com o backend Next.js (ou até mesmo diretamente com os clientes, através de um gateway seguro) e enviasse ("push") atualizações de dados em tempo real sempre que um evento relevante ocorresse no jogo (por exemplo, um jogador entra ou sai, o dia vira noite). Isso eliminaria a necessidade de polling constante, resultando em uma aplicação mais eficiente e com uma experiência de usuário superior. A arquitetura de três camadas estabelecida aqui serve como uma base perfeita para essa futura expansão.

#### **Referências citadas**

1. How to Secure Your Public API in Next.js Using API Keys \- DEV Community, acessado em setembro 18, 2025, [https://dev.to/guiolmar/how-to-secure-your-public-api-in-nextjs-using-api-keys-356m](https://dev.to/guiolmar/how-to-secure-your-public-api-in-nextjs-using-api-keys-356m)  
2. Your Next.js API Routes are NOT Secure\! (and how to fix them) \- YouTube, acessado em setembro 18, 2025, [https://www.youtube.com/watch?v=JXeotFdL\_ZY](https://www.youtube.com/watch?v=JXeotFdL_ZY)  
3. How do you guys secure API routes used internally? : r/nextjs \- Reddit, acessado em setembro 18, 2025, [https://www.reddit.com/r/nextjs/comments/1fvdxdf/how\_do\_you\_guys\_secure\_api\_routes\_used\_internally/](https://www.reddit.com/r/nextjs/comments/1fvdxdf/how_do_you_guys_secure_api_routes_used_internally/)  
4. HTTP vs HTTPS \- Difference Between Transfer Protocols \- AWS, acessado em setembro 18, 2025, [https://aws.amazon.com/compare/the-difference-between-https-and-http/](https://aws.amazon.com/compare/the-difference-between-https-and-http/)  
5. POST over HTTP: Is it ever safe to post sensitive information?, acessado em setembro 18, 2025, [https://security.stackexchange.com/questions/266858/post-over-http-is-it-ever-safe-to-post-sensitive-information](https://security.stackexchange.com/questions/266858/post-over-http-is-it-ever-safe-to-post-sensitive-information)  
6. Building APIs with Next.js, acessado em setembro 18, 2025, [https://nextjs.org/blog/building-apis-with-nextjs](https://nextjs.org/blog/building-apis-with-nextjs)  
7. Modding \- Necesse Wiki, acessado em setembro 18, 2025, [https://necessewiki.com/Modding](https://necessewiki.com/Modding)  
8. Gaming \- Necesse \- Modding \- 00 \- Intro to modding \- YouTube, acessado em setembro 18, 2025, [https://www.youtube.com/watch?v=w52jI8cmwNM](https://www.youtube.com/watch?v=w52jI8cmwNM)  
9. idea for a Mod maker ??? :: Necesse Gameplay Discussions \- Steam Community, acessado em setembro 18, 2025, [https://steamcommunity.com/app/1169040/discussions/0/3827550902573069919/](https://steamcommunity.com/app/1169040/discussions/0/3827550902573069919/)  
10. Modding Snippets \- Necesse Wiki, acessado em setembro 18, 2025, [https://necessewiki.com/Modding\_Snippets](https://necessewiki.com/Modding_Snippets)  
11. Gson User Guide \- Google, acessado em setembro 18, 2025, [http://google.github.io/gson/UserGuide.html](http://google.github.io/gson/UserGuide.html)  
12. GSON Java/JSON Serialization/Deserialization Library \- Crank Software, acessado em setembro 18, 2025, [https://resources.cranksoftware.com/cranksoftware/v7.0.0/license/webhelp/ch02s02s04.html](https://resources.cranksoftware.com/cranksoftware/v7.0.0/license/webhelp/ch02s02s04.html)  
13. How To Serialize And Deserialize Interfaces In Java Using Gson | FINRA.org, acessado em setembro 18, 2025, [https://www.finra.org/about/how-we-operate/technology/blog/how-to-serialize-deserialize-interfaces-in-java-using-gson](https://www.finra.org/about/how-we-operate/technology/blog/how-to-serialize-deserialize-interfaces-in-java-using-gson)  
14. JSON Handling With GSON in Java With OOP Essence \- DZone, acessado em setembro 18, 2025, [https://dzone.com/articles/mastering-json-handling-with-gson-in-java](https://dzone.com/articles/mastering-json-handling-with-gson-in-java)  
15. FasterXML/jackson: Main Portal page for the Jackson project \- GitHub, acessado em setembro 18, 2025, [https://github.com/FasterXML/jackson](https://github.com/FasterXML/jackson)  
16. Efficient JSON serialization with Jackson and Java \- Oracle Blogs, acessado em setembro 18, 2025, [https://blogs.oracle.com/javamagazine/post/java-json-serialization-jackson](https://blogs.oracle.com/javamagazine/post/java-json-serialization-jackson)  
17. Document, acessado em setembro 18, 2025, [https://necesse-community.github.io/unofficial-docs/](https://necesse-community.github.io/unofficial-docs/)  
18. Advanced System Monitoring in Java Using OSHI | by Jefster \- Medium, acessado em setembro 18, 2025, [https://medium.com/@dev.jefster/advanced-system-monitoring-in-java-using-oshi-8c5f8df666b4](https://medium.com/@dev.jefster/advanced-system-monitoring-in-java-using-oshi-8c5f8df666b4)  
19. Get Operating System Information in Java using OSHI library \- Simple Solution, acessado em setembro 18, 2025, [https://simplesolution.dev/java-get-operating-system-information-oshi-library/](https://simplesolution.dev/java-get-operating-system-information-oshi-library/)  
20. oshi/oshi: Native Operating System and Hardware Information \- GitHub, acessado em setembro 18, 2025, [https://github.com/oshi/oshi](https://github.com/oshi/oshi)  
21. Routing: API Routes \- Next.js, acessado em setembro 18, 2025, [https://nextjs.org/docs/pages/building-your-application/routing/api-routes](https://nextjs.org/docs/pages/building-your-application/routing/api-routes)  
22. Nextjs API Post Example \- DEV Community, acessado em setembro 18, 2025, [https://dev.to/turingvangisms/nextjs-api-post-example-4ili](https://dev.to/turingvangisms/nextjs-api-post-example-4ili)  
23. How To Send POST Request To External API In NextJS? \- GeeksforGeeks, acessado em setembro 18, 2025, [https://www.geeksforgeeks.org/reactjs/how-to-send-post-request-to-external-api-in-nextjs/](https://www.geeksforgeeks.org/reactjs/how-to-send-post-request-to-external-api-in-nextjs/)  
24. Necesse | How to install mods to server \- Knowledgebase \- Pingperfect Ltd, acessado em setembro 18, 2025, [https://pingperfect.com/knowledgebase/1132/Necesse--How-to-install-mods-to-server.html](https://pingperfect.com/knowledgebase/1132/Necesse--How-to-install-mods-to-server.html)  
25. How to install mods on a Necesse server \- Knowledgebase \- BisectHosting, acessado em setembro 18, 2025, [https://www.bisecthosting.com/clients/index.php?rp=/knowledgebase/1793](https://www.bisecthosting.com/clients/index.php?rp=/knowledgebase/1793)  
26. How to Install Mods on your Necesse Server \- Knowledgebase \- Shockbyte, acessado em setembro 18, 2025, [https://shockbyte.com/billing/knowledgebase/961/How-to-Install-Mods-on-your-Necesse-Server.html](https://shockbyte.com/billing/knowledgebase/961/How-to-Install-Mods-on-your-Necesse-Server.html)  
27. How to Install Mods on your Necesse Server \- Knowledgebase, acessado em setembro 18, 2025, [https://www.citadelservers.com/client/knowledgebase/567/How-to-Install-Mods-on-your-Necesse-Server.html](https://www.citadelservers.com/client/knowledgebase/567/How-to-Install-Mods-on-your-Necesse-Server.html)