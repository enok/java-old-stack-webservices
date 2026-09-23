<?xml version="1.0" encoding="UTF-8"?>
<!--
  SMELL #2: per-customer XSLT response shaping.
  RIVERTON's middleware (a 2009 TIBCO flow) cannot read the canonical element
  order, so we re-order and re-label the marshalled response on the way out.
  This stylesheet runs on EVERY RIVERTON getStudent call.
-->
<xsl:stylesheet version="1.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:sm="http://campusconnect.example.edu/sis/student/messages/v1"
                xmlns:sc="http://campusconnect.example.edu/sis/student/common/v1">

  <xsl:output method="xml" indent="no" omit-xml-declaration="yes"/>

  <xsl:template match="/">
    <RivertonStudent>
      <xsl:apply-templates select="//sc:*[local-name()='rivertonStudentDetail']
                                 | //sm:getStudentResponse/*[local-name()='rivertonStudentDetail']"/>
    </RivertonStudent>
  </xsl:template>

  <xsl:template match="*[local-name()='rivertonStudentDetail']">
    <!-- RIVERTON's order: campus id first, then name, then the canonical id. -->
    <CampusId><xsl:value-of select="*[local-name()='rivertonCampusId']"/></CampusId>
    <Surname><xsl:value-of select="*[local-name()='lastName']"/></Surname>
    <GivenName><xsl:value-of select="*[local-name()='firstName']"/></GivenName>
    <SisId><xsl:value-of select="*[local-name()='studentId']"/></SisId>
    <Dob><xsl:value-of select="*[local-name()='dateOfBirth']"/></Dob>
    <Pidm><xsl:value-of select="*[local-name()='legacyBannerPidm']"/></Pidm>
    <Residency><xsl:value-of select="*[local-name()='residencyIndicator']"/></Residency>
    <!-- TODO: advisorNetworkId is silently dropped here. RIVERTON has asked
         twice. Nobody owns this stylesheet. -->
  </xsl:template>

</xsl:stylesheet>
