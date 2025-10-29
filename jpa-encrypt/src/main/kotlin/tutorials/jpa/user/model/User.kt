package tutorials.jpa.user.model

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.ColumnTransformer
import org.hibernate.annotations.UuidGenerator
import tutorials.jpa.base.constants.ColumnEncryptionConstants
import tutorials.jpa.config.EncryptedStringConverter

@Entity
@Table(name = "mt_user")
class User(
  @Id
  @UuidGenerator
  @Column(length = 50)
  var id: String? = null,

  @Column(nullable = false, unique = true)
  @ColumnTransformer(read = ColumnEncryptionConstants.DEC_USERNAME, write = ColumnEncryptionConstants.ENC_COLUMN)
  var username: String,

  @Column(nullable = false)
  @ColumnTransformer(
    read = ColumnEncryptionConstants.INSPECTOR_DEC_NAME,
    write = ColumnEncryptionConstants.INSPECTOR_ENC_COLUMN
  )
  var name: String,

  @Column(nullable = true)
  @Convert(converter = EncryptedStringConverter::class)
  var nickname: String? = null,

  ) {

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as User

    return id == other.id
  }

  override fun hashCode(): Int {
    return id?.hashCode() ?: 0
  }

  override fun toString(): String {
    return "User(id=$id, username='$username', name='$name', nickname=$nickname)"
  }

}
