package br.ifg.urutai.sdapipedido.dto;

import java.io.Serializable;
import java.util.List;

public class PedidoCreteDTO implements Serializable {
    private Long idCliente;
    private List<ItemPedidoCreateDTO> itens;

    public PedidoCreteDTO() {
    }

    public Long getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Long idCliente) {
        this.idCliente = idCliente;
    }

    public List<ItemPedidoCreateDTO> getItens() {
        return itens;
    }

    public void setItens(List<ItemPedidoCreateDTO> itens) {
        this.itens = itens;
    }
}
