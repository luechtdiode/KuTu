"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[6482],{6482:(B,m,g)=>{g.r(m),g.d(m,{RegJudgelistPageModule:()=>Y});var a=g(6895),d=g(4719),s=g(9928),p=g(229),_=g(591),f=g(5529),J=g(1086),h=g(2198),b=g(2986),R=g(4850),v=g(13),x=g(5778),C=g(2868),I=g(7545),T=g(2474),e=g(5675),Z=g(600);function A(n,c){if(1&n&&(e.TgZ(0,"span"),e._uU(1),e._UZ(2,"br"),e.qZA()),2&n){const t=c.$implicit;e.xp6(1),e.Oqu(t)}}function k(n,c){if(1&n&&(e.TgZ(0,"ion-note",4),e.YNc(1,A,3,1,"span",5),e.qZA()),2&n){const t=e.oxw();e.xp6(1),e.Q6J("ngForOf",t.getCommentLines())}}let P=(()=>{class n{constructor(){this.selected=new e.vpe}ngOnInit(){}getProgramList(){var t=[];return void 0===this.selectedDisciplinlist&&(this.selectedDisciplinlist=[]),this.selectedDisciplinlist.forEach(i=>{void 0===t.find(r=>r.program==i.program)&&t.push(i)}),t}getProgrammDisciplinText(t){return t.program+"/"+t.disziplin}getProgrammText(t){return t.program}getCommentLines(){return this.judgeregistration.comment.split("\n")}}return n.\u0275fac=function(t){return new(t||n)},n.\u0275cmp=e.Xpm({type:n,selectors:[["app-reg-judge-item"]],inputs:{judgeregistration:"judgeregistration",status:"status",selectedDisciplinlist:"selectedDisciplinlist"},outputs:{selected:"selected"},decls:9,vars:6,consts:[[3,"click"],["slot","start"],["src","assets/imgs/chief.png"],["slot","end",4,"ngIf"],["slot","end"],[4,"ngFor","ngForOf"]],template:function(t,i){1&t&&(e.TgZ(0,"ion-item",0),e.NdJ("click",function(){return i.selected?i.selected.emit(i.judgeregistration):{}}),e.TgZ(1,"ion-avatar",1),e._UZ(2,"img",2),e.qZA(),e.TgZ(3,"ion-label"),e._uU(4),e._UZ(5,"br"),e.TgZ(6,"small"),e._uU(7),e.qZA()(),e.YNc(8,k,2,1,"ion-note",3),e.qZA()),2&t&&(e.xp6(4),e.lnq("",i.judgeregistration.name,", ",i.judgeregistration.vorname," (",i.judgeregistration.geschlecht,")"),e.xp6(3),e.AsE("",i.judgeregistration.mobilephone,", ",i.judgeregistration.mail,""),e.xp6(1),e.Q6J("ngIf",i.judgeregistration.comment))},dependencies:[a.sg,a.O5,s.BJ,s.Ie,s.Q$,s.uN]}),n})();function M(n,c){if(1&n&&(e.TgZ(0,"ion-select-option",10),e._uU(1),e.ALo(2,"date"),e.qZA()),2&n){const t=c.$implicit;e.Q6J("value",t.uuid),e.xp6(1),e.hij(" ",t.titel+" "+e.xi3(2,2,t.datum,"dd-MM-yy"),"")}}function y(n,c){if(1&n){const t=e.EpF();e.TgZ(0,"ion-select",8),e.NdJ("ngModelChange",function(r){e.CHM(t);const o=e.oxw(2);return e.KtG(o.competition=r)}),e.YNc(1,M,3,5,"ion-select-option",9),e.qZA()}if(2&n){const t=e.oxw(2);e.Q6J("ngModel",t.competition),e.xp6(1),e.Q6J("ngForOf",t.getCompetitions())}}function S(n,c){if(1&n&&(e.TgZ(0,"ion-col")(1,"ion-item")(2,"ion-label"),e._uU(3,"Wettkampf"),e.qZA(),e.YNc(4,y,2,2,"ion-select",7),e.qZA()()),2&n){const t=e.oxw();e.xp6(4),e.Q6J("ngIf",t.getCompetitions().length>0)}}function L(n,c){if(1&n&&(e.TgZ(0,"ion-note",11),e._uU(1),e.qZA()),2&n){const t=e.oxw();e.xp6(1),e.hij(" ",t.competitionName()," ")}}function j(n,c){1&n&&(e.TgZ(0,"ion-item")(1,"ion-label"),e._uU(2,"loading ..."),e.qZA(),e._UZ(3,"ion-spinner"),e.qZA())}function U(n,c){if(1&n){const t=e.EpF();e.TgZ(0,"ion-item-option",19),e.NdJ("click",function(){e.CHM(t);const r=e.oxw().$implicit,o=e.MAs(1),l=e.oxw(2);return e.KtG(l.delete(r,o))}),e._UZ(1,"ion-icon",17),e._uU(2," L\xf6schen "),e.qZA()}}function Q(n,c){if(1&n){const t=e.EpF();e.TgZ(0,"ion-item-sliding",null,13)(2,"app-reg-judge-item",14),e.NdJ("click",function(){const o=e.CHM(t).$implicit,l=e.MAs(1),u=e.oxw(2);return e.KtG(u.itemTapped(o,l))}),e.qZA(),e.TgZ(3,"ion-item-options",15)(4,"ion-item-option",16),e.NdJ("click",function(){const o=e.CHM(t).$implicit,l=e.MAs(1),u=e.oxw(2);return e.KtG(u.edit(o,l))}),e._UZ(5,"ion-icon",17),e._uU(6),e.qZA(),e.YNc(7,U,3,0,"ion-item-option",18),e.qZA()()}if(2&n){const t=c.$implicit,i=e.oxw(2);e.xp6(2),e.Q6J("judgeregistration",t)("selectedDisciplinlist",i.wkPgms),e.xp6(4),e.hij(" ",i.isLoggedInAsClub()?"Bearbeiten":i.isLoggedIn()?"Best\xe4tigen":"Anzeigen"," "),e.xp6(1),e.Q6J("ngIf",i.isLoggedInAsClub())}}function N(n,c){if(1&n&&(e.TgZ(0,"ion-content")(1,"ion-list"),e.YNc(2,j,4,0,"ion-item",3),e.ALo(3,"async"),e.YNc(4,Q,8,4,"ion-item-sliding",12),e.qZA()()),2&n){const t=e.oxw();e.xp6(2),e.Q6J("ngIf",e.lcZ(3,2,t.busy)||!(t.competition&&t.currentRegistration&&t.sJudgeRegistrationList)),e.xp6(2),e.Q6J("ngForOf",t.filteredStartList)}}function F(n,c){if(1&n){const t=e.EpF();e.TgZ(0,"ion-button",20),e.NdJ("click",function(){e.CHM(t);const r=e.oxw();return e.KtG(r.createRegistration())}),e._UZ(1,"ion-icon",21),e._uU(2," Neue Anmeldung... "),e.qZA()}}const w=[{path:"",component:(()=>{class n{constructor(t,i,r,o){this.navCtrl=t,this.route=i,this.backendService=r,this.alertCtrl=o,this.busy=new _.X(!1),this.tMyQueryStream=new f.xQ,this.sFilterTask=void 0,this.sSyncActions=[],this.backendService.competitions||this.backendService.getCompetitions()}ngOnInit(){this.busy.next(!0);const t=this.route.snapshot.paramMap.get("wkId");this.currentRegId=parseInt(this.route.snapshot.paramMap.get("regId")),t?this.competition=t:this.backendService.competition&&(this.competition=this.backendService.competition)}ionViewWillEnter(){this.refreshList()}refreshList(){this.busy.next(!0),this.currentRegistration=void 0,this.backendService.getClubRegistrations(this.competition).pipe((0,h.h)(t=>!!t.find(i=>i.id===this.currentRegId)),(0,b.q)(1)).subscribe(t=>{this.currentRegistration=t.find(i=>i.id===this.currentRegId),console.log("ask Judgees-list for registration"),this.backendService.loadJudgeRegistrations(this.competition,this.currentRegId).subscribe(i=>{this.busy.next(!1),this.judgeregistrations=i,this.tMyQueryStream.pipe((0,h.h)(o=>!!o&&!!o.target),(0,R.U)(o=>o.target.value||"*"),(0,v.b)(300),(0,x.x)(),(0,C.b)(o=>this.busy.next(!0)),(0,I.w)(this.runQuery(i)),(0,T.B)()).subscribe(o=>{this.sFilteredRegistrationList=o,this.busy.next(!1)})})})}set competition(t){(!this.currentRegistration||t!==this.backendService.competition)&&this.backendService.loadJudgeProgramDisziplinList(this.competition).subscribe(i=>{this.wkPgms=i})}get competition(){return this.backendService.competition||""}getCompetitions(){return this.backendService.competitions||[]}competitionName(){return this.backendService.competitionName}runQuery(t){return i=>{const r=i.trim(),o=[];return r&&t&&t.forEach(l=>{this.filter(r)(l)&&o.push(l)}),(0,J.of)(o)}}set judgeregistrations(t){this.sJudgeRegistrationList=t,this.reloadList(this.sMyQuery)}get judgeregistrations(){return this.sJudgeRegistrationList}get filteredStartList(){return this.sFilteredRegistrationList||this.sJudgeRegistrationList||[]}reloadList(t){this.tMyQueryStream.next(t)}itemTapped(t,i){i.getOpenAmount().then(r=>{r>0?i.close():i.open("end")})}isLoggedInAsClub(){return this.backendService.loggedIn&&this.backendService.authenticatedClubId===this.currentRegId+""}isLoggedInAsAdmin(){return this.backendService.loggedIn&&!!this.backendService.authenticatedClubId}isLoggedIn(){return this.backendService.loggedIn}filter(t){const i=t.toUpperCase().split(" ");return r=>"*"===t.trim()||i.filter(o=>{if(r.name.toUpperCase().indexOf(o)>-1||r.vorname.toUpperCase().indexOf(o)>-1||r.mobilephone.toUpperCase().indexOf(o)>-1||r.mail.toUpperCase().indexOf(o)>-1||r.comment.toUpperCase().indexOf(o)>-1)return!0}).length===i.length}createRegistration(){this.navCtrl.navigateForward(`reg-judgelist/${this.backendService.competition}/${this.currentRegId}/0`)}edit(t,i){this.navCtrl.navigateForward(`reg-judgelist/${this.backendService.competition}/${this.currentRegId}/${t.id}`),i.close()}delete(t,i){i.close(),this.alertCtrl.create({header:"Achtung",subHeader:"L\xf6schen der Wertungsrichter-Anmeldung am Wettkampf",message:"Hiermit wird die Anmeldung von "+t.name+", "+t.vorname+" am Wettkampf gel\xf6scht.",buttons:[{text:"ABBRECHEN",role:"cancel",handler:()=>{}},{text:"OKAY",handler:()=>{this.backendService.deleteJudgeRegistration(this.backendService.competition,this.currentRegId,t).subscribe(()=>{this.refreshList(),this.navCtrl.pop()})}}]}).then(o=>o.present())}}return n.\u0275fac=function(t){return new(t||n)(e.Y36(s.SH),e.Y36(p.gz),e.Y36(Z.v),e.Y36(s.Br))},n.\u0275cmp=e.Xpm({type:n,selectors:[["app-reg-judgelist"]],decls:18,vars:5,consts:[["slot","start"],["defaultHref","/"],["no-padding",""],[4,"ngIf"],["slot","end",4,"ngIf"],["placeholder","Search","showCancelButton","never",3,"ngModel","ngModelChange","ionInput","ionCancel"],["size","large","expand","block","color","success",3,"click",4,"ngIf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],["slot","end"],[4,"ngFor","ngForOf"],["slidingJudgeRegistrationItem",""],[3,"judgeregistration","selectedDisciplinlist","click"],["side","end"],["color","primary",3,"click"],["name","arrow-dropright-circle","ios","md-arrow-dropright-circle"],["color","danger",3,"click",4,"ngIf"],["color","danger",3,"click"],["size","large","expand","block","color","success",3,"click"],["slot","start","name","add"]],template:function(t,i){1&t&&(e.TgZ(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",0),e._UZ(3,"ion-back-button",1),e.qZA(),e.TgZ(4,"ion-title")(5,"ion-grid",2)(6,"ion-row")(7,"ion-col")(8,"ion-label"),e._uU(9,"Wertungsrichter-Anmeldungen"),e.qZA()(),e.YNc(10,S,5,1,"ion-col",3),e.qZA()()(),e.YNc(11,L,2,1,"ion-note",4),e.qZA(),e.TgZ(12,"ion-searchbar",5),e.NdJ("ngModelChange",function(o){return i.sMyQuery=o})("ionInput",function(o){return i.reloadList(o)})("ionCancel",function(o){return i.reloadList(o)}),e.qZA()(),e.YNc(13,N,5,4,"ion-content",3),e.TgZ(14,"ion-footer")(15,"ion-toolbar")(16,"ion-list"),e.YNc(17,F,3,0,"ion-button",6),e.qZA()()()),2&t&&(e.xp6(10),e.Q6J("ngIf",!i.competition),e.xp6(1),e.Q6J("ngIf",i.competition),e.xp6(1),e.Q6J("ngModel",i.sMyQuery),e.xp6(1),e.Q6J("ngIf",i.wkPgms&&i.wkPgms.length>0),e.xp6(4),e.Q6J("ngIf",i.isLoggedInAsClub()))},dependencies:[a.sg,a.O5,d.JJ,d.On,s.oU,s.YG,s.Sm,s.wI,s.W2,s.fr,s.jY,s.Gu,s.gu,s.Ie,s.u8,s.IK,s.td,s.Q$,s.q_,s.uN,s.Nd,s.VI,s.t9,s.n0,s.PQ,s.sr,s.wd,s.QI,s.j9,s.cs,P,a.Ov,a.uU]}),n})()}];let O=(()=>{class n{}return n.\u0275fac=function(t){return new(t||n)},n.\u0275mod=e.oAB({type:n}),n.\u0275inj=e.cJS({imports:[p.Bz.forChild(w),p.Bz]}),n})(),Y=(()=>{class n{}return n.\u0275fac=function(t){return new(t||n)},n.\u0275mod=e.oAB({type:n}),n.\u0275inj=e.cJS({imports:[a.ez,d.u5,s.Pc,O]}),n})()}}]);