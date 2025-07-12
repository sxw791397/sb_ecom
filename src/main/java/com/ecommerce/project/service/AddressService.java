package com.ecommerce.project.service;

import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.AddressDto;
import jakarta.validation.Valid;

import java.util.List;

public interface AddressService {
    AddressDto createAddress(AddressDto addressDto, User user);

    List<AddressDto> getAddresses();

    AddressDto getAddressesById(Long addressId);

    List<AddressDto> getAddressesByUser(User user);

    AddressDto updateAddressesById(Long addressId, @Valid AddressDto addressDto);

    String deleteAddressesById(Long addressId);
}
