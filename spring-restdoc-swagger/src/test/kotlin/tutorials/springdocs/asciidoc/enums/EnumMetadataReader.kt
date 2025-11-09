package tutorials.springdocs.asciidoc.enums

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.test.util.ReflectionTestUtils
import java.io.File

class EnumMetadataReader {

  private val log: Logger = LoggerFactory.getLogger(javaClass)

  fun getEnumsMetadataMap(): Map<String, Set<EnumMetadata>> {
    val enumClasses = getEnumClassesFromPackage(ENUM_BASE_PACKAGE)

    return enumClasses
      .associateBy(
        keySelector = { it.simpleName },
        valueTransform = {
          getEnumConstants(it)
        }
      )
  }

  private fun getEnumClassesFromPackage(packageName: String): Set<Class<*>> {
    val path = packageName.replace('.', '/')
    val resources = Thread.currentThread().contextClassLoader.getResources(path)

    val enumClasses = mutableSetOf<Class<*>>()
    while (resources.hasMoreElements()) {
      val resource = resources.nextElement()
      val file = File(resource.toURI())

      file.walkTopDown().forEach { classFile ->
        if (classFile.name.endsWith(".class")) {
          // 클래스 이름을 가져와서 Class로 변환
          val className = "${packageName}.${classFile.relativeTo(file).path.replace(File.separatorChar, '.')}"
            .removeSuffix(".class")
          try {
            val clazz = Class.forName(className)
            if (clazz.isEnum) {
              enumClasses.add(clazz)
            }
          } catch (e: Exception) {
            log.error(e.message, e)
          }
        }
      }
    }
    return enumClasses
  }

  private fun getEnumConstants(enumClass: Class<*>): Set<EnumMetadata> {
    // 결과를 저장할 Set 생성
    val enumMetadataSet = mutableSetOf<EnumMetadata>()

    // enum 클래스의 모든 상수를 꺼낸다
    enumClass.enumConstants.forEach { enumConstant ->
      try {
        val description = ReflectionTestUtils.getField(enumConstant, "description")

        val restDocsDocumentEnum = EnumMetadata(
          name = enumConstant.toString(),
          description = description as String
        )
        enumMetadataSet.add(restDocsDocumentEnum)
      } catch (e: Exception) {
        log.error(e.message, e)
        throw e
      }
    }
    return enumMetadataSet
  }

}
