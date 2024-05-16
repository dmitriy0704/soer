package my.home.customerapp.controller;

import lombok.RequiredArgsConstructor;
import my.home.customerapp.client.FavouriteProductsClient;
import my.home.customerapp.client.ProductReviewsClient;
import my.home.customerapp.client.ProductsClient;
import my.home.customerapp.client.payload.NewProductReviewPayload;
import my.home.customerapp.entity.Product;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.NoSuchElementException;

@Controller
@RequiredArgsConstructor
@RequestMapping("customer/products/{productId:\\d+}")
public class ProductController {

    private final ProductsClient productsClient;
    private final FavouriteProductsClient favouriteProductsClient;
    private final ProductReviewsClient productReviewsClient;

    @ModelAttribute(name = "product", binding = false)
    public Mono<Product> loadProduct(@PathVariable("productId") int id) {
        return this.productsClient.findProducts(id)
                .switchIfEmpty(Mono.error(new NoSuchElementException("customer.products.error.not_found")));
    }

    @GetMapping
    public Mono<String> getProductPage(@PathVariable("productId") int id, Model model) {
        model.addAttribute("inFavourite", false);
        return this.productReviewsClient.findProductReviewsByProductId(id)
                .collectList()
                .doOnNext(productReviews -> model.addAttribute("reviews", productReviews))
                .then(this.favouriteProductsClient.findFavouriteProductByProductId(id)
                        .doOnNext(favouriteProduct -> model.addAttribute("inFavourite", true)))
                .thenReturn("customer/products/product");
    }

    @PostMapping("add-to-favourites")
    public Mono<String> addProductToFavourites(@ModelAttribute("product") Mono<Product> productMono) {
        return productMono
                .map(Product::id)
                .flatMap(productId ->
                        this.favouriteProductsClient.addProductToFavourite(productId)
                                .thenReturn("redirect:/customer/products/%d".formatted(productId)));
    }

    @PostMapping("remove-from-favourites")
    public Mono<String> removeProductFromFavourites(@ModelAttribute("product") Mono<Product> productMono) {
        return productMono
                .map(Product::id)
                .flatMap(productId ->
                        this.favouriteProductsClient.removeProductFromFavourite(productId)
                                .thenReturn("redirect:/customer/products/%d".formatted(productId)));
    }

    @PostMapping("create-review")
    public Mono<String> createReview(
            @PathVariable("productId") int id,
            NewProductReviewPayload payload,
            BindingResult bindingResult,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("inFavourite", false);
            model.addAttribute("payload", payload);
            model.addAttribute("errors", bindingResult.getAllErrors()
                    .stream().map(ObjectError::getDefaultMessage)
                    .toList()
            );
            return this.favouriteProductsClient.findFavouriteProductByProduct(id)
                    .doOnNext(favouriteProduct -> model.addAttribute("inFavourite", true))
                    .thenReturn("customer/products/product");
        } else {
            return this.productReviewsClient.createProductReview(id, payload.rating(), payload.review())
                    .thenReturn("redirect:/customer/products/%d".formatted(id));
        }
    }

    @ExceptionHandler(NoSuchElementException.class)
public String handleNoSuchElementException(NoSuchElementException ex, Model model) {
        model.addAttribute("error", ex.getMessage());
        return "errors/404";
    }

}
