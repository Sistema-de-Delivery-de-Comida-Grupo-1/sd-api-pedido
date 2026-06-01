package br.ifg.urutai.sdapipedido.repository;

import br.ifg.urutai.sdapipedido.model.Pedido;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {
}
