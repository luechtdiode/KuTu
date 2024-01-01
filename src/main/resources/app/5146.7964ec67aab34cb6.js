"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[5146],{5146:(H,u,a)=>{a.r(u),a.d(u,{RegAthletlistPageModule:()=>Y});var g=a(6814),h=a(95),m=a(335),r=a(3582),A=a(5619),v=a(8645),b=a(2096),p=a(8180),f=a(2181),x=a(7398),T=a(3620),I=a(3997),R=a(9397),C=a(4664),Z=a(3020),t=a(2029),_=a(4414),k=a(9253);function y(s,c){if(1&s&&(t.TgZ(0,"ion-col")(1,"small"),t._uU(2),t.qZA()()),2&s){const e=t.oxw();t.xp6(2),t.Oqu(e.getTeamText())}}let S=(()=>{class s{constructor(){}athletregistration;vereinregistration;status;programmlist;teams;selected=new t.vpe;statusBadgeColor(){return"in sync"===this.status?"success":"warning"}statusComment(){return"in sync"===this.status?"":this.status.substring(this.status.indexOf("(")+1,this.status.length-1)}statusBadgeText(){return"in sync"===this.status?"\u2714 "+this.status:"\u2757 "+this.status.substring(0,this.status.indexOf("(")-1)}ngOnInit(){}getProgrammText(){const e=this.programmlist.find(i=>i.id===this.athletregistration.programId);return e?e.name:"..."}mapTeam(e){return[...this.teams.filter(i=>i.name?.trim().length>0&&i.index==e).map(i=>i.index>0?i.name+" "+i.index:i.name),""][0]}getTeamText(){if(this.athletregistration.team){const i=this.getProgrammText();return"Team "+this.mapTeam(this.athletregistration.team)+" (bei "+this.athletregistration.geschlecht+"/"+i+")"}return""}static \u0275fac=function(i){return new(i||s)};static \u0275cmp=t.Xpm({type:s,selectors:[["app-reg-athlet-item"]],inputs:{athletregistration:"athletregistration",vereinregistration:"vereinregistration",status:"status",programmlist:"programmlist",teams:"teams"},outputs:{selected:"selected"},decls:19,vars:10,consts:[[3,"click"],["slot","start"],["src","assets/imgs/athlete.png"],["no-padding",""],[4,"ngIf"],["slot","end",3,"color"]],template:function(i,n){1&i&&(t.TgZ(0,"ion-item",0),t.NdJ("click",function(){return n.selected?n.selected.emit(n.athletregistration):{}}),t.TgZ(1,"ion-avatar",1),t._UZ(2,"img",2),t.qZA(),t.TgZ(3,"ion-grid",3)(4,"ion-row")(5,"ion-col")(6,"ion-label"),t._uU(7),t._UZ(8,"br"),t.TgZ(9,"small"),t._uU(10),t.ALo(11,"date"),t.qZA()()()(),t.TgZ(12,"ion-row"),t.YNc(13,y,3,1,"ion-col",4),t.qZA()(),t.TgZ(14,"ion-badge",5),t._uU(15),t._UZ(16,"br"),t.TgZ(17,"small"),t._uU(18),t.qZA()()()),2&i&&(t.xp6(7),t.lnq("",n.athletregistration.name,", ",n.athletregistration.vorname," (",n.athletregistration.geschlecht,")"),t.xp6(3),t.hij("(",t.lcZ(11,8,n.athletregistration.gebdat),")"),t.xp6(3),t.Q6J("ngIf",0!=n.athletregistration.team),t.xp6(1),t.Q6J("color",n.statusBadgeColor()),t.xp6(1),t.Oqu(n.statusBadgeText()),t.xp6(3),t.Oqu(n.statusComment()))},dependencies:[g.O5,r.BJ,r.yp,r.wI,r.jY,r.Ie,r.Q$,r.Nd,g.uU]})}return s})();function P(s,c){if(1&s&&(t.TgZ(0,"ion-select-option",10),t._uU(1),t.ALo(2,"date"),t.qZA()),2&s){const e=c.$implicit;t.Q6J("value",e.uuid),t.xp6(1),t.hij(" ",e.titel+" "+t.xi3(2,2,e.datum,"dd-MM-yy"),"")}}function M(s,c){if(1&s){const e=t.EpF();t.TgZ(0,"ion-select",8),t.NdJ("ngModelChange",function(n){t.CHM(e);const o=t.oxw(2);return t.KtG(o.competition=n)}),t.YNc(1,P,3,5,"ion-select-option",9),t.qZA()}if(2&s){const e=t.oxw(2);t.Q6J("ngModel",e.competition),t.xp6(1),t.Q6J("ngForOf",e.getCompetitions())}}function w(s,c){if(1&s&&(t.TgZ(0,"ion-col")(1,"ion-item"),t.YNc(2,M,2,2,"ion-select",7),t.qZA()()),2&s){const e=t.oxw();t.xp6(2),t.Q6J("ngIf",e.getCompetitions().length>0)}}function L(s,c){if(1&s&&(t.TgZ(0,"ion-note",11),t._uU(1),t.qZA()),2&s){const e=t.oxw();t.xp6(1),t.hij(" ",e.competitionName()," ")}}function Q(s,c){1&s&&(t.TgZ(0,"ion-item")(1,"ion-label"),t._uU(2,"loading ..."),t.qZA(),t._UZ(3,"ion-spinner"),t.qZA())}function U(s,c){if(1&s){const e=t.EpF();t.TgZ(0,"ion-item-option",19),t.NdJ("click",function(){t.CHM(e);const n=t.oxw().$implicit,o=t.MAs(1),l=t.oxw(3);return t.KtG(l.delete(n,o))}),t._UZ(1,"ion-icon",17),t._uU(2," L\xf6schen "),t.qZA()}}function O(s,c){if(1&s){const e=t.EpF();t.TgZ(0,"ion-item-sliding",null,13)(2,"app-reg-athlet-item",14),t.NdJ("click",function(){const o=t.CHM(e).$implicit,l=t.MAs(1),d=t.oxw(3);return t.KtG(d.itemTapped(o,l))}),t.qZA(),t.TgZ(3,"ion-item-options",15)(4,"ion-item-option",16),t.NdJ("click",function(){const o=t.CHM(e).$implicit,l=t.MAs(1),d=t.oxw(3);return t.KtG(d.edit(o,l))}),t._UZ(5,"ion-icon",17),t._uU(6),t.qZA(),t.YNc(7,U,3,0,"ion-item-option",18),t.qZA()()}if(2&s){const e=c.$implicit,i=t.oxw(3);t.xp6(2),t.Q6J("athletregistration",e)("vereinregistration",i.currentRegistration)("status",i.getStatus(e))("programmlist",i.wkPgms)("teams",i.teams),t.xp6(4),t.hij(" ",i.isLoggedInAsClub()?"Bearbeiten":i.isLoggedIn()?"Best\xe4tigen":"Anzeigen"," "),t.xp6(1),t.Q6J("ngIf",i.isLoggedInAsClub())}}function F(s,c){if(1&s&&(t.TgZ(0,"div")(1,"ion-item-divider"),t._uU(2),t.qZA(),t.YNc(3,O,8,7,"ion-item-sliding",12),t.qZA()),2&s){const e=c.$implicit,i=t.oxw(2);t.xp6(2),t.Oqu(e.name),t.xp6(1),t.Q6J("ngForOf",i.filteredStartList(e.id))}}function J(s,c){if(1&s&&(t.TgZ(0,"ion-content")(1,"ion-list"),t.YNc(2,Q,4,0,"ion-item",3),t.ALo(3,"async"),t.YNc(4,F,4,2,"div",12),t.qZA()()),2&s){const e=t.oxw();t.xp6(2),t.Q6J("ngIf",t.lcZ(3,2,e.busy)||!(e.competition&&e.currentRegistration&&e.filteredPrograms)),t.xp6(2),t.Q6J("ngForOf",e.filteredPrograms)}}function N(s,c){if(1&s){const e=t.EpF();t.TgZ(0,"ion-button",20),t.NdJ("click",function(){t.CHM(e);const n=t.oxw();return t.KtG(n.createRegistration())}),t._UZ(1,"ion-icon",21),t._uU(2," Neue Anmeldung... "),t.qZA()}}const B=[{path:"",component:(()=>{class s{navCtrl;route;backendService;alertCtrl;busy=new A.X(!1);currentRegistration;teams;currentRegId;wkPgms;tMyQueryStream=new v.x;sFilterTask=void 0;sFilteredRegistrationList;sAthletRegistrationList;sMyQuery;sSyncActions=[];constructor(e,i,n,o){this.navCtrl=e,this.route=i,this.backendService=n,this.alertCtrl=o,this.backendService.competitions||this.backendService.getCompetitions()}ngOnInit(){this.busy.next(!0);const e=this.route.snapshot.paramMap.get("wkId");this.currentRegId=parseInt(this.route.snapshot.paramMap.get("regId")),e?this.competition=e:this.backendService.competition&&(this.competition=this.backendService.competition)}ionViewWillEnter(){this.refreshList()}getSyncActions(){this.backendService.loadRegistrationSyncActions().pipe((0,p.q)(1)).subscribe(e=>{this.sSyncActions=e})}getStatus(e){if(this.sSyncActions){const i=this.sSyncActions.find(n=>n.verein.id===e.vereinregistrationId&&n.caption.indexOf(e.name)>-1&&n.caption.indexOf(e.vorname)>-1);return i?"pending ("+i.caption.substring(0,(i.caption+":").indexOf(":"))+")":"in sync"}return"n/a"}refreshList(){this.busy.next(!0),this.currentRegistration=void 0,this.getSyncActions(),this.backendService.getClubRegistrations(this.competition).pipe((0,f.h)(e=>!!e.find(i=>i.id===this.currentRegId)),(0,p.q)(1)).subscribe(e=>{this.currentRegistration=e.find(i=>i.id===this.currentRegId),this.backendService.loadTeamsListForClub(this.competition,this.currentRegId).subscribe(i=>{this.teams=i}),console.log("ask athletes-list for registration"),this.backendService.loadAthletRegistrations(this.competition,this.currentRegId).subscribe(i=>{this.busy.next(!1),this.athletregistrations=i,this.tMyQueryStream.pipe((0,f.h)(o=>!!o&&!!o.target),(0,x.U)(o=>o.target.value||"*"),(0,T.b)(300),(0,I.x)(),(0,R.b)(o=>this.busy.next(!0)),(0,C.w)(this.runQuery(i)),(0,Z.B)()).subscribe(o=>{this.sFilteredRegistrationList=o,this.busy.next(!1)})})})}set competition(e){(!this.currentRegistration||e!==this.backendService.competition)&&this.backendService.loadProgramsForCompetition(this.competition).subscribe(i=>{this.wkPgms=i})}get competition(){return this.backendService.competition||""}getCompetitions(){return this.backendService.competitions||[]}competitionName(){return this.backendService.competitionName}runQuery(e){return i=>{const n=i.trim(),o=new Map;return e&&e.forEach(l=>{if(this.filter(n)(l)){const q=o.get(l.programId)||[];o.set(l.programId,[...q,l].sort(($,E)=>$.name.localeCompare(E.name)))}}),(0,b.of)(o)}}set athletregistrations(e){this.sAthletRegistrationList=e,this.runQuery(e)("*").subscribe(i=>this.sFilteredRegistrationList=i),this.reloadList(this.sMyQuery||"*")}get athletregistrations(){return this.sAthletRegistrationList}get filteredPrograms(){return[...this.wkPgms.filter(e=>this.sFilteredRegistrationList?.has(e.id))]}mapProgram(e){return this.wkPgms.filter(i=>i.id==e)[0]}filteredStartList(e){return this.sFilteredRegistrationList?.get(e)||this.sAthletRegistrationList||[]}reloadList(e){this.tMyQueryStream.next(e)}itemTapped(e,i){i.getOpenAmount().then(n=>{n>0?i.close():i.open("end")})}isLoggedInAsClub(){return this.backendService.loggedIn&&this.backendService.authenticatedClubId===this.currentRegId+""}isLoggedInAsAdmin(){return this.backendService.loggedIn&&!!this.backendService.authenticatedClubId}isLoggedIn(){return this.backendService.loggedIn}filter(e){const i=e.toUpperCase().split(" ");return n=>"*"===e.trim()||i.filter(o=>{if(n.name.toUpperCase().indexOf(o)>-1||n.vorname.toUpperCase().indexOf(o)>-1||this.wkPgms.find(l=>n.programId===l.id&&l.name===o)||n.gebdat.indexOf(o)>-1||3==o.length&&n.geschlecht.indexOf(o.substring(1,2))>-1)return!0}).length===i.length}createRegistration(){this.navCtrl.navigateForward(`reg-athletlist/${this.backendService.competition}/${this.currentRegId}/0`)}edit(e,i){this.navCtrl.navigateForward(`reg-athletlist/${this.backendService.competition}/${this.currentRegId}/${e.id}`),i.close()}needsPGMChoice(){const e=[...this.wkPgms][0];return!(1==e.aggregate&&e.riegenmode>1)}similarRegistration(e,i){return e.athletId===i.athletId||e.name===i.name&&e.vorname===i.vorname&&e.gebdat===i.gebdat&&e.geschlecht===i.geschlecht}delete(e,i){i.close(),this.alertCtrl.create({header:"Achtung",subHeader:"L\xf6schen der Athlet-Anmeldung am Wettkampf",message:"Hiermit wird die Anmeldung von "+e.name+", "+e.vorname+" am Wettkampf gel\xf6scht.",buttons:[{text:"ABBRECHEN",role:"cancel",handler:()=>{}},{text:"OKAY",handler:()=>{this.needsPGMChoice()||this.athletregistrations.filter(o=>this.similarRegistration(o,e)).filter(o=>o.id!==e.id).forEach(o=>{this.backendService.deleteAthletRegistration(this.backendService.competition,this.currentRegId,o)}),this.backendService.deleteAthletRegistration(this.backendService.competition,this.currentRegId,e).subscribe(()=>{this.refreshList()})}}]}).then(o=>o.present())}static \u0275fac=function(i){return new(i||s)(t.Y36(_.SH),t.Y36(m.gz),t.Y36(k.v),t.Y36(_.Br))};static \u0275cmp=t.Xpm({type:s,selectors:[["app-reg-athletlist"]],decls:18,vars:5,consts:[["slot","start"],["defaultHref","/"],["no-padding",""],[4,"ngIf"],["slot","end",4,"ngIf"],["placeholder","Search","showCancelButton","never",3,"ngModel","ngModelChange","ionInput","ionCancel"],["size","large","expand","block","color","success",3,"click",4,"ngIf"],["label","Wettkampf","placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["label","Wettkampf","placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],["slot","end"],[4,"ngFor","ngForOf"],["slidingAthletRegistrationItem",""],[3,"athletregistration","vereinregistration","status","programmlist","teams","click"],["side","end"],["color","primary",3,"click"],["name","arrow-forward-circle-outline","ios","md-arrow-forward-circle-outline"],["color","danger",3,"click",4,"ngIf"],["color","danger",3,"click"],["size","large","expand","block","color","success",3,"click"],["slot","start","name","add"]],template:function(i,n){1&i&&(t.TgZ(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",0),t._UZ(3,"ion-back-button",1),t.qZA(),t.TgZ(4,"ion-title")(5,"ion-grid",2)(6,"ion-row")(7,"ion-col")(8,"ion-label"),t._uU(9,"Angemeldete Athleten/Athletinnen"),t.qZA()(),t.YNc(10,w,3,1,"ion-col",3),t.qZA()()(),t.YNc(11,L,2,1,"ion-note",4),t.qZA(),t.TgZ(12,"ion-searchbar",5),t.NdJ("ngModelChange",function(l){return n.sMyQuery=l})("ionInput",function(l){return n.reloadList(l)})("ionCancel",function(l){return n.reloadList(l)}),t.qZA()(),t.YNc(13,J,5,4,"ion-content",3),t.TgZ(14,"ion-footer")(15,"ion-toolbar")(16,"ion-list"),t.YNc(17,N,3,0,"ion-button",6),t.qZA()()()),2&i&&(t.xp6(10),t.Q6J("ngIf",!n.competition),t.xp6(1),t.Q6J("ngIf",n.competition),t.xp6(1),t.Q6J("ngModel",n.sMyQuery),t.xp6(1),t.Q6J("ngIf",n.wkPgms&&n.wkPgms.length>0),t.xp6(4),t.Q6J("ngIf",n.isLoggedInAsClub()))},dependencies:[g.sg,g.O5,h.JJ,h.On,r.YG,r.Sm,r.wI,r.W2,r.fr,r.jY,r.Gu,r.gu,r.Ie,r.rH,r.u8,r.IK,r.td,r.Q$,r.q_,r.uN,r.Nd,r.VI,r.t9,r.n0,r.PQ,r.wd,r.sr,r.QI,r.j9,r.oU,S,g.Ov,g.uU]})}return s})()}];let Y=(()=>{class s{static \u0275fac=function(i){return new(i||s)};static \u0275mod=t.oAB({type:s});static \u0275inj=t.cJS({imports:[g.ez,h.u5,r.Pc,m.Bz.forChild(B)]})}return s})()}}]);