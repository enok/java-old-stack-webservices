<?xml version="1.0" encoding="UTF-8"?>
<!--
  SMELL #2: SUMMIT's hold summary is produced by XSLT and then stuffed into a
  plain xs:string element (sm:summitHoldSummaryXml) as escaped XML. A string
  carrying XML inside an XML document. This is the single worst thing in the
  repository and it is load-bearing.
-->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

  <xsl:output method="xml" indent="no" omit-xml-declaration="yes"/>

  <xsl:template match="/">
    <SummitHoldSummary>
      <xsl:attribute name="count">
        <xsl:value-of select="count(//*[local-name()='hold'])"/>
      </xsl:attribute>
      <xsl:for-each select="//*[local-name()='hold']">
        <Hold>
          <xsl:attribute name="code">
            <xsl:value-of select="*[local-name()='holdCode']"/>
          </xsl:attribute>
          <xsl:attribute name="releasable">
            <xsl:value-of select="*[local-name()='releasable']"/>
          </xsl:attribute>
          <xsl:value-of select="*[local-name()='holdReason']"/>
        </Hold>
      </xsl:for-each>
    </SummitHoldSummary>
  </xsl:template>

</xsl:stylesheet>
