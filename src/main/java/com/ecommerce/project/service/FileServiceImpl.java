package com.ecommerce.project.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileServiceImpl implements FileService{

    @Override
    public String uploadImage(String path, MultipartFile file) throws IOException {
        /*file name of the current file or original file*/
        String originalFileName = file.getOriginalFilename();
        /*generate the unique file name*/
        String randomId = UUID.randomUUID().toString();
        String fileName = randomId.concat(originalFileName.substring(originalFileName.lastIndexOf('.')));
        String filePath = path + File.separator+fileName;
        /*check path if exists & create*/
        File folder = new File(path);
        if (!folder.exists()){
            folder.mkdir();
        }
        /*upload to the server*/
        Files.copy(file.getInputStream(), Paths.get(filePath));
        return fileName;
    }
}
