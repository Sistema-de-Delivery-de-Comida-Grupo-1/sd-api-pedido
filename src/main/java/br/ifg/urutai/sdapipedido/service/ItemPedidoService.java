package br.ifg.urutai.sdapipedido.service;

import br.ifg.urutai.sdapipedido.model.ItemPedido;
import br.ifg.urutai.sdapipedido.repository.ItemPedidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ItemPedidoService {
    private final ItemPedidoRepository itemPedidoRepository;

    @Autowired
    public ItemPedidoService(ItemPedidoRepository itemPedidoRepository) {
        this.itemPedidoRepository = itemPedidoRepository;
    }

    public ItemPedido salvar(ItemPedido itemPedido) {
        return itemPedidoRepository.save(itemPedido);
    }

    public ItemPedido atualizar(Long id,ItemPedido itemPedido) {
        ItemPedido itemPedidoNovo = itemPedidoRepository.findById(id).orElseThrow(() -> new RuntimeException("Item não encontrado"));

        if (itemPedido.getDescricao() != null && !itemPedido.getDescricao().equals(itemPedidoNovo.getDescricao())) {
            itemPedidoNovo.setDescricao(itemPedido.getDescricao());
        }

        if (itemPedido.getQuantidade() != itemPedidoNovo.getQuantidade()) {
            itemPedidoNovo.setQuantidade(itemPedido.getQuantidade());
        }

        if (itemPedido.getPreco() != itemPedidoNovo.getPreco()) {
            itemPedidoNovo.setPreco(itemPedido.getPreco());
        }

        if (itemPedido.getPedido() != null && !itemPedido.getPedido().equals(itemPedidoNovo.getPedido())) {
            itemPedidoNovo.setPedido(itemPedido.getPedido());
        }

        return itemPedidoRepository.save(itemPedidoNovo);
    }

    public ItemPedido buscarPorId(Long id) {
        return itemPedidoRepository.findById(id).orElseThrow(()-> new RuntimeException("Item não encontrado"));
    }

    public List<ItemPedido> busarTodos(){
        return itemPedidoRepository.findAll();
    }

    public void remover(Long id) {
        itemPedidoRepository.findById(id).orElseThrow(() -> new RuntimeException("Item não encontrado"));
        itemPedidoRepository.deleteById(id);
    }

}
