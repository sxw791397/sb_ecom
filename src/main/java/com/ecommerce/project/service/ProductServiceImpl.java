package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.APIException;
import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Category;
import com.ecommerce.project.model.Product;
import com.ecommerce.project.payload.ProductDto;
import com.ecommerce.project.payload.ProductResponse;
import com.ecommerce.project.repositories.CategoryRepository;
import com.ecommerce.project.repositories.ProductRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService{
    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Autowired
    private FileService fileService;

    @Value("${project.image}")
    private String path ;

    @Override
    public ProductDto addProduct(Long categoryId, ProductDto productDto) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(()->new ResourceNotFoundException("Category", "categoryId", categoryId));

        boolean isProductNotPresent = true;
        List<Product> products = category.getProducts();

        for (Product value :products){
            if(value.getProductName().equals(productDto.getProductName())){
                isProductNotPresent=false;
                break;
            }
        }
        if (isProductNotPresent) {
            Product product = modelMapper.map(productDto, Product.class);
            product.setCategory(category);

            product.setImage("default.png");
            double specialPrice = product.getPrice() -
                    ((product.getDiscount() * 0.01) * product.getPrice());
            Product savedProduct = productRepository.save(product);
            product.setSpecialPrice(specialPrice);
            return modelMapper.map(savedProduct, ProductDto.class);
        }else {
            throw new APIException("Product already present");
        }
    }

    @Override
    public ProductResponse getAllProducts(Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

        Sort sortAndOrder = sortOrder.equalsIgnoreCase("asc")?
                Sort.by(sortBy).ascending()
                :Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber,pageSize,sortAndOrder);

        Page<Product> productsPage = productRepository.findAll(pageDetails);
        List<Product>products = productsPage.getContent();

        if (products.isEmpty()) {
            throw new APIException("No product created till now.");
        }
        List<ProductDto> productDto = products.stream()
                .map(product -> modelMapper.map(product, ProductDto.class))
                .toList();

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDto);
        productResponse.setPageNumber(productsPage.getNumber());
        productResponse.setPageSize(productsPage.getSize());
        productResponse.setTotalPages(productsPage.getTotalPages());
        productResponse.setTotalElements(productsPage.getTotalElements());
        productResponse.setLastPage(productsPage.isLast());

        return productResponse;
    }

    @Override
    public ProductResponse getAllProductsByCategory(Long categoryId, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "categoryId", categoryId));

        Sort sortAndOrder = sortOrder.equalsIgnoreCase("asc")?
                Sort.by(sortBy).ascending()
                :Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber,pageSize,sortAndOrder);

        Page<Product> productsPage = productRepository.findByCategoryOrderByPriceAsc(category,pageDetails);
        List<Product>products = productsPage.getContent();

        if (products.isEmpty()) {
            throw new APIException(category.getCategoryName() +"category does not have any products.");
        }
        List<ProductDto> productDto = products.stream()
                .map(product -> modelMapper.map(product, ProductDto.class))
                .toList();

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDto);
        productResponse.setPageNumber(productsPage.getNumber());
        productResponse.setPageSize(productsPage.getSize());
        productResponse.setTotalPages(productsPage.getTotalPages());
        productResponse.setTotalElements(productsPage.getTotalElements());
        productResponse.setLastPage(productsPage.isLast());

        return productResponse;
    }

    @Override
    public ProductResponse getProductsByKeyword(String keyword, Integer pageNumber, Integer pageSize, String sortBy, String sortOrder) {

        Sort sortAndOrder = sortOrder.equalsIgnoreCase("asc")?
                Sort.by(sortBy).ascending()
                :Sort.by(sortBy).descending();
        Pageable pageDetails = PageRequest.of(pageNumber,pageSize,sortAndOrder);
        Page<Product> productsPage =productRepository.findByProductNameLikeIgnoreCase("%"+keyword+"%",pageDetails);
        List<Product> products = productsPage.getContent();
        if (products.isEmpty()) {
            throw new APIException("No product found with keyword."+keyword);
        }
        List<ProductDto> productDto = products.stream()
                .map(product -> modelMapper.map(product, ProductDto.class))
                .toList();

        ProductResponse productResponse = new ProductResponse();
        productResponse.setContent(productDto);
        productResponse.setPageNumber(productsPage.getNumber());
        productResponse.setPageSize(productsPage.getSize());
        productResponse.setTotalPages(productsPage.getTotalPages());
        productResponse.setTotalElements(productsPage.getTotalElements());
        productResponse.setLastPage(productsPage.isLast());

        return productResponse;    }

    @Override
    public ProductDto updateProduct(ProductDto productDto, Long productId) {
        Product productFromDb = productRepository.findById(productId)
                .orElseThrow(()->new ResourceNotFoundException("Product","productId",productId));
        Product product = modelMapper.map(productDto,Product.class);
        productFromDb.setProductName(product.getProductName());
        productFromDb.setDescription(product.getDescription());
        productFromDb.setQuantity(product.getQuantity());
        productFromDb.setDiscount(product.getDiscount());
        productFromDb.setPrice(product.getPrice());
        productFromDb.setSpecialPrice(product.getSpecialPrice());
    Product savedProduct = productRepository.save(productFromDb);
    return modelMapper.map(savedProduct,ProductDto.class);
    }

    @Override
    public ProductDto deleteProduct(Long productId) {
        Product productFromDb = productRepository.findById(productId)
                .orElseThrow(()->new ResourceNotFoundException("Product","productId",productId));
        productRepository.delete(productFromDb);
        return modelMapper.map(productFromDb,ProductDto.class);
    }

    @Override
    public ProductDto updateProductImage(Long productId, MultipartFile image) throws IOException {
        /*get the product from the db*/
        Product productFromDb = productRepository.findById(productId)
                .orElseThrow(()->new ResourceNotFoundException("Product","productId",productId));

        /*upload the image to the server*/
        /*get the file name of the uploaded image*/
        String fileName = fileService.uploadImage(path,image);

        /*updating the new file name to the product*/
        productFromDb.setImage(fileName);

        /*save the updated product*/
        Product savedProduct = productRepository.save(productFromDb);

        return modelMapper.map(savedProduct,ProductDto.class);
    }


}
