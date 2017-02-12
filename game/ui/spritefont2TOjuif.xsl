<?xml version="1.0"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<!-- JevaEngine's font transformation. Designed for use with SpriteFont 2 Texture Tool -->

	<!-- Output with -->
	<xsl:output method="text" indent="no" />

	<xsl:template match="fontMetrics">
		<xsl:text>{ "glyphs": [ </xsl:text>
		<xsl:for-each select="character">
			<xsl:call-template name="character" />
		</xsl:for-each>
		<xsl:text>], "texture": "</xsl:text>
		<xsl:call-template name="escape-javascript">
			<xsl:with-param name="string" select="@file"/>
		</xsl:call-template>
		<xsl:text>"}</xsl:text>
	</xsl:template>

	<xsl:template name="character">
		<xsl:if test="position() > 1">
			<xsl:text>,</xsl:text>
		</xsl:if>
		
		<xsl:text>{ "char": </xsl:text>
		<xsl:value-of select="@key"/>
		<xsl:text>, "region": { "width": </xsl:text>
		<xsl:value-of select="width"/>
		<xsl:text>, "height": </xsl:text>
		<xsl:value-of select="height"/>
		<xsl:text>, "x": </xsl:text>
		<xsl:value-of select="x"/>
		<xsl:text>, "y": </xsl:text>
		<xsl:value-of select="y"/>
		<xsl:text>}}</xsl:text>
	</xsl:template>

	<xsl:template name="escape-javascript">
		<xsl:param name="string" />
		<xsl:choose>

			<xsl:when test="contains($string, '&quot;')">
				<xsl:call-template name="escape-javascript">
					<xsl:with-param name="string"
						select="substring-before($string, '&quot;')" />
				</xsl:call-template>
				<xsl:text>\"</xsl:text>
				<xsl:call-template name="escape-javascript">
					<xsl:with-param name="string"
						select="substring-after($string, '&quot;')" />
				</xsl:call-template>
			</xsl:when>

			<xsl:when test="contains($string, '&#xA;')">
				<xsl:call-template name="escape-javascript">
					<xsl:with-param name="string"
						select="substring-before($string, '&#xA;')" />
				</xsl:call-template>
				<xsl:text>\n</xsl:text>
				<xsl:call-template name="escape-javascript">
					<xsl:with-param name="string"
						select="substring-after($string, '&#xA;')" />
				</xsl:call-template>
			</xsl:when>

			<xsl:when test="contains($string, '\')">
				<xsl:value-of select="substring-before($string, '\')" />
				<xsl:text>\\</xsl:text>
				<xsl:call-template name="escape-javascript">
					<xsl:with-param name="string" select="substring-after($string, '\')" />
				</xsl:call-template>
			</xsl:when>

			<xsl:otherwise>
				<xsl:value-of select="$string" />
			</xsl:otherwise>

		</xsl:choose>
	</xsl:template>
</xsl:stylesheet>
