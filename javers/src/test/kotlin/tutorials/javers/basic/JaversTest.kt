package tutorials.javers.basic

import org.javers.core.Javers
import org.javers.core.JaversBuilder
import org.javers.core.diff.changetype.InitialValueChange
import org.javers.core.diff.changetype.ValueChange
import org.javers.core.metamodel.annotation.DiffIgnore
import org.javers.core.metamodel.`object`.CdoSnapshot
import org.javers.repository.jql.QueryBuilder
import org.javers.spring.auditable.SpringSecurityAuthorProvider
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import tutorials.javers.base.extentions.logger

class JaversTest {

  private val log by logger()

  private val javers: Javers = JaversBuilder.javers().build()

  data class Team(val name: String, val members: MutableList<Member>) {
    fun addMember(member: Member) {
      members.add(member)
    }
  }

  data class Member(
    val name: String,
    var age: Int,
    @DiffIgnore
    var password: String? = "",
  ) {
    fun increaseAge() {
      age += 1
    }
  }

  fun logs(snapshots: List<CdoSnapshot>) {
    snapshots.forEach { log.info("snapshot: $it") }
  }

  @Test
  fun `두 객체를 비교하여 변경사항을 감지한다`() {
    val member1 = Member("member1", 20)
    val member2 = Member("member2", 30)

    val diff = javers.compare(member1, member2)
    log.info("diff: $diff")

    val changes = diff.changes
    assertEquals(2, changes.size)

    val valueChange = changes[0] as ValueChange
    assertEquals("name", valueChange.propertyName)
    assertEquals("member1", valueChange.left)
    assertEquals("member2", valueChange.right)
  }

  @Test
  fun `동일 객체는 변경 사항이 없다`() {
    val member1 = Member("member1", 20)
    val member2 = Member("member1", 20)

    val diff = javers.compare(member1, member2)
    assertEquals(0, diff.changes.size)
  }

  @Test
  fun `변경 내역을 Snapshot으로 조회한다`() {
    val member = Member("member", 30)
    javers.commit("author", member)

    val snapshots = javers.findSnapshots(
      QueryBuilder.byInstance(member).build()
    )
    logs(snapshots)

    assertNotNull(snapshots)
    assertEquals(1, snapshots.size)

    val snapshot = snapshots[0]
    assertEquals("member", snapshot.getPropertyValue("name"))
    assertEquals(30, snapshot.getPropertyValue("age"))
  }

  @Test
  fun `변경 내역을 Changes로 조회한다`() {
    val member = Member("member", 30)
    javers.commit("author", member)

    val changes = javers.findChanges(
      QueryBuilder.byInstance(member).build()
    )
    log.info("changes: $changes")

    assertNotNull(changes)
    assertEquals(2, changes.size)

    val change = changes[0] as InitialValueChange
    assertEquals("name", change.propertyName)
    assertEquals(null, change.left)
    assertEquals("member", change.right)
  }

  @Test
  fun `객체의 변경 이력을 추적한다`() {
    val member = Member("member", 30)
    javers.commit("author", member)

    member.increaseAge()
    javers.commit("author", member)

    member.increaseAge()
    javers.commit("author", member)

    val snapshots = javers.findSnapshots(
      QueryBuilder.byInstance(member).build()
    )
    logs(snapshots)

    assertEquals(3, snapshots.size)
  }

  @Test
  fun `List도 감지된다`() {
    val member1 = Member("member1", 20)
    val member2 = Member("member2", 30)

    val team = Team("team", mutableListOf(member1))
    javers.commit("author", team)

    team.addMember(member2)
    javers.commit("author", team)

    val snapshots = javers.findSnapshots(
      QueryBuilder.byInstance(team).build()
    )
    logs(snapshots)
    SpringSecurityAuthorProvider

    assertEquals(2, snapshots.size)
  }

  @Test
  fun `DiffIgnore 필드는 변경 감지 시 무시된다`() {
    val member1 = Member("member1", 20, "1234")
    val member2 = Member("member1", 20, "abcd")

    val diff = javers.compare(member1, member2)
    assertEquals(0, diff.changes.size)
  }

}
