<?xml version="1.0" encoding="UTF-8"?>
<!-- XML -> HTML (XSLT 1.0) - Clase de 2º DAM -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="html" encoding="UTF-8" indent="yes"/>

    <xsl:template match="/clase">
        <html>
            <head>
                <title>Actas de Evaluación - <xsl:value-of select="@grupo"/></title>
                <style>
                    body { font-family: sans-serif; margin: 2em; color: #222; }
                    h1 { color: #1F3864; }
                    table { border-collapse: collapse; width: 100%; max-width: 800px; }
                    th, td { border: 1px solid #999; padding: 8px 12px; text-align: left; }
                    th { background: #1F3864; color: white; }
                    tr:nth-child(even) { background: #f4f6f9; }
                    .alerta { color: #b00020; font-weight: bold; background-color: #fdecea; }
                    .excelente { color: #1b5e20; font-weight: bold; }
                </style>
            </head>
            <body>
                <h1>Listado de Evaluación · Grupo <xsl:value-of select="@grupo"/></h1>
                <p>Fecha del acta: <strong><xsl:value-of select="@fecha"/></strong> · Total matriculados: <strong><xsl:value-of select="count(alumno)"/> alumnos</strong>.</p>
                <table>
                    <tr>
                        <th>ID</th>
                        <th>Alumno/a</th>
                        <th>Módulo</th>
                        <th>Nota Media</th>
                        <th>Horas de Falta</th>
                    </tr>
                    <xsl:for-each select="alumno">
                        <xsl:sort select="notaMedia" data-type="number" order="descending"/>
                        <tr>
                            <td><xsl:value-of select="@id"/></td>
                            <td><xsl:value-of select="nombre"/></td>
                            <td><xsl:value-of select="modulo"/></td>
                            <td>
                                <xsl:choose>
                                    <xsl:when test="number(notaMedia) &lt; 6.5">
                                        <xsl:attribute name="class">alerta</xsl:attribute>
                                    </xsl:when>
                                    <xsl:when test="number(notaMedia) &gt;= 9.0">
                                        <xsl:attribute name="class">excelente</xsl:attribute>
                                    </xsl:when>
                                </xsl:choose>
                                <xsl:value-of select="format-number(notaMedia, '0.00')"/>
                                <xsl:text> / </xsl:text>
                                <xsl:value-of select="notaMedia/@escala"/>
                            </td>
                            <td>
                                <xsl:if test="number(horasFaltas) &gt;= 10">
                                    <xsl:attribute name="class">alerta</xsl:attribute>
                                </xsl:if>
                                <xsl:value-of select="horasFaltas"/> h
                            </td>
                        </tr>
                    </xsl:for-each>
                </table>
            </body>
        </html>
    </xsl:template>
</xsl:stylesheet>