"use strict";(self.webpackChunkapp=self.webpackChunkapp||[]).push([[8646],{8646:(O,m,c)=>{c.r(m),c.d(m,{RegistrationPageModule:()=>N});var g=c(177),d=c(4341),p=c(305),r=c(4742),h=c(467),b=c(1413),f=c(4412),_=c(7673),u=c(6697),R=c(5964),k=c(6354),C=c(152),v=c(3294),y=c(8141),I=c(5558),S=c(7647),e=c(3953),F=c(2872),j=c(2350);let T=(()=>{class o{clubregistration;status;selected=new e.bkB;constructor(){}ngOnInit(){}static \u0275fac=function(i){return new(i||o)};static \u0275cmp=e.VBU({type:o,selectors:[["clublist-item"]],inputs:{clubregistration:"clubregistration",status:"status"},outputs:{selected:"selected"},decls:10,vars:5,consts:[[3,"click"],["slot","start"],["src","assets/imgs/verein.png"],["slot","end"]],template:function(i,n){1&i&&(e.j41(0,"ion-item",0),e.bIt("click",function(){return n.selected?n.selected.emit(n.clubregistration):{}}),e.j41(1,"ion-avatar",1),e.nrm(2,"img",2),e.k0s(),e.j41(3,"ion-label"),e.EFF(4),e.nrm(5,"br"),e.j41(6,"small"),e.EFF(7),e.k0s()(),e.j41(8,"ion-note",3),e.EFF(9),e.k0s()()),2&i&&(e.R7$(4),e.Lme("",n.clubregistration.vereinname,", ",n.clubregistration.verband,""),e.R7$(3),e.Lme("(",n.clubregistration.respVorname," ",n.clubregistration.respName,")"),e.R7$(2),e.SpI(" Status ",n.status," "))},dependencies:[r.mC,r.uz,r.he,r.JI]})}return o})();function A(o,l){if(1&o&&(e.j41(0,"ion-select-option",12),e.EFF(1),e.nI1(2,"date"),e.k0s()),2&o){const t=l.$implicit;e.Y8G("value",t.uuid),e.R7$(),e.SpI(" ",t.titel+" "+e.i5U(2,2,t.datum,"dd-MM-yy"),"")}}function L(o,l){if(1&o){const t=e.RV6();e.j41(0,"ion-select",10),e.mxI("ngModelChange",function(n){e.eBV(t);const s=e.XpG();return e.DH7(s.competition,n)||(s.competition=n),e.Njj(n)}),e.DNE(1,A,3,5,"ion-select-option",11),e.k0s()}if(2&o){const t=e.XpG();e.R50("ngModel",t.competition),e.R7$(),e.Y8G("ngForOf",t.getCompetitions())}}function x(o,l){1&o&&(e.j41(0,"ion-item")(1,"ion-label"),e.EFF(2,"loading ..."),e.k0s(),e.nrm(3,"ion-spinner"),e.k0s())}function M(o,l){if(1&o){const t=e.RV6();e.j41(0,"ion-item-option",20),e.bIt("click",function(){e.eBV(t),e.XpG();const n=e.sdS(1),s=e.XpG(2);return e.Njj(s.logout(n))}),e.nrm(1,"ion-icon",17),e.EFF(2," Abmelden "),e.k0s()}}function B(o,l){if(1&o){const t=e.RV6();e.j41(0,"ion-item-option",21),e.bIt("click",function(){e.eBV(t);const n=e.XpG().$implicit,s=e.sdS(1),a=e.XpG(2);return e.Njj(a.delete(n,s))}),e.nrm(1,"ion-icon",17),e.EFF(2," L\xf6schen "),e.k0s()}}function E(o,l){if(1&o){const t=e.RV6();e.j41(0,"ion-item-sliding",null,0)(2,"clublist-item",14),e.bIt("click",function(){const n=e.eBV(t).$implicit,s=e.sdS(1),a=e.XpG(2);return e.Njj(a.itemTapped(n,s))}),e.k0s(),e.j41(3,"ion-item-options",15)(4,"ion-item-option",16),e.bIt("click",function(){const n=e.eBV(t).$implicit,s=e.sdS(1),a=e.XpG(2);return e.Njj(a.login(n,s))}),e.nrm(5,"ion-icon",17),e.EFF(6),e.k0s(),e.DNE(7,M,3,0,"ion-item-option",18)(8,B,3,0,"ion-item-option",19),e.k0s()()}if(2&o){const t=l.$implicit,i=e.XpG(2);e.R7$(2),e.Y8G("clubregistration",t)("status",i.getStatus(t)),e.R7$(4),e.SpI(" ",i.isLoggedIn(t)?"Bearbeiten":"Login"," "),e.R7$(),e.Y8G("ngIf",i.isLoggedIn(t)),e.R7$(),e.Y8G("ngIf",i.isLoggedIn(t))}}function P(o,l){if(1&o&&(e.j41(0,"ion-content")(1,"ion-list"),e.DNE(2,x,4,0,"ion-item",7),e.nI1(3,"async"),e.DNE(4,E,9,5,"ion-item-sliding",13),e.k0s()()),2&o){const t=e.XpG();e.R7$(2),e.Y8G("ngIf",e.bMT(3,2,t.isBusy)),e.R7$(2),e.Y8G("ngForOf",t.filteredStartList)}}function G(o,l){if(1&o){const t=e.RV6();e.j41(0,"ion-button",22),e.bIt("click",function(){e.eBV(t);const n=e.XpG();return e.Njj(n.createRegistration())}),e.nrm(1,"ion-icon",23),e.EFF(2," Neue Anmeldung... "),e.k0s()}}function V(o,l){if(1&o){const t=e.RV6();e.j41(0,"ion-button",24,1),e.nI1(2,"async"),e.bIt("click",function(){e.eBV(t);const n=e.XpG();return e.Njj(n.logout(null))}),e.nrm(3,"ion-icon",25),e.EFF(4,"Vereins-Login Abmelden"),e.k0s()}if(2&o){const t=e.XpG();e.Y8G("disabled",e.bMT(2,1,t.isBusy))}}const $=[{path:"",component:(()=>{class o{navCtrl;route;backendService;toastController;alertCtrl;sClubRegistrationList;sFilteredRegistrationList;sMyQuery;sSyncActions=[];tMyQueryStream=new b.B;sFilterTask=void 0;busy=new f.t(!1);constructor(t,i,n,s,a){this.navCtrl=t,this.route=i,this.backendService=n,this.toastController=s,this.alertCtrl=a,this.backendService.competitions||this.backendService.getCompetitions()}ngOnInit(){this.busy.next(!0);const t=this.route.snapshot.paramMap.get("wkId");t?this.competition=t:this.backendService.competition&&(this.competition=this.backendService.competition)}ionViewWillEnter(){this.getSyncActions()}get stationFreezed(){return this.backendService.stationFreezed}set competition(t){(!this.clubregistrations||t!==this.backendService.competition)&&(this.busy.next(!0),this.clubregistrations=[],this.backendService.getClubRegistrations(t).pipe((0,u.s)(1)).subscribe(i=>{this.getSyncActions(),this.clubregistrations=i,this.busy.next(!1),this.tMyQueryStream.pipe((0,R.p)(s=>!!s&&!!s.target),(0,k.T)(s=>s.target.value||"*"),(0,C.B)(300),(0,v.F)(),(0,y.M)(s=>this.busy.next(!0)),(0,I.n)(this.runQuery(i)),(0,S.u)()).subscribe(s=>{this.sFilteredRegistrationList=s,this.busy.next(!1)})}))}get competition(){return this.backendService.competition||""}set clubregistrations(t){this.sClubRegistrationList=t,this.reloadList(this.sMyQuery)}get clubregistrations(){return this.sClubRegistrationList}get filteredStartList(){return this.sFilteredRegistrationList||this.sClubRegistrationList}get isBusy(){return this.busy}getSyncActions(){this.backendService.loadRegistrationSyncActions().pipe((0,u.s)(1)).subscribe(t=>{this.sSyncActions=t})}getStatus(t){return this.sSyncActions?this.sSyncActions.find(i=>i.verein.id===t.id)?"Abgleich ausstehend":"Nachgef\xfchrt":"n/a"}runQuery(t){return i=>{const n=i.trim(),s=[];return n&&t&&t.forEach(a=>{this.filter(n)(a)&&s.push(a)}),(0,_.of)(s)}}reloadList(t){this.tMyQueryStream.next(t)}itemTapped(t,i){i.getOpenAmount().then(n=>{n>0?i.close():i.open("end")})}isLoggedIn(t){return this.backendService.loggedIn&&this.backendService.authenticatedClubId===t.id+""}isLoggedInAsClub(){return this.backendService.loggedIn&&!!this.backendService.authenticatedClubId}logout(t){t&&t.close(),this.alertCtrl.create({header:"Achtung",message:"F\xfcr sp\xe4tere Bearbeitungen an der Registrierung des Vereins am Wettkampf ist ein erneutes Login notwendig.",buttons:[{text:"ABBRECHEN",role:"cancel",handler:()=>{}},{text:"OKAY",handler:()=>{this.backendService.clublogout()}}]}).then(n=>n.present())}delete(t,i){i.close(),this.alertCtrl.create({header:"Achtung",subHeader:"L\xf6schen der Vereins-Registrierung und dessen Anmeldungen am Wettkampf",message:"Hiermit wird die Vereinsanmeldung am Wettkampf komplett gel\xf6scht.",buttons:[{text:"ABBRECHEN",role:"cancel",handler:()=>{}},{text:"OKAY",handler:()=>{this.backendService.deleteClubRegistration(this.competition,t.id)}}]}).then(s=>s.present())}showPasswordResetSuccess(){var t=this;return(0,h.A)(function*(){(yield t.toastController.create({header:"Passwort-Reset",message:"Es wurde eine EMail mit dem Reset-Link versendet!",animated:!0,position:"middle",buttons:[{text:"OK",role:"cancel"}]})).present()})()}login(t,i){i.close(),this.isLoggedIn(t)?this.navCtrl.navigateForward(`registration/${this.competition}/${t.id}`):this.alertCtrl.create({header:"Vereins-Login",message:`Login f\xfcr die Bearbeitung der Vereinsanmeldung am Wettkampf ${this.competitionName()}`,inputs:[{name:"username",label:"Vereinsname",placeholder:"Vereinsname",value:`${t.vereinname} (${t.verband})`,type:"text"},{name:"pw",label:"Passwort",placeholder:"Passwort",type:"password"}],buttons:[{text:"Abbrechen",role:"cancel",handler:()=>{console.log("Cancel clicked")}},{text:"Login",handler:s=>!!(s.pw&&s.pw.trim().length>0)&&(this.backendService.clublogin(t.id,s.pw).pipe((0,u.s)(1)).subscribe({next:a=>{this.navCtrl.navigateForward(`registration/${this.competition}/${t.id}`)},error:a=>{401===a.status&&this.alertCtrl.create({header:"Vereins-Login Fehlgeschlagen",message:`Soll f\xfcr diese Vereinsanmeldung am Wettkampf ${this.competitionName()} ein Passwort-Reset Vorgang gestartet werden?`,inputs:[],buttons:[{text:"Abbrechen",role:"cancel",handler:()=>(console.log("Cancel clicked"),!1)},{text:"Passwort-Reset",handler:()=>(this.backendService.resetRegistration(t.id).pipe((0,u.s)(1)).subscribe(()=>{this.showPasswordResetSuccess()}),!0)}]}).then(X=>{X.present()})}}),!0)}]}).then(s=>{s.onkeypress=a=>{"Enter"===a.code&&s.getElementsByClassName("ion-activatable")[1].click()},s.present()})}createRegistration(){this.navCtrl.navigateForward(`registration/${this.competition}/0`)}getCompetitions(){return this.backendService.competitions||[]}competitionName(){return this.backendService.competitionName}filter(t){const i=t.toUpperCase().split(" ");return n=>"*"===t.trim()||i.filter(s=>{if(n.vereinname.toUpperCase().indexOf(s)>-1||n.verband.toUpperCase().indexOf(s)>-1||n.mail.toUpperCase().indexOf(s)>-1||n.respName.toUpperCase().indexOf(s)>-1||n.respVorname.toUpperCase().indexOf(s)>-1)return!0}).length===i.length}static \u0275fac=function(i){return new(i||o)(e.rXU(F.q9),e.rXU(p.nX),e.rXU(j.m),e.rXU(r.K_),e.rXU(r.hG))};static \u0275cmp=e.VBU({type:o,selectors:[["app-registration"]],decls:18,vars:5,consts:[["slidingClubRegistrationItem",""],["btnDelete",""],["slot","start"],["slot","icon-only","name","menu"],["slot","end"],["label","Wettkampf","placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModel","ngModelChange",4,"ngIf"],["placeholder","Search","showCancelButton","never",3,"ngModelChange","ionInput","ionCancel","ngModel"],[4,"ngIf"],["size","large","expand","block","color","success",3,"click",4,"ngIf"],["size","large","expand","block","color","danger",3,"disabled","click",4,"ngIf"],["label","Wettkampf","placeholder","Bitte ausw\xe4hlen","okText","Okay","cancelText","Abbrechen",3,"ngModelChange","ngModel"],[3,"value",4,"ngFor","ngForOf"],[3,"value"],[4,"ngFor","ngForOf"],[3,"click","clubregistration","status"],["side","end"],["color","primary",3,"click"],["name","arrow-forward-circle-outline","ios","md-arrow-forward-circle-outline"],["color","secondary",3,"click",4,"ngIf"],["color","danger",3,"click",4,"ngIf"],["color","secondary",3,"click"],["color","danger",3,"click"],["size","large","expand","block","color","success",3,"click"],["slot","start","name","add"],["size","large","expand","block","color","danger",3,"click","disabled"],["slot","start","name","log-out"]],template:function(i,n){1&i&&(e.j41(0,"ion-header")(1,"ion-toolbar")(2,"ion-buttons",2)(3,"ion-menu-toggle")(4,"ion-button"),e.nrm(5,"ion-icon",3),e.k0s()()(),e.j41(6,"ion-title")(7,"ion-label"),e.EFF(8,"Wettkampf-Anmeldungen"),e.k0s()(),e.j41(9,"ion-buttons",4),e.DNE(10,L,2,2,"ion-select",5),e.k0s()(),e.j41(11,"ion-searchbar",6),e.mxI("ngModelChange",function(a){return e.DH7(n.sMyQuery,a)||(n.sMyQuery=a),a}),e.bIt("ionInput",function(a){return n.reloadList(a)})("ionCancel",function(a){return n.reloadList(a)}),e.k0s()(),e.DNE(12,P,5,4,"ion-content",7),e.j41(13,"ion-footer")(14,"ion-toolbar")(15,"ion-list"),e.DNE(16,G,3,0,"ion-button",8)(17,V,5,3,"ion-button",9),e.k0s()()()),2&i&&(e.R7$(10),e.Y8G("ngIf",n.getCompetitions().length>0),e.R7$(),e.R50("ngModel",n.sMyQuery),e.R7$(),e.Y8G("ngIf",n.competition&&n.clubregistrations),e.R7$(4),e.Y8G("ngIf",!n.isLoggedInAsClub()),e.R7$(),e.Y8G("ngIf",n.isLoggedInAsClub()))},dependencies:[g.Sq,g.bT,d.BC,d.vS,r.Jm,r.QW,r.W9,r.M0,r.eU,r.iq,r.uz,r.LU,r.CE,r.A7,r.he,r.nf,r.cA,r.S1,r.Nm,r.Ip,r.w2,r.BC,r.ai,r.Je,r.Gw,T,g.Jj,g.vh]})}return o})()}];let N=(()=>{class o{static \u0275fac=function(i){return new(i||o)};static \u0275mod=e.$C({type:o});static \u0275inj=e.G2t({imports:[g.MD,d.YN,r.bv,p.iI.forChild($)]})}return o})()}}]);