package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Cart;
import com.ecommerce.project.model.CartItems;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.CartDto;
import com.ecommerce.project.payload.ProductDto;
import com.ecommerce.project.repositories.CartItemRepository;
import com.ecommerce.project.repositories.CartRepository;
import com.ecommerce.project.repositories.ProductRepository;
import com.ecommerce.project.utils.AuthUtil;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    ProductRepository productRepository;

    @Autowired
    CartRepository cartRepository;

    @Autowired
    AuthUtil authUtil;

    @Autowired
    CartItemRepository cartItemRepository;

    @Autowired
    ModelMapper modelMapper;


    @Override
    public CartDto addProductToCart(Long productId, Integer quantity) {
        Cart cart = createCart();

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItems cartItems = cartItemRepository.findCartItemByProductIdAndCartId(cart.getCartId(), productId);
        if (cartItems != null) {
            throw new APIException("Product " + product.getProductName() + " already exists");
        }
        if (product.getQuantity() == 0) {
            throw new APIException(product.getProductName() + " is not available");
        }
        if (product.getQuantity() < quantity) {
            throw new APIException("Please, make an order of the " + product.getProductName() +
                    "less than or equal to the quantity" + product.getQuantity());
        }

        CartItems newCartItems = new CartItems();
        newCartItems.setCart(cart);
        newCartItems.setProducts(product);
        newCartItems.setQuantity(quantity);
        newCartItems.setDiscount(product.getDiscount());
        newCartItems.setProductPrice(product.getSpecialPrice());

        cartItemRepository.save(newCartItems);
        /*need to update later for remaining quantities left*/
        product.setQuantity(product.getQuantity());

        Double price = product.getSpecialPrice() != 0.0 ? product.getSpecialPrice() : product.getPrice();
        cart.setTotalPrice(cart.getTotalPrice() + (price * quantity));

        //cart.setTotalPrice(cart.getTotalPrice() + (product.getSpecialPrice() * quantity));
        cartRepository.save(cart);
        CartDto cartDto = modelMapper.map(cart, CartDto.class);

        // List<CartItems> cartItemsList = cart.getCartItems();
        List<CartItems> cartItemsList = cartItemRepository.findAllByCartId(cart.getCartId());
        Stream<ProductDto> productDtoStream = cartItemsList.stream().map(item -> {
            ProductDto map = modelMapper.map(item.getProducts(), ProductDto.class);
            map.setQuantity(item.getQuantity());
            return map;
        });
        cartDto.setProducts(productDtoStream.toList());
        return cartDto;
    }

    @Override
    public List<CartDto> getAllCarts() {
        List<Cart> carts = cartRepository.findAll();
        if (carts.isEmpty()) {
            throw new APIException("No cart exists");
        }
        List<CartDto> cartDtos = carts.stream()
                .map(cart -> {
                    CartDto cartDto = modelMapper.map(cart, CartDto.class);
                    List<ProductDto> productDtos = cart.getCartItems().stream()
                            .map(product -> modelMapper.map(product.getProducts(), ProductDto.class))
                            .toList();
                    cartDto.setProducts(productDtos);
                    return cartDto;
                }).toList();
        return cartDtos;
    }

    @Override
    public CartDto getCart(String emailId, Long cartId) {
        Cart cart = cartRepository.findCartByEmailAndCartId(emailId, cartId);
        if (cart == null) {
            throw new ResourceNotFoundException("Cart", "cartId", cartId);
        }
        CartDto cartDto = modelMapper.map(cart, CartDto.class);
        cart.getCartItems().forEach(c -> c.getProducts().setQuantity(c.getQuantity()));
        List<ProductDto> products = cart.getCartItems().stream()
                .map(product -> modelMapper.map(product.getProducts(), ProductDto.class))
                .toList();
        cartDto.setProducts(products);
        cartDto.setTotalPrice(cart.getTotalPrice());
        return cartDto;
    }

    @Transactional
    @Override
    public CartDto updateProductQuantityInCart(Long productId, Integer quantity) {
        String emailId = authUtil.loggedInEmail();
        Cart cart = cartRepository.findCartByEmail(emailId);
        Long cartId = cart.getCartId();

        Cart cartDb = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        if (product.getQuantity() == 0) {
            throw new APIException(product.getProductName() + " is not available");
        }
        if (product.getQuantity() < quantity) {
            throw new APIException("Please, make an order of the " + product.getProductName() +
                    "less than or equal to the quantity" + product.getQuantity());
        }

        CartItems cartItems = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);
        if (cartItems == null) {
            throw new APIException("Product " + product.getProductName() + " not available in the cart");
        }

        /*calculate new quantity*/

        int newQuantity = cartItems.getQuantity()+quantity;
        if (newQuantity<0){
            throw new APIException("Resulting quantity can not be negative");
        }
        if (newQuantity==0){
            deleteProductFromCart(cartId,productId);
        }else {
            cartItems.setProductPrice(product.getSpecialPrice());
            cartItems.setQuantity(cartItems.getQuantity() + quantity);
            cartItems.setDiscount(product.getDiscount());
            cartDb.setTotalPrice(cart.getTotalPrice() + (cartItems.getProductPrice() * quantity));
            cartRepository.save(cartDb);
        }
        CartItems updatedItem = cartItemRepository.save(cartItems);
        if (updatedItem.getQuantity() == 0) {
            cartItemRepository.deleteById(updatedItem.getCartItemId());
        }
        CartDto cartDto = modelMapper.map(cartDb, CartDto.class);
        List<CartItems> cartItemsList = cartDb.getCartItems();
        Stream<ProductDto> productDtos =cartItemsList
                .stream()
                .map(p->{
                    ProductDto productDto = modelMapper.map(p.getProducts(), ProductDto.class);
                    productDto.setQuantity(p.getQuantity());
                    return productDto;
                });
        cartDto.setProducts(productDtos.toList());
        return cartDto;
    }

    @Transactional
    @Override
    public String deleteProductFromCart(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        CartItems cartItems = cartItemRepository.findCartItemByProductIdAndCartId(cartId, productId);
        if (cartItems == null) {
            throw new APIException("Product " + "productId"+ productId);
        }
cart.setTotalPrice(cart.getTotalPrice()-(cartItems.getProductPrice()*cartItems.getQuantity()));
        cartItemRepository.deleteCartItemByProductIdAndCartId(cartId,productId);
        return "Product "+cartItems.getProducts()+" removed ";
    }

    @Override
    public void updateProductsInCarts(Long cartId, Long productId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart", "cartId", cartId));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "productId", productId));

        CartItems cartItems = cartItemRepository.findCartItemByProductIdAndCartId(cartId,productId);

        if (cartItems==null){
            throw new APIException("Product "+product.getProductName()+" not available in the cart");
        }

        double cartPrice = cart.getTotalPrice() - (cartItems.getProductPrice()*cartItems.getQuantity());
        cartItems.setProductPrice(product.getSpecialPrice());
        cart.setTotalPrice(cartPrice + (cartItems.getProductPrice()*cartItems.getQuantity()));
        cartItems =cartItemRepository.save(cartItems);
    }

    private Cart createCart() {
        Cart userCart = cartRepository.findCartByEmail(authUtil.loggedInEmail());
        if (userCart != null) {
            return userCart;
        }
        Cart cart = new Cart();
        cart.setTotalPrice(0.0);
        cart.setUser(authUtil.loggedInUser());
        return cartRepository.save(cart);
    }
}
