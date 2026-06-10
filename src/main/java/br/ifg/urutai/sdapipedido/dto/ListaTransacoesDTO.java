package br.ifg.urutai.sdapipedido.dto;

import java.util.List;

public class ListaTransacoesDTO {
    private List<TransacaoDTO> transacoes;
    private long totalElementos;


    public ListaTransacoesDTO(List<TransacaoDTO> transacoes, long totalElementos) {
        this.transacoes = transacoes;
        this.totalElementos = totalElementos;
    }

    public ListaTransacoesDTO() {
    }

    public List<TransacaoDTO> getTransacoes() {
        return transacoes;
    }

    public void setTransacoes(List<TransacaoDTO> transacoes) {
        this.transacoes = transacoes;
    }

    public long getTotalElementos() {
        return totalElementos;
    }

    public void setTotalElementos(long totalElementos) {
        this.totalElementos = totalElementos;
    }
}
