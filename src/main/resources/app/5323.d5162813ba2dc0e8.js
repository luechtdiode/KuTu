"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[5323],{5323:(O,m,a)=>{a.r(m),a.d(m,{RegistrationPageModule:()=>B});var g=a(9808),u=a(2382),h=a(1631),r=a(3349),_=a(655),f=a(7579),b=a(1135),v=a(9646),x=a(9300),C=a(4004),A=a(8372),y=a(1884),k=a(8505),T=a(3900),Z=a(3099),d=a(5698),e=a(7587),R=a(600);let S=(()=>{class o{constructor(){this.selected=new e.vpe}ngOnInit(){}}return o.\u0275fac=function(t){return new(t||o)},o.\u0275cmp=e.Xpm({type:o,selectors:[["clublist-item"]],inputs:{clubregistration:"clubregistration",status:"status"},outputs:{selected:"selected"},decls:10,vars:5,consts:[[3,"click"],["slot","start"],["src","assets/imgs/verein.png"],["slot","end"]],template:function(t,i){1&t&&(e.TgZ(0,"ion-item",0),e.NdJ("click",function(){return i.selected?i.selected.emit(i.clubregistration):{}}),e.TgZ(1,"ion-avatar",1),e._UZ(2,"img",2),e.qZA(),e.TgZ(3,"ion-label"),e._uU(4),e._UZ(5,"br"),e.TgZ(6,"small"),e._uU(7),e.qZA()(),e.TgZ(8,"ion-note",3),e._uU(9),e.qZA()()),2&t&&(e.xp6(4),e.AsE("",i.clubregistration.vereinname,", ",i.clubregistration.verband,""),e.xp6(3),e.AsE("(",i.clubregistration.respVorname," ",i.clubregistration.respName,")"),e.xp6(2),e.hij(" Status ",i.status," "))},directives:[r.Ie,r.BJ,r.Q$,r.uN],styles:[""]}),o})();function I(o,c){if(1&o&&(e.TgZ(0,"ion-select-option",10),e._uU(1),e.ALo(2,"date"),e.qZA()),2&o){const t=c.$implicit;e.Q6J("value",t.uuid),e.xp6(1),e.hij(" ",t.titel+" "+e.xi3(2,2,t.datum,"dd-MM-yy"),"")}}function M(o,c){if(1&o){const t=e.EpF();e.TgZ(0,"ion-select",8),e.NdJ("ngModelChange",function(s){return e.CHM(t),e.oxw().competition=s}),e.YNc(1,I,3,5,"ion-select-option",9),e.qZA()}if(2&o){const t=e.oxw();e.Q6J("ngModel",t.competition),e.xp6(1),e.Q6J("ngForOf",t.getCompetitions())}}function P(o,c){1&o&&(e.TgZ(0,"ion-item")(1,"ion-label"),e._uU(2,"loading ..."),e.qZA(),e._UZ(3,"ion-spinner"),e.qZA())}function w(o,c){if(1&o){const t=e.EpF();e.TgZ(0,"ion-item-option",19),e.NdJ("click",function(){e.CHM(t),e.oxw();const s=e.MAs(1);return e.oxw(2).logout(s)}),e._UZ(1,"ion-icon",16),e._uU(2," Abmelden "),e.qZA()}}function L(o,c){if(1&o){const t=e.EpF();e.TgZ(0,"ion-item-option",20),e.NdJ("click",function(){e.CHM(t);const s=e.oxw().$implicit,n=e.MAs(1);return e.oxw(2).delete(s,n)}),e._UZ(1,"ion-icon",16),e._uU(2," L\xf6schen "),e.qZA()}}function N(o,c){if(1&o){const t=e.EpF();e.TgZ(0,"ion-item-sliding",null,12)(2,"clublist-item",13),e.NdJ("click",function(){const n=e.CHM(t).$implicit,l=e.MAs(1);return e.oxw(2).itemTapped(n,l)}),e.qZA(),e.TgZ(3,"ion-item-options",14)(4,"ion-item-option",15),e.NdJ("click",function(){const n=e.CHM(t).$implicit,l=e.MAs(1);return e.oxw(2).login(n,l)}),e._UZ(5,"ion-icon",16),e._uU(6),e.qZA(),e.YNc(7,w,3,0,"ion-item-option",17),e.YNc(8,L,3,0,"ion-item-option",18),e.qZA()()}if(2&o){const t=c.$implicit,i=e.oxw(2);e.xp6(2),e.Q6J("clubregistration",t)("status",i.getStatus(t)),e.xp6(4),e.hij(" ",i.isLoggedIn(t)?"Bearbeiten":"Login"," "),e.xp6(1),e.Q6J("ngIf",i.isLoggedIn(t)),e.xp6(1),e.Q6J("ngIf",i.isLoggedIn(t))}}function F(o,c){if(1&o&&(e.TgZ(0,"ion-content")(1,"ion-list"),e.YNc(2,P,4,0,"ion-item",5),e.ALo(3,"async"),e.YNc(4,N,9,5,"ion-item-sliding",11),e.qZA()()),2&o){const t=e.oxw();e.xp6(2),e.Q6J("ngIf",e.lcZ(3,2,t.isBusy)),e.xp6(2),e.Q6J("ngForOf",t.filteredStartList)}}function U(o,c){if(1&o){const t=e.EpF();e.TgZ(0,"ion-button",21),e.NdJ("click",function(){return e.CHM(t),e.oxw().createRegistration()}),e._UZ(1,"ion-icon",22),e._uU(2," Neue Anmeldung... "),e.qZA()}}function J(o,c){if(1&o){const t=e.EpF();e.TgZ(0,"ion-button",23,24),e.NdJ("click",function(){return e.CHM(t),e.oxw().logout(null)}),e.ALo(2,"async"),e._UZ(3,"ion-icon",25),e._uU(4,"Vereins-Login Abmelden"),e.qZA()}if(2&o){const t=e.oxw();e.Q6J("disabled",e.lcZ(2,1,t.isBusy))}}const Q=[{path:"",component:(()=>{class o{constructor(t,i,s,n,l){this.navCtrl=t,this.route=i,this.backendService=s,this.toastController=n,this.alertCtrl=l,this.sSyncActions=[],this.tMyQueryStream=new f.x,this.sFilterTask=void 0,this.busy=new b.X(!1),this.backendService.competitions||this.backendService.getCompetitions()}ngOnInit(){this.busy.next(!0);const t=this.route.snapshot.paramMap.get("wkId");t?this.competition=t:this.backendService.competition&&(this.competition=this.backendService.competition)}ionViewWillEnter(){this.getSyncActions()}get stationFreezed(){return this.backendService.stationFreezed}set competition(t){(!this.clubregistrations||t!==this.backendService.competition)&&(this.busy.next(!0),this.clubregistrations=[],this.backendService.getClubRegistrations(t).subscribe(i=>{this.getSyncActions(),this.clubregistrations=i,this.busy.next(!1),this.tMyQueryStream.pipe((0,x.h)(n=>!!n&&!!n.target),(0,C.U)(n=>n.target.value||"*"),(0,A.b)(300),(0,y.x)(),(0,k.b)(n=>this.busy.next(!0)),(0,T.w)(this.runQuery(i)),(0,Z.B)()).subscribe(n=>{this.sFilteredRegistrationList=n,this.busy.next(!1)})}))}get competition(){return this.backendService.competition||""}set clubregistrations(t){this.sClubRegistrationList=t,this.reloadList(this.sMyQuery)}get clubregistrations(){return this.sClubRegistrationList}get filteredStartList(){return this.sFilteredRegistrationList||this.sClubRegistrationList}get isBusy(){return this.busy}getSyncActions(){this.backendService.loadRegistrationSyncActions().pipe((0,d.q)(1)).subscribe(t=>{this.sSyncActions=t})}getStatus(t){return this.sSyncActions?this.sSyncActions.find(i=>i.verein.id===t.id)?"Abgleich ausstehend":"Nachgef\xfchrt":"n/a"}runQuery(t){return i=>{const s=i.trim(),n=[];return s&&t&&t.forEach(l=>{this.filter(s)(l)&&n.push(l)}),(0,v.of)(n)}}reloadList(t){this.tMyQueryStream.next(t)}itemTapped(t,i){i.getOpenAmount().then(s=>{s>0?i.close():i.open("end")})}isLoggedIn(t){return this.backendService.loggedIn&&this.backendService.authenticatedClubId===t.id+""}isLoggedInAsClub(){return this.backendService.loggedIn&&!!this.backendService.authenticatedClubId}logout(t){t&&t.close(),this.alertCtrl.create({header:"Achtung",message:"F\xfcr sp\xe4tere Bearbeitungen an der Registrierung des Vereins am Wettkampf ist ein erneutes Login notwendig.",buttons:[{text:"ABBRECHEN",role:"cancel",handler:()=>{}},{text:"OKAY",handler:()=>{this.backendService.clublogout()}}]}).then(s=>s.present())}delete(t,i){i.close(),this.alertCtrl.create({header:"Achtung",subHeader:"L\xf6schen der Vereins-Registrierung und dessen Anmeldungen am Wettkampf",message:"Hiermit wird die Vereinsanmeldung am Wettkampf komplett gel\xf6scht.",buttons:[{text:"ABBRECHEN",role:"cancel",handler:()=>{}},{text:"OKAY",handler:()=>{this.backendService.deleteClubRegistration(this.competition,t.id)}}]}).then(n=>n.present())}showPasswordResetSuccess(){return(0,_.mG)(this,void 0,void 0,function*(){(yield this.toastController.create({header:"Passwort-Reset",message:"Es wurde eine EMail mit dem Reset-Link versendet!",animated:!0,position:"middle",buttons:[{text:"OK",role:"cancel"}]})).present()})}login(t,i){i.close(),this.isLoggedIn(t)?this.navCtrl.navigateForward(`registration/${this.competition}/${t.id}`):this.alertCtrl.create({header:"Vereins-Login",message:`Login f\xfcr die Bearbeitung der Vereinsanmeldung am Wettkampf ${this.competitionName()}`,inputs:[{name:"username",label:"Vereinsname",placeholder:"Vereinsname",value:`${t.vereinname} (${t.verband})`,type:"text"},{name:"pw",label:"Passwort",placeholder:"Passwort",type:"password"}],buttons:[{text:"Abbrechen",role:"cancel",handler:()=>{console.log("Cancel clicked")}},{text:"Login",handler:n=>!!(n.pw&&n.pw.trim().length>0)&&(this.backendService.clublogin(t.id,n.pw).pipe((0,d.q)(1)).subscribe(l=>{this.navCtrl.navigateForward(`registration/${this.competition}/${t.id}`)},l=>{401===l.status&&this.alertCtrl.create({header:"Vereins-Login Fehlgeschlagen",message:`Soll f\xfcr diese Vereinsanmeldung am Wettkampf ${this.competitionName()} ein Passwort-Reset Vorgang gestartet werden?`,inputs:[],buttons:[{text:"Abbrechen",role:"cancel",handler:()=>(console.log("Cancel clicked"),!1)},{text:"Passwort-Reset",handler:()=>(this.backendService.resetRegistration(t.id).pipe((0,d.q)(1)).subscribe(()=>{this.showPasswordResetSuccess()}),!0)}]}).then(E=>{E.present()})}),!0)}]}).then(n=>{n.onkeypress=l=>{"Enter"===l.code&&n.getElementsByClassName("ion-activatable")[1].click()},n.present()})}createRegistration(){this.navCtrl.navigateForward(`registration/${this.competition}/0`)}getCompetitions(){return this.backendService.competitions||[]}competitionName(){return this.backendService.competitionName}filter(t){const i=t.toUpperCase().split(" ");return s=>"*"===t.trim()||i.filter(n=>{if(s.vereinname.toUpperCase().indexOf(n)>-1||s.verband.toUpperCase().indexOf(n)>-1||s.mail.toUpperCase().indexOf(n)>-1||s.respName.toUpperCase().indexOf(n)>-1||s.respVorname.toUpperCase().indexOf(n)>-1)return!0}).length===i.length}}return o.\u0275fac=function(t){return new(t||o)(e.Y36(r.SH),e.Y36(h.gz),e.Y36(R.v),e.Y36(r.yF),e.Y36(r.Br))},o.\u0275cmp=e.Xpm({type:o,selectors:[["app-registration"]],decls:18,vars:5,consts:[["slot","start"],["slot","icon-only","name","menu"],["slot","end"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["placeholder","Search","showCancelButton","never",3,"ngModel","ngModelChange","ionInput","ionCancel"],[4,"ngIf"],["size","large","expand","block","color","success",3,"click",4,"ngIf"],["size","large","expand","block","color","danger",3,"disabled","click",4,"ngIf"],["placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],[4,"ngFor","ngForOf"],["slidingClubRegistrationItem",""],[3,"clubregistration","status","click"],["side","end"],["color","primary",3,"click"],["name","arrow-dropright-circle","ios","md-arrow-dropright-circle"],["color","secondary",3,"click",4,"ngIf"],["color","danger",3,"click",4,"ngIf"],["color","secondary",3,"click"],["color","danger",3,"click"],["size","large","expand","block","color","success",3,"click"],["slot","start","name","add"],["size","large","expand","block","color","danger",3,"disabled","click"],["btnDelete",""],["slot","start","name","log-out"]],template:function(t,i){1&t&&(e.TgZ(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",0)(3,"ion-menu-toggle")(4,"ion-button"),e._UZ(5,"ion-icon",1),e.qZA()()(),e.TgZ(6,"ion-title")(7,"ion-label"),e._uU(8,"Wettkampf-Anmeldungen"),e.qZA()(),e.TgZ(9,"ion-buttons",2),e.YNc(10,M,2,2,"ion-select",3),e.qZA()(),e.TgZ(11,"ion-searchbar",4),e.NdJ("ngModelChange",function(n){return i.sMyQuery=n})("ionInput",function(n){return i.reloadList(n)})("ionCancel",function(n){return i.reloadList(n)}),e.qZA()(),e.YNc(12,F,5,4,"ion-content",5),e.TgZ(13,"ion-footer")(14,"ion-toolbar")(15,"ion-list"),e.YNc(16,U,3,0,"ion-button",6),e.YNc(17,J,5,3,"ion-button",7),e.qZA()()()),2&t&&(e.xp6(10),e.Q6J("ngIf",i.getCompetitions().length>0),e.xp6(1),e.Q6J("ngModel",i.sMyQuery),e.xp6(1),e.Q6J("ngIf",i.competition&&i.clubregistrations),e.xp6(4),e.Q6J("ngIf",!i.isLoggedInAsClub()),e.xp6(1),e.Q6J("ngIf",i.isLoggedInAsClub()))},directives:[r.Gu,r.sr,r.Sm,r.zc,r.YG,r.gu,r.wd,r.Q$,g.O5,r.t9,r.QI,u.JJ,u.On,g.sg,r.n0,r.VI,r.j9,r.W2,r.q_,r.Ie,r.PQ,r.td,S,r.IK,r.u8,r.fr],pipes:[g.uU,g.Ov],styles:[""]}),o})()}];let B=(()=>{class o{}return o.\u0275fac=function(t){return new(t||o)},o.\u0275mod=e.oAB({type:o}),o.\u0275inj=e.cJS({imports:[[g.ez,u.u5,r.Pc,h.Bz.forChild(Q)]]}),o})()}}]);