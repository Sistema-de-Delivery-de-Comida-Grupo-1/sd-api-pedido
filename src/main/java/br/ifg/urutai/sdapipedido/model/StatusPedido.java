package br.ifg.urutai.sdapipedido.model;

public enum StatusPedido {
    AGUARDANDO_PAGAMENTO,

    PAGAMENTO_APROVADO,

    PAGAMENTO_RECUSADO,

    EM_PREPARO,

    PRONTO_PARA_ENTREGA,

    SAIU_PARA_ENTREGA,

    ENTREGUE,

    CANCELADO
}
