package br.ifg.urutai.sdapipedido.util;

import org.modelmapper.ModelMapper;
import java.util.ArrayList;
import java.util.List;

public class DataMapper {
    private static final ModelMapper mapper = new ModelMapper();

    public static <T> T parseObject(Object origem, Class<T> destino) {
        return mapper.map(origem, destino);
    }

    public static <O, D> List<D> parseListObjects(List<O> origin, Class<D> destination) {
        List<D> destinationObjects = new ArrayList<D>();
        for (O o : origin) {
            destinationObjects.add(mapper.map(o, destination));
        }
        return destinationObjects;
    }
}
