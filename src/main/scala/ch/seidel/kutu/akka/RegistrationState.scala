package ch.seidel.kutu.akka

import ch.seidel.kutu.domain.{JudgeRegistration, SyncAction}

case class JudgeSyncAtions(
                            changed: List[JudgeRegistration],
                            removed: List[JudgeRegistration],
                            added: List[JudgeRegistration]
                          ) {
  def nonEmpty = changed.nonEmpty || removed.nonEmpty || added.nonEmpty
}

case class RegistrationState(
                              syncJudgeList: List[JudgeRegistration] = List(),
                              syncJudgeListNotified: List[JudgeRegistration] = List(),
                              syncActions: List[SyncAction] = List(),
                              syncActionsNotified: List[SyncAction] = List(),
                            ) {

  def hasChanges: Boolean =
    ((syncActions.nonEmpty && syncActions != syncActionsNotified)
      || (syncJudgeList.nonEmpty && syncJudgeList != syncJudgeListNotified))

  def judgeSyncActions: JudgeSyncAtions = {
    val changed = syncJudgeList.filter(j => syncJudgeListNotified.exists(jj => jj.id == j.id && jj != j))
    val removed = syncJudgeListNotified.filter(j => !syncJudgeList.exists(jj => jj.id == j.id))
    val added = syncJudgeList.filter(j => !syncJudgeListNotified.exists(jj => jj.id == j.id))
    JudgeSyncAtions(changed, removed, added)
  }

  def resynced(syncActions: List[SyncAction], syncJudgeList: List[JudgeRegistration]): RegistrationState =
    RegistrationState(syncJudgeList, this.syncJudgeListNotified, syncActions, this.syncActionsNotified)

  def notified(): RegistrationState =
    RegistrationState(this.syncJudgeList, this.syncJudgeList, this.syncActions, this.syncActions)
}
