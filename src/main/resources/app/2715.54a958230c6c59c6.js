"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[2715],{2715:(E,m,c)=>{c.r(m),c.d(m,{RegJudgelistPageModule:()=>B});var l=c(6814),d=c(95),r=c(3582),p=c(335),_=c(5619),b=c(8645),J=c(2096),h=c(2181),v=c(8180),x=c(7398),C=c(3620),I=c(3997),R=c(9397),T=c(4664),Z=c(3020),e=c(2029),f=c(4414),A=c(9253);function k(n,a){if(1&n&&(e.TgZ(0,"span"),e._uU(1),e._UZ(2,"br"),e.qZA()),2&n){const t=a.$implicit;e.xp6(1),e.Oqu(t)}}function y(n,a){if(1&n&&(e.TgZ(0,"ion-note",4),e.YNc(1,k,3,1,"span",5),e.qZA()),2&n){const t=e.oxw();e.xp6(1),e.Q6J("ngForOf",t.getCommentLines())}}let M=(()=>{class n{constructor(){this.selected=new e.vpe}ngOnInit(){}getProgramList(){var t=[];return void 0===this.selectedDisciplinlist&&(this.selectedDisciplinlist=[]),this.selectedDisciplinlist.forEach(i=>{void 0===t.find(o=>o.program==i.program)&&t.push(i)}),t}getProgrammDisciplinText(t){return t.program+"/"+t.disziplin}getProgrammText(t){return t.program}getCommentLines(){return this.judgeregistration.comment.split("\n")}static#e=this.\u0275fac=function(i){return new(i||n)};static#t=this.\u0275cmp=e.Xpm({type:n,selectors:[["app-reg-judge-item"]],inputs:{judgeregistration:"judgeregistration",status:"status",selectedDisciplinlist:"selectedDisciplinlist"},outputs:{selected:"selected"},decls:9,vars:6,consts:[[3,"click"],["slot","start"],["src","assets/imgs/chief.png"],["slot","end",4,"ngIf"],["slot","end"],[4,"ngFor","ngForOf"]],template:function(i,o){1&i&&(e.TgZ(0,"ion-item",0),e.NdJ("click",function(){return o.selected?o.selected.emit(o.judgeregistration):{}}),e.TgZ(1,"ion-avatar",1),e._UZ(2,"img",2),e.qZA(),e.TgZ(3,"ion-label"),e._uU(4),e._UZ(5,"br"),e.TgZ(6,"small"),e._uU(7),e.qZA()(),e.YNc(8,y,2,1,"ion-note",3),e.qZA()),2&i&&(e.xp6(4),e.lnq("",o.judgeregistration.name,", ",o.judgeregistration.vorname," (",o.judgeregistration.geschlecht,")"),e.xp6(3),e.AsE("",o.judgeregistration.mobilephone,", ",o.judgeregistration.mail,""),e.xp6(1),e.Q6J("ngIf",o.judgeregistration.comment))},dependencies:[l.sg,l.O5,r.BJ,r.Ie,r.Q$,r.uN]})}return n})();function S(n,a){if(1&n&&(e.TgZ(0,"ion-select-option",10),e._uU(1),e.ALo(2,"date"),e.qZA()),2&n){const t=a.$implicit;e.Q6J("value",t.uuid),e.xp6(1),e.hij(" ",t.titel+" "+e.xi3(2,2,t.datum,"dd-MM-yy"),"")}}function P(n,a){if(1&n){const t=e.EpF();e.TgZ(0,"ion-select",8),e.NdJ("ngModelChange",function(o){e.CHM(t);const s=e.oxw(2);return e.KtG(s.competition=o)}),e.YNc(1,S,3,5,"ion-select-option",9),e.qZA()}if(2&n){const t=e.oxw(2);e.Q6J("ngModel",t.competition),e.xp6(1),e.Q6J("ngForOf",t.getCompetitions())}}function L(n,a){if(1&n&&(e.TgZ(0,"ion-col")(1,"ion-item")(2,"ion-label"),e._uU(3,"Wettkampf"),e.qZA(),e.YNc(4,P,2,2,"ion-select",7),e.qZA()()),2&n){const t=e.oxw();e.xp6(4),e.Q6J("ngIf",t.getCompetitions().length>0)}}function j(n,a){if(1&n&&(e.TgZ(0,"ion-note",11),e._uU(1),e.qZA()),2&n){const t=e.oxw();e.xp6(1),e.hij(" ",t.competitionName()," ")}}function U(n,a){1&n&&(e.TgZ(0,"ion-item")(1,"ion-label"),e._uU(2,"loading ..."),e.qZA(),e._UZ(3,"ion-spinner"),e.qZA())}function Q(n,a){if(1&n){const t=e.EpF();e.TgZ(0,"ion-item-option",19),e.NdJ("click",function(){e.CHM(t);const o=e.oxw().$implicit,s=e.MAs(1),g=e.oxw(2);return e.KtG(g.delete(o,s))}),e._UZ(1,"ion-icon",17),e._uU(2," L\xf6schen "),e.qZA()}}function N(n,a){if(1&n){const t=e.EpF();e.TgZ(0,"ion-item-sliding",null,13)(2,"app-reg-judge-item",14),e.NdJ("click",function(){const s=e.CHM(t).$implicit,g=e.MAs(1),u=e.oxw(2);return e.KtG(u.itemTapped(s,g))}),e.qZA(),e.TgZ(3,"ion-item-options",15)(4,"ion-item-option",16),e.NdJ("click",function(){const s=e.CHM(t).$implicit,g=e.MAs(1),u=e.oxw(2);return e.KtG(u.edit(s,g))}),e._UZ(5,"ion-icon",17),e._uU(6),e.qZA(),e.YNc(7,Q,3,0,"ion-item-option",18),e.qZA()()}if(2&n){const t=a.$implicit,i=e.oxw(2);e.xp6(2),e.Q6J("judgeregistration",t)("selectedDisciplinlist",i.wkPgms),e.xp6(4),e.hij(" ",i.isLoggedInAsClub()?"Bearbeiten":i.isLoggedIn()?"Best\xe4tigen":"Anzeigen"," "),e.xp6(1),e.Q6J("ngIf",i.isLoggedInAsClub())}}function F(n,a){if(1&n&&(e.TgZ(0,"ion-content")(1,"ion-list"),e.YNc(2,U,4,0,"ion-item",3),e.ALo(3,"async"),e.YNc(4,N,8,4,"ion-item-sliding",12),e.qZA()()),2&n){const t=e.oxw();e.xp6(2),e.Q6J("ngIf",e.lcZ(3,2,t.busy)||!(t.competition&&t.currentRegistration&&t.sJudgeRegistrationList)),e.xp6(2),e.Q6J("ngForOf",t.filteredStartList)}}function w(n,a){if(1&n){const t=e.EpF();e.TgZ(0,"ion-button",20),e.NdJ("click",function(){e.CHM(t);const o=e.oxw();return e.KtG(o.createRegistration())}),e._UZ(1,"ion-icon",21),e._uU(2," Neue Anmeldung... "),e.qZA()}}const O=[{path:"",component:(()=>{class n{constructor(t,i,o,s){this.navCtrl=t,this.route=i,this.backendService=o,this.alertCtrl=s,this.busy=new _.X(!1),this.tMyQueryStream=new b.x,this.sFilterTask=void 0,this.sSyncActions=[],this.backendService.competitions||this.backendService.getCompetitions()}ngOnInit(){this.busy.next(!0);const t=this.route.snapshot.paramMap.get("wkId");this.currentRegId=parseInt(this.route.snapshot.paramMap.get("regId")),t?this.competition=t:this.backendService.competition&&(this.competition=this.backendService.competition)}ionViewWillEnter(){this.refreshList()}refreshList(){this.busy.next(!0),this.currentRegistration=void 0,this.backendService.getClubRegistrations(this.competition).pipe((0,h.h)(t=>!!t.find(i=>i.id===this.currentRegId)),(0,v.q)(1)).subscribe(t=>{this.currentRegistration=t.find(i=>i.id===this.currentRegId),console.log("ask Judgees-list for registration"),this.backendService.loadJudgeRegistrations(this.competition,this.currentRegId).subscribe(i=>{this.busy.next(!1),this.judgeregistrations=i,this.tMyQueryStream.pipe((0,h.h)(s=>!!s&&!!s.target),(0,x.U)(s=>s.target.value||"*"),(0,C.b)(300),(0,I.x)(),(0,R.b)(s=>this.busy.next(!0)),(0,T.w)(this.runQuery(i)),(0,Z.B)()).subscribe(s=>{this.sFilteredRegistrationList=s,this.busy.next(!1)})})})}set competition(t){(!this.currentRegistration||t!==this.backendService.competition)&&this.backendService.loadJudgeProgramDisziplinList(this.competition).subscribe(i=>{this.wkPgms=i})}get competition(){return this.backendService.competition||""}getCompetitions(){return this.backendService.competitions||[]}competitionName(){return this.backendService.competitionName}runQuery(t){return i=>{const o=i.trim(),s=[];return o&&t&&t.forEach(g=>{this.filter(o)(g)&&s.push(g)}),(0,J.of)(s)}}set judgeregistrations(t){this.sJudgeRegistrationList=t,this.reloadList(this.sMyQuery)}get judgeregistrations(){return this.sJudgeRegistrationList}get filteredStartList(){return this.sFilteredRegistrationList||this.sJudgeRegistrationList||[]}reloadList(t){this.tMyQueryStream.next(t)}itemTapped(t,i){i.getOpenAmount().then(o=>{o>0?i.close():i.open("end")})}isLoggedInAsClub(){return this.backendService.loggedIn&&this.backendService.authenticatedClubId===this.currentRegId+""}isLoggedInAsAdmin(){return this.backendService.loggedIn&&!!this.backendService.authenticatedClubId}isLoggedIn(){return this.backendService.loggedIn}filter(t){const i=t.toUpperCase().split(" ");return o=>"*"===t.trim()||i.filter(s=>{if(o.name.toUpperCase().indexOf(s)>-1||o.vorname.toUpperCase().indexOf(s)>-1||o.mobilephone.toUpperCase().indexOf(s)>-1||o.mail.toUpperCase().indexOf(s)>-1||o.comment.toUpperCase().indexOf(s)>-1)return!0}).length===i.length}createRegistration(){this.navCtrl.navigateForward(`reg-judgelist/${this.backendService.competition}/${this.currentRegId}/0`)}edit(t,i){this.navCtrl.navigateForward(`reg-judgelist/${this.backendService.competition}/${this.currentRegId}/${t.id}`),i.close()}delete(t,i){i.close(),this.alertCtrl.create({header:"Achtung",subHeader:"L\xf6schen der Wertungsrichter-Anmeldung am Wettkampf",message:"Hiermit wird die Anmeldung von "+t.name+", "+t.vorname+" am Wettkampf gel\xf6scht.",buttons:[{text:"ABBRECHEN",role:"cancel",handler:()=>{}},{text:"OKAY",handler:()=>{this.backendService.deleteJudgeRegistration(this.backendService.competition,this.currentRegId,t).subscribe(()=>{this.refreshList(),this.navCtrl.pop()})}}]}).then(s=>s.present())}static#e=this.\u0275fac=function(i){return new(i||n)(e.Y36(f.SH),e.Y36(p.gz),e.Y36(A.v),e.Y36(f.Br))};static#t=this.\u0275cmp=e.Xpm({type:n,selectors:[["app-reg-judgelist"]],decls:18,vars:5,consts:[["slot","start"],["defaultHref","/"],["no-padding",""],[4,"ngIf"],["slot","end",4,"ngIf"],["placeholder","Search","showCancelButton","never",3,"ngModel","ngModelChange","ionInput","ionCancel"],["size","large","expand","block","color","success",3,"click",4,"ngIf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],["slot","end"],[4,"ngFor","ngForOf"],["slidingJudgeRegistrationItem",""],[3,"judgeregistration","selectedDisciplinlist","click"],["side","end"],["color","primary",3,"click"],["name","arrow-forward-circle-outline","ios","md-arrow-forward-circle-outline"],["color","danger",3,"click",4,"ngIf"],["color","danger",3,"click"],["size","large","expand","block","color","success",3,"click"],["slot","start","name","add"]],template:function(i,o){1&i&&(e.TgZ(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",0),e._UZ(3,"ion-back-button",1),e.qZA(),e.TgZ(4,"ion-title")(5,"ion-grid",2)(6,"ion-row")(7,"ion-col")(8,"ion-label"),e._uU(9,"Wertungsrichter-Anmeldungen"),e.qZA()(),e.YNc(10,L,5,1,"ion-col",3),e.qZA()()(),e.YNc(11,j,2,1,"ion-note",4),e.qZA(),e.TgZ(12,"ion-searchbar",5),e.NdJ("ngModelChange",function(g){return o.sMyQuery=g})("ionInput",function(g){return o.reloadList(g)})("ionCancel",function(g){return o.reloadList(g)}),e.qZA()(),e.YNc(13,F,5,4,"ion-content",3),e.TgZ(14,"ion-footer")(15,"ion-toolbar")(16,"ion-list"),e.YNc(17,w,3,0,"ion-button",6),e.qZA()()()),2&i&&(e.xp6(10),e.Q6J("ngIf",!o.competition),e.xp6(1),e.Q6J("ngIf",o.competition),e.xp6(1),e.Q6J("ngModel",o.sMyQuery),e.xp6(1),e.Q6J("ngIf",o.wkPgms&&o.wkPgms.length>0),e.xp6(4),e.Q6J("ngIf",o.isLoggedInAsClub()))},dependencies:[l.sg,l.O5,d.JJ,d.On,r.YG,r.Sm,r.wI,r.W2,r.fr,r.jY,r.Gu,r.gu,r.Ie,r.u8,r.IK,r.td,r.Q$,r.q_,r.uN,r.Nd,r.VI,r.t9,r.n0,r.PQ,r.wd,r.sr,r.QI,r.j9,r.oU,M,l.Ov,l.uU]})}return n})()}];let Y=(()=>{class n{static#e=this.\u0275fac=function(i){return new(i||n)};static#t=this.\u0275mod=e.oAB({type:n});static#i=this.\u0275inj=e.cJS({imports:[p.Bz.forChild(O),p.Bz]})}return n})(),B=(()=>{class n{static#e=this.\u0275fac=function(i){return new(i||n)};static#t=this.\u0275mod=e.oAB({type:n});static#i=this.\u0275inj=e.cJS({imports:[l.ez,d.u5,r.Pc,Y]})}return n})()}}]);