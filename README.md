# SD-API-PEDIDO

## Descrição

O **SD-API-PEDIDO** é um microsserviço desenvolvido em Spring Boot responsável pelo gerenciamento dos pedidos de um sistema de delivery.

O serviço realiza:

* Cadastro e gerenciamento de pedidos;
* Controle do ciclo de vida dos pedidos;
* Integração com o microsserviço de usuários;
* Integração com o microsserviço de pagamento via gRPC;
* Comunicação assíncrona via RabbitMQ;
* Publicação de eventos utilizando Publish/Subscribe;
* Descoberta de serviços utilizando Eureka Service Discovery.

---

# Arquitetura

O sistema segue uma arquitetura baseada em microsserviços.

```text
┌─────────────────────┐
│ Microsserviço       │
│ Usuários            │
└─────────┬───────────┘
          │ REST
          ▼
┌─────────────────────┐
│ SD-API-PEDIDO       │
└──────┬───────┬──────┘
       │       │
       │ gRPC  │ RabbitMQ
       ▼       ▼
┌──────────┐ ┌─────────────┐
│ Pagamento│ │ Notificação │
└──────────┘ └─────────────┘

           ▲
           │
       Eureka
(Service Discovery)
```

---

# Tecnologias Utilizadas

* Java 21
* Spring Boot
* Spring Data JPA
* H2 Database
* RabbitMQ
* Spring Cloud Stream
* gRPC
* Eureka Server
* Spring Cloud Discovery Client

---

# Service Discovery (Serviço de Nomes)

O sistema utiliza o **Eureka Server** para descoberta dinâmica dos microsserviços.

Configuração:

```properties
spring.application.name=sd-api-pedido

eureka.client.service-url.defaultZone=http://localhost:8761/eureka/
```

Quando o microsserviço inicia, ele registra sua instância no Eureka.

Exemplo de descoberta do serviço de usuários:

```java
List<ServiceInstance> instances =
    discoveryClient.getInstances("MICROSSERVICO-USUARIOS");
```

Exemplo de descoberta do serviço de pagamento:

```java
ServiceInstance instance =
    discoveryClient.getInstances("SD-API-PAGAMENTO").get(0);
```

### Benefícios

* Elimina endereços fixos.
* Permite escalabilidade horizontal.
* Balanceamento de carga.
* Tolerância a falhas.

---

# Invocação Remota (RPC)

O sistema utiliza **Remote Procedure Call (RPC)** através do protocolo **gRPC**.

O gRPC permite que um serviço execute operações em outro serviço como se estivesse chamando métodos locais.

Neste projeto o microsserviço de pedidos atua como cliente do microsserviço de pagamento.

Fluxo:

```text
Pedido Service
      │
      │ gRPC
      ▼
Pagamento Service
```

---

# Comunicação Síncrona com gRPC

A comunicação síncrona ocorre quando o pedido precisa aguardar a resposta do serviço de pagamento.

## Operações gRPC Utilizadas

### Processar Pagamento

```proto
rpc ProcessarPagamento(RequisicaoPagamento)
returns (RespostaPagamento);
```

Utilizada quando um pedido é pago.

### Estornar Pagamento

```proto
rpc EstornarPagamento(RequisicaoEstorno)
returns (RespostaEstorno);
```

Utilizada para cancelamento de pagamentos.

### Consultar Saldo

```proto
rpc ConsultarSaldo(RequisicaoSaldo)
returns (RespostaSaldo);
```

Consulta o saldo financeiro do sistema.

### Listar Transações

```proto
rpc ListarTransacoes(RequisicaoListaTransacoes)
returns (RespostaListaTransacoes);
```

Retorna o histórico de transações.

---

# Mensageria e Eventos

O sistema utiliza RabbitMQ para comunicação assíncrona entre microsserviços.

Foram utilizados dois padrões:

## 1. Filas (Queue)

Utilizadas para processamento assíncrono.

Fila configurada:

```properties
app.queue-name=queue.pedido-entrega
```

Criação da fila:

```java
@Bean
public Queue minhaFila() {
    return new Queue(queueName, true);
}
```

Envio:

```java
rabbitTemplate.convertAndSend(
    queueName,
    pedidoSalvo
);
```

### Caso de uso

Quando um pedido é finalizado:

```text
Pedido
  ↓
PRONTO_PARA_ENTREGA
  ↓
RabbitMQ Queue
  ↓
Serviço de Entrega
```

O processamento ocorre sem bloquear o usuário.

---

## 2. Publish/Subscribe (Eventos)

O sistema também publica eventos utilizando Spring Cloud Stream.

Binding:

```properties
spring.cloud.stream.bindings.sd-api-pedido.destination=event-notificacao
```

Publicação:

```java
streamBridge.send(
    "sd-api-pedido",
    pedidoSalvo
);
```

### Eventos gerados

* Pedido criado
* Pedido atualizado
* Pagamento aprovado
* Pagamento recusado
* Pedido em preparo
* Pedido pronto para entrega
* Pedido cancelado

### Fluxo

```text
SD-API-PEDIDO
      │
      ▼
 Evento RabbitMQ
      │
 ┌────┼────┐
 ▼    ▼    ▼
Email SMS Push
```

Vários consumidores podem receber o mesmo evento simultaneamente.

---

# Modelos (Entities)

## Pedido

Representa um pedido realizado por um cliente.

| Campo      | Tipo             |
| ---------- | ---------------- |
| id         | Long             |
| idCliente  | Long             |
| valorTotal | double           |
| status     | StatusPedido     |
| itens      | List<ItemPedido> |

Relacionamento:

```java
@OneToMany(
 mappedBy="pedido",
 cascade=CascadeType.ALL,
 orphanRemoval=true
)
```

---

## ItemPedido

Representa um item pertencente a um pedido.

| Campo      | Tipo   |
| ---------- | ------ |
| id         | Long   |
| quantidade | int    |
| preco      | double |
| descricao  | String |

Relacionamento:

```java
@ManyToOne
@JoinColumn(name = "pedido_id")
```

---

# Status dos Pedidos

```java
AGUARDANDO_PAGAMENTO
PAGAMENTO_APROVADO
PAGAMENTO_RECUSADO
EM_PREPARO
PRONTO_PARA_ENTREGA
SAIU_PARA_ENTREGA
ENTREGUE
CANCELADO
```

---

# DTOs

## PedidoCreateDTO

Utilizado para criação de pedidos.

```json
{
  "idCliente": 1,
  "itens": []
}
```

---

## PedidoResponseDTO

Retornado pelas operações da API.

Campos:

* id
* idCliente
* valorTotal
* itens
* status

---

## ItemPedidoCreateDTO

Representa um item enviado na criação do pedido.

Campos:

* quantidade
* preco
* descricao

---

## ItemPedidoResumeDTO

Resumo dos itens retornados pela API.

Campos:

* id
* quantidade
* preco
* descricao

---

## UsuarioResumeDTO

Resumo do cliente retornado pelo microsserviço de usuários.

Campos:

* id
* nome

---

## SaldoDTO

Resumo financeiro retornado pelo serviço de pagamento.

Campos:

* saldoAtual
* totalRecebido
* totalEstornado
* totalTransacoes
* totalEstornos

---

## TransacaoDTO

Representa uma transação financeira.

Campos:

* idTransacao
* idUsuario
* idPedido
* valor
* formaPagamento
* status
* criadoEm
* estornadoEm
* motivoEstorno

---

## ListaTransacoesDTO

Lista paginada de transações.

Campos:

* transacoes
* totalElementos

---

# Repositórios

## PedidoRepository

```java
public interface PedidoRepository
extends JpaRepository<Pedido, Long>
```

Responsável pela persistência dos pedidos.

---

## ItemPedidoRepository

```java
public interface ItemPedidoRepository
extends JpaRepository<ItemPedido, Long>
```

Responsável pela persistência dos itens.

---

# Endpoints

## Criar Pedido

```http
POST /pedidos
```

### Body

```json
{
  "idCliente": 1,
  "itens": [
    {
      "quantidade": 2,
      "preco": 20.0,
      "descricao": "Pizza"
    }
  ]
}
```

---

## Atualizar Pedido

```http
PUT /pedidos/{id}
```

---

## Buscar Pedido

```http
GET /pedidos/{id}
```

---

## Listar Pedidos

```http
GET /pedidos
```

---

## Remover Pedido

```http
DELETE /pedidos/{id}
```

---

## Pagar Pedido

```http
PUT /pedidos/{id}/pagar
```

Executa chamada gRPC para o microsserviço de pagamento.

---

## Estornar Pedido

```http
PUT /pedidos/{id}/estornar?motivo=Motivo
```

Executa chamada gRPC de estorno.

---

## Iniciar Preparo

```http
PUT /pedidos/{id}/iniciar-preparo
```

---

## Finalizar Preparo

```http
PUT /pedidos/{id}/finalizar-preparo
```

Envia mensagem para RabbitMQ e publica evento.

---

## Consultar Saldo

```http
GET /pedidos/pagamentos/saldo
```

Consulta o saldo do serviço de pagamento via gRPC.

---

## Listar Transações

```http
GET /pedidos/pagamentos/transacoes
```

Parâmetros:

```text
pagina
tamanho
```

---

# Banco de Dados

Banco utilizado:

```properties
spring.datasource.url=jdbc:h2:mem:pedidodb
```

Console:

```text
http://localhost:8084/h2-console
```

---

# Conclusão

O SD-API-PEDIDO implementa os principais conceitos de Sistemas Distribuídos:

✅ Comunicação síncrona utilizando gRPC (RPC)

✅ Comunicação assíncrona utilizando RabbitMQ

✅ Processamento por filas

✅ Arquitetura Publish/Subscribe baseada em eventos

✅ Service Discovery utilizando Eureka

✅ Integração entre múltiplos microsserviços

✅ Persistência de dados utilizando Spring Data JPA
