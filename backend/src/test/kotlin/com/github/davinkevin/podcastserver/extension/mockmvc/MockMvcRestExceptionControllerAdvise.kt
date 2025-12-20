package com.github.davinkevin.podcastserver.extension.mockmvc

import jakarta.servlet.RequestDispatcher
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.boot.webmvc.autoconfigure.error.BasicErrorController
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.util.WebUtils
import tools.jackson.databind.ObjectMapper

class MockMvcRestExceptionConfiguration(
    private val errorController: BasicErrorController,
    private val om: ObjectMapper
) : WebMvcConfigurer {
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(object : HandlerInterceptor {
            @Throws(Exception::class)
            override fun afterCompletion(
                request: HttpServletRequest,
                response: HttpServletResponse,
                handler: Any,
                ex: java.lang.Exception?
            ) {
                val status: Int = response.status

                if (status < 400) return

                request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status)
                request.setAttribute(WebUtils.ERROR_STATUS_CODE_ATTRIBUTE, status)
                request.setAttribute(WebUtils.ERROR_REQUEST_URI_ATTRIBUTE, request.requestURI.toString())

                // The original exception is already saved as an attribute request
                when (val exception = request.getAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE) as Exception?) {
                    null -> {}
                    is ResponseStatusException -> request.apply {
                        setAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE, exception)
                        setAttribute(WebUtils.ERROR_MESSAGE_ATTRIBUTE, exception.reason)
                    }
                }

                om.writeValue(response.outputStream, errorController.error(request).body)
            }
        })
    }
}
