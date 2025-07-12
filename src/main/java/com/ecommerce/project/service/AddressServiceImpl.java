package com.ecommerce.project.service;

import com.ecommerce.project.exceptions.ResourceNotFoundException;
import com.ecommerce.project.model.Address;
import com.ecommerce.project.model.User;
import com.ecommerce.project.payload.AddressDto;
import com.ecommerce.project.repositories.AddressRepository;
import com.ecommerce.project.repositories.UserRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressServiceImpl implements AddressService{
    
    @Autowired
    ModelMapper modelMapper;

    @Autowired
    AddressRepository addressRepository;

    @Autowired
    UserRepository userRepository;

    @Override
    public AddressDto createAddress(AddressDto addressDto, User user) {
        Address address = modelMapper.map(addressDto, Address.class);
        List<Address> addressList = user.getAddresses();
        addressList.add(address);
        user.setAddresses(addressList);

        address.setUser(user);
        Address savedAddress = addressRepository.save(address);

        return modelMapper.map(savedAddress,AddressDto.class);
    }

    @Override
    public List<AddressDto> getAddresses() {
        List<Address> addresses = addressRepository.findAll();
        return addresses.stream()
                 .map(address -> modelMapper.map(address,AddressDto.class))
                 .toList();
    }

    @Override
    public AddressDto getAddressesById(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(()-> new ResourceNotFoundException("Address","addressId",addressId));

        return modelMapper.map(address,AddressDto.class);
    }

    @Override
    public List<AddressDto> getAddressesByUser(User user) {
        List<Address> addresses = user.getAddresses();
        return addresses.stream()
                .map(address -> modelMapper.map(address,AddressDto.class))
                .toList();    }

    @Override
    public AddressDto updateAddressesById(Long addressId, AddressDto addressDto) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(()-> new ResourceNotFoundException("Address","addressId",addressId));

        address.setCity(addressDto.getCity());
        address.setPincode(addressDto.getPincode());
        address.setState(addressDto.getState());
        address.setBuildingName(addressDto.getBuildingName());
        address.setCountry(addressDto.getCountry());
        address.setStreet(addressDto.getStreet());

        Address updatedAddress = addressRepository.save(address);

        User user = address.getUser();
        user.getAddresses().removeIf(userAddress->userAddress.getAddressId().equals(addressId));
        user.getAddresses().add(updatedAddress);
        userRepository.save(user);
        return modelMapper.map(updatedAddress,AddressDto.class);
    }

    @Override
    public String deleteAddressesById(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(()-> new ResourceNotFoundException("Address","addressId",addressId));

        User user = address.getUser();
        user.getAddresses().removeIf(userAddress->userAddress.getAddressId().equals(addressId));
        userRepository.save(user);
        addressRepository.deleteById(addressId);

        return "Address deleted successfully with addressId :"+addressId;
    }
}
