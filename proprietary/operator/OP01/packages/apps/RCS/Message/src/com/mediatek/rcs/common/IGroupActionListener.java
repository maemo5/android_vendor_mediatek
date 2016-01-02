package com.mediatek.rcs.common;

import com.mediatek.rcs.common.service.Participant;

public interface IGroupActionListener {

    void onParticipantAdded(Participant participant);
    void onParticipantLeft(Participant participant);
    void onParticipantRemoved(Participant participant);
    void onChairmenTransferred(Participant newChairmen);
    void onSubjectModified(String newSubject);
    void onNickNameModified(String newNickName);
    void onSelfNickNameModified(String newSelfNickName);
    void onMeRemoved();
    void onGroupAborted();

    void onAddParticipantFail(Participant participant);
    void onAddParticipantsResult(int result);
    void onRemoveParticipantResult(int result);
    void onTransferChairmenResult(int result);
    void onModifySubjectResult(String subject, int result);
    void onModifyNickNameResult(int result);
    void onModifySelfNickNameResult(String selfNickName, int result);
    void onExitGroupResult(int result);
    void onDestroyGroupResult(int result);
}
