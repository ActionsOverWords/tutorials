package tutorials.jpa.base.constants

object ColumnEncryptionConstants {

  const val SECRET_KEY = "tutorials-secret-key"
  const val SQL_BINDING_LETTER = "?"
  const val DEC_COLUMN_PREFIX = "CAST(AES_DECRYPT(UNHEX("
  const val DEC_COLUMN_SUFFIX = "), '$SECRET_KEY') AS CHAR)"
  const val ENC_COLUMN = "HEX(AES_ENCRYPT($SQL_BINDING_LETTER, '$SECRET_KEY'))"
  const val DEC_USERNAME = DEC_COLUMN_PREFIX + "username" + DEC_COLUMN_SUFFIX

  const val INSPECTOR_SECRET_KEY: String = "'__SECRET_KEY__'"
  const val INSPECTOR_ENC_COLUMN: String = "encrypt_column(?, $INSPECTOR_SECRET_KEY)"
  const val INSPECTOR_DEC_NAME: String = "decrypt_column(name, $INSPECTOR_SECRET_KEY)"

}
