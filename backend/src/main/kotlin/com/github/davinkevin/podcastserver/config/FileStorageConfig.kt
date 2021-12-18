package com.github.davinkevin.podcastserver.config

import com.github.davinkevin.podcastserver.service.FileStorageService
import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.netty.http.client.HttpClient
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Configuration
import java.net.URI

/**
 * Created by kevin on 17/12/2021
 */
@Configuration
@EnableConfigurationProperties(StorageProperties::class)
class FileStorageConfig {

    @Bean
    fun fileStorageService(
        webClientBuilder: WebClient.Builder,
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
            .region(Region.EU_CENTRAL_1)
            .build()

        val wcb = webClientBuilder
            .clone()
            .clientConnector(ReactorClientHttpConnector(HttpClient.create().followRedirect(true)))

        return FileStorageService(wcb, bucketClient, properties.bucket)
    }

    @Bean
    fun createBucket(file: FileStorageService) = CommandLineRunner {
        file.initBucket().blockOptional()
    }
}

@ConstructorBinding
@ConfigurationProperties(value = "podcastserver.storage")
data class StorageProperties(
    val bucket: String = "data",
    val username: String,
    val password: String,
    val url: URI = URI.create("http://storage:9000/")
)
