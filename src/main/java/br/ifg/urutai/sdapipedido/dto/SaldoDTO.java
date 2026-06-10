package br.ifg.urutai.sdapipedido.dto;

public class SaldoDTO {
    private double saldoAtual;
    private double totalRecebido;
    private double totalEstornado;
    private long totalTransacoes;
    private long totalEstornos;

    public SaldoDTO() {
    }

    public double getSaldoAtual() {
        return saldoAtual;
    }

    public void setSaldoAtual(double saldoAtual) {
        this.saldoAtual = saldoAtual;
    }

    public double getTotalRecebido() {
        return totalRecebido;
    }

    public void setTotalRecebido(double totalRecebido) {
        this.totalRecebido = totalRecebido;
    }

    public double getTotalEstornado() {
        return totalEstornado;
    }

    public void setTotalEstornado(double totalEstornado) {
        this.totalEstornado = totalEstornado;
    }

    public long getTotalTransacoes() {
        return totalTransacoes;
    }

    public void setTotalTransacoes(long totalTransacoes) {
        this.totalTransacoes = totalTransacoes;
    }

    public long getTotalEstornos() {
        return totalEstornos;
    }

    public void setTotalEstornos(long totalEstornos) {
        this.totalEstornos = totalEstornos;
    }
}
