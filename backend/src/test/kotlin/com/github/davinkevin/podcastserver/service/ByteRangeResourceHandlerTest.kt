package com.github.davinkevin.podcastserver.service

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.nio.file.Paths
import javax.servlet.http.HttpServletRequest

/**
 * Created by kevin on 02/09/2017
 */
class ByteRangeResourceHandlerTest {

    private val brh = ByteRangeResourceHandler()

    @Test
    fun `should expose a file resource`() {
        /* GIVEN */
        val file = Paths.get("/tmp/foo")
        val request: HttpServletRequest = mock()
        whenever(request.getAttribute(ByteRangeResourceHandler.ATTR_FILE)).thenReturn(file)

        /* WHEN  */
        val r = brh.getResource(request)

        /* THEN  */
        assertThat(r.file.toPath()).isEqualByComparingTo(file)
    }

    @Test
    fun `should raise exception if no attribute`() {
        /* GIVEN */
        val request = mock(HttpServletRequest::class.java)
        /* WHEN  */
        assertThatThrownBy { brh.getResource(request) }
                /* THEN  */
                .hasMessage("Error during serving of byte range resources")
    }

}