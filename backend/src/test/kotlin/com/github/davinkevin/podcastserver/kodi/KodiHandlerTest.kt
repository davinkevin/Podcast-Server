package com.github.davinkevin.podcastserver.kodi

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class KodiHandlerTest {

    val header = """
        <!DOCTYPE html>
        <html>
        <body>
        <table>
            <thead>
                <tr>
                    <th><a>Name</a></th>
                    <th><a>Last modified</a></th>
                    <th><a>Size</a></th>
                </tr>
            </thead>
            <tbody>
    """.trimIndent()
    val footer = """
            </tbody>
        </table>
        </body>
        </html>
    """.trimIndent()

    @Test
    fun `should do the job`() {
        /* Given */
        val expected = """
        <!DOCTYPE html>
        <html>
        <body>
        <table>
            <thead>
                <tr>
                    <th><a>Name</a></th>
                    <th><a>Last modified</a></th>
                    <th><a>Size</a></th>
                </tr>
            </thead>
            <tbody>
                <tr>
                    <td><a href="URL">TITLE</a></td>
                    <td align="right"> - </td>
                    <td align="right"> - </td>
                </tr>
                <tr>
                    <td><a href="URL">TITLE</a></td>
                    <td align="right"> - </td>
                    <td align="right"> - </td>
                </tr>
            </tbody>
        </table>
        </body>
        </html>
        """.trimIndent()
        /* When */
        val result = (0 .. 1)
            .joinToString(separator = "\n") {
                """
                    <tr>
                        <td><a href="URL">TITLE</a></td>
                        <td align="right"> - </td>
                        <td align="right"> - </td>
                    </tr>
                """
                    .replaceIndent("        ")
            }

        /* Then */
        assertThat(header + "\n" + result + "\n" + footer).isEqualTo(expected)
    }

}