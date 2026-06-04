# SD-API-PEDIDO

## Descrição

O **SD-API-PEDIDO** é um microsserviço responsável pelo gerenciamento de pedidos em um sistema de delivery distribuído.

Este serviço permite:

* Criar pedidos.
* Consultar pedidos.
* Atualizar pedidos.
* Remover pedidos.
* Processar pagamentos via gRPC.
* Controlar o fluxo de estados do pedido.
* Publicar pedidos prontos para entrega em uma fila RabbitMQ.
* Registrar-se no Eureka Server para descoberta de serviços.

---

# Arquitetura

O microsserviço foi desenvolvido utilizando:

* Java
* Spring Boot
* Spring Data JPA
* Banco de dados H2
* RabbitMQ
* gRPC
* Eureka Client

Fluxo simplificado:

```text
Cliente
   |
   v
API Pedido
   |
   +--> Banco H2
   |
   +--> Serviço de Pagamento (gRPC)
   |
   +--> RabbitMQ
   |
   +--> Eureka Server
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
```

---

# Modelo de Dados

## Pedido

```json
{
  "id": 1,
  "idCliente": 10,
  "valorTotal": 55.0,
  "status": "AGUARDANDO_PAGAMENTO",
  "itens": []
}
```

## ItemPedido

```json
{
  "id": 1,
  "quantidade": 2,
  "preco": 25.0,
  "descricao": "Pizza Calabresa"
}
```

---

# Estados do Pedido

O pedido pode assumir os seguintes estados:

| Status               |
| -------------------- |
| AGUARDANDO_PAGAMENTO |
| PAGAMENTO_APROVADO   |
| PAGAMENTO_RECUSADO   |
| EM_PREPARO           |
| PRONTO_PARA_ENTREGA  |
| SAIU_PARA_ENTREGA    |
| ENTREGUE             |
| CANCELADO            |

---

# Endpoints

## Criar Pedido

### POST /pedidos

### Requisição

```json
{
  "idCliente": 1,
  "itens": [
    {
      "quantidade": 2,
      "preco": 25.0,
      "descricao": "Pizza Calabresa"
    },
    {
      "quantidade": 1,
      "preco": 5.0,
      "descricao": "Refrigerante"
    }
  ]
}
```

### Resposta

```json
{
  "id": 1,
  "idCliente": 1,
  "valorTotal": 55.0,
  "status": "AGUARDANDO_PAGAMENTO",
  "itens": [
    {
      "id": 1,
      "quantidade": 2,
      "preco": 25.0,
      "descricao": "Pizza Calabresa"
    },
    {
      "id": 2,
      "quantidade": 1,
      "preco": 5.0,
      "descricao": "Refrigerante"
    }
  ]
}
```

---

## Buscar Pedido por ID

### GET /pedidos/{id}

### Exemplo

```http
GET /pedidos/1
```

---

## Listar Todos os Pedidos

### GET /pedidos

### Exemplo

```http
GET /pedidos
```

---

## Atualizar Pedido

### PUT /pedidos/{id}

### Requisição

```json
{
  "idCliente": 1,
  "itens": [
    {
      "quantidade": 3,
      "preco": 25.0,
      "descricao": "Pizza Calabresa"
    }
  ]
}
```

---

## Remover Pedido

### DELETE /pedidos/{id}

### Exemplo

```http
DELETE /pedidos/1
```

---

# Pagamento

## Processar Pagamento

### PUT /pedidos/{id}/pagar

Este endpoint envia uma requisição para o microsserviço de pagamento utilizando gRPC.

### Exemplo

```http
PUT /pedidos/1/pagar
```

### Possíveis Resultados

#### Pagamento aprovado

```json
{
  "status": "PAGAMENTO_APROVADO"
}
```

#### Pagamento recusado

```json
{
  "status": "PAGAMENTO_RECUSADO"
}
```

---

# Preparo do Pedido

## Iniciar Preparo

### PUT /pedidos/{id}/iniciar-preparo

Somente pedidos com status:

```text
PAGAMENTO_APROVADO
```

podem entrar em preparo.

### Exemplo

```http
PUT /pedidos/1/iniciar-preparo
```

---

## Finalizar Preparo

### PUT /pedidos/{id}/finalizar-preparo

Somente pedidos em preparo podem ser finalizados.

### Exemplo

```http
PUT /pedidos/1/finalizar-preparo
```

Ao finalizar o preparo:

1. O status é alterado para:

```text
PRONTO_PARA_ENTREGA
```

2. O pedido é publicado na fila RabbitMQ:

```text
queue.pedido-entrega
```

---

# Integração gRPC

O serviço utiliza um cliente gRPC para comunicação com o microsserviço de pagamento.

Método utilizado:

```text
processarPagamento()
```

Informações enviadas:

```json
{
  "idUsuario": "1",
  "idPedido": "10",
  "valor": 55.0,
  "formaPagamento": "PIX"
}
```

---

# Integração RabbitMQ

Fila utilizada:

```text
queue.pedido-entrega
```

Quando um pedido é finalizado, ele é enviado para esta fila para que o microsserviço de entrega possa consumi-lo.

---

# Banco de Dados

Banco utilizado:

```text
H2 Database
```

Console disponível em:

```text
http://localhost:8084/h2-console
```

Configurações:

```text
JDBC URL: jdbc:h2:mem:pedidodb
User: sa
Password:
```

---

# Registro no Eureka

Ao iniciar a aplicação, ela se registra automaticamente no Eureka Server.

Nome registrado:

```text
sd-api-pedido
```

Eureka Server:

```text
http://localhost:8761
```

---

# Fluxo Completo

```text
Criar Pedido
      |
      v
AGUARDANDO_PAGAMENTO
      |
      v
Pagar Pedido
      |
      +--> PAGAMENTO_APROVADO
      |
      +--> PAGAMENTO_RECUSADO
              |
              v
          Fim
      |
      v
Iniciar Preparo
      |
      v
EM_PREPARO
      |
      v
Finalizar Preparo
      |
      v
PRONTO_PARA_ENTREGA
      |
      v
RabbitMQ
      |
      v
Microsserviço de Entrega
```
