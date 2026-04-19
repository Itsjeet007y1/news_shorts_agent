package com.example.news.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

@Service
public class GcsService {
	private static final Logger logger = Logger.getLogger(GcsService.class.getName());

	@Value("${gcs.bucket-name:news-images-bucket}")
	private String bucketName;

	private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
	private static final Set<String> ALLOWED_MIME_TYPES = new HashSet<>();

	static {
		ALLOWED_MIME_TYPES.add("image/jpeg");
		ALLOWED_MIME_TYPES.add("image/png");
	}

	/**
	 * Upload a file to Google Cloud Storage
	 *
	 * @param file the MultipartFile to upload
	 * @return the public URL of the uploaded file
	 * @throws IllegalArgumentException if file validation fails
	 * @throws IOException if upload fails
	 */
	public String uploadFile(MultipartFile file) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new IllegalArgumentException("File cannot be null or empty");
		}

		// Validate file type
		String contentType = file.getContentType();
		if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
			throw new IllegalArgumentException(
				"Invalid file type. Allowed types: image/jpeg, image/png. Received: " + contentType
			);
		}

		// Validate file size
		if (file.getSize() > MAX_FILE_SIZE) {
			throw new IllegalArgumentException(
				"File size exceeds maximum limit of 5 MB. Received: " + (file.getSize() / 1024) + " KB"
			);
		}

		// Generate unique filename with UUID
		String fileName = generateFileName(file.getOriginalFilename());

		try {
			// Initialize Storage client
			Storage storage = StorageOptions.getDefaultInstance().getService();

			// Create blob metadata
			BlobId blobId = BlobId.of(bucketName, fileName);
			BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
				.setContentType(contentType)
				.build();

			// Upload file
			storage.create(blobInfo, file.getBytes());

			// Return public URL
			String url = String.format("https://storage.googleapis.com/%s/%s", bucketName, fileName);
			logger.info("File uploaded successfully to GCS: " + url);
			return url;

		} catch (IOException e) {
			logger.severe("Failed to upload file to GCS: " + e.getMessage());
			throw new IOException("Failed to upload file to Google Cloud Storage: " + e.getMessage(), e);
		}
	}

	/**
	 * Generate a unique filename using UUID and preserve the extension
	 *
	 * @param originalFilename the original filename
	 * @return a unique filename with UUID prefix
	 */
	private String generateFileName(String originalFilename) {
		if (originalFilename == null || originalFilename.isEmpty()) {
			return UUID.randomUUID().toString() + ".jpg";
		}

		// Extract file extension
		String fileExtension = "";
		if (originalFilename.contains(".")) {
			fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
		}

		// Generate unique filename with UUID
		return UUID.randomUUID().toString() + fileExtension;
	}

	/**
	 * Delete a file from Google Cloud Storage
	 *
	 * @param fileName the name of the file to delete
	 * @return true if deletion was successful, false otherwise
	 */
	public boolean deleteFile(String fileName) {
		try {
			if (fileName == null || fileName.isEmpty()) {
				return false;
			}

			// Extract just the filename from URL if full URL is provided
			String actualFileName = fileName;
			if (fileName.contains("/")) {
				actualFileName = fileName.substring(fileName.lastIndexOf("/") + 1);
			}

			Storage storage = StorageOptions.getDefaultInstance().getService();
			BlobId blobId = BlobId.of(bucketName, actualFileName);
			boolean deleted = storage.delete(blobId);

			if (deleted) {
				logger.info("File deleted successfully from GCS: " + actualFileName);
			}
			return deleted;

		} catch (Exception e) {
			logger.severe("Failed to delete file from GCS: " + e.getMessage());
			return false;
		}
	}
}
