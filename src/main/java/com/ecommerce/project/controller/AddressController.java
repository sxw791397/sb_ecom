package com.ecommerce.project.controller;

import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.AddressDto;
import com.ecommerce.project.service.AddressService;
import com.ecommerce.project.utils.AuthUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class AddressController {

    @Autowired
    AddressService addressService;

    @Autowired
    AuthUtil authUtil;

    @PostMapping("/addresses")
    public ResponseEntity<AddressDto> createAddress(@Valid @RequestBody AddressDto addressDto){
        User user = authUtil.loggedInUser();
        AddressDto savedAddress = addressService.createAddress(addressDto,user);
        return new ResponseEntity<>(savedAddress, HttpStatus.CREATED);
    }

    @GetMapping("/addresses")
    public ResponseEntity<List<AddressDto>> getAddresses(){
        List<AddressDto> addressDtos = addressService.getAddresses();
        return new ResponseEntity<>(addressDtos,HttpStatus.OK);
    }

    @GetMapping("/addresses/{addressId}")
    public ResponseEntity<AddressDto> getAddressesById(@PathVariable Long addressId){
        AddressDto addressDtos = addressService.getAddressesById(addressId);
        return new ResponseEntity<>(addressDtos,HttpStatus.OK);
    }

    @GetMapping("/users/addresses")
    public ResponseEntity<List<AddressDto>> getAddressesByUser(){
        User user = authUtil.loggedInUser();
        List<AddressDto> addressDtos = addressService.getAddressesByUser(user);
        return new ResponseEntity<>(addressDtos,HttpStatus.OK);
    }

    @PutMapping("/addresses/{addressId}")
    public ResponseEntity<AddressDto> updateAddressesById(@PathVariable Long addressId,
                                                          @Valid @RequestBody AddressDto addressDto){
        AddressDto addressDtos = addressService.updateAddressesById(addressId,addressDto);
        return new ResponseEntity<>(addressDtos,HttpStatus.OK);
    }

    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<String> deleteAddressesById(@PathVariable Long addressId){
        String status = addressService.deleteAddressesById(addressId);
        return new ResponseEntity<>(status,HttpStatus.OK);
    }
}
