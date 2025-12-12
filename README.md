# Googol Search Engine

**Googol** é um motor de busca distribuído desenvolvido em Java, JavaScript e HTML utilizando HTTPS / WebSocket / RMI para comunicação entre componentes. O sistema é composto por múltiplos servidores que trabalham em conjunto para indexar, armazenar e pesquisar páginas web de forma eficiente e escalável.

## 📋 Índice

- [Arquitetura do Sistema](#-arquitetura-do-sistema)
- [Requisitos](#-requisitos)
- [Configuração](#-configuração)
- [Instalação e Execução](#-instalação-e-execução)
- [Componentes do Sistema](#-componentes-do-sistema)
- [Funcionalidades](#-funcionalidades)
- [Interface Web](#-interface-web)
- [APIs Externas](#-apis-externas)
- [Estrutura do Projeto](#-estrutura-do-projeto)

---

## 🏗 Arquitetura do Sistema

O Googol é composto por **4 componentes principais** que comunicam entre si via RMI:

```
┌─────────────────┐
│   Web Server    │ ← Interface do utilizador (Spring Boot)
│   (Port 8080)   │
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│ Gateway Server  │ ← Coordenador central do sistema
│   (Port 1099)   │
└────┬────────┬───┘
     │        │
     ▼        ▼
┌─────────┐  ┌──────────────┐
│ Crawler │  │ Barrel       │
│ Server  │→ │ Servers (N)  │
│(Port    │  │              │
│ 1100)   │  │              │
└─────────┘  └──────────────┘
```

### Fluxo de Dados:
1. **Web Server** recebe pedidos HTTP/WebSocket dos utilizadores
2. **Gateway Server** coordena as operações entre componentes
3. **Crawler Server** descarrega e processa páginas web
4. **Barrel Servers** armazenam e indexam os dados para pesquisa

---

## 🔧 Requisitos

### Software Necessário:
- **Java JDK 25** ou superior
- **Maven 3.6+** (para gestão de dependências)
- **Spring Boot 4.0.0**

### Dependências Principais:
- Spring Boot Starter Web
- Spring Boot Starter Thymeleaf
- Spring Boot Starter WebSocket
- Jsoup 1.18.1 (parsing HTML)
- Gson 2.11.0 (serialização JSON)
- Google GenAI 1.0.0 (integração com IA)

---

## ⚙️ Configuração

### Ficheiro de Configuração

Edite o ficheiro `src/main/resources/files/SystemConfiguration` para ajustar os parâmetros do sistema:

```properties
###############################
# SYSTEM CONFIGURATION EXAMPLE
###############################

# Gateway Server
gateway.host=localhost
gateway.portRMI=1099
gateway.serviceName=Gateway

# Crawler Server
crawler.host=localhost
crawler.portRMI=1100
crawler.serviceName=CrawlerServer
crawler.queue=URLQueue
crawler.numberOfCrawlers=3      # Número de threads crawler
crawler.readFile=true            # Carregar URLs do ficheiro inicial

# Barrel Servers
barrels.numberOfBarrels=5        # Número de barrels a executar
barrels.serviceNames=Barrel 1,Barrel 2,Barrel 3,Barrel 4,Barrel 5
```

### Configuração do Web Server

Edite `src/main/resources/static/config.json`:

```json
{
  "serverIP": "localhost",
  "serverPort": 8080
}
```

### URLs Iniciais

Adicione URLs para indexação inicial em `src/main/resources/files/URLs`:

```
###########
URL EXAMPLE
###########
https://www.example.com
https://www.wikipedia.org
https://www.github.com
```

---

## 🚀 Instalação e Execução

### 1. Compilar o Projeto

```bash
mvnw.cmd clean install
```

### 2. Ordem de Inicialização

Os servidores não têm uma ordem de inicialização fixa, no entanto recomenda-se iniciar da seguinte forma:

#### **Passo 1: Web Server**
```bash
mvnw.cmd spring-boot:run
```

#### **Passo 2: Gateway Server**

#### **Passo 3: Crawler Server**

#### **Passo 4: Barrel Servers (N instâncias)**

### 3. Aceder à Interface Web

Abra o navegador em: **http://{serverIP}:{serverPort}**

---

## 🔌 Componentes do Sistema

### 1. **Gateway Server** (Coordenador Central)

**Responsabilidades:**
- Gerir registo e descoberta de Barrel Servers
- Distribuir pedidos de pesquisa entre barrels disponíveis
- Monitorizar estado e desempenho dos barrels
- Coordenar operações de indexação
- Fornecer estatísticas do sistema

**Portas:**
- RMI Registry: `1099` (configurável)

**Funcionalidades:**
- Registo automático de barrels
- Load balancing baseado em tempo de resposta
- Sistema de backup automático entre barrels
- Ping periódico para detetar barrels offline

---

### 2. **Crawler Server** (Downloader & Parser)

**Responsabilidades:**
- Descarregar páginas web via HTTP
- Extrair conteúdo (título, texto, links)
- Processar tokens (palavras-chave)
- Distribuir dados processados pelos Barrel Servers

**Portas:**
- RMI Registry: `1100` (configurável)

**Características:**
- Multi-threaded (número de crawlers configurável)
- Fila de URLs com controlo de duplicados
- Parser HTML com Jsoup
- Filtro de stop words (PT/EN)
- Limite de 100 tokens por página
- Timeout de 5 segundos por pedido HTTP

**Algoritmo de Crawling:**
1. Obter URL da fila
2. Descarregar página (Jsoup)
3. Extrair título, snippet (250 chars), keywords
4. Tokenizar texto (remover stop words)
5. Extrair links absolutos
6. Enviar `PageData` para todos os barrels
7. Adicionar novos links à fila

---

### 3. **Barrel Servers** (Storage & Indexing)

**Responsabilidades:**
- Armazenar dados de páginas indexadas
- Manter índices invertidos (token → URLs)
- Realizar pesquisas eficientes
- Fornecer links que apontam para uma URL
- Sistema de backup entre barrels

**Estruturas de Dados:**

```java
// Índice de cabeçalhos (URL → Título + Snippet)
HashMap<String, PageHeader> pageHeaderIndex

// Índice invertido (Token → Set<URLs>)
ConcurrentHashMap<String, Set<String>> tokenIndex

// Índice de links (URL → Set<URLs que apontam para ela>)
ConcurrentHashMap<String, Set<String>> linkIndex
```

**Características:**
- Suporte para múltiplas instâncias (replicação)
- Backup automático ao iniciar
- Thread-safe com `ConcurrentHashMap`
- Ranking por número de links recebidos
- Pesquisa por interseção de tokens

---

### 4. **Web Server** (Interface do Utilizador)

**Tecnologias:**
- Spring Boot 4.0.0
- Thymeleaf (templates)
- WebSocket (atualizações em tempo real)
- REST API

**Portas:**
- HTTP Server: `8080` (padrão Spring Boot)

---

## 🎯 Funcionalidades

### 1. **Pesquisa de Páginas** (`/search`)
- Pesquisa por palavras-chave (tokens)
- Paginação de resultados (10 por página)
- Ranking por número de backlinks
- **AI Summary**: Resumo gerado por IA (Gemini) na primeira página
- Resultados ordenados por relevância

**Exemplo de Pesquisa:**
```
Query: "java programming"
→ Procura páginas que contêm "java" E "programming"
→ Ordena por número de links a apontar para essa página
→ Retorna 10 resultados por página
```

### 2. **Indexação de URLs** (`/index`)
- Submeter novas URLs para indexação
- Verificação de duplicados
- Validação de formato de URL
- Feedback em tempo real

### 3. **Links para URL** (`/links`)
- Descobrir que páginas apontam para uma URL específica
- Útil para análise de backlinks
- Visualização de páginas que referenciam um site

### 4. **Estatísticas do Sistema** (`/stats`)
- **TOP 10 Pesquisas** mais realizadas
- **Estado dos Barrels** (online/offline)
- **Métricas por Barrel:**
    - Páginas recebidas
    - Páginas no índice
    - Número de tokens
    - Associações token-URL
    - URLs únicas
    - Linking URLs
    - Tempo médio de resposta
    - Número de pedidos
- **Atualizações em tempo real** via WebSocket

---

## 🌐 Interface Web

### Páginas Principais:

| Rota | Descrição |
|------|-----------|
| `/` | Página inicial com menu de navegação |
| `/search` | Motor de pesquisa com AI Summary |
| `/index` | Submeter URLs para indexação |
| `/links` | Encontrar páginas que apontam para uma URL |
| `/stats` | Estatísticas do sistema em tempo real |
| `/apis` | Menu de APIs externas |
| `/apis/hackernews` | Pesquisa de notícias do Hacker News |
| `/apis/ai` | Chat com IA (Gemini) |

### Características da Interface:
- **Design Dark Theme** moderno
- **Real-time updates** (WebSocket para estatísticas)
- **AI-powered** (resumos automáticos de pesquisas)
- **Paginação** intuitiva
- **Feedback visual** de estado das operações

---

## 🔗 APIs Externas

### 1. **Hacker News Search** (`/apis/hackernews`)
- Integração com **Algolia HN Search API**
- Pesquisa de notícias e artigos
- Paginação de resultados
- Links diretos para artigos

**Endpoint REST:**
```
GET /api/hackernews/search?q={query}&p={page}
```

### 2. **AI Chat** (`/apis/ai`)
- Integração com **Google Gemini 2.5 Flash**
- Chat conversacional com IA
- Geração de texto assistida
- Interface de chat moderna

**Endpoint REST:**
```
POST /apis/ai/generate
Body: { "prompt": "sua pergunta" }
```

---

## 📁 Estrutura do Projeto

```
src/
├── main/
│   ├── java/pt/dei/googol/Projeto_SD/
│   │   ├── Common/
│   │   │   ├── DataStructures/      # Records e classes de dados
│   │   │   └── Functions/            # Utilitários (RMI, URLCleaner)
│   │   ├── Servers/
│   │   │   ├── BarrelServer/
│   │   │   │   ├── Components/       # Barrel, BarrelServer
│   │   │   │   ├── DataStructures/   # PageHeader
│   │   │   │   └── Interfaces/       # RMI Interfaces
│   │   │   ├── CrawlerServer/
│   │   │   │   ├── Components/       # Crawler, URLQueue
│   │   │   │   ├── DataStructures/   # BarrelInfo
│   │   │   │   └── Interfaces/       # RMI Interfaces
│   │   │   ├── GatewayServer/
│   │   │   │   ├── Components/       # GatewayServer
│   │   │   │   ├── DataStructures/   # BarrelInfo
│   │   │   │   └── Interfaces/       # RMI Interfaces
│   │   │   └── WebServer/
│   │   │       ├── Components/       # WebServer
│   │   │       ├── Configuration/    # RMI Config
│   │   │       ├── Controllers/      # REST + Thymeleaf
│   │   │       ├── Services/         # Lógica de negócio
│   │   │       ├── Interfaces/       # RMI Callback
│   │   │       └── WebSockets/       # WebSocket handlers
│   │   └── ProjetoSdApplication.java
│   └── resources/
│       ├── static/
│       │   ├── js/                   # JavaScript frontend
│       │   └── config.json           # Config do cliente
│       ├── templates/                # Thymeleaf HTML templates
│       ├── files/
│       │   ├── SystemConfiguration   # Config do sistema
│       │   └── URLs                  # URLs iniciais
│       └── application.properties
└── test/
```

---

## 🛠 Comandos de Consola

### Gateway Server
- **Enter**: Desliga todo o backend (cascata)

### Crawler Server
- **Enter**: Desliga o Crawler Server

### Barrel Server
- **Enter**: Desliga o Barrel Server

### Web Server
- **Ctrl+C** ou fechar terminal: Desliga o Web Server

---

## 📊 Monitorização e Debug

### Logs do Sistema

Cada componente imprime logs no terminal:

```
[Gateway Server] Sistema iniciado...
[Crawler Server] Crawler 0 thread started.
[Barrel Server] Barrel 'Barrel 1' registered to Gateway Server.
[Web Server] Error: Gateway unavailable.
```

### Estatísticas em Tempo Real

Aceda a `/stats` para visualizar:
- Estado dos componentes
- Desempenho dos barrels
- Top pesquisas
- Métricas detalhadas

---

## 🔒 Considerações de Segurança

- **RMI sem autenticação**: Adequado apenas para ambientes de desenvolvimento/teste
- **CORS**: Configurar adequadamente para produção
- **Rate Limiting**: Implementar controlo de taxa de pedidos em produção
- **Input Validation**: URLs são validadas antes de processamento

---

## 📝 Notas de Desenvolvimento

### Performance:
- Sistema suporta **N barrels** em paralelo
- Crawlers configuram-se por `numberOfCrawlers`
- Load balancing automático baseado em latência
- Cache de conexões RMI

### Escalabilidade:
- Adicionar mais barrels: aumentar `numberOfBarrels`
- Adicionar mais crawlers: aumentar `numberOfCrawlers`
- Distribuir em múltiplas máquinas: alterar hosts no config

### Backup e Redundância:
- Barrels fazem backup uns dos outros ao iniciar
- Gateway mantém lista de barrels disponíveis
- Sistema continua operacional mesmo com barrels offline

---

## 👥 Autoria

Miguel Figueira Santos Braga - 2021221519

---

## 📄 Licença

Projeto desenvolvido no âmbito da disciplina de **Sistemas Distribuídos** 2025/2026 para uso académico.