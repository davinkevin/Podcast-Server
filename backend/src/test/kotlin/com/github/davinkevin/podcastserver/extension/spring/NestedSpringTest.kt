package com.github.davinkevin.podcastserver.extension.spring

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.context.junit.jupiter.SpringExtensionConfig
import java.lang.annotation.Inherited

@ExtendWith(SpringExtension::class)
@Inherited
@SpringExtensionConfig(useTestClassScopedExtensionContext = true)
annotation class NestedSpringTest
// REASON: https://github.com/spring-projects/spring-framework/issues/31456