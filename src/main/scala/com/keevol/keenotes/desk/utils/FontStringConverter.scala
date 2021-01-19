package com.keevol.keenotes.desk.utils

import javafx.scene.text.{Font, FontWeight}
import javafx.util.StringConverter
import org.apache.commons.lang3.StringUtils
import org.slf4j.{Logger, LoggerFactory}

class FontStringConverter extends StringConverter[Font] {
  val logger: Logger = LoggerFactory.getLogger(classOf[FontStringConverter])

  override def toString(font: Font): String = s"${font.getFamily}, ${font.getSize}, ${font.getStyle}"

  override def fromString(fontString: String): Font = {
    logger.info(s"create font from string: $fontString")
    val fontFamily = StringUtils.substringBefore(fontString, ",")
    val fontSize = StringUtils.substringBetween(fontString, ", ", ", ")
    val fontStyle = StringUtils.substringAfterLast(fontString, ", ")

    val f = if (StringUtils.contains(fontStyle.toLowerCase, "bold")) {
      Font.font(fontFamily, FontWeight.BOLD, fontSize.toDouble)
    } else {
      Font.font(fontFamily, fontSize.toDouble)
    }
    f
  }
}