"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[4685],{4685:(U,m,c)=>{c.r(m),c.d(m,{RegJudgelistPageModule:()=>O});var g=c(177),d=c(4341),r=c(7125),u=c(1313),h=c(4412),f=c(1413),_=c(7673),p=c(5964),R=c(6697),b=c(6354),k=c(152),I=c(3294),v=c(8141),j=c(5558),C=c(7647),e=c(4020),J=c(2872),F=c(2350);function S(n,l){if(1&n&&(e.j41(0,"span"),e.EFF(1),e.nrm(2,"br"),e.k0s()),2&n){const t=l.$implicit;e.R7$(),e.JRh(t)}}function y(n,l){if(1&n&&(e.j41(0,"ion-note",4),e.DNE(1,S,3,1,"span",5),e.k0s()),2&n){const t=e.XpG();e.R7$(),e.Y8G("ngForOf",t.getCommentLines())}}let T=(()=>{class n{judgeregistration;status;selectedDisciplinlist;selected=new e.bkB;ngOnInit(){}getProgramList(){var t=[];return void 0===this.selectedDisciplinlist&&(this.selectedDisciplinlist=[]),this.selectedDisciplinlist.forEach(i=>{void 0===t.find(s=>s.program==i.program)&&t.push(i)}),t}getProgrammDisciplinText(t){return t.program+"/"+t.disziplin}getProgrammText(t){return t.program}getCommentLines(){return this.judgeregistration.comment.split("\n")}static \u0275fac=function(i){return new(i||n)};static \u0275cmp=e.VBU({type:n,selectors:[["app-reg-judge-item"]],inputs:{judgeregistration:"judgeregistration",status:"status",selectedDisciplinlist:"selectedDisciplinlist"},outputs:{selected:"selected"},standalone:!1,decls:9,vars:6,consts:[[3,"click"],["slot","start"],["src","assets/imgs/chief.png"],["slot","end",4,"ngIf"],["slot","end"],[4,"ngFor","ngForOf"]],template:function(i,s){1&i&&(e.j41(0,"ion-item",0),e.bIt("click",function(){return s.selected?s.selected.emit(s.judgeregistration):{}}),e.j41(1,"ion-avatar",1),e.nrm(2,"img",2),e.k0s(),e.j41(3,"ion-label"),e.EFF(4),e.nrm(5,"br"),e.j41(6,"small"),e.EFF(7),e.k0s()(),e.DNE(8,y,2,1,"ion-note",3),e.k0s()),2&i&&(e.R7$(4),e.E5c("",s.judgeregistration.name,", ",s.judgeregistration.vorname," (",s.judgeregistration.geschlecht,")"),e.R7$(3),e.Lme("",s.judgeregistration.mobilephone,", ",s.judgeregistration.mail,""),e.R7$(),e.Y8G("ngIf",s.judgeregistration.comment))},dependencies:[g.Sq,g.bT,r.mC,r.uz,r.he,r.JI],encapsulation:2})}return n})();function L(n,l){if(1&n&&(e.j41(0,"ion-select-option",11),e.EFF(1),e.nI1(2,"date"),e.k0s()),2&n){const t=l.$implicit;e.Y8G("value",t.uuid),e.R7$(),e.SpI(" ",t.titel+" "+e.i5U(2,2,t.datum,"dd-MM-yy"),"")}}function E(n,l){if(1&n){const t=e.RV6();e.j41(0,"ion-select",9),e.mxI("ngModelChange",function(s){e.eBV(t);const o=e.XpG(2);return e.DH7(o.competition,s)||(o.competition=s),e.Njj(s)}),e.DNE(1,L,3,5,"ion-select-option",10),e.k0s()}if(2&n){const t=e.XpG(2);e.R50("ngModel",t.competition),e.R7$(),e.Y8G("ngForOf",t.getCompetitions())}}function M(n,l){if(1&n&&(e.j41(0,"ion-col")(1,"ion-item")(2,"ion-label"),e.EFF(3,"Wettkampf"),e.k0s(),e.DNE(4,E,2,2,"ion-select",8),e.k0s()()),2&n){const t=e.XpG();e.R7$(4),e.Y8G("ngIf",t.getCompetitions().length>0)}}function P(n,l){if(1&n&&(e.j41(0,"ion-note",12),e.EFF(1),e.k0s()),2&n){const t=e.XpG();e.R7$(),e.SpI(" ",t.competitionName()," ")}}function G(n,l){1&n&&(e.j41(0,"ion-item")(1,"ion-label"),e.EFF(2,"loading ..."),e.k0s(),e.nrm(3,"ion-spinner"),e.k0s())}function $(n,l){if(1&n){const t=e.RV6();e.j41(0,"ion-item-option",19),e.bIt("click",function(){e.eBV(t);const s=e.XpG().$implicit,o=e.sdS(1),a=e.XpG(2);return e.Njj(a.delete(s,o))}),e.nrm(1,"ion-icon",17),e.EFF(2," L\xf6schen "),e.k0s()}}function x(n,l){if(1&n){const t=e.RV6();e.j41(0,"ion-item-sliding",null,0)(2,"app-reg-judge-item",14),e.bIt("click",function(){const s=e.eBV(t).$implicit,o=e.sdS(1),a=e.XpG(2);return e.Njj(a.itemTapped(s,o))}),e.k0s(),e.j41(3,"ion-item-options",15)(4,"ion-item-option",16),e.bIt("click",function(){const s=e.eBV(t).$implicit,o=e.sdS(1),a=e.XpG(2);return e.Njj(a.edit(s,o))}),e.nrm(5,"ion-icon",17),e.EFF(6),e.k0s(),e.DNE(7,$,3,0,"ion-item-option",18),e.k0s()()}if(2&n){const t=l.$implicit,i=e.XpG(2);e.R7$(2),e.Y8G("judgeregistration",t)("selectedDisciplinlist",i.wkPgms),e.R7$(4),e.SpI(" ",i.isLoggedInAsClub()?"Bearbeiten":i.isLoggedIn()?"Best\xe4tigen":"Anzeigen"," "),e.R7$(),e.Y8G("ngIf",i.isLoggedInAsClub())}}function D(n,l){if(1&n&&(e.j41(0,"ion-content")(1,"ion-list"),e.DNE(2,G,4,0,"ion-item",4),e.nI1(3,"async"),e.DNE(4,x,8,4,"ion-item-sliding",13),e.k0s()()),2&n){const t=e.XpG();e.R7$(2),e.Y8G("ngIf",e.bMT(3,2,t.busy)||!(t.competition&&t.currentRegistration&&t.sJudgeRegistrationList)),e.R7$(2),e.Y8G("ngForOf",t.filteredStartList)}}function N(n,l){if(1&n){const t=e.RV6();e.j41(0,"ion-button",20),e.bIt("click",function(){e.eBV(t);const s=e.XpG();return e.Njj(s.createRegistration())}),e.nrm(1,"ion-icon",21),e.EFF(2," Neue Anmeldung... "),e.k0s()}}const B=[{path:"",component:(()=>{class n{navCtrl;route;backendService;alertCtrl;busy=new h.t(!1);currentRegistration;currentRegId;wkPgms;tMyQueryStream=new f.B;sFilterTask=void 0;sFilteredRegistrationList;sJudgeRegistrationList;sMyQuery;sSyncActions=[];constructor(t,i,s,o){this.navCtrl=t,this.route=i,this.backendService=s,this.alertCtrl=o,this.backendService.competitions||this.backendService.getCompetitions()}ngOnInit(){this.busy.next(!0);const t=this.route.snapshot.paramMap.get("wkId");this.currentRegId=parseInt(this.route.snapshot.paramMap.get("regId")),t?this.competition=t:this.backendService.competition&&(this.competition=this.backendService.competition)}ionViewWillEnter(){this.refreshList()}refreshList(){this.busy.next(!0),this.currentRegistration=void 0,this.backendService.getClubRegistrations(this.competition).pipe((0,p.p)(t=>!!t.find(i=>i.id===this.currentRegId)),(0,R.s)(1)).subscribe(t=>{this.currentRegistration=t.find(i=>i.id===this.currentRegId),console.log("ask Judgees-list for registration"),this.backendService.loadJudgeRegistrations(this.competition,this.currentRegId).subscribe(i=>{this.busy.next(!1),this.judgeregistrations=i,this.tMyQueryStream.pipe((0,p.p)(o=>!!o&&!!o.target),(0,b.T)(o=>o.target.value||"*"),(0,k.B)(300),(0,I.F)(),(0,v.M)(o=>this.busy.next(!0)),(0,j.n)(this.runQuery(i)),(0,C.u)()).subscribe(o=>{this.sFilteredRegistrationList=o,this.busy.next(!1)})})})}set competition(t){(!this.currentRegistration||t!==this.backendService.competition)&&this.backendService.loadJudgeProgramDisziplinList(this.competition).subscribe(i=>{this.wkPgms=i})}get competition(){return this.backendService.competition||""}getCompetitions(){return this.backendService.competitions||[]}competitionName(){return this.backendService.competitionName}runQuery(t){return i=>{const s=i.trim(),o=[];return s&&t&&t.forEach(a=>{this.filter(s)(a)&&o.push(a)}),(0,_.of)(o)}}set judgeregistrations(t){this.sJudgeRegistrationList=t,this.reloadList(this.sMyQuery)}get judgeregistrations(){return this.sJudgeRegistrationList}get filteredStartList(){return this.sFilteredRegistrationList||this.sJudgeRegistrationList||[]}reloadList(t){this.tMyQueryStream.next(t)}itemTapped(t,i){i.getOpenAmount().then(s=>{s>0?i.close():i.open("end")})}isLoggedInAsClub(){return this.backendService.loggedIn&&this.backendService.authenticatedClubId===this.currentRegId+""}isLoggedInAsAdmin(){return this.backendService.loggedIn&&!!this.backendService.authenticatedClubId}isLoggedIn(){return this.backendService.loggedIn}filter(t){const i=t.toUpperCase().split(" ");return s=>"*"===t.trim()||i.filter(o=>{if(s.name.toUpperCase().indexOf(o)>-1||s.vorname.toUpperCase().indexOf(o)>-1||s.mobilephone.toUpperCase().indexOf(o)>-1||s.mail.toUpperCase().indexOf(o)>-1||s.comment.toUpperCase().indexOf(o)>-1)return!0}).length===i.length}createRegistration(){this.navCtrl.navigateForward(`reg-judgelist/${this.backendService.competition}/${this.currentRegId}/0`)}edit(t,i){this.navCtrl.navigateForward(`reg-judgelist/${this.backendService.competition}/${this.currentRegId}/${t.id}`),i.close()}delete(t,i){i.close(),this.alertCtrl.create({header:"Achtung",subHeader:"L\xf6schen der Wertungsrichter-Anmeldung am Wettkampf",message:"Hiermit wird die Anmeldung von "+t.name+", "+t.vorname+" am Wettkampf gel\xf6scht.",buttons:[{text:"ABBRECHEN",role:"cancel",handler:()=>{}},{text:"OKAY",handler:()=>{this.backendService.deleteJudgeRegistration(this.backendService.competition,this.currentRegId,t).subscribe(()=>{this.refreshList(),this.navCtrl.pop()})}}]}).then(o=>o.present())}static \u0275fac=function(i){return new(i||n)(e.rXU(J.q9),e.rXU(u.nX),e.rXU(F.m),e.rXU(r.hG))};static \u0275cmp=e.VBU({type:n,selectors:[["app-reg-judgelist"]],standalone:!1,decls:18,vars:5,consts:[["slidingJudgeRegistrationItem",""],["slot","start"],["defaultHref","/"],["no-padding",""],[4,"ngIf"],["slot","end",4,"ngIf"],["placeholder","Search","showCancelButton","never",3,"ngModelChange","ionInput","ionCancel","ngModel"],["size","large","expand","block","color","success",3,"click",4,"ngIf"],["label","Wettkampf","placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["label","Wettkampf","placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModelChange","ngModel"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],["slot","end"],[4,"ngFor","ngForOf"],[3,"click","judgeregistration","selectedDisciplinlist"],["side","end"],["color","primary",3,"click"],["name","arrow-forward-circle-outline","ios","md-arrow-forward-circle-outline"],["color","danger",3,"click",4,"ngIf"],["color","danger",3,"click"],["size","large","expand","block","color","success",3,"click"],["slot","start","name","add"]],template:function(i,s){1&i&&(e.j41(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",1),e.nrm(3,"ion-back-button",2),e.k0s(),e.j41(4,"ion-title")(5,"ion-grid",3)(6,"ion-row")(7,"ion-col")(8,"ion-label"),e.EFF(9,"Wertungsrichter-Anmeldungen"),e.k0s()(),e.DNE(10,M,5,1,"ion-col",4),e.k0s()()(),e.DNE(11,P,2,1,"ion-note",5),e.k0s(),e.j41(12,"ion-searchbar",6),e.mxI("ngModelChange",function(a){return e.DH7(s.sMyQuery,a)||(s.sMyQuery=a),a}),e.bIt("ionInput",function(a){return s.reloadList(a)})("ionCancel",function(a){return s.reloadList(a)}),e.k0s()(),e.DNE(13,D,5,4,"ion-content",4),e.j41(14,"ion-footer")(15,"ion-toolbar")(16,"ion-list"),e.DNE(17,N,3,0,"ion-button",7),e.k0s()()()),2&i&&(e.R7$(10),e.Y8G("ngIf",!s.competition),e.R7$(),e.Y8G("ngIf",s.competition),e.R7$(),e.R50("ngModel",s.sMyQuery),e.R7$(),e.Y8G("ngIf",s.wkPgms&&s.wkPgms.length>0),e.R7$(4),e.Y8G("ngIf",s.isLoggedInAsClub()))},dependencies:[g.Sq,g.bT,d.BC,d.vS,r.Jm,r.QW,r.hU,r.W9,r.M0,r.lO,r.eU,r.iq,r.uz,r.LU,r.CE,r.A7,r.he,r.nf,r.JI,r.ln,r.S1,r.Nm,r.Ip,r.w2,r.BC,r.ai,r.Je,r.Gw,r.el,T,g.Jj,g.vh],encapsulation:2})}return n})()}];let A=(()=>{class n{static \u0275fac=function(i){return new(i||n)};static \u0275mod=e.$C({type:n});static \u0275inj=e.G2t({imports:[u.iI.forChild(B),u.iI]})}return n})(),O=(()=>{class n{static \u0275fac=function(i){return new(i||n)};static \u0275mod=e.$C({type:n});static \u0275inj=e.G2t({imports:[g.MD,d.YN,r.bv,A]})}return n})()}}]);