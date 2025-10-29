# JPA 사용 시 Column 암복호화 방식

## 1. ColumnTransformer
- RDBMS에서 지원하는 함수를 사용해서 컬럼 암복호화를 지원
```kotlin
object ColumnEncryptionConstants {
  const val SECRET_KEY = "SECRET_KEY"
  const val SQL_BINDING_LETTER = "?"
  const val DEC_COLUMN_PREFIX = "CAST(AES_DECRYPT(UNHEX("
  const val DEC_COLUMN_SUFFIX = "), '$SECRET_KEY') AS CHAR)"
  
  const val ENC_COLUMN = "HEX(AES_ENCRYPT($SQL_BINDING_LETTER, '$SECRET_KEY'))"
  const val DEC_USERNAME = DEC_COLUMN_PREFIX + "username" + DEC_COLUMN_SUFFIX
}

@Entity
class User(
  @Id
  var id: String? = null,

  @Column
  @ColumnTransformer(read = DEC_USERNAME, write = ENC_COLUMN)
  var username: String,
)
```
```sql
INSERT INTO USER (USERNAME, ID) VALUES ( HEX(AES_ENCRYPT(?, 'SECRET_KEY')), ? );
    
SELECT HEX(AES_ENCRYPT(username, 'SECRET_KEY')) FROM USER;
```
### 단점
- Annotation 방식으로 동작되다보니 `SECRET_KEY`가 컴파일 시점에 필요하여 `하드 코딩`되어야 함


## 2. AttributeConverter
- RDBMS와 동일한 암복화 기능을 구현
- [EncryptedStringConverter.kt](src/main/kotlin/tutorials/jpa/config/EncryptedStringConverter.kt) 참조
```sql
INSERT INTO USER (USERNAME, ID) VALUES ( ?, ? );
```
### 단점
- Application단에서 암복호화가 실행되어 암호화한 값을 저장되어 Like 검색 미지원


## 3. StatementInspector
- 실행되는 SQL에 접근할 수 있는 점에 착안하여 암호화 키를 치환, 소스에서 제거할 수 있음
- @ColumnTransformer 방식과 동일하게 선언 가능
### 적용 방법
#### Custom Function 등록
- Custom Function 추가하여 @ColumnTransformer 방식과 유사하게 동작
- [DatabaseFunctionInitializer](src/main/kotlin/tutorials/jpa/config/DatabaseFunctionInitializer.kt) 참조
#### StatementInspector 구현
- [EncryptionStatementInspector](src/main/kotlin/tutorials/jpa/config/EncryptionStatementInspector.kt) 참조
```kotlin
class EncryptionStatementInspector(
  private val secretKey: String,
) : StatementInspector {

  override fun inspect(sql: String): String {
    if (sql.contains(INSPECTOR_SECRET_KEY)) {
      return sql.replace(INSPECTOR_SECRET_KEY, "'" + secretKey + "'")
    }

    return sql
  }
}
```
#### StatementInspector 등록
- [HibernateConfig](src/main/kotlin/tutorials/jpa/config/HibernateConfig.kt) 참조
```kotlin
class HibernateConfig(
  val secretKey: String,
) {

  @Bean
  fun hibernatePropertiesCustomizer(): HibernatePropertiesCustomizer {
    return HibernatePropertiesCustomizer { hibernateProperties: MutableMap<String, Any> ->
      val inspector = EncryptionStatementInspector(secretKey)
      hibernateProperties[AvailableSettings.STATEMENT_INSPECTOR] = inspector
    }
  }

}
```

## 4. Integrator
- Entity Metadata에 접근하여 @ColumnTransformer를 통해 설정된 customRead, customWrite를 변경할 수 있음
- @ColumnTransformer 방식과 동일하게 선언 가능
```kotlin
class EncryptionIntegrator(
  val secretKey: String,
) : Integrator {

  override fun integrate(
    metadata: Metadata,
    bootstrapContext: BootstrapContext,
    sessionFactory: SessionFactoryImplementor,
  ) {
    metadata.entityBindings.forEach { entity ->
      entity.referenceableProperties.forEach { property ->
        property.columns.forEach { column ->
          val customRead = column.customRead
          if (customRead != null && customRead.contains(INSPECTOR_SECRET_KEY)) {
            column.customRead = customRead.replace(INSPECTOR_SECRET_KEY, "'${secretKey}'")
          }

          val customWrite = column.customWrite
          if (customWrite != null && customWrite.contains(INSPECTOR_SECRET_KEY)) {
            column.customWrite = customWrite.replace(INSPECTOR_SECRET_KEY, "'${secretKey}'")
          }
        }
      }
    }
  }
  
}
```
