package com.ecommerce.project.controller;

import com.ecommerce.project.model.Category;
import com.ecommerce.project.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/public/categories")
    public ResponseEntity<List<Category>> getAllCategories(){
        List<Category> allCategories = categoryService.getAllCategories();
        return new ResponseEntity<>(allCategories,HttpStatus.OK);
    }

    @PostMapping("/public/categories")
    public ResponseEntity<String> createCategory(@RequestBody Category category){
        categoryService.createCategory(category);
        String response = "Category added successfully";
        return new ResponseEntity<>(response,HttpStatus.CREATED);
    }

    @DeleteMapping("/admin/categories/{categoryId}")
    public ResponseEntity<String> deleteCategory(@PathVariable Long categoryId){
        try {
            String status = categoryService.deleteCategory(categoryId);
            return new ResponseEntity<>(status,HttpStatus.OK);
        }catch (ResponseStatusException e){
            return new ResponseEntity<>(e.getReason(),e.getStatusCode());
        }
    }

    @PutMapping("/public/categories/{categoryId}")
    public ResponseEntity<String>updateCategory(@RequestBody Category category,
                                                @PathVariable Long categoryId){
        try{
             categoryService.updateCategory(category,categoryId);
            return new ResponseEntity<>("Updated Category with category id "+categoryId,HttpStatus.CREATED);
        }catch (ResponseStatusException e){
            return new ResponseEntity<>(e.getReason(),e.getStatusCode());
        }
    }
}
