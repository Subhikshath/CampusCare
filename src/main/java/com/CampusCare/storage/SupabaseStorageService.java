package com.CampusCare.storage;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class SupabaseStorageService implements StorageService {

	@Value("${supabase.url:}")
	private String supabaseUrl;

	@Value("${supabase.service-key:}")
	private String serviceKey;

	@Value("${supabase.storage-bucket:complaint-images}")
	private String bucket;

	@Override
	public String upload(MultipartFile file, String path) {
		if (serviceKey.isBlank() || supabaseUrl.isBlank()) {
			throw new IllegalStateException("Supabase Storage is not configured");
		}
		try {
			RestClient.create()
					.post()
					.uri(supabaseUrl + "/storage/v1/object/" + bucket + "/" + path)
					.header("Authorization", "Bearer " + serviceKey)
					.header("apikey", serviceKey)
					.contentType(file.getContentType() == null
							? MediaType.APPLICATION_OCTET_STREAM
							: MediaType.parseMediaType(file.getContentType()))
					.body(file.getBytes())
					.retrieve()
					.toBodilessEntity();
			return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
		} catch (Exception exception) {
			throw new IllegalStateException("Unable to upload complaint image", exception);
		}
	}
}
