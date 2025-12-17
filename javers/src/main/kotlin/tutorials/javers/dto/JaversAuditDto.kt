package tutorials.javers.dto

import org.javers.core.Changes
import org.javers.core.commit.CommitId
import org.javers.core.diff.Change
import org.javers.core.diff.changetype.InitialValueChange
import org.javers.core.diff.changetype.NewObject
import org.javers.core.diff.changetype.ObjectRemoved
import org.javers.core.diff.changetype.ReferenceChange
import org.javers.core.diff.changetype.ValueChange
import org.javers.core.diff.changetype.container.ListChange
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.core.metamodel.`object`.SnapshotType
import java.math.BigDecimal
import java.time.LocalDateTime

data class SearchCondition(
  val author: String? = null,
  val domainType: String? = null,
  val fromDate: LocalDateTime? = null,
  val toDate: LocalDateTime? = null,
  val pageNumber: Int = 0,
  val pageSize: Int = 10,
)

data class AuditDto(
  val commitId: BigDecimal,
  val author: String,
  val commitDate: LocalDateTime,
  val changedDomains: List<ChangedDomainInfo>,
) {
  companion object {
    fun from(commitId: CommitId, snapshots: List<CdoSnapshot>): AuditDto {
      return AuditDto(
        commitId = commitId.valueAsNumber(),
        author = snapshots.first().commitMetadata.author,
        commitDate = snapshots.first().commitMetadata.commitDate,
        changedDomains = snapshots.map { ChangedDomainInfo.from(it) }
      )
    }
  }

  override fun toString(): String {
    return """
      AuditDto(
        commitId: $commitId,
        author: $author,
        commitDate: $commitDate,
        changeType: ${changedDomains.map { it.changeType }.distinct().joinToString { it }},
        changedDomains: ${changedDomains.joinToString { it.domainType }},
      )
    """.trimIndent()
  }
}

data class ChangedDomainInfo(
  val domainType: String,
  val changeType: String,
  val version: Long,
) {
  companion object {
    fun from(snapshot: CdoSnapshot): ChangedDomainInfo {
      return ChangedDomainInfo(
        domainType = snapshot.globalId.typeName.substringAfterLast("."),
        changeType = when (snapshot.type) {
          SnapshotType.INITIAL -> "CREATE"
          SnapshotType.UPDATE -> "UPDATE"
          SnapshotType.TERMINAL -> "DELETE"
        },
        version = snapshot.version
      )
    }
  }
}

data class AuditDetailDto(
  val commitId: BigDecimal,
  val author: String,
  val commitDate: LocalDateTime,
  val changedTables: List<ChangedTableDetail>,
) {
  companion object {
    fun from(changes: Changes): AuditDetailDto {
      val firstChange = changes.first()
      val commitMetadata = firstChange.commitMetadata.get()

      val changedTables = changes
        .groupBy { it.affectedGlobalId.typeName }
        .map { (typeName, domainChanges) ->
          ChangedTableDetail.from(typeName, domainChanges)
        }

      return AuditDetailDto(
        commitId = commitMetadata.id.valueAsNumber(),
        author = commitMetadata.author,
        commitDate = commitMetadata.commitDate,
        changedTables = changedTables
      )
    }
  }
}

data class ChangedTableDetail(
  val tableName: String,
  val changeType: String,
  val changedProperties: List<PropertyChange>,
) {
  companion object {
    fun from(typeName: String, changes: List<Change>): ChangedTableDetail {
      val tableName = typeName.substringAfterLast('.')

      val changeType = when {
        changes.any { it is NewObject } -> "CREATE"
        changes.any { it is ObjectRemoved } -> "DELETE"
        else -> "UPDATE"
      }

      val changedProperties = changes.mapNotNull { change ->
        when (change) {
          is InitialValueChange -> {
            PropertyChange(
              propertyName = change.propertyName,
              oldValue = change.left?.toString(),
              newValue = change.right?.toString()
            )
          }

          is ValueChange -> {
            PropertyChange(
              propertyName = change.propertyName,
              oldValue = change.left?.toString(),
              newValue = change.right?.toString()
            )
          }

          is ReferenceChange -> {
            PropertyChange(
              propertyName = change.propertyName,
              oldValue = change.left?.toString(),
              newValue = change.right?.toString()
            )
          }

          is ListChange -> {
            PropertyChange(
              propertyName = change.propertyName,
              oldValue = change.left?.toString(),
              newValue = change.right?.toString()
            )
          }

          is NewObject -> null
          is ObjectRemoved -> null
          else -> {
            PropertyChange(
              propertyName = change.javaClass.simpleName,
              oldValue = "Changed",
              newValue = change.toString()
            )
          }
        }
      }

      return ChangedTableDetail(
        tableName = tableName,
        changeType = changeType,
        changedProperties = changedProperties
      )
    }
  }
}

data class PropertyChange(
  val propertyName: String,
  val oldValue: String?,
  val newValue: String?,
)
