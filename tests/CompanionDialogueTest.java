package work.solour.pulse;
public class CompanionDialogueTest {
 static int checks;static void ok(boolean v,String label){checks++;if(!v)throw new AssertionError(label);}
 static CompanionDialogue ask(CompanionDialogue.Topic t){CompanionDialogue d=new CompanionDialogue();ok(d.offer(t,0,true,2,20000),"qualified suggestion");return d;}
 public static void main(String[] args){
  CompanionDialogue d=new CompanionDialogue();ok(!d.offer(CompanionDialogue.Topic.REST,0,false,2,20000),"missing data no question");ok(!d.offer(CompanionDialogue.Topic.REST,0,true,1,20000),"one signal insufficient");ok(!d.offer(CompanionDialogue.Topic.REST,0,true,2,19000),"short change ignored");
  d=ask(CompanionDialogue.Topic.REST);ok(d.answer("对",1000)==CompanionDialogue.Outcome.UNCHANGED,"cannot accept while question still speaking");ok(d.pending()!=null,"keeps awaiting speech completion");d.beginReplyWindow(3000);ok(d.answer("不是",4000)==CompanionDialogue.Outcome.DECLINED,"negative never matches affirmative");ok(d.pending()==null,"negative clears pending");ok(!d.offer(CompanionDialogue.Topic.CHANGE,5000,true,2,20000),"no immediate re-prompt");ok(d.offer(CompanionDialogue.Topic.CHANGE,605000,true,2,20000),"cooldown expires");
  d=ask(CompanionDialogue.Topic.REST);d.beginReplyWindow(2500);ok(d.answer("可能吧",3000)==CompanionDialogue.Outcome.WAITING,"unclear reply not confirmation");ok(d.answer("对。",4000)==CompanionDialogue.Outcome.REST_CONFIRMED,"simple affirmative in pending rest context");ok(d.answer("对",4100)==CompanionDialogue.Outcome.UNCHANGED,"no duplicate action");
  d=ask(CompanionDialogue.Topic.CHANGE);d.beginReplyWindow(3000);ok(d.answer("是的",4000)==CompanionDialogue.Outcome.CHANGE_CONFIRMED,"confirmed change");
  d=ask(CompanionDialogue.Topic.END);d.beginReplyWindow(3000);ok(d.answer("结束",4000)==CompanionDialogue.Outcome.END_CONFIRMED,"explicit end only in pending end question");
  d=ask(CompanionDialogue.Topic.REST);d.beginReplyWindow(3000);ok(d.answer("结束",4000)==CompanionDialogue.Outcome.WAITING,"end phrase not misapplied to rest question");ok(d.expire(9000)==CompanionDialogue.Outcome.UNCHANGED,"silence leaves state unchanged");ok(d.pending()==null,"no lingering pending after timeout");ok(!d.offer(CompanionDialogue.Topic.REST,10000,true,2,20000),"silence suppresses repeat");
  d=ask(CompanionDialogue.Topic.END);d.beginReplyWindow(3000);ok(d.answer("对",9000)==CompanionDialogue.Outcome.UNCHANGED,"reply at deadline not accepted");
  d=ask(CompanionDialogue.Topic.REST);d.beginReplyWindow(3000);ok(d.answer("安静陪我",4000)==CompanionDialogue.Outcome.QUIET,"quiet is respected");ok(!d.offer(CompanionDialogue.Topic.CHANGE,999999,true,2,20000),"quiet blocks future questions");
  d=ask(CompanionDialogue.Topic.REST);d.interrupt();ok(d.answer("对",1000)==CompanionDialogue.Outcome.UNCHANGED,"interrupt invalidates answer");
  System.out.println("PASS "+checks+" dialogue-policy prototype assertions; no speech recognition accuracy claimed");
 }
}
