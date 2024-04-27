package my.home.customerapp.client;

import my.home.customerapp.entity.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface ProductsClient {

    Flux<Product> findAllProducts(String filter);

    Mono<Product> findProducts(int id);
}
