package com.github.davinkevin.podcastserver.service.storage

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Configuration
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import java.net.URI

/**
 * Created by kevin on 17/12/2021
 */
@Configuration
@EnableConfigurationProperties(StorageProperties::class)
class FileStorageConfig {

    @Bean
    fun fileStorageService(
        rcb: RestClient.Builder,
        properties: StorageProperties,
    ): FileStorageService {
        val s3conf = S3Configuration.builder()
            .pathStyleAccessEnabled(true)
            .build()

        val s3Credentials = AwsBasicCredentials.create(properties.username, properties.password)

        val bucketClient =  S3AsyncClient.builder()
            .credentialsProvider { s3Credentials }
            .serviceConfiguration(s3conf)
            .endpointOverride(properties.url)
            .region(Region.AWS_GLOBAL)
            .asyncConfiguration {  }
            .build()

        val preSignerBuilder = S3Presigner.builder()
            .region(Region.AWS_GLOBAL)
            .credentialsProvider { s3Credentials }
            .endpointOverride(properties.url)
            .serviceConfiguration(s3conf)

        val externalPreSigner: (URI) -> S3Presigner = { preSignerBuilder.build() }
        val requestSpecificPreSigner: (URI) -> S3Presigner = { preSignerBuilder.endpointOverride(it).build() }

        val preSigner = if (properties.isInternal) requestSpecificPreSigner else externalPreSigner

        return FileStorageService(
            rcb = rcb.clone(),
            bucket = bucketClient,
            preSignerBuilder = preSigner,
            properties = properties,
        )
    }

    @Bean
    fun createBucket(file: FileStorageService) = CommandLineRunner {
        file.initBucket()
    }
}

@ConfigurationProperties(value = "podcastserver.storage")
data class StorageProperties(
    val bucket: String,
    val username: String,
    val password: String,
    val url: URI,
    val isInternal: Boolean = false,
)
