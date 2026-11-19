<?xml version="1.0" encoding="UTF-8"?>
<!-- XML -> CSV (texto plano, separador «;») - Clase de 2º DAM -->
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
    <xsl:output method="text" encoding="UTF-8"/>

    <xsl:template match="/clase">
        <xsl:text>id;nombre;modulo;notaMedia;horasFaltas&#10;</xsl:text>
        <xsl:for-each select="alumno">
            <xsl:value-of select="@id"/><xsl:text>;</xsl:text>
            <xsl:value-of select="normalize-space(nombre)"/><xsl:text>;</xsl:text>
            <xsl:value-of select="normalize-space(modulo)"/><xsl:text>;</xsl:text>
            <xsl:value-of select="notaMedia"/><xsl:text>;</xsl:text>
            <xsl:value-of select="horasFaltas"/><xsl:text>&#10;</xsl:text>
        </xsl:for-each>
    </xsl:template>
</xsl:stylesheet>