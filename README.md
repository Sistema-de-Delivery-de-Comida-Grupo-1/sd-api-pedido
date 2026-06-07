# SD API PEDIDO

## Visão Geral

O **SD API Pedido** é um microsserviço responsável pelo gerenciamento do ciclo de vida dos pedidos em uma arquitetura distribuída de delivery.

O serviço é responsável por:

* Criar pedidos.
* Atualizar pedidos.
* Consultar pedidos.
* Remover pedidos.
* Solicitar processamento de pagamento via gRPC.
* Controlar o fluxo de estados do pedido.
* Publicar eventos para outros microsserviços através do RabbitMQ utilizando Spring Cloud Stream.
* Enviar pedidos prontos para entrega para uma fila RabbitMQ.
* Registrar-se automaticamente no servidor Eureka para descoberta de serviços.

---

# Arquitetura

O microsserviço segue a arquitetura em camadas:

```text
Controller
    ↓
Service
    ↓
Repository
    ↓
Banco de Dados (H2)

Service
    ↓
gRPC (API Pagamento)

Service
    ↓
RabbitMQ
```

---

# Configuração

## application.properties

```properties
spring.application.name=sd-api-pedido

server.port=8084

eureka.client.service-url.defaultZone=http://localhost:8761/eureka/

spring.datasource.url=jdbc:h2:mem:pedidodb
spring.datasource.driverClassName=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.h2.console.enabled=true
spring.h2.console.path=/h2-console

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

spring.rabbitmq.host=localhost
spring.rabbitmq.port=5672
spring.rabbitmq.username=guest
spring.rabbitmq.password=guest

app.queue-name=queue.pedido-entrega

spring.cloud.stream.default-binder=rabbit
spring.cloud.stream.bindings.sd-api-pedido.destination=event-notificacao
spring.cloud.stream.bindings.sd-api-pedido.content-type=application/json
```

---

# Banco de Dados

O projeto utiliza banco H2 em memória.

Console disponível em:

```text
http://localhost:8084/h2-console
```

---

# Modelo de Domínio

## Pedido

Representa um pedido realizado por um cliente.

### Atributos

| Campo      | Tipo             |
| ---------- | ---------------- |
| id         | Long             |
| idCliente  | Long             |
| valorTotal | double           |
| status     | StatusPedido     |
| itens      | List<ItemPedido> |

### Relacionamentos

```text
Pedido
   |
   | 1:N
   |
ItemPedido
```

---

## ItemPedido

Representa um item pertencente a um pedido.

### Atributos

| Campo      | Tipo   |
| ---------- | ------ |
| id         | Long   |
| quantidade | int    |
| preco      | double |
| descricao  | String |

---

# Estados do Pedido

O pedido segue o seguinte fluxo:

```text
AGUARDANDO_PAGAMENTO
          ↓
PAGAMENTO_APROVADO
          ↓
EM_PREPARO
          ↓
PRONTO_PARA_ENTREGA
```

Fluxos alternativos:

```text
AGUARDANDO_PAGAMENTO
          ↓
PAGAMENTO_RECUSADO
```

ou

```text
CANCELADO
```

Estados disponíveis:

* AGUARDANDO_PAGAMENTO
* PAGAMENTO_APROVADO
* PAGAMENTO_RECUSADO
* EM_PREPARO
* PRONTO_PARA_ENTREGA
* SAIU_PARA_ENTREGA
* ENTREGUE
* CANCELADO

---

# DTOs

DTOs são utilizados para separar a camada de API da camada de persistência.

---

## PedidoCreateDTO

Utilizado na criação e atualização de pedidos.

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

## PedidoResponseDTO

Retornado pelas operações da API.

```json
{
  "id": 1,
  "idCliente": 1,
  "valorTotal": 40.0,
  "status": "AGUARDANDO_PAGAMENTO",
  "itens": [
    {
      "id": 1,
      "quantidade": 2,
      "preco": 20.0,
      "descricao": "Pizza"
    }
  ]
}
```

---

# Repository

## PedidoRepository

Responsável pelo acesso aos dados da entidade Pedido.

```java
public interface PedidoRepository
        extends JpaRepository<Pedido, Long> {
}
```

---

## ItemPedidoRepository

Responsável pelo acesso aos dados da entidade ItemPedido.

```java
public interface ItemPedidoRepository
        extends JpaRepository<ItemPedido, Long> {
}
```

---

# Service

A camada Service contém toda a regra de negócio da aplicação.

## criarPedido()

Responsabilidades:

* Validar itens.
* Calcular valor total.
* Definir status inicial.
* Persistir pedido.
* Publicar evento RabbitMQ.

---

## atualizar()

Responsabilidades:

* Atualizar cliente.
* Atualizar itens.
* Recalcular valor total.
* Publicar evento.

---

## pagarPedido()

Responsabilidades:

* Validar pedido.
* Chamar API de pagamento via gRPC.
* Atualizar status.
* Publicar evento.

Fluxo:

```text
Pedido Service
       ↓
gRPC
       ↓
API Pagamento
       ↓
Resposta
       ↓
Atualização Status
```

---

## iniciarPreparo()

Permite que um pedido pago seja colocado em preparo.

Validação:

```text
PAGAMENTO_APROVADO
```

---

## finalizarPreparo()

Permite finalizar o preparo.

Validação:

```text
EM_PREPARO
```

Após finalizar:

* Atualiza status.
* Publica evento.
* Envia pedido para fila de entrega.

---

## buscarPorId()

Consulta pedido pelo identificador.

---

## buscarTodos()

Lista todos os pedidos.

---

## remover()

Remove um pedido existente.

---

# Comunicação gRPC

O microsserviço integra-se ao serviço de pagamento.

## Configuração

```java
ManagedChannelBuilder
    .forAddress("localhost", 8086)
```

### Requisição

```text
idUsuario
idPedido
valor
formaPagamento
```

### Resposta

```text
SUCESSO
ou
ERRO
```

---

# Comunicação RabbitMQ

O sistema utiliza duas formas de comunicação assíncrona.

---

## Spring Cloud Stream

Destino:

```text
event-notificacao
```

Todos os eventos do pedido são publicados neste tópico.

Eventos publicados:

* Pedido criado.
* Pedido atualizado.
* Pedido pago.
* Pedido em preparo.
* Pedido pronto para entrega.

---

## Fila de Entrega

Fila:

```text
queue.pedido-entrega
```

Quando um pedido é finalizado:

```text
Pedido
   ↓
RabbitMQ
   ↓
queue.pedido-entrega
   ↓
Microsserviço de Entrega
```

---

# Eureka

O serviço registra-se automaticamente no Eureka.

Nome registrado:

```text
SD-API-PEDIDO
```

Servidor Eureka:

```text
http://localhost:8761
```

---

# Endpoints

## Criar Pedido

```http
POST /pedidos
```

Exemplo:

```json
{
  "idCliente": 1,
  "itens": [
    {
      "quantidade": 2,
      "preco": 25.0,
      "descricao": "Hambúrguer"
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

## Efetuar Pagamento

```http
PUT /pedidos/{id}/pagar
```

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

# Fluxo Completo do Pedido

```text
Cliente
   ↓
POST /pedidos
   ↓
AGUARDANDO_PAGAMENTO
   ↓
PUT /pedidos/{id}/pagar
   ↓
PAGAMENTO_APROVADO
   ↓
PUT /pedidos/{id}/iniciar-preparo
   ↓
EM_PREPARO
   ↓
PUT /pedidos/{id}/finalizar-preparo
   ↓
PRONTO_PARA_ENTREGA
   ↓
RabbitMQ
   ↓
Microsserviço de Entrega
```

---

# Tecnologias Utilizadas

* Java 21
* Spring Boot
* Spring Data JPA
* Spring Cloud Stream
* RabbitMQ
* gRPC
* H2 Database
* Eureka Discovery Server
* Maven
