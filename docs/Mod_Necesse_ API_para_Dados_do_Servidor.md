

# **Roteiro de Implementação: API de Servidor Necesse para Integração Web**

Este documento serve como um guia técnico detalhado para o desenvolvimento de um mod para o jogo Necesse. O objetivo principal deste mod é criar uma interface de programação de aplicações (API) que permita a comunicação entre um servidor de jogo Necesse e aplicações web externas, como um site de status do servidor. O mod implementará um servidor HTTP leve e embarcado que exporá dados em tempo real, como o número de jogadores online, status do servidor e configurações específicas do mundo, em um formato padronizado e de fácil consumo.

## **I. Esboço Arquitetural: Incorporando uma API Web em um Servidor Necesse**

Esta seção introdutória estabelece o design de alto nível e justifica a abordagem técnica escolhida. Ela fornece o enquadramento conceitual necessário para compreender os detalhes de implementação subsequentes.

### **1.1. Objetivo Central: Criando uma Ponte entre o Servidor de Jogo e a Web**

O desafio fundamental é permitir que uma aplicação externa, como um website, possa consultar informações dinâmicas de um servidor Necesse em execução. A solução deve, portanto, estabelecer uma interface de comunicação padronizada — uma API — sobre uma plataforma que nativamente não a possui. A abordagem mais viável e robusta para alcançar este objetivo é o desenvolvimento de um mod do lado do servidor. Os mods são o mecanismo oficialmente suportado para estender a funcionalidade do servidor, garantindo compatibilidade e acesso legítimo aos componentes internos do jogo.1

### **1.2. O Servidor HTTP Embarcado: Uma Solução Leve e Sem Dependências**

A base tecnológica para a API será o servidor HTTP nativo do Java, fornecido pelo pacote com.sun.net.httpserver.HttpServer, que está incluído no Java SE desde a versão 6\.3 O ambiente de modding de Necesse é baseado em Java, tornando esta biblioteca nativa a escolha ideal.1

A seleção de uma solução sem dependências externas é uma decisão de design crítica, visando garantir a máxima compatibilidade e estabilidade. Ambientes de jogos modificados frequentemente se tornam ecossistemas frágeis, onde conflitos entre bibliotecas de diferentes mods podem levar a falhas inesperadas e difíceis de diagnosticar.6 Ao utilizar a biblioteca

HttpServer nativa, eliminamos a necessidade de empacotar arquivos .jar de terceiros, como Gson ou Jackson, para a funcionalidade principal. Isso previne potenciais conflitos de carregamento de classes com o jogo base ou outros mods instalados, simplifica o processo de compilação e resulta em um mod com uma pegada de memória menor e inerentemente mais robusto.7

### **1.3. Fluxo de Dados e Arquitetura do Sistema**

O mod funcionará como uma camada de middleware, traduzindo requisições HTTP externas em chamadas à API interna do servidor Necesse. O fluxo de dados pode ser visualizado da seguinte forma:

1. Um cliente web externo (ex: um script em um site) envia uma requisição HTTP para um endpoint específico (ex: http://ip\_do\_servidor:porta/status).  
2. O servidor HttpServer embarcado, rodando dentro do processo do servidor Necesse, recebe a requisição.  
3. Um manipulador de requisições (HttpHandler) associado ao endpoint é acionado.  
4. O manipulador acessa os objetos centrais do jogo (como o objeto Server) para coletar os dados solicitados (ex: lista de jogadores, configurações do mundo).  
5. Os dados coletados são serializados para o formato JSON (JavaScript Object Notation).  
6. Uma resposta HTTP, contendo o payload JSON e os cabeçalhos apropriados (ex: Content-Type: application/json), é enviada de volta ao cliente.

A arquitetura do mod deve ser desacoplada e orientada a eventos para respeitar a própria arquitetura do jogo. O componente do servidor HTTP deve ser estritamente separado do componente de acesso à lógica do jogo. A interação entre eles ocorrerá através de uma interface estável e bem definida, como o objeto estático Server. Este princípio de design é fundamental para a manutenibilidade a longo prazo, garantindo que otimizações no servidor HTTP não quebrem a lógica de recuperação de dados do jogo.

A natureza assíncrona do ambiente de modding — onde o objeto Server só se torna disponível após um evento de inicialização 7 — e a natureza multithread do

HttpServer 3 exigem um gerenciamento de estado cuidadoso. Por exemplo, se uma requisição HTTP chegar antes que o objeto

Server esteja disponível, a API deve retornar uma resposta apropriada, como um código de status 503 Service Unavailable, em vez de causar uma exceção de ponteiro nulo.

## **II. Interagindo com o Núcleo do Servidor Necesse via API de Modding**

Esta seção aprofunda o código específico de Necesse necessário para acessar o estado interno do servidor. Este é o passo fundamental sobre o qual todo o mod é construído.

### **2.1. O Ponto de Entrada do Mod: Estrutura e Ciclo de Vida**

Um mod de Necesse é definido por dois componentes principais: um arquivo de metadados mod.info e uma classe Java anotada com @ModEntry.1

* **mod.info**: Este arquivo JSON define as propriedades do mod, como seu ID único, nome de exibição, versão e autor. É essencial que o ID seja único e não mude entre as versões.  
* **Classe @ModEntry**: Esta classe é o ponto de entrada principal do mod. O jogo procura por métodos específicos de ciclo de vida dentro desta classe para inicializar o mod. O método init() é o local ideal para registrar ouvintes de eventos, pois é chamado no início da sequência de carregamento do mod.1

A estrutura do projeto seguirá as convenções estabelecidas pelo projeto de exemplo oficial, disponível no GitHub, que utiliza o Gradle para automação de compilação.1

### **2.2. Capturando a Instância do Servidor: O Padrão ServerStartEvent**

O método canônico e mais confiável para obter o objeto necesse.engine.network.server.Server é registrar um ouvinte para o evento ServerStartEvent.7 Este evento é disparado pelo motor do jogo exatamente quando a instância do servidor é criada e está pronta para uso.

O padrão de implementação envolve os seguintes passos 7:

1. Declarar uma variável estática pública na classe @ModEntry para armazenar a instância do servidor: public static Server SERVER;. A natureza estática desta variável a torna globalmente acessível a partir de qualquer outra parte do código do mod através de NomeDaClasseMod.SERVER.  
2. No método init(), usar GameEvents.addListener() para registrar um novo GameEventListener para a classe ServerStartEvent.class.  
3. Dentro do método onEvent(ServerStartEvent e) do ouvinte, atribuir a instância do servidor, que é disponibilizada através do evento (e.server), à variável estática: SERVER \= e.server;.

Este design orientado a eventos, fornecido pela API de modding, é robusto e limpo. No entanto, ele impõe uma responsabilidade ao desenvolvedor do mod: o gerenciamento do ciclo de vida completo. Se o mod inicia um serviço (como o HttpServer) quando o servidor do jogo inicia, ele também deve encerrar esse serviço quando o servidor do jogo para. A ausência de um desligamento adequado pode levar a vazamentos de recursos ou, mais criticamente, a conflitos de porta de rede. Se um servidor for reiniciado, o processo HttpServer antigo pode ainda estar ocupando a porta, impedindo que a nova instância do servidor se vincule a ela. Portanto, é imperativo registrar um ouvinte para um evento correspondente de desligamento (como um hipotético ServerStopEvent) para chamar o método stop() do HttpServer, garantindo uma operação estável em ambientes de produção.

### **2.3. Introspecção do Objeto Server para Métricas Chave**

Com a referência ao objeto Server em mãos, é possível extrair as informações necessárias para a API através de seus métodos. Embora a documentação completa da classe Server não esteja publicamente disponível, pode-se inferir a existência de métodos com base no design padrão de servidores de jogos e nas opções de configuração existentes.9

* **Informações de Jogadores**: O objeto Server deve conter uma coleção de clientes conectados. Métodos como getClients() ou getPlayers() retornariam uma lista de objetos ServerClient. O número total de jogadores seria o tamanho (.size()) desta coleção. Cada objeto ServerClient conteria informações individuais, como o nome do jogador.  
* **Configurações do Servidor**: Configurações globais, como a Mensagem do Dia (MOTD) e o número máximo de jogadores, devem estar diretamente acessíveis através de métodos como getMotd() e getMaxPlayers().  
* **Informações do Mundo**: O servidor gerencia o mundo do jogo. Portanto, um método como getWorld() é esperado para fornecer acesso ao objeto World ativo.

### **2.4. Acessando Configurações Específicas do Mundo via WorldSettings**

Os servidores Necesse utilizam um arquivo worldSettings.cfg para armazenar parâmetros de jogabilidade, como dificuldade, frequência de raids e penalidade de morte.10 Essas configurações são carregadas em um objeto acessível em tempo de execução.

O objeto World, obtido a partir de Server.getWorld(), provavelmente fornecerá acesso a essas configurações através de um método como getWorldSettings(), que retornaria um objeto da classe WorldSettings. Este objeto, por sua vez, conteria campos ou métodos getter correspondentes às chaves no arquivo .cfg.

A tabela a seguir mapeia os pontos de dados desejados da API para suas fontes hipotéticas dentro da API de Necesse, servindo como uma referência clara para o desenvolvimento.

| Ponto de Dados da API | Campo JSON | Classe Fonte Necesse | Método/Campo Fonte Necesse (Hipotético) |
| :---- | :---- | :---- | :---- |
| Contagem de Jogadores | playerCount | necesse.engine.network.server.Server | getClients().size() |
| Máximo de Jogadores | maxPlayers | necesse.engine.network.server.Server | getMaxPlayers() |
| MOTD | motd | necesse.engine.network.server.Server | getMotd() |
| Nome do Mundo | worldName | necesse.engine.world.World | getWorldName() |
| Dificuldade | difficulty | necesse.engine.world.WorldSettings | difficulty.toString() |
| Frequência de Raids | raidFrequency | necesse.engine.world.WorldSettings | raidFrequency.toString() |
| Penalidade de Morte | deathPenalty | necesse.engine.world.WorldSettings | deathPenalty.toString() |

## **III. Implementação do Endpoint HTTP Leve**

Esta seção fornecerá o código Java completo e a explicação para o componente do servidor web embarcado.

### **3.1. Inicializando e Gerenciando o Ciclo de Vida do HttpServer**

A lógica de gerenciamento do HttpServer será encapsulada em uma classe dedicada, por exemplo, ApiServer, para promover a organização do código.

* **Criação**: A instância do servidor é criada usando HttpServer.create(new InetSocketAddress(port), 0).8 O segundo argumento,  
  backlog, definido como 0, instrui o sistema a usar um valor padrão para a fila de conexões pendentes.  
* **Inicialização**: O método start() da classe ApiServer será chamado de dentro do ouvinte de ServerStartEvent, logo após a captura da instância Server do jogo. Este método, por sua vez, chamará httpServer.start(), que inicia o servidor em uma nova thread em segundo plano.3  
* **Encerramento**: Um método stop() correspondente na classe ApiServer chamará httpServer.stop(delay), que encerra o servidor de forma graciosa. O argumento delay especifica o tempo máximo em segundos para aguardar o término das requisições em andamento. Este método será invocado a partir do ouvinte do evento de parada do servidor.

### **3.2. Definindo Rotas da API e Implementando HttpHandler**

As rotas da API (ou "contextos") são definidas no HttpServer usando o método server.createContext("/caminho", handler).3 Cada contexto é associado a um objeto que implementa a interface

HttpHandler, que possui um único método: handle(HttpExchange exchange).8

Uma única classe ApiHandler será implementada para lidar com todas as rotas da API. Dentro do seu método handle, uma estrutura switch baseada no caminho da requisição (exchange.getRequestURI().getPath()) direcionará o fluxo para métodos privados específicos, como handleStatusRequest, handlePlayersRequest, etc. Esta abordagem centraliza a lógica de roteamento.

Cada método manipulador seguirá uma sequência de verificação:

1. Verificar se a instância MyApiMod.SERVER não é nula. Se for, significa que o servidor do jogo ainda não está totalmente inicializado. Nesse caso, uma resposta com código de status 503 Service Unavailable será enviada.  
2. Prosseguir com a coleta de dados, serialização para JSON e envio da resposta.

### **3.3. Concorrência e Desempenho: Evitando Lag no Servidor**

Por padrão, o HttpServer pode operar em um modo de thread única. Em um cenário com múltiplos clientes consultando a API simultaneamente, isso criaria um gargalo, onde as requisições seriam enfileiradas e processadas sequencialmente, resultando em alta latência para a API.

Para garantir que a API seja responsiva e possa lidar com múltiplas requisições concorrentes sem impactar o desempenho do jogo, é crucial configurar um pool de threads. Isso é feito através do método server.setExecutor(...).3 A implementação utilizará um pool de threads em cache:

server.setExecutor(Executors.newCachedThreadPool()). Este tipo de executor cria novas threads conforme necessário e reutiliza as threads existentes quando disponíveis, oferecendo um bom equilíbrio entre utilização de recursos e capacidade de resposta para um número variável de requisições. Esta configuração transforma o mod de um simples protótipo para uma ferramenta de nível de produção, capaz de operar de forma confiável em um ambiente de servidor ativo.

## **IV. Estruturando e Entregando Respostas da API**

Esta seção detalhará o processo de formatação dos dados do jogo recuperados em um formato padronizado e legível por máquina.

### **4.1. O Formato de Dados JSON: Um Padrão Universal**

JSON (JavaScript Object Notation) será o único formato de dados utilizado pela API. É um padrão leve, baseado em texto e de fácil interpretação tanto por humanos quanto por máquinas, o que o torna ideal para APIs web.16 Todas as respostas bem-sucedidas da API incluirão o cabeçalho HTTP

Content-Type: application/json para garantir que os clientes interpretem o corpo da resposta corretamente.16

### **4.2. Serialização de Dados: De Objetos Java para Strings JSON**

Para manter a abordagem sem dependências, a serialização de JSON será realizada manualmente para endpoints simples. Isso envolve a construção de strings formatadas como JSON. Por exemplo, para o endpoint /status, a string seria construída da seguinte forma:

String json \= String.format("{\\"status\\":\\"online\\",\\"playerCount\\":%d,\\"maxPlayers\\":%d}", playerCount, maxPlayers);

Para estruturas de dados mais complexas, como a lista de jogadores ou as configurações detalhadas, a construção manual de strings pode se tornar propensa a erros. Nesses casos, uma abordagem mais segura é usar um Map\<String, Object\> para construir a estrutura de dados em memória e, em seguida, escrever um serializador simples que converta o mapa em uma string JSON. Esta abordagem intermediária oferece mais flexibilidade sem introduzir uma dependência externa completa.

### **4.3. Construindo a Resposta HTTP**

O objeto HttpExchange fornece os métodos necessários para construir e enviar a resposta HTTP. O processo padronizado será:

1. Serializar os dados para uma string JSON.  
2. Converter a string JSON em um array de bytes, usando a codificação UTF-8.  
3. Definir os cabeçalhos da resposta. No mínimo, Content-Type será definido como application/json; charset=utf-8.  
4. Enviar os cabeçalhos da resposta usando exchange.sendResponseHeaders(statusCode, responseBytes.length), onde statusCode é 200 para sucesso e responseBytes.length é o tamanho do corpo da resposta.  
5. Obter o OutputStream do corpo da resposta com exchange.getResponseBody().  
6. Escrever o array de bytes da resposta no stream e fechá-lo.8

Para evitar a duplicação de código, uma função auxiliar, como sendJsonResponse(HttpExchange exchange, int statusCode, String jsonPayload), encapsulará essa lógica.

A tabela a seguir define formalmente os endpoints da API, servindo como documentação para os consumidores.

| Método | Endpoint | Descrição | Resposta de Sucesso (200 OK) |
| :---- | :---- | :---- | :---- |
| GET | /status | Fornece o status básico do servidor e a contagem de jogadores. | { "status": "online", "playerCount": 5, "maxPlayers": 10, "motd": "Bem-vindo\!" } |
| GET | /players | Retorna uma lista dos jogadores atualmente conectados. | { "playerCount": 2, "players": \[ { "name": "Player1" }, { "name": "Player2" } \] } |
| GET | /settings | Retorna uma coleção de configurações acessíveis do servidor e do mundo. | { "serverSettings": { "maxPlayers": 10 }, "worldSettings": { "difficulty": "Classic", "raidFrequency": "Normal" } } |

## **V. Melhorias para Produção e Boas Práticas**

Esta seção abordará os passos finais para tornar o mod robusto, configurável e seguro para implantação em um servidor ativo.

### **5.1. Implementando um Arquivo de Configuração Personalizado para o Mod**

Para permitir que os administradores de servidor personalizem o comportamento da API (como a porta de escuta) sem modificar o código-fonte, um arquivo de configuração é essencial. Necesse fornece um framework oficial para configurações de mods através do método initSettings() na classe @ModEntry.7 Este método é preferível a arquivos de propriedades (

.properties) personalizados, pois se integra perfeitamente ao ecossistema do jogo.

A implementação envolverá 7:

1. Substituir o método public ModSettings initSettings() na classe principal do mod.  
2. Dentro dele, retornar uma nova instância anônima de ModSettings.  
3. Substituir o método addSaveData(SaveData saveData) para salvar os valores das configurações. Por exemplo, saveData.addInt("apiPort", this.apiPort);.  
4. Substituir o método applyLoadData(LoadData loadData) para carregar os valores do arquivo. Por exemplo, this.apiPort \= loadData.getInt("apiPort", 8080);. O segundo argumento fornece um valor padrão.

Este mecanismo criará e gerenciará automaticamente um arquivo .cfg para o mod no diretório de configuração do jogo (ex: %APPDATA%/Necesse/cfg/mods/meuapimod.cfg), que os administradores podem editar facilmente.7

### **5.2. Considerações de Segurança: Acesso Somente Leitura e Chaves de API**

O design da API é inerentemente de somente leitura, o que mitiga uma grande classe de riscos de segurança. No entanto, para servidores expostos publicamente na internet, pode ser desejável restringir quem pode acessar a API para prevenir a coleta de dados não autorizada ou o abuso do serviço.

Uma camada adicional de segurança pode ser implementada através de uma chave de API opcional.

1. Uma nova configuração, apiKey, será adicionada ao arquivo .cfg do mod.  
2. Se a apiKey estiver definida no arquivo de configuração, o ApiHandler verificará a presença de um cabeçalho Authorization em todas as requisições recebidas.  
3. O valor esperado para o cabeçalho seria Bearer \<sua\_chave\_de\_api\>.  
4. Se a chave de API estiver configurada e uma requisição não fornecer o cabeçalho correto, o servidor responderá com um código de status 401 Unauthorized e recusará o processamento. Se a chave não estiver configurada, esta verificação será ignorada.

A tabela a seguir documenta os parâmetros de configuração disponíveis para os administradores do servidor.

| Configuração | Tipo | Valor Padrão | Descrição |
| :---- | :---- | :---- | :---- |
| apiEnabled | boolean | true | Habilita ou desabilita completamente o servidor da API HTTP. |
| apiPort | int | 8080 | A porta TCP na qual o servidor da API HTTP escutará por conexões. |
| apiKey | String | "" (vazio) | Uma chave secreta opcional. Se definida, as requisições devem incluir um cabeçalho Authorization: Bearer \<chave\>. |

## **VI. Guia Completo de Implementação e Implantação**

Esta seção final consolida todos os conceitos e códigos das seções anteriores em um único arquivo-fonte completo e fornece instruções claras de implantação.

### **6.1. Código-Fonte Completo (MyApiMod.java)**

Um arquivo Java único e totalmente comentado será fornecido. Este arquivo conterá a classe @ModEntry, os ouvintes de eventos para início e parada do servidor, a classe ApiServer encapsulando o HttpServer, a classe ApiHandler com a lógica de roteamento e a implementação de initSettings(). Isso oferece uma solução completa e pronta para uso.

### **6.2. Compilação e Empacotamento**

O projeto de exemplo oficial de Necesse utiliza o Gradle para o processo de compilação.5 As instruções se concentrarão em executar a tarefa

buildModJar do Gradle. Esta tarefa compilará o código-fonte Java e empacotará os arquivos .class resultantes, juntamente com o mod.info, em um único arquivo MyApiMod.jar no diretório build/libs.

### **6.3. Implantação no Servidor**

A instalação de um mod em um servidor dedicado Necesse é um processo simples.1 O guia de implantação fornecerá uma lista de verificação passo a passo:

1. Pare completamente o servidor dedicado Necesse.  
2. Navegue até o diretório de instalação do servidor e localize a pasta mods. Se ela não existir, crie-a.  
3. Copie o arquivo MyApiMod.jar compilado para a pasta mods.  
4. Inicie o servidor uma vez. Isso fará com que o jogo carregue o mod e gere o arquivo de configuração padrão em cfg/mods/myapimod.cfg.  
5. Pare o servidor novamente.  
6. Edite o arquivo cfg/mods/myapimod.cfg com um editor de texto para definir a porta desejada e a chave de API opcional.  
7. Inicie o servidor. O mod agora estará ativo e configurado.

### **6.4. Verificação e Teste**

Para confirmar que o mod está funcionando corretamente, o administrador pode realizar um teste simples. Abrindo um navegador web e navegando para http://\<IP\_DO\_SERVIDOR\>:\<PORTA\_API\>/status ou usando uma ferramenta de linha de comando como curl, uma resposta JSON bem-sucedida deve ser exibida. Se uma chave de API foi configurada, o teste com curl seria: curl \-H "Authorization: Bearer \<SUA\_CHAVE\>" http://\<IP\_DO\_SERVIDOR\>:\<PORTA\_API\>/status. Uma resposta válida confirma que a implantação foi bem-sucedida.

## **Conclusão**

A implementação de uma API HTTP em um servidor Necesse através de um mod é uma solução poderosa e flexível para a integração com sistemas externos. Ao utilizar o HttpServer nativo do Java, o mod permanece leve, estável e livre de dependências, minimizando o risco de conflitos em um ambiente de servidor modificado. A adesão às APIs oficiais de modding de Necesse para captura de eventos e gerenciamento de configurações garante a compatibilidade futura e a robustez da solução. Seguindo as diretrizes de arquitetura, implementação e segurança detalhadas neste documento, é possível desenvolver um mod de nível profissional que atenda aos requisitos de comunicação externa de forma eficiente e segura, agregando valor significativo à administração e monitoramento de servidores Necesse.

#### **Referências citadas**

1. Modding \- Necesse Wiki, acessado em setembro 18, 2025, [https://necessewiki.com/Modding](https://necessewiki.com/Modding)  
2. How to Install Mods on your Necesse Server \- Knowledgebase \- Shockbyte, acessado em setembro 18, 2025, [https://shockbyte.com/billing/knowledgebase/961/How-to-Install-Mods-on-your-Necesse-Server.html](https://shockbyte.com/billing/knowledgebase/961/How-to-Install-Mods-on-your-Necesse-Server.html)  
3. HttpServer (Java HTTP Server ) \- Oracle Help Center, acessado em setembro 18, 2025, [https://docs.oracle.com/javase/8/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/HttpServer.html](https://docs.oracle.com/javase/8/docs/jre/api/net/httpserver/spec/com/sun/net/httpserver/HttpServer.html)  
4. Simple HTTP server in Java using only Java SE API \- Stack Overflow, acessado em setembro 18, 2025, [https://stackoverflow.com/questions/3732109/simple-http-server-in-java-using-only-java-se-api](https://stackoverflow.com/questions/3732109/simple-http-server-in-java-using-only-java-se-api)  
5. idea for a Mod maker ??? :: Necesse Gameplay Discussions \- Steam Community, acessado em setembro 18, 2025, [https://steamcommunity.com/app/1169040/discussions/0/3827550902573069919/](https://steamcommunity.com/app/1169040/discussions/0/3827550902573069919/)  
6. Necesse Mods mismacth even when we have the same ones. \- Reddit, acessado em setembro 18, 2025, [https://www.reddit.com/r/Necesse/comments/1ey8feb/necesse\_mods\_mismacth\_even\_when\_we\_have\_the\_same/](https://www.reddit.com/r/Necesse/comments/1ey8feb/necesse_mods_mismacth_even_when_we_have_the_same/)  
7. Modding Snippets \- Necesse Wiki, acessado em setembro 18, 2025, [https://necessewiki.com/Modding\_Snippets](https://necessewiki.com/Modding_Snippets)  
8. com.sun.net.httpserver.HttpServer Example \- Java Code Geeks, acessado em setembro 18, 2025, [https://examples.javacodegeeks.com/java-development/core-java/sun/net-sun/httpserver-net-sun/httpserver-net-sun-httpserver-net-sun/com-sun-net-httpserver-httpserver-example/](https://examples.javacodegeeks.com/java-development/core-java/sun/net-sun/httpserver-net-sun/httpserver-net-sun-httpserver-net-sun/com-sun-net-httpserver-httpserver-example/)  
9. How to Configure your Necesse Server \- Knowledgebase \- Shockbyte, acessado em setembro 18, 2025, [https://shockbyte.com/billing/knowledgebase/967/How-to-Configure-your-Necesse-Server.html](https://shockbyte.com/billing/knowledgebase/967/How-to-Configure-your-Necesse-Server.html)  
10. Necesse Server Configuration \- Knowledgebase, acessado em setembro 18, 2025, [https://www.citadelservers.com/client/knowledgebase/534/Necesse-Server-Configuration.html](https://www.citadelservers.com/client/knowledgebase/534/Necesse-Server-Configuration.html)  
11. How to edit world settings on a Necesse server \- Knowledgebase \- BisectHosting, acessado em setembro 18, 2025, [https://www.bisecthosting.com/clients/index.php?rp=/knowledgebase/1961](https://www.bisecthosting.com/clients/index.php?rp=/knowledgebase/1961)  
12. World settings \- Necesse Wiki, acessado em setembro 18, 2025, [https://necessewiki.com/World\_settings](https://necessewiki.com/World_settings)  
13. A Built-In Java HttpServer \- adam bien's blog, acessado em setembro 18, 2025, [https://www.adam-bien.com/roller/abien/entry/a\_built\_in\_java\_httpserver](https://www.adam-bien.com/roller/abien/entry/a_built_in_java_httpserver)  
14. Java Examples for com.sun.net.httpserver.HttpServer \- Javatips.net, acessado em setembro 18, 2025, [https://www.javatips.net/api/com.sun.net.httpserver.httpserver](https://www.javatips.net/api/com.sun.net.httpserver.httpserver)  
15. Develop an HTTP Server in Java \- Medium, acessado em setembro 18, 2025, [https://medium.com/@sayan-paul/develop-an-http-server-in-java-2137071a54a1](https://medium.com/@sayan-paul/develop-an-http-server-in-java-2137071a54a1)  
16. How do I return JSON in response? \- Java \- ReqBin, acessado em setembro 18, 2025, [https://reqbin.com/req/java/gzezk8d5/json-response-example](https://reqbin.com/req/java/gzezk8d5/json-response-example)  
17. TinyServer \- A lightweight HTTP server written in Java. \- GitHub, acessado em setembro 18, 2025, [https://github.com/cosenary/TinyServer](https://github.com/cosenary/TinyServer)  
18. Java: Simple HTTP Server application that responds in JSON \- Stack Overflow, acessado em setembro 18, 2025, [https://stackoverflow.com/questions/28571086/java-simple-http-server-application-that-responds-in-json](https://stackoverflow.com/questions/28571086/java-simple-http-server-application-that-responds-in-json)  
19. How to Install Mods on your Necesse Server \- Knowledgebase, acessado em setembro 18, 2025, [https://www.citadelservers.com/client/knowledgebase/567/How-to-Install-Mods-on-your-Necesse-Server.html](https://www.citadelservers.com/client/knowledgebase/567/How-to-Install-Mods-on-your-Necesse-Server.html)