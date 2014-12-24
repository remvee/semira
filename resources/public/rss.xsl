<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:dc="http://purl.org/dc/elements/1.1/">
  <xsl:template match="/rss/channel">
    <html>
      <head>
        <title><xsl:value-of select="title"/></title>
        <link rel="stylesheet" type="text/css" href="css/screen.css"/>
      </head>
      <body class="rss">
        <div id="container">
          <div id="header">
            <h1><a><xsl:attribute name="href"><xsl:value-of select="link"/></xsl:attribute><xsl:value-of select="title"/></a></h1>
            <div id="description">
              <xsl:value-of select="description"/>
            </div>
          </div>
          <ul class="items">
            <xsl:for-each select="item">
              <li class="item">
                <h2>
                  <a><xsl:attribute name="href"><xsl:value-of select="link"/></xsl:attribute><xsl:value-of select="title"/></a>
                </h2>
                <div class="description">
                  <xsl:value-of select="description" disable-output-escaping="yes"/>
                </div>
                <div class="pubDate">
                  <xsl:value-of select="pubDate"/>
                </div>
              </li>
            </xsl:for-each>
          </ul>
        </div>
      </body>
    </html>
  </xsl:template>
</xsl:stylesheet>

