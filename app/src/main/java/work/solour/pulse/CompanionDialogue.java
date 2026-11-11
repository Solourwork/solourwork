package work.solour.pulse;

/** Question/answer policy prototype; production sensor inference and ASR are deliberately not wired to it. */
public final class CompanionDialogue {
    public enum Topic { REST, CHANGE, END }
    public enum Outcome { WAITING, REST_CONFIRMED, CHANGE_CONFIRMED, END_CONFIRMED, DECLINED, QUIET, UNCHANGED }
    private Topic pending;
    private long deadline, nextQuestion;
    private boolean quiet,listening;
    public Topic pending(){return pending;}
    public boolean quiet(){return quiet;}
    public boolean offer(Topic topic,long now,boolean freshSignals,int independentSignals,long stableMillis){
        if(quiet||pending!=null||now<nextQuestion||!freshSignals||independentSignals<2||stableMillis<20000)return false;
        pending=topic;listening=false;deadline=Long.MAX_VALUE;nextQuestion=now+300000;return true;
    }
    public void beginReplyWindow(long now){if(pending!=null){listening=true;deadline=now+6000;}}
    public String question(){if(pending==Topic.REST)return "现在在休息吗？";if(pending==Topic.CHANGE)return "运动节奏好像变了，要换个更带劲的节奏吗？";if(pending==Topic.END)return "要结束这次陪伴吗？";return "";}
    public Outcome answer(String text,long now){
        if(pending==null||!listening)return Outcome.UNCHANGED;
        if(now>=deadline){expire(now);return Outcome.UNCHANGED;}
        String v=text==null?"":text.replaceAll("[\\s，。！？,.!?]","");
        if(v.equals("安静")||v.equals("安静陪我")||v.equals("别问了")||v.equals("少问一点")){pending=null;quiet=true;return Outcome.QUIET;}
        if(v.equals("不是")||v.equals("不用")||v.equals("不用了")||v.equals("还在动")||v.equals("继续")||v.equals("我还在动")||v.equals("不要")){pending=null;nextQuestion=now+600000;return Outcome.DECLINED;}
        if(pending==Topic.REST&&(v.equals("休息")||v.equals("休息一下")||v.equals("我在休息"))){pending=null;return Outcome.REST_CONFIRMED;}
        if(pending==Topic.END&&(v.equals("结束")||v.equals("结束陪伴")||v.equals("收工"))){pending=null;return Outcome.END_CONFIRMED;}
        if(v.equals("是")||v.equals("是的")||v.equals("对")||v.equals("对的")||v.equals("好")||v.equals("好的")||v.equals("可以")){
            Topic p=pending;pending=null;return p==Topic.REST?Outcome.REST_CONFIRMED:p==Topic.CHANGE?Outcome.CHANGE_CONFIRMED:Outcome.END_CONFIRMED;
        }
        return Outcome.WAITING;
    }
    public Outcome expire(long now){if(pending!=null&&listening&&now>=deadline){pending=null;nextQuestion=now+600000;return Outcome.UNCHANGED;}return Outcome.WAITING;}
    public void interrupt(){pending=null;listening=false;}
}
