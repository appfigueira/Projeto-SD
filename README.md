# Googol Search Engine - README

## Requisitos
- Java JDK 21 ou superior
- Nenhuma biblioteca externa necessária

## Configuração
Verifique o ficheiro `files/SystemConfiguration` e ajuste os parâmetros 
conforme necessário tendo em conta o ambiente de teste.
Parâmetros do ficheiro de configuração descritos no relatório.

## Ordem de Inicialização
Os diversos servidores do sistema podem ser iniciados em qualquer ordem, 
no entanto, recomenda-se seguir a sequência:
- GatewayServer
- CrawlerServer
- BarrelServer
- Client

## Funcionalidades de Consola
- GatewayServer - *Enter* - Desliga todo o sistema
- CrawlerServer - *Enter* - Desliga o CrawlerServer
- BarrelServer - *Enter* - Desliga o BarrelServer
- Client - Instruções na Consola