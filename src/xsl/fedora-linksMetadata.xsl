<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
    xmlns:dams="http://library.ucsd.edu/ontology/dams#"
    xmlns:ns0="info:fedora/fedora-system:def/model#">
  <xsl:param name="objid"/>
  <xsl:template match="/">
    <rdf:RDF>
      <rdf:Description rdf:about="info:fedora/{$objid}">
        <xsl:choose>
          <xsl:when test="//dams:RelatedResource[dams:type='hydra-afmodel']">
            <ns0:hasModel rdf:resource="{//dams:RelatedResource[dams:type='hydra-afmodel']/dams:uri}"/>
          </xsl:when>
          <xsl:otherwise>
            <ns0:hasModel rdf:resource="info:fedora/afmodel:DamsObject"/>
          </xsl:otherwise>
        </xsl:choose>
      </rdf:Description>
    </rdf:RDF>
  </xsl:template>
</xsl:stylesheet>
