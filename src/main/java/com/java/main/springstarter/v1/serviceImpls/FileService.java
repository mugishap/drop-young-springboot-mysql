package com.java.main.springstarter.v1.serviceImpls;

import com.java.main.springstarter.v1.enums.EFileSizeType;
import com.java.main.springstarter.v1.enums.EFileStatus;
import com.java.main.springstarter.v1.exceptions.ResourceNotFoundException;
import com.java.main.springstarter.v1.fileHandling.File;
import com.java.main.springstarter.v1.fileHandling.FileStorageService;
import com.java.main.springstarter.v1.repositories.IFileRepository;
import com.java.main.springstarter.v1.utils.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class FileServiceImpl {
    private final IFileRepository fileRepository;
    private final FileStorageService fileStorageService;

    @Value("${upload.extensions}")
    private String extensions;

    @Autowired
    public FileServiceImpl(IFileRepository fileRepository, FileStorageService fileStorageService) {
        this.fileRepository = fileRepository;
        this.fileStorageService = fileStorageService;
    }

    public List<File> getAll() {
        return this.fileRepository.findAll();
    }

    public Page<File> getAll(Pageable pageable) {
        return  this.fileRepository.findAll(pageable);
    }

    public File getById(UUID id) {
        return this.fileRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("File", "id", id.toString()));
    }

    public File create(MultipartFile document, String directory) {
        File file = new File();
        file.setStatus(EFileStatus.PENDING);


        String fileName = FileUtil.generateUUID(Objects.requireNonNull(document.getOriginalFilename()));
        String documentSizeType = FileUtil.getFileSizeTypeFromFileSize(file.getSize());
        int documentSize = FileUtil.getFormattedFileSizeFromFileSize(file.getSize(), EFileSizeType.valueOf(documentSizeType));

        file.setName(fileName);
        file.setPath(fileStorageService.save(document, directory, fileName));
        file.setStatus(EFileStatus.SAVED);
        file.setType(document.getContentType());
        file.setSize(documentSize);
        file.setSizeType(EFileSizeType.valueOf(documentSizeType));

        return this.fileRepository.save(file);
    }


    public boolean delete(UUID id) {
        boolean exists = this.fileRepository.existsById(id);
        if (!exists)
            throw new ResourceNotFoundException("File", "id", id.toString());
        this.fileRepository.deleteById(id);
        return true;
    }

    public Page<File> getAllByStatus(Pageable pageable, EFileStatus status) {
        return this.fileRepository.findAllByStatus(pageable, status);
    }

    public File uploadFile(MultipartFile file, String directory, UUID appointeeID) throws InvalidFileException, IOException{
        String fileName = handleFileName(Objects.requireNonNull(file.getOriginalFilename()), appointeeID);
        Path path = Paths.get(directory, fileName);
        System.out.println(path.toString());
        Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);

        String extension = getFileExtension(fileName);


        assert extension != null;
        String fileBaseName = fileName.substring(
                0,
                fileName.length()-extension.length()-1
        );
        return new rw.gov.primature.tms.utils.File(directory, fileName, extension, fileBaseName);
    }

    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf(".");
        if(dotIndex < 0) {
            return null;
        }
        return fileName.substring(dotIndex+1);
    }

    String handleFileName(String fileName, UUID id)
            throws InvalidFileException {

        String cleanFileName = fileName.replaceAll("[^A-Za-z0-9.()]", "");
        String extension = getFileExtension(cleanFileName);

        if(!isValidExtension(cleanFileName)) {
            throw new InvalidFileException("Invalid File Extension");
        }

        String base = "image-"+id;

        cleanFileName = base+"."+extension;

        return cleanFileName;
    }

    boolean isValidExtension(String fileName)
            throws InvalidFileException {
        String fileExtension = getFileExtension(fileName);



        if (fileExtension == null) {
            throw new InvalidFileException("No File Extension");
        }

        fileExtension = fileExtension.toLowerCase();

        for (String validExtension : extensions.split(",")) {
            if (fileExtension.equals(validExtension)) {
                return true;
            }
        }
        return false;
    }

}
