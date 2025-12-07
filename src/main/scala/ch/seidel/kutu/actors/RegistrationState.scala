package ch.seidel.kutu.actors

import ch.seidel.kutu.domain.{JudgeRegistration, SyncAction}

import java.time.Instant

case class JudgeSyncActions(
                            changed: List[JudgeRegistration],
                            removed: List[JudgeRegistration],
                            added: List[JudgeRegistration]
                          ) {
  def nonEmpty: Boolean = changed.nonEmpty || removed.nonEmpty || added.nonEmpty
}

case class RegistrationState(
                              syncJudgeList: List[JudgeRegistration] = List(),
                              syncJudgeListNotified: List[JudgeRegistration] = List(),
                              syncActions: List[SyncAction] = List(),
                              syncActionsNotified: List[SyncAction] = List(),
                              created: Instant = Instant.now(),
                              emailApproved: Boolean = true
                            ) {
  def approved: RegistrationState =
    RegistrationState(this.syncJudgeList, this.syncJudgeListNotified, syncActions, this.syncActionsNotified, this.created)

  def unapproved: RegistrationState =
    RegistrationState(this.syncJudgeList, this.syncJudgeListNotified, syncActions, this.syncActionsNotified, this.created, emailApproved = false)

  def hasChanges: Boolean =
    (syncActions.nonEmpty && syncActions != syncActionsNotified)
      || (syncJudgeList.nonEmpty && syncJudgeList != syncJudgeListNotified)

  def judgeSyncActions: JudgeSyncActions = {
    val changed = syncJudgeList.filter(j => syncJudgeListNotified.exists(jj => jj.id == j.id && jj != j))
    val removed = syncJudgeListNotified.filter(j => !syncJudgeList.exists(jj => jj.id == j.id))
    val added = syncJudgeList.filter(j => !syncJudgeListNotified.exists(jj => jj.id == j.id))
    JudgeSyncActions(changed, removed, added)
  }

  def resynced(syncActions: List[SyncAction], syncJudgeList: List[JudgeRegistration]): RegistrationState =
    RegistrationState(syncJudgeList, this.syncJudgeListNotified, syncActions, this.syncActionsNotified, created, emailApproved)

  def notified(): RegistrationState =
    RegistrationState(this.syncJudgeList, this.syncJudgeList, this.syncActions, this.syncActions, created, emailApproved)
}
